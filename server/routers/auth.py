"""V2.3 — Auth router: register / login / refresh / logout.

All endpoints under /auth/*. Audit trail rows written to auth_events.
"""
from __future__ import annotations

import hashlib
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
from server.db.models import AuthEvent, RefreshToken, User
from server.logging_config import get_logger
from server.security.auth import (
    ACCESS_TOKEN_TTL_SECONDS,
    REFRESH_TOKEN_TTL_SECONDS,
    check_registration_token,
    create_access_token,
    create_refresh_token,
    decode_access_token,
    decode_refresh_token,
    hash_password,
    verify_password,
    _DUMMY_HASH,
)


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
