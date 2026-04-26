"""V2.3.1 — Admin endpoint(s) gated by X-Registration-Token (stop-gap until V2.3.2 admin role).

GET /admin/pending-verifications : list active verification_tokens whose `token_raw`
is still in the in-memory cache (TTL 60s). Reconstructs `verify_link` from
`SAMSUNGHEALTH_PUBLIC_BASE_URL` (NEVER from request Host header).
"""
from __future__ import annotations

import os
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, Header, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import AuthEvent, User, VerificationToken
from server.logging_config import get_logger
from server.security.auth import PUBLIC_BASE_URL_ENV, check_registration_token
from server.security.email_outbound import _outbound_link_cache


_log = get_logger(__name__)

router = APIRouter(prefix="/admin", tags=["admin"])


def _purpose_path(purpose: str) -> str:
    if purpose == "password_reset":
        return "/auth/password/reset/confirm"
    return "/auth/verify-email/confirm"


@router.get("/pending-verifications", status_code=status.HTTP_200_OK)
def list_pending_verifications(
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
) -> list[dict]:
    # Spec uses 401 for missing/invalid admin token (vs 403 for register endpoint).
    if not x_registration_token:
        from fastapi import HTTPException

        raise HTTPException(status_code=401, detail="admin_token_required")
    expected = os.environ.get("SAMSUNGHEALTH_REGISTRATION_TOKEN")
    if not expected or x_registration_token != expected:
        # Reuse check_registration_token would raise 403 — admin endpoint expects 401.
        from fastapi import HTTPException
        import secrets as _secrets

        if not expected or not _secrets.compare_digest(x_registration_token, expected):
            raise HTTPException(status_code=401, detail="admin_token_required")

    base_url = os.environ.get(PUBLIC_BASE_URL_ENV, "").rstrip("/")

    # Audit row first (fail-fast: every admin call leaves a trace).
    db.add(
        AuthEvent(
            event_type="admin_pending_verifications_read",
            user_id=None,
            email_hash=None,
        )
    )
    db.commit()

    cache_pairs = dict(_outbound_link_cache.items())  # {hash: raw}
    if not cache_pairs:
        return []

    now = datetime.now(timezone.utc)
    rows = db.execute(
        select(VerificationToken, User.email)
        .join(User, User.id == VerificationToken.user_id)
        .where(
            VerificationToken.token_hash.in_(list(cache_pairs.keys())),
            VerificationToken.consumed_at.is_(None),
            VerificationToken.expires_at > now,
        )
    ).all()

    result: list[dict] = []
    for row, user_email in rows:
        raw = cache_pairs.get(row.token_hash)
        if raw is None:
            continue
        path = _purpose_path(row.purpose)
        verify_link = f"{base_url}{path}?token={raw}"
        result.append(
            {
                "verify_link": verify_link,
                "purpose": row.purpose,
                "user_email": user_email,
                "expires_at": row.expires_at.isoformat() if row.expires_at else None,
                "issued_at": row.issued_at.isoformat() if row.issued_at else None,
            }
        )
    return result
