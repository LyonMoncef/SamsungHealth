"""V2.3.2 — Google OAuth router (POST /start, GET /callback, POST /oauth-link/confirm).

Account linking matrix (post-pentester revision — JAMAIS d'auto-link sur email match) :
1. provider_sub already linked → login existing user.
2. email match + email_verified=true + sub unknown → 202 oauth_link_pending +
   verification_token (purpose=oauth_link_confirm) emitted.
3. email match + email_verified=false → 409 oauth_provider_email_unverified.
4. email unknown + AUTO_REGISTER=true → atomic create user + identity_providers row
   (ON CONFLICT (email) for race condition fix).
5. email unknown + AUTO_REGISTER=false → 403 oauth_registration_disabled.
6. is_active=false → 403 account_disabled.
"""
from __future__ import annotations

import hashlib
import os
import uuid as _uuid
from datetime import datetime, timedelta, timezone
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Header, Request, Response, status
from pydantic import BaseModel, Field
from sqlalchemy import select, text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import (
    AuthEvent,
    IdentityProvider,
    RefreshToken,
    User,
    VerificationToken,
)
from server.logging_config import get_logger
from server.security.auth import (
    OAUTH_SENTINEL,
    REFRESH_TOKEN_TTL_SECONDS,
    TTL_OAUTH_LINK_CONFIRM,
    create_access_token,
    create_refresh_token,
    generate_verification_token,
    hash_verification_token,
    verify_verification_token,
)
from server.security.auth_providers import AuthProviderError
from server.security.auth_providers import google as google_mod
from server.security.auth_providers import state as state_mod
from server.security.auth_providers.google import (
    GoogleAuthProvider,
    _GOOGLE_ERROR_MAP,
    _filter_claims,
    _map_google_error,
)
from server.security.email_outbound import (
    _outbound_link_cache,
    send_verification_email,
)


_log = get_logger(__name__)

router = APIRouter(prefix="/auth", tags=["auth", "oauth"])


_PROVIDER_NAME = "google"
_AUTO_REGISTER_ENV = "SAMSUNGHEALTH_OAUTH_AUTO_REGISTER"
_PUBLIC_BASE_URL_ENV = "SAMSUNGHEALTH_PUBLIC_BASE_URL"
_GOOGLE_CALLBACK_PATH = "/auth/google/callback"


# ── pydantic ───────────────────────────────────────────────────────────────
class OauthStartIn(BaseModel):
    return_to: str | None = Field(default=None, max_length=2048)


class OauthStartOut(BaseModel):
    authorize_url: str


class OauthLinkConfirmIn(BaseModel):
    token: str = Field(min_length=1, max_length=512)


# ── helpers ────────────────────────────────────────────────────────────────
def _email_hash(email: str) -> str:
    return hashlib.sha256(email.lower().encode("utf-8")).hexdigest()


def _safe_ip(raw: str | None) -> str | None:
    """Return raw if it parses as a valid IPv4/v6 address, else None.

    Starlette's TestClient sets `client.host = "testclient"` which is not a
    valid INET — would poison the surrounding transaction on cast failure.
    """
    if not raw:
        return None
    import ipaddress

    try:
        ipaddress.ip_address(raw)
        return raw
    except ValueError:
        return None


def _audit(
    db: Session,
    *,
    event_type: str,
    user_id: _uuid.UUID | None,
    email_hash: str | None,
    request: Request | None,
) -> None:
    ip = None
    ua = None
    rid = None
    if request is not None:
        client = getattr(request, "client", None)
        if client is not None:
            ip = _safe_ip(client.host)
        ua = request.headers.get("user-agent")
        rid = request.headers.get("x-request-id")
        if not rid:
            from server.middleware.request_context import request_id_var

            rid = request_id_var.get(None)

    # Use a savepoint so that an audit insert failure (rare) does not poison
    # the outer transaction (we still want the auth path to succeed).
    try:
        with db.begin_nested():
            db.add(
                AuthEvent(
                    event_type=event_type,
                    user_id=user_id,
                    email_hash=email_hash,
                    ip=ip,
                    user_agent=ua,
                    request_id=rid,
                )
            )
    except Exception as exc:  # pragma: no cover
        _log.warning("oauth.audit.insert_failed", event_type=event_type, error=str(exc))


def _google_enabled() -> bool:
    return bool(os.environ.get("SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_ID")) and bool(
        os.environ.get("SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_SECRET")
    )


