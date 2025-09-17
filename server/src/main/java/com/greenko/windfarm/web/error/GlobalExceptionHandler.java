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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetails> handleGeneric(Exception ex, HttpServletRequest request) {
    log.error("Unhandled error", ex);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
  }

  private ResponseEntity<ProblemDetails> problem(
      HttpStatus status, Exception exception, HttpServletRequest request) {
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
