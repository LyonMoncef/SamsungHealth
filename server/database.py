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
    conn.execute("""
        CREATE TABLE IF NOT EXISTS sleep_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sleep_start TEXT NOT NULL,
            sleep_end TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT (datetime('now'))
        )
    """)
    conn.execute("""
        CREATE INDEX IF NOT EXISTS idx_sleep_start ON sleep_sessions(sleep_start)
    """)
    conn.commit()
    conn.close()
