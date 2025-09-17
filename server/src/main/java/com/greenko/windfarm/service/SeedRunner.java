// path: server/src/main/java/com/greenko/windfarm/service/SeedRunner.java
package com.greenko.windfarm.service;

import com.greenko.windfarm.config.WindfarmProperties;
import com.greenko.windfarm.csv.DatasetLoader;
import com.greenko.windfarm.model.TelemetryRecord;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "windfarm.seed", name = "enabled", havingValue = "true")
public class SeedRunner implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);
  private final DatasetLoader datasetLoader;
  private final WindfarmProperties properties;
  private final JdbcTemplate jdbcTemplate;

  public SeedRunner(
      DatasetLoader datasetLoader, WindfarmProperties properties, JdbcTemplate jdbcTemplate) {
    this.datasetLoader = datasetLoader;
    this.properties = properties;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(String... args) {
    int minutes = properties.getSeed().getMinutes();
    List<TelemetryRecord> records = datasetLoader.loadDataset().records();
    if (records.isEmpty()) {
      log.warn("No records available for seeding");
      return;
    }
    int limit = Math.min(minutes * 100, records.size());
    List<TelemetryRecord> subset = records.subList(0, limit);
    jdbcTemplate.batchUpdate(
        "INSERT INTO telemetry(time, device_id, energy_kwh) VALUES (?, ?, ?) "
            + "ON CONFLICT (time, device_id) DO NOTHING",
        subset,
        subset.size(),
        (ps, record) -> {
          ps.setObject(1, record.time());
          ps.setString(2, record.deviceId());
          ps.setDouble(3, record.energyKwh());
        });
    log.info("Seeded {} telemetry records", subset.size());
  }
}
