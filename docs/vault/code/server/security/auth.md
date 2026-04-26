---
type: code-source
language: python
file_path: server/security/auth.py
git_blob: 9333edb1a3cdc760b360d2c54b24a7328a5c8d2e
last_synced: '2026-04-26T16:48:28Z'
loc: 351
annotations: []
imports:
- math
- os
- secrets
- time
- collections
- jwt
- argon2
- argon2.exceptions
- fastapi
- sqlalchemy
- sqlalchemy.orm
- server.database
- server.logging_config
exports:
- JwtConfigError
- hash_password
- verify_password
- _shannon_entropy
- _validate_jwt_secret_at_boot
- _validate_registration_token
- _current_secret
- _previous_secret
- _encode
- _decode_with_secret
- _decode_try_both
- create_access_token
- create_refresh_token
- decode_access_token
- decode_refresh_token
- revoke_refresh_token
- rotate_refresh_token
- get_current_user
- check_registration_token
tags:
- code
- python
---

# server/security/auth.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/security/auth.py`](../../../server/security/auth.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3 — Auth foundation: argon2id password hashing + JWT (HS256) + get_current_user dep.

Modules-level responsibilities:
- `hash_password` / `verify_password` — argon2id (RFC 9106 profile #2)
- `_DUMMY_HASH` — pre-computed at module load for timing equalization
- `create_access_token` / `create_refresh_token` / `decode_*` — PyJWT HS256 with strict validation
- `_validate_jwt_secret_at_boot` / `_validate_registration_token` — boot-time fail-fast
- `rotate_refresh_token` / `revoke_refresh_token` — DB-backed refresh chain
- `get_current_user` — FastAPI dependency
"""
from __future__ import annotations

import math
import os
import secrets
import time
import uuid as _uuid
from collections import Counter

import jwt
from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError, InvalidHashError, VerificationError
from fastapi import Depends, Header, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from server.database import get_session
from server.logging_config import get_logger


_log = get_logger(__name__)


# ── env vars ───────────────────────────────────────────────────────────────
JWT_SECRET_ENV = "SAMSUNGHEALTH_JWT_SECRET"
JWT_SECRET_PREVIOUS_ENV = "SAMSUNGHEALTH_JWT_SECRET_PREVIOUS"
REGISTRATION_TOKEN_ENV = "SAMSUNGHEALTH_REGISTRATION_TOKEN"

# ── argon2 params (RFC 9106 profile #2) ────────────────────────────────────
_ARGON2_TIME_COST = 2
_ARGON2_MEMORY_COST = 46_080  # ≈45 MiB
_ARGON2_PARALLELISM = 1

_hasher = PasswordHasher(
    time_cost=_ARGON2_TIME_COST,
    memory_cost=_ARGON2_MEMORY_COST,
    parallelism=_ARGON2_PARALLELISM,
)


# ── JWT constants ──────────────────────────────────────────────────────────
JWT_ALGORITHM = "HS256"
JWT_ISSUER = "samsunghealth"
JWT_AUDIENCE = "samsunghealth-api"
ACCESS_TOKEN_TTL_SECONDS = 30 * 60  # 30 minutes
REFRESH_TOKEN_TTL_SECONDS = 30 * 24 * 60 * 60  # 30 days
JWT_KID_CURRENT = "v1"

_FORBIDDEN_SECRETS = {"changeme", "secret", "test", "password", "default"}
_MIN_SECRET_BITS = 256


class JwtConfigError(RuntimeError):
    """Raised when JWT secret env var is absent / weak / forbidden."""


# ── password hashing ───────────────────────────────────────────────────────
def hash_password(plain: str) -> str:
    """Hash `plain` with argon2id (RFC 9106 profile #2). Returns encoded string."""
    return _hasher.hash(plain)


def verify_password(plain: str, encoded: str) -> bool:
    """Verify `plain` against `encoded` argon2id hash. Returns False on mismatch (no raise)."""
    try:
        return _hasher.verify(encoded, plain)
    except (VerifyMismatchError, InvalidHashError, VerificationError, Exception):
        return False


# Pre-computed dummy hash used by login when user is unknown — keeps wall-clock
# uniform between unknown-user and wrong-password paths.
_DUMMY_HASH: str = hash_password("____unused_dummy_password_for_timing____")


# ── JWT secret validation ──────────────────────────────────────────────────
def _shannon_entropy(s: str) -> float:
    if not s:
        return 0.0
    counts = Counter(s)
    total = len(s)
    return -sum((c / total) * math.log2(c / total) for c in counts.values())


def _validate_jwt_secret_at_boot() -> str:
    """Validate `SAMSUNGHEALTH_JWT_SECRET`. Raise JwtConfigError if absent/weak/forbidden.

    Returns the validated raw secret string.
    """
    raw = os.environ.get(JWT_SECRET_ENV)
    if not raw:
        raise JwtConfigError(
            f"Env var {JWT_SECRET_ENV} absente. Génère via : "
            f'python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"'
        )
    if raw.lower() in _FORBIDDEN_SECRETS:
        raise JwtConfigError(f"{JWT_SECRET_ENV} = trivial value forbidden ({raw!r})")
    # Length check: ASCII secret must be ≥ 32 chars (256 bits). If looks like
    # base64, also accept if decoded length ≥ 32 bytes.
    bit_len = len(raw) * 8
    if bit_len < _MIN_SECRET_BITS:
        # Try base64 decode as a fallback — if it decodes to ≥ 32 bytes, accept.
        accepted = False
        try:
            import base64

            decoded = base64.b64decode(raw, validate=True)
            if len(decoded) >= 32:
                accepted = True
        except Exception:
            accepted = False
        if not accepted:
            raise JwtConfigError(
                f"{JWT_SECRET_ENV} doit être ≥ 256 bits (≥ 32 chars ASCII ou ≥ 32 bytes base64-decoded)"
            )
    # Entropy check (ASCII only — base64 strings already have decent entropy by construction).
    entropy = _shannon_entropy(raw)
    if entropy < 4.0:
        # Tolerate base64-looking secrets even if entropy slightly low.
        try:
            import base64

            base64.b64decode(raw, validate=True)
        except Exception:
            raise JwtConfigError(
                f"{JWT_SECRET_ENV} entropy too low ({entropy:.2f} bits/char, min 4.0)"
            )
    return raw


def _validate_registration_token() -> str | None:
    """Validate `SAMSUNGHEALTH_REGISTRATION_TOKEN` (warning if absent, no raise).

    Returns the token if set, else None.
    """
    raw = os.environ.get(REGISTRATION_TOKEN_ENV)
    if not raw:
        _log.warning("auth.registration_token.absent", env_var=REGISTRATION_TOKEN_ENV)
        return None
    return raw


# ── JWT encode / decode ────────────────────────────────────────────────────
def _current_secret() -> str:
    raw = os.environ.get(JWT_SECRET_ENV)
    if not raw:
        raise JwtConfigError(f"{JWT_SECRET_ENV} not set")
    return raw


def _previous_secret() -> str | None:
    raw = os.environ.get(JWT_SECRET_PREVIOUS_ENV)
    return raw or None


def _encode(payload: dict) -> str:
    """Encode payload with current secret + HS256 + kid header."""
    secret = _current_secret()
    return jwt.encode(payload, secret, algorithm=JWT_ALGORITHM, headers={"kid": JWT_KID_CURRENT})


def _decode_with_secret(token: str, secret: str, expected_typ: str) -> dict:
    """Strict decode: HS256 only, all claims required, iss/aud validated."""
    payload = jwt.decode(
        token,
        secret,
        algorithms=[JWT_ALGORITHM],
        options={"require": ["exp", "iat", "sub", "iss", "aud", "typ"]},
        issuer=JWT_ISSUER,
        audience=JWT_AUDIENCE,
    )
    # Belt-and-braces: re-check header alg post-decode (defense vs alg-confusion).
    header = jwt.get_unverified_header(token)
    if header.get("alg") != JWT_ALGORITHM:
        raise jwt.InvalidAlgorithmError(f"alg mismatch: {header.get('alg')!r}")
    if payload.get("typ") != expected_typ:
        raise jwt.InvalidTokenError(f"typ mismatch: expected {expected_typ}, got {payload.get('typ')}")
    return payload


def _decode_try_both(token: str, expected_typ: str) -> dict:
    """Decode with current secret first, then previous (decode-only rotation)."""
    current = _current_secret()
    try:
        return _decode_with_secret(token, current, expected_typ)
    except jwt.InvalidTokenError:
        prev = _previous_secret()
        if prev is None:
            raise
        return _decode_with_secret(token, prev, expected_typ)


def create_access_token(user_id: str) -> str:
    """Create access token (TTL 30min). Payload: sub, iat, exp, iss, aud, typ."""
    now = int(time.time())
    payload = {
        "sub": str(user_id),
        "iat": now,
        "exp": now + ACCESS_TOKEN_TTL_SECONDS,
        "iss": JWT_ISSUER,
        "aud": JWT_AUDIENCE,
        "typ": "access",
    }
    return _encode(payload)


def create_refresh_token(user_id: str, jti: str) -> str:
    """Create refresh token (TTL 30 days). Payload: sub, iat, exp, iss, aud, typ, jti."""
    now = int(time.time())
    payload = {
        "sub": str(user_id),
        "iat": now,
        "exp": now + REFRESH_TOKEN_TTL_SECONDS,
        "iss": JWT_ISSUER,
        "aud": JWT_AUDIENCE,
        "typ": "refresh",
        "jti": str(jti),
    }
    return _encode(payload)


def decode_access_token(token: str) -> dict:
    """Strict decode (current + previous secret). Raise on any deviation."""
    payload = _decode_try_both(token, expected_typ="access")
    # Strip jti if accidentally present (access shouldn't carry it).
    return payload


def decode_refresh_token(token: str) -> dict:
    """Strict decode for refresh JWT (current + previous secret). Raise on any deviation."""
    payload = _decode_try_both(token, expected_typ="refresh")
    if "jti" not in payload:
        raise jwt.InvalidTokenError("refresh token missing jti")
    return payload


# ── refresh token DB helpers ───────────────────────────────────────────────
def revoke_refresh_token(db: Session, jti: str) -> bool:
    """Mark refresh_tokens row as revoked. Returns True if a row was revoked."""
    from datetime import datetime, timezone

    from server.db.models import RefreshToken

    row = db.execute(
        select(RefreshToken).where(RefreshToken.jti == _uuid.UUID(str(jti)))
    ).scalar_one_or_none()
    if row is None:
        return False
    if row.revoked_at is None:
        row.revoked_at = datetime.now(timezone.utc)
    return True


def rotate_refresh_token(
    db: Session, old_jti: str, user_id: _uuid.UUID
) -> tuple[str, str]:
    """Revoke old refresh row + issue new (jti, jwt). Returns (new_jti, new_jwt).

    Caller responsible for db.commit().
    """
    from datetime import datetime, timedelta, timezone

    from server.db.models import RefreshToken

    old_row = db.execute(
        select(RefreshToken).where(RefreshToken.jti == _uuid.UUID(str(old_jti)))
    ).scalar_one_or_none()
    if old_row is None or old_row.revoked_at is not None:
        raise HTTPException(status_code=401, detail="invalid_refresh")

    now = datetime.now(timezone.utc)
    new_jti = _uuid.uuid4()
    new_row = RefreshToken(
        user_id=user_id,
        jti=new_jti,
        issued_at=now,
        expires_at=now + timedelta(seconds=REFRESH_TOKEN_TTL_SECONDS),
    )
    db.add(new_row)
    db.flush()

    old_row.revoked_at = now
    old_row.replaced_by = new_row.id

    new_jwt = create_refresh_token(user_id=str(user_id), jti=str(new_jti))
    return str(new_jti), new_jwt


# ── get_current_user dep ───────────────────────────────────────────────────
def get_current_user(
    authorization: str | None = Header(default=None),
    db: Session = Depends(get_session),
):
    """FastAPI dependency: parse Bearer token, decode, fetch active user, bind contextvars."""
    import structlog

    from server.db.models import User
    from server.middleware.request_context import user_id_var

    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="invalid_credentials")

    token = authorization[7:]
    try:
        payload = decode_access_token(token)
    except Exception:
        raise HTTPException(status_code=401, detail="invalid_credentials")

    sub = payload.get("sub")
    if not sub:
        raise HTTPException(status_code=401, detail="invalid_credentials")

    try:
        user_uuid = _uuid.UUID(sub)
    except (ValueError, TypeError):
        raise HTTPException(status_code=401, detail="invalid_credentials")

    user = db.execute(
        select(User).where(User.id == user_uuid, User.is_active.is_(True))
    ).scalar_one_or_none()
    if user is None:
        raise HTTPException(status_code=401, detail="invalid_credentials")

    user_id_var.set(str(user.id))
    structlog.contextvars.bind_contextvars(user_id=str(user.id))
    return user


# ── helpers used by routers ────────────────────────────────────────────────
def check_registration_token(provided: str | None) -> None:
    """Constant-time compare of header X-Registration-Token vs env var.

    Raise HTTPException 403 if absent (env or header) or mismatch.
    """
    expected = os.environ.get(REGISTRATION_TOKEN_ENV)
    if not expected:
        raise HTTPException(status_code=403, detail="registration_disabled")
    if not provided:
        raise HTTPException(status_code=403, detail="registration_disabled")
    if not secrets.compare_digest(provided, expected):
        raise HTTPException(status_code=403, detail="registration_disabled")
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2-auth-foundation]] — symbols: `hash_password`, `verify_password`, `_DUMMY_HASH`, `create_access_token`, `create_refresh_token`, `decode_access_token`, `rotate_refresh_token`, `revoke_refresh_token`, `get_current_user`, `_validate_jwt_secret_at_boot`, `_validate_registration_token`

