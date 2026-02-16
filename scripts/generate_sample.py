#!/usr/bin/env python3
"""Generate ~30 days of realistic sample sleep data."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import random
from datetime import datetime, timedelta
from server.database import init_db, get_connection


def generate():
    init_db()
    conn = get_connection()

    # Clear existing sample data
    conn.execute("DELETE FROM sleep_sessions")

    base_date = datetime(2026, 1, 15)
    for day_offset in range(30):
        date = base_date + timedelta(days=day_offset)

        # Bedtime: 22:00–01:00 (next day)
        bed_hour = random.choice([22, 23, 0, 1])
        bed_minute = random.randint(0, 59)
        if bed_hour >= 22:
            sleep_start = date.replace(hour=bed_hour, minute=bed_minute, second=0)
        else:
            sleep_start = (date + timedelta(days=1)).replace(
                hour=bed_hour, minute=bed_minute, second=0
            )

        # Wake time: 6:00–9:00
        wake_hour = random.randint(6, 8)
        wake_minute = random.randint(0, 59)
        sleep_end = (sleep_start + timedelta(days=1)).replace(
            hour=wake_hour, minute=wake_minute, second=0
        ) if bed_hour >= 22 else sleep_start.replace(
            hour=wake_hour, minute=wake_minute, second=0
        )

        # Ensure end is after start
        if sleep_end <= sleep_start:
            sleep_end += timedelta(days=1)

        conn.execute(
            "INSERT INTO sleep_sessions (sleep_start, sleep_end) VALUES (?, ?)",
            (sleep_start.isoformat(), sleep_end.isoformat()),
        )

    conn.commit()
    count = conn.execute("SELECT COUNT(*) FROM sleep_sessions").fetchone()[0]
    conn.close()
    print(f"Inserted {count} sleep sessions into sleep.db")


if __name__ == "__main__":
    generate()
