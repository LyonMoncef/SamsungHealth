#!/usr/bin/env python3
"""Generate ~30 days of realistic sample health data."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import random
from datetime import datetime, timedelta
from server.database import init_db, get_connection


STAGE_TYPES = ["light", "deep", "rem", "awake"]

EXERCISE_TYPES = ["running", "walking", "cycling", "swimming", "hiking", "yoga", "strength_training"]


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


def generate_steps(conn, base_date: datetime, num_days: int):
    """Generate hourly step data for num_days. Low at night, peak midday."""
    for day_offset in range(num_days):
        date = base_date + timedelta(days=day_offset)
        date_str = date.strftime("%Y-%m-%d")
        for hour in range(24):
            if hour < 6:
                steps = random.randint(0, 10)
            elif hour < 8:
                steps = random.randint(100, 400)
            elif hour < 12:
                steps = random.randint(300, 800)
            elif hour < 14:
                steps = random.randint(500, 1200)
            elif hour < 18:
                steps = random.randint(300, 900)
            elif hour < 21:
                steps = random.randint(200, 600)
            else:
                steps = random.randint(20, 150)
            conn.execute(
                "INSERT OR IGNORE INTO steps_hourly (date, hour, step_count) VALUES (?, ?, ?)",
                (date_str, hour, steps),
            )


def generate_heart_rate(conn, base_date: datetime, num_days: int):
    """Generate hourly heart rate data. Lower at night, higher during day."""
    for day_offset in range(num_days):
        date = base_date + timedelta(days=day_offset)
        date_str = date.strftime("%Y-%m-%d")
        for hour in range(24):
            if hour < 6:
                avg = random.randint(55, 65)
                spread = random.randint(3, 8)
            elif hour < 9:
                avg = random.randint(65, 80)
                spread = random.randint(5, 15)
            elif hour < 18:
                avg = random.randint(72, 95)
                spread = random.randint(8, 25)
            elif hour < 22:
                avg = random.randint(65, 82)
                spread = random.randint(5, 15)
            else:
                avg = random.randint(58, 70)
                spread = random.randint(3, 10)
            min_bpm = max(40, avg - spread)
            max_bpm = min(180, avg + spread)
            sample_count = random.randint(5, 30)
            conn.execute(
                "INSERT OR IGNORE INTO heart_rate_hourly (date, hour, min_bpm, max_bpm, avg_bpm, sample_count) VALUES (?, ?, ?, ?, ?, ?)",
                (date_str, hour, min_bpm, max_bpm, avg, sample_count),
            )


def generate_exercise(conn, base_date: datetime, num_days: int):
    """Generate 10-15 exercise sessions spread across num_days."""
    num_sessions = random.randint(10, 15)
    used_days = random.sample(range(num_days), min(num_sessions, num_days))
    for day_offset in used_days:
        date = base_date + timedelta(days=day_offset)
        ex_type = random.choice(EXERCISE_TYPES)
        start_hour = random.randint(6, 19)
        start_minute = random.choice([0, 15, 30, 45])
        duration = random.randint(20, 90)
        ex_start = date.replace(hour=start_hour, minute=start_minute, second=0)
        ex_end = ex_start + timedelta(minutes=duration)
        conn.execute(
            "INSERT OR IGNORE INTO exercise_sessions (exercise_type, exercise_start, exercise_end, duration_minutes) VALUES (?, ?, ?, ?)",
            (ex_type, ex_start.isoformat(), ex_end.isoformat(), duration),
        )


def generate():
    init_db()
    conn = get_connection()
    conn.execute("PRAGMA foreign_keys = ON")

    conn.execute("DELETE FROM sleep_stages")
    conn.execute("DELETE FROM sleep_sessions")
    conn.execute("DELETE FROM steps_hourly")
    conn.execute("DELETE FROM heart_rate_hourly")
    conn.execute("DELETE FROM exercise_sessions")

    base_date = datetime(2026, 1, 15)
    num_days = 30

    # Sleep data
    for day_offset in range(num_days):
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

    # Steps, heart rate, exercise
    generate_steps(conn, base_date, num_days)
    generate_heart_rate(conn, base_date, num_days)
    generate_exercise(conn, base_date, num_days)

    conn.commit()

    session_count = conn.execute("SELECT COUNT(*) FROM sleep_sessions").fetchone()[0]
    stage_count = conn.execute("SELECT COUNT(*) FROM sleep_stages").fetchone()[0]
    step_count = conn.execute("SELECT COUNT(*) FROM steps_hourly").fetchone()[0]
    hr_count = conn.execute("SELECT COUNT(*) FROM heart_rate_hourly").fetchone()[0]
    ex_count = conn.execute("SELECT COUNT(*) FROM exercise_sessions").fetchone()[0]
    conn.close()
    print(f"Inserted into health.db:")
    print(f"  {session_count} sleep sessions with {stage_count} stages")
    print(f"  {step_count} steps_hourly records")
    print(f"  {hr_count} heart_rate_hourly records")
    print(f"  {ex_count} exercise sessions")


if __name__ == "__main__":
    generate()
