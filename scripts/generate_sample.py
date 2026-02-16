#!/usr/bin/env python3
"""Generate ~30 days of realistic sample sleep data with stages."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import random
from datetime import datetime, timedelta
from server.database import init_db, get_connection


STAGE_TYPES = ["light", "deep", "rem", "awake"]


def generate_stages(sleep_start: datetime, sleep_end: datetime) -> list[dict]:
    """Generate realistic sleep stage cycles.

    A typical night cycles through: light -> deep -> light -> REM,
    with brief awake periods between cycles. Each cycle ~90 minutes.
    """
    stages = []
    cursor = sleep_start
    total_duration = (sleep_end - sleep_start).total_seconds()
    cycle_count = max(1, int(total_duration / 5400))  # ~90min cycles

    for cycle in range(cycle_count):
        if cursor >= sleep_end:
            break

        # Light sleep: 10-25 min
        duration = random.randint(10, 25)
        end = min(cursor + timedelta(minutes=duration), sleep_end)
        stages.append({"stage_type": "light", "start": cursor, "end": end})
        cursor = end
        if cursor >= sleep_end:
            break

        # Deep sleep: 15-40 min (more in early cycles)
        deep_max = 40 if cycle < 2 else 20
        duration = random.randint(15, deep_max)
        end = min(cursor + timedelta(minutes=duration), sleep_end)
        stages.append({"stage_type": "deep", "start": cursor, "end": end})
        cursor = end
        if cursor >= sleep_end:
            break

        # Light sleep again: 5-15 min
        duration = random.randint(5, 15)
        end = min(cursor + timedelta(minutes=duration), sleep_end)
        stages.append({"stage_type": "light", "start": cursor, "end": end})
        cursor = end
        if cursor >= sleep_end:
            break

        # REM: 10-30 min (longer in later cycles)
        rem_min = 10 if cycle < 2 else 20
        rem_max = 20 if cycle < 2 else 35
        duration = random.randint(rem_min, rem_max)
        end = min(cursor + timedelta(minutes=duration), sleep_end)
        stages.append({"stage_type": "rem", "start": cursor, "end": end})
        cursor = end
        if cursor >= sleep_end:
            break

        # Brief awake between cycles: 1-5 min (not always)
        if random.random() < 0.3:
            duration = random.randint(1, 5)
            end = min(cursor + timedelta(minutes=duration), sleep_end)
            stages.append({"stage_type": "awake", "start": cursor, "end": end})
            cursor = end

    return stages


def generate():
    init_db()
    conn = get_connection()
    conn.execute("PRAGMA foreign_keys = ON")

    conn.execute("DELETE FROM sleep_stages")
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

        cursor = conn.execute(
            "INSERT INTO sleep_sessions (sleep_start, sleep_end) VALUES (?, ?)",
            (sleep_start.isoformat(), sleep_end.isoformat()),
        )
        session_id = cursor.lastrowid

        stages = generate_stages(sleep_start, sleep_end)
        for st in stages:
            conn.execute(
                "INSERT INTO sleep_stages (session_id, stage_type, stage_start, stage_end) VALUES (?, ?, ?, ?)",
                (session_id, st["stage_type"], st["start"].isoformat(), st["end"].isoformat()),
            )

    conn.commit()
    session_count = conn.execute("SELECT COUNT(*) FROM sleep_sessions").fetchone()[0]
    stage_count = conn.execute("SELECT COUNT(*) FROM sleep_stages").fetchone()[0]
    conn.close()
    print(f"Inserted {session_count} sleep sessions with {stage_count} stages into sleep.db")


if __name__ == "__main__":
    generate()
