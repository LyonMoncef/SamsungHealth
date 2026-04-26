from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import StepsHourly, User
from server.logging_config import get_logger
from server.models import StepsBulkIn, StepsHourlyOut
from server.security.auth import get_current_user

_log = get_logger(__name__)

router = APIRouter(prefix="/api/steps", tags=["steps"])


@router.post("", status_code=201)
def create_steps(
    body: StepsBulkIn,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    inserted = 0
    skipped = 0
    for r in body.records:
        stmt = (
            pg_insert(StepsHourly)
            .values(
                user_id=current_user.id,
                date=r.date,
                hour=r.hour,
                step_count=r.step_count,
            )
            .on_conflict_do_nothing(index_elements=["user_id", "date", "hour"])
            .returning(StepsHourly.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_steps(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[StepsHourlyOut]:
    stmt = select(StepsHourly).where(StepsHourly.user_id == current_user.id)
    if from_date:
        stmt = stmt.where(StepsHourly.date >= from_date)
    if to_date:
        stmt = stmt.where(StepsHourly.date <= to_date)
    stmt = stmt.order_by(StepsHourly.date, StepsHourly.hour)
    rows = db.execute(stmt).scalars().all()
    return [StepsHourlyOut(date=r.date, hour=r.hour, step_count=r.step_count) for r in rows]
