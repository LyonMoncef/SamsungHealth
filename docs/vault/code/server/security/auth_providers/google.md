---
type: code-source
language: python
file_path: server/security/auth_providers/google.py
git_blob: c2f0187045bc04ac1c6804ef159a4a7fc6fffd78
last_synced: '2026-04-27T07:34:23Z'
loc: 258
annotations: []
imports:
- os
- time
- urllib.parse
- datetime
- typing
- httpx
- server.logging_config
- server.security.auth_providers
exports:
- _filter_claims
- _map_google_error
- GoogleOAuthConfigError
- _validate_google_oauth_env_at_boot
- _compute_jwks_ttl_seconds
- _fetch_jwks
- _key_from_jwks
- GoogleAuthProvider
tags:
- code
- python
---

# server/security/auth_providers/google.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/security/auth_providers/google.py`](../../../server/security/auth_providers/google.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.2 — Google OAuth provider (Authorization Code Flow + ID token validation).

Hardening highlights:
- JWKS endpoint is HARDCODED (anti-SSRF — never derived from `iss`).
- JWKS cache TTL = min(Cache-Control max-age, 4h), fallback 1h.
- raw_claims stored in DB are filtered through an 8-key whitelist (RGPD minimisation).
- Google `error_description` is logged server-side (with redaction) but never
  forwarded in the HTTP response.
