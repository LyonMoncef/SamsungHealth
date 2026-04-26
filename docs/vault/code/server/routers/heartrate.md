---
type: code-source
language: python
file_path: server/routers/heartrate.py
git_blob: d31e04b26f8455f0b21c6b82c4290cea518da88e
last_synced: '2026-04-26T16:48:27Z'
loc: 72
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

# server/routers/heartrate.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/heartrate.py`](../../../server/routers/heartrate.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import HeartRateHourly, User
from server.logging_config import get_logger
from server.models import HeartRateBulkIn, HeartRateHourlyOut
from server.security.auth import get_current_user

_log = get_logger(__name__)

router = APIRouter(prefix="/api/heartrate", tags=["heartrate"])


@router.post("", status_code=201)
def create_heartrate(
    body: HeartRateBulkIn,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    inserted = 0
    skipped = 0
    for r in body.records:
        stmt = (
            pg_insert(HeartRateHourly)
            .values(
                user_id=current_user.id,
                date=r.date,
                hour=r.hour,
                min_bpm=r.min_bpm,
                max_bpm=r.max_bpm,
                avg_bpm=r.avg_bpm,
                sample_count=r.sample_count,
            )
            .on_conflict_do_nothing(index_elements=["user_id", "date", "hour"])
            .returning(HeartRateHourly.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_heartrate(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[HeartRateHourlyOut]:
    stmt = select(HeartRateHourly).where(HeartRateHourly.user_id == current_user.id)
    if from_date:
        stmt = stmt.where(HeartRateHourly.date >= from_date)
    if to_date:
        stmt = stmt.where(HeartRateHourly.date <= to_date)
    stmt = stmt.order_by(HeartRateHourly.date, HeartRateHourly.hour)
    rows = db.execute(stmt).scalars().all()
    return [
        HeartRateHourlyOut(
            date=r.date,
            hour=r.hour,
            min_bpm=r.min_bpm,
            max_bpm=r.max_bpm,
            avg_bpm=r.avg_bpm,
            sample_count=r.sample_count,
        )
        for r in rows
    ]
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
