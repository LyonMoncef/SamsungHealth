# SamsungHealth

## Problem

Samsung Health data is locked inside the mobile app with no open API. Visualizing sleep patterns over time requires manual CSV exports and external tools.

## Solution

An end-to-end pipeline that automatically syncs Samsung Health data via Android Health Connect, stores it in a database, and serves an interactive web dashboard with tabs for sleep, steps, heart rate, exercise, and trends.

## Features

| Feature | Files | Commit |
|---------|-------|--------|
| Sleep API + stage-aware calendar | `server/routers/sleep.py`, `static/app.js` | `8d5cfb0` |
| Steps API + bar chart | `server/routers/steps.py`, `static/app.js` | `242040a` |
| Heart rate API + range chart | `server/routers/heartrate.py`, `static/app.js` | `242040a` |
| Exercise API + card list | `server/routers/exercise.py`, `static/app.js` | `242040a` |
| Tabbed dashboard + trends | `static/index.html`, `static/app.js`, `static/style.css` | `242040a` |
| Android Health Connect sync | `android-app/` | `242040a` |
| Sample data generator | `scripts/generate_sample.py` | `242040a` |

## Architecture

```
Samsung Health → Health Connect ← Android App →HTTP→ FastAPI Backend →→ SQLite (health.db)
                                                            ↓
                                                   Tabbed Web Dashboard
                                            (Sleep | Steps | HR | Exercise | Trends)
```

### Components

| Component | Tech | Status |
|-----------|------|--------|
| Backend API | Python, FastAPI | Done |
| Database | SQLite (`health.db`) | Done |
| Web dashboard | HTML/CSS/JS (5 tabs) | Done |
| Android app | Kotlin, Jetpack Compose, Health Connect | Done |
| Sample data generator | Python | Done |

## Setup

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Usage

```bash
# Generate sample data (sleep, steps, HR, exercise)
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
