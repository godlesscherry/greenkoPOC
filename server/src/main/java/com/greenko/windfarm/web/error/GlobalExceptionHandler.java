// path: server/src/main/java/com/greenko/windfarm/web/error/GlobalExceptionHandler.java
package com.greenko.windfarm.web.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@ControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetails> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    return problem(HttpStatus.BAD_REQUEST, ex, request);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetails> handleIllegalState(
      IllegalStateException ex, HttpServletRequest request) {
    return problem(HttpStatus.CONFLICT, ex, request);
  }

  @ExceptionHandler(AsyncRequestNotUsableException.class)
  public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex, HttpServletRequest request) {
    // Log at debug level - this is expected for SSE disconnections
    log.debug("Async request not usable (likely SSE disconnection): {}", ex.getMessage());
    // Don't try to return a response for SSE endpoints - just let it complete
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetails> handleGeneric(Exception ex, HttpServletRequest request) {
    // Skip error responses for SSE endpoints to avoid converter issues
    String contentType = request.getHeader("Accept");
    if (contentType != null && contentType.contains("text/event-stream")) {
      log.debug("Skipping error response for SSE endpoint: {}", ex.getMessage());
      return null; // Let Spring handle it gracefully
    }

    log.error("Unhandled error", ex);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
  }

  private ResponseEntity<ProblemDetails> problem(
      HttpStatus status, Exception exception, HttpServletRequest request) {
    // Skip error responses for SSE endpoints to avoid converter issues
    String contentType = request.getHeader("Accept");
    if (contentType != null && contentType.contains("text/event-stream")) {
      log.debug("Skipping error response for SSE endpoint: {}", exception.getMessage());
      return null;
    }

    ProblemDetails details =
        new ProblemDetails(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            exception.getMessage(),
            request.getRequestURI());
    return ResponseEntity.status(status).body(details);
  }
}
