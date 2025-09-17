// path: server/src/main/java/com/greenko/windfarm/web/MetricsController.java
package com.greenko.windfarm.web;

import com.greenko.windfarm.model.MetricPoint;
import com.greenko.windfarm.service.MetricsService;
import com.greenko.windfarm.web.dto.MetricsResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@Validated
public class MetricsController {
  private final MetricsService metricsService;

  public MetricsController(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @GetMapping
  public MetricsResponse metrics(
      @RequestParam(name = "from", required = false) String fromStr,
      @RequestParam(name = "to", required = false) String toStr,
      @RequestParam(name = "deviceId", required = false) String deviceId,
      @RequestParam(name = "bucket", defaultValue = "PT1M") String bucketStr) {
    Instant to = toStr != null ? parseInstant(toStr) : Instant.now();
    Instant from = fromStr != null ? parseInstant(fromStr) : to.minus(Duration.ofHours(6));
    Duration bucket = parseDuration(bucketStr);
    List<MetricPoint> points =
        metricsService.loadMetrics(
            from, to, Optional.ofNullable(deviceId).filter(id -> !id.isBlank()), bucket);
    return new MetricsResponse(from, to, deviceId == null ? "ALL" : deviceId, bucketStr, points);
  }

  private Instant parseInstant(String value) {
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException("Invalid instant: " + value, ex);
    }
  }

  private Duration parseDuration(String value) {
    try {
      if (value.toLowerCase().endsWith("minute") || value.toLowerCase().endsWith("minutes")) {
        String digits = value.split(" ")[0];
        return Duration.ofMinutes(Long.parseLong(digits));
      }
      return Duration.parse(value);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid bucket duration: " + value, ex);
    }
  }
}
