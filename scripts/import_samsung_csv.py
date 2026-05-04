"""
Import Samsung Health CSV export into Postgres (V2.1.2 SQLAlchemy refactor).

Idempotent — toutes les insertions utilisent `ON CONFLICT DO NOTHING`.

Usage:
    python3 scripts/import_samsung_csv.py [export_dir] [--user-email <email>]

Default export_dir: /mnt/c/Users/idsmf/Desktop/SamsungHealth
Default user_email: legacy@samsunghealth.local (créé par alembic 0004 backfill)
"""

import argparse
import csv
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import (
    ActivityDaily,
    ActivityLevel,
    BloodPressure,
    Ecg,
    ExerciseSession,
    FloorsDaily,
    HeartRateHourly,
    Height,
    Hrv,
    Mood,
    RespiratoryRate,
    SkinTemperature,
    SleepSession,
    SleepStage,
    Spo2,
    StepsDaily,
    StepsHourly,
    Stress,
    User,
    VitalityScore,
    WaterIntake,
    Weight,
)

DEFAULT_LEGACY_EMAIL = "legacy@samsunghealth.local"
DEFAULT_EXPORT_DIR = Path("/mnt/c/Users/idsmf/Desktop/SamsungHealth")

# Réinjectés par main() via argparse, exposés en module-global pour ne pas
# avoir à propager un user_id à chaque signature import_*(db).
EXPORT_DIR: Path = DEFAULT_EXPORT_DIR
TARGET_USER_ID: str | None = None

# Samsung shealth CSV stage codes (different from Health Connect 1-4)
SLEEP_STAGE_MAP = {40001: "awake", 40002: "light", 40003: "deep", 40004: "rem"}


# ── helpers ──────────────────────────────────────────────────────────────────


def find_csv(name_pattern: str) -> Path | None:
    matches = sorted(EXPORT_DIR.glob(f"{name_pattern}.*.csv"))
    return matches[0] if matches else None


def read_csv(name_pattern: str):
    path = find_csv(name_pattern)
    if path is None:
        return
    with open(path, encoding="utf-8-sig", newline="") as f:
        f.readline()  # skip Samsung's metadata line 1
        reader = csv.DictReader(f)
        for row in reader:
            yield {k: v for k, v in row.items() if k is not None}


def fv(row: dict, *keys, default=None):
    for k in keys:
        v = row.get(k, "")
        if v not in ("", None):
            return v
    return default


