---
type: code-source
language: python
file_path: server/routers/sleep.py
git_blob: c4c1cfb7d9924afd71904c7758ae5f4cdfdd11d4
last_synced: '2026-04-23T09:31:47Z'
loc: 70
annotations: []
imports:
- fastapi
- server.database
- server.models
exports: []
tags:
- code
- python
---

# server/routers/sleep.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/sleep.py`](../../../server/routers/sleep.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from fastapi import APIRouter, Query
from server.database import get_connection
from server.models import SleepSessionOut, SleepBulkIn

router = APIRouter(prefix="/api/sleep", tags=["sleep"])


@router.post("", status_code=201)
def create_sleep_sessions(body: SleepBulkIn) -> dict:
    conn = get_connection()
    conn.execute("PRAGMA foreign_keys = ON")
    inserted = 0
    skipped = 0
    for s in body.sessions:
        cursor = conn.execute(
            "INSERT OR IGNORE INTO sleep_sessions (sleep_start, sleep_end) VALUES (?, ?)",
            (s.sleep_start.isoformat(), s.sleep_end.isoformat()),
        )
        if cursor.rowcount == 0:
            skipped += 1
            continue
        session_id = cursor.lastrowid
        inserted += 1
        if s.stages:
            for st in s.stages:
                conn.execute(
                    "INSERT INTO sleep_stages (session_id, stage_type, stage_start, stage_end) VALUES (?, ?, ?, ?)",
                    (session_id, st.stage_type, st.stage_start.isoformat(), st.stage_end.isoformat()),
                )
    conn.commit()
    conn.close()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_sleep_sessions(
    from_date: str = Query(None, alias="from"),
    to_date: str = Query(None, alias="to"),
    include_stages: bool = Query(False),
) -> list[SleepSessionOut]:
    conn = get_connection()
    query = "SELECT id, sleep_start, sleep_end, created_at FROM sleep_sessions"
    params: list[str] = []

    if from_date and to_date:
        query += " WHERE sleep_start >= ? AND sleep_start < date(?, '+1 day')"
        params = [from_date, to_date]
    elif from_date:
        query += " WHERE sleep_start >= ?"
        params = [from_date]
    elif to_date:
        query += " WHERE sleep_start < date(?, '+1 day')"
        params = [to_date]

    query += " ORDER BY sleep_start"
    rows = conn.execute(query, params).fetchall()

    sessions = []
    for r in rows:
        data = dict(r)
        if include_stages:
            stage_rows = conn.execute(
                "SELECT id, session_id, stage_type, stage_start, stage_end FROM sleep_stages WHERE session_id = ?",
                (r["id"],),
            ).fetchall()
            data["stages"] = [dict(sr) for sr in stage_rows]
        sessions.append(SleepSessionOut(**data))

    conn.close()
    return sessions
```

---

## Appendix — symbols & navigation *(auto)*

### Imports
- `fastapi`
- `server.database`
- `server.models`
