---
type: code-source
language: python
file_path: server/routers/steps.py
git_blob: 2e35238b4d5d206b217cca8a19f0b1d348f68cbf
last_synced: '2026-05-07T16:11:01Z'
loc: 92
annotations: []
imports:
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
exports: []
tags:
- code
- python
coverage_pct: 22.857142857142858
---

# server/routers/steps.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/steps.py`](../../../server/routers/steps.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from fastapi import APIRouter, Depends, File, HTTPException, Query, Request, Response, UploadFile
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import StepsHourly, User
from server.logging_config import get_logger
from server.models import StepsBulkIn, StepsHourlyOut
from server.security.auth import get_current_user
from server.security.rate_limit import _api_post_cap, _user_id_key, limiter
from server.services.csv_import import MAX_CSV_BYTES, parse_samsung_csv, parse_steps_rows

_log = get_logger(__name__)

router = APIRouter(prefix="/api/steps", tags=["steps"])


@router.post("", status_code=201)
@limiter.limit(_api_post_cap, key_func=_user_id_key)
def create_steps(
    request: Request,
    body: StepsBulkIn,
    response: Response,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    inserted = 0
    skipped = 0
    for r in body.records:
        stmt = (
            pg_insert(StepsHourly)
            .values(
                user_id=current_user.id,
                date=r.date,
                hour=r.hour,
                step_count=r.step_count,
            )
            .on_conflict_do_nothing(index_elements=["user_id", "date", "hour"])
            .returning(StepsHourly.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


@router.post("/import", status_code=200)
@limiter.limit(_api_post_cap, key_func=_user_id_key)
def import_steps(
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
    inserted, skipped = parse_steps_rows(rows, current_user.id, db)
    _log.info(
        "csv_import.done",
        endpoint="steps",
        inserted=inserted,
        skipped=skipped,
        user_id=str(current_user.id),
        filename=file.filename[:255] if file.filename else None,
    )
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_steps(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[StepsHourlyOut]:
    stmt = select(StepsHourly).where(StepsHourly.user_id == current_user.id)
    if from_date:
        stmt = stmt.where(StepsHourly.date >= from_date)
    if to_date:
        stmt = stmt.where(StepsHourly.date <= to_date)
    stmt = stmt.order_by(StepsHourly.date, StepsHourly.hour)
    rows = db.execute(stmt).scalars().all()
    return [StepsHourlyOut(date=r.date, hour=r.hour, step_count=r.step_count) for r in rows]
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-postgres-routers-cutover]] — symbols: `router`
- [[../../specs/2026-04-26-v2-auth-foundation]] — symbols: `router`
- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — symbols: `router`

### Imports
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
