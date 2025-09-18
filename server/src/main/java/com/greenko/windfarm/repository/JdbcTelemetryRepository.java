// path: server/src/main/java/com/greenko/windfarm/repository/JdbcTelemetryRepository.java
package com.greenko.windfarm.repository;

import com.greenko.windfarm.model.MetricPoint;
import com.greenko.windfarm.model.TelemetryRecord;
import com.greenko.windfarm.model.TimeSeriesPoint;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTelemetryRepository implements TelemetryRepository {
  private static final Logger log = LoggerFactory.getLogger(JdbcTelemetryRepository.class);
  private final JdbcTemplate jdbcTemplate;

  public JdbcTelemetryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<MetricPoint> queryMetrics(
          Instant from, Instant to, Optional<String> deviceId, Duration bucket) {
    String intervalLiteral = intervalLiteral(bucket);
    StringBuilder sql =
            new StringBuilder(
                    "SELECT time_bucket('"
                            + intervalLiteral
                            + "', time) AS bucket_start, "
                            + "AVG(power_kw) AS avg_power, "
                            + "SUM(energy_kwh) AS total_energy "
                            + "FROM telemetry WHERE time >= ? AND time <= ?");
    if (deviceId.isPresent()) {
      sql.append(" AND device_id = ?");
    }
    sql.append(" GROUP BY bucket_start ORDER BY bucket_start");

    Object[] params;
    if (deviceId.isPresent()) {
      params = new Object[] {
          java.sql.Timestamp.from(from),
          java.sql.Timestamp.from(to),
          deviceId.get()
      };
    } else {
      params = new Object[] {
          java.sql.Timestamp.from(from),
          java.sql.Timestamp.from(to)
      };
    }

    return jdbcTemplate.query(
            sql.toString(),
            params,
            (rs, rowNum) ->
                    new MetricPoint(
                            rs.getTimestamp("bucket_start").toInstant(),
                            rs.getDouble("avg_power"),
                            rs.getDouble("total_energy")));
  }

  @Override
  public List<TelemetryRecord> findLatest(Optional<String> deviceId, int limit) {
    StringBuilder sql =
            new StringBuilder("SELECT time, device_id, energy_kwh, power_kw FROM telemetry WHERE 1=1");
    if (deviceId.isPresent()) {
      sql.append(" AND device_id = ?");
    }
    sql.append(" ORDER BY time DESC LIMIT ?");

    Object[] params;
    if (deviceId.isPresent()) {
      params = new Object[] {deviceId.get(), limit};
    } else {
      params = new Object[] {limit};
    }

    RowMapper<TelemetryRecord> mapper =
            (rs, rowNum) ->
                    new TelemetryRecord(
                            rs.getTimestamp("time").toInstant(),
                            rs.getString("device_id"),
                            rs.getDouble("energy_kwh"),
                            rs.getDouble("power_kw"));

    List<TelemetryRecord> results = jdbcTemplate.query(sql.toString(), params, mapper);
    return results.stream()
            .sorted((a, b) -> a.time().compareTo(b.time()))
            .collect(Collectors.toList());
  }

  @Override
  public List<TimeSeriesPoint> loadPowerSeries(
          Instant from, Instant to, Optional<String> deviceId) {
    StringBuilder sql = new StringBuilder("SELECT time, ");
    if (deviceId.isPresent()) {
      sql.append(
              "power_kw AS power FROM telemetry WHERE device_id = ? AND time >= ? AND time <= ? ORDER BY time");
      return jdbcTemplate.query(
              sql.toString(),
              new Object[] {
                  deviceId.get(),
                  java.sql.Timestamp.from(from),
                  java.sql.Timestamp.from(to)
              },
              this::mapTimeSeriesPoint);
    }
    sql.append(
            "SUM(power_kw) AS power FROM telemetry WHERE time >= ? AND time <= ? GROUP BY time ORDER BY time");
    return jdbcTemplate.query(
            sql.toString(),
            new Object[] {
                java.sql.Timestamp.from(from),
                java.sql.Timestamp.from(to)
            },
            this::mapTimeSeriesPoint);
  }

  @Override
  public Optional<Instant> findMostRecentTimestamp() {
    return jdbcTemplate.query(
            "SELECT time FROM telemetry ORDER BY time DESC LIMIT 1",
            rs -> rs.next() ? Optional.of(rs.getTimestamp("time").toInstant()) : Optional.empty());
  }

  @Override
  public List<String> listDeviceIds() {
    return jdbcTemplate.query(
            "SELECT DISTINCT device_id FROM telemetry ORDER BY device_id",
            (rs, rowNum) -> rs.getString("device_id"));
  }

  private String intervalLiteral(Duration bucket) {
    long seconds = bucket.getSeconds();
    if (seconds <= 0) {
      throw new IllegalArgumentException("Bucket must be positive");
    }
    long abs = Math.abs(seconds);
    long days = abs / 86400;
    long hours = (abs % 86400) / 3600;
    long minutes = (abs % 3600) / 60;
    long secs = abs % 60;
    StringBuilder builder = new StringBuilder();
    if (days > 0) {
      builder.append(days).append(days == 1 ? " day" : " days");
    }
    if (hours > 0) {
      if (builder.length() > 0) {
        builder.append(' ');
      }
      builder.append(hours).append(hours == 1 ? " hour" : " hours");
    }
    if (minutes > 0) {
      if (builder.length() > 0) {
        builder.append(' ');
      }
      builder.append(minutes).append(minutes == 1 ? " minute" : " minutes");
    }
    if (secs > 0) {
      if (builder.length() > 0) {
        builder.append(' ');
      }
      builder.append(secs).append(secs == 1 ? " second" : " seconds");
    }
    return builder.toString();
  }

  private TimeSeriesPoint mapTimeSeriesPoint(ResultSet rs, int rowNum) throws SQLException {
    return new TimeSeriesPoint(rs.getTimestamp("time").toInstant(), rs.getDouble("power"));
  }
}