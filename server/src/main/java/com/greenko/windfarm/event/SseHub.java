// path: server/src/main/java/com/greenko/windfarm/event/SseHub.java
package com.greenko.windfarm.event;

import com.greenko.windfarm.model.TelemetryRecord;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

@Component
public class SseHub {
  private static final Logger log = LoggerFactory.getLogger(SseHub.class);
  private final TelemetryEventBus eventBus;
  private final Map<SseEmitter, Optional<String>> emitters = new ConcurrentHashMap<>();
  private final ScheduledExecutorService heartbeatScheduler =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            Thread thread = new Thread(runnable, "sse-heartbeat");
            thread.setDaemon(true);
            return thread;
          });
  private Disposable subscription;

  public SseHub(TelemetryEventBus eventBus) {
    this.eventBus = eventBus;
  }

  @PostConstruct
  void start() {
    subscription =
        eventBus.stream()
            .subscribe(this::broadcast, error -> log.error("SSE hub encountered error", error));
    heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeats, 30, 30, TimeUnit.SECONDS);
  }

  public SseEmitter register(Optional<String> deviceId) {
    SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(
        () -> {
          emitters.remove(emitter);
          emitter.complete();
        });
    emitter.onError(
        ex -> {
          emitters.remove(emitter);
          log.debug("Removed errored emitter", ex);
        });
    emitters.put(emitter, deviceId.filter(id -> !id.isBlank()));
    try {
      emitter.send(SseEmitter.event().name("init").data("connected").reconnectTime(3000));
    } catch (IOException e) {
      log.debug("Failed to send initial SSE message", e);
    }
    return emitter;
  }

  private void broadcast(TelemetryRecord record) {
    emitters.entrySet().removeIf(entry -> {
      try {
        return !send(entry.getKey(), entry.getValue(), record);
      } catch (Exception e) {
        log.debug("Error during broadcast, removing emitter", e);
        // Don't try to complete emitters that are already in error state
        return true; // Remove this emitter
      }
    });
  }

  private boolean send(SseEmitter emitter, Optional<String> filter, TelemetryRecord record) {
    if (filter.isPresent() && !filter.get().equals(record.deviceId())) {
      return true;
    }
    try {
      emitter.send(
          SseEmitter.event()
              .name("telemetry")
              .data(record, MediaType.APPLICATION_JSON)
              .id(record.time().toString()));
      return true;
    } catch (IOException ex) {
      log.debug("Removing SSE emitter due to send failure", ex);
      // Don't try to complete - just remove from map
      return false;
    } catch (IllegalStateException ex) {
      log.debug("SSE emitter already in invalid state, removing", ex);
      return false;
    }
  }

  private void sendHeartbeats() {
    emitters.keySet().removeIf(emitter -> {
      try {
        emitter.send(SseEmitter.event().name("heartbeat").comment("keep-alive"));
        return false; // Keep this emitter
      } catch (IOException | IllegalStateException e) {
        log.debug("Removing disconnected emitter during heartbeat", e);
        // Don't try to complete - just remove from map
        return true; // Remove this emitter
      }
    });
  }

  @PreDestroy
  void shutdown() {
    if (subscription != null && !subscription.isDisposed()) {
      subscription.dispose();
    }
    heartbeatScheduler.shutdownNow();
    emitters.keySet().forEach(SseEmitter::complete);
    emitters.clear();
  }

  public int connectionCount() {
    return emitters.size();
  }
}
