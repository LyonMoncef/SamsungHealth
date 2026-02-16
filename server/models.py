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
