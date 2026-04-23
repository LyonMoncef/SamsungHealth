---
type: code-source
language: python
file_path: scripts/import_samsung_csv.py
git_blob: 038f9c4235e77939e91c817f7b788b78353b595f
last_synced: '2026-04-23T08:43:08Z'
loc: 690
annotations: []
imports:
- 'v

  i'
- 's

  f'
- llections i
- tetime i
- thlib i
- rver.database i
exports:
- f matche
- ")\n    if"
- .g
- fmt in (
- " try:\n    "
- 'one



  de'
- 'None


  '
- ──────
- UUID namespa
- "p_sessions\")\n    }\n"
- '"com.samsung.health'
- IGNORE INTO steps_
- ' to_float(fv(row, f"{pfx'
- type  = fv(row,
- "xecute(\n     "
- "e\"))\n      "
- '       cur = conn.execu'
- "\n         "
- '       cur = conn.execu'
- '          (st'
- ime, height_c
- "cur = conn.execute(\n "
- 'mood_type, '
- er_intake (start_ti
- '   cur = conn.execute'
- "e\"))\n        if not d"
- RE INTO floors_dail
- O activity_level (sta
- "\"\"\"\n      "
- rt_s
tags:
- code
- python
---

# scripts/import_samsung_csv.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`scripts/import_samsung_csv.py`](../../../scripts/import_samsung_csv.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Import Samsung Health CSV export into health.db.
Idempotent — uses INSERT OR IGNORE / upsert throughout.

Usage:
    python3 scripts/import_samsung_csv.py [export_dir]

Default export_dir: /mnt/c/Users/idsmf/Desktop/SamsungHealth
"""

import csv
import sys
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from server.database import DB_PATH, init_db, get_connection

EXPORT_DIR = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/mnt/c/Users/idsmf/Desktop/SamsungHealth")

# Samsung shealth CSV stage codes (different from Health Connect 1-4)
SLEEP_STAGE_MAP = {40001: "awake", 40002: "light", 40003: "deep", 40004: "rem"}


# ── CSV helpers ──────────────────────────────────────────────────────────────

def find_csv(name_pattern: str) -> Path | None:
    matches = sorted(EXPORT_DIR.glob(f"{name_pattern}.*.csv"))
    return matches[0] if matches else None


def read_csv(name_pattern: str):
    """Yield DictReader rows, skipping Samsung's metadata line 1."""
    path = find_csv(name_pattern)
    if path is None:
        return
    with open(path, encoding="utf-8-sig", newline="") as f:
        f.readline()  # skip: "table_name,id,count"
        reader = csv.DictReader(f)
        for row in reader:
            # DictReader puts overflow values (trailing comma) under key None
            yield {k: v for k, v in row.items() if k is not None}


def fv(row: dict, *keys, default=None):
    """Return first non-empty value from the given keys."""
    for k in keys:
        v = row.get(k, "")
        if v not in ("", None):
            return v
    return default


def parse_dt(s) -> str | None:
    """Samsung datetime → ISO 8601 (no ms, no tz suffix)."""
    if not s:
        return None
    for fmt in ("%Y-%m-%d %H:%M:%S.%f", "%Y-%m-%d %H:%M:%S", "%Y-%m-%d"):
        try:
            return datetime.strptime(s, fmt).strftime("%Y-%m-%dT%H:%M:%S")
        except ValueError:
            pass
    return None


def ms_to_date(ms_str) -> str | None:
    """Unix ms integer → YYYY-MM-DD (UTC)."""
    if ms_str in (None, ""):
        return None
    try:
        ms = int(float(ms_str))
        return datetime.fromtimestamp(ms / 1000, tz=timezone.utc).strftime("%Y-%m-%d")
    except (ValueError, OSError):
        return None


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


# ── importers ────────────────────────────────────────────────────────────────

def import_sleep(conn) -> dict:
    """
    Import sleep sessions from com.samsung.shealth.sleep (1242 rows, HC-prefixed columns).
    Returns HC datauuid → (sleep_start, sleep_end) map for stages FK resolution.
    sleep_combined only has 155 rows and uses a different UUID namespace.
    """
    pfx = "com.samsung.health.sleep."
    uuid_map: dict[str, tuple[str, str]] = {}
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.sleep"):
        start = parse_dt(fv(row, f"{pfx}start_time"))
        end   = parse_dt(fv(row, f"{pfx}end_time"))
        if not start or not end:
            continue
        uuid = fv(row, f"{pfx}datauuid")
        if uuid:
            uuid_map[uuid] = (start, end)
        cur = conn.execute("""
            INSERT INTO sleep_sessions
                (sleep_start, sleep_end, sleep_score, efficiency, sleep_duration_min,
                 sleep_cycle, mental_recovery, physical_recovery, sleep_type)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(sleep_start, sleep_end) DO UPDATE SET
                sleep_score        = excluded.sleep_score,
                efficiency         = excluded.efficiency,
                sleep_duration_min = excluded.sleep_duration_min,
                sleep_cycle        = excluded.sleep_cycle,
                mental_recovery    = excluded.mental_recovery,
                physical_recovery  = excluded.physical_recovery,
                sleep_type         = excluded.sleep_type
        """, (
            start, end,
            to_int(fv(row, "sleep_score")),
            to_float(fv(row, "efficiency")),
            to_int(fv(row, "sleep_duration")),
            to_int(fv(row, "sleep_cycle")),
            to_float(fv(row, "mental_recovery")),
            to_float(fv(row, "physical_recovery")),
            to_int(fv(row, "sleep_type")),
        ))
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("sleep_sessions", ins, skp)
    return uuid_map


def import_sleep_stages(conn, uuid_map: dict) -> None:
    # Build (start, end) → session_id from the DB
    session_id_map: dict[tuple, int] = {
        (r["sleep_start"], r["sleep_end"]): r["id"]
        for r in conn.execute("SELECT id, sleep_start, sleep_end FROM sleep_sessions")
    }

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
        end   = parse_dt(fv(row, "end_time"))
        if not start or not end:
            skp += 1
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO sleep_stages (session_id, stage_type, stage_start, stage_end) VALUES (?,?,?,?)",
            (session_id, stage_type, start, end),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("sleep_stages", ins, skp)


def import_steps_hourly(conn) -> None:
    buckets: dict[tuple, int] = defaultdict(int)
    for row in read_csv("com.samsung.shealth.tracker.pedometer_step_count"):
        start = parse_dt(fv(row, "com.samsung.health.step_count.start_time"))
        count = to_int(fv(row, "com.samsung.health.step_count.count"))
        if not start or count is None:
            continue
        dt = datetime.fromisoformat(start)
        buckets[(dt.strftime("%Y-%m-%d"), dt.hour)] += count
    ins = skp = 0
    for (date, hour), total in buckets.items():
        cur = conn.execute(
            "INSERT OR IGNORE INTO steps_hourly (date, hour, step_count) VALUES (?,?,?)",
            (date, hour, total),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("steps_hourly", ins, skp)


def import_steps_daily(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.tracker.pedometer_day_summary"):
        day = ms_to_date(fv(row, "day_time"))
        if not day:
            continue
        cur = conn.execute("""
            INSERT OR IGNORE INTO steps_daily
                (day_date, step_count, walk_step_count, run_step_count, distance_m, calorie_kcal, active_time_ms)
            VALUES (?,?,?,?,?,?,?)
        """, (
            day,
            to_int(fv(row, "step_count")),
            to_int(fv(row, "walk_step_count")),
            to_int(fv(row, "run_step_count")),
            to_float(fv(row, "distance")),
            to_float(fv(row, "calorie")),
            to_int(fv(row, "active_time")),
        ))
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("steps_daily", ins, skp)


def import_heart_rate_hourly(conn) -> None:
    buckets: dict[tuple, list] = defaultdict(list)
    pfx = "com.samsung.health.heart_rate."
    for row in read_csv("com.samsung.shealth.tracker.heart_rate"):
        start = parse_dt(fv(row, f"{pfx}start_time"))
        bpm   = to_float(fv(row, f"{pfx}heart_rate"))
        if not start or bpm is None:
            continue
        dt = datetime.fromisoformat(start)
        buckets[(dt.strftime("%Y-%m-%d"), dt.hour)].append(bpm)
    ins = skp = 0
    for (date, hour), bpms in buckets.items():
        cur = conn.execute(
            "INSERT OR IGNORE INTO heart_rate_hourly (date, hour, min_bpm, max_bpm, avg_bpm, sample_count) VALUES (?,?,?,?,?,?)",
            (date, hour, round(min(bpms)), round(max(bpms)), round(sum(bpms) / len(bpms)), len(bpms)),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("heart_rate_hourly", ins, skp)


def import_exercise(conn) -> None:
    pfx = "com.samsung.health.exercise."
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.exercise"):
        start    = parse_dt(fv(row, f"{pfx}start_time"))
        end      = parse_dt(fv(row, f"{pfx}end_time"))
        ex_type  = fv(row, f"{pfx}exercise_type", default="unknown")
        duration_ms = to_int(fv(row, f"{pfx}duration"))
        if not start or not end:
            continue
        dur_min = round(duration_ms / 60000, 2) if duration_ms else 0.0
        cur = conn.execute("""
            INSERT INTO exercise_sessions
                (exercise_type, exercise_start, exercise_end, duration_minutes,
                 calorie_kcal, distance_m, mean_heart_rate, max_heart_rate, min_heart_rate, mean_speed_ms)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(exercise_start, exercise_end) DO UPDATE SET
                calorie_kcal    = excluded.calorie_kcal,
                distance_m      = excluded.distance_m,
                mean_heart_rate = excluded.mean_heart_rate,
                max_heart_rate  = excluded.max_heart_rate,
                min_heart_rate  = excluded.min_heart_rate,
                mean_speed_ms   = excluded.mean_speed_ms
        """, (
            str(ex_type), start, end, dur_min,
            to_float(fv(row, f"{pfx}calorie")),
            to_float(fv(row, f"{pfx}distance")),
            to_float(fv(row, f"{pfx}mean_heart_rate")),
            to_float(fv(row, f"{pfx}max_heart_rate")),
            to_float(fv(row, f"{pfx}min_heart_rate")),
            to_float(fv(row, f"{pfx}mean_speed")),
        ))
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("exercise_sessions", ins, skp)


def import_stress(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.stress"):
        start = parse_dt(fv(row, "start_time"))
        end   = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO stress (start_time, end_time, score, tag_id) VALUES (?,?,?,?)",
            (start, end, to_float(fv(row, "score")), to_int(fv(row, "tag_id"))),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("stress", ins, skp)


def import_spo2(conn) -> None:
    pfx = "com.samsung.health.oxygen_saturation."
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.tracker.oxygen_saturation"):
        start = parse_dt(fv(row, f"{pfx}start_time"))
        end   = parse_dt(fv(row, f"{pfx}end_time"))
        if not start or not end:
            continue
        cur = conn.execute("""
            INSERT OR IGNORE INTO spo2 (start_time, end_time, spo2, min_spo2, max_spo2, low_duration_s, tag_id)
            VALUES (?,?,?,?,?,?,?)
        """, (
            start, end,
            to_float(fv(row, f"{pfx}spo2")),
            to_float(fv(row, f"{pfx}min")),
            to_float(fv(row, f"{pfx}max")),
            to_int(fv(row, f"{pfx}low_duration")),
            to_int(fv(row, "tag_id")),
        ))
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("spo2", ins, skp)


def import_respiratory_rate(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.respiratory_rate"):
        start = parse_dt(fv(row, "start_time"))
        end   = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO respiratory_rate (start_time, end_time, average, lower_limit, upper_limit) VALUES (?,?,?,?,?)",
            (start, end, to_float(fv(row, "average")), to_float(fv(row, "lower_limit")), to_float(fv(row, "upper_limit"))),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("respiratory_rate", ins, skp)


def import_hrv(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.hrv"):
        start = parse_dt(fv(row, "start_time"))
        end   = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO hrv (start_time, end_time) VALUES (?,?)",
            (start, end),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("hrv", ins, skp)


def import_skin_temperature(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.skin_temperature"):
        start = parse_dt(fv(row, "start_time"))
        end   = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO skin_temperature (start_time, end_time, temperature, min_temp, max_temp, tag_id) VALUES (?,?,?,?,?,?)",
            (start, end, to_float(fv(row, "temperature")), to_float(fv(row, "min")), to_float(fv(row, "max")), to_int(fv(row, "tag_id"))),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("skin_temperature", ins, skp)


def import_weight(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.weight"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        cur = conn.execute("""
            INSERT OR IGNORE INTO weight
                (start_time, weight_kg, body_fat_pct, skeletal_muscle_pct,
                 skeletal_muscle_mass_kg, fat_free_mass_kg, basal_metabolic_rate, total_body_water_kg)
            VALUES (?,?,?,?,?,?,?,?)
        """, (
            start,
            to_float(fv(row, "weight")),
            to_float(fv(row, "body_fat")),
            to_float(fv(row, "skeletal_muscle")),
            to_float(fv(row, "skeletal_muscle_mass")),
            to_float(fv(row, "fat_free_mass")),
            to_int(fv(row, "basal_metabolic_rate")),
            to_float(fv(row, "total_body_water")),
        ))
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("weight", ins, skp)


def import_height(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.height"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO height (start_time, height_cm) VALUES (?,?)",
            (start, to_float(fv(row, "height"))),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("height", ins, skp)


def import_blood_pressure(conn) -> None:
    pfx = "com.samsung.health.blood_pressure."
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.blood_pressure"):
        start = parse_dt(fv(row, f"{pfx}start_time"))
        if not start:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO blood_pressure (start_time, systolic, diastolic, pulse, mean_bp) VALUES (?,?,?,?,?)",
            (
                start,
                to_float(fv(row, f"{pfx}systolic")),
                to_float(fv(row, f"{pfx}diastolic")),
                to_int(fv(row, f"{pfx}pulse")),
                to_float(fv(row, f"{pfx}mean")),
            ),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("blood_pressure", ins, skp)


def import_mood(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.mood"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO mood (start_time, mood_type, emotions, factors, notes, place, company) VALUES (?,?,?,?,?,?,?)",
            (
                start,
                to_int(fv(row, "mood_type")),
                fv(row, "emotions"),
                fv(row, "factors"),
                fv(row, "notes"),
                fv(row, "place"),
                fv(row, "company"),
            ),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("mood", ins, skp)


def import_water_intake(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.water_intake"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO water_intake (start_time, amount_ml) VALUES (?,?)",
            (start, to_float(fv(row, "amount"))),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("water_intake", ins, skp)


def import_activity_daily(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.activity.day_summary"):
        day = parse_dt(fv(row, "day_time"))
        if day:
            day = day[:10]  # date only
        if not day:
            continue
        cur = conn.execute("""
            INSERT OR IGNORE INTO activity_daily
                (day_date, step_count, distance_m, calorie_kcal, exercise_time_ms, active_time_ms, floor_count, score)
            VALUES (?,?,?,?,?,?,?,?)
        """, (
            day,
            to_int(fv(row, "step_count")),
            to_float(fv(row, "distance")),
            to_float(fv(row, "calorie")),
            to_int(fv(row, "exercise_time")),
            to_int(fv(row, "active_time")),
            to_float(fv(row, "floor_count")),
            to_int(fv(row, "score")),
        ))
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("activity_daily", ins, skp)


def import_vitality_score(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.vitality_score"):
        day = parse_dt(fv(row, "day_time"))
        if day:
            day = day[:10]
        if not day:
            day = ms_to_date(fv(row, "day_time"))
        if not day:
            continue
        cur = conn.execute("""
            INSERT OR IGNORE INTO vitality_score
                (day_date, total_score, sleep_score, sleep_balance, sleep_regularity, sleep_timing,
                 activity_score, active_time_ms, mvpa_time_ms, shr_score, shr_value, shrv_score, shrv_value)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, (
            day,
            to_float(fv(row, "total_score")),
            to_float(fv(row, "sleep_score")),
            to_float(fv(row, "sleep_balance")),
            to_float(fv(row, "sleep_regularity")),
            to_float(fv(row, "sleep_timing")),
            to_float(fv(row, "activity_score")),
            to_int(fv(row, "active_time")),
            to_int(fv(row, "mvpa_time")),
            to_float(fv(row, "shr_score")),
            to_float(fv(row, "shr_value")),
            to_float(fv(row, "shrv_score")),
            to_float(fv(row, "shrv_value")),
        ))
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("vitality_score", ins, skp)


def import_floors_daily(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.tracker.floors_day_summary"):
        day = ms_to_date(fv(row, "day_time"))
        if not day:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO floors_daily (day_date, floor_count) VALUES (?,?)",
            (day, to_int(fv(row, "floor_count"))),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("floors_daily", ins, skp)


def import_activity_level(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.shealth.activity_level"):
        start = parse_dt(fv(row, "start_time"))
        if not start:
            continue
        cur = conn.execute(
            "INSERT OR IGNORE INTO activity_level (start_time, activity_level) VALUES (?,?)",
            (start, to_int(fv(row, "activity_level"))),
        )
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("activity_level", ins, skp)


def import_ecg(conn) -> None:
    ins = skp = 0
    for row in read_csv("com.samsung.health.ecg"):
        start = parse_dt(fv(row, "start_time"))
        end   = parse_dt(fv(row, "end_time"))
        if not start or not end:
            continue
        cur = conn.execute("""
            INSERT OR IGNORE INTO ecg (start_time, end_time, mean_heart_rate, sample_frequency, sample_count, classification)
            VALUES (?,?,?,?,?,?)
        """, (
            start, end,
            to_float(fv(row, "mean_heart_rate")),
            to_int(fv(row, "sample_frequency")),
            to_int(fv(row, "sample_count")),
            to_int(fv(row, "classification")),
        ))
        if cur.rowcount:
            ins += 1
        else:
            skp += 1
    conn.commit()
    report("ecg", ins, skp)


# ── main ─────────────────────────────────────────────────────────────────────

def main() -> None:
    if not EXPORT_DIR.exists():
        print(f"ERROR: export dir not found: {EXPORT_DIR}", file=sys.stderr)
        sys.exit(1)

    print(f"DB: {DB_PATH}")
    print(f"Export: {EXPORT_DIR}")
    print("Initialising schema …")
    init_db()
    print()

    conn = get_connection()
    conn.execute("PRAGMA foreign_keys = ON")

    print("Importing …")
    uuid_map = import_sleep(conn)
    import_sleep_stages(conn, uuid_map)
    import_steps_hourly(conn)
    import_steps_daily(conn)
    import_heart_rate_hourly(conn)
    import_exercise(conn)
    import_stress(conn)
    import_spo2(conn)
    import_respiratory_rate(conn)
    import_hrv(conn)
    import_skin_temperature(conn)
    import_weight(conn)
    import_height(conn)
    import_blood_pressure(conn)
    import_mood(conn)
    import_water_intake(conn)
    import_activity_daily(conn)
    import_vitality_score(conn)
    import_floors_daily(conn)
    import_activity_level(conn)
    import_ecg(conn)

    conn.close()
    print("\nDone.")


if __name__ == "__main__":
    main()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `f matche` (function) — lines 28-30
- `)
    if` (function) — lines 33-43
- `.g` (function) — lines 46-52
- `fmt in (` (function) — lines 55-64
- ` try:
    ` (function) — lines 67-75
- `one


de` (function) — lines 78-82
- `None

` (function) — lines 85-89
- `──────` (function) — lines 92-93
- `UUID namespa` (function) — lines 98-144
- `p_sessions")
    }
` (function) — lines 147-181
- `"com.samsung.health` (function) — lines 184-204
- `IGNORE INTO steps_` (function) — lines 207-231
- ` to_float(fv(row, f"{pfx` (function) — lines 234-255
- `type  = fv(row,` (function) — lines 258-295
- `xecute(
     ` (function) — lines 298-314
- `e"))
      ` (function) — lines 317-341
- `       cur = conn.execu` (function) — lines 344-360
- `
         ` (function) — lines 363-379
- `       cur = conn.execu` (function) — lines 382-398
- `          (st` (function) — lines 401-427
- `ime, height_c` (function) — lines 430-445
- `cur = conn.execute(
 ` (function) — lines 448-470
- `mood_type, ` (function) — lines 473-496
- `er_intake (start_ti` (function) — lines 499-514
- `   cur = conn.execute` (function) — lines 517-544
- `e"))
        if not d` (function) — lines 547-582
- `RE INTO floors_dail` (function) — lines 585-600
- `O activity_level (sta` (function) — lines 603-618
- `"""
      ` (function) — lines 621-643
- `rt_s` (function) — lines 648-686

### Imports
- `v
i`
- `s
f`
- `llections i`
- `tetime i`
- `thlib i`
- `rver.database i`

### Exports
- `f matche`
- `)
    if`
- `.g`
- `fmt in (`
- ` try:
    `
- `one


de`
- `None

`
- `──────`
- `UUID namespa`
- `p_sessions")
    }
`
- `"com.samsung.health`
- `IGNORE INTO steps_`
- ` to_float(fv(row, f"{pfx`
- `type  = fv(row,`
- `xecute(
     `
- `e"))
      `
- `       cur = conn.execu`
- `
         `
- `       cur = conn.execu`
- `          (st`
- `ime, height_c`
- `cur = conn.execute(
 `
- `mood_type, `
- `er_intake (start_ti`
- `   cur = conn.execute`
- `e"))
        if not d`
- `RE INTO floors_dail`
- `O activity_level (sta`
- `"""
      `
- `rt_s`