### Symbols
- `JwtConfigError` (class) — lines 63-64
- `hash_password` (function) — lines 68-70 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `verify_password` (function) — lines 73-78 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `_shannon_entropy` (function) — lines 87-92
- `_validate_jwt_secret_at_boot` (function) — lines 95-138 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `_validate_registration_token` (function) — lines 141-150 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `_current_secret` (function) — lines 154-158
- `_previous_secret` (function) — lines 161-163
- `_encode` (function) — lines 166-169
- `_decode_with_secret` (function) — lines 172-188
- `_decode_try_both` (function) — lines 191-200
- `create_access_token` (function) — lines 203-214 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `create_refresh_token` (function) — lines 217-229 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `decode_access_token` (function) — lines 232-236 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `decode_refresh_token` (function) — lines 239-244
- `revoke_refresh_token` (function) — lines 248-261 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `rotate_refresh_token` (function) — lines 264-296 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `get_current_user` (function) — lines 300-336 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]
- `check_registration_token` (function) — lines 340-351

### Imports
- `math`
- `os`
- `secrets`
- `time`
- `collections`
- `jwt`
- `argon2`
- `argon2.exceptions`
- `fastapi`
- `sqlalchemy`
- `sqlalchemy.orm`
- `server.database`
- `server.logging_config`

### Exports
- `JwtConfigError`
- `hash_password`
- `verify_password`
- `_shannon_entropy`
- `_validate_jwt_secret_at_boot`
- `_validate_registration_token`
- `_current_secret`
- `_previous_secret`
- `_encode`
- `_decode_with_secret`
- `_decode_try_both`
- `create_access_token`
- `create_refresh_token`
- `decode_access_token`
- `decode_refresh_token`
- `revoke_refresh_token`
- `rotate_refresh_token`
- `get_current_user`
- `check_registration_token`
