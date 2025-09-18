// path: server/src/main/java/com/greenko/windfarm/service/LatestTelemetryService.java
package com.greenko.windfarm.service;

import com.greenko.windfarm.model.TelemetryRecord;
import com.greenko.windfarm.repository.TelemetryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LatestTelemetryService {
  private final TelemetryRepository repository;

  public LatestTelemetryService(TelemetryRepository repository) {
    this.repository = repository;
  }

  public List<TelemetryRecord> fetch(Optional<String> deviceId, int limit) {
    return repository.findLatest(deviceId, limit);
  }
}
