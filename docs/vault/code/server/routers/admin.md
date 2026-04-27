---
type: code-source
language: python
file_path: server/routers/admin.py
git_blob: 1a1090c4d0a9ac8777c1b3a397b0fca9bb18f124
last_synced: '2026-04-27T17:56:06Z'
loc: 187
annotations: []
imports:
- os
- datetime
- fastapi
- pydantic
- sqlalchemy
- sqlalchemy.orm
- server.database
- server.db.models
- server.logging_config
- server.security.auth
- server.security.email_outbound
- server.security.rate_limit
exports:
- _purpose_path
- _check_admin_token
- AdminLockBody
tags:
- code
- python
---

# server/routers/admin.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/admin.py`](../../../server/routers/admin.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 — Admin endpoints gated by X-Registration-Token (stop-gap until V2.3.2 admin role).

GET /admin/pending-verifications : list active verification_tokens whose `token_raw`
is still in the in-memory cache (TTL 60s). Reconstructs `verify_link` from
`SAMSUNGHEALTH_PUBLIC_BASE_URL` (NEVER from request Host header).

V2.3.3.1 — POST /admin/users/{user_id}/lock|unlock for manual hard-lockout.
"""
from __future__ import annotations

import os
import secrets as _secrets
import uuid as _uuid
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Header, HTTPException, Request, Response, status
from pydantic import BaseModel, Field
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import AuthEvent, User, VerificationToken
from server.logging_config import get_logger
from server.security.auth import PUBLIC_BASE_URL_ENV
from server.security.email_outbound import _outbound_link_cache
from server.security.rate_limit import _pure_ip_key, limiter


_log = get_logger(__name__)

router = APIRouter(prefix="/admin", tags=["admin"])


def _purpose_path(purpose: str) -> str:
    if purpose == "password_reset":
        return "/auth/password/reset/confirm"
    return "/auth/verify-email/confirm"


def _check_admin_token(provided: str | None) -> None:
    """Constant-time check of X-Registration-Token. Raise 401 if missing/invalid.

    Admin endpoints expect 401 (not 403 like /auth/register).
    """
    expected = os.environ.get("SAMSUNGHEALTH_REGISTRATION_TOKEN")
    if not provided or not expected or not _secrets.compare_digest(provided, expected):
        raise HTTPException(status_code=401, detail="admin_token_required")


@router.get("/pending-verifications", status_code=status.HTTP_200_OK)
@limiter.limit("60/minute", key_func=_pure_ip_key)
def list_pending_verifications(
    request: Request,
    response: Response,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
) -> list[dict]:
    _check_admin_token(x_registration_token)

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


# ── V2.3.3.1 admin lock / unlock ──────────────────────────────────────────
class AdminLockBody(BaseModel):
    duration_minutes: int = Field(ge=1, le=60 * 24 * 30)
    reason: str = Field(min_length=1, max_length=512)


@router.post("/users/{user_id}/lock", status_code=status.HTTP_200_OK)
@limiter.limit("60/minute", key_func=_pure_ip_key)
def lock_user(
    request: Request,
    response: Response,
    user_id: str,
    body: AdminLockBody,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
) -> dict:
    _check_admin_token(x_registration_token)

    try:
        uid = _uuid.UUID(user_id)
    except (ValueError, TypeError):
        raise HTTPException(status_code=400, detail="invalid_user_id")

    user = db.execute(select(User).where(User.id == uid)).scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=404, detail="user_not_found")

    locked_until = datetime.now(timezone.utc) + timedelta(minutes=body.duration_minutes)
    db.execute(
        update(User).where(User.id == uid).values(locked_until=locked_until)
    )
    db.add(
        AuthEvent(
            event_type="admin_user_locked",
            user_id=uid,
            email_hash=None,
            request_id=f"reason:{body.reason}|duration_minutes:{body.duration_minutes}",
        )
    )
    db.commit()
    _log.info(
        "admin.user.locked",
        user_id=str(uid),
        duration_minutes=body.duration_minutes,
        reason=body.reason,
    )
    return {"status": "locked", "locked_until": locked_until.isoformat()}


@router.post("/users/{user_id}/unlock", status_code=status.HTTP_200_OK)
@limiter.limit("60/minute", key_func=_pure_ip_key)
def unlock_user(
    request: Request,
    response: Response,
    user_id: str,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
) -> dict:
    _check_admin_token(x_registration_token)

    try:
        uid = _uuid.UUID(user_id)
    except (ValueError, TypeError):
        raise HTTPException(status_code=400, detail="invalid_user_id")

    user = db.execute(select(User).where(User.id == uid)).scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=404, detail="user_not_found")

    db.execute(
        update(User).where(User.id == uid).values(failed_login_count=0, locked_until=None)
    )
    db.add(
        AuthEvent(
            event_type="admin_user_unlocked",
            user_id=uid,
            email_hash=None,
        )
    )
    db.commit()
    _log.info("admin.user.unlocked", user_id=str(uid))
    return {"status": "unlocked"}
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — symbols: `router`, `list_pending_verifications`
- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — symbols: `router`, `list_pending_verifications`, `lock_user`, `unlock_user`

### Symbols
- `_purpose_path` (function) — lines 34-37
- `_check_admin_token` (function) — lines 40-47
- `AdminLockBody` (class) — lines 107-109

### Imports
- `os`
- `datetime`
- `fastapi`
- `pydantic`
- `sqlalchemy`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.logging_config`
- `server.security.auth`
- `server.security.email_outbound`
- `server.security.rate_limit`

### Exports
- `_purpose_path`
- `_check_admin_token`
- `AdminLockBody`
