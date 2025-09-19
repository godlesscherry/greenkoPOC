import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
  Area
} from 'recharts';
import type { ForecastPoint, MetricPoint } from '../api/types';
import dayjs from 'dayjs';

interface PowerChartProps {
  metrics: MetricPoint[];
  forecast: ForecastPoint[];
  actualMultiplier: number;
  actualLabel: string;
  forecastLabel: string;
}

export function PowerChart({
  metrics,
  forecast,
  actualMultiplier,
  actualLabel,
  forecastLabel
}: PowerChartProps) {
  const data = metrics.map((point) => ({
    time: point.bucketStart,
    actual: point.averagePowerKw * actualMultiplier,
    forecast: null as number | null,
    ciBase: 0,
    ciRange: 0
  }));

  const forecastData = forecast.map((point) => ({
    time: point.time,
    forecast: point.predictedPowerKw,
    lower: point.lowerBoundKw,
    upper: point.upperBoundKw,
    actual: null as number | null,
    ciBase: point.lowerBoundKw,
    ciRange: point.upperBoundKw - point.lowerBoundKw
  }));

  const combined = [...data, ...forecastData];

  return (
    <ResponsiveContainer width="100%" height={420}>
      <LineChart data={combined} margin={{ top: 20, left: 20, right: 20, bottom: 20 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
        <XAxis
          dataKey="time"
          tickFormatter={(value) => dayjs(value).format('HH:mm')}
          stroke="#94a3b8"
        />
        <YAxis stroke="#94a3b8" unit=" kW" />
        <Tooltip
          contentStyle={{ backgroundColor: '#0f172a', borderRadius: 8, borderColor: '#1f2937' }}
          labelFormatter={(value) => dayjs(value).format('YYYY-MM-DD HH:mm')}
        />
        <Legend />
        <Line
          type="monotone"
          dataKey="actual"
          name={actualLabel}
          stroke="#38bdf8"
          strokeWidth={2}
          dot={false}
        />
        <Area
          type="monotone"
          dataKey="ciBase"
          stackId="ci"
          stroke="none"
          fill="transparent"
          activeDot={false}
          legendType="none"
        />
        <Area
          type="monotone"
          dataKey="ciRange"
          stackId="ci"
          stroke="none"
          fill="rgba(14, 165, 233, 0.15)"
          activeDot={false}
          legendType="none"
        />
        <Line
          type="monotone"
          dataKey="forecast"
          name={forecastLabel}
          stroke="#fbbf24"
          strokeWidth={2}
          dot={false}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}
