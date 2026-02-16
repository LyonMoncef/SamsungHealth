from pydantic import BaseModel
from datetime import datetime


class SleepSessionIn(BaseModel):
    sleep_start: datetime
    sleep_end: datetime


class SleepSessionOut(BaseModel):
    id: int
    sleep_start: str
    sleep_end: str
    created_at: str


class SleepBulkIn(BaseModel):
    sessions: list[SleepSessionIn]
