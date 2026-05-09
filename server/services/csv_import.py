from __future__ import annotations

import csv
import io
import sys
from bisect import bisect_right
from collections import Counter, defaultdict
from datetime import datetime, timezone
from uuid import UUID

from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from sqlalchemy import select

from server.db.models import ExerciseSession, HeartRateHourly, SleepSession, SleepStage, StepsHourly
from server.logging_config import get_logger

_log = get_logger(__name__)

MAX_CSV_BYTES = 100 * 1024 * 1024

_SLEEP_STAGE_MAP: dict[int, str] = {
    40001: "AWAKE",
    40002: "LIGHT",
    40003: "DEEP",
    40004: "REM",
}

_EXERCISE_TYPE_MAP: dict[int, str] = {
    1001: "running",
    1002: "cycling",
    1007: "walking",
    1008: "hiking",
    3000: "swimming",
    90001: "indoor_cycling",
}


def parse_samsung_csv(raw_bytes: bytes) -> list[dict]:
    csv.field_size_limit(sys.maxsize)
    # utf-8-sig strips the BOM (U+FEFF) that Samsung Health prepends to every CSV
    text = raw_bytes.decode("utf-8-sig")
    lines = [ln for ln in text.splitlines() if not ln.startswith("#")]
    if not lines:
        return []
    # Samsung Health CSVs start with a metadata line: namespace,user_id,version
    # The second field is always a numeric user ID — use that to detect it.
    parts = lines[0].split(",", 2)
    if len(parts) >= 2 and parts[1].strip().isdigit():
        lines = lines[1:]
    if not lines:
        return []
    try:
        reader = csv.DictReader(io.StringIO("\n".join(lines)))
        return list(reader)
    except csv.Error as exc:
        raise ValueError(f"malformed_csv: {exc}") from exc


def _parse_ts(value: str) -> datetime:
    v = value.strip()
    if "." in v:
        dt = datetime.strptime(v, "%Y-%m-%d %H:%M:%S.%f")
    else:
        dt = datetime.strptime(v, "%Y-%m-%d %H:%M:%S")
    return dt.replace(tzinfo=timezone.utc)


