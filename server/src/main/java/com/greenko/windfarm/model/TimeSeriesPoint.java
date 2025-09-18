// path: server/src/main/java/com/greenko/windfarm/model/TimeSeriesPoint.java
package com.greenko.windfarm.model;

import java.time.Instant;

public record TimeSeriesPoint(Instant time, double value) {}
