# Roadmap

## Phase 1 — Backend + Database + Visualization (current)

### 1.1 Project scaffolding
- [x] Git repo + GitHub remote
- [x] Documentation files (README, NOTES, HISTORY, ROADMAP)
- [ ] Python project structure + dependencies
- [ ] SQLite database schema (sleep_sessions table, extensible for future data)

### 1.2 Backend API
- [ ] FastAPI app with endpoints:
  - `POST /api/sleep` — ingest sleep sessions
  - `GET /api/sleep?from=&to=` — query sleep data by date range
  - `GET /` — serve web UI
- [ ] Data validation (Pydantic models)

### 1.3 Sleep calendar visualization
- [ ] HTML/CSS grid: rows = days, columns = 24 hours
- [ ] Color cells for hours asleep, handle midnight crossover
- [ ] Date range navigation (month selector)
- [ ] Hover tooltips with exact sleep/wake times

### 1.4 Data import CLI
- [ ] Script to import Samsung Health CSV exports into the database
- [ ] Script to generate test/sample data for development

---

## Phase 2 — Android App (Samsung Health SDK)

### 2.1 Android project setup
- [ ] Android Studio project (Kotlin)
- [ ] Samsung Health SDK integration + permissions

### 2.2 Sleep data sync
- [ ] Read sleep sessions from Samsung Health
- [ ] Push to backend API
- [ ] Background sync (periodic or on-demand)

### 2.3 App UI
- [ ] Minimal UI: sync button + status + last sync time
- [ ] Settings: backend URL configuration

---

## Phase 3 — Expansion

### 3.1 Additional data types
- [ ] Steps (daily totals + hourly breakdown)
- [ ] Heart rate
- [ ] Exercise sessions

### 3.2 Enhanced visualizations
- [ ] Multi-data dashboard
- [ ] Trends and statistics
- [ ] Correlation views (sleep vs. activity)

---

## Status

| Phase | Status | Target |
|-------|--------|--------|
| Phase 1 | **In progress** | — |
| Phase 2 | Not started | — |
| Phase 3 | Not started | — |

_Last updated: 2026-02-16_
