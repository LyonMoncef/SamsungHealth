---
type: code-source
language: python
file_path: server/db/models.py
git_blob: 299e9a712ebefc022813937829372f172007a46c
last_synced: '2026-04-27T17:56:06Z'
loc: 610
annotations: []
imports:
- datetime
- uuid
- sqlalchemy
- sqlalchemy.dialects.postgresql
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
- User
- RefreshToken
- AuthEvent
- VerificationToken
- IdentityProvider
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
- V2.3 — colonne `user_id UUID NULL` + index sur les 22 tables santé (FK users)
"""
from datetime import datetime
from uuid import UUID

from sqlalchemy import (
    Boolean,
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
from sqlalchemy.dialects.postgresql import CITEXT, INET, JSONB
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
        UniqueConstraint("user_id", "sleep_start", "sleep_end", name="uq_sleep_sessions_window"),
        Index("idx_sleep_start", "sleep_start"),
        Index("idx_sleep_sessions_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
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
        UniqueConstraint("user_id", "stage_start", "stage_end", name="uq_sleep_stages_window"),
        Index("idx_stages_session", "session_id"),
        Index("idx_sleep_stages_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
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
    __table_args__ = (
        UniqueConstraint("user_id", "date", "hour", name="uq_steps_hourly_slot"),
        Index("idx_steps_hourly_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
    date: Mapped[str] = mapped_column(String(10), nullable=False)
    hour: Mapped[int] = mapped_column(Integer, nullable=False)
    step_count: Mapped[int] = mapped_column(Integer, nullable=False)


class StepsDaily(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "steps_daily"
    __table_args__ = (
        UniqueConstraint("user_id", "day_date", name="uq_steps_daily_day"),
        Index("idx_steps_daily_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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
    __table_args__ = (
        UniqueConstraint("user_id", "date", "hour", name="uq_hr_hourly_slot"),
        Index("idx_heart_rate_hourly_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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
        UniqueConstraint("user_id", "exercise_start", "exercise_end", name="uq_exercise_window"),
        Index("idx_exercise_sessions_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
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
        UniqueConstraint("user_id", "start_time", "end_time", name="uq_stress_window"),
        Index("idx_stress_start", "start_time"),
        Index("idx_stress_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
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
        UniqueConstraint("user_id", "start_time", "end_time", name="uq_spo2_window"),
        Index("idx_spo2_start", "start_time"),
        Index("idx_spo2_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
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
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", "end_time", name="uq_respi_window"),
        Index("idx_respiratory_rate_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", "end_time", name="uq_hrv_window"),
        Index("idx_hrv_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class SkinTemperature(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "skin_temperature"
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", "end_time", name="uq_skin_temp_window"),
        Index("idx_skin_temperature_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", name="uq_weight_time"),
        Index("idx_weight_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", name="uq_height_time"),
        Index("idx_height_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    height_cm: Mapped[float | None] = mapped_column(Float)


class BloodPressure(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "blood_pressure"
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", name="uq_bp_time"),
        Index("idx_blood_pressure_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", name="uq_mood_time"),
        Index("idx_mood_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", name="uq_water_time"),
        Index("idx_water_intake_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    amount_ml: Mapped[float | None] = mapped_column(Float)


# ── daily composites ───────────────────────────────────────────────────────
class ActivityDaily(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "activity_daily"
    __table_args__ = (
        UniqueConstraint("user_id", "day_date", name="uq_activity_daily_day"),
        Index("idx_activity_daily_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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
    __table_args__ = (
        UniqueConstraint("user_id", "day_date", name="uq_vitality_day"),
        Index("idx_vitality_score_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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
    __table_args__ = (
        UniqueConstraint("user_id", "day_date", name="uq_floors_day"),
        Index("idx_floors_daily_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
    day_date: Mapped[str] = mapped_column(String(10), nullable=False)
    floor_count: Mapped[int | None] = mapped_column(Integer)


class ActivityLevel(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "activity_level"
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", name="uq_activity_level_time"),
        Index("idx_activity_level_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    activity_level: Mapped[int | None] = mapped_column(Integer)


# ── ECG ────────────────────────────────────────────────────────────────────
class Ecg(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "ecg"
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", "end_time", name="uq_ecg_window"),
        Index("idx_ecg_user_id", "user_id"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=False
    )
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


# ── V2.3 auth foundation ───────────────────────────────────────────────────
class User(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "users"

    email: Mapped[str] = mapped_column(CITEXT(), nullable=False, unique=True)
    password_hash: Mapped[str] = mapped_column(Text, nullable=False)
    email_verified_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    failed_login_count: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0, server_default="0"
    )
    locked_until: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    last_failed_login_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    last_login_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    last_login_ip: Mapped[str | None] = mapped_column(INET)
    password_changed_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    is_active: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=True, server_default="true"
    )


class RefreshToken(Uuid7PkMixin, Base):
    __tablename__ = "refresh_tokens"
    __table_args__ = (
        Index("idx_refresh_tokens_user_id", "user_id"),
        Index("idx_refresh_tokens_jti", "jti", unique=True),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    jti: Mapped[UUID] = mapped_column(Uuid7(), nullable=False, unique=True)
    issued_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    replaced_by: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("refresh_tokens.id"), nullable=True
    )
    last_used_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    user_agent: Mapped[str | None] = mapped_column(Text)
    ip: Mapped[str | None] = mapped_column(INET)


class AuthEvent(Uuid7PkMixin, Base):
    __tablename__ = "auth_events"
    __table_args__ = (
        Index("idx_auth_events_user_id", "user_id"),
        Index("idx_auth_events_event_type", "event_type"),
    )

    event_type: Mapped[str] = mapped_column(Text, nullable=False)
    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id", ondelete="SET NULL"), nullable=True
    )
    email_hash: Mapped[str | None] = mapped_column(Text)
    ip: Mapped[str | None] = mapped_column(INET)
    user_agent: Mapped[str | None] = mapped_column(Text)
    request_id: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


# ── V2.3.1 verification tokens (email verification + password reset) ──────
class VerificationToken(Uuid7PkMixin, Base):
    __tablename__ = "verification_tokens"
    __table_args__ = (
        Index("idx_verification_tokens_user_id", "user_id"),
        Index("idx_verification_tokens_purpose", "purpose"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    token_hash: Mapped[str] = mapped_column(Text, nullable=False, unique=True)
    purpose: Mapped[str] = mapped_column(Text, nullable=False)
    issued_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    consumed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    ip: Mapped[str | None] = mapped_column(INET)
    user_agent: Mapped[str | None] = mapped_column(Text)
    # V2.3.2 — payload arbitraire (utilisé pour oauth_link_confirm: provider+sub+email+raw_claims).
    payload: Mapped[dict | None] = mapped_column(JSONB, nullable=True)


# ── V2.3.2 OAuth — identity providers (Google + futur Apple/MS) ───────────
class IdentityProvider(Uuid7PkMixin, Base):
    __tablename__ = "identity_providers"
    __table_args__ = (
        UniqueConstraint(
            "provider", "provider_sub", name="uq_identity_providers_provider_sub"
        ),
        UniqueConstraint(
            "user_id", "provider", name="uq_identity_providers_user_provider"
        ),
        Index("idx_identity_providers_user_id", "user_id"),
        Index("idx_identity_providers_provider_sub", "provider", "provider_sub"),
    )

    user_id: Mapped[UUID] = mapped_column(
        Uuid7(), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    provider: Mapped[str] = mapped_column(Text, nullable=False)
    provider_sub: Mapped[str] = mapped_column(Text, nullable=False)
    provider_email: Mapped[str | None] = mapped_column(Text, nullable=True)
    email_verified: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=False, server_default="false"
    )
    linked_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    last_used_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    raw_claims: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — symbols: `Mood`
- [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9]] — symbols: `SleepSession`, `Weight`, `BloodPressure`, `Stress`, `Spo2`, `HeartRateHourly`, `RespiratoryRate`, `SkinTemperature`, `Ecg`
- [[../../specs/2026-04-24-v2-postgres-migration]] — symbols: `Base`, `SleepSession`, `SleepStage`, `HeartRateHourly`, `StepsDaily`, `StepsHourly`, `ExerciseSession`, `ActivityDaily`, `Stress`, `Spo2`, `RespiratoryRate`, `Hrv`, `SkinTemperature`, `Weight`, `Height`, `BloodPressure`, `Mood`, `WaterIntake`, `VitalityScore`, `FloorsDaily`, `ActivityLevel`, `Ecg`
- [[../../specs/2026-04-26-v2-auth-foundation]] — symbols: `User`, `RefreshToken`, `AuthEvent`
- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — symbols: `VerificationToken`, `User`
- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — symbols: `IdentityProvider`, `VerificationToken`
- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — symbols: `User`

### Symbols
- `Base` (class) — lines 32-33 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `TimestampedMixin` (class) — lines 36-45
- `Uuid7PkMixin` (class) — lines 48-49
- `SleepSession` (class) — lines 53-84 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `SleepStage` (class) — lines 87-105 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `StepsHourly` (class) — lines 109-121 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `StepsDaily` (class) — lines 124-140 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `HeartRateHourly` (class) — lines 144-163 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `ExerciseSession` (class) — lines 167-186 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Stress` (class) — lines 190-206 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Spo2` (class) — lines 209-231 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `RespiratoryRate` (class) — lines 234-252 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Hrv` (class) — lines 255-266 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `SkinTemperature` (class) — lines 269-288 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Weight` (class) — lines 292-317 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Height` (class) — lines 320-331 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `BloodPressure` (class) — lines 334-353 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Mood` (class) — lines 356-379 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `WaterIntake` (class) — lines 382-393 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `ActivityDaily` (class) — lines 397-414 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `VitalityScore` (class) — lines 417-439 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `FloorsDaily` (class) — lines 442-453 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `ActivityLevel` (class) — lines 456-467 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Ecg` (class) — lines 471-490 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-extend-art9|2026-04-24-v2-aes256-gcm-extend-art9]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `User` (class) — lines 494-512 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]], [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify|2026-04-26-v2.3.1-reset-password-email-verify]], [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout|2026-04-26-v2.3.3.1-rate-limit-lockout]]
- `RefreshToken` (class) — lines 515-536 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `AuthEvent` (class) — lines 539-556 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `VerificationToken` (class) — lines 560-580 · **Specs**: [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify|2026-04-26-v2.3.1-reset-password-email-verify]], [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]
- `IdentityProvider` (class) — lines 584-610 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]

### Imports
- `datetime`
- `uuid`
- `sqlalchemy`
- `sqlalchemy.dialects.postgresql`
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
- `User`
- `RefreshToken`
- `AuthEvent`
- `VerificationToken`
- `IdentityProvider`
