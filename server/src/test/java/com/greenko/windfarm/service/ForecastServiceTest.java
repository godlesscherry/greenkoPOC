// path: server/src/test/java/com/greenko/windfarm/service/ForecastServiceTest.java
package com.greenko.windfarm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.greenko.windfarm.model.TimeSeriesPoint;
import com.greenko.windfarm.repository.TelemetryRepository;
import com.greenko.windfarm.web.dto.ForecastResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForecastServiceTest {
  private FakeRepository repository;
  private ForecastService service;

  @BeforeEach
  void setUp() {
    repository = new FakeRepository();
    service = new ForecastService(repository);
  }

  @Test
  void computesForecastWithConfidenceInterval() {
    Instant start = Instant.parse("2025-01-01T00:00:00Z");
    for (int i = 0; i < 60; i++) {
      repository.addPoint(start.plusSeconds(60L * i), 100 + i);
    }

    ForecastResponse response =
        service.forecast(
            Optional.empty(), Duration.ofHours(1), Duration.ofMinutes(30), Duration.ofMinutes(1));

    assertThat(response.points()).hasSize(30);
    assertThat(response.points().get(0).predictedPowerKw()).isGreaterThan(100);
    assertThat(response.residualStdDev()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void requiresEnoughData() {
    repository.addPoint(Instant.parse("2025-01-01T00:00:00Z"), 100);
    repository.addPoint(Instant.parse("2025-01-01T00:01:00Z"), 101);

    assertThatThrownBy(
            () ->
                service.forecast(
                    Optional.empty(),
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static class FakeRepository implements TelemetryRepository {
    private final List<TimeSeriesPoint> points = new ArrayList<>();
    private Instant latest;

    void addPoint(Instant time, double value) {
      points.add(new TimeSeriesPoint(time, value));
      latest = time;
    }

    @Override
    public List<com.greenko.windfarm.model.MetricPoint> queryMetrics(
        Instant from, Instant to, Optional<String> deviceId, Duration bucket) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<com.greenko.windfarm.model.TelemetryRecord> findLatest(
        Optional<String> deviceId, int limit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<TimeSeriesPoint> loadPowerSeries(
        Instant from, Instant to, Optional<String> deviceId) {
      return points;
    }

    @Override
    public Optional<Instant> findMostRecentTimestamp() {
      return Optional.ofNullable(latest);
    }

    @Override
    public List<String> listDeviceIds() {
      return List.of();
    }
  }
}
