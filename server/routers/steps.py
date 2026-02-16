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
