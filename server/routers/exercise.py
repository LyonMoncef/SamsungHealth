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
