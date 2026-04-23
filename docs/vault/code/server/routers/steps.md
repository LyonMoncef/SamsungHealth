---
type: code-source
language: python
file_path: server/routers/steps.py
git_blob: 47b3f072e8535c9f9735ed2a9d70ec2efeb33a80
last_synced: '2026-04-23T10:31:18Z'
loc: 49
annotations: []
imports:
- fastapi
- server.database
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
from fastapi import APIRouter, Query
from server.database import get_connection
from server.models import StepsHourlyOut, StepsBulkIn

router = APIRouter(prefix="/api/steps", tags=["steps"])


@router.post("", status_code=201)
def create_steps(body: StepsBulkIn) -> dict:
    conn = get_connection()
    inserted = 0
    skipped = 0
    for r in body.records:
        cursor = conn.execute(
            "INSERT OR IGNORE INTO steps_hourly (date, hour, step_count) VALUES (?, ?, ?)",
            (r.date, r.hour, r.step_count),
        )
        if cursor.rowcount == 0:
            skipped += 1
        else:
            inserted += 1
    conn.commit()
    conn.close()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_steps(
    from_date: str = Query(None, alias="from"),
    to_date: str = Query(None, alias="to"),
) -> list[StepsHourlyOut]:
    conn = get_connection()
    query = "SELECT date, hour, step_count FROM steps_hourly"
    params: list[str] = []

    if from_date and to_date:
        query += " WHERE date >= ? AND date <= ?"
        params = [from_date, to_date]
    elif from_date:
        query += " WHERE date >= ?"
        params = [from_date]
    elif to_date:
        query += " WHERE date <= ?"
        params = [to_date]

    query += " ORDER BY date, hour"
    rows = conn.execute(query, params).fetchall()
    conn.close()
    return [StepsHourlyOut(**dict(r)) for r in rows]
```

---

## Appendix — symbols & navigation *(auto)*

### Imports
- `fastapi`
- `server.database`
- `server.models`