"""
from __future__ import annotations

import os
import time
import urllib.parse
from datetime import datetime, timedelta, timezone
from typing import Any

import httpx
import jwt as pyjwt

from server.logging_config import get_logger
from server.security.auth_providers import (
    AuthProvider,
    AuthProviderError,
    ProviderProfile,
)


_log = get_logger(__name__)

# ── env vars ───────────────────────────────────────────────────────────────
_GOOGLE_CLIENT_ID_ENV = "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_ID"
_GOOGLE_CLIENT_SECRET_ENV = "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_SECRET"

# ── constants (hardcoded, do NOT derive from iss claim — SSRF protection) ──
_GOOGLE_AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth"
_GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
_GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs"

_VALID_ISSUERS = frozenset({"https://accounts.google.com", "accounts.google.com"})

_JWKS_TTL_MAX_SECONDS = 4 * 3600  # 4h cap (pentester #5)
_JWKS_TTL_FALLBACK_SECONDS = 3600  # 1h default

# ── raw_claims whitelist (RGPD minimisation, pentester #9) ─────────────────
_RAW_CLAIMS_WHITELIST: frozenset[str] = frozenset(
    {"sub", "email", "email_verified", "iss", "aud", "iat", "exp", "jti"}
)


def _filter_claims(raw: dict) -> dict:
    """Keep only the 8 whitelisted claims (no PII : name/picture/locale/hd/at_hash/nonce)."""
    return {k: v for k, v in raw.items() if k in _RAW_CLAIMS_WHITELIST}


# ── error mapping (pentester #14) ──────────────────────────────────────────
_GOOGLE_ERROR_MAP: dict[str, tuple[int, str]] = {
    "access_denied": (400, "oauth_user_declined"),
    "interaction_required": (400, "oauth_user_declined"),
    "login_required": (400, "oauth_user_declined"),
    "account_selection_required": (400, "oauth_user_declined"),
    "consent_required": (400, "oauth_consent_required"),
    "invalid_request": (500, "oauth_provider_error"),
    "unauthorized_client": (500, "oauth_provider_error"),
    "unsupported_response_type": (500, "oauth_provider_error"),
    "invalid_scope": (500, "oauth_provider_error"),
    "server_error": (502, "oauth_provider_unavailable"),
    "temporarily_unavailable": (503, "oauth_provider_unavailable"),
}


def _map_google_error(error_code: str) -> tuple[int, str]:
    return _GOOGLE_ERROR_MAP.get(error_code, (400, "oauth_callback_error"))


# ── boot validation ────────────────────────────────────────────────────────
class GoogleOAuthConfigError(RuntimeError):
    """Raised when Google OAuth env vars are partially set (one absent without the other)."""


def _validate_google_oauth_env_at_boot() -> bool:
    """Returns True when Google OAuth is enabled, False (None-ish) when disabled.

    Raises GoogleOAuthConfigError if exactly one of the two env vars is set.
    """
    cid = os.environ.get(_GOOGLE_CLIENT_ID_ENV)
    csec = os.environ.get(_GOOGLE_CLIENT_SECRET_ENV)

    if not cid and not csec:
        return False  # disabled mode (dev self-host without GCP)
    if bool(cid) != bool(csec):
        raise GoogleOAuthConfigError(
            f"Both {_GOOGLE_CLIENT_ID_ENV} and {_GOOGLE_CLIENT_SECRET_ENV} "
            "must be set together (or both absent to disable Google OAuth)."
        )
    if not cid.endswith(".apps.googleusercontent.com"):
        raise GoogleOAuthConfigError(
            f"{_GOOGLE_CLIENT_ID_ENV} must end with '.apps.googleusercontent.com'"
        )
    return True


# ── JWKS cache TTL ─────────────────────────────────────────────────────────
def _compute_jwks_ttl_seconds(cache_control: str | None) -> int:
    """Parse Cache-Control header → return TTL capped at 4h. Fallback 1h on missing/malformed."""
    if not cache_control:
        return _JWKS_TTL_FALLBACK_SECONDS
    try:
        for directive in cache_control.split(","):
            directive = directive.strip().lower()
            if directive.startswith("max-age="):
                value = int(directive.split("=", 1)[1].strip())
                return max(0, min(value, _JWKS_TTL_MAX_SECONDS))
    except Exception:
        pass
    return _JWKS_TTL_FALLBACK_SECONDS


# ── JWKS cache (in-memory) ─────────────────────────────────────────────────
_JWKS_CACHE: dict | None = None
_JWKS_CACHE_EXPIRES_AT: datetime | None = None


async def _fetch_jwks() -> dict:
    """Fetch JWKS from the HARDCODED Google endpoint. Caches per Cache-Control TTL (max 4h)."""
    global _JWKS_CACHE, _JWKS_CACHE_EXPIRES_AT
    now = datetime.now(timezone.utc)
    if _JWKS_CACHE is not None and _JWKS_CACHE_EXPIRES_AT is not None:
        if now < _JWKS_CACHE_EXPIRES_AT:
            return _JWKS_CACHE

    async with httpx.AsyncClient(timeout=5.0) as client:
        resp = await client.get(_GOOGLE_JWKS_URL)
    resp.raise_for_status()
    data = resp.json()
    cache_control = (resp.headers or {}).get("Cache-Control")
    ttl = _compute_jwks_ttl_seconds(cache_control)
    _JWKS_CACHE = data
    _JWKS_CACHE_EXPIRES_AT = now + timedelta(seconds=ttl)
    return data


def _key_from_jwks(jwks: dict, kid: str) -> Any:
    """Extract the JWK matching `kid` and return a public key object usable by PyJWT."""
    from jwt.algorithms import RSAAlgorithm

    for key in (jwks or {}).get("keys", []):
        if key.get("kid") == kid:
            return RSAAlgorithm.from_jwk(key)
    raise AuthProviderError(f"jwk kid not found: {kid}")


# ── provider impl ──────────────────────────────────────────────────────────
class GoogleAuthProvider(AuthProvider):
    name = "google"

    def _client_id(self) -> str:
        cid = os.environ.get(_GOOGLE_CLIENT_ID_ENV)
        if not cid:
            raise AuthProviderError("google client_id not configured")
        return cid

    def _client_secret(self) -> str:
        sec = os.environ.get(_GOOGLE_CLIENT_SECRET_ENV)
        if not sec:
            raise AuthProviderError("google client_secret not configured")
        return sec

    def build_authorize_url(
        self, *, state: str, nonce: str, redirect_uri: str
    ) -> str:
        params = {
            "client_id": self._client_id(),
            "redirect_uri": redirect_uri,
            "response_type": "code",
            "scope": "openid email profile",
            "state": state,
            "nonce": nonce,
            "prompt": "select_account",
        }
        return f"{_GOOGLE_AUTHORIZE_URL}?{urllib.parse.urlencode(params)}"

    async def exchange_code_for_tokens(
        self, *, code: str, redirect_uri: str
    ) -> dict:
        body = {
            "code": code,
            "client_id": self._client_id(),
            "client_secret": self._client_secret(),
            "redirect_uri": redirect_uri,
            "grant_type": "authorization_code",
        }
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(_GOOGLE_TOKEN_URL, data=body)
        try:
            resp.raise_for_status()
        except Exception as exc:
            raise AuthProviderError(f"token exchange failed: {exc}") from exc
        data = resp.json()
        if not isinstance(data, dict) or "id_token" not in data:
            raise AuthProviderError("token exchange returned no id_token")
        return data

    async def validate_id_token(
        self, *, id_token: str, expected_nonce: str
    ) -> ProviderProfile:
        try:
            unverified_header = pyjwt.get_unverified_header(id_token)
        except Exception as exc:
            raise AuthProviderError(f"id_token header invalid: {exc}") from exc

        kid = unverified_header.get("kid")
        if not kid:
            raise AuthProviderError("id_token missing kid")
        if unverified_header.get("alg") != "RS256":
            raise AuthProviderError("id_token alg must be RS256")

        # JWKS endpoint is HARDCODED — never derived from iss (anti-SSRF).
        jwks = await _fetch_jwks()
        public_key = _key_from_jwks(jwks, kid)

        client_id = self._client_id()
        try:
            claims = pyjwt.decode(
                id_token,
                public_key,
                algorithms=["RS256"],
                audience=client_id,
                options={"require": ["iss", "aud", "exp", "iat", "sub"]},
            )
        except pyjwt.InvalidTokenError as exc:
            raise AuthProviderError(f"id_token invalid: {exc}") from exc

        iss = claims.get("iss")
        if iss not in _VALID_ISSUERS:
            raise AuthProviderError(f"id_token iss invalid: {iss!r}")

        # iat clock skew tolerance (≤ 10 min in the future).
        iat = claims.get("iat")
        if iat and iat - int(time.time()) > 600:
            raise AuthProviderError("id_token iat too far in the future")

        nonce_claim = claims.get("nonce")
        if not nonce_claim or nonce_claim != expected_nonce:
            raise AuthProviderError("id_token nonce mismatch")

        sub = claims.get("sub")
        email = claims.get("email")
        if not sub or not email:
            raise AuthProviderError("id_token missing sub/email")

        email_verified = bool(claims.get("email_verified", False))
        return ProviderProfile(
            sub=str(sub),
            email=str(email).lower(),
            email_verified=email_verified,
            raw_claims=_filter_claims(claims),
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — symbols: `GoogleAuthProvider`, `_validate_google_oauth_env_at_boot`, `_RAW_CLAIMS_WHITELIST`, `_filter_claims`, `_GOOGLE_ERROR_MAP`

### Symbols
- `_filter_claims` (function) — lines 51-53 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]
- `_map_google_error` (function) — lines 72-73
- `GoogleOAuthConfigError` (class) — lines 77-78
- `_validate_google_oauth_env_at_boot` (function) — lines 81-100 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]
- `_compute_jwks_ttl_seconds` (function) — lines 104-116
- `_fetch_jwks` (function) — lines 124-140
- `_key_from_jwks` (function) — lines 143-150
- `GoogleAuthProvider` (class) — lines 154-258 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]

### Imports
- `os`
- `time`
- `urllib.parse`
- `datetime`
- `typing`
- `httpx`
- `server.logging_config`
- `server.security.auth_providers`

### Exports
- `_filter_claims`
- `_map_google_error`
- `GoogleOAuthConfigError`
- `_validate_google_oauth_env_at_boot`
- `_compute_jwks_ttl_seconds`
- `_fetch_jwks`
- `_key_from_jwks`
- `GoogleAuthProvider`
