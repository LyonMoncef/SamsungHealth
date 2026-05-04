---
type: spec
tags: [samsunghealth, data, schema, import]
created: 2026-04-21
status: reference
---

# Samsung Health CSV Export — Schema Reference

Export generated: 2026-04-21 · 56 CSV files  
Source: Samsung Health app → Export data  
Format: line 1 = `table_name,id,count` (metadata), line 2 = headers, line 3+ = data rows, trailing comma on each row.

---

## Import priority

| Priority | Table | Key fields | Use case |
|----------|-------|-----------|---------|
| ✅ P0 | `com.samsung.shealth.sleep` | `start_time`, `end_time`, `sleep_score`, `efficiency`, `sleep_duration` | Historical sleep sessions since 12/2023 |
| ✅ P0 | `com.samsung.health.sleep_stage` | `start_time`, `end_time`, `stage` (int), `sleep_id` | Stage breakdown per session |
| ✅ P1 | `com.samsung.shealth.tracker.heart_rate` | `start_time`, `end_time`, `heart_rate` | Per-measurement HR |
| ✅ P1 | `com.samsung.shealth.tracker.pedometer_day_summary` | `day_time`, `step_count`, `distance`, `calorie` | Daily steps |
| ✅ P1 | `com.samsung.shealth.exercise` | `start_time`, `end_time`, `exercise_type`, `duration`, `calorie`, `mean_heart_rate` | Exercise sessions |
| 🔵 P2 | `com.samsung.shealth.stress` | `start_time`, `end_time`, `score` | Stress score |
| 🔵 P2 | `com.samsung.shealth.tracker.oxygen_saturation` | `start_time`, `end_time`, `spo2`, `min`, `max` | SpO2 |
| 🔵 P2 | `com.samsung.health.respiratory_rate` | `start_time`, `end_time`, `average`, `lower_limit`, `upper_limit` | Respiratory rate |
| ⚪ P3 | `com.samsung.health.hrv` | `start_time`, `end_time`, `binning_data` | HRV (raw binned) |
| ⚪ P3 | `com.samsung.health.skin_temperature` | `start_time`, `end_time`, `temperature`, `min`, `max` | Skin temp |

---

## Key format notes

- **Datetimes**: `2023-12-22 11:48:00.000` — format `%Y-%m-%d %H:%M:%S.%f`
- **Timezone**: `time_offset` column, string like `UTC+0100`
- **Primary key**: `datauuid` (string UUID) on all tables
- **FK sleep stages → sleep**: `sleep_id` → `com.samsung.health.sleep.datauuid`
- **Prefixed columns**: some tables prefix columns with the full table name, e.g. `com.samsung.health.sleep.start_time`
- **Sleep stage int mapping** (Samsung Health convention):
  - `1` = awake
  - `2` = REM
  - `3` = light
  - `4` = deep

---

## Sleep

### `com.samsung.shealth.sleep` — 62 cols
Main sleep sessions. Prefixed cols for HC fields.

Key fields:
- `com.samsung.health.sleep.start_time` `<datetime>` — bed time
- `com.samsung.health.sleep.end_time` `<datetime>` — wake time
- `sleep_score` `<int>` — overall score
- `sleep_duration` `<int>` — total duration (minutes?)
- `sleep_cycle` `<int>` — number of cycles
- `efficiency` `<float>` — sleep efficiency %
- `mental_recovery` `<float>`
- `physical_recovery` `<float>`
- `original_bed_time` `<datetime>` — detected (vs goal)
- `original_wake_up_time` `<datetime>`
- `sleep_type` `<int>` — 0=night, 1=nap?
- `quality` `<int>`

Full column list: total_sleep_time_weight, original_efficiency, mental_recovery, wake_score, factor_01–10, deep_score, integrated_id, latency_weight, has_sleep_data, bedtime_detection_delay, sleep_efficiency_with_latency, wakeup_time_detection_delay, total_rem_duration, combined_id, nap_score, sleep_type, sleep_latency, data_version, latency_score, deep_weight, rem_weight, physical_recovery, original_wake_up_time, movement_awakening, is_integrated, original_bed_time, goal_bed_time, quality, extra_data, wake_weight, rem_score, goal_wake_up_time, sleep_cycle, total_light_duration, efficiency, sleep_score, sleep_duration, stage_analyzed_type, total_sleep_time_score, + prefixed HC fields

### `com.samsung.health.sleep_stage` — 13 cols
Stage intervals FK'd to sleep sessions.

Key fields:
- `start_time` `<datetime>`
- `end_time` `<datetime>`
- `stage` `<int>` — 1=awake, 2=REM, 3=light, 4=deep
- `sleep_id` `<str>` → `com.samsung.health.sleep.datauuid`
- `time_offset` `<str>`

### `com.samsung.shealth.sleep_combined` — 55 cols
Unified sleep table with cleaner (non-prefixed) column names. Overlaps with `shealth.sleep`.

Key additional field: `stage_analysis_type`