def _redirect_uri() -> str:
    base = os.environ.get(_PUBLIC_BASE_URL_ENV, "").rstrip("/")
    return f"{base}{_GOOGLE_CALLBACK_PATH}"


def _auto_register_enabled() -> bool:
    return os.environ.get(_AUTO_REGISTER_ENV, "false").lower() == "true"


def _issue_jwt_pair(db: Session, user: User) -> tuple[str, str]:
    """Create access + refresh JWTs and persist the refresh row."""
    access = create_access_token(user_id=str(user.id))
    new_jti = _uuid.uuid4()
    now = datetime.now(timezone.utc)
    db.add(
        RefreshToken(
            user_id=user.id,
            jti=new_jti,
            issued_at=now,
            expires_at=now + timedelta(seconds=REFRESH_TOKEN_TTL_SECONDS),
        )
    )
    refresh = create_refresh_token(user_id=str(user.id), jti=str(new_jti))
    return access, refresh


def _disabled_404() -> None:
    raise HTTPException(status_code=404, detail="oauth_provider_disabled")


# ── POST /auth/google/start ────────────────────────────────────────────────
@router.post("/google/start", response_model=OauthStartOut)
async def google_start(
    body: OauthStartIn,
    request: Request,
    db: Session = Depends(get_session),
) -> OauthStartOut:
    if not _google_enabled():
        _disabled_404()

    # CSRF: refuse cross-site/same-site Sec-Fetch-Site (browsers send same-origin/none for legitimate XHR).
    sfs = request.headers.get("sec-fetch-site")
    if sfs is not None and sfs.lower() not in {"same-origin", "none"}:
        raise HTTPException(status_code=403, detail="oauth_csrf_check_failed")

    state_jwt, _jti, nonce = state_mod.generate_oauth_state()
    provider = GoogleAuthProvider()
    authorize_url = provider.build_authorize_url(
        state=state_jwt, nonce=nonce, redirect_uri=_redirect_uri()
    )
    _audit(
        db,
        event_type="oauth_start",
        user_id=None,
        email_hash=None,
        request=request,
    )
    db.commit()
    _log.info("oauth.start", provider=_PROVIDER_NAME)
    return OauthStartOut(authorize_url=authorize_url)


# ── GET /auth/google/callback ──────────────────────────────────────────────
@router.get("/google/callback")
async def google_callback(
    request: Request,
    db: Session = Depends(get_session),
    code: str | None = None,
    state: str | None = None,
    error: str | None = None,
    error_description: str | None = None,
) -> dict:
    if not _google_enabled():
        _disabled_404()

    # 1. Provider returned an error (user declined, server error, etc.).
    if error:
        status_code, our_code = _map_google_error(error)
        # error_description goes to logs (redacted by structlog processor) — never to client.
        _log.warning(
            "oauth.callback.failure",
            provider=_PROVIDER_NAME,
            error=error,
            error_description=error_description,
        )
        _audit(
            db,
            event_type="oauth_callback_failure",
            user_id=None,
            email_hash=None,
            request=request,
        )
        db.commit()
        raise HTTPException(status_code=status_code, detail=our_code)

    if not code or not state:
        raise HTTPException(status_code=400, detail="oauth_callback_error")

    # 2. Verify state JWT (signature + single-use) — module-level call so tests can patch.
    try:
        state_payload = state_mod.verify_oauth_state(state)
    except state_mod.OAuthStateReplay:
        _audit(
            db,
            event_type="oauth_state_replay_attempt",
            user_id=None,
            email_hash=None,
            request=request,
        )
        db.commit()
        raise HTTPException(status_code=400, detail="oauth_state_replay")
    except state_mod.OAuthStateExpired:
        raise HTTPException(status_code=400, detail="oauth_state_expired")
    except state_mod.OAuthStateInvalid:
        raise HTTPException(status_code=400, detail="oauth_state_invalid")

    expected_nonce = (state_payload or {}).get("nonce")
    if not expected_nonce:
        raise HTTPException(status_code=400, detail="oauth_state_invalid")

    provider = GoogleAuthProvider()

    # 3. Exchange code → tokens.
    try:
        tokens = await provider.exchange_code_for_tokens(
            code=code, redirect_uri=_redirect_uri()
        )
    except AuthProviderError:
        _audit(
            db,
            event_type="oauth_callback_failure",
            user_id=None,
            email_hash=None,
            request=request,
        )
        db.commit()
        raise HTTPException(status_code=502, detail="oauth_provider_unavailable")

    id_token = tokens.get("id_token")
    if not id_token:
        raise HTTPException(status_code=502, detail="oauth_provider_unavailable")

    # 4. Validate ID token.
    try:
        profile = await provider.validate_id_token(
            id_token=id_token, expected_nonce=expected_nonce
        )
    except AuthProviderError as exc:
        _log.warning(
            "oauth.id_token.invalid",
            provider=_PROVIDER_NAME,
            reason=str(exc),
        )
        _audit(
            db,
            event_type="oauth_id_token_invalid",
            user_id=None,
            email_hash=None,
            request=request,
        )
        db.commit()
        raise HTTPException(status_code=400, detail="oauth_id_token_invalid")

    # 5. Account linking matrix.
    return await _resolve_account_linking(
        db=db, request=request, profile=profile
    )


