from datetime import datetime, timezone

from fastapi import APIRouter, Depends, Query, Request, Response
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import ExerciseSession, User
from server.logging_config import get_logger
from server.models import ExerciseBulkIn, ExerciseSessionOut
from server.security.auth import get_current_user
from server.security.rate_limit import _api_post_cap, _user_id_key, limiter

_log = get_logger(__name__)

router = APIRouter(prefix="/api/exercise", tags=["exercise"])


def _to_dt(s: str) -> datetime:
    dt = datetime.fromisoformat(s)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt


def _iso(dt: datetime | None) -> str:
    if dt is None:
        return ""
    return dt.isoformat()


@router.post("", status_code=201)
@limiter.limit(_api_post_cap, key_func=_user_id_key)
def create_exercise(
    request: Request,
    body: ExerciseBulkIn,
    response: Response,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    inserted = 0
    skipped = 0
    for s in body.sessions:
        stmt = (
            pg_insert(ExerciseSession)
            .values(
                user_id=current_user.id,
                exercise_type=s.exercise_type,
                exercise_start=_to_dt(s.exercise_start),
                exercise_end=_to_dt(s.exercise_end),
                duration_minutes=s.duration_minutes,
            )
            .on_conflict_do_nothing(
                index_elements=["user_id", "exercise_start", "exercise_end"]
            )
            .returning(ExerciseSession.id)
        )
        if db.execute(stmt).first() is not None:
            inserted += 1
        else:
            skipped += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


@router.get("")
def get_exercise(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> list[ExerciseSessionOut]:
    stmt = select(ExerciseSession).where(ExerciseSession.user_id == current_user.id)
    if from_date:
        d_from = datetime.fromisoformat(from_date).replace(tzinfo=timezone.utc)
        stmt = stmt.where(ExerciseSession.exercise_start >= d_from)
    if to_date:
        d_to = datetime.fromisoformat(to_date).replace(tzinfo=timezone.utc)
        d_to_eod = d_to.replace(hour=23, minute=59, second=59)
        stmt = stmt.where(ExerciseSession.exercise_start <= d_to_eod)
    stmt = stmt.order_by(ExerciseSession.exercise_start)
    rows = db.execute(stmt).scalars().all()
    return [
        ExerciseSessionOut(
            exercise_type=r.exercise_type,
            exercise_start=_iso(r.exercise_start),
            exercise_end=_iso(r.exercise_end),
            duration_minutes=r.duration_minutes,
        )
        for r in rows
    ]