### `com.samsung.shealth.sleep_raw_data` — 10 cols
Raw sensor data blob per session.
- `sleep_uuid` `<str>` → FK to session
- `data` `<str>` — binary/encoded blob
- `version` `<int>`

### `com.samsung.shealth.sleep_snoring` — 12 cols
Snoring events.
- `start_time`, `end_time` `<datetime>`
- `duration` `<int>`

---

## Activity / Steps

### `com.samsung.shealth.tracker.pedometer_day_summary` — 21 cols
Daily step totals. **Best source for day-level steps.**

Key fields:
- `day_time` `<int>` — Unix timestamp ms for the day
- `step_count` `<int>`
- `walk_step_count` `<int>`, `run_step_count` `<int>`
- `distance` `<float>` — meters
- `calorie` `<float>` — kcal
- `active_time` `<int>` — ms
- `speed` `<float>` — m/s
- `binning_data` `<str>` — hourly breakdown (encoded)

### `com.samsung.shealth.tracker.pedometer_step_count` — 18 cols
Granular step intervals (sub-daily). Prefixed HC columns.

Key fields:
- `com.samsung.health.step_count.start_time` / `end_time` `<datetime>`
- `com.samsung.health.step_count.count` `<int>`
- `duration` `<int>`, `run_step` `<int>`, `walk_step` `<int>`
- `com.samsung.health.step_count.distance` `<float>`, `calorie` `<float>`, `speed` `<float>`

### `com.samsung.shealth.step_daily_trend` — 13 cols
Daily step trend.
- `day_time` `<int>` (ms)
- `count` `<int>`, `speed` `<float>`, `distance` `<float>`, `calorie` `<float>`
- `binning_data` `<str>`

### `com.samsung.shealth.activity.day_summary` — 33 cols
Daily activity composite (steps + exercise + floors).

Key fields:
- `day_time` `<datetime>`
- `step_count` `<int>`, `distance` `<float>`, `calorie` `<float>`
- `exercise_time` `<int>`, `active_time` `<int>`
- `floor_count` `<float>`
- `score` `<int>`

### `com.samsung.shealth.activity_level` — 8 cols
- `start_time` `<datetime>`
- `activity_level` `<int>`

### `com.samsung.health.floors_climbed` — 13 cols
- `start_time`, `end_time` `<datetime>`
- `floor` `<float>`

### `com.samsung.shealth.tracker.floors_day_summary` — 11 cols
- `day_time` `<int>` (ms)
- `floor_count` `<int>`
- `binning_data` `<str>`

---

## Heart Rate

### `com.samsung.shealth.tracker.heart_rate` — 21 cols
Individual HR measurements. Prefixed HC columns.

Key fields:
- `com.samsung.health.heart_rate.start_time` / `end_time` `<datetime>`
- `com.samsung.health.heart_rate.heart_rate` `<float>` — bpm
- `com.samsung.health.heart_rate.heart_beat_count` `<int>`
- `com.samsung.health.heart_rate.max` / `min` `<empty>` (often empty for spot measurements)
- `tag_id` `<int>` — measurement context (resting, exercise, etc.)
- `com.samsung.health.heart_rate.binning_data` `<empty>` — only set for session-type measurements

### `com.samsung.health.hrv` — 13 cols
HRV measurements.
- `start_time`, `end_time` `<datetime>`
- `binning_data` `<str>` — encoded HRV bins

### `com.samsung.shealth.stress` — 18 cols
Stress scores.
- `start_time`, `end_time` `<datetime>`
- `score` `<float>` — 0–100
- `tag_id` `<int>`
- `binning_data` `<str>` (often empty for single measurements)
- `max`, `min` `<empty>` on single measurements

### `com.samsung.shealth.stress.histogram` — 8 cols
- `base_hr` `<int>`, `decay_time` `<int>`
- `histogram` `<str>`

---

## Exercise

### `com.samsung.shealth.exercise` — 73 cols
Exercise sessions. Prefixed HC columns for core fields.

Key fields:
- `com.samsung.health.exercise.start_time` / `end_time` `<datetime>`
- `com.samsung.health.exercise.exercise_type` `<int>` — Samsung exercise type code
- `com.samsung.health.exercise.duration` `<int>` — ms
- `com.samsung.health.exercise.calorie` `<float>` — kcal
- `total_calorie` `<float>`
- `com.samsung.health.exercise.mean_heart_rate` `<float>`
- `com.samsung.health.exercise.max_heart_rate` `<float>`
- `com.samsung.health.exercise.min_heart_rate` `<float>`
- `com.samsung.health.exercise.distance` `<empty>` on indoor
- `com.samsung.health.exercise.mean_speed` `<empty>` on indoor
- `com.samsung.health.exercise.live_data` `<str>` — encoded time-series data

### `com.samsung.shealth.exercise.recovery_heart_rate` — 12 cols
Post-exercise HR.
- `start_time`, `end_time` `<datetime>`
- `exercise_id` `<str>` → FK to exercise session
- `heart_rate` `<str>` — encoded sequence

