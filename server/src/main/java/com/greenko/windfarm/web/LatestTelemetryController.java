// path: server/src/main/java/com/greenko/windfarm/web/LatestTelemetryController.java
package com.greenko.windfarm.web;

import com.greenko.windfarm.model.TelemetryRecord;
import com.greenko.windfarm.service.LatestTelemetryService;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/latest")
public class LatestTelemetryController {
  private final LatestTelemetryService latestTelemetryService;

  public LatestTelemetryController(LatestTelemetryService latestTelemetryService) {
    this.latestTelemetryService = latestTelemetryService;
  }

  @GetMapping
  public List<TelemetryRecord> latest(
      @RequestParam(name = "deviceId", required = false) String deviceId,
      @RequestParam(name = "limit", defaultValue = "50") int limit) {
    if (limit <= 0 || limit > 1000) {
      throw new IllegalArgumentException("limit must be between 1 and 1000");
    }
    return latestTelemetryService.fetch(
        Optional.ofNullable(deviceId).filter(id -> !id.isBlank()), limit);
  }
}
