# SamsungHealth

## Problem

Samsung Health data is locked inside the mobile app with no open API. Visualizing sleep patterns over time requires manual CSV exports and external tools.

## Solution

An end-to-end pipeline that automatically syncs Samsung Health data (starting with sleep) via the Android SDK, stores it in a database, and serves an interactive web visualization — a calendar grid where each row is a day and each column is an hour, showing sleep patterns at a glance.

## Features

| Feature | Files | Commit |
|---------|-------|--------|
| — | — | — |

## Architecture

```
Samsung Health ←SDK→ Android App →HTTP→ Python Backend (FastAPI) →→ SQLite
                                              ↓
                                       Web UI (sleep grid)
```

### Components

| Component | Tech | Status |
|-----------|------|--------|
| Backend API | Python, FastAPI | Planned |
| Database | SQLite | Planned |
| Sleep visualization | HTML/CSS/JS | Planned |
| Data import (CLI) | Python | Planned |
| Android app | Kotlin, Samsung Health SDK | Planned |

## Setup

```bash
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Usage

_Coming soon._

## Related Projects

- [agent-schemas](https://github.com/LyonMoncef/agent-schemas) — Shared schema definitions
- [DocAgent](https://github.com/LyonMoncef/DocAgent) — Config query/edit agent
- [WidgetGenerator](https://github.com/LyonMoncef/WidgetGenerator) — JSON-to-Rainmeter generator
