import dayjs from 'dayjs';
import type { TelemetryRecord } from '../api/types';

interface LatestEventsTableProps {
  events: TelemetryRecord[];
}

export function LatestEventsTable({ events }: LatestEventsTableProps) {
  return (
    <div className="card table-container">
      <h3>Latest Events</h3>
      <table>
        <thead>
          <tr>
            <th>Time</th>
            <th>Device</th>
            <th>Energy (kWh)</th>
            <th>Power (kW)</th>
          </tr>
        </thead>
        <tbody>
          {events.map((event) => (
            <tr key={`${event.deviceId}-${event.time}`}>
              <td>{dayjs(event.time).format('HH:mm:ss')}</td>
              <td>{event.deviceId}</td>
              <td>{event.energyKwh.toFixed(2)}</td>
              <td>{event.powerKw.toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
