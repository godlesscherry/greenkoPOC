// path: server/src/main/java/com/greenko/windfarm/event/TelemetryEventBus.java
package com.greenko.windfarm.event;

import com.greenko.windfarm.model.TelemetryRecord;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class TelemetryEventBus {
  private static final Logger log = LoggerFactory.getLogger(TelemetryEventBus.class);
  private final Sinks.Many<TelemetryRecord> sink =
      Sinks.many().multicast().onBackpressureBuffer(5000, false);

  public void publish(TelemetryRecord record) {
    Objects.requireNonNull(record, "record");
    Sinks.EmitResult result = sink.tryEmitNext(record);
    if (!result.isSuccess()) {
      log.warn("Dropped telemetry event {} due to backpressure ({})", record, result);
    }
  }

  public Flux<TelemetryRecord> stream() {
    return sink.asFlux();
  }
}
