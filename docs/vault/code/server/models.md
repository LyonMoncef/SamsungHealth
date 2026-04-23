---
type: code-source
language: python
file_path: server/models.py
git_blob: 5706f27c34bd6e78819147bc5227fa99fdc383b2
last_synced: '2026-04-23T08:13:16Z'
loc: 90
annotations: []
imports:
- pydantic
- datetime
exports:
- SleepStageIn
- SleepStageOut
- SleepSessionIn
- SleepSessionOut
- SleepBulkIn
- StepsHourlyIn
- StepsHourlyOut
- StepsBulkIn
- HeartRateHourlyIn
- HeartRateHourlyOut
- HeartRateBulkIn
- ExerciseSessionIn
- ExerciseSessionOut
- ExerciseBulkIn
tags:
- code
- python
---

# server/models.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/models.py`](../../../server/models.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepStageIn` (class) — lines 5-8
- `SleepStageOut` (class) — lines 11-16
- `SleepSessionIn` (class) — lines 19-22
- `SleepSessionOut` (class) — lines 25-30
- `SleepBulkIn` (class) — lines 33-34
- `StepsHourlyIn` (class) — lines 37-40
- `StepsHourlyOut` (class) — lines 43-46
- `StepsBulkIn` (class) — lines 49-50
- `HeartRateHourlyIn` (class) — lines 53-59
- `HeartRateHourlyOut` (class) — lines 62-68
- `HeartRateBulkIn` (class) — lines 71-72
- `ExerciseSessionIn` (class) — lines 75-79
- `ExerciseSessionOut` (class) — lines 82-86
- `ExerciseBulkIn` (class) — lines 89-90

### Imports
- `pydantic`
- `datetime`

### Exports
- `SleepStageIn`
- `SleepStageOut`
- `SleepSessionIn`
- `SleepSessionOut`
- `SleepBulkIn`
- `StepsHourlyIn`
- `StepsHourlyOut`
- `StepsBulkIn`
- `HeartRateHourlyIn`
- `HeartRateHourlyOut`
- `HeartRateBulkIn`
- `ExerciseSessionIn`
- `ExerciseSessionOut`
- `ExerciseBulkIn`
