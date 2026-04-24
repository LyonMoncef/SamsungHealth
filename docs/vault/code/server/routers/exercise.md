---
type: code-source
language: python
file_path: server/routers/exercise.py
git_blob: 1ab8769f4fd06a4be34cd32f3700965266ae9586
last_synced: '2026-04-24T02:28:09Z'
loc: 76
annotations: []
imports:
- datetime
- fastapi
- sqlalchemy
- sqlalchemy.dialects.postgresql
- sqlalchemy.orm
- server.database
- server.db.models
- server.models
exports:
- _to_dt
- _iso
tags:
- code
- python
coverage_pct: 22.857142857142858
---

# server/routers/exercise.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/exercise.py`](../../../server/routers/exercise.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import ExerciseSession
from server.models import ExerciseBulkIn, ExerciseSessionOut

router = APIRouter(prefix="/api/exercise", tags=["exercise"])


def _to_dt(s: str) -> datetime:
    dt = datetime.fromisoformat(s)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt


def _iso(dt: datetime | None) -> str:
    if dt is None:
        return ""
    return dt.isoformat()


@router.post("", status_code=201)
def create_exercise(body: ExerciseBulkIn, db: Session = Depends(get_session)) -> dict:
    inserted = 0
    skipped = 0
    for s in body.sessions:
        stmt = (
            pg_insert(ExerciseSession)
            .values(
                exercise_type=s.exercise_type,
                exercise_start=_to_dt(s.exercise_start),
                exercise_end=_to_dt(s.exercise_end),
                duration_minutes=s.duration_minutes,
            )
            .on_conflict_do_nothing(index_elements=["exercise_start", "exercise_end"])
            .returning(ExerciseSession.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_exercise(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    db: Session = Depends(get_session),
) -> list[ExerciseSessionOut]:
    stmt = select(ExerciseSession)
    if from_date:
        d_from = datetime.fromisoformat(from_date).replace(tzinfo=timezone.utc)
        stmt = stmt.where(ExerciseSession.exercise_start >= d_from)
    if to_date:
        d_to = datetime.fromisoformat(to_date).replace(tzinfo=timezone.utc)
        d_to_eod = d_to.replace(hour=23, minute=59, second=59)
        stmt = stmt.where(ExerciseSession.exercise_start <= d_to_eod)
    stmt = stmt.order_by(ExerciseSession.exercise_start)
    rows = db.execute(stmt).scalars().all()
    return [
        ExerciseSessionOut(
            exercise_type=r.exercise_type,
            exercise_start=_iso(r.exercise_start),
            exercise_end=_iso(r.exercise_end),
            duration_minutes=r.duration_minutes,
        )
        for r in rows
    ]
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-postgres-routers-cutover]] — symbols: `router`

### Symbols
- `_to_dt` (function) — lines 15-19
- `_iso` (function) — lines 22-25

### Imports
- `datetime`
- `fastapi`
- `sqlalchemy`
- `sqlalchemy.dialects.postgresql`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.models`

### Exports
- `_to_dt`
- `_iso`