async def _resolve_account_linking(
    *, db: Session, request: Request, profile
) -> dict:
    sub = profile.sub
    email = profile.email.lower()
    email_verified = profile.email_verified

    # Case 1: provider_sub already linked → login.
    idp = db.execute(
        select(IdentityProvider).where(
            IdentityProvider.provider == _PROVIDER_NAME,
            IdentityProvider.provider_sub == sub,
        )
    ).scalar_one_or_none()
    if idp is not None:
        user = db.execute(select(User).where(User.id == idp.user_id)).scalar_one_or_none()
        if user is None:
            raise HTTPException(status_code=400, detail="oauth_callback_error")
        if not user.is_active:
            raise HTTPException(status_code=403, detail="account_disabled")

        idp.last_used_at = datetime.now(timezone.utc)
        access, refresh = _issue_jwt_pair(db, user)
        _audit(
            db,
            event_type="oauth_callback_success",
            user_id=user.id,
            email_hash=_email_hash(user.email),
            request=request,
        )
        db.commit()
        _log.info("oauth.callback.success", provider=_PROVIDER_NAME, user_id=str(user.id))
        return {
            "access_token": access,
            "refresh_token": refresh,
            "user": {"id": str(user.id), "email": user.email},
        }

    # Case 2/3: email matches an existing user.
    user = db.execute(select(User).where(User.email == email)).scalar_one_or_none()
    if user is not None:
        if not user.is_active:
            raise HTTPException(status_code=403, detail="account_disabled")
        if not email_verified:
            _audit(
                db,
                event_type="oauth_callback_failure",
                user_id=user.id,
                email_hash=_email_hash(user.email),
                request=request,
            )
            db.commit()
            raise HTTPException(
                status_code=409, detail="oauth_provider_email_unverified"
            )

        # Linking deferred — emit oauth_link_confirm token.
        raw, hashed = generate_verification_token()
        now = datetime.now(timezone.utc)
        expires_at = now + TTL_OAUTH_LINK_CONFIRM
        payload = {
            "provider": _PROVIDER_NAME,
            "provider_sub": sub,
            "provider_email": email,
            "raw_claims_filtered": _filter_claims(profile.raw_claims),
        }
        db.add(
            VerificationToken(
                user_id=user.id,
                token_hash=hashed,
                purpose="oauth_link_confirm",
                issued_at=now,
                expires_at=expires_at,
                payload=payload,
            )
        )
        try:
            db.flush()
        except IntegrityError:
            db.rollback()
            raise HTTPException(status_code=503, detail="token_issue_race")

        send_verification_email(
            to_email=user.email,
            token_raw=raw,
            token_hash=hashed,
            expires_at=expires_at,
            purpose="oauth_link_confirm",
        )
        _audit(
            db,
            event_type="oauth_account_link_requested",
            user_id=user.id,
            email_hash=_email_hash(user.email),
            request=request,
        )
        db.commit()
        return Response(
            content='{"detail":"oauth_link_pending"}',
            status_code=202,
            media_type="application/json",
        )

    # Case 4/5: email unknown.
    if not _auto_register_enabled():
        _audit(
            db,
            event_type="oauth_callback_failure",
            user_id=None,
            email_hash=_email_hash(email),
            request=request,
        )
        db.commit()
        raise HTTPException(status_code=403, detail="oauth_registration_disabled")

    if not email_verified:
        _audit(
            db,
            event_type="oauth_callback_failure",
            user_id=None,
            email_hash=_email_hash(email),
            request=request,
        )
        db.commit()
        raise HTTPException(
            status_code=409, detail="oauth_provider_email_unverified"
        )

    # Auto-register: atomic INSERT user ... ON CONFLICT (email) DO NOTHING + fallback SELECT.
    user_id = _uuid.UUID(bytes=__import__("uuid_utils").uuid7().bytes)
    inserted = db.execute(
        text(
            "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
            "VALUES (:id, :email, :hash, true, now()) "
            "ON CONFLICT (email) DO NOTHING "
            "RETURNING id"
        ),
        {"id": user_id, "email": email, "hash": OAUTH_SENTINEL},
    ).scalar_one_or_none()
    if inserted is None:
        # Race: another concurrent callback created the user. Fetch it.
        user = db.execute(
            select(User).where(User.email == email)
        ).scalar_one_or_none()
        if user is None:
            db.rollback()
            raise HTTPException(status_code=503, detail="oauth_user_create_race")
    else:
        user = db.execute(select(User).where(User.id == inserted)).scalar_one()

    # INSERT identity_providers row (fallback: also ON CONFLICT to dedup if races).
    idp_row = IdentityProvider(
        user_id=user.id,
        provider=_PROVIDER_NAME,
        provider_sub=sub,
        provider_email=email,
        email_verified=email_verified,
        raw_claims=_filter_claims(profile.raw_claims),
        last_used_at=datetime.now(timezone.utc),
    )
    db.add(idp_row)
    try:
        db.flush()
    except IntegrityError:
        db.rollback()
        # Either (provider, sub) or (user_id, provider) already existed → load it.
        existing = db.execute(
            select(IdentityProvider).where(
                IdentityProvider.provider == _PROVIDER_NAME,
                IdentityProvider.provider_sub == sub,
            )
        ).scalar_one_or_none()
        if existing is None:
            raise HTTPException(status_code=400, detail="oauth_callback_error")
        existing.last_used_at = datetime.now(timezone.utc)

    access, refresh = _issue_jwt_pair(db, user)
    _audit(
        db,
        event_type="oauth_account_created",
        user_id=user.id,
        email_hash=_email_hash(user.email),
        request=request,
    )
    _audit(
        db,
        event_type="oauth_callback_success",
        user_id=user.id,
        email_hash=_email_hash(user.email),
        request=request,
    )
    db.commit()
    _log.info("oauth.account.created", provider=_PROVIDER_NAME, user_id=str(user.id))
    return {
        "access_token": access,
        "refresh_token": refresh,
        "user": {"id": str(user.id), "email": user.email},
    }