def parse_dt(s) -> datetime | None:
    """Samsung datetime string → datetime aware UTC."""
    if not s:
        return None
    for fmt in ("%Y-%m-%d %H:%M:%S.%f", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            return datetime.strptime(s, fmt).replace(tzinfo=timezone.utc)
        except ValueError:
            pass
    return None


def ms_to_date_str(ms_str) -> str | None:
    if ms_str in (None, ""):
        return None
    try:
        ms = int(float(ms_str))
        return datetime.fromtimestamp(ms / 1000, tz=timezone.utc).strftime("%Y-%m-%d")
    except (ValueError, OSError):
        return None


def parse_day(s) -> str | None:
    """Parse to YYYY-MM-DD string (10 chars) for *_daily tables."""
    dt = parse_dt(s)
    if dt is not None:
        return dt.strftime("%Y-%m-%d")
    return ms_to_date_str(s)


def to_float(s) -> float | None:
    try:
        return float(s) if s not in (None, "") else None
    except ValueError:
        return None


def to_int(s) -> int | None:
    try:
        return int(float(s)) if s not in (None, "") else None
    except ValueError:
        return None


def report(label: str, ins: int, skp: int) -> None:
    print(f"  {label:<35} inserted {ins:>6} | skipped {skp:>6}")


def _upsert(db: Session, model, values: dict, conflict_cols: list[str]) -> bool:
    """Insert via ON CONFLICT DO NOTHING. Returns True si insert effectif, False si skip.

    V2.3.0.1 — l'unique constraint matche `(user_id, ...cols)` pour permettre
    le multi-user. Le helper injecte `user_id=TARGET_USER_ID` côté serveur.
    """
    if TARGET_USER_ID is None:
        raise RuntimeError("TARGET_USER_ID not initialized — call main() first")

    stmt = (
        pg_insert(model)
        .values(user_id=TARGET_USER_ID, **values)
        .on_conflict_do_nothing(
            index_elements=["user_id", *conflict_cols],
        )
        .returning(model.id)
    )
    return db.execute(stmt).first() is not None


def _resolve_target_user_id(db: Session, email: str) -> str:
    user = db.execute(select(User).where(User.email == email)).scalar_one_or_none()
    if user is None:
        raise SystemExit(
            f"ERROR: user '{email}' introuvable. Crée-le via /auth/register "
            f"(ou attends que la migration 0004 ait backfill le legacy user)."
        )
    return str(user.id)


# ── importers ────────────────────────────────────────────────────────────────


def import_sleep(db: Session) -> dict:
    """Import sleep_sessions, retourne {datauuid: (sleep_start, sleep_end)} pour stages FK."""
    pfx = "com.samsung.health.sleep."
    uuid_map: dict[str, tuple[datetime, datetime]] = {}
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.sleep"):
        start = parse_dt(fv(row, f"{pfx}start_time"))
        end = parse_dt(fv(row, f"{pfx}end_time"))
        if not start or not end:
            continue
        uuid = fv(row, f"{pfx}datauuid")
        if uuid:
            uuid_map[uuid] = (start, end)
        values = dict(
            sleep_start=start,
            sleep_end=end,
            sleep_score=to_int(fv(row, "sleep_score")),
            efficiency=to_float(fv(row, "efficiency")),
            sleep_duration_min=to_int(fv(row, "sleep_duration")),
            sleep_cycle=to_int(fv(row, "sleep_cycle")),
            mental_recovery=to_float(fv(row, "mental_recovery")),
            physical_recovery=to_float(fv(row, "physical_recovery")),
            sleep_type=to_int(fv(row, "sleep_type")),
        )
        if _upsert(db, SleepSession, values, ["sleep_start", "sleep_end"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("sleep_sessions", ins, skp)
    return uuid_map


def import_sleep_stages(db: Session, uuid_map: dict) -> None:
    # Build (start, end) → session_id from PG
    rows = db.execute(select(SleepSession.id, SleepSession.sleep_start, SleepSession.sleep_end)).all()
    session_id_map = {(r.sleep_start, r.sleep_end): r.id for r in rows}

    ins = skp = 0
    for row in read_csv("com.samsung.health.sleep_stage"):
        sleep_uuid = fv(row, "sleep_id")
        if sleep_uuid not in uuid_map:
            skp += 1
            continue
        se = uuid_map[sleep_uuid]
        session_id = session_id_map.get(se)
        if session_id is None:
            skp += 1
            continue
        stage_int = to_int(fv(row, "stage"))
        stage_type = SLEEP_STAGE_MAP.get(stage_int, f"unknown_{stage_int}")
        start = parse_dt(fv(row, "start_time"))
        end = parse_dt(fv(row, "end_time"))
        if not start or not end:
            skp += 1
            continue
        values = dict(
            session_id=session_id,
            stage_type=stage_type,
            stage_start=start,
            stage_end=end,
        )
        if _upsert(db, SleepStage, values, ["stage_start", "stage_end"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("sleep_stages", ins, skp)


def import_steps_hourly(db: Session) -> None:
    buckets: dict[tuple, int] = defaultdict(int)
    for row in read_csv("com.samsung.shealth.tracker.pedometer_step_count"):
        start = parse_dt(fv(row, "com.samsung.health.step_count.start_time"))
        count = to_int(fv(row, "com.samsung.health.step_count.count"))
        if not start or count is None:
            continue
        buckets[(start.strftime("%Y-%m-%d"), start.hour)] += count
    ins = skp = 0
    for (date, hour), total in buckets.items():
        if _upsert(db, StepsHourly, dict(date=date, hour=hour, step_count=total), ["date", "hour"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("steps_hourly", ins, skp)


def import_steps_daily(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.tracker.pedometer_day_summary"):
        day = ms_to_date_str(fv(row, "day_time"))
        if not day:
            continue
        values = dict(
            day_date=day,
            step_count=to_int(fv(row, "step_count")),
            walk_step_count=to_int(fv(row, "walk_step_count")),
            run_step_count=to_int(fv(row, "run_step_count")),
            distance_m=to_float(fv(row, "distance")),
            calorie_kcal=to_float(fv(row, "calorie")),
            active_time_ms=to_int(fv(row, "active_time")),
        )
        if _upsert(db, StepsDaily, values, ["day_date"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("steps_daily", ins, skp)


def import_heart_rate_hourly(db: Session) -> None:
    buckets: dict[tuple, list] = defaultdict(list)
    pfx = "com.samsung.health.heart_rate."
    for row in read_csv("com.samsung.shealth.tracker.heart_rate"):
        start = parse_dt(fv(row, f"{pfx}start_time"))
        bpm = to_float(fv(row, f"{pfx}heart_rate"))
        if not start or bpm is None:
            continue
        buckets[(start.strftime("%Y-%m-%d"), start.hour)].append(bpm)
    ins = skp = 0
    for (date, hour), bpms in buckets.items():
        values = dict(
            date=date,
            hour=hour,
            min_bpm=round(min(bpms)),
            max_bpm=round(max(bpms)),
            avg_bpm=round(sum(bpms) / len(bpms)),
            sample_count=len(bpms),
        )
        if _upsert(db, HeartRateHourly, values, ["date", "hour"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("heart_rate_hourly", ins, skp)


def import_exercise(db: Session) -> None:
    pfx = "com.samsung.health.exercise."
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.exercise"):
        start = parse_dt(fv(row, f"{pfx}start_time"))
        end = parse_dt(fv(row, f"{pfx}end_time"))
        ex_type = fv(row, f"{pfx}exercise_type", default="unknown")
        duration_ms = to_int(fv(row, f"{pfx}duration"))
        if not start or not end:
            continue
        dur_min = round(duration_ms / 60000, 2) if duration_ms else 0.0
        values = dict(
            exercise_type=str(ex_type),
            exercise_start=start,
            exercise_end=end,
            duration_minutes=dur_min,
            calorie_kcal=to_float(fv(row, f"{pfx}calorie")),
            distance_m=to_float(fv(row, f"{pfx}distance")),
            mean_heart_rate=to_float(fv(row, f"{pfx}mean_heart_rate")),
            max_heart_rate=to_float(fv(row, f"{pfx}max_heart_rate")),
            min_heart_rate=to_float(fv(row, f"{pfx}min_heart_rate")),
            mean_speed_ms=to_float(fv(row, f"{pfx}mean_speed")),
        )
        if _upsert(db, ExerciseSession, values, ["exercise_start", "exercise_end"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("exercise_sessions", ins, skp)


def import_stress(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.stress"):
        start = parse_dt(fv(row, "start_time"))
        end = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        values = dict(
            start_time=start,
            end_time=end,
            score=to_float(fv(row, "score")),
            tag_id=to_int(fv(row, "tag_id")),
        )
        if _upsert(db, Stress, values, ["start_time", "end_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("stress", ins, skp)


def import_spo2(db: Session) -> None:
    pfx = "com.samsung.health.oxygen_saturation."
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.tracker.oxygen_saturation"):
        start = parse_dt(fv(row, f"{pfx}start_time"))
        end = parse_dt(fv(row, f"{pfx}end_time"))
        if not start or not end:
            continue
        values = dict(
            start_time=start,
            end_time=end,
            spo2=to_float(fv(row, f"{pfx}spo2")),
            min_spo2=to_float(fv(row, f"{pfx}min")),
            max_spo2=to_float(fv(row, f"{pfx}max")),
            low_duration_s=to_int(fv(row, f"{pfx}low_duration")),
            tag_id=to_int(fv(row, "tag_id")),
        )
        if _upsert(db, Spo2, values, ["start_time", "end_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("spo2", ins, skp)


def import_respiratory_rate(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.respiratory_rate"):
        start = parse_dt(fv(row, "start_time"))
        end = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        values = dict(
            start_time=start,
            end_time=end,
            average=to_float(fv(row, "average")),
            lower_limit=to_float(fv(row, "lower_limit")),
            upper_limit=to_float(fv(row, "upper_limit")),
        )
        if _upsert(db, RespiratoryRate, values, ["start_time", "end_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("respiratory_rate", ins, skp)


def import_hrv(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.hrv"):
        start = parse_dt(fv(row, "start_time"))
        end = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        if _upsert(db, Hrv, dict(start_time=start, end_time=end), ["start_time", "end_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("hrv", ins, skp)


def import_skin_temperature(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.skin_temperature"):
        start = parse_dt(fv(row, "start_time"))
        end = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        values = dict(
            start_time=start,
            end_time=end,
            temperature=to_float(fv(row, "temperature")),
            min_temp=to_float(fv(row, "min")),
            max_temp=to_float(fv(row, "max")),
            tag_id=to_int(fv(row, "tag_id")),
        )
        if _upsert(db, SkinTemperature, values, ["start_time", "end_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("skin_temperature", ins, skp)


def import_weight(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.weight"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        values = dict(
            start_time=start,
            weight_kg=to_float(fv(row, "weight")),
            body_fat_pct=to_float(fv(row, "body_fat")),
            skeletal_muscle_pct=to_float(fv(row, "skeletal_muscle")),
            skeletal_muscle_mass_kg=to_float(fv(row, "skeletal_muscle_mass")),
            fat_free_mass_kg=to_float(fv(row, "fat_free_mass")),
            basal_metabolic_rate=to_int(fv(row, "basal_metabolic_rate")),
            total_body_water_kg=to_float(fv(row, "total_body_water")),
        )
        if _upsert(db, Weight, values, ["start_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("weight", ins, skp)


def import_height(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.height"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        values = dict(start_time=start, height_cm=to_float(fv(row, "height")))
        if _upsert(db, Height, values, ["start_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("height", ins, skp)


def import_blood_pressure(db: Session) -> None:
    pfx = "com.samsung.health.blood_pressure."
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.blood_pressure"):
        start = parse_dt(fv(row, f"{pfx}start_time"))
        if not start:
            continue
        values = dict(
            start_time=start,
            systolic=to_float(fv(row, f"{pfx}systolic")),
            diastolic=to_float(fv(row, f"{pfx}diastolic")),
            pulse=to_int(fv(row, f"{pfx}pulse")),
            mean_bp=to_float(fv(row, f"{pfx}mean")),
        )
        if _upsert(db, BloodPressure, values, ["start_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("blood_pressure", ins, skp)


def import_mood(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.mood"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        values = dict(
            start_time=start,
            mood_type=to_int(fv(row, "mood_type")),
            emotions=fv(row, "emotions"),
            factors=fv(row, "factors"),
            notes=fv(row, "notes"),
            place=fv(row, "place"),
            company=fv(row, "company"),
        )
        if _upsert(db, Mood, values, ["start_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("mood", ins, skp)


def import_water_intake(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.water_intake"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        values = dict(start_time=start, amount_ml=to_float(fv(row, "amount")))
        if _upsert(db, WaterIntake, values, ["start_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("water_intake", ins, skp)


def import_activity_daily(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.activity.day_summary"):
        day = parse_day(fv(row, "day_time"))
        if not day:
            continue
        values = dict(
            day_date=day,
            step_count=to_int(fv(row, "step_count")),
            distance_m=to_float(fv(row, "distance")),
            calorie_kcal=to_float(fv(row, "calorie")),
            exercise_time_ms=to_int(fv(row, "exercise_time")),
            active_time_ms=to_int(fv(row, "active_time")),
            floor_count=to_float(fv(row, "floor_count")),
            score=to_int(fv(row, "score")),
        )
        if _upsert(db, ActivityDaily, values, ["day_date"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("activity_daily", ins, skp)


def import_vitality_score(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.vitality_score"):
        day = parse_day(fv(row, "day_time"))
        if not day:
            continue
        values = dict(
            day_date=day,
            total_score=to_float(fv(row, "total_score")),
            sleep_score=to_float(fv(row, "sleep_score")),
            sleep_balance=to_float(fv(row, "sleep_balance")),
            sleep_regularity=to_float(fv(row, "sleep_regularity")),
            sleep_timing=to_float(fv(row, "sleep_timing")),
            activity_score=to_float(fv(row, "activity_score")),
            active_time_ms=to_int(fv(row, "active_time")),
            mvpa_time_ms=to_int(fv(row, "mvpa_time")),
            shr_score=to_float(fv(row, "shr_score")),
            shr_value=to_float(fv(row, "shr_value")),
            shrv_score=to_float(fv(row, "shrv_score")),
            shrv_value=to_float(fv(row, "shrv_value")),
        )
        if _upsert(db, VitalityScore, values, ["day_date"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("vitality_score", ins, skp)


def import_floors_daily(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.tracker.floors_day_summary"):
        day = ms_to_date_str(fv(row, "day_time"))
        if not day:
            continue
        if _upsert(db, FloorsDaily, dict(day_date=day, floor_count=to_int(fv(row, "floor_count"))), ["day_date"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("floors_daily", ins, skp)


def import_activity_level(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.activity_level"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        if _upsert(db, ActivityLevel, dict(start_time=start, activity_level=to_int(fv(row, "activity_level"))), ["start_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("activity_level", ins, skp)


def import_ecg(db: Session) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.ecg"):
        start = parse_dt(fv(row, "start_time"))
        end = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        values = dict(
            start_time=start,
            end_time=end,
            mean_heart_rate=to_float(fv(row, "mean_heart_rate")),
            sample_frequency=to_int(fv(row, "sample_frequency")),
            sample_count=to_int(fv(row, "sample_count")),
            classification=to_int(fv(row, "classification")),
        )
        if _upsert(db, Ecg, values, ["start_time", "end_time"]):
            ins += 1
        else:
            skp += 1
    db.commit()
    report("ecg", ins, skp)


# ── main ─────────────────────────────────────────────────────────────────────


def main() -> None:
    global EXPORT_DIR, TARGET_USER_ID

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "export_dir",
        nargs="?",
        type=Path,
        default=DEFAULT_EXPORT_DIR,
        help=f"Samsung Health export directory (default: {DEFAULT_EXPORT_DIR})",
    )
    parser.add_argument(
        "--user-email",
        default=DEFAULT_LEGACY_EMAIL,
        help=f"Email of the target user (default: {DEFAULT_LEGACY_EMAIL})",
    )
    args = parser.parse_args()
    EXPORT_DIR = args.export_dir

    if not EXPORT_DIR.exists():
        print(f"ERROR: export dir not found: {EXPORT_DIR}", file=sys.stderr)
        sys.exit(1)

    print(f"Export: {EXPORT_DIR}")
    print("Importing into Postgres (alembic schema déjà appliqué via `make db-migrate`) …")
    db = get_session()
    TARGET_USER_ID = _resolve_target_user_id(db, args.user_email)
    print(f"Target user: {args.user_email} ({TARGET_USER_ID})")

    uuid_map = import_sleep(db)
    import_sleep_stages(db, uuid_map)
    import_steps_hourly(db)
    import_steps_daily(db)
    import_heart_rate_hourly(db)
    import_exercise(db)
    import_stress(db)
    import_spo2(db)
    import_respiratory_rate(db)
    import_hrv(db)
    import_skin_temperature(db)
    import_weight(db)
    import_height(db)
    import_blood_pressure(db)
    import_mood(db)
    import_water_intake(db)
    import_activity_daily(db)
    import_vitality_score(db)
    import_floors_daily(db)
    import_activity_level(db)
    import_ecg(db)

    db.close()
    print("\nDone.")


if __name__ == "__main__":
    main()
