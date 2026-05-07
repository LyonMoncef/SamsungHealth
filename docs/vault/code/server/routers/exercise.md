---
type: code-source
language: python
file_path: server/routers/exercise.py
git_blob: d29256706dbcd1788c6d731961a62ca98789c764
last_synced: '2026-05-07T16:11:01Z'
loc: 121
annotations: []
imports:
- datetime
- fastapi
- sqlalchemy
- sqlalchemy.dialects.postgresql
- sqlalchemy.orm
- server.database
- server.db.models
- server.logging_config
- server.models
- server.security.auth
- server.security.rate_limit
- server.services.csv_import
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

from fastapi import APIRouter, Depends, File, HTTPException, Query, Request, Response, UploadFile
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import ExerciseSession, User
from server.logging_config import get_logger
from server.models import ExerciseBulkIn, ExerciseSessionOut
from server.security.auth import get_current_user
from server.security.rate_limit import _api_post_cap, _user_id_key, limiter
from server.services.csv_import import MAX_CSV_BYTES, parse_exercise_rows, parse_samsung_csv

_log = get_logger(__name__)

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
@limiter.limit(_api_post_cap, key_func=_user_id_key)
def create_exercise(
    request: Request,
    body: ExerciseBulkIn,
    response: Response,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    inserted = 0
    skipped = 0
    for s in body.sessions:
        stmt = (
            pg_insert(ExerciseSession)
            .values(
                user_id=current_user.id,
                exercise_type=s.exercise_type,
                exercise_start=_to_dt(s.exercise_start),
                exercise_end=_to_dt(s.exercise_end),
                duration_minutes=s.duration_minutes,
            )
            .on_conflict_do_nothing(
                index_elements=["user_id", "exercise_start", "exercise_end"]
            )
            .returning(ExerciseSession.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


@router.post("/import", status_code=200)
@limiter.limit(_api_post_cap, key_func=_user_id_key)
def import_exercise(
    request: Request,
    response: Response,
    file: UploadFile = File(...),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    raw = file.file.read(MAX_CSV_BYTES + 1)
    if len(raw) > MAX_CSV_BYTES:
        raise HTTPException(status_code=413, detail="file_too_large")
    try:
        rows = parse_samsung_csv(raw)
    except (UnicodeDecodeError, ValueError):
        raise HTTPException(status_code=422, detail="invalid_csv_encoding")
    inserted, skipped = parse_exercise_rows(rows, current_user.id, db)
    _log.info(
        "csv_import.done",
        endpoint="exercise",
        inserted=inserted,
        skipped=skipped,
        user_id=str(current_user.id),
        filename=file.filename[:255] if file.filename else None,
    )
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_exercise(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[ExerciseSessionOut]:
    stmt = select(ExerciseSession).where(ExerciseSession.user_id == current_user.id)
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
- [[../../specs/2026-04-26-v2-auth-foundation]] — symbols: `router`
- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — symbols: `router`

### Symbols
- `_to_dt` (function) — lines 21-25
- `_iso` (function) — lines 28-31

### Imports
- `datetime`
- `fastapi`
- `sqlalchemy`
- `sqlalchemy.dialects.postgresql`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.logging_config`
- `server.models`
- `server.security.auth`
- `server.security.rate_limit`
- `server.services.csv_import`

### Exports
- `_to_dt`
- `_iso`
