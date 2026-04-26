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
from sqlalchemy.dialects.postgresql import CITEXT, INET
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
    )
    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class SkinTemperature(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "skin_temperature"
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", "end_time", name="uq_skin_temp_window"),
        Index("idx_skin_temperature_user_id", "user_id"),
    )

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
    )
    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    height_cm: Mapped[float | None] = mapped_column(Float)


class BloodPressure(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "blood_pressure"
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", name="uq_bp_time"),
        Index("idx_blood_pressure_user_id", "user_id"),
    )

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
    )
    day_date: Mapped[str] = mapped_column(String(10), nullable=False)
    floor_count: Mapped[int | None] = mapped_column(Integer)


class ActivityLevel(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "activity_level"
    __table_args__ = (
        UniqueConstraint("user_id", "start_time", name="uq_activity_level_time"),
        Index("idx_activity_level_user_id", "user_id"),
    )

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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

    user_id: Mapped[UUID | None] = mapped_column(
        Uuid7(), ForeignKey("users.id"), nullable=True
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
