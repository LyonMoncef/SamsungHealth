---
type: code-source
language: python
file_path: server/routers/sleep.py
git_blob: 66005bbe5a93a6511be0a0ef66a533a5b8ca1158
last_synced: '2026-04-26T16:48:27Z'
loc: 116
annotations: []
imports:
- datetime
- fastapi
- sqlalchemy
- sqlalchemy.orm
- server.database
- server.db.models
- server.logging_config
- server.models
- server.security.auth
exports:
- _parse_day
- _to_iso
- _serialize
tags:
- code
- python
coverage_pct: 87.5
---

# server/routers/sleep.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/sleep.py`](../../../server/routers/sleep.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from datetime import date, datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from server.database import get_session
from server.db.models import SleepSession, SleepStage, User
from server.logging_config import get_logger
from server.models import SleepBulkIn, SleepSessionOut, SleepStageOut
from server.security.auth import get_current_user

_log = get_logger(__name__)

router = APIRouter(prefix="/api/sleep", tags=["sleep"])


def _parse_day(s: str) -> date:
    return datetime.fromisoformat(s).date()


@router.post("", status_code=201)
def create_sleep_sessions(
    body: SleepBulkIn,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    inserted = 0
    skipped = 0
    for s in body.sessions:
        existing = db.execute(
            select(SleepSession).where(
                SleepSession.user_id == current_user.id,
                SleepSession.sleep_start == s.sleep_start,
                SleepSession.sleep_end == s.sleep_end,
            )
        ).scalar_one_or_none()
        if existing is not None:
            skipped += 1
            continue
        new_session = SleepSession(
            user_id=current_user.id,
            sleep_start=s.sleep_start,
            sleep_end=s.sleep_end,
        )
        db.add(new_session)
        db.flush()
        if s.stages:
            for st in s.stages:
                db.add(
                    SleepStage(
                        user_id=current_user.id,
                        session_id=new_session.id,
                        stage_type=st.stage_type,
                        stage_start=st.stage_start,
                        stage_end=st.stage_end,
                    )
                )
        inserted += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


def _to_iso(value) -> str:
    if value is None:
        return ""
    if isinstance(value, datetime):
        return value.isoformat()
    return str(value)


def _serialize(session: SleepSession, include_stages: bool) -> SleepSessionOut:
    out = SleepSessionOut(
        id=str(session.id),
        sleep_start=_to_iso(session.sleep_start),
        sleep_end=_to_iso(session.sleep_end),
        created_at=_to_iso(session.created_at) if session.created_at else None,
    )
    if include_stages:
        out.stages = [
            SleepStageOut(
                id=str(st.id),
                session_id=str(st.session_id),
                stage_type=st.stage_type,
                stage_start=_to_iso(st.stage_start),
                stage_end=_to_iso(st.stage_end),
            )
            for st in session.stages
        ]
    return out


@router.get("")
def get_sleep_sessions(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    include_stages: bool = Query(False),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[SleepSessionOut]:
    stmt = select(SleepSession).where(SleepSession.user_id == current_user.id)
    if include_stages:
        stmt = stmt.options(selectinload(SleepSession.stages))

    if from_date:
        d_from = _parse_day(from_date)
        start_dt = datetime.combine(d_from, datetime.min.time(), tzinfo=timezone.utc)
        stmt = stmt.where(SleepSession.sleep_start >= start_dt)
    if to_date:
        d_to = _parse_day(to_date) + timedelta(days=1)
        end_dt = datetime.combine(d_to, datetime.min.time(), tzinfo=timezone.utc)
        stmt = stmt.where(SleepSession.sleep_start < end_dt)

    stmt = stmt.order_by(SleepSession.sleep_start)
    sessions = db.execute(stmt).scalars().all()
    return [_serialize(s, include_stages) for s in sessions]
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-postgres-routers-cutover]] — symbols: `router`, `create_sleep_sessions`, `get_sleep_sessions`
- [[../../specs/2026-04-26-v2-auth-foundation]] — symbols: `router`, `list_sleep`, `get_sleep_session`

### Symbols
- `_parse_day` (function) — lines 18-19
- `_to_iso` (function) — lines 64-69
- `_serialize` (function) — lines 72-90

### Imports
- `datetime`
- `fastapi`
- `sqlalchemy`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.logging_config`
- `server.models`
- `server.security.auth`

### Exports
- `_parse_day`
- `_to_iso`
- `_serialize`
