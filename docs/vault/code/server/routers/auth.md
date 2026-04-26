---
type: code-source
language: python
file_path: server/routers/auth.py
git_blob: ca7a2654c412ebbed08210a3cbb88cfaace994d3
last_synced: '2026-04-26T22:07:14Z'
loc: 616
annotations: []
imports:
- hashlib
- os
- random
- time
- datetime
- jwt
- fastapi
- pydantic
- sqlalchemy
- sqlalchemy.exc
- sqlalchemy.orm
- server.database
- server.db.models
- server.logging_config
- server.security.auth
- server.security.email_outbound
- server.security.passwords
exports:
- RegisterIn
- RegisterOut
- LoginIn
- TokenPair
- RefreshIn
- LogoutIn
- _email_hash
- _record_event
- VerifyEmailRequestIn
- VerifyEmailConfirmIn
- PasswordResetRequestIn
- PasswordResetConfirmIn
- _anti_enum_jitter
- _dummy_token_ops
- _revoke_active_tokens_for_purpose
- _issue_verification_token
tags:
- code
- python
---

# server/routers/auth.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/auth.py`](../../../server/routers/auth.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3 — Auth router: register / login / refresh / logout.

All endpoints under /auth/*. Audit trail rows written to auth_events.
"""
from __future__ import annotations

import hashlib
import os
import random
import time
import uuid as _uuid
from datetime import datetime, timedelta, timezone

import jwt
from fastapi import APIRouter, Depends, Header, HTTPException, Response, status
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import AuthEvent, RefreshToken, User, VerificationToken
from server.logging_config import get_logger
from server.security.auth import (
    ACCESS_TOKEN_TTL_SECONDS,
    REFRESH_TOKEN_TTL_SECONDS,
    REQUIRE_EMAIL_VERIFICATION_ENV,
    TTL_EMAIL_VERIFICATION,
    TTL_PASSWORD_RESET,
    check_registration_token,
    create_access_token,
    create_refresh_token,
    decode_access_token,
    decode_refresh_token,
    generate_verification_token,
    hash_password,
    hash_verification_token,
    verify_password,
    verify_verification_token,
    _DUMMY_HASH,
)
from server.security.email_outbound import (
    _outbound_link_cache,
    send_password_reset_email,
    send_verification_email,
)
from server.security.passwords import WeakPasswordError, validate_password_strength


_log = get_logger(__name__)

router = APIRouter(prefix="/auth", tags=["auth"])


# ── pydantic models ────────────────────────────────────────────────────────
class RegisterIn(BaseModel):
    email: str = Field(min_length=3, max_length=320)
    password: str = Field(min_length=12, max_length=1024)


class RegisterOut(BaseModel):
    id: str
    email: str


class LoginIn(BaseModel):
    email: str = Field(min_length=3, max_length=320)
    password: str = Field(min_length=1, max_length=1024)


class TokenPair(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int = ACCESS_TOKEN_TTL_SECONDS


class RefreshIn(BaseModel):
    refresh_token: str


class LogoutIn(BaseModel):
    refresh_token: str


# ── helpers ────────────────────────────────────────────────────────────────
def _email_hash(email: str) -> str:
    return hashlib.sha256(email.lower().encode("utf-8")).hexdigest()


def _record_event(
    db: Session,
    *,
    event_type: str,
    user_id: _uuid.UUID | None,
    email_hash: str | None,
) -> None:
    """Insert auth_events row. Catch errors → log warning, never crash auth."""
    try:
        db.add(
            AuthEvent(
                event_type=event_type,
                user_id=user_id,
                email_hash=email_hash,
            )
        )
        db.flush()
    except Exception as exc:  # pragma: no cover
        _log.warning("auth.event.insert_failed", event_type=event_type, error=str(exc))


# ── endpoints ──────────────────────────────────────────────────────────────
@router.post("/register", response_model=RegisterOut, status_code=status.HTTP_201_CREATED)
def register(
    body: RegisterIn,
    response: Response,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
) -> RegisterOut:
    check_registration_token(x_registration_token)

    email = str(body.email)
    try:
        validate_password_strength(body.password)
    except WeakPasswordError:
        raise HTTPException(status_code=400, detail="weak_password")
    pwd_hash = hash_password(body.password)

    # Check duplicate before insert (clean 409 vs IntegrityError).
    existing = db.execute(select(User).where(User.email == email)).scalar_one_or_none()
    if existing is not None:
        raise HTTPException(status_code=409, detail="email_already_exists")

    user = User(email=email, password_hash=pwd_hash)
    db.add(user)
    try:
        db.flush()
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=409, detail="email_already_exists")

    _record_event(
        db,
        event_type="register",
        user_id=user.id,
        email_hash=_email_hash(email),
    )
    db.commit()
    db.refresh(user)
    _log.info("auth.register.success", user_id=str(user.id), email_hash=_email_hash(email))
    return RegisterOut(id=str(user.id), email=user.email)


@router.post("/login", response_model=TokenPair)
def login(body: LoginIn, db: Session = Depends(get_session)) -> TokenPair:
    email = str(body.email)
    user = db.execute(select(User).where(User.email == email)).scalar_one_or_none()

    if user is None:
        # Run dummy hash to equalize timing.
        verify_password(body.password, _DUMMY_HASH)
        _record_event(
            db, event_type="login_failure", user_id=None, email_hash=_email_hash(email)
        )
        db.commit()
        _log.warning("auth.login.failure", reason="unknown_user", email_hash=_email_hash(email))
        raise HTTPException(status_code=401, detail="invalid_credentials")

    if not verify_password(body.password, user.password_hash):
        _record_event(
            db,
            event_type="login_failure",
            user_id=user.id,
            email_hash=_email_hash(email),
        )
        db.commit()
        _log.warning(
            "auth.login.failure",
            reason="wrong_password",
            email_hash=_email_hash(email),
        )
        raise HTTPException(status_code=401, detail="invalid_credentials")

    if not user.is_active:
        _record_event(
            db,
            event_type="login_failure",
            user_id=user.id,
            email_hash=_email_hash(email),
        )
        db.commit()
        raise HTTPException(status_code=401, detail="invalid_credentials")

    # V2.3.1 — email verification gate (dynamically read flag).
    require_verify = (
        os.environ.get(REQUIRE_EMAIL_VERIFICATION_ENV, "false").lower() == "true"
    )
    if user.email_verified_at is None:
        if require_verify:
            db.commit()
            raise HTTPException(status_code=403, detail="email_not_verified")
        # Lazy logger fetch — capture_logs() in tests requires a logger built AFTER
        # the test enters the context (module-level loggers freeze the prod config).
        get_logger(__name__).warning(
            "auth.login.unverified_email",
            user_id=str(user.id),
            email_hash=_email_hash(email),
        )

    # Issue tokens.
    access = create_access_token(user_id=str(user.id))
    new_jti = _uuid.uuid4()
    now = datetime.now(timezone.utc)
    refresh_row = RefreshToken(
        user_id=user.id,
        jti=new_jti,
        issued_at=now,
        expires_at=now + timedelta(seconds=REFRESH_TOKEN_TTL_SECONDS),
    )
    db.add(refresh_row)
    refresh_jwt = create_refresh_token(user_id=str(user.id), jti=str(new_jti))

    user.last_login_at = now

    _record_event(
        db,
        event_type="login_success",
        user_id=user.id,
        email_hash=_email_hash(email),
    )
    db.commit()

    _log.info("auth.login.success", user_id=str(user.id), email_hash=_email_hash(email))
    return TokenPair(access_token=access, refresh_token=refresh_jwt)


@router.post("/refresh", response_model=TokenPair)
def refresh(body: RefreshIn, db: Session = Depends(get_session)) -> TokenPair:
    try:
        payload = decode_refresh_token(body.refresh_token)
    except Exception:
        raise HTTPException(status_code=401, detail="invalid_refresh")

    jti_raw = payload.get("jti")
    sub_raw = payload.get("sub")
    if not jti_raw or not sub_raw:
        raise HTTPException(status_code=401, detail="invalid_refresh")

    try:
        jti = _uuid.UUID(str(jti_raw))
        user_uuid = _uuid.UUID(str(sub_raw))
    except (ValueError, TypeError):
        raise HTTPException(status_code=401, detail="invalid_refresh")

    row = db.execute(
        select(RefreshToken).where(RefreshToken.jti == jti)
    ).scalar_one_or_none()

    if row is None or row.revoked_at is not None:
        # Replay of revoked token — log error.
        _log.error(
            "auth.refresh.replay_attempt",
            email_hash=None,
            revoked_jti=str(jti),
        )
        raise HTTPException(status_code=401, detail="invalid_refresh")

    user = db.execute(select(User).where(User.id == user_uuid)).scalar_one_or_none()
    if user is None or not user.is_active:
        raise HTTPException(status_code=401, detail="invalid_refresh")

    # Rotate.
    now = datetime.now(timezone.utc)
    new_jti = _uuid.uuid4()
    new_row = RefreshToken(
        user_id=user.id,
        jti=new_jti,
        issued_at=now,
        expires_at=now + timedelta(seconds=REFRESH_TOKEN_TTL_SECONDS),
    )
    db.add(new_row)
    db.flush()
    row.revoked_at = now
    row.replaced_by = new_row.id
    row.last_used_at = now

    new_access = create_access_token(user_id=str(user.id))
    new_refresh_jwt = create_refresh_token(user_id=str(user.id), jti=str(new_jti))

    _record_event(
        db,
        event_type="refresh",
        user_id=user.id,
        email_hash=hashlib.sha256(user.email.lower().encode("utf-8")).hexdigest(),
    )
    db.commit()

    _log.info(
        "auth.refresh.success",
        user_id=str(user.id),
        old_jti=str(jti),
        new_jti=str(new_jti),
    )
    return TokenPair(access_token=new_access, refresh_token=new_refresh_jwt)


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
def logout(
    body: LogoutIn,
    authorization: str | None = Header(default=None),
    db: Session = Depends(get_session),
) -> Response:
    # Validate access token (idempotent: still requires valid Bearer, but missing/expired = 401).
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="invalid_credentials")
    access_token = authorization[7:]
    try:
        access_payload = decode_access_token(access_token)
    except Exception:
        raise HTTPException(status_code=401, detail="invalid_credentials")

    # Best-effort: revoke the refresh row by jti.
    try:
        refresh_payload = decode_refresh_token(body.refresh_token)
        jti = _uuid.UUID(str(refresh_payload.get("jti")))
        row = db.execute(
            select(RefreshToken).where(RefreshToken.jti == jti)
        ).scalar_one_or_none()
        if row is not None and row.revoked_at is None:
            row.revoked_at = datetime.now(timezone.utc)
    except Exception:
        # idempotent — refresh malformed/expired/already-revoked → still 204.
        pass

    user_uuid = None
    try:
        user_uuid = _uuid.UUID(str(access_payload.get("sub")))
    except Exception:
        user_uuid = None

    _record_event(
        db, event_type="logout", user_id=user_uuid, email_hash=None
    )
    db.commit()

    _log.info("auth.logout.success", user_id=str(user_uuid) if user_uuid else None)
    return Response(status_code=204)


# ── V2.3.1 — verification token request/confirm ────────────────────────────
class VerifyEmailRequestIn(BaseModel):
    email: str | None = Field(default=None, min_length=3, max_length=320)


class VerifyEmailConfirmIn(BaseModel):
    token: str = Field(min_length=1, max_length=512)


class PasswordResetRequestIn(BaseModel):
    email: str = Field(min_length=3, max_length=320)


class PasswordResetConfirmIn(BaseModel):
    token: str = Field(min_length=1, max_length=512)
    new_password: str = Field(min_length=1, max_length=1024)


def _anti_enum_jitter() -> None:
    """Sleep 80-120ms to flatten the timing channel between known/unknown branches."""
    time.sleep(random.uniform(0.080, 0.120))


def _dummy_token_ops() -> None:
    """Constant-cost fake token gen + sha256 to mirror the real branch's CPU work."""
    raw, _ = generate_verification_token()
    hash_verification_token(raw)


def _revoke_active_tokens_for_purpose(
    db: Session, user_id: _uuid.UUID, purpose: str
) -> None:
    """Set consumed_at = now() on any active token of `(user_id, purpose)`. Same txn."""
    now = datetime.now(timezone.utc)
    rows = db.execute(
        select(VerificationToken).where(
            VerificationToken.user_id == user_id,
            VerificationToken.purpose == purpose,
            VerificationToken.consumed_at.is_(None),
        )
    ).scalars().all()
    for r in rows:
        r.consumed_at = now


def _issue_verification_token(
    db: Session,
    *,
    user: User,
    purpose: str,
    ttl: timedelta,
) -> tuple[str, str, datetime]:
    """Revoke previous active token for (user, purpose) + insert a fresh one.

    Catches IntegrityError (race with concurrent insert on the unique partial index)
    by retrying once with a fresh revoke. Returns (raw, hashed, expires_at).
    """
    now = datetime.now(timezone.utc)
    expires_at = now + ttl

    for _attempt in range(2):
        _revoke_active_tokens_for_purpose(db, user.id, purpose)
        db.flush()

        raw, hashed = generate_verification_token()
        row = VerificationToken(
            user_id=user.id,
            token_hash=hashed,
            purpose=purpose,
            issued_at=now,
            expires_at=expires_at,
        )
        db.add(row)
        try:
            db.flush()
            return raw, hashed, expires_at
        except IntegrityError:
            db.rollback()
            # Race with concurrent issuer — retry once: the other writer's row is
            # now the active one, and our retry will revoke it then insert anew.
            continue
    raise HTTPException(status_code=503, detail="token_issue_race")


@router.post("/verify-email/request", status_code=status.HTTP_202_ACCEPTED)
def request_email_verification(
    body: VerifyEmailRequestIn,
    authorization: str | None = Header(default=None),
    db: Session = Depends(get_session),
) -> dict:
    # Auth optional: prefer Bearer current_user, else use body.email.
    target_email: str | None = None
    if authorization and authorization.startswith("Bearer "):
        try:
            payload = decode_access_token(authorization[7:])
            sub = payload.get("sub")
            if sub:
                user_row = db.execute(
                    select(User).where(User.id == _uuid.UUID(str(sub)))
                ).scalar_one_or_none()
                if user_row is not None:
                    target_email = user_row.email
        except Exception:
            target_email = None
    if target_email is None and body.email:
        target_email = str(body.email)

    if not target_email:
        _anti_enum_jitter()
        return {"status": "pending"}

    user = db.execute(
        select(User).where(User.email == target_email)
    ).scalar_one_or_none()

    # Anti-enum: if unknown OR already verified → dummy ops + 202.
    if user is None or user.email_verified_at is not None or not user.is_active:
        _dummy_token_ops()
        _anti_enum_jitter()
        return {"status": "pending"}

    raw, hashed, expires_at = _issue_verification_token(
        db, user=user, purpose="email_verification", ttl=TTL_EMAIL_VERIFICATION
    )
    send_verification_email(
        to_email=target_email,
        token_raw=raw,
        token_hash=hashed,
        expires_at=expires_at,
        purpose="email_verification",
    )
    _record_event(
        db,
        event_type="email_verification_request",
        user_id=user.id,
        email_hash=_email_hash(target_email),
    )
    db.commit()
    _anti_enum_jitter()
    return {"status": "pending"}


@router.post("/verify-email/confirm")
def confirm_email_verification(
    body: VerifyEmailConfirmIn,
    db: Session = Depends(get_session),
) -> dict:
    row = verify_verification_token(db, body.token, "email_verification")
    if row is None:
        raise HTTPException(status_code=400, detail="invalid_or_expired")

    user = db.execute(
        select(User).where(User.id == row.user_id)
    ).scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=400, detail="invalid_or_expired")

    now = datetime.now(timezone.utc)
    if user.email_verified_at is None:
        user.email_verified_at = now
    row.consumed_at = now
    _record_event(
        db,
        event_type="email_verification_confirm",
        user_id=user.id,
        email_hash=_email_hash(user.email),
    )
    db.commit()
    return {"status": "verified"}


@router.post("/password/reset/request", status_code=status.HTTP_202_ACCEPTED)
def request_password_reset(
    body: PasswordResetRequestIn,
    db: Session = Depends(get_session),
) -> dict:
    target_email = str(body.email)
    user = db.execute(
        select(User).where(User.email == target_email)
    ).scalar_one_or_none()

    if user is None or not user.is_active:
        _dummy_token_ops()
        _anti_enum_jitter()
        return {"status": "pending"}

    raw, hashed, expires_at = _issue_verification_token(
        db, user=user, purpose="password_reset", ttl=TTL_PASSWORD_RESET
    )
    send_password_reset_email(
        to_email=target_email,
        token_raw=raw,
        token_hash=hashed,
        expires_at=expires_at,
        purpose="password_reset",
    )
    _record_event(
        db,
        event_type="password_reset_request",
        user_id=user.id,
        email_hash=_email_hash(target_email),
    )
    db.commit()
    _anti_enum_jitter()
    return {"status": "pending"}


@router.post("/password/reset/confirm")
def confirm_password_reset(
    body: PasswordResetConfirmIn,
    db: Session = Depends(get_session),
) -> dict:
    row = verify_verification_token(db, body.token, "password_reset")
    if row is None:
        raise HTTPException(status_code=400, detail="invalid_or_expired")

    # Validate password BEFORE any state change → token NOT consumed on weak.
    try:
        validate_password_strength(body.new_password)
    except WeakPasswordError:
        raise HTTPException(status_code=400, detail="weak_password")

    user = db.execute(
        select(User).where(User.id == row.user_id)
    ).scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=400, detail="invalid_or_expired")

    # Single transaction. Audit insert can RAISE → entire txn rolls back.
    now = datetime.now(timezone.utc)
    new_hash = hash_password(body.new_password)

    user.password_hash = new_hash
    user.password_changed_at = now

    # Revoke ALL active refresh tokens for this user.
    refresh_rows = db.execute(
        select(RefreshToken).where(
            RefreshToken.user_id == user.id, RefreshToken.revoked_at.is_(None)
        )
    ).scalars().all()
    for r in refresh_rows:
        r.revoked_at = now

    row.consumed_at = now

    # Audit insert is fail-on-error here (spec MED fix). Differs from V2.3 login
    # where audit is best-effort.
    db.add(
        AuthEvent(
            event_type="password_reset_confirm",
            user_id=user.id,
            email_hash=_email_hash(user.email),
        )
    )
    try:
        db.commit()
    except Exception:
        db.rollback()
        raise HTTPException(status_code=500, detail="audit_failed")

    _log.info(
        "auth.password_reset.success",
        user_id=str(user.id),
        email_hash=_email_hash(user.email),
    )
    return {"status": "reset"}
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2-auth-foundation]] — symbols: `router`, `register`, `login`, `refresh`, `logout`
- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — symbols: `request_email_verification`, `confirm_email_verification`, `request_password_reset`, `confirm_password_reset`

### Symbols
- `RegisterIn` (class) — lines 56-58
- `RegisterOut` (class) — lines 61-63
- `LoginIn` (class) — lines 66-68
- `TokenPair` (class) — lines 71-75
- `RefreshIn` (class) — lines 78-79
- `LogoutIn` (class) — lines 82-83
- `_email_hash` (function) — lines 87-88
- `_record_event` (function) — lines 91-109
- `VerifyEmailRequestIn` (class) — lines 351-352
- `VerifyEmailConfirmIn` (class) — lines 355-356
- `PasswordResetRequestIn` (class) — lines 359-360
- `PasswordResetConfirmIn` (class) — lines 363-365
- `_anti_enum_jitter` (function) — lines 368-370
- `_dummy_token_ops` (function) — lines 373-376
- `_revoke_active_tokens_for_purpose` (function) — lines 379-392
- `_issue_verification_token` (function) — lines 395-431

### Imports
- `hashlib`
- `os`
- `random`
- `time`
- `datetime`
- `jwt`
- `fastapi`
- `pydantic`
- `sqlalchemy`
- `sqlalchemy.exc`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.logging_config`
- `server.security.auth`
- `server.security.email_outbound`
- `server.security.passwords`

### Exports
- `RegisterIn`
- `RegisterOut`
- `LoginIn`
- `TokenPair`
- `RefreshIn`
- `LogoutIn`
- `_email_hash`
- `_record_event`
- `VerifyEmailRequestIn`
- `VerifyEmailConfirmIn`
- `PasswordResetRequestIn`
- `PasswordResetConfirmIn`
- `_anti_enum_jitter`
- `_dummy_token_ops`
- `_revoke_active_tokens_for_purpose`
- `_issue_verification_token`
