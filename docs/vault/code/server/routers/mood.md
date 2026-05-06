---
type: code-source
language: python
file_path: server/routers/mood.py
git_blob: 7c63fcb22736acc54caae10afc903e8a20311765
last_synced: '2026-05-06T08:02:34Z'
loc: 130
annotations: []
imports:
- datetime
- fastapi
- pydantic
- sqlalchemy
- sqlalchemy.dialects.postgresql
- sqlalchemy.orm
- server.database
- server.db.models
- server.logging_config
- server.models
- server.security.auth
- server.security.crypto
- server.security.rate_limit
exports:
- _to_dt
- _iso
- _normalize_payload
tags:
- code
- python
---

# server/routers/mood.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/mood.py`](../../../server/routers/mood.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.2 — router /api/mood avec champs Art.9 chiffrés transparent via TypeDecorator."""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Query, Request, Response
from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import Mood, User
from server.logging_config import get_logger
from server.models import MoodBulkIn, MoodIn, MoodOut
from server.security.auth import get_current_user
from server.security.crypto import DecryptionError
from server.security.rate_limit import _api_post_cap, _user_id_key, limiter

_log = get_logger(__name__)

router = APIRouter(prefix="/api/mood", tags=["mood"])


def _to_dt(s: str) -> datetime:
    dt = datetime.fromisoformat(s)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt


def _iso(dt: datetime | None) -> str | None:
    return dt.isoformat() if dt is not None else None


def _normalize_payload(raw: dict) -> list[MoodIn]:
    """Accept legacy {"entries": [MoodIn]} OR alt {"moods": [{recorded_at, mood_score, tags}]}.

    The "moods" form is a forward-compat schema used by V2.3 isolation tests.
    """
    if "entries" in raw:
        try:
            parsed = MoodBulkIn(entries=raw["entries"])
        except ValidationError as exc:
            raise HTTPException(status_code=422, detail=str(exc))
        return list(parsed.entries)
    if "moods" in raw:
        out: list[MoodIn] = []
        for m in raw["moods"]:
            start = m.get("recorded_at") or m.get("start_time")
            if not start:
                raise HTTPException(status_code=422, detail="missing recorded_at/start_time")
            out.append(
                MoodIn(
                    start_time=start,
                    mood_type=m.get("mood_score") if isinstance(m.get("mood_score"), int) else None,
                    notes=m.get("notes"),
                )
            )
        return out
    raise HTTPException(status_code=422, detail="missing entries or moods")


@router.post("", status_code=201)
@limiter.limit(_api_post_cap, key_func=_user_id_key)
async def create_mood_entries(
    request: Request,
    response: Response,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    raw = await request.json()
    entries = _normalize_payload(raw)
    inserted = 0
    skipped = 0
    for entry in entries:
        stmt = (
            pg_insert(Mood)
            .values(
                user_id=current_user.id,
                start_time=_to_dt(entry.start_time),
                mood_type=entry.mood_type,
                emotions=entry.emotions,
                factors=entry.factors,
                notes=entry.notes,
                place=entry.place,
                company=entry.company,
            )
            .on_conflict_do_nothing(index_elements=["user_id", "start_time"])
            .returning(Mood.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_mood_entries(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[MoodOut]:
    stmt = select(Mood).where(Mood.user_id == current_user.id)
    if from_date:
        d_from = _to_dt(from_date)
        stmt = stmt.where(Mood.start_time >= d_from)
    if to_date:
        d_to = _to_dt(to_date).replace(hour=23, minute=59, second=59)
        stmt = stmt.where(Mood.start_time <= d_to)
    stmt = stmt.order_by(Mood.start_time)
    try:
        rows = db.execute(stmt).scalars().all()
        out = [
            MoodOut(
                start_time=_iso(r.start_time),
                mood_type=r.mood_type,
                emotions=r.emotions,
                factors=r.factors,
                notes=r.notes,
                place=r.place,
                company=r.company,
            )
            for r in rows
        ]
    except DecryptionError as exc:
        # V2.2 §16 — sanitization erreur : 500 générique, pas de leak (clé tournée ? tampering ?)
        raise HTTPException(status_code=500, detail="internal_decryption_error") from exc
    return out
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — symbols: `router`, `create_mood_entry`, `get_mood_entries`
- [[../../specs/2026-04-26-v2-auth-foundation]] — symbols: `router`
- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — symbols: `router`

### Symbols
- `_to_dt` (function) — lines 23-27
- `_iso` (function) — lines 30-31
- `_normalize_payload` (function) — lines 34-59

### Imports
- `datetime`
- `fastapi`
- `pydantic`
- `sqlalchemy`
- `sqlalchemy.dialects.postgresql`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.logging_config`
- `server.models`
- `server.security.auth`
- `server.security.crypto`
- `server.security.rate_limit`

### Exports
- `_to_dt`
- `_iso`
- `_normalize_payload`
