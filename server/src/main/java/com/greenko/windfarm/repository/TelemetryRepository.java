// path: server/src/main/java/com/greenko/windfarm/repository/TelemetryRepository.java
package com.greenko.windfarm.repository;

import com.greenko.windfarm.model.MetricPoint;
import com.greenko.windfarm.model.TelemetryRecord;
import com.greenko.windfarm.model.TimeSeriesPoint;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TelemetryRepository {
  List<MetricPoint> queryMetrics(
      Instant from, Instant to, Optional<String> deviceId, Duration bucket);

  List<TelemetryRecord> findLatest(Optional<String> deviceId, int limit);

  List<TimeSeriesPoint> loadPowerSeries(Instant from, Instant to, Optional<String> deviceId);

  Optional<Instant> findMostRecentTimestamp();

  List<String> listDeviceIds();
}
