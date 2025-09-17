// path: server/src/main/java/com/greenko/windfarm/web/error/ProblemDetails.java
package com.greenko.windfarm.web.error;

import java.time.Instant;

public record ProblemDetails(
    Instant timestamp, int status, String error, String message, String path) {}
