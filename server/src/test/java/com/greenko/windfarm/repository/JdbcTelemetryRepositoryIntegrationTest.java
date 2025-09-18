// path:
// server/src/test/java/com/greenko/windfarm/repository/JdbcTelemetryRepositoryIntegrationTest.java
package com.greenko.windfarm.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.greenko.windfarm.model.MetricPoint;
import com.greenko.windfarm.model.TimeSeriesPoint;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(SpringExtension.class)
class JdbcTelemetryRepositoryIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("timescale/timescaledb:latest-pg15")
          .withDatabaseName("windfarm")
          .withUsername("postgres")
          .withPassword("postgres");

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private JdbcTelemetryRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Flyway flyway;

  @BeforeEach
  void setUp() {
    flyway.clean();
    flyway.migrate();
    insertSampleData();
  }

  @Test
  void aggregatesMetricsByBucket() {
    List<MetricPoint> metrics =
        repository.queryMetrics(
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-01-01T00:10:00Z"),
            Optional.empty(),
            Duration.ofMinutes(5));

    assertThat(metrics).isNotEmpty();
    assertThat(metrics.get(0).totalEnergyKwh()).isGreaterThan(0);
  }

  @Test
  void loadsSeriesForForecast() {
    List<TimeSeriesPoint> series =
        repository.loadPowerSeries(
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-01-01T00:30:00Z"),
            Optional.of("Device_1"));
    assertThat(series).isNotEmpty();
  }

  @Test
  void loadsAggregatedSeriesAcrossDevices() {
    List<TimeSeriesPoint> series =
        repository.loadPowerSeries(
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-01-01T00:30:00Z"),
            Optional.empty());

    assertThat(series).isNotEmpty();
    assertThat(series.get(0).value()).isGreaterThan(0);
  }

  private void insertSampleData() {
    Instant start = Instant.parse("2025-01-01T00:00:00Z");
    for (int minute = 0; minute < 30; minute++) {
      Instant time = start.plusSeconds(60L * minute);
      jdbcTemplate.update(
          "INSERT INTO telemetry(time, device_id, energy_kwh) VALUES (?, ?, ?)"
              + " ON CONFLICT DO NOTHING",
          time,
          "Device_" + (minute % 2 + 1),
          1.0 + minute * 0.01);
    }
  }
}
