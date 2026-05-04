#!/usr/bin/env python3
"""Generate ~30 days of realistic sample health data into Postgres (V2.1.2 SQLAlchemy).

Usage:
    python3 scripts/generate_sample.py [--user-email <email>]

Default user_email: legacy@samsunghealth.local (créé par alembic 0004 backfill).
"""

import argparse
import random
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import (
    ExerciseSession,
    HeartRateHourly,
    SleepSession,
    SleepStage,
    StepsHourly,
    User,
)


DEFAULT_LEGACY_EMAIL = "legacy@samsunghealth.local"


STAGE_TYPES = ["light", "deep", "rem", "awake"]
EXERCISE_TYPES = ["running", "walking", "cycling", "swimming", "hiking", "yoga", "strength_training"]


def generate_stages(sleep_start: datetime, sleep_end: datetime) -> list[dict]:
    """Generate realistic sleep stage cycles (~90 min each : light → deep → light → REM, occasional awake)."""
    stages = []
    cursor = sleep_start
    total_duration = (sleep_end - sleep_start).total_seconds()
    cycle_count = max(1, int(total_duration / 5400))

    for cycle in range(cycle_count):
        if cursor >= sleep_end:
            break
        for stage_type, dmin, dmax in (
            ("light", 10, 25),
            ("deep", 15, 40 if cycle < 2 else 20),
            ("light", 5, 15),
            ("rem", 10 if cycle < 2 else 20, 20 if cycle < 2 else 35),
        ):
            if cursor >= sleep_end:
                break
            duration = random.randint(dmin, dmax)
            end = min(cursor + timedelta(minutes=duration), sleep_end)
            stages.append({"stage_type": stage_type, "start": cursor, "end": end})
            cursor = end
        if cursor < sleep_end and random.random() < 0.3:
            duration = random.randint(1, 5)
            end = min(cursor + timedelta(minutes=duration), sleep_end)
            stages.append({"stage_type": "awake", "start": cursor, "end": end})
            cursor = end
    return stages


def _upsert_steps(db: Session, base_date: datetime, num_days: int, user_id: str) -> int:  # base_date timezone-aware
    inserted = 0
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
            stmt = (
                pg_insert(StepsHourly)
                .values(user_id=user_id, date=date_str, hour=hour, step_count=steps)
                .on_conflict_do_nothing(
                    index_elements=["user_id", "date", "hour"],
                )
                .returning(StepsHourly.id)
            )
            if db.execute(stmt).first() is not None:
                inserted += 1
    return inserted


def _upsert_heart_rate(db: Session, base_date: datetime, num_days: int, user_id: str) -> int:
    inserted = 0
    for day_offset in range(num_days):
        date = base_date + timedelta(days=day_offset)
        date_str = date.strftime("%Y-%m-%d")
        for hour in range(24):
            if hour < 6:
                avg, spread = random.randint(55, 65), random.randint(3, 8)
            elif hour < 9:
                avg, spread = random.randint(65, 80), random.randint(5, 15)
            elif hour < 18:
                avg, spread = random.randint(72, 95), random.randint(8, 25)
            elif hour < 22:
                avg, spread = random.randint(65, 82), random.randint(5, 15)
            else:
                avg, spread = random.randint(58, 70), random.randint(3, 10)
            min_bpm = max(40, avg - spread)
            max_bpm = min(180, avg + spread)
            stmt = (
                pg_insert(HeartRateHourly)
                .values(
                    user_id=user_id,
                    date=date_str,
                    hour=hour,
                    min_bpm=min_bpm,
                    max_bpm=max_bpm,
                    avg_bpm=avg,
                    sample_count=random.randint(5, 30),
                )
                .on_conflict_do_nothing(
                    index_elements=["user_id", "date", "hour"],
                )
                .returning(HeartRateHourly.id)
            )
            if db.execute(stmt).first() is not None:
                inserted += 1
    return inserted


