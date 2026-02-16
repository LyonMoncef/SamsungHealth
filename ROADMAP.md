# Roadmap

## Phase 1 — Backend + Database + Visualization

### 1.1 Project scaffolding
- [x] Git repo + GitHub remote
- [x] Documentation files (README, NOTES, HISTORY, ROADMAP)
- [x] Python project structure + dependencies
- [x] SQLite database schema (sleep_sessions table, extensible for future data)

### 1.2 Backend API
- [x] FastAPI app with endpoints:
  - `POST /api/sleep` — ingest sleep sessions
  - `GET /api/sleep?from=&to=` — query sleep data by date range
  - `GET /` — serve web UI
- [x] Data validation (Pydantic models)

### 1.3 Sleep calendar visualization
- [x] HTML/CSS grid: rows = days, columns = 24 hours
- [x] Color cells for hours asleep, handle midnight crossover
- [x] Date range navigation (month selector)
- [x] Hover tooltips with exact sleep/wake times

### 1.4 Data import CLI
- [x] Script to import Samsung Health CSV exports into the database
- [x] Script to generate test/sample data for development

---

## Phase 2 — Android App (Health Connect) + Sleep Stages

### 2.1 Sleep stages backend
- [x] `sleep_stages` table with FK to sessions
- [x] Stage models (SleepStageIn/Out) + optional stages on session models
- [x] `POST /api/sleep` inserts stages, dedup via UNIQUE constraint
- [x] `GET /api/sleep?include_stages=true` returns stages per session

### 2.2 Stage-aware visualization
- [x] Color-coded calendar cells by dominant stage (light/deep/REM/awake)
- [x] Tooltip shows stage breakdown per hour

### 2.3 Android project setup
- [x] Android Studio project (Kotlin, Jetpack Compose)
- [x] Health Connect integration (replaced deprecated Samsung Health SDK)
- [x] Gradle build with Health Connect client, Retrofit, DataStore

### 2.4 Sleep data sync
- [x] Read sleep sessions + stages from Health Connect
- [x] Map Health Connect stage constants to backend format
- [x] Push to backend API via Retrofit
- [x] Incremental sync (since last sync timestamp)
- [x] Full historical fetch on first sync

### 2.5 App UI
- [x] Sync screen: sync button, progress, status, last sync time
- [x] Settings screen: backend URL configuration
- [x] Permission flow for Health Connect READ_SLEEP

### 2.6 Sample data
- [x] Generator produces realistic sleep cycles with stages

---

## Phase 3 — Expansion

### 3.1 Additional data types
- [x] Steps API + hourly granularity storage
- [x] Heart rate API + hourly min/max/avg
- [x] Exercise sessions API

### 3.2 Enhanced visualizations
- [x] Tabbed dashboard (Sleep, Steps, Heart Rate, Exercise, Trends)
- [x] Steps bar chart (green bars, daily totals)
- [x] Heart rate range bars (min-max with avg marker)
- [x] Exercise card list grouped by date
- [x] Trends stat cards (avg sleep, daily steps, resting HR, exercise count)

### 3.3 Android sync expansion
- [x] Read steps, heart rate, exercise from Health Connect
- [x] Hourly aggregation on device before POST
- [x] Combined sync status (Sleep/Steps/HR/Exercise)

### 3.4 Sample data
- [x] Generator produces all 4 data types with realistic patterns

---

## Status

| Phase | Status | Target |
|-------|--------|--------|
| Phase 1 | **Done** | — |
| Phase 2 | **Done** | — |
| Phase 3 | **Done** | — |

_Last updated: 2026-02-16_
