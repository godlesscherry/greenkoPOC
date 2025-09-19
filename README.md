# Greenko Windfarm Streaming PoC

A local-only full-stack proof-of-concept that replays a one-day windfarm dataset, stores the
timeseries in TimescaleDB, and visualises live power output alongside a lightweight forecast.

## Architecture

- **PostgreSQL 13 + TimescaleDB** via Docker Compose.
- **Spring Boot 3 / Java 21** backend with SSE streaming, JDBC ingestion pipeline, and REST APIs.
- **Vite + React 18** dashboard with Recharts visualisations and live event handling.
- **Flyway** for database migrations, **Spotless + ESLint/Prettier** for formatting.

## Prerequisites

- Docker & Docker Compose
- Node.js 18+
- Java 21 (only required if you run the Gradle wrapper manually)
- Internet access on first run so Gradle can download its distribution

## Quickstart

```bash
# install JS dependencies
npm install

# boot database, run backend & dashboard together
npm run dev
```

`npm run dev` launches TimescaleDB (docker), the Spring Boot API (`./server`), and the Vite client
(`./client`). The backend will stream telemetry from `data/device_energy_data.csv` (or the path set
in `DATASET_PATH`) into TimescaleDB, publish SSE updates, and serve REST endpoints under `/api`.

The Gradle wrapper materialises its helper JAR from the checked-in Base64 payload. Override
`GRADLE_WRAPPER_JAR_BASE64` to point at an alternative source or place the decoded file at
`server/gradle/wrapper/gradle-wrapper.jar` when running in a restricted environment.

Visit <http://localhost:5173> for the dashboard. Use the `--accelerate` flag when starting the server
(e.g. `npm run server -- --args='--accelerate'`) or set `EMIT_ACCELERATE=true` to speed up the
replay for demos.

## Useful Commands

| Command | Description |
| --- | --- |
| `npm run db:up` | Start TimescaleDB container |
| `npm run db:down` | Stop containers |
| `npm run db:reset` | Stop containers and wipe data volume |
| `npm run server` | Run backend only (`./server`) |
| `npm run client` | Run React dashboard only |
| `./scripts/migrate.sh` | Apply Flyway migrations |
| `./scripts/load-sample.sh [minutes]` | Seed the first N minutes of data (default 180) without running the emitter |

## Configuration

Environment variables can be supplied via `.env` at the repository root:

- `DATASET_PATH` – path to the CSV dataset (default `./data/device_energy_data.csv`)
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` – database connectivity
- `EMIT_INTERVAL_MS` – override emission interval in milliseconds
- `EMIT_ACCELERATE` – set to `true` to use the accelerated interval (default 1s)

The backend also honours:

- `WINDFARM_SEED_ENABLED` / `WINDFARM_SEED_MINUTES` for one-off seeding runs
- `windfarm.emitter.interval`, `windfarm.listener.flush-interval`, etc. within
  `server/src/main/resources/application.yml`

## Dataset Normalisation

`DatasetLoader` de-duplicates the CSV by `(timestamp, device_id)` and sums the minute-level energy
before converting to power (`power_kw = energy_kwh * 60`). All timestamps are treated as UTC.

## Forecast Model

The forecast endpoint applies a rolling mean (last 15 samples) blended with an Ordinary Least Squares
trend using Apache Commons Math. An 80% confidence band is derived from regression residuals and
exposed to the frontend for chart shading.

## Testing & Quality Gates

```bash
# backend
(cd server && ./gradlew clean check)

# frontend
(cd client && npm install && npm run lint)
```

Spotless enforces Google Java formatting. ESLint/Prettier cover the client.

## Troubleshooting

- Ensure the TimescaleDB container is healthy (`docker compose ps`).
- If the dataset path differs, update `DATASET_PATH` or pass `--windfarm.dataset-path=...` when
  starting the backend.
- Use `./scripts/reset.sh` to tear down the stack and clear persisted data.
