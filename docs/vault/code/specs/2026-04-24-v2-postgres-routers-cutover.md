---
type: spec-summary
slug: 2026-04-24-v2-postgres-routers-cutover
original_type: spec
status: delivered
source: ../../specs/2026-04-24-v2-postgres-routers-cutover
---

# Spec — 2026-04-24-v2-postgres-routers-cutover

Source : [[../../specs/2026-04-24-v2-postgres-routers-cutover]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/server/routers/sleep|server/routers/sleep.py]] — symbols: `router`, `create_sleep_sessions`, `get_sleep_sessions`
- [[../code/server/routers/heartrate|server/routers/heartrate.py]] — symbols: `router`
- [[../code/server/routers/steps|server/routers/steps.py]] — symbols: `router`
- [[../code/server/routers/exercise|server/routers/exercise.py]] — symbols: `router`
- [[../code/server/main|server/main.py]] — symbols: `app`, `startup`
- [[../code/server/database|server/database.py]] — symbols: `get_engine`, `get_session`, `SessionLocal`
- [[../code/server/models|server/models.py]] — symbols: `SleepSessionOut`, `SleepStageOut`

### Tests
- [[../code/tests/server/test_models_postgres|tests/server/test_models_postgres.py]] — classes: `TestApiBackCompat` · methods: `test_get_sleep_period_6m_response_shape_unchanged`
- [[../code/tests/test_sleep|tests/test_sleep.py]]
- [[../code/tests/test_sleep_api_shape|tests/test_sleep_api_shape.py]]
- [[../code/tests/server/test_routers_cutover|tests/server/test_routers_cutover.py]] — classes: `TestHeartRateRouter`, `TestStepsRouter`, `TestExerciseRouter`, `TestNoSqliteResidual` · methods: `test_post_get_heart_rate_round_trip`, `test_post_get_steps_round_trip`, `test_post_get_exercise_round_trip`, `test_no_sqlite_imports_in_server`, `test_no_health_db_in_repo`, `test_get_connection_removed`
