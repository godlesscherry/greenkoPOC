// path: server/src/test/java/com/greenko/windfarm/csv/DatasetLoaderTest.java
package com.greenko.windfarm.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.greenko.windfarm.config.WindfarmProperties;
import com.greenko.windfarm.csv.DatasetLoader.DatasetSnapshot;
import com.greenko.windfarm.model.TelemetryRecord;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class DatasetLoaderTest {
  @Test
  void aggregatesDuplicateRows() {
    WindfarmProperties properties = new WindfarmProperties();
    properties.setDatasetPath(Paths.get("src/test/resources/test-data/sample.csv"));
    DatasetLoader loader = new DatasetLoader(properties);

    DatasetSnapshot snapshot = loader.loadDataset();

    assertThat(snapshot.records()).hasSize(5);
    TelemetryRecord first = snapshot.records().get(0);
    assertThat(first.energyKwh()).isEqualTo(1.5);
    assertThat(first.powerKw()).isEqualTo(90.0);
  }
}