def _upsert_exercise(db: Session, base_date: datetime, num_days: int, user_id: str) -> int:
    inserted = 0
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
        stmt = (
            pg_insert(ExerciseSession)
            .values(
                user_id=user_id,
                exercise_type=ex_type,
                exercise_start=ex_start,
                exercise_end=ex_end,
                duration_minutes=float(duration),
            )
            .on_conflict_do_nothing(
                index_elements=["user_id", "exercise_start", "exercise_end"],
            )
            .returning(ExerciseSession.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
    return inserted


def _resolve_target_user_id(db: Session, email: str) -> str:
    user = db.execute(select(User).where(User.email == email)).scalar_one_or_none()
    if user is None:
        raise SystemExit(
            f"ERROR: user '{email}' introuvable. Crée-le via /auth/register "
            f"(ou attends que la migration 0004 ait backfill le legacy user)."
        )
    return str(user.id)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--user-email",
        default=DEFAULT_LEGACY_EMAIL,
        help=f"Email of the target user (default: {DEFAULT_LEGACY_EMAIL})",
    )
    args = parser.parse_args()

    # Seed déterministe : permet l'idempotence (2 runs = mêmes timestamps → ON CONFLICT skip)
    random.seed(42)
    db = get_session()
    user_id = _resolve_target_user_id(db, args.user_email)
    print(f"Target user: {args.user_email} ({user_id})")
    base_date = datetime(2026, 1, 15, tzinfo=timezone.utc)
    num_days = 30

    # Phase 1 : générer TOUS les (sleep_session, stages[]) en mémoire.
    # Consommer le random linéairement, indépendamment de l'état DB → idempotence garantie.
    sleep_plan: list[tuple[datetime, datetime, list[dict]]] = []
    for day_offset in range(num_days):
        date = base_date + timedelta(days=day_offset)
        bed_hour = random.choice([22, 23, 0, 1])
        bed_minute = random.randint(0, 59)
        if bed_hour >= 22:
            sleep_start = date.replace(hour=bed_hour, minute=bed_minute, second=0)
        else:
            sleep_start = (date + timedelta(days=1)).replace(hour=bed_hour, minute=bed_minute, second=0)
        wake_hour = random.randint(6, 8)
        wake_minute = random.randint(0, 59)
        sleep_end = (
            (sleep_start + timedelta(days=1)).replace(hour=wake_hour, minute=wake_minute, second=0)
            if bed_hour >= 22
            else sleep_start.replace(hour=wake_hour, minute=wake_minute, second=0)
        )
        if sleep_end <= sleep_start:
            sleep_end += timedelta(days=1)
        stages = generate_stages(sleep_start, sleep_end)  # consomme random même si la session sera skippée
        sleep_plan.append((sleep_start, sleep_end, stages))

    # Phase 2 : insert via ON CONFLICT DO NOTHING (idempotent vs DB)
    from server.db.uuid7 import uuid7
    sleep_inserted = 0
    for sleep_start, sleep_end, stages in sleep_plan:
        new_uuid = uuid7()
        stmt = (
            pg_insert(SleepSession)
            .values(id=new_uuid, user_id=user_id, sleep_start=sleep_start, sleep_end=sleep_end)
            .on_conflict_do_nothing(
                index_elements=["user_id", "sleep_start", "sleep_end"],
            )
            .returning(SleepSession.id)
        )
        result = db.execute(stmt).first()
        if result is None:
            continue
        session_id = result[0]
        sleep_inserted += 1
        for st in stages:
            stage_stmt = (
                pg_insert(SleepStage)
                .values(
                    user_id=user_id,
                    session_id=session_id,
                    stage_type=st["stage_type"],
                    stage_start=st["start"],
                    stage_end=st["end"],
                )
                .on_conflict_do_nothing(
                    index_elements=["user_id", "stage_start", "stage_end"],
                )
            )
            db.execute(stage_stmt)

    steps_inserted = _upsert_steps(db, base_date, num_days, user_id)
    hr_inserted = _upsert_heart_rate(db, base_date, num_days, user_id)
    ex_inserted = _upsert_exercise(db, base_date, num_days, user_id)
    db.commit()

    print("Inserted into Postgres :")
    print(f"  {sleep_inserted} sleep sessions (avec stages)")
    print(f"  {steps_inserted} steps_hourly records")
    print(f"  {hr_inserted} heart_rate_hourly records")
    print(f"  {ex_inserted} exercise sessions")


if __name__ == "__main__":
    main()
