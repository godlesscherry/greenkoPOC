// path: server/src/main/java/com/greenko/windfarm/web/DeviceController.java
package com.greenko.windfarm.web;

import com.greenko.windfarm.service.DeviceService;
import com.greenko.windfarm.web.dto.DeviceListResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {
  private final DeviceService deviceService;

  public DeviceController(DeviceService deviceService) {
    this.deviceService = deviceService;
  }

  @GetMapping
  public DeviceListResponse list() {
    return new DeviceListResponse(deviceService.listDevices());
  }
}