def parse_sleep_rows(rows: list[dict], user_id: UUID, db: Session) -> tuple[int, int]:
    inserted = 0
    skipped = 0
    skip_reasons: Counter = Counter()

    for row in rows:
        try:
            start = _parse_ts(row["com.samsung.health.sleep.start_time"])
            end = _parse_ts(row["com.samsung.health.sleep.end_time"])
        except KeyError:
            skip_reasons["missing_field"] += 1
            skipped += 1
            continue
        except (ValueError, TypeError):
            skip_reasons["invalid_date"] += 1
            skipped += 1
            continue

        stmt = (
            pg_insert(SleepSession)
            .values(user_id=user_id, sleep_start=start, sleep_end=end)
            .on_conflict_do_nothing(index_elements=["user_id", "sleep_start", "sleep_end"])
            .returning(SleepSession.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1

    db.commit()
    if skip_reasons:
        _log.warning("csv_import.rows_skipped", endpoint="sleep", count=sum(skip_reasons.values()), reasons=dict(skip_reasons), user_id=str(user_id))
    return inserted, skipped


_STAGE_INSERT_BATCH = 1000


def parse_sleep_stage_rows(rows: list[dict], user_id: UUID, db: Session) -> tuple[int, int]:
    inserted = 0
    skipped = 0
    skip_reasons: Counter = Counter()

    sessions = db.execute(
        select(SleepSession.id, SleepSession.sleep_start, SleepSession.sleep_end)
        .where(SleepSession.user_id == user_id)
        .order_by(SleepSession.sleep_start)
    ).all()
    starts = [s.sleep_start for s in sessions]

    def find_session_id(stage_start: datetime, stage_end: datetime):
        # bisect_right(starts, stage_start) - 1 = index of last session with sleep_start <= stage_start
        idx = bisect_right(starts, stage_start) - 1
        if idx < 0:
            return None
        s = sessions[idx]
        if s.sleep_end >= stage_end:
            return s.id
        return None

    pending: list[dict] = []

    def flush() -> tuple[int, int]:
        nonlocal pending
        attempted = len(pending)
        if attempted == 0:
            return 0, 0
        stmt = (
            pg_insert(SleepStage)
            .values(pending)
            .on_conflict_do_nothing(index_elements=["user_id", "stage_start", "stage_end"])
            .returning(SleepStage.id)
        )
        ids = db.execute(stmt).all()
        pending = []
        ins = len(ids)
        return ins, attempted - ins

    for row in rows:
        try:
            start = _parse_ts(row["start_time"])
            end = _parse_ts(row["end_time"])
            stage_code = int(row["stage"])
        except (KeyError, ValueError, TypeError):
            skip_reasons["missing_or_invalid_field"] += 1
            skipped += 1
            continue

        stage_type = _SLEEP_STAGE_MAP.get(stage_code)
        if stage_type is None:
            skip_reasons["unknown_stage_code"] += 1
            skipped += 1
            continue

        session_id = find_session_id(start, end)
        if session_id is None:
            skip_reasons["no_matching_session"] += 1
            skipped += 1
            continue

        pending.append({
            "user_id": user_id,
            "session_id": session_id,
            "stage_type": stage_type,
            "stage_start": start,
            "stage_end": end,
        })

        if len(pending) >= _STAGE_INSERT_BATCH:
            ins, conflicts = flush()
            inserted += ins
            skipped += conflicts
            if conflicts:
                skip_reasons["conflict"] += conflicts

    ins, conflicts = flush()
    inserted += ins
    skipped += conflicts
    if conflicts:
        skip_reasons["conflict"] += conflicts

    db.commit()
    if skip_reasons:
        _log.warning("csv_import.rows_skipped", endpoint="sleep_stage", count=sum(skip_reasons.values()), reasons=dict(skip_reasons), user_id=str(user_id))
    return inserted, skipped


def parse_heartrate_rows(rows: list[dict], user_id: UUID, db: Session) -> tuple[int, int]:
    skip_reasons: Counter = Counter()
    slots: dict[tuple[str, int], list[tuple[int, int, int]]] = defaultdict(list)

    for row in rows:
        try:
            ts = _parse_ts(row["com.samsung.health.heart_rate.start_time"])
            bpm = round(float(row["com.samsung.health.heart_rate.heart_rate"]))
            mn = round(float(row["com.samsung.health.heart_rate.min"]))
            mx = round(float(row["com.samsung.health.heart_rate.max"]))
        except KeyError:
            skip_reasons["missing_field"] += 1
            continue
        except (ValueError, TypeError):
            skip_reasons["invalid_value"] += 1
            continue
        date_str = ts.strftime("%Y-%m-%d")
        slots[(date_str, ts.hour)].append((bpm, mn, mx))

    inserted = 0
    skipped = 0
    for (date_str, hour), samples in slots.items():
        avg_bpm = round(sum(s[0] for s in samples) / len(samples))
        min_bpm = min(s[1] for s in samples)
        max_bpm = max(s[2] for s in samples)
        sample_count = len(samples)
        stmt = (
            pg_insert(HeartRateHourly)
            .values(
                user_id=user_id,
                date=date_str,
                hour=hour,
                min_bpm=min_bpm,
                max_bpm=max_bpm,
                avg_bpm=avg_bpm,
                sample_count=sample_count,
            )
            .on_conflict_do_nothing(index_elements=["user_id", "date", "hour"])
            .returning(HeartRateHourly.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()

    if skip_reasons:
        _log.warning("csv_import.rows_skipped", endpoint="heartrate", count=sum(skip_reasons.values()), reasons=dict(skip_reasons), user_id=str(user_id))
    return inserted, skipped


def parse_steps_rows(rows: list[dict], user_id: UUID, db: Session) -> tuple[int, int]:
    skip_reasons: Counter = Counter()
    slots: dict[tuple[str, int], int] = defaultdict(int)

    for row in rows:
        try:
            # Samsung Health export: day_time is a Unix timestamp in milliseconds
            ts_ms = int(row["day_time"])
            ts = datetime.fromtimestamp(ts_ms / 1000.0, tz=timezone.utc)
            count = int(row["count"])
        except KeyError:
            # Fallback: legacy com.samsung.health.* column names
            try:
                ts = _parse_ts(row["com.samsung.health.step_daily_trend.start_time"])
                count = int(row["com.samsung.health.step_daily_trend.count"])
            except KeyError:
                skip_reasons["missing_field"] += 1
                continue
            except (ValueError, TypeError):
                skip_reasons["invalid_value"] += 1
                continue
        except (ValueError, TypeError):
            skip_reasons["invalid_value"] += 1
            continue
        date_str = ts.strftime("%Y-%m-%d")
        slots[(date_str, ts.hour)] += count

    inserted = 0
    skipped = 0
    for (date_str, hour), step_count in slots.items():
        stmt = (
            pg_insert(StepsHourly)
            .values(user_id=user_id, date=date_str, hour=hour, step_count=step_count)
            .on_conflict_do_nothing(index_elements=["user_id", "date", "hour"])
            .returning(StepsHourly.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()

    if skip_reasons:
        _log.warning("csv_import.rows_skipped", endpoint="steps", count=sum(skip_reasons.values()), reasons=dict(skip_reasons), user_id=str(user_id))
    return inserted, skipped


def parse_exercise_rows(rows: list[dict], user_id: UUID, db: Session) -> tuple[int, int]:
    inserted = 0
    skipped = 0
    skip_reasons: Counter = Counter()

    for row in rows:
        try:
            start = _parse_ts(row["com.samsung.health.exercise.start_time"])
            end = _parse_ts(row["com.samsung.health.exercise.end_time"])
            type_code = int(row["com.samsung.health.exercise.exercise_type"])
            duration_ms = float(row["com.samsung.health.exercise.duration"])
        except KeyError:
            skip_reasons["missing_field"] += 1
            skipped += 1
            continue
        except (ValueError, TypeError):
            skip_reasons["invalid_value"] += 1
            skipped += 1
            continue

        exercise_type = _EXERCISE_TYPE_MAP.get(type_code, f"samsung_{type_code}")
        duration_minutes = duration_ms / 60000.0

        stmt = (
            pg_insert(ExerciseSession)
            .values(
                user_id=user_id,
                exercise_type=exercise_type,
                exercise_start=start,
                exercise_end=end,
                duration_minutes=duration_minutes,
            )
            .on_conflict_do_nothing(index_elements=["user_id", "exercise_start", "exercise_end"])
            .returning(ExerciseSession.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()

    if skip_reasons:
        _log.warning("csv_import.rows_skipped", endpoint="exercise", count=sum(skip_reasons.values()), reasons=dict(skip_reasons), user_id=str(user_id))
    return inserted, skipped
