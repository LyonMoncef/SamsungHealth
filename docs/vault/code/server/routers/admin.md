---
type: code-source
language: python
file_path: server/routers/admin.py
git_blob: a5cda9b099bad1b3fc07809ea81479ea81e95ee4
last_synced: '2026-04-28T14:04:54Z'
loc: 560
annotations: []
imports:
- asyncio
- os
- re
- datetime
- pathlib
- typing
- uuid
- fastapi
- fastapi.responses
- pydantic
- sqlalchemy
- sqlalchemy.orm
- server.database
- server.db.models
- server.logging_config
- server.security.auth
- server.security.csrf
- server.security.email_outbound
- server.security.rate_limit
exports:
- _validate_admin_return_to
- _wants_html
- _purpose_path
- _audit
- _admin_probe_global_cap
- _admin_probe_global_key
- _check_admin_token_async
- _check_admin_token
- require_admin_token
- UserSummary
- IdentityProviderOut
- AuthEventOut
- _sanitize_event
- UserDetail
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

V2.3.3.3 — Auth finitions :
- `_check_admin_token` async + `asyncio.sleep(0.5)` constant + audit log à chaque 401
- `require_admin_token` Depends factorisé + audit `admin_token_used`
- GET /admin/users (list, JSON) + GET /admin/users/{id} (detail, JSON)
- GET /admin/users + /admin/users/{id} content-negotiate Accept text/html → page
- GET /admin/pending-verifications content-negotiate Accept text/html → page
- POST /admin/probe (sentinel auth check) avec second bucket global 100/h
- CSRF check `Sec-Fetch-Site` ajouté à TOUS les POST admin (lock/unlock/probe)
- `last_login_ip_hash` HMAC dans UserSummary (jamais brut)
- recent_events sanitize `request_id` field abuse (V2.3.3.1 lock format)
- Open redirect prevention helper `_validate_admin_return_to`
"""
from __future__ import annotations

import asyncio
import os
import re
import secrets as _secrets
import uuid as _uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any
from uuid import UUID

from fastapi import APIRouter, Depends, Header, HTTPException, Request, Response, status
from fastapi.responses import FileResponse, JSONResponse
from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import AuthEvent, IdentityProvider, User, VerificationToken
from server.logging_config import get_logger
from server.security.auth import PUBLIC_BASE_URL_ENV
from server.security.csrf import check_sec_fetch_site
from server.security.email_outbound import _outbound_link_cache
from server.security.rate_limit import _ip_hash, _pure_ip_key, _resolve_client_ip, limiter


_log = get_logger(__name__)

router = APIRouter(prefix="/admin", tags=["admin"])

_STATIC_ADMIN_DIR = Path(__file__).resolve().parent.parent.parent / "static" / "admin"
_NO_STORE = {"Cache-Control": "no-store"}

_RETURN_TO_RE = re.compile(r"^/admin/[a-z0-9/-]+$")


def _validate_admin_return_to(return_to: str | None) -> str | None:
    """Strict allowlist for ?return_to= on /admin/login (pentester M1)."""
    if not return_to:
        return None
    if _RETURN_TO_RE.match(return_to):
        return return_to
    return None


def _wants_html(request: Request) -> bool:
    """V2.3.3.3 — Content negotiation for admin GET endpoints.

    HTML page served only when `Accept: text/html` is explicit (browser nav).
    Default (Accept: */* or application/json or absent) → JSON API path.
    Presence of `X-Registration-Token` always forces JSON (admin SPA fetch).
    """
    if request.headers.get("x-registration-token") is not None:
        return False
    accept = (request.headers.get("accept") or "").lower()
    if "text/html" in accept and "application/json" not in accept:
        return True
    return False


def _purpose_path(purpose: str) -> str:
    if purpose == "password_reset":
        return "/auth/password/reset/confirm"
    return "/auth/verify-email/confirm"


def _audit(
    db: Session,
    *,
    event_type: str,
    user_id: UUID | None = None,
    request_id: str | None = None,
    user_agent: str | None = None,
) -> None:
    db.add(
        AuthEvent(
            event_type=event_type,
            user_id=user_id,
            email_hash=None,
            request_id=request_id,
            user_agent=user_agent,
        )
    )


def _admin_probe_global_cap() -> str:
    """Second bucket global cap for /admin/probe (anti rotation IP via Tor).

    Defaults to 100/hour per spec. Overridable via env so tests can lower the
    cap without flooding 100 requests.
    """
    return os.environ.get("SAMSUNGHEALTH_RL_ADMIN_PROBE_GLOBAL_CAP", "100/hour")


def _admin_probe_global_key(request: Any) -> str:
    return "admin_probe_global"


async def _check_admin_token_async(
    request: Request,
    provided: str | None,
    *,
    fail_event_type: str,
    db: Session | None = None,
) -> None:
    """Constant-time check + 0.5s sleep + audit on 401 (no sample).

    `db` provided → audit row inserted + commit. If db is None, audit is
    skipped (caller must handle).
    """
    expected = os.environ.get("SAMSUNGHEALTH_REGISTRATION_TOKEN")
    valid = bool(provided and expected and _secrets.compare_digest(provided, expected))
    if not valid:
        await asyncio.sleep(0.5)
        if db is not None:
            try:
                ip = _resolve_client_ip(request)
                ip_h = _ip_hash(ip) if ip else None
                _audit(
                    db,
                    event_type=fail_event_type,
                    request_id=f"endpoint:{request.url.path}|ip_hash:{ip_h}",
                )
                db.commit()
            except Exception as exc:  # pragma: no cover
                _log.warning("admin.audit_fail.error", error=str(exc))
        raise HTTPException(status_code=401, detail="admin_token_required")


def _check_admin_token(provided: str | None) -> None:
    """Sync legacy check used by older endpoints. Constant-time only.

    Admin endpoints expect 401 (not 403 like /auth/register).
    """
    expected = os.environ.get("SAMSUNGHEALTH_REGISTRATION_TOKEN")
    if not provided or not expected or not _secrets.compare_digest(provided, expected):
        raise HTTPException(status_code=401, detail="admin_token_required")


async def require_admin_token(
    request: Request,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
) -> None:
    """Depends factorisé : vérifie le token + audit `admin_token_used` à chaque succès.

    Sur échec : 0.5s sleep constant + audit `admin_token_used_failed` (no sample).
    """
    await _check_admin_token_async(
        request,
        x_registration_token,
        fail_event_type="admin_token_used_failed",
        db=db,
    )
    # success path: log usage (forensique post-leak)
    try:
        ip = _resolve_client_ip(request)
        ip_h = _ip_hash(ip) if ip else None
        _audit(
            db,
            event_type="admin_token_used",
            request_id=f"endpoint:{request.url.path}|ip_hash:{ip_h}",
        )
        db.commit()
    except Exception as exc:  # pragma: no cover
        _log.warning("admin.audit_used.error", error=str(exc))


# ── Pydantic models ───────────────────────────────────────────────────────
class UserSummary(BaseModel):
    """Shape sûre pour exposition admin. password_hash JAMAIS exposé.
    last_login_ip JAMAIS brut — uniquement HMAC truncated 16 hex.
    """

    model_config = ConfigDict(from_attributes=True)

    id: UUID
    email: str
    is_active: bool
    email_verified_at: datetime | None = None
    failed_login_count: int = 0
    locked_until: datetime | None = None
    last_login_at: datetime | None = None
    last_login_ip_hash: str | None = None
    created_at: datetime

    @classmethod
    def from_user(cls, u: User) -> "UserSummary":
        ip_h: str | None = None
        raw_ip = getattr(u, "last_login_ip", None)
        if raw_ip:
            ip_h = _ip_hash(str(raw_ip))
        return cls(
            id=u.id,
            email=u.email,
            is_active=u.is_active,
            email_verified_at=u.email_verified_at,
            failed_login_count=u.failed_login_count,
            locked_until=u.locked_until,
            last_login_at=u.last_login_at,
            last_login_ip_hash=ip_h,
            created_at=u.created_at,
        )


class IdentityProviderOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    provider: str
    provider_email: str | None = None
    email_verified: bool = False
    linked_at: datetime
    last_used_at: datetime | None = None


class AuthEventOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: UUID
    event_type: str
    request_id: str | None = None
    user_agent: str | None = None
    created_at: datetime


_LOCK_REQUEST_ID_RE = re.compile(r"^reason:.*\|duration_minutes:\d+$")


def _sanitize_event(ev: AuthEvent) -> AuthEventOut:
    """Strip `request_id` blob format `reason:...|duration_minutes:...` for
    admin_user_locked / admin_user_unlocked events (V2.3.3.1 field abuse).

    The reason / duration may contain PII; we drop the field in the response.
    """
    req_id = ev.request_id
    if ev.event_type in ("admin_user_locked", "admin_user_unlocked"):
        if req_id and _LOCK_REQUEST_ID_RE.match(req_id):
            req_id = None
    return AuthEventOut(
        id=ev.id,
        event_type=ev.event_type,
        request_id=req_id,
        user_agent=ev.user_agent,
        created_at=ev.created_at,
    )


class UserDetail(BaseModel):
    user: UserSummary
    providers: list[IdentityProviderOut]
    recent_events: list[AuthEventOut]


# ── GET /admin/pending-verifications (HTML page or JSON API) ──────────────
@router.get("/pending-verifications", status_code=status.HTTP_200_OK)
@limiter.limit("60/minute", key_func=_pure_ip_key)
def list_pending_verifications(
    request: Request,
    response: Response,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
):
    # V2.3.3.3 — content negotiation: serve HTML page when browser asks.
    if _wants_html(request):
        return FileResponse(
            _STATIC_ADMIN_DIR / "pending-verifications.html",
            media_type="text/html",
            headers=_NO_STORE,
        )

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


# ── V2.3.3.3 GET /admin/users (HTML page or JSON list) ────────────────────
@router.get("/users", status_code=status.HTTP_200_OK)
@limiter.limit("60/minute", key_func=_pure_ip_key)
async def list_users(
    request: Request,
    response: Response,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
):
    """List users (JSON) — admin gated. Content-negotiates: text/html → page."""
    if _wants_html(request):
        return FileResponse(
            _STATIC_ADMIN_DIR / "users.html",
            media_type="text/html",
            headers=_NO_STORE,
        )

    await _check_admin_token_async(
        request,
        x_registration_token,
        fail_event_type="admin_users_list_failed",
        db=db,
    )

    rows = db.execute(
        select(User).order_by(User.created_at.desc()).limit(100)
    ).scalars().all()
    users = [UserSummary.from_user(u).model_dump(mode="json") for u in rows]

    ip = _resolve_client_ip(request)
    ip_h = _ip_hash(ip) if ip else None
    _audit(
        db,
        event_type="admin_users_list",
        request_id=f"endpoint:{request.url.path}|ip_hash:{ip_h}|users_count:{len(users)}",
    )
    db.commit()
    return users


# ── V2.3.3.3 GET /admin/users/{user_id} (HTML page or JSON detail) ────────
@router.get("/users/{user_id}", status_code=status.HTTP_200_OK)
@limiter.limit("60/minute", key_func=_pure_ip_key)
async def get_user_detail(
    user_id: str,
    request: Request,
    response: Response,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
):
    """User detail (JSON) — admin gated. Content-negotiates: text/html → page."""
    if _wants_html(request):
        return FileResponse(
            _STATIC_ADMIN_DIR / "user-detail.html",
            media_type="text/html",
            headers=_NO_STORE,
        )

    await _check_admin_token_async(
        request,
        x_registration_token,
        fail_event_type="admin_user_detail_read_failed",
        db=db,
    )

    try:
        uid = _uuid.UUID(user_id)
    except (ValueError, TypeError):
        raise HTTPException(status_code=400, detail="invalid_user_id")

    user = db.execute(select(User).where(User.id == uid)).scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=404, detail="user_not_found")

    providers = (
        db.execute(select(IdentityProvider).where(IdentityProvider.user_id == uid))
        .scalars()
        .all()
    )
    recent = (
        db.execute(
            select(AuthEvent)
            .where(AuthEvent.user_id == uid)
            .order_by(AuthEvent.created_at.desc())
            .limit(10)
        )
        .scalars()
        .all()
    )

    detail = UserDetail(
        user=UserSummary.from_user(user),
        providers=[IdentityProviderOut.model_validate(p) for p in providers],
        recent_events=[_sanitize_event(e) for e in recent],
    )

    ip = _resolve_client_ip(request)
    ip_h = _ip_hash(ip) if ip else None
    _audit(
        db,
        event_type="admin_user_detail_read",
        user_id=uid,
        request_id=f"endpoint:{request.url.path}|ip_hash:{ip_h}|target_user_id:{uid}",
    )
    db.commit()
    return detail.model_dump(mode="json")


# ── V2.3.3.3 POST /admin/probe (sentinel for client-side token validation) ─
@router.post("/probe", status_code=status.HTTP_200_OK)
@limiter.limit(_admin_probe_global_cap, key_func=_admin_probe_global_key)
@limiter.limit("10/minute", key_func=_pure_ip_key)
async def admin_probe(
    request: Request,
    response: Response,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
) -> dict:
    """Sentinel endpoint pour validation client-side du admin token.

    - CSRF check Sec-Fetch-Site (HIGH H1 fix)
    - 0.5s sleep constant sur 401 (anti-timing) + audit `admin_probe_failed`
    - Second bucket global anti rotation IP
    """
    check_sec_fetch_site(request)
    await _check_admin_token_async(
        request,
        x_registration_token,
        fail_event_type="admin_probe_failed",
        db=db,
    )
    return {"status": "ok"}


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
    # V2.3.3.3 — CSRF check (HIGH H1 fix : Sec-Fetch-Site).
    check_sec_fetch_site(request)
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
    # V2.3.3.3 — CSRF check (HIGH H1 fix).
    check_sec_fetch_site(request)
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
- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]] — symbols: `list_users`, `get_user_detail`

### Symbols
- `_validate_admin_return_to` (function) — lines 58-64
- `_wants_html` (function) — lines 67-79
- `_purpose_path` (function) — lines 82-85
- `_audit` (function) — lines 88-104
- `_admin_probe_global_cap` (function) — lines 107-113
- `_admin_probe_global_key` (function) — lines 116-117
- `_check_admin_token_async` (function) — lines 120-148
- `_check_admin_token` (function) — lines 151-158
- `require_admin_token` (function) — lines 161-187
- `UserSummary` (class) — lines 191-224
- `IdentityProviderOut` (class) — lines 227-235
- `AuthEventOut` (class) — lines 238-245
- `_sanitize_event` (function) — lines 251-267
- `UserDetail` (class) — lines 270-273
- `AdminLockBody` (class) — lines 476-478

### Imports
- `asyncio`
- `os`
- `re`
- `datetime`
- `pathlib`
- `typing`
- `uuid`
- `fastapi`
- `fastapi.responses`
- `pydantic`
- `sqlalchemy`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.logging_config`
- `server.security.auth`
- `server.security.csrf`
- `server.security.email_outbound`
- `server.security.rate_limit`

### Exports
- `_validate_admin_return_to`
- `_wants_html`
- `_purpose_path`
- `_audit`
- `_admin_probe_global_cap`
- `_admin_probe_global_key`
- `_check_admin_token_async`
- `_check_admin_token`
- `require_admin_token`
- `UserSummary`
- `IdentityProviderOut`
- `AuthEventOut`
- `_sanitize_event`
- `UserDetail`
- `AdminLockBody`
