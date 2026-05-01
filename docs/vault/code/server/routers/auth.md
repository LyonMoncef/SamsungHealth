---
type: code-source
language: python
file_path: server/routers/auth.py
git_blob: 010a85aa81e8fe417361f5b3d25d1d4d5985ea93
last_synced: '2026-05-01T12:19:09Z'
loc: 807
annotations: []
imports:
- hashlib
- os
- random
- time
- datetime
- fastapi
- pydantic
- sqlalchemy
- sqlalchemy.exc
- sqlalchemy.orm
- server.security.csrf
- server.security.lockout
- server.security.rate_limit
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
- _cookie_secure
- _set_refresh_cookie
- _delete_refresh_cookie
- _email_hash
- _record_event
- VerifyEmailRequestIn
- VerifyEmailConfirmIn
- PasswordResetRequestIn
- PasswordResetConfirmIn
- _anti_enum_jitter
- _hmac_email_for_cap
- _check_email_global_cap
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

from fastapi import APIRouter, Depends, Header, HTTPException, Request, Response, status
from pydantic import BaseModel, Field
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from server.security.csrf import check_sec_fetch_site
from server.security.lockout import (
    is_user_locked,
    register_failed_login,
    register_successful_login,
)
from server.security.rate_limit import (
    _email_composite_key,
    _email_request_composite_cap,
    _login_composite_key,
    _pure_ip_key,
    _refresh_composite_key,
    limiter,
)

