---
type: code-source
language: python
file_path: server/routers/mood.py
git_blob: 5875e4040ea3a241cf5f5128405c90d919766b27
last_synced: '2026-04-24T03:44:10Z'
loc: 86
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
- server.security.crypto
exports:
- _to_dt
- _iso
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

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import Mood
from server.models import MoodBulkIn, MoodOut
from server.security.crypto import DecryptionError

router = APIRouter(prefix="/api/mood", tags=["mood"])


def _to_dt(s: str) -> datetime:
    dt = datetime.fromisoformat(s)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt


def _iso(dt: datetime | None) -> str | None:
    return dt.isoformat() if dt is not None else None


@router.post("", status_code=201)
def create_mood_entries(body: MoodBulkIn, db: Session = Depends(get_session)) -> dict:
    inserted = 0
    skipped = 0
    for entry in body.entries:
        stmt = (
            pg_insert(Mood)
            .values(
                start_time=_to_dt(entry.start_time),
                mood_type=entry.mood_type,
                emotions=entry.emotions,
                factors=entry.factors,
                notes=entry.notes,
                place=entry.place,
                company=entry.company,
            )
            .on_conflict_do_nothing(index_elements=["start_time"])
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
) -> list[MoodOut]:
    stmt = select(Mood)
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

### Symbols
- `_to_dt` (function) — lines 17-21
- `_iso` (function) — lines 24-25

### Imports
- `datetime`
- `fastapi`
- `sqlalchemy`
- `sqlalchemy.dialects.postgresql`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.models`
- `server.security.crypto`

### Exports
- `_to_dt`
- `_iso`
