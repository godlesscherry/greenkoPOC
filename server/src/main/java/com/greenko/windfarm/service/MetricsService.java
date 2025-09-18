// path: server/src/main/java/com/greenko/windfarm/service/MetricsService.java
package com.greenko.windfarm.service;

import com.greenko.windfarm.model.MetricPoint;
import com.greenko.windfarm.repository.TelemetryRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {
  private final TelemetryRepository telemetryRepository;

  public MetricsService(TelemetryRepository telemetryRepository) {
    this.telemetryRepository = telemetryRepository;
  }

  public List<MetricPoint> loadMetrics(
      Instant from, Instant to, Optional<String> deviceId, Duration bucket) {
    if (from.isAfter(to)) {
      throw new IllegalArgumentException("from must be before to");
    }
    return telemetryRepository.queryMetrics(from, to, deviceId, bucket);
  }
}