from server.database import get_session
from server.db.models import AuthEvent, RefreshToken, User, VerificationToken
from server.logging_config import get_logger
from server.security.auth import (
    ACCESS_TOKEN_TTL_SECONDS,
    OAUTH_SENTINEL,
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
    # V2.3.3.2 — refresh_token optional in body (fallback for legacy clients);
    # primary source is the httpOnly cookie set at login.
    refresh_token: str | None = None


class LogoutIn(BaseModel):
    refresh_token: str


# ── helpers ────────────────────────────────────────────────────────────────
_REFRESH_COOKIE_NAME = "refresh_token"
_REFRESH_COOKIE_PATH = "/auth/refresh"


def _cookie_secure() -> bool:
    return os.environ.get("SAMSUNGHEALTH_ENV", "").lower() == "production"


def _set_refresh_cookie(response: Response, refresh_jwt: str) -> None:
    """V2.3.3.2 — Set refresh_token cookie httpOnly + SameSite=Strict + Path=/auth/refresh."""
    response.set_cookie(
        key=_REFRESH_COOKIE_NAME,
        value=refresh_jwt,
        httponly=True,
        secure=_cookie_secure(),
        samesite="strict",
        path=_REFRESH_COOKIE_PATH,
        max_age=REFRESH_TOKEN_TTL_SECONDS,
    )


def _delete_refresh_cookie(response: Response) -> None:
    response.delete_cookie(_REFRESH_COOKIE_NAME, path=_REFRESH_COOKIE_PATH)


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
@limiter.limit("3/minute", key_func=_pure_ip_key)
def register(
    request: Request,
    body: RegisterIn,
    response: Response,
    x_registration_token: str | None = Header(default=None, alias="X-Registration-Token"),
    db: Session = Depends(get_session),
) -> RegisterOut:
    check_sec_fetch_site(request)
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
@limiter.limit("30/minute", key_func=_pure_ip_key)
@limiter.limit("5/minute", key_func=_login_composite_key)
def login(
    request: Request,
    body: LoginIn,
    response: Response,
    db: Session = Depends(get_session),
) -> TokenPair:
    check_sec_fetch_site(request)
    email = str(body.email)
    user = db.execute(select(User).where(User.email == email)).scalar_one_or_none()

    if user is None:
        # Equalize timing + jitter (anti-enum, V2.3.3.1).
        verify_password(body.password, _DUMMY_HASH)
        time.sleep(random.uniform(0.080, 0.120))
        _record_event(
            db, event_type="login_failure", user_id=None, email_hash=_email_hash(email)
        )
        db.commit()
        _log.warning("auth.login.failure", reason="unknown_user", email_hash=_email_hash(email))
        raise HTTPException(status_code=401, detail="invalid_credentials")

    # Admin lock — 401 (anti-enum), no failed_login_count increment.
    if is_user_locked(user):
        verify_password(body.password, _DUMMY_HASH)
        time.sleep(random.uniform(0.080, 0.120))
        _record_event(
            db,
            event_type="login_locked_attempt",
            user_id=user.id,
            email_hash=_email_hash(email),
        )
        db.commit()
        raise HTTPException(status_code=401, detail="invalid_credentials")

    if not verify_password(body.password, user.password_hash):
        _record_event(
            db,
            event_type="login_failure",
            user_id=user.id,
            email_hash=_email_hash(email),
        )
        db.commit()
        # Atomic increment + soft backoff sleep (V2.3.3.1).
        register_failed_login(db, user)
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

    _record_event(
        db,
        event_type="login_success",
        user_id=user.id,
        email_hash=_email_hash(email),
    )
    db.commit()

    # Reset failed_login_count + last_login_at (race-guard preserves admin lock).
    register_successful_login(db, user)

    _log.info("auth.login.success", user_id=str(user.id), email_hash=_email_hash(email))
    _set_refresh_cookie(response, refresh_jwt)
    return TokenPair(access_token=access, refresh_token=refresh_jwt)


@router.post("/refresh", response_model=TokenPair)
@limiter.limit("60/minute", key_func=_pure_ip_key)
@limiter.limit("30/minute", key_func=_refresh_composite_key)
def refresh(
    request: Request,
    body: RefreshIn,
    response: Response,
    db: Session = Depends(get_session),
) -> TokenPair:
    check_sec_fetch_site(request)

    # V2.3.3.2 — explicit body > implicit cookie. Si le caller fournit explicitement
    # un refresh_token dans le body, c'est intentionnel (tests V2.3, clients legacy
    # Android pre-V2.3.3.2) et doit gagner sur le cookie httpOnly automatiquement
    # renvoyé par le browser. Sinon (frontend Nightfall classique), on lit le cookie.
    cookie_token = request.cookies.get(_REFRESH_COOKIE_NAME)
    refresh_token = body.refresh_token or cookie_token
    if not refresh_token:
        raise HTTPException(status_code=401, detail="invalid_refresh")

    try:
        payload = decode_refresh_token(refresh_token)
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

    # V2.3.3.1 — admin-locked user: revoke ONLY this token (preserve other devices).
    if is_user_locked(user):
        row.revoked_at = datetime.now(timezone.utc)
        _record_event(
            db,
            event_type="refresh_locked_attempt",
            user_id=user.id,
            email_hash=_email_hash(user.email),
        )
        db.commit()
        raise HTTPException(status_code=423, detail="account_locked")

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
    _set_refresh_cookie(response, new_refresh_jwt)
    return TokenPair(access_token=new_access, refresh_token=new_refresh_jwt)


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
@limiter.limit("30/minute", key_func=_pure_ip_key)
def logout(
    request: Request,
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
    out = Response(status_code=204)
    _delete_refresh_cookie(out)
    return out


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


def _hmac_email_for_cap(email: str) -> str:
    import hmac as _hmac

    salt = os.environ.get("SAMSUNGHEALTH_EMAIL_HASH_SALT", "")
    return _hmac.new(
        salt.encode("utf-8"),
        email.lower().encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()


def _check_email_global_cap(
    db: Session, email: str, event_type: str
) -> None:
    """V2.3.3.1 — anti mail-bombing distribué (rotation IPs bypass cap (IP,email)).

    Counts auth_events for `event_type` + `email_hash` in last N hours; raises 400
    if >= cap. Cap and window are env-overridable (test monkeypatch friendly).
    """
    try:
        cap = int(os.environ.get("SAMSUNGHEALTH_EMAIL_GLOBAL_CAP", "60"))
        window_hours = int(
            os.environ.get("SAMSUNGHEALTH_EMAIL_GLOBAL_CAP_WINDOW_HOURS", "24")
        )
    except ValueError:
        cap = 60
        window_hours = 24

    cutoff = datetime.now(timezone.utc) - timedelta(hours=window_hours)
    eh = _hmac_email_for_cap(email)
    count = db.execute(
        select(func.count())
        .select_from(AuthEvent)
        .where(
            AuthEvent.event_type == event_type,
            AuthEvent.email_hash == eh,
            AuthEvent.created_at > cutoff,
        )
    ).scalar_one()
    if count >= cap:
        raise HTTPException(status_code=400, detail="email_global_cap_exceeded")


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
@limiter.limit("10/5minutes", key_func=_pure_ip_key)
@limiter.limit(_email_request_composite_cap, key_func=_email_composite_key)
def request_email_verification(
    request: Request,
    body: VerifyEmailRequestIn,
    response: Response,
    authorization: str | None = Header(default=None),
    db: Session = Depends(get_session),
) -> dict:
    check_sec_fetch_site(request)
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

    # V2.3.3.1 — anti mail-bombing distribué (cap pur-email global).
    _check_email_global_cap(db, target_email, "email_verification_request")

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
@limiter.limit("10/minute", key_func=_pure_ip_key)
def confirm_email_verification(
    request: Request,
    body: VerifyEmailConfirmIn,
    response: Response,
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
@limiter.limit("10/5minutes", key_func=_pure_ip_key)
@limiter.limit(_email_request_composite_cap, key_func=_email_composite_key)
def request_password_reset(
    request: Request,
    body: PasswordResetRequestIn,
    response: Response,
    db: Session = Depends(get_session),
) -> dict:
    check_sec_fetch_site(request)
    target_email = str(body.email)

    # V2.3.3.1 — anti mail-bombing distribué (cap pur-email global).
    _check_email_global_cap(db, target_email, "password_reset_request")

    user = db.execute(
        select(User).where(User.email == target_email)
    ).scalar_one_or_none()

    if user is None or not user.is_active:
        _dummy_token_ops()
        _anti_enum_jitter()
        return {"status": "pending"}

    # V2.3.2 — OAuth-only user has no password to reset (sentinel hash).
    # Anti-enum: same 202 silent response, no token emitted, no mail sent.
    if user.password_hash == OAUTH_SENTINEL:
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
@limiter.limit("10/minute", key_func=_pure_ip_key)
def confirm_password_reset(
    request: Request,
    body: PasswordResetConfirmIn,
    response: Response,
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

    # V2.3.3.1 — password reset confirms ownership → unlock user (clear admin lock).
    was_locked = user.locked_until is not None and user.locked_until > now
    user.failed_login_count = 0
    user.locked_until = None

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
    if was_locked:
        db.add(
            AuthEvent(
                event_type="password_reset_unlocked_user",
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
- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — symbols: `request_password_reset`
- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — symbols: `login`, `refresh`, `request_email_verification`, `confirm_email_verification`, `request_password_reset`, `confirm_password_reset`
- [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall]] — symbols: `login`, `register`, `refresh`, `logout`, `request_email_verification`, `request_password_reset`

### Symbols
- `RegisterIn` (class) — lines 70-72
- `RegisterOut` (class) — lines 75-77
- `LoginIn` (class) — lines 80-82
- `TokenPair` (class) — lines 85-89
- `RefreshIn` (class) — lines 92-95
- `LogoutIn` (class) — lines 98-99
- `_cookie_secure` (function) — lines 107-108
- `_set_refresh_cookie` (function) — lines 111-121
- `_delete_refresh_cookie` (function) — lines 124-125
- `_email_hash` (function) — lines 128-129
- `_record_event` (function) — lines 132-150
- `VerifyEmailRequestIn` (class) — lines 456-457
- `VerifyEmailConfirmIn` (class) — lines 460-461
- `PasswordResetRequestIn` (class) — lines 464-465
- `PasswordResetConfirmIn` (class) — lines 468-470
- `_anti_enum_jitter` (function) — lines 473-475
- `_hmac_email_for_cap` (function) — lines 478-486
- `_check_email_global_cap` (function) — lines 489-518
- `_dummy_token_ops` (function) — lines 521-524
- `_revoke_active_tokens_for_purpose` (function) — lines 527-540
- `_issue_verification_token` (function) — lines 543-579

### Imports
- `hashlib`
- `os`
- `random`
- `time`
- `datetime`
- `fastapi`
- `pydantic`
- `sqlalchemy`
- `sqlalchemy.exc`
- `sqlalchemy.orm`
- `server.security.csrf`
- `server.security.lockout`
- `server.security.rate_limit`
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
- `_cookie_secure`
- `_set_refresh_cookie`
- `_delete_refresh_cookie`
- `_email_hash`
- `_record_event`
- `VerifyEmailRequestIn`
- `VerifyEmailConfirmIn`
- `PasswordResetRequestIn`
- `PasswordResetConfirmIn`
- `_anti_enum_jitter`
- `_hmac_email_for_cap`
- `_check_email_global_cap`
- `_dummy_token_ops`
- `_revoke_active_tokens_for_purpose`
- `_issue_verification_token`
