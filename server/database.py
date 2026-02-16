import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).resolve().parent.parent / "health.db"


def get_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(str(DB_PATH))
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def init_db():
    conn = get_connection()
    conn.execute("PRAGMA foreign_keys = ON")
    conn.execute("""
        CREATE TABLE IF NOT EXISTS sleep_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sleep_start TEXT NOT NULL,
            sleep_end TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            UNIQUE(sleep_start, sleep_end)
        )
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_sleep_start ON sleep_sessions(sleep_start)
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS sleep_stages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id INTEGER NOT NULL,
            stage_type TEXT NOT NULL,
            stage_start TEXT NOT NULL,
            stage_end TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now')),
            FOREIGN KEY (session_id) REFERENCES sleep_sessions(id) ON DELETE CASCADE
        )
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_stages_session ON sleep_stages(session_id)
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS steps_hourly (
            date TEXT NOT NULL,
            hour INTEGER NOT NULL,
            step_count INTEGER NOT NULL,
            UNIQUE(date, hour)
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS heart_rate_hourly (
            date TEXT NOT NULL,
            hour INTEGER NOT NULL,
            min_bpm INTEGER NOT NULL,
            max_bpm INTEGER NOT NULL,
            avg_bpm INTEGER NOT NULL,
            sample_count INTEGER NOT NULL,
            UNIQUE(date, hour)
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS exercise_sessions (
            exercise_type TEXT NOT NULL,
            exercise_start TEXT NOT NULL,
            exercise_end TEXT NOT NULL,
            duration_minutes REAL NOT NULL,
            UNIQUE(exercise_start, exercise_end)
        )
    """)
    conn.commit()
    conn.close()
