from fastapi import APIRouter, Query
from datetime import datetime
from server.database import get_connection
from server.models import SleepSessionIn, SleepSessionOut, SleepBulkIn

router = APIRouter(prefix="/api/sleep", tags=["sleep"])


@router.post("", status_code=201)
def create_sleep_sessions(body: SleepBulkIn) -> dict:
    conn = get_connection()
    inserted = 0
    for s in body.sessions:
        conn.execute(
            "INSERT INTO sleep_sessions (sleep_start, sleep_end) VALUES (?, ?)",
            (s.sleep_start.isoformat(), s.sleep_end.isoformat()),
        )
        inserted += 1
    conn.commit()
    conn.close()
    return {"inserted": inserted}


@router.get("")
def get_sleep_sessions(
    from_date: str = Query(None, alias="from"),
    to_date: str = Query(None, alias="to"),
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
    conn.close()
    return [SleepSessionOut(**dict(r)) for r in rows]
