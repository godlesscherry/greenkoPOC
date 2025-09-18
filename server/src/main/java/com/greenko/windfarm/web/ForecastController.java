// path: server/src/main/java/com/greenko/windfarm/web/ForecastController.java
package com.greenko.windfarm.web;

import com.greenko.windfarm.service.ForecastService;
import com.greenko.windfarm.web.dto.ForecastResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {
  private final ForecastService forecastService;

  public ForecastController(ForecastService forecastService) {
    this.forecastService = forecastService;
  }

  @GetMapping
  public ForecastResponse forecast(
      @RequestParam(name = "deviceId", required = false) String deviceId,
      @RequestParam(name = "horizonMinutes", defaultValue = "60") int horizonMinutes,
      @RequestParam(name = "windowMinutes", defaultValue = "180") int windowMinutes,
      @RequestParam(name = "bucket", defaultValue = "PT1M") String bucketStr) {
    if (horizonMinutes <= 0) {
      throw new IllegalArgumentException("horizonMinutes must be positive");
    }
    if (windowMinutes <= 0) {
      throw new IllegalArgumentException("windowMinutes must be positive");
    }
    Duration horizon = Duration.ofMinutes(horizonMinutes);
    Duration window = Duration.ofMinutes(windowMinutes);
    Duration bucket = parseDuration(bucketStr);
    return forecastService.forecast(
        Optional.ofNullable(deviceId).filter(id -> !id.isBlank()), window, horizon, bucket);
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
