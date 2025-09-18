// path: server/src/main/java/com/greenko/windfarm/csv/DatasetLoader.java
package com.greenko.windfarm.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.greenko.windfarm.config.WindfarmProperties;
import com.greenko.windfarm.model.TelemetryRecord;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatasetLoader {
  private static final Logger log = LoggerFactory.getLogger(DatasetLoader.class);
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.ENGLISH);

  private final Path datasetPath;
  private volatile DatasetSnapshot cached;

  public DatasetLoader(WindfarmProperties properties) {
    Objects.requireNonNull(properties, "properties");
    this.datasetPath = properties.getDatasetPath();
  }

  public synchronized DatasetSnapshot loadDataset() {
    if (cached != null) {
      return cached;
    }
    if (!Files.exists(datasetPath)) {
      throw new IllegalStateException("Dataset not found at " + datasetPath.toAbsolutePath());
    }

    CsvMapper mapper = new CsvMapper();
    CsvSchema schema =
        CsvSchema.builder()
            .addColumn("timestamp")
            .addColumn("device_id")
            .addColumn("energy_produced_kwh")
            .setUseHeader(true)
            .setColumnSeparator(',')
            .build();

    Map<Key, DoubleAdder> accumulator = new ConcurrentHashMap<>();
    int rawRows = 0;
    try (Reader reader = Files.newBufferedReader(datasetPath)) {
      MappingIterator<RawRecord> iterator =
          mapper.readerFor(RawRecord.class).with(schema).readValues(reader);
      while (iterator.hasNext()) {
        RawRecord row = iterator.next();
        rawRows++;
        if (row.timestamp() == null || row.deviceId() == null || row.energyKwh() == null) {
          log.warn("Skipping malformed row: {}", row);
          continue;
        }
        Instant time =
            LocalDateTime.parse(row.timestamp().trim(), FORMATTER)
                .atOffset(ZoneOffset.UTC)
                .toInstant();
        String deviceId = row.deviceId().trim();
        double energy = row.energyKwh();
        if (!Double.isFinite(energy)) {
          log.warn("Skipping non-finite energy value {} for {} at {}", energy, deviceId, time);
          continue;
        }
        accumulator.computeIfAbsent(new Key(time, deviceId), __ -> new DoubleAdder()).add(energy);
      }
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read dataset: " + datasetPath, ex);
    }

    List<TelemetryRecord> normalized = new ArrayList<>(accumulator.size());
    accumulator.forEach(
        (key, adder) ->
            normalized.add(
                new TelemetryRecord(key.time(), key.deviceId(), adder.sum(), adder.sum() * 60d)));
    normalized.sort(
        Comparator.comparing(TelemetryRecord::time).thenComparing(TelemetryRecord::deviceId));

    if (normalized.isEmpty()) {
      throw new IllegalStateException("Dataset " + datasetPath + " produced no telemetry rows");
    }

    DoubleSummaryStatistics stats =
        normalized.stream().collect(Collectors.summarizingDouble(TelemetryRecord::powerKw));

    DatasetSnapshot snapshot =
        new DatasetSnapshot(
            normalized,
            normalized.get(0).time(),
            normalized.get(normalized.size() - 1).time(),
            rawRows,
            stats);
    this.cached = snapshot;
    log.info(
        "Loaded dataset {} (raw rows: {}, normalized rows: {})",
        datasetPath.toAbsolutePath(),
        rawRows,
        normalized.size());
    return snapshot;
  }

  record RawRecord(
      @JsonProperty("timestamp") String timestamp,
      @JsonProperty("device_id") String deviceId,
      @JsonProperty("energy_produced_kwh") Double energyKwh) {}

  record Key(Instant time, String deviceId) {}

  public record DatasetSnapshot(
      List<TelemetryRecord> records,
      Instant start,
      Instant end,
      int rawRowCount,
      DoubleSummaryStatistics powerStatistics) {
    public long durationMinutes() {
      return (end.toEpochMilli() - start.toEpochMilli()) / 60000L + 1L;
    }
  }
}
