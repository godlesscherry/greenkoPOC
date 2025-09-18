// path: server/src/main/java/com/greenko/windfarm/event/TelemetryListener.java
package com.greenko.windfarm.event;

import com.greenko.windfarm.config.WindfarmProperties;
import com.greenko.windfarm.model.TelemetryRecord;
import jakarta.annotation.PreDestroy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

@Service
public class TelemetryListener {
  private static final Logger log = LoggerFactory.getLogger(TelemetryListener.class);
  private final JdbcTemplate jdbcTemplate;
  private final TelemetryEventBus eventBus;
  private final WindfarmProperties properties;
  private Disposable subscription;
  private final AtomicBoolean started = new AtomicBoolean();

  public TelemetryListener(
      JdbcTemplate jdbcTemplate, TelemetryEventBus eventBus, WindfarmProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.eventBus = eventBus;
    this.properties = properties;
    initialize();
  }

  private void initialize() {
    if (!properties.getListener().isEnabled()) {
      log.info("Telemetry listener disabled via configuration");
      return;
    }
    Duration flush = properties.getListener().getFlushInterval();
    int batchSize = properties.getListener().getBatchSize();
    subscription =
        eventBus.stream()
            .publishOn(Schedulers.newSingle("telemetry-listener"))
            .bufferTimeout(batchSize, flush)
            .filter(batch -> !batch.isEmpty())
            .subscribe(this::persistBatch, this::handleError);
    started.set(true);
  }

  private void handleError(Throwable throwable) {
    log.error("Telemetry listener terminated due to error", throwable);
  }

  private void persistBatch(List<TelemetryRecord> batch) {
    jdbcTemplate.batchUpdate(
        "INSERT INTO telemetry(time, device_id, energy_kwh) VALUES (?, ?, ?) "
            + "ON CONFLICT (time, device_id) DO UPDATE SET energy_kwh = EXCLUDED.energy_kwh",
        batch,
        batch.size(),
        new ParameterizedPreparedStatementSetter<>() {
          @Override
          public void setValues(PreparedStatement ps, TelemetryRecord record) throws SQLException {
            ps.setObject(1, java.sql.Timestamp.from(record.time()));
            ps.setString(2, record.deviceId());
            ps.setDouble(3, record.energyKwh());
          }
        });
    if (log.isDebugEnabled()) {
      log.debug("Persisted {} telemetry rows", batch.size());
    }
  }

  public boolean isStarted() {
    return started.get();
  }

  @PreDestroy
  public void shutdown() {
    if (subscription != null && !subscription.isDisposed()) {
      subscription.dispose();
    }
  }
}
