---
type: spec-summary
slug: 2026-04-24-v2-csv-import-sqlalchemy
original_type: spec
status: delivered
source: ../../specs/2026-04-24-v2-csv-import-sqlalchemy
---

# Spec — 2026-04-24-v2-csv-import-sqlalchemy

Source : [[../../specs/2026-04-24-v2-csv-import-sqlalchemy]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/scripts/import_samsung_csv|scripts/import_samsung_csv.py]] — symbols: `main`, `import_sleep`, `import_sleep_stages`, `import_steps_hourly`, `import_steps_daily`, `import_heart_rate_hourly`, `import_exercise`, `import_stress`, `import_spo2`, `import_respiratory_rate`, `import_hrv`, `import_skin_temperature`, `import_weight`, `import_height`, `import_blood_pressure`, `import_mood`, `import_water_intake`, `import_activity_daily`, `import_vitality_score`, `import_floors_daily`, `import_activity_level`, `import_ecg`
- [[../code/scripts/generate_sample|scripts/generate_sample.py]] — symbols: `main`, `generate_sleep_sessions`, `generate_steps_hourly`, `generate_heart_rate_hourly`, `generate_exercise_sessions`

### Tests
- [[../code/tests/server/test_scripts_csv_import|tests/server/test_scripts_csv_import.py]] — classes: `TestImportSamsungCsv`, `TestGenerateSample` · methods: `test_import_sleep_round_trip_pg`, `test_import_idempotent_second_run_zero_inserts`, `test_generate_sample_creates_30d_data`, `test_generate_sample_idempotent`
