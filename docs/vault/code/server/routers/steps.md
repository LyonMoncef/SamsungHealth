---
type: code-source
language: python
file_path: server/routers/steps.py
git_blob: f0d6906726af0d195eb944d4c205906ba5aba7df
last_synced: '2026-04-24T02:28:09Z'
loc: 45
annotations: []
imports:
- fastapi
- sqlalchemy
- sqlalchemy.dialects.postgresql
- sqlalchemy.orm
- server.database
- server.db.models
- server.models
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
from server.db.models import StepsHourly
from server.models import StepsBulkIn, StepsHourlyOut

router = APIRouter(prefix="/api/steps", tags=["steps"])


@router.post("", status_code=201)
def create_steps(body: StepsBulkIn, db: Session = Depends(get_session)) -> dict:
    inserted = 0
    skipped = 0
    for r in body.records:
        stmt = (
            pg_insert(StepsHourly)
            .values(date=r.date, hour=r.hour, step_count=r.step_count)
            .on_conflict_do_nothing(index_elements=["date", "hour"])
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
) -> list[StepsHourlyOut]:
    stmt = select(StepsHourly)
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

### Imports
- `fastapi`
- `sqlalchemy`
- `sqlalchemy.dialects.postgresql`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.models`
