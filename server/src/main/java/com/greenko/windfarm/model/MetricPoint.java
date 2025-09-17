// path: server/src/main/java/com/greenko/windfarm/model/MetricPoint.java
package com.greenko.windfarm.model;

import java.time.Instant;

public record MetricPoint(Instant bucketStart, double averagePowerKw, double totalEnergyKwh) {}
