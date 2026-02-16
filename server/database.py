import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).resolve().parent.parent / "sleep.db"


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
    conn.commit()
    conn.close()
