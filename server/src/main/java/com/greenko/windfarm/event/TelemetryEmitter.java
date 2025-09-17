// path: server/src/main/java/com/greenko/windfarm/event/TelemetryEmitter.java
package com.greenko.windfarm.event;

import com.greenko.windfarm.config.WindfarmProperties;
import com.greenko.windfarm.csv.DatasetLoader;
import com.greenko.windfarm.csv.DatasetLoader.DatasetSnapshot;
import com.greenko.windfarm.model.TelemetryRecord;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TelemetryEmitter implements DisposableBean {
  private static final Logger log = LoggerFactory.getLogger(TelemetryEmitter.class);
  private final TelemetryEventBus eventBus;
  private final DatasetLoader datasetLoader;
  private final WindfarmProperties properties;
  private final ApplicationArguments arguments;
  private final Environment environment;
  private final ScheduledExecutorService scheduler;

  private List<TelemetryRecord> records;
  private Duration interval;
  private Duration datasetSpan;
  private final AtomicInteger index = new AtomicInteger();
  private final AtomicLong cycle = new AtomicLong();

  public TelemetryEmitter(
      TelemetryEventBus eventBus,
      DatasetLoader datasetLoader,
      WindfarmProperties properties,
      ApplicationArguments arguments,
      Environment environment) {
    this.eventBus = eventBus;
    this.datasetLoader = datasetLoader;
    this.properties = properties;
    this.arguments = arguments;
    this.environment = environment;
    CustomizableThreadFactory factory = new CustomizableThreadFactory("telemetry-emitter-");
    factory.setDaemon(true);
    this.scheduler = Executors.newSingleThreadScheduledExecutor(factory);
  }

  @PostConstruct
  private void initialize() {
    if (!properties.getEmitter().isEnabled()) {
      log.info("Telemetry emitter disabled via configuration");
      return;
    }
    DatasetSnapshot snapshot = datasetLoader.loadDataset();
    this.records = snapshot.records();
    this.interval = determineInterval();
    Instant start = snapshot.start();
    Instant end = snapshot.end();
    this.datasetSpan = Duration.between(start, end).plus(interval);
    log.info(
        "Telemetry emitter started with {} normalized rows (interval={} ms)",
        records.size(),
        interval.toMillis());
    scheduler.scheduleAtFixedRate(this::emitNext, 0L, interval.toMillis(), TimeUnit.MILLISECONDS);
  }

  private Duration determineInterval() {
    String override = environment.getProperty("EMIT_INTERVAL_MS");
    if (StringUtils.hasText(override)) {
      try {
        return Duration.ofMillis(Long.parseLong(override));
      } catch (NumberFormatException ex) {
        log.warn("Invalid EMIT_INTERVAL_MS value '{}', falling back to defaults", override);
      }
    }
    boolean accelerate =
        properties.getEmitter().isAccelerate()
            || arguments.containsOption("accelerate")
            || Boolean.parseBoolean(environment.getProperty("EMIT_ACCELERATE", "false"));
    return accelerate
        ? properties.getEmitter().getAcceleratedInterval()
        : properties.getEmitter().getInterval();
  }

  private void emitNext() {
    if (records == null || records.isEmpty()) {
      return;
    }
    int currentIndex = index.getAndUpdate(i -> (i + 1) % records.size());
    TelemetryRecord base = records.get(currentIndex);
    long cycleNumber = cycle.get();
    TelemetryRecord adjusted =
        base.withTime(base.time().plus(datasetSpan.multipliedBy(cycleNumber)));
    eventBus.publish(adjusted);
    if (currentIndex == records.size() - 1) {
      cycle.incrementAndGet();
      log.debug("Completed emitter cycle {}", cycleNumber);
    }
  }

  @Override
  public void destroy() {
    scheduler.shutdownNow();
  }
}