---

## Biometrics

### `com.samsung.shealth.tracker.oxygen_saturation` — 24 cols
SpO2 measurements. Prefixed HC columns.

Key fields:
- `com.samsung.health.oxygen_saturation.start_time` / `end_time` `<datetime>`
- `com.samsung.health.oxygen_saturation.spo2` `<float>` — %
- `com.samsung.health.oxygen_saturation.min` / `max` `<float>`
- `com.samsung.health.oxygen_saturation.low_duration` `<int>`
- `tag_id` `<int>`

### `com.samsung.health.oxygen_saturation.raw` — 11 cols
Raw SpO2 series.
- `start_time`, `end_time` `<datetime>`
- `binning_data` `<str>`

### `com.samsung.health.respiratory_rate` — 20 cols
- `start_time`, `end_time` `<datetime>`
- `average` `<float>`, `lower_limit` `<float>`, `upper_limit` `<float>`
- `is_outlier` `<int>`

### `com.samsung.health.skin_temperature` — 24 cols
- `start_time`, `end_time` `<datetime>`
- `temperature` `<float>` — °C
- `min` `<float>`, `max` `<float>`
- `tag_id` `<int>`
- `binning_data` `<str>`

### `com.samsung.health.ecg` — 27 cols
- `start_time`, `end_time` `<datetime>`
- `mean_heart_rate` `<float>`
- `sample_frequency` `<int>`, `sample_count` `<int>`
- `data` `<str>` — raw ECG signal (encoded)
- `classification` `<int>`
- `symptoms` `<str>`

### `com.samsung.shealth.vitality_score` — 69 cols
Daily vitality composite score.
- `day_time` `<datetime>` (note: datetime, not int like other day_time cols)
- `total_score` `<float>`
- `sleep_score` `<float>`, `sleep_balance` `<float>`, `sleep_regularity` `<float>`, `sleep_timing` `<float>`
- `activity_score` `<float>`, `active_time` `<int>`, `mvpa_time` `<int>`
- `shr_score` `<float>`, `shr_value` `<float>` — stress/heart rate balance
- `shrv_score` `<float>`, `shrv_value` `<float>` — HRV balance

---

## Body measurements

### `com.samsung.health.weight` — 25 cols
- `start_time` `<datetime>`
- `weight` `<float>` — kg
- `body_fat` `<float>` — %
- `skeletal_muscle` `<float>`, `skeletal_muscle_mass` `<float>`
- `fat_free_mass` `<float>`, `fat_free` `<float>`
- `basal_metabolic_rate` `<int>`
- `total_body_water` `<float>`

### `com.samsung.health.height` — 11 cols
- `start_time` `<datetime>`
- `height` `<float>` — cm

### `com.samsung.shealth.blood_pressure` — 23 cols
Prefixed HC columns.
- `com.samsung.health.blood_pressure.start_time` `<datetime>`
- `com.samsung.health.blood_pressure.systolic` `<float>`
- `com.samsung.health.blood_pressure.diastolic` `<float>`
- `com.samsung.health.blood_pressure.pulse` `<int>`
- `com.samsung.health.blood_pressure.mean` `<float>`

---

## Mood

### `com.samsung.shealth.mood` — 16 cols
- `start_time` `<datetime>`
- `mood_type` `<int>` — mood level
- `emotions` `<str>` — encoded list
- `factors` `<str>` — encoded list
- `notes` `<str>`, `place` `<str>`, `company` `<str>`

---

## Nutrition

### `com.samsung.health.food_info` — 34 cols
Food database entries (not time-series).
- `name` `<str>`, `calorie` `<float>`, `carbohydrate` `<float>`, `protein` `<float>`, `total_fat` `<float>`, `sugar` `<float>`, `sodium` `<float>`, etc.

### `com.samsung.health.water_intake` — 13 cols
- `start_time` `<datetime>`
- `amount` `<float>` — ml
- `unit_amount` `<float>`

---

## Ignored / metadata only

| Table | Reason |
|-------|--------|
| `device_profile` | Device inventory, no time-series value |
| `user_profile` | Key-value settings |
| `preferences` / `service_preferences` | App settings |
| `badge` / `rewards` / `best_records` | Gamification |
| `hsp.references` | Internal cross-references |
| `insight_message` / `insight.message_notification` | App-generated insights |
| `social.service_status` | Social feature metadata |
| `report` | Compressed weekly reports |
| `cycle.daily_temperature.raw` | Cycle tracking (not relevant) |
| `shm_device` | SHM device registry |
| `body_composition_goal` / `activity.daily_goal` | Goals, not measurements |
| `exercise.periodization_*` / `exercise.program` | Training programs |
| `program.sleep_coaching.*` | Sleep coaching missions |
| `calories_burned.details` | Redundant with day_summary |
| `vitality.nap_data` | Nap scores (secondary) |
| `food_frequent` | Food frequency stats |
