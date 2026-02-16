# History

## Features

| Feature | Files | Commit |
|---------|-------|--------|
| Phase 1: Backend + DB + UI + Scripts | `server/`, `static/`, `scripts/`, `requirements.txt` | [`6200a93`](#2026-02-16-6200a93) |
| Project scaffolding | `.gitignore`, `README.md`, `NOTES.md`, `HISTORY.md`, `ROADMAP.md` | [`6cc83dc`](#2026-02-16-6cc83dc) |

---

## Changelog

### 2026-02-16 `6200a93`
Add Phase 1: FastAPI backend, SQLite DB, sleep calendar UI, and import scripts
- Added `server/database.py` with SQLite connection (WAL mode), `sleep_sessions` table and index
- Added `server/models.py` with Pydantic models for sleep session validation
- Added `server/routers/sleep.py` with POST/GET `/api/sleep` endpoints (bulk insert, date range query)
- Added `server/main.py` wiring FastAPI app, static file serving, DB init on startup
- Added `static/index.html`, `style.css`, `app.js` — sleep calendar grid (24h columns, per-day rows, month nav, hover tooltips)
- Added `scripts/generate_sample.py` — generates 30 days of realistic sleep data
- Added `scripts/import_csv.py` — parses Samsung Health CSV exports and POSTs to API
- Added `requirements.txt` (fastapi, uvicorn, pydantic)
- Updated `.gitignore` to include `.venv/`
- Updated `README.md` with features table, setup, and usage instructions

### 2026-02-16 `6cc83dc`
Initial project scaffolding with documentation
- Created GitHub repo (LyonMoncef/SamsungHealth)
- Added README with architecture overview (Android SDK → FastAPI → SQLite → Web UI)
- Added NOTES.md with backlog items
- Added ROADMAP.md with 3-phase plan (backend+viz → Android app → expansion)
- Added .gitignore for Python, Android, IDE, and DB files
