// path: server/src/main/java/com/greenko/windfarm/model/TelemetryRecord.java
package com.greenko.windfarm.model;

import java.time.Instant;

public record TelemetryRecord(Instant time, String deviceId, double energyKwh, double powerKw) {
  public TelemetryRecord withTime(Instant updated) {
    return new TelemetryRecord(updated, deviceId, energyKwh, powerKw);
  }
}
