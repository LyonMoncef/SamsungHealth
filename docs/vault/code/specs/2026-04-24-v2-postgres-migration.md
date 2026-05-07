---
type: spec-summary
slug: 2026-04-24-v2-postgres-migration
original_type: spec
status: delivered
source: ../../specs/2026-04-24-v2-postgres-migration
---

# Spec — 2026-04-24-v2-postgres-migration

Source : [[../../specs/2026-04-24-v2-postgres-migration]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/docker-compose.yml|docker-compose.yml]] — symbols: `postgres`
- [[../code/server/database|server/database.py]] — symbols: `get_engine`, `get_session`, `SessionLocal`
- [[../code/server/db/models|server/db/models.py]] — symbols: `Base`, `SleepSession`, `SleepStage`, `HeartRateHourly`, `StepsDaily`, `StepsHourly`, `ExerciseSession`, `ActivityDaily`, `Stress`, `Spo2`, `RespiratoryRate`, `Hrv`, `SkinTemperature`, `Weight`, `Height`, `BloodPressure`, `Mood`, `WaterIntake`, `VitalityScore`, `FloorsDaily`, `ActivityLevel`, `Ecg`
- [[../code/server/db/uuid7|server/db/uuid7.py]] — symbols: `uuid7`
- [[../code/alembic/env|alembic/env.py]]
- [[../code/alembic/versions/0001_initial|alembic/versions/0001_initial.py]] — symbols: `upgrade`, `downgrade`
- [[../code/alembic.ini|alembic.ini]]

### Tests
- [[../code/tests/server/test_postgres_bootstrap|tests/server/test_postgres_bootstrap.py]] — classes: `TestBootstrap` · methods: `test_alembic_upgrade_creates_schema`, `test_alembic_idempotent`, `test_alembic_downgrade_reverses`
- [[../code/tests/server/test_uuid7|tests/server/test_uuid7.py]] — classes: `TestUuid7` · methods: `test_monotonic_within_ms`, `test_version_field_is_7`, `test_timestamp_extractable`
- [[../code/tests/server/test_models_postgres|tests/server/test_models_postgres.py]] — classes: `TestSleepSessionPersistence`, `TestApiBackCompat` · methods: `test_insert_sleep_session_assigns_uuid_and_created_at`, `test_read_sleep_session_by_uuid`, `test_get_sleep_period_6m_response_shape_unchanged`, `test_sleep_session_with_stages_atomic`
