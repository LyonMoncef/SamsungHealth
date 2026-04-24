from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import StepsHourly
from server.models import StepsBulkIn, StepsHourlyOut

router = APIRouter(prefix="/api/steps", tags=["steps"])


@router.post("", status_code=201)
def create_steps(body: StepsBulkIn, db: Session = Depends(get_session)) -> dict:
    inserted = 0
    skipped = 0
    for r in body.records:
        stmt = (
            pg_insert(StepsHourly)
            .values(date=r.date, hour=r.hour, step_count=r.step_count)
            .on_conflict_do_nothing(index_elements=["date", "hour"])
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
) -> list[StepsHourlyOut]:
    stmt = select(StepsHourly)
    if from_date:
        stmt = stmt.where(StepsHourly.date >= from_date)
    if to_date:
        stmt = stmt.where(StepsHourly.date <= to_date)
    stmt = stmt.order_by(StepsHourly.date, StepsHourly.hour)
    rows = db.execute(stmt).scalars().all()
    return [StepsHourlyOut(date=r.date, hour=r.hour, step_count=r.step_count) for r in rows]
