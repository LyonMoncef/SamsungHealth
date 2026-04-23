---
type: code-source
language: python
file_path: server/routers/exercise.py
git_blob: bb9ac3a96d96b4aa0eca2ad925af30ddbb5e8057
last_synced: '2026-04-23T10:21:39Z'
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

# server/routers/exercise.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/exercise.py`](../../../server/routers/exercise.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
from fastapi import APIRouter, Query
from server.database import get_connection
from server.models import ExerciseSessionOut, ExerciseBulkIn

router = APIRouter(prefix="/api/exercise", tags=["exercise"])


@router.post("", status_code=201)
def create_exercise(body: ExerciseBulkIn) -> dict:
    conn = get_connection()
    inserted = 0
    skipped = 0
    for s in body.sessions:
        cursor = conn.execute(
            "INSERT OR IGNORE INTO exercise_sessions (exercise_type, exercise_start, exercise_end, duration_minutes) VALUES (?, ?, ?, ?)",
            (s.exercise_type, s.exercise_start, s.exercise_end, s.duration_minutes),
        )
        if cursor.rowcount == 0:
            skipped += 1
        else:
            inserted += 1
    conn.commit()
    conn.close()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_exercise(
    from_date: str = Query(None, alias="from"),
    to_date: str = Query(None, alias="to"),
) -> list[ExerciseSessionOut]:
    conn = get_connection()
    query = "SELECT exercise_type, exercise_start, exercise_end, duration_minutes FROM exercise_sessions"
    params: list[str] = []

    if from_date and to_date:
        query += " WHERE date(exercise_start) >= ? AND date(exercise_start) <= ?"
        params = [from_date, to_date]
    elif from_date:
        query += " WHERE date(exercise_start) >= ?"
        params = [from_date]
    elif to_date:
        query += " WHERE date(exercise_start) <= ?"
        params = [to_date]

    query += " ORDER BY exercise_start"
    rows = conn.execute(query, params).fetchall()
    conn.close()
    return [ExerciseSessionOut(**dict(r)) for r in rows]
```

---

## Appendix — symbols & navigation *(auto)*

### Imports
- `fastapi`
- `server.database`
- `server.models`
