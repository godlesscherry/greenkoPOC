CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS telemetry (
    time        TIMESTAMPTZ NOT NULL,
    device_id   TEXT NOT NULL,
    energy_kwh  DOUBLE PRECISION NOT NULL,
    power_kw    DOUBLE PRECISION GENERATED ALWAYS AS (energy_kwh * 60) STORED,
    PRIMARY KEY (time, device_id)
);

SELECT create_hypertable('telemetry', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_telemetry_device_time_desc
    ON telemetry (device_id, time DESC);
