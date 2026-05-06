---
type: spec-summary
slug: 2026-04-24-v2-aes256-gcm-extend-art9
original_type: spec
status: delivered
source: ../../specs/2026-04-24-v2-aes256-gcm-extend-art9
---

# Spec — 2026-04-24-v2-aes256-gcm-extend-art9

Source : [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/server/db/encrypted|server/db/encrypted.py]] — symbols: `EncryptedFloat`
- [[../code/server/db/models|server/db/models.py]] — symbols: `SleepSession`, `Weight`, `BloodPressure`, `Stress`, `Spo2`, `HeartRateHourly`, `RespiratoryRate`, `SkinTemperature`, `Ecg`
- [[../code/alembic/versions/0003_encrypt_remaining_art9|alembic/versions/0003_encrypt_remaining_art9.py]] — symbols: `upgrade`, `downgrade`

### Tests
- [[../code/tests/server/test_encryption_extend_art9|tests/server/test_encryption_extend_art9.py]] — classes: `TestSentinelleBytea`, `TestRoundTripCritique` · methods: `test_sleep_sessions_art9_columns_are_bytea`, `test_weight_art9_columns_are_bytea`, `test_blood_pressure_art9_columns_are_bytea`, `test_stress_score_is_bytea`, `test_spo2_art9_columns_are_bytea`, `test_heart_rate_hourly_art9_columns_are_bytea`, `test_respiratory_rate_art9_columns_are_bytea`, `test_skin_temperature_art9_columns_are_bytea`, `test_ecg_art9_columns_are_bytea`, `test_sleep_score_round_trip_via_orm`, `test_weight_kg_round_trip_via_orm`, `test_crypto_v_columns_default_to_1_everywhere`
