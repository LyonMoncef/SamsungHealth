---
type: code-source
language: python
file_path: server/models.py
git_blob: 3068f278a792f3b0f6ec526f5d08dad1990cb277
last_synced: '2026-04-24T03:44:10Z'
loc: 115
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
- MoodIn
- MoodOut
- MoodBulkIn
tags:
- code
- python
coverage_pct: 100.0
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
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — symbols: `MoodIn`, `MoodOut`, `MoodBulkIn`
- [[../../specs/2026-04-24-v2-postgres-routers-cutover]] — symbols: `SleepSessionOut`, `SleepStageOut`

### Symbols
- `SleepStageIn` (class) — lines 5-8
- `SleepStageOut` (class) — lines 11-16 · **Specs**: [[../../specs/2026-04-24-v2-postgres-routers-cutover|2026-04-24-v2-postgres-routers-cutover]]
- `SleepSessionIn` (class) — lines 19-22
- `SleepSessionOut` (class) — lines 25-30 · **Specs**: [[../../specs/2026-04-24-v2-postgres-routers-cutover|2026-04-24-v2-postgres-routers-cutover]]
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
- `MoodIn` (class) — lines 94-101 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `MoodOut` (class) — lines 104-111 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `MoodBulkIn` (class) — lines 114-115 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]

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
- `MoodIn`
- `MoodOut`
- `MoodBulkIn`
