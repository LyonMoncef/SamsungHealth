---
type: code-source
language: python
file_path: server/database.py
git_blob: ee741d78f2a40ce2816bd100f4a410a73eaadb48
last_synced: '2026-04-23T10:31:18Z'
loc: 298
annotations: []
imports:
- sqlite3
- pathlib
exports:
- get_connection
- _add_col
- init_db
tags:
- code
- python
coverage_pct: 100.0
---

# server/database.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/database.py`](../../../server/database.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).resolve().parent.parent / "health.db"


def get_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def _add_col(conn, table: str, column: str, col_type: str) -> None:
    existing = [r[1] for r in conn.execute(f"PRAGMA table_info({table})")]
    if column not in existing:
        conn.execute(f"ALTER TABLE {table} ADD COLUMN {column} {col_type}")


def init_db():
    conn = get_connection()
    conn.execute("PRAGMA foreign_keys = ON")

    # ── sleep ──────────────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS sleep_sessions (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            sleep_start TEXT NOT NULL,
            sleep_end   TEXT NOT NULL,
            created_at  TEXT NOT NULL DEFAULT (datetime('now')),
            UNIQUE(sleep_start, sleep_end)
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_sleep_start ON sleep_sessions(sleep_start)")
    for col, typ in [
        ("sleep_score",        "INTEGER"),
        ("efficiency",         "REAL"),
        ("sleep_duration_min", "INTEGER"),
        ("sleep_cycle",        "INTEGER"),
        ("mental_recovery",    "REAL"),
        ("physical_recovery",  "REAL"),
        ("sleep_type",         "INTEGER"),
    ]:
        _add_col(conn, "sleep_sessions", col, typ)

    conn.execute("""
        CREATE TABLE IF NOT EXISTS sleep_stages (
            id         INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id INTEGER NOT NULL,
            stage_type TEXT NOT NULL,
            stage_start TEXT NOT NULL,
            stage_end   TEXT NOT NULL,
            created_at  TEXT NOT NULL DEFAULT (datetime('now')),
            FOREIGN KEY (session_id) REFERENCES sleep_sessions(id) ON DELETE CASCADE
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_stages_session ON sleep_stages(session_id)")
    conn.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_stages_unique ON sleep_stages(stage_start, stage_end)")

    # ── steps ──────────────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS steps_hourly (
            date       TEXT NOT NULL,
            hour       INTEGER NOT NULL,
            step_count INTEGER NOT NULL,
            UNIQUE(date, hour)
        )
    """)

    conn.execute("""
        CREATE TABLE IF NOT EXISTS steps_daily (
            day_date         TEXT NOT NULL,
            step_count       INTEGER,
            walk_step_count  INTEGER,
            run_step_count   INTEGER,
            distance_m       REAL,
            calorie_kcal     REAL,
            active_time_ms   INTEGER,
            UNIQUE(day_date)
        )
    """)

    # ── heart rate ─────────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS heart_rate_hourly (
            date         TEXT NOT NULL,
            hour         INTEGER NOT NULL,
            min_bpm      INTEGER NOT NULL,
            max_bpm      INTEGER NOT NULL,
            avg_bpm      INTEGER NOT NULL,
            sample_count INTEGER NOT NULL,
            UNIQUE(date, hour)
        )
    """)

    # ── exercise ───────────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS exercise_sessions (
            exercise_type  TEXT NOT NULL,
            exercise_start TEXT NOT NULL,
            exercise_end   TEXT NOT NULL,
            duration_minutes REAL NOT NULL,
            UNIQUE(exercise_start, exercise_end)
        )
    """)
    for col, typ in [
        ("calorie_kcal",    "REAL"),
        ("distance_m",      "REAL"),
        ("mean_heart_rate", "REAL"),
        ("max_heart_rate",  "REAL"),
        ("min_heart_rate",  "REAL"),
        ("mean_speed_ms",   "REAL"),
    ]:
        _add_col(conn, "exercise_sessions", col, typ)

    # ── stress ─────────────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS stress (
            start_time TEXT NOT NULL,
            end_time   TEXT NOT NULL,
            score      REAL,
            tag_id     INTEGER,
            UNIQUE(start_time, end_time)
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_stress_start ON stress(start_time)")

    # ── SpO2 ───────────────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS spo2 (
            start_time      TEXT NOT NULL,
            end_time        TEXT NOT NULL,
            spo2            REAL,
            min_spo2        REAL,
            max_spo2        REAL,
            low_duration_s  INTEGER,
            tag_id          INTEGER,
            UNIQUE(start_time, end_time)
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_spo2_start ON spo2(start_time)")

    # ── respiratory rate ───────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS respiratory_rate (
            start_time   TEXT NOT NULL,
            end_time     TEXT NOT NULL,
            average      REAL,
            lower_limit  REAL,
            upper_limit  REAL,
            UNIQUE(start_time, end_time)
        )
    """)

    # ── HRV ────────────────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS hrv (
            start_time TEXT NOT NULL,
            end_time   TEXT NOT NULL,
            UNIQUE(start_time, end_time)
        )
    """)

    # ── skin temperature ───────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS skin_temperature (
            start_time   TEXT NOT NULL,
            end_time     TEXT NOT NULL,
            temperature  REAL,
            min_temp     REAL,
            max_temp     REAL,
            tag_id       INTEGER,
            UNIQUE(start_time, end_time)
        )
    """)

    # ── weight / height / blood pressure ───────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS weight (
            start_time              TEXT NOT NULL,
            weight_kg               REAL,
            body_fat_pct            REAL,
            skeletal_muscle_pct     REAL,
            skeletal_muscle_mass_kg REAL,
            fat_free_mass_kg        REAL,
            basal_metabolic_rate    INTEGER,
            total_body_water_kg     REAL,
            UNIQUE(start_time)
        )
    """)

    conn.execute("""
        CREATE TABLE IF NOT EXISTS height (
            start_time TEXT NOT NULL,
            height_cm  REAL,
            UNIQUE(start_time)
        )
    """)

    conn.execute("""
        CREATE TABLE IF NOT EXISTS blood_pressure (
            start_time TEXT NOT NULL,
            systolic   REAL,
            diastolic  REAL,
            pulse      INTEGER,
            mean_bp    REAL,
            UNIQUE(start_time)
        )
    """)

    # ── mood ───────────────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS mood (
            start_time TEXT NOT NULL,
            mood_type  INTEGER,
            emotions   TEXT,
            factors    TEXT,
            notes      TEXT,
            place      TEXT,
            company    TEXT,
            UNIQUE(start_time)
        )
    """)

    # ── water intake ───────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS water_intake (
            start_time TEXT NOT NULL,
            amount_ml  REAL,
            UNIQUE(start_time)
        )
    """)

    # ── daily composites ───────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS activity_daily (
            day_date         TEXT NOT NULL,
            step_count       INTEGER,
            distance_m       REAL,
            calorie_kcal     REAL,
            exercise_time_ms INTEGER,
            active_time_ms   INTEGER,
            floor_count      REAL,
            score            INTEGER,
            UNIQUE(day_date)
        )
    """)

    conn.execute("""
        CREATE TABLE IF NOT EXISTS vitality_score (
            day_date        TEXT NOT NULL,
            total_score     REAL,
            sleep_score     REAL,
            sleep_balance   REAL,
            sleep_regularity REAL,
            sleep_timing    REAL,
            activity_score  REAL,
            active_time_ms  INTEGER,
            mvpa_time_ms    INTEGER,
            shr_score       REAL,
            shr_value       REAL,
            shrv_score      REAL,
            shrv_value      REAL,
            UNIQUE(day_date)
        )
    """)

    conn.execute("""
        CREATE TABLE IF NOT EXISTS floors_daily (
            day_date    TEXT NOT NULL,
            floor_count INTEGER,
            UNIQUE(day_date)
        )
    """)

    conn.execute("""
        CREATE TABLE IF NOT EXISTS activity_level (
            start_time     TEXT NOT NULL,
            activity_level INTEGER,
            UNIQUE(start_time)
        )
    """)

    # ── ECG ────────────────────────────────────────────────────────────────
    conn.execute("""
        CREATE TABLE IF NOT EXISTS ecg (
            start_time       TEXT NOT NULL,
            end_time         TEXT NOT NULL,
            mean_heart_rate  REAL,
            sample_frequency INTEGER,
            sample_count     INTEGER,
            classification   INTEGER,
            UNIQUE(start_time, end_time)
        )
    """)

    conn.commit()
    conn.close()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `get_connection` (function) — lines 7-11 · **Tested by (12)**: `test_sleep.test_get_sleep_not_found`, `test_sleep.test_get_sleep_sessions`, `test_sleep.test_post_sleep_dedup`, `test_sleep.test_post_sleep_session`, `test_sleep.test_post_sleep_with_stages` _+7_
- `_add_col` (function) — lines 14-17 · ⚠️ no test
- `init_db` (function) — lines 20-298 · ⚠️ no test

### Imports
- `sqlite3`
- `pathlib`

### Exports
- `get_connection`
- `_add_col`
- `init_db`
