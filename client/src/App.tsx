import { useCallback, useEffect, useMemo, useState } from 'react';
import dayjs from 'dayjs';
import { fetchDevices, fetchForecast, fetchLatest, fetchMetrics } from './api/client';
import type { ForecastPoint, MetricPoint, TelemetryRecord } from './api/types';
import { useTelemetryStream } from './hooks/useTelemetryStream';
import { KpiCard } from './components/KpiCard';
import { PowerChart } from './components/PowerChart';
import { LatestEventsTable } from './components/LatestEventsTable';

const RANGE_OPTIONS = [
  { label: 'Last 1 hour', minutes: 60 },
  { label: 'Last 6 hours', minutes: 360 },
  { label: 'Last 24 hours', minutes: 1440 }
];

export default function App() {
  const [range, setRange] = useState(RANGE_OPTIONS[0]);
  const [deviceId, setDeviceId] = useState('ALL');
  const [devices, setDevices] = useState<string[]>([]);
  const [metrics, setMetrics] = useState<MetricPoint[]>([]);
  const [forecast, setForecast] = useState<ForecastPoint[]>([]);
  const [latestEvents, setLatestEvents] = useState<TelemetryRecord[]>([]);
  const [live, setLive] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = 'Windfarm Control Room';
  }, []);

  useEffect(() => {
    fetchDevices()
      .then((ids) => setDevices(ids))
      .catch((err) => console.error('Failed to load devices', err));
  }, []);

  const loadMetrics = useCallback(async () => {
    const now = dayjs();
    const to = now.toISOString();
    const from = now.subtract(range.minutes, 'minute').toISOString();
    try {
      const response = await fetchMetrics({
        from,
        to,
        deviceId,
        bucket: 'PT1M'
      });
      setMetrics(response.points);
      setError(null);
    } catch (err) {
      console.error(err);
      setError('Failed to load metrics');
    }
  }, [range.minutes, deviceId]);

  const loadForecast = useCallback(async () => {
    try {
      const response = await fetchForecast({
        deviceId,
        horizonMinutes: 60,
        windowMinutes: Math.max(180, range.minutes),
        bucket: 'PT1M'
      });
      setForecast(response.points);
      setError(null);
    } catch (err) {
      console.error(err);
      setError('Failed to compute forecast');
    }
  }, [deviceId, range.minutes]);

  const loadLatest = useCallback(async () => {
    try {
      const events = await fetchLatest(deviceId, 50);
      const sorted = events.sort(
        (a, b) => new Date(a.time).getTime() - new Date(b.time).getTime()
      );
      setLatestEvents(sorted);
    } catch (err) {
      console.error(err);
    }
  }, [deviceId]);

  useEffect(() => {
    loadMetrics();
    loadForecast();
    loadLatest();
  }, [loadMetrics, loadForecast, loadLatest]);

  const handleIncoming = useCallback(
    (record: TelemetryRecord) => {
      setLatestEvents((prev) => {
        const filtered = prev.filter(
          (item) => !(item.time === record.time && item.deviceId === record.deviceId)
        );
        const next = [...filtered, record];
        next.sort((a, b) => new Date(a.time).getTime() - new Date(b.time).getTime());
        return next.slice(-50);
      });
      if (live) {
        loadMetrics();
        loadForecast();
      }
    },
    [live, loadMetrics, loadForecast]
  );

  useTelemetryStream(deviceId, live, handleIncoming);

  const currentPower = useMemo(() => {
    const last = metrics[metrics.length - 1];
    return last ? last.averagePowerKw : 0;
  }, [metrics]);

  const totalEnergyMWh = useMemo(() => {
    const sum = metrics.reduce((acc, point) => acc + point.totalEnergyKwh, 0);
    return sum / 1000;
  }, [metrics]);

  const maxPower = useMemo(() => {
    return metrics.reduce((acc, point) => Math.max(acc, point.averagePowerKw), 0);
  }, [metrics]);

  const forecastFirst = forecast[0]?.predictedPowerKw ?? 0;

  return (
    <div className="app-container">
      <header className="controls">
        <div className="control-group">
          {RANGE_OPTIONS.map((option) => (
            <button
              key={option.label}
              className={option.label === range.label ? 'active' : ''}
              onClick={() => setRange(option)}
            >
              {option.label}
            </button>
          ))}
        </div>
        <div className="control-group">
          <label htmlFor="device-select">Device</label>
          <select
            id="device-select"
            value={deviceId}
            onChange={(event) => setDeviceId(event.target.value)}
          >
            <option value="ALL">All turbines</option>
            {devices.map((id) => (
              <option key={id} value={id}>
                {id}
              </option>
            ))}
          </select>
          <button className={live ? 'active' : ''} onClick={() => setLive((prev) => !prev)}>
            {live ? 'Live On' : 'Live Off'}
          </button>
        </div>
      </header>

      {error ? <div className="card">{error}</div> : null}

      <section className="card-grid">
        <KpiCard title="Current Power" value={`${currentPower.toFixed(1)} kW`} />
        <KpiCard title="Energy (range)" value={`${totalEnergyMWh.toFixed(2)} MWh`} />
        <KpiCard title="Peak Power" value={`${maxPower.toFixed(1)} kW`} />
        <KpiCard title="Next Forecast" value={`${forecastFirst.toFixed(1)} kW`} />
      </section>

      <section className="chart-wrapper">
        <h3>Power Output & Forecast</h3>
        <PowerChart metrics={metrics} forecast={forecast} />
      </section>

      <LatestEventsTable events={latestEvents} />
    </div>
  );
}
