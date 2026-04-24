from datetime import date, datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from server.database import get_session
from server.db.models import SleepSession, SleepStage
from server.models import SleepBulkIn, SleepSessionOut, SleepStageOut

router = APIRouter(prefix="/api/sleep", tags=["sleep"])


def _parse_day(s: str) -> date:
    return datetime.fromisoformat(s).date()


@router.post("", status_code=201)
def create_sleep_sessions(body: SleepBulkIn, db: Session = Depends(get_session)) -> dict:
    inserted = 0
    skipped = 0
    for s in body.sessions:
        existing = db.execute(
            select(SleepSession).where(
                SleepSession.sleep_start == s.sleep_start,
                SleepSession.sleep_end == s.sleep_end,
            )
        ).scalar_one_or_none()
        if existing is not None:
            skipped += 1
            continue
        new_session = SleepSession(sleep_start=s.sleep_start, sleep_end=s.sleep_end)
        db.add(new_session)
        db.flush()
        if s.stages:
            for st in s.stages:
                db.add(
                    SleepStage(
                        session_id=new_session.id,
                        stage_type=st.stage_type,
                        stage_start=st.stage_start,
                        stage_end=st.stage_end,
                    )
                )
        inserted += 1
    db.commit()
    return {"inserted": inserted, "skipped": skipped}


def _to_iso(value) -> str:
    if value is None:
        return ""
    if isinstance(value, datetime):
        return value.isoformat()
    return str(value)


def _serialize(session: SleepSession, include_stages: bool) -> SleepSessionOut:
    out = SleepSessionOut(
        id=str(session.id),
        sleep_start=_to_iso(session.sleep_start),
        sleep_end=_to_iso(session.sleep_end),
        created_at=_to_iso(session.created_at) if session.created_at else None,
    )
    if include_stages:
        out.stages = [
            SleepStageOut(
                id=str(st.id),
                session_id=str(st.session_id),
                stage_type=st.stage_type,
                stage_start=_to_iso(st.stage_start),
                stage_end=_to_iso(st.stage_end),
            )
            for st in session.stages
        ]
    return out


@router.get("")
def get_sleep_sessions(
    from_date: str | None = Query(None, alias="from"),
    to_date: str | None = Query(None, alias="to"),
    include_stages: bool = Query(False),
    db: Session = Depends(get_session),
) -> list[SleepSessionOut]:
    stmt = select(SleepSession)
    if include_stages:
        stmt = stmt.options(selectinload(SleepSession.stages))

    if from_date:
        d_from = _parse_day(from_date)
        start_dt = datetime.combine(d_from, datetime.min.time(), tzinfo=timezone.utc)
        stmt = stmt.where(SleepSession.sleep_start >= start_dt)
    if to_date:
        d_to = _parse_day(to_date) + timedelta(days=1)
        end_dt = datetime.combine(d_to, datetime.min.time(), tzinfo=timezone.utc)
        stmt = stmt.where(SleepSession.sleep_start < end_dt)

    stmt = stmt.order_by(SleepSession.sleep_start)
    sessions = db.execute(stmt).scalars().all()
    return [_serialize(s, include_stages) for s in sessions]
