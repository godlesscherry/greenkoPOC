// path: server/src/main/java/com/greenko/windfarm/web/dto/MetricsResponse.java
package com.greenko.windfarm.web.dto;

import com.greenko.windfarm.model.MetricPoint;
import java.time.Instant;
import java.util.List;

public record MetricsResponse(
    Instant from, Instant to, String deviceId, String bucket, List<MetricPoint> points) {}
