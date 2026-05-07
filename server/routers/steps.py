from fastapi import APIRouter, Depends, File, HTTPException, Query, Request, Response, UploadFile
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import StepsHourly, User
from server.logging_config import get_logger
from server.models import StepsBulkIn, StepsHourlyOut
from server.security.auth import get_current_user
from server.security.rate_limit import _api_post_cap, _user_id_key, limiter
from server.services.csv_import import MAX_CSV_BYTES, parse_samsung_csv, parse_steps_rows

_log = get_logger(__name__)

router = APIRouter(prefix="/api/steps", tags=["steps"])


@router.post("", status_code=201)
@limiter.limit(_api_post_cap, key_func=_user_id_key)
def create_steps(
    request: Request,
    body: StepsBulkIn,
    response: Response,
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


@router.post("/import", status_code=200)
@limiter.limit(_api_post_cap, key_func=_user_id_key)
def import_steps(
    request: Request,
    response: Response,
    file: UploadFile = File(...),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    raw = file.file.read(MAX_CSV_BYTES + 1)
    if len(raw) > MAX_CSV_BYTES:
        raise HTTPException(status_code=413, detail="file_too_large")
    try:
        rows = parse_samsung_csv(raw)
    except (UnicodeDecodeError, ValueError):
        raise HTTPException(status_code=422, detail="invalid_csv_encoding")
    inserted, skipped = parse_steps_rows(rows, current_user.id, db)
    _log.info(
        "csv_import.done",
        endpoint="steps",
        inserted=inserted,
        skipped=skipped,
        user_id=str(current_user.id),
        filename=file.filename[:255] if file.filename else None,
    )
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
