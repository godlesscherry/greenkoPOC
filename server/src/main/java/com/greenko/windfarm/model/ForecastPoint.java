// path: server/src/main/java/com/greenko/windfarm/model/ForecastPoint.java
package com.greenko.windfarm.model;

import java.time.Instant;

public record ForecastPoint(
    Instant time, double predictedPowerKw, double lowerBoundKw, double upperBoundKw) {}
