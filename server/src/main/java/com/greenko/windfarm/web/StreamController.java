// path: server/src/main/java/com/greenko/windfarm/web/StreamController.java
package com.greenko.windfarm.web;

import com.greenko.windfarm.event.SseHub;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
public class StreamController {
  private final SseHub hub;

  public StreamController(SseHub hub) {
    this.hub = hub;
  }

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@RequestParam(name = "deviceId", required = false) String deviceId) {
    return hub.register(Optional.ofNullable(deviceId).filter(id -> !id.isBlank()));
  }
}
