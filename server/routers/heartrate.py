from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import HeartRateHourly
from server.logging_config import get_logger
from server.models import HeartRateBulkIn, HeartRateHourlyOut

_log = get_logger(__name__)

router = APIRouter(prefix="/api/heartrate", tags=["heartrate"])


@router.post("", status_code=201)
def create_heartrate(body: HeartRateBulkIn, db: Session = Depends(get_session)) -> dict:
    inserted = 0
    skipped = 0
    for r in body.records:
        stmt = (
            pg_insert(HeartRateHourly)
            .values(
                date=r.date,
                hour=r.hour,
                min_bpm=r.min_bpm,
                max_bpm=r.max_bpm,
                avg_bpm=r.avg_bpm,
                sample_count=r.sample_count,
            )
            .on_conflict_do_nothing(index_elements=["date", "hour"])
            .returning(HeartRateHourly.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_heartrate(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    db: Session = Depends(get_session),
) -> list[HeartRateHourlyOut]:
    stmt = select(HeartRateHourly)
    if from_date:
        stmt = stmt.where(HeartRateHourly.date >= from_date)
    if to_date:
        stmt = stmt.where(HeartRateHourly.date <= to_date)
    stmt = stmt.order_by(HeartRateHourly.date, HeartRateHourly.hour)
    rows = db.execute(stmt).scalars().all()
    return [
        HeartRateHourlyOut(
            date=r.date,
            hour=r.hour,
            min_bpm=r.min_bpm,
            max_bpm=r.max_bpm,
            avg_bpm=r.avg_bpm,
            sample_count=r.sample_count,
        )
        for r in rows
    ]
