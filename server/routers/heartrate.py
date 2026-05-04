from fastapi import APIRouter, Depends, Query, Request, Response
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import HeartRateHourly, User
from server.logging_config import get_logger
from server.models import HeartRateBulkIn, HeartRateHourlyOut
from server.security.auth import get_current_user
from server.security.rate_limit import _api_post_cap, _user_id_key, limiter

_log = get_logger(__name__)

router = APIRouter(prefix="/api/heartrate", tags=["heartrate"])


@router.post("", status_code=201)
@limiter.limit(_api_post_cap, key_func=_user_id_key)
def create_heartrate(
    request: Request,
    body: HeartRateBulkIn,
    response: Response,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    inserted = 0
    skipped = 0
    for r in body.records:
        stmt = (
            pg_insert(HeartRateHourly)
            .values(
                user_id=current_user.id,
                date=r.date,
                hour=r.hour,
                min_bpm=r.min_bpm,
                max_bpm=r.max_bpm,
                avg_bpm=r.avg_bpm,
                sample_count=r.sample_count,
            )
            .on_conflict_do_nothing(index_elements=["user_id", "date", "hour"])
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
    current_user: User = Depends(get_current_user),
) -> list[HeartRateHourlyOut]:
    stmt = select(HeartRateHourly).where(HeartRateHourly.user_id == current_user.id)
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
