---
type: code-source
language: python
file_path: tests/server/test_encryption_extend_art9.py
git_blob: f74f5be3764728a31406dddfa1bac69aae3ca350
last_synced: '2026-05-06T06:14:02Z'
loc: 301
annotations: []
imports:
- datetime
- pytest
- sqlalchemy
exports:
- _is_bytes_storage
- _bytes_value
- TestSentinelleBytea
- TestRoundTripCritique
tags:
- code
- python
---

# tests/server/test_encryption_extend_art9.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_encryption_extend_art9.py`](../../../tests/server/test_encryption_extend_art9.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Tests RED — V2.2.1 chiffrement étendu aux 9 tables Art.9 restantes.

Mappé sur frontmatter tested_by: tests/server/test_encryption_extend_art9.py
Classes: TestSentinelleBytea, TestRoundTripCritique
"""
from datetime import datetime, timezone

import pytest
from sqlalchemy import text


def _is_bytes_storage(raw) -> bool:
    return isinstance(raw, (bytes, memoryview))


def _bytes_value(raw) -> bytes:
    return bytes(raw) if raw is not None else b""


class TestSentinelleBytea:
    def test_sleep_sessions_art9_columns_are_bytea(self, schema_ready, db_session):
        from server.db.models import SleepSession
        db_session.add(SleepSession(
            sleep_start=datetime(2026, 4, 24, 22, 0, tzinfo=timezone.utc),
            sleep_end=datetime(2026, 4, 25, 6, 0, tzinfo=timezone.utc),
            sleep_score=85,
            efficiency=0.92,
            sleep_duration_min=480,
        ))
        db_session.commit()
        row = db_session.execute(text(
            "SELECT sleep_score, efficiency, sleep_duration_min FROM sleep_sessions LIMIT 1"
        )).fetchone()
        for col in row:
            assert _is_bytes_storage(col), f"Attendu bytes, got {type(col)} : {col!r}"
        # Plaintext "85" / "0.92" / "480" ne doivent pas apparaître
        all_bytes = b"".join(_bytes_value(c) for c in row)
        for needle in (b"85", b"0.92", b"480"):
            assert needle not in all_bytes, f"Plaintext {needle!r} leak in {all_bytes!r}"

    def test_weight_art9_columns_are_bytea(self, schema_ready, db_session):
        from server.db.models import Weight
        db_session.add(Weight(
            start_time=datetime(2026, 4, 24, 8, 0, tzinfo=timezone.utc),
            weight_kg=72.5,
            body_fat_pct=18.3,
            basal_metabolic_rate=1650,
        ))
        db_session.commit()
        row = db_session.execute(text(
            "SELECT weight_kg, body_fat_pct, basal_metabolic_rate FROM weight LIMIT 1"
        )).fetchone()
        for col in row:
            assert _is_bytes_storage(col)
        all_bytes = b"".join(_bytes_value(c) for c in row)
        for needle in (b"72.5", b"18.3", b"1650"):
            assert needle not in all_bytes

    def test_blood_pressure_art9_columns_are_bytea(self, schema_ready, db_session):
        from server.db.models import BloodPressure
        db_session.add(BloodPressure(
            start_time=datetime(2026, 4, 24, 9, 0, tzinfo=timezone.utc),
            systolic=120.5,
            diastolic=80.0,
            pulse=68,
        ))
        db_session.commit()
        row = db_session.execute(text(
            "SELECT systolic, diastolic, pulse FROM blood_pressure LIMIT 1"
        )).fetchone()
        for col in row:
            assert _is_bytes_storage(col)
        all_bytes = b"".join(_bytes_value(c) for c in row)
        for needle in (b"120.5", b"80.0", b"68"):
            assert needle not in all_bytes

    def test_stress_score_is_bytea(self, schema_ready, db_session):
        from server.db.models import Stress
        db_session.add(Stress(
            start_time=datetime(2026, 4, 24, 12, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 24, 12, 30, tzinfo=timezone.utc),
            score=42.7,
        ))
        db_session.commit()
        row = db_session.execute(text("SELECT score FROM stress LIMIT 1")).fetchone()
        assert _is_bytes_storage(row[0])
        assert b"42.7" not in _bytes_value(row[0])

    def test_spo2_art9_columns_are_bytea(self, schema_ready, db_session):
        from server.db.models import Spo2
        db_session.add(Spo2(
            start_time=datetime(2026, 4, 24, 14, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 24, 14, 5, tzinfo=timezone.utc),
            spo2=97.5,
            min_spo2=95.0,
            low_duration_s=10,
        ))
        db_session.commit()
        row = db_session.execute(text(
            "SELECT spo2, min_spo2, low_duration_s FROM spo2 LIMIT 1"
        )).fetchone()
        for col in row:
            assert _is_bytes_storage(col)
        all_bytes = b"".join(_bytes_value(c) for c in row)
        for needle in (b"97.5", b"95.0", b"10"):
            assert needle not in all_bytes

    def test_heart_rate_hourly_art9_columns_are_bytea(self, schema_ready, db_session):
        from server.db.models import HeartRateHourly
        db_session.add(HeartRateHourly(
            date="2026-04-24",
            hour=10,
            min_bpm=58,
            max_bpm=110,
            avg_bpm=72,
            sample_count=60,
        ))
        db_session.commit()
        row = db_session.execute(text(
            "SELECT min_bpm, max_bpm, avg_bpm FROM heart_rate_hourly LIMIT 1"
        )).fetchone()
        for col in row:
            assert _is_bytes_storage(col)
        all_bytes = b"".join(_bytes_value(c) for c in row)
        for needle in (b"58", b"110", b"72"):
            assert needle not in all_bytes

    def test_respiratory_rate_art9_columns_are_bytea(self, schema_ready, db_session):
        from server.db.models import RespiratoryRate
        db_session.add(RespiratoryRate(
            start_time=datetime(2026, 4, 24, 15, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 24, 15, 1, tzinfo=timezone.utc),
            average=14.5,
            lower_limit=12.0,
            upper_limit=18.0,
        ))
        db_session.commit()
        row = db_session.execute(text(
            "SELECT average, lower_limit, upper_limit FROM respiratory_rate LIMIT 1"
        )).fetchone()
        for col in row:
            assert _is_bytes_storage(col)
        all_bytes = b"".join(_bytes_value(c) for c in row)
        for needle in (b"14.5", b"12.0", b"18.0"):
            assert needle not in all_bytes

    def test_skin_temperature_art9_columns_are_bytea(self, schema_ready, db_session):
        from server.db.models import SkinTemperature
        db_session.add(SkinTemperature(
            start_time=datetime(2026, 4, 24, 16, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 24, 16, 5, tzinfo=timezone.utc),
            temperature=36.7,
            min_temp=36.5,
            max_temp=36.9,
        ))
        db_session.commit()
        row = db_session.execute(text(
            "SELECT temperature, min_temp, max_temp FROM skin_temperature LIMIT 1"
        )).fetchone()
        for col in row:
            assert _is_bytes_storage(col)
        all_bytes = b"".join(_bytes_value(c) for c in row)
        for needle in (b"36.7", b"36.5", b"36.9"):
            assert needle not in all_bytes

    def test_ecg_art9_columns_are_bytea(self, schema_ready, db_session):
        from server.db.models import Ecg
        db_session.add(Ecg(
            start_time=datetime(2026, 4, 24, 17, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 24, 17, 1, tzinfo=timezone.utc),
            mean_heart_rate=68.0,
            classification=1,
        ))
        db_session.commit()
        row = db_session.execute(text(
            "SELECT mean_heart_rate, classification FROM ecg LIMIT 1"
        )).fetchone()
        for col in row:
            assert _is_bytes_storage(col)
        all_bytes = b"".join(_bytes_value(c) for c in row)
        for needle in (b"68.0",):
            assert needle not in all_bytes


class TestRoundTripCritique:
    def test_sleep_score_round_trip_via_orm(self, schema_ready, db_session):
        from sqlalchemy import select
        from server.db.models import SleepSession
        db_session.add(SleepSession(
            sleep_start=datetime(2026, 4, 23, 22, 0, tzinfo=timezone.utc),
            sleep_end=datetime(2026, 4, 24, 6, 0, tzinfo=timezone.utc),
            sleep_score=85,
            efficiency=0.92,
            sleep_duration_min=480,
            sleep_cycle=5,
            mental_recovery=0.78,
            physical_recovery=0.85,
            sleep_type=1,
        ))
        db_session.commit()
        db_session.expire_all()
        loaded = db_session.execute(select(SleepSession)).scalar_one()
        assert loaded.sleep_score == 85
        assert abs(loaded.efficiency - 0.92) < 1e-9
        assert loaded.sleep_duration_min == 480
        assert loaded.sleep_cycle == 5
        assert abs(loaded.mental_recovery - 0.78) < 1e-9
        assert abs(loaded.physical_recovery - 0.85) < 1e-9
        assert loaded.sleep_type == 1

    def test_weight_kg_round_trip_via_orm(self, schema_ready, db_session):
        from sqlalchemy import select
        from server.db.models import Weight
        db_session.add(Weight(
            start_time=datetime(2026, 4, 23, 8, 0, tzinfo=timezone.utc),
            weight_kg=72.55,
            body_fat_pct=18.3,
            skeletal_muscle_pct=42.1,
            skeletal_muscle_mass_kg=30.5,
            fat_free_mass_kg=59.2,
            basal_metabolic_rate=1650,
            total_body_water_kg=43.7,
        ))
        db_session.commit()
        db_session.expire_all()
        loaded = db_session.execute(select(Weight)).scalar_one()
        assert abs(loaded.weight_kg - 72.55) < 1e-9
        assert abs(loaded.body_fat_pct - 18.3) < 1e-9
        assert abs(loaded.skeletal_muscle_pct - 42.1) < 1e-9
        assert abs(loaded.skeletal_muscle_mass_kg - 30.5) < 1e-9
        assert abs(loaded.fat_free_mass_kg - 59.2) < 1e-9
        assert loaded.basal_metabolic_rate == 1650
        assert abs(loaded.total_body_water_kg - 43.7) < 1e-9

    def test_crypto_v_columns_default_to_1_everywhere(self, schema_ready, db_session):
        from server.db.models import (
            BloodPressure,
            Ecg,
            HeartRateHourly,
            RespiratoryRate,
            SkinTemperature,
            SleepSession,
            Spo2,
            Stress,
            Weight,
        )

        # Insert minimal pour chaque table
        db_session.add(SleepSession(
            sleep_start=datetime(2026, 4, 22, 22, 0, tzinfo=timezone.utc),
            sleep_end=datetime(2026, 4, 23, 6, 0, tzinfo=timezone.utc),
        ))
        db_session.add(Weight(start_time=datetime(2026, 4, 22, 8, 0, tzinfo=timezone.utc), weight_kg=70.0))
        db_session.add(BloodPressure(start_time=datetime(2026, 4, 22, 9, 0, tzinfo=timezone.utc), systolic=120.0))
        db_session.add(Stress(
            start_time=datetime(2026, 4, 22, 12, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 22, 12, 30, tzinfo=timezone.utc),
            score=40.0,
        ))
        db_session.add(Spo2(
            start_time=datetime(2026, 4, 22, 14, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 22, 14, 5, tzinfo=timezone.utc),
            spo2=97.0,
        ))
        db_session.add(HeartRateHourly(
            date="2026-04-22", hour=8, min_bpm=60, max_bpm=80, avg_bpm=70, sample_count=60,
        ))
        db_session.add(RespiratoryRate(
            start_time=datetime(2026, 4, 22, 15, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 22, 15, 1, tzinfo=timezone.utc),
            average=14.0,
        ))
        db_session.add(SkinTemperature(
            start_time=datetime(2026, 4, 22, 16, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 22, 16, 5, tzinfo=timezone.utc),
            temperature=36.5,
        ))
        db_session.add(Ecg(
            start_time=datetime(2026, 4, 22, 17, 0, tzinfo=timezone.utc),
            end_time=datetime(2026, 4, 22, 17, 1, tzinfo=timezone.utc),
            mean_heart_rate=68.0,
        ))
        db_session.commit()

        # Vérifie qu'au moins une colonne _crypto_v vaut 1 sur chaque table
        checks = {
            "sleep_sessions": "sleep_score_crypto_v",
            "weight": "weight_kg_crypto_v",
            "blood_pressure": "systolic_crypto_v",
            "stress": "score_crypto_v",
            "spo2": "spo2_crypto_v",
            "heart_rate_hourly": "min_bpm_crypto_v",
            "respiratory_rate": "average_crypto_v",
            "skin_temperature": "temperature_crypto_v",
            "ecg": "mean_heart_rate_crypto_v",
        }
        for table, col in checks.items():
            row = db_session.execute(text(f"SELECT {col} FROM {table} LIMIT 1")).fetchone()
            assert row is not None, f"{table} sans row"
            assert row[0] == 1, f"{table}.{col} doit valoir 1, got {row[0]}"
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_is_bytes_storage` (function) — lines 13-14
- `_bytes_value` (function) — lines 17-18
- `TestSentinelleBytea` (class) — lines 21-183
- `TestRoundTripCritique` (class) — lines 186-301

### Imports
- `datetime`
- `pytest`
- `sqlalchemy`

### Exports
- `_is_bytes_storage`
- `_bytes_value`
- `TestSentinelleBytea`
- `TestRoundTripCritique`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9]] — classes: `TestSentinelleBytea`, `TestRoundTripCritique` · methods: `test_sleep_sessions_art9_columns_are_bytea`, `test_weight_art9_columns_are_bytea`, `test_blood_pressure_art9_columns_are_bytea`, `test_stress_score_is_bytea`, `test_spo2_art9_columns_are_bytea`, `test_heart_rate_hourly_art9_columns_are_bytea`, `test_respiratory_rate_art9_columns_are_bytea`, `test_skin_temperature_art9_columns_are_bytea`, `test_ecg_art9_columns_are_bytea`, `test_sleep_score_round_trip_via_orm`, `test_weight_kg_round_trip_via_orm`, `test_crypto_v_columns_default_to_1_everywhere`
