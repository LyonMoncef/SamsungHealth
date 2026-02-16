# SamsungHealth

## Problem

Samsung Health data is locked inside the mobile app with no open API. Visualizing sleep patterns over time requires manual CSV exports and external tools.

## Solution

An end-to-end pipeline that automatically syncs Samsung Health data (starting with sleep) via the Android SDK, stores it in a database, and serves an interactive web visualization — a calendar grid where each row is a day and each column is an hour, showing sleep patterns at a glance.

## Features

| Feature | Files | Commit |
|---------|-------|--------|
| Sleep API (POST/GET) | `server/routers/sleep.py`, `server/models.py` | — |
| SQLite database | `server/database.py` | — |
| Sleep calendar grid | `static/index.html`, `static/style.css`, `static/app.js` | — |
| CSV import tool | `scripts/import_csv.py` | — |
| Sample data generator | `scripts/generate_sample.py` | — |

## Architecture

```
Samsung Health ←SDK→ Android App →HTTP→ Python Backend (FastAPI) →→ SQLite
                                              ↓
                                       Web UI (sleep grid)
```

### Components

| Component | Tech | Status |
|-----------|------|--------|
| Backend API | Python, FastAPI | Done |
| Database | SQLite | Done |
| Sleep visualization | HTML/CSS/JS | Done |
| Data import (CLI) | Python | Done |
| Android app | Kotlin, Samsung Health SDK | Planned |

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Usage

```bash
# Generate sample sleep data
python scripts/generate_sample.py

# Start the server
uvicorn server.main:app

# Open http://localhost:8000 in your browser

# Import Samsung Health CSV export
python scripts/import_csv.py path/to/sleep.csv
```

## Related Projects

- [agent-schemas](https://github.com/LyonMoncef/agent-schemas) — Shared schema definitions
- [DocAgent](https://github.com/LyonMoncef/DocAgent) — Config query/edit agent
- [WidgetGenerator](https://github.com/LyonMoncef/WidgetGenerator) — JSON-to-Rainmeter generator
