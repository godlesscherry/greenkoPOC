export interface TelemetryRecord {
  time: string;
  deviceId: string;
  energyKwh: number;
  powerKw: number;
}

export interface MetricPoint {
  bucketStart: string;
  averagePowerKw: number;
  totalEnergyKwh: number;
}

export interface MetricsResponse {
  from: string;
  to: string;
  deviceId: string;
  bucket: string;
  points: MetricPoint[];
}

export interface ForecastPoint {
  time: string;
  predictedPowerKw: number;
  lowerBoundKw: number;
  upperBoundKw: number;
}

export interface ForecastResponse {
  deviceId: string;
  windowStart: string;
  windowEnd: string;
  forecastStart: string;
  horizonMinutes: number;
  rollingMeanKw: number;
  trendSlopePerMinute: number;
  trendIntercept: number;
  residualStdDev: number;
  points: ForecastPoint[];
}

export interface DeviceListResponse {
  devices: string[];
}