# ── POST /auth/oauth-link/confirm ──────────────────────────────────────────
@router.post("/oauth-link/confirm")
def oauth_link_confirm(
    body: OauthLinkConfirmIn,
    request: Request,
    db: Session = Depends(get_session),
) -> dict:
    row = verify_verification_token(db, body.token, "oauth_link_confirm")
    if row is None:
        raise HTTPException(status_code=400, detail="invalid_or_expired")

    payload = row.payload or {}
    provider = payload.get("provider")
    provider_sub = payload.get("provider_sub")
    provider_email = payload.get("provider_email")
    if not provider or not provider_sub:
        raise HTTPException(status_code=400, detail="invalid_or_expired")

    user = db.execute(select(User).where(User.id == row.user_id)).scalar_one_or_none()
    if user is None or not user.is_active:
        raise HTTPException(status_code=400, detail="invalid_or_expired")

    raw_claims = payload.get("raw_claims_filtered") or {}
    now = datetime.now(timezone.utc)
    idp = IdentityProvider(
        user_id=user.id,
        provider=provider,
        provider_sub=provider_sub,
        provider_email=provider_email,
        email_verified=True,
        raw_claims=raw_claims,
        last_used_at=now,
    )
    db.add(idp)
    try:
        db.flush()
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=400, detail="invalid_or_expired")

    row.consumed_at = now
    access, refresh = _issue_jwt_pair(db, user)
    _audit(
        db,
        event_type="oauth_account_linked",
        user_id=user.id,
        email_hash=_email_hash(user.email),
        request=request,
    )
    db.commit()
    _log.info("oauth.account.linked", provider=provider, user_id=str(user.id))
    return {
        "access_token": access,
        "refresh_token": refresh,
        "user": {"id": str(user.id), "email": user.email},
    }
