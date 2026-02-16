from pydantic import BaseModel
from datetime import datetime


class SleepStageIn(BaseModel):
    stage_type: str
    stage_start: datetime
    stage_end: datetime


class SleepStageOut(BaseModel):
    id: int
    session_id: int
    stage_type: str
    stage_start: str
    stage_end: str


class SleepSessionIn(BaseModel):
    sleep_start: datetime
    sleep_end: datetime
    stages: list[SleepStageIn] | None = None


class SleepSessionOut(BaseModel):
    id: int
    sleep_start: str
    sleep_end: str
    created_at: str
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
