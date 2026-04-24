---
type: code-source
language: python
file_path: server/db/models.py
git_blob: 760c3f5d3f8968b6867ee3ec6f0163541cbc4405
last_synced: '2026-04-24T04:04:56Z'
loc: 371
annotations: []
imports:
- datetime
- uuid
- sqlalchemy
- sqlalchemy.orm
- .encrypted
- .uuid7
exports:
- Base
- TimestampedMixin
- Uuid7PkMixin
- SleepSession
- SleepStage
- StepsHourly
- StepsDaily
- HeartRateHourly
- ExerciseSession
- Stress
- Spo2
- RespiratoryRate
- Hrv
- SkinTemperature
- Weight
- Height
- BloodPressure
- Mood
- WaterIntake
- ActivityDaily
- VitalityScore
- FloorsDaily
- ActivityLevel
- Ecg
tags:
- code
- python
---

# server/db/models.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/db/models.py`](../../../server/db/models.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""SQLAlchemy 2.x models — schema V2.1 (UUID v7 PK, timestamps partout, FK UUID).

Mirror du schéma SQLite legacy (server/database.py::init_db) avec :
- PK INTEGER → UUID v7 partout (timestamp-sortable)
- FK INTEGER → UUID v7
- Ajout systématique created_at/updated_at (timestamptz, server_default)
- Contraintes UNIQUE conservées pour idempotence d'import CSV
"""
from datetime import datetime
from uuid import UUID

from sqlalchemy import (
    DateTime,
    Float,
    ForeignKey,
    Index,
    Integer,
    String,
    Text,
    UniqueConstraint,
    func,
)
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, relationship

from .encrypted import EncryptedFloat, EncryptedInt, EncryptedString
from .uuid7 import Uuid7, uuid7


class Base(DeclarativeBase):
    pass


class TimestampedMixin:
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )


class Uuid7PkMixin:
    id: Mapped[UUID] = mapped_column(Uuid7(), primary_key=True, default=uuid7)


# ── sleep ──────────────────────────────────────────────────────────────────
class SleepSession(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "sleep_sessions"
    __table_args__ = (
        UniqueConstraint("sleep_start", "sleep_end", name="uq_sleep_sessions_window"),
        Index("idx_sleep_start", "sleep_start"),
    )

    sleep_start: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    sleep_end: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # V2.2.1 — colonnes Art.9 chiffrées
    sleep_score: Mapped[int | None] = mapped_column(EncryptedInt)
    efficiency: Mapped[float | None] = mapped_column(EncryptedFloat)
    sleep_duration_min: Mapped[int | None] = mapped_column(EncryptedInt)
    sleep_cycle: Mapped[int | None] = mapped_column(EncryptedInt)
    mental_recovery: Mapped[float | None] = mapped_column(EncryptedFloat)
    physical_recovery: Mapped[float | None] = mapped_column(EncryptedFloat)
    sleep_type: Mapped[int | None] = mapped_column(EncryptedInt)
    sleep_score_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    efficiency_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    sleep_duration_min_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    sleep_cycle_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    mental_recovery_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    physical_recovery_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    sleep_type_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")

    stages: Mapped[list["SleepStage"]] = relationship(
        back_populates="session", cascade="all, delete-orphan"
    )


class SleepStage(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "sleep_stages"
    __table_args__ = (
        UniqueConstraint("stage_start", "stage_end", name="uq_sleep_stages_window"),
        Index("idx_stages_session", "session_id"),
    )

    session_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("sleep_sessions.id", ondelete="CASCADE"), nullable=False
    )
    stage_type: Mapped[str] = mapped_column(String(32), nullable=False)
    stage_start: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    stage_end: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)

    session: Mapped[SleepSession] = relationship(back_populates="stages")


# ── steps ──────────────────────────────────────────────────────────────────
class StepsHourly(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "steps_hourly"
    __table_args__ = (UniqueConstraint("date", "hour", name="uq_steps_hourly_slot"),)

    date: Mapped[str] = mapped_column(String(10), nullable=False)
    hour: Mapped[int] = mapped_column(Integer, nullable=False)
    step_count: Mapped[int] = mapped_column(Integer, nullable=False)


class StepsDaily(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "steps_daily"
    __table_args__ = (UniqueConstraint("day_date", name="uq_steps_daily_day"),)

    day_date: Mapped[str] = mapped_column(String(10), nullable=False)
    step_count: Mapped[int | None] = mapped_column(Integer)
    walk_step_count: Mapped[int | None] = mapped_column(Integer)
    run_step_count: Mapped[int | None] = mapped_column(Integer)
    distance_m: Mapped[float | None] = mapped_column(Float)
    calorie_kcal: Mapped[float | None] = mapped_column(Float)
    active_time_ms: Mapped[int | None] = mapped_column(Integer)


# ── heart rate ─────────────────────────────────────────────────────────────
class HeartRateHourly(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "heart_rate_hourly"
    __table_args__ = (UniqueConstraint("date", "hour", name="uq_hr_hourly_slot"),)

    date: Mapped[str] = mapped_column(String(10), nullable=False)
    hour: Mapped[int] = mapped_column(Integer, nullable=False)
    # V2.2.1 — colonnes Art.9 chiffrées
    min_bpm: Mapped[int] = mapped_column(EncryptedInt, nullable=False)
    max_bpm: Mapped[int] = mapped_column(EncryptedInt, nullable=False)
    avg_bpm: Mapped[int] = mapped_column(EncryptedInt, nullable=False)
    sample_count: Mapped[int] = mapped_column(Integer, nullable=False)
    min_bpm_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    max_bpm_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    avg_bpm_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")


# ── exercise ───────────────────────────────────────────────────────────────
class ExerciseSession(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "exercise_sessions"
    __table_args__ = (
        UniqueConstraint("exercise_start", "exercise_end", name="uq_exercise_window"),
    )

    exercise_type: Mapped[str] = mapped_column(String(64), nullable=False)
    exercise_start: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    exercise_end: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    duration_minutes: Mapped[float] = mapped_column(Float, nullable=False)
    calorie_kcal: Mapped[float | None] = mapped_column(Float)
    distance_m: Mapped[float | None] = mapped_column(Float)
    mean_heart_rate: Mapped[float | None] = mapped_column(Float)
    max_heart_rate: Mapped[float | None] = mapped_column(Float)
    min_heart_rate: Mapped[float | None] = mapped_column(Float)
    mean_speed_ms: Mapped[float | None] = mapped_column(Float)


# ── stress / spo2 / respi / hrv / skin temp ────────────────────────────────
class Stress(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "stress"
    __table_args__ = (
        UniqueConstraint("start_time", "end_time", name="uq_stress_window"),
        Index("idx_stress_start", "start_time"),
    )

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # V2.2.1 — score Art.9 chiffré
    score: Mapped[float | None] = mapped_column(EncryptedFloat)
    score_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    tag_id: Mapped[int | None] = mapped_column(Integer)


class Spo2(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "spo2"
    __table_args__ = (
        UniqueConstraint("start_time", "end_time", name="uq_spo2_window"),
        Index("idx_spo2_start", "start_time"),
    )

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # V2.2.1 — colonnes Art.9 chiffrées
    spo2: Mapped[float | None] = mapped_column(EncryptedFloat)
    min_spo2: Mapped[float | None] = mapped_column(EncryptedFloat)
    max_spo2: Mapped[float | None] = mapped_column(EncryptedFloat)
    low_duration_s: Mapped[int | None] = mapped_column(EncryptedInt)
    spo2_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    min_spo2_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    max_spo2_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    low_duration_s_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    tag_id: Mapped[int | None] = mapped_column(Integer)


class RespiratoryRate(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "respiratory_rate"
    __table_args__ = (UniqueConstraint("start_time", "end_time", name="uq_respi_window"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # V2.2.1 — colonnes Art.9 chiffrées
    average: Mapped[float | None] = mapped_column(EncryptedFloat)
    lower_limit: Mapped[float | None] = mapped_column(EncryptedFloat)
    upper_limit: Mapped[float | None] = mapped_column(EncryptedFloat)
    average_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    lower_limit_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    upper_limit_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")


class Hrv(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "hrv"
    __table_args__ = (UniqueConstraint("start_time", "end_time", name="uq_hrv_window"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class SkinTemperature(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "skin_temperature"
    __table_args__ = (UniqueConstraint("start_time", "end_time", name="uq_skin_temp_window"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # V2.2.1 — colonnes Art.9 chiffrées
    temperature: Mapped[float | None] = mapped_column(EncryptedFloat)
    min_temp: Mapped[float | None] = mapped_column(EncryptedFloat)
    max_temp: Mapped[float | None] = mapped_column(EncryptedFloat)
    temperature_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    min_temp_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    max_temp_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    tag_id: Mapped[int | None] = mapped_column(Integer)


# ── weight / height / bp / mood / water ────────────────────────────────────
class Weight(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "weight"
    __table_args__ = (UniqueConstraint("start_time", name="uq_weight_time"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # V2.2.1 — colonnes Art.9 chiffrées
    weight_kg: Mapped[float | None] = mapped_column(EncryptedFloat)
    body_fat_pct: Mapped[float | None] = mapped_column(EncryptedFloat)
    skeletal_muscle_pct: Mapped[float | None] = mapped_column(EncryptedFloat)
    skeletal_muscle_mass_kg: Mapped[float | None] = mapped_column(EncryptedFloat)
    fat_free_mass_kg: Mapped[float | None] = mapped_column(EncryptedFloat)
    basal_metabolic_rate: Mapped[int | None] = mapped_column(EncryptedInt)
    total_body_water_kg: Mapped[float | None] = mapped_column(EncryptedFloat)
    weight_kg_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    body_fat_pct_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    skeletal_muscle_pct_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    skeletal_muscle_mass_kg_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    fat_free_mass_kg_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    basal_metabolic_rate_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    total_body_water_kg_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")


class Height(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "height"
    __table_args__ = (UniqueConstraint("start_time", name="uq_height_time"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    height_cm: Mapped[float | None] = mapped_column(Float)


class BloodPressure(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "blood_pressure"
    __table_args__ = (UniqueConstraint("start_time", name="uq_bp_time"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # V2.2.1 — colonnes Art.9 chiffrées
    systolic: Mapped[float | None] = mapped_column(EncryptedFloat)
    diastolic: Mapped[float | None] = mapped_column(EncryptedFloat)
    pulse: Mapped[int | None] = mapped_column(EncryptedInt)
    mean_bp: Mapped[float | None] = mapped_column(EncryptedFloat)
    systolic_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    diastolic_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    pulse_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    mean_bp_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")


class Mood(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "mood"
    __table_args__ = (UniqueConstraint("start_time", name="uq_mood_time"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # V2.2 — colonnes Art.9 chiffrées AES-256-GCM (BYTEA en DB, str/int en Python)
    mood_type: Mapped[int | None] = mapped_column(EncryptedInt)
    emotions: Mapped[str | None] = mapped_column(EncryptedString)
    factors: Mapped[str | None] = mapped_column(EncryptedString)
    notes: Mapped[str | None] = mapped_column(EncryptedString)
    # Versionning chiffrement — permet rotation future sans perte
    mood_type_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    emotions_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    factors_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    notes_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    # Champs non Art.9 (contexte, pas valeur santé)
    place: Mapped[str | None] = mapped_column(Text)
    company: Mapped[str | None] = mapped_column(Text)


class WaterIntake(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "water_intake"
    __table_args__ = (UniqueConstraint("start_time", name="uq_water_time"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    amount_ml: Mapped[float | None] = mapped_column(Float)


# ── daily composites ───────────────────────────────────────────────────────
class ActivityDaily(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "activity_daily"
    __table_args__ = (UniqueConstraint("day_date", name="uq_activity_daily_day"),)

    day_date: Mapped[str] = mapped_column(String(10), nullable=False)
    step_count: Mapped[int | None] = mapped_column(Integer)
    distance_m: Mapped[float | None] = mapped_column(Float)
    calorie_kcal: Mapped[float | None] = mapped_column(Float)
    exercise_time_ms: Mapped[int | None] = mapped_column(Integer)
    active_time_ms: Mapped[int | None] = mapped_column(Integer)
    floor_count: Mapped[float | None] = mapped_column(Float)
    score: Mapped[int | None] = mapped_column(Integer)


class VitalityScore(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "vitality_score"
    __table_args__ = (UniqueConstraint("day_date", name="uq_vitality_day"),)

    day_date: Mapped[str] = mapped_column(String(10), nullable=False)
    total_score: Mapped[float | None] = mapped_column(Float)
    sleep_score: Mapped[float | None] = mapped_column(Float)
    sleep_balance: Mapped[float | None] = mapped_column(Float)
    sleep_regularity: Mapped[float | None] = mapped_column(Float)
    sleep_timing: Mapped[float | None] = mapped_column(Float)
    activity_score: Mapped[float | None] = mapped_column(Float)
    active_time_ms: Mapped[int | None] = mapped_column(Integer)
    mvpa_time_ms: Mapped[int | None] = mapped_column(Integer)
    shr_score: Mapped[float | None] = mapped_column(Float)
    shr_value: Mapped[float | None] = mapped_column(Float)
    shrv_score: Mapped[float | None] = mapped_column(Float)
    shrv_value: Mapped[float | None] = mapped_column(Float)


class FloorsDaily(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "floors_daily"
    __table_args__ = (UniqueConstraint("day_date", name="uq_floors_day"),)

    day_date: Mapped[str] = mapped_column(String(10), nullable=False)
    floor_count: Mapped[int | None] = mapped_column(Integer)


class ActivityLevel(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "activity_level"
    __table_args__ = (UniqueConstraint("start_time", name="uq_activity_level_time"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    activity_level: Mapped[int | None] = mapped_column(Integer)


# ── ECG ────────────────────────────────────────────────────────────────────
class Ecg(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "ecg"
    __table_args__ = (UniqueConstraint("start_time", "end_time", name="uq_ecg_window"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # V2.2.1 — colonnes Art.9 chiffrées
    mean_heart_rate: Mapped[float | None] = mapped_column(EncryptedFloat)
    classification: Mapped[int | None] = mapped_column(EncryptedInt)
    mean_heart_rate_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    classification_crypto_v: Mapped[int] = mapped_column(Integer, nullable=False, default=1, server_default="1")
    # Métadonnées non sensibles (échantillonnage signal, pas valeur santé)
    sample_frequency: Mapped[int | None] = mapped_column(Integer)
    sample_count: Mapped[int | None] = mapped_column(Integer)
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — symbols: `Mood`
- [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9]] — symbols: `SleepSession`, `Weight`, `BloodPressure`, `Stress`, `Spo2`, `HeartRateHourly`, `RespiratoryRate`, `SkinTemperature`, `Ecg`
- [[../../specs/2026-04-24-v2-postgres-migration]] — symbols: `Base`, `SleepSession`, `SleepStage`, `HeartRateHourly`, `StepsDaily`, `StepsHourly`, `ExerciseSession`, `ActivityDaily`, `Stress`, `Spo2`, `RespiratoryRate`, `Hrv`, `SkinTemperature`, `Weight`, `Height`, `BloodPressure`, `Mood`, `WaterIntake`, `VitalityScore`, `FloorsDaily`, `ActivityLevel`, `Ecg`

### Symbols
- `Base` (class) — lines 29-30 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `TimestampedMixin` (class) — lines 33-42
- `Uuid7PkMixin` (class) — lines 45-46
- `SleepSession` (class) — lines 50-77 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `SleepStage` (class) — lines 80-94 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `StepsHourly` (class) — lines 98-104 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `StepsDaily` (class) — lines 107-117 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `HeartRateHourly` (class) — lines 121-134 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `ExerciseSession` (class) — lines 138-153 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Stress` (class) — lines 157-169 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Spo2` (class) — lines 172-190 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `RespiratoryRate` (class) — lines 193-205 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Hrv` (class) — lines 208-213 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `SkinTemperature` (class) — lines 216-229 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Weight` (class) — lines 233-252 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Height` (class) — lines 255-260 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `BloodPressure` (class) — lines 263-276 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Mood` (class) — lines 279-296 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `WaterIntake` (class) — lines 299-304 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `ActivityDaily` (class) — lines 308-319 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `VitalityScore` (class) — lines 322-338 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `FloorsDaily` (class) — lines 341-346 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `ActivityLevel` (class) — lines 349-354 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Ecg` (class) — lines 358-371 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]

### Imports
- `datetime`
- `uuid`
- `sqlalchemy`
- `sqlalchemy.orm`
- `.encrypted`
- `.uuid7`

### Exports
- `Base`
- `TimestampedMixin`
- `Uuid7PkMixin`
- `SleepSession`
- `SleepStage`
- `StepsHourly`
- `StepsDaily`
- `HeartRateHourly`
- `ExerciseSession`
- `Stress`
- `Spo2`
- `RespiratoryRate`
- `Hrv`
- `SkinTemperature`
- `Weight`
- `Height`
- `BloodPressure`
- `Mood`
- `WaterIntake`
- `ActivityDaily`
- `VitalityScore`
- `FloorsDaily`
- `ActivityLevel`
- `Ecg`
