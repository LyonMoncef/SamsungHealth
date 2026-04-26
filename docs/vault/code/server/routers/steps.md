---
type: code-source
language: python
file_path: server/routers/steps.py
git_blob: 8b149e46b9d4e3efd8f9a40e85812facdec5c0c1
last_synced: '2026-04-26T16:48:27Z'
loc: 59
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
from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import StepsHourly, User
from server.logging_config import get_logger
from server.models import StepsBulkIn, StepsHourlyOut
from server.security.auth import get_current_user

_log = get_logger(__name__)

router = APIRouter(prefix="/api/steps", tags=["steps"])


@router.post("", status_code=201)
def create_steps(
    body: StepsBulkIn,
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
