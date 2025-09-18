// path: server/src/main/java/com/greenko/windfarm/service/ForecastService.java
package com.greenko.windfarm.service;

import com.greenko.windfarm.model.ForecastPoint;
import com.greenko.windfarm.model.TimeSeriesPoint;
import com.greenko.windfarm.repository.TelemetryRepository;
import com.greenko.windfarm.web.dto.ForecastResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

@Service
public class ForecastService {
  private static final double Z_80 = 1.28155; // 80% confidence interval z-score
  private final TelemetryRepository repository;

  public ForecastService(TelemetryRepository repository) {
    this.repository = repository;
  }

  public ForecastResponse forecast(
      Optional<String> deviceId, Duration window, Duration horizon, Duration bucket) {
    if (horizon.isNegative() || horizon.isZero()) {
      throw new IllegalArgumentException("horizon must be positive");
    }
    if (window.isNegative() || window.isZero()) {
      throw new IllegalArgumentException("window must be positive");
    }
    Instant now =
        repository
            .findMostRecentTimestamp()
            .orElseThrow(() -> new IllegalStateException("No telemetry available"));
    Instant windowStart = now.minus(window);
    List<TimeSeriesPoint> history = repository.loadPowerSeries(windowStart, now, deviceId);
    if (history.size() < 5) {
      throw new IllegalArgumentException("Insufficient data to forecast; need at least 5 points");
    }

    int rollingWindow = Math.min(15, history.size());
    double rollingMean =
        history.subList(history.size() - rollingWindow, history.size()).stream()
            .mapToDouble(TimeSeriesPoint::value)
            .average()
            .orElse(0.0);

    SimpleRegression regression = new SimpleRegression(true);
    for (int i = 0; i < history.size(); i++) {
      regression.addData(i, history.get(i).value());
    }
    double slope = regression.getSlope();
    double intercept = regression.getIntercept();

    double residualSum = 0d;
    for (int i = 0; i < history.size(); i++) {
      double expected = intercept + slope * i;
      double residual = history.get(i).value() - expected;
      residualSum += residual * residual;
    }
    double residualStd = Math.sqrt(residualSum / Math.max(1, history.size() - 2));

    List<ForecastPoint> points = new ArrayList<>();
    long bucketSeconds = Math.max(60, bucket.toSeconds());
    long steps = horizon.getSeconds() / bucketSeconds;
    if (steps <= 0) {
      steps = Math.max(1, horizon.toSeconds() / bucketSeconds);
    }
    Instant forecastStart = now.plusSeconds(bucketSeconds);
    for (int i = 1; i <= steps; i++) {
      double regressionValue = intercept + slope * (history.size() + i - 1);
      double blended = 0.5 * regressionValue + 0.5 * rollingMean;
      double lower = Math.max(0d, blended - Z_80 * residualStd);
      double upper = blended + Z_80 * residualStd;
      points.add(
          new ForecastPoint(
              forecastStart.plusSeconds(bucketSeconds * (i - 1)), blended, lower, upper));
    }

    return new ForecastResponse(
        deviceId.orElse("ALL"),
        windowStart,
        now,
        forecastStart,
        (int) steps,
        rollingMean,
        slope,
        intercept,
        residualStd,
        points);
  }
}
