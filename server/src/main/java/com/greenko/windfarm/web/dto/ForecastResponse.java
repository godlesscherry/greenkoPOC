// path: server/src/main/java/com/greenko/windfarm/web/dto/ForecastResponse.java
package com.greenko.windfarm.web.dto;

import com.greenko.windfarm.model.ForecastPoint;
import java.time.Instant;
import java.util.List;

public record ForecastResponse(
    String deviceId,
    Instant windowStart,
    Instant windowEnd,
    Instant forecastStart,
    int horizonMinutes,
    double rollingMeanKw,
    double trendSlopePerMinute,
    double trendIntercept,
    double residualStdDev,
    List<ForecastPoint> points) {}
