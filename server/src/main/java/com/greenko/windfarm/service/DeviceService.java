// path: server/src/main/java/com/greenko/windfarm/service/DeviceService.java
package com.greenko.windfarm.service;

import com.greenko.windfarm.repository.TelemetryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {
  private final TelemetryRepository repository;

  public DeviceService(TelemetryRepository repository) {
    this.repository = repository;
  }

  public List<String> listDevices() {
    return repository.listDeviceIds();
  }
}
