import type {
  DeviceListResponse,
  ForecastResponse,
  MetricsResponse,
  TelemetryRecord
} from './types';

const RAW_BASE = import.meta.env.VITE_API_BASE_URL as string | undefined;
const NORMALISED_BASE = RAW_BASE ? RAW_BASE.replace(/\/$/, '') : undefined;

function buildUrl(path: string): URL {
  if (NORMALISED_BASE) {
    return new URL(path, NORMALISED_BASE);
  }
  return new URL(path, window.location.origin);
}

export function resolveApiUrl(path: string): string {
  return buildUrl(path).toString();
}

async function handleResponse<T>(resp: Response): Promise<T> {
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(text || resp.statusText);
  }
  return resp.json() as Promise<T>;
}

export async function fetchMetrics(params: {
  from: string;
  to: string;
  deviceId?: string;
  bucket: string;
}): Promise<MetricsResponse> {
  const url = buildUrl('/api/metrics');
  url.searchParams.set('from', params.from);
  url.searchParams.set('to', params.to);
  url.searchParams.set('bucket', params.bucket);
  if (params.deviceId && params.deviceId !== 'ALL') {
    url.searchParams.set('deviceId', params.deviceId);
  }
  const resp = await fetch(url.toString());
  return handleResponse<MetricsResponse>(resp);
}

export async function fetchForecast(params: {
  deviceId?: string;
  horizonMinutes: number;
  windowMinutes: number;
  bucket: string;
}): Promise<ForecastResponse> {
  const url = buildUrl('/api/forecast');
  url.searchParams.set('horizonMinutes', params.horizonMinutes.toString());
  url.searchParams.set('windowMinutes', params.windowMinutes.toString());
  url.searchParams.set('bucket', params.bucket);
  if (params.deviceId && params.deviceId !== 'ALL') {
    url.searchParams.set('deviceId', params.deviceId);
  }
  const resp = await fetch(url.toString());
  return handleResponse<ForecastResponse>(resp);
}

export async function fetchDevices(): Promise<string[]> {
  const resp = await fetch(resolveApiUrl('/api/devices'));
  const json = await handleResponse<DeviceListResponse>(resp);
  return json.devices;
}

export async function fetchLatest(deviceId?: string, limit = 50): Promise<TelemetryRecord[]> {
  const url = buildUrl('/api/latest');
  url.searchParams.set('limit', limit.toString());
  if (deviceId && deviceId !== 'ALL') {
    url.searchParams.set('deviceId', deviceId);
  }
  const resp = await fetch(url.toString());
  return handleResponse<TelemetryRecord[]>(resp);
}
