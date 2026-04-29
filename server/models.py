from pydantic import BaseModel
from datetime import datetime


class SleepStageIn(BaseModel):
    stage_type: str
    stage_start: datetime
    stage_end: datetime


class SleepStageOut(BaseModel):
    id: str
    session_id: str
    stage_type: str
    stage_start: str
    stage_end: str


class SleepSessionIn(BaseModel):
    sleep_start: datetime
    sleep_end: datetime
    stages: list[SleepStageIn] | None = None


class SleepSessionOut(BaseModel):
    id: str
    sleep_start: str
    sleep_end: str
    created_at: str | None = None
    stages: list[SleepStageOut] | None = None


class SleepBulkIn(BaseModel):
    sessions: list[SleepSessionIn]


class StepsHourlyIn(BaseModel):
    date: str
    hour: int
    step_count: int


class StepsHourlyOut(BaseModel):
    date: str
    hour: int
    step_count: int


class StepsBulkIn(BaseModel):
    records: list[StepsHourlyIn]


class HeartRateHourlyIn(BaseModel):
    date: str
    hour: int
    min_bpm: int
    max_bpm: int
    avg_bpm: int
    sample_count: int


class HeartRateHourlyOut(BaseModel):
    date: str
    hour: int
    min_bpm: int
    max_bpm: int
    avg_bpm: int
    sample_count: int


class HeartRateBulkIn(BaseModel):
    records: list[HeartRateHourlyIn]


class ExerciseSessionIn(BaseModel):
    exercise_type: str
    exercise_start: str
    exercise_end: str
    duration_minutes: float


class ExerciseSessionOut(BaseModel):
    exercise_type: str
    exercise_start: str
    exercise_end: str
    duration_minutes: float


class ExerciseBulkIn(BaseModel):
    sessions: list[ExerciseSessionIn]


# V2.2 — mood (champs Art.9 chiffrés côté DB, types python natifs côté API)
class MoodIn(BaseModel):
    start_time: str
    mood_type: int | None = None
    emotions: str | None = None
    factors: str | None = None
    notes: str | None = None
    place: str | None = None
    company: str | None = None


class MoodOut(BaseModel):
    start_time: str | None
    mood_type: int | None = None
    emotions: str | None = None
    factors: str | None = None
    notes: str | None = None
    place: str | None = None
    company: str | None = None


class MoodBulkIn(BaseModel):
    entries: list[MoodIn]
