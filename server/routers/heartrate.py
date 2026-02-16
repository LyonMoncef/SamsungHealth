from fastapi import APIRouter, Query
from server.database import get_connection
from server.models import HeartRateHourlyOut, HeartRateBulkIn

router = APIRouter(prefix="/api/heartrate", tags=["heartrate"])


@router.post("", status_code=201)
def create_heartrate(body: HeartRateBulkIn) -> dict:
    conn = get_connection()
    inserted = 0
    skipped = 0
    for r in body.records:
        cursor = conn.execute(
            "INSERT OR IGNORE INTO heart_rate_hourly (date, hour, min_bpm, max_bpm, avg_bpm, sample_count) VALUES (?, ?, ?, ?, ?, ?)",
            (r.date, r.hour, r.min_bpm, r.max_bpm, r.avg_bpm, r.sample_count),
        )
        if cursor.rowcount == 0:
            skipped += 1
        else:
            inserted += 1
    conn.commit()
    conn.close()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_heartrate(
    from_date: str = Query(None, alias="from"),
    to_date: str = Query(None, alias="to"),
) -> list[HeartRateHourlyOut]:
    conn = get_connection()
    query = "SELECT date, hour, min_bpm, max_bpm, avg_bpm, sample_count FROM heart_rate_hourly"
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
    return [HeartRateHourlyOut(**dict(r)) for r in rows]
