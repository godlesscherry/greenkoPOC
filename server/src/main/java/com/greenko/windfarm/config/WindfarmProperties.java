// path: server/src/main/java/com/greenko/windfarm/config/WindfarmProperties.java
package com.greenko.windfarm.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "windfarm")
public class WindfarmProperties {
  private Path datasetPath = Paths.get("data/device_energy_data.csv");
  private final Emitter emitter = new Emitter();
  private final Listener listener = new Listener();
  private final Seed seed = new Seed();

  public Path getDatasetPath() {
    return datasetPath;
  }

  public void setDatasetPath(Path datasetPath) {
    this.datasetPath = datasetPath;
  }

  public Emitter getEmitter() {
    return emitter;
  }

  public Listener getListener() {
    return listener;
  }

  public Seed getSeed() {
    return seed;
  }

  public static class Emitter {
    private boolean enabled = true;
    private Duration interval = Duration.ofMinutes(1);
    private Duration acceleratedInterval = Duration.ofSeconds(1);
    private boolean accelerate = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getInterval() {
      return interval;
    }

    public void setInterval(Duration interval) {
      this.interval = interval;
    }

    public Duration getAcceleratedInterval() {
      return acceleratedInterval;
    }

    public void setAcceleratedInterval(Duration acceleratedInterval) {
      this.acceleratedInterval = acceleratedInterval;
    }

    public boolean isAccelerate() {
      return accelerate;
    }

    public void setAccelerate(boolean accelerate) {
      this.accelerate = accelerate;
    }
  }

  public static class Listener {
    private boolean enabled = true;
    private Duration flushInterval = Duration.ofMillis(200);
    private int batchSize = 500;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getFlushInterval() {
      return flushInterval;
    }

    public void setFlushInterval(Duration flushInterval) {
      this.flushInterval = flushInterval;
    }

    public int getBatchSize() {
      return batchSize;
    }

    public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
    }
  }

  public static class Seed {
    private boolean enabled = false;
    private int minutes = 180;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getMinutes() {
      return minutes;
    }

    public void setMinutes(int minutes) {
      this.minutes = minutes;
    }
  }
}
