---
type: code-source
language: python
file_path: server/db/models.py
git_blob: be17fbee6197f85733569708f3d3bd34d6778f71
last_synced: '2026-04-24T03:44:10Z'
loc: 327
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

from .encrypted import EncryptedInt, EncryptedString
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
    sleep_score: Mapped[int | None] = mapped_column(Integer)
    efficiency: Mapped[float | None] = mapped_column(Float)
    sleep_duration_min: Mapped[int | None] = mapped_column(Integer)
    sleep_cycle: Mapped[int | None] = mapped_column(Integer)
    mental_recovery: Mapped[float | None] = mapped_column(Float)
    physical_recovery: Mapped[float | None] = mapped_column(Float)
    sleep_type: Mapped[int | None] = mapped_column(Integer)

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
    min_bpm: Mapped[int] = mapped_column(Integer, nullable=False)
    max_bpm: Mapped[int] = mapped_column(Integer, nullable=False)
    avg_bpm: Mapped[int] = mapped_column(Integer, nullable=False)
    sample_count: Mapped[int] = mapped_column(Integer, nullable=False)


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
    score: Mapped[float | None] = mapped_column(Float)
    tag_id: Mapped[int | None] = mapped_column(Integer)


class Spo2(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "spo2"
    __table_args__ = (
        UniqueConstraint("start_time", "end_time", name="uq_spo2_window"),
        Index("idx_spo2_start", "start_time"),
    )

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    spo2: Mapped[float | None] = mapped_column(Float)
    min_spo2: Mapped[float | None] = mapped_column(Float)
    max_spo2: Mapped[float | None] = mapped_column(Float)
    low_duration_s: Mapped[int | None] = mapped_column(Integer)
    tag_id: Mapped[int | None] = mapped_column(Integer)


class RespiratoryRate(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "respiratory_rate"
    __table_args__ = (UniqueConstraint("start_time", "end_time", name="uq_respi_window"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    average: Mapped[float | None] = mapped_column(Float)
    lower_limit: Mapped[float | None] = mapped_column(Float)
    upper_limit: Mapped[float | None] = mapped_column(Float)


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
    temperature: Mapped[float | None] = mapped_column(Float)
    min_temp: Mapped[float | None] = mapped_column(Float)
    max_temp: Mapped[float | None] = mapped_column(Float)
    tag_id: Mapped[int | None] = mapped_column(Integer)


# ── weight / height / bp / mood / water ────────────────────────────────────
class Weight(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "weight"
    __table_args__ = (UniqueConstraint("start_time", name="uq_weight_time"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    weight_kg: Mapped[float | None] = mapped_column(Float)
    body_fat_pct: Mapped[float | None] = mapped_column(Float)
    skeletal_muscle_pct: Mapped[float | None] = mapped_column(Float)
    skeletal_muscle_mass_kg: Mapped[float | None] = mapped_column(Float)
    fat_free_mass_kg: Mapped[float | None] = mapped_column(Float)
    basal_metabolic_rate: Mapped[int | None] = mapped_column(Integer)
    total_body_water_kg: Mapped[float | None] = mapped_column(Float)


class Height(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "height"
    __table_args__ = (UniqueConstraint("start_time", name="uq_height_time"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    height_cm: Mapped[float | None] = mapped_column(Float)


class BloodPressure(Uuid7PkMixin, TimestampedMixin, Base):
    __tablename__ = "blood_pressure"
    __table_args__ = (UniqueConstraint("start_time", name="uq_bp_time"),)

    start_time: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    systolic: Mapped[float | None] = mapped_column(Float)
    diastolic: Mapped[float | None] = mapped_column(Float)
    pulse: Mapped[int | None] = mapped_column(Integer)
    mean_bp: Mapped[float | None] = mapped_column(Float)


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
    mean_heart_rate: Mapped[float | None] = mapped_column(Float)
    sample_frequency: Mapped[int | None] = mapped_column(Integer)
    sample_count: Mapped[int | None] = mapped_column(Integer)
    classification: Mapped[int | None] = mapped_column(Integer)
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — symbols: `Mood`
- [[../../specs/2026-04-24-v2-postgres-migration]] — symbols: `Base`, `SleepSession`, `SleepStage`, `HeartRateHourly`, `StepsDaily`, `StepsHourly`, `ExerciseSession`, `ActivityDaily`, `Stress`, `Spo2`, `RespiratoryRate`, `Hrv`, `SkinTemperature`, `Weight`, `Height`, `BloodPressure`, `Mood`, `WaterIntake`, `VitalityScore`, `FloorsDaily`, `ActivityLevel`, `Ecg`

### Symbols
- `Base` (class) — lines 29-30 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `TimestampedMixin` (class) — lines 33-42
- `Uuid7PkMixin` (class) — lines 45-46
- `SleepSession` (class) — lines 50-69 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `SleepStage` (class) — lines 72-86 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `StepsHourly` (class) — lines 90-96 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `StepsDaily` (class) — lines 99-109 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `HeartRateHourly` (class) — lines 113-122 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `ExerciseSession` (class) — lines 126-141 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Stress` (class) — lines 145-155 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Spo2` (class) — lines 158-171 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `RespiratoryRate` (class) — lines 174-182 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Hrv` (class) — lines 185-190 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `SkinTemperature` (class) — lines 193-202 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Weight` (class) — lines 206-217 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Height` (class) — lines 220-225 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `BloodPressure` (class) — lines 228-236 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Mood` (class) — lines 239-256 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]], [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `WaterIntake` (class) — lines 259-264 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `ActivityDaily` (class) — lines 268-279 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `VitalityScore` (class) — lines 282-298 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `FloorsDaily` (class) — lines 301-306 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `ActivityLevel` (class) — lines 309-314 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Ecg` (class) — lines 318-327 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]

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
