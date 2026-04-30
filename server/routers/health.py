"""Phase 6 CI/CD MVP — endpoints liveness (/healthz) et readiness (/readyz).

Spec: docs/vault/specs/2026-04-30-phase6-cicd-mvp.md §3.

- /healthz : liveness probe — process alive, pas de check DB. Toujours 200
  si l'app a boot.
- /readyz  : readiness probe — DB ping (`SELECT 1`) + check `alembic_version`
  matches `script.get_current_head()`. 503 sinon avec `reason` informatif
  mais sans info-leak interne (D-3 : reverse-proxy `/readyz` localhost-only
  recommandé).

Pas de Bearer auth ni de rate-limit (probes infrastructure).
"""
from __future__ import annotations

from alembic.script import ScriptDirectory
from fastapi import APIRouter, Depends, Response, status
from pydantic import BaseModel
from sqlalchemy import text
from sqlalchemy.orm import Session

from server.database import get_session


router = APIRouter(tags=["health"])


class HealthOut(BaseModel):
    status: str  # "ok"


class ReadyOut(BaseModel):
    status: str  # "ready" | "not_ready"
    reason: str | None = None  # ex: "db_unreachable", "alembic_mismatch"


@router.get("/healthz", response_model=HealthOut)
def healthz() -> HealthOut:
    """Liveness probe — process alive, no DB check (always 200 si app boot)."""
    return HealthOut(status="ok")


@router.get("/readyz", response_model=ReadyOut)
def readyz(
    response: Response, db: Session = Depends(get_session)
) -> ReadyOut:
    """Readiness probe — DB ping + alembic head match. 503 si pas ready."""
    # 1. DB ping.
    try:
        db.execute(text("SELECT 1"))
    except Exception:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        return ReadyOut(status="not_ready", reason="db_unreachable")
    # 2. Alembic head check.
    try:
        head_db = db.execute(
            text("SELECT version_num FROM alembic_version")
        ).scalar()
        from alembic.config import Config

        cfg = Config("alembic.ini")
        script = ScriptDirectory.from_config(cfg)
        head_expected = script.get_current_head()
        if head_db != head_expected:
            response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
            return ReadyOut(status="not_ready", reason="alembic_mismatch")
    except Exception:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE
        return ReadyOut(status="not_ready", reason="alembic_check_failed")
    return ReadyOut(status="ready")
