import { useEffect, useRef } from 'react';
import type { TelemetryRecord } from '../api/types';
import { resolveApiUrl } from '../api/client';

export function useTelemetryStream(
  deviceId: string,
  enabled: boolean,
  onMessage: (record: TelemetryRecord) => void
) {
  const handlerRef = useRef(onMessage);
  handlerRef.current = onMessage;

  useEffect(() => {
    if (!enabled) {
      return;
    }
    const url = new URL(resolveApiUrl('/api/stream'));
    if (deviceId && deviceId !== 'ALL') {
      url.searchParams.set('deviceId', deviceId);
    }
    const eventSource = new EventSource(url.toString());
    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as TelemetryRecord;
        handlerRef.current(data);
      } catch (err) {
        console.error('Failed to parse telemetry payload', err);
      }
    };
    return () => {
      eventSource.close();
    };
  }, [deviceId, enabled]);
}
