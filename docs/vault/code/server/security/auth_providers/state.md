---
type: code-source
language: python
file_path: server/security/auth_providers/state.py
git_blob: d7eecd544276850aa86ebdcb8fa575edcb415b0f
last_synced: '2026-04-27T07:34:23Z'
loc: 110
annotations: []
imports:
- os
- secrets
- time
- uuid
- collections
- typing
- server.logging_config
- server.security.auth
exports:
- OAuthStateInvalid
- OAuthStateReplay
- OAuthStateExpired
- _state_secret
- _evict_if_capped
- generate_oauth_state
- verify_oauth_state
tags:
- code
- python
---

# server/security/auth_providers/state.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/security/auth_providers/state.py`](../../../server/security/auth_providers/state.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.2 — OAuth state CSRF + nonce generation, single-use, LRU cap.

State JWT (HS256) carries jti+iat+exp; the cache is keyed by jti and enforces
single-use (mark `used=True` at callback). LRU eviction caps memory at 10_000
entries (DoS hardening — pentester M5).
"""
from __future__ import annotations

import os
import secrets
import time
import uuid
from collections import OrderedDict
from typing import Any

import jwt as pyjwt

from server.logging_config import get_logger
from server.security.auth import JWT_SECRET_ENV


_log = get_logger(__name__)

_STATE_TTL_SECONDS = 600  # 10 min
_STATE_ALG = "HS256"
_OAUTH_STATE_CACHE_MAX = 10_000

# Module-level LRU-ish cache: {jti: {nonce, created_at, used}}.
_OAUTH_STATE_CACHE: "OrderedDict[str, dict[str, Any]]" = OrderedDict()


class OAuthStateInvalid(Exception):
    """state JWT signature/structure invalid (or jti unknown)."""


class OAuthStateReplay(Exception):
    """state has already been consumed (single-use enforced)."""


class OAuthStateExpired(Exception):
    """state JWT exp claim is in the past."""


def _state_secret() -> str:
    raw = os.environ.get(JWT_SECRET_ENV)
    if not raw:
        raise RuntimeError(f"{JWT_SECRET_ENV} not set")
    return raw


def _evict_if_capped() -> None:
    """LRU eviction when cache exceeds the cap. Reads cap dynamically (test monkeypatches)."""
    import sys

    mod = sys.modules[__name__]
    cap = getattr(mod, "_OAUTH_STATE_CACHE_MAX", _OAUTH_STATE_CACHE_MAX)
    while len(_OAUTH_STATE_CACHE) > cap:
        _OAUTH_STATE_CACHE.popitem(last=False)


def generate_oauth_state() -> tuple[str, str, str]:
    """Generate (state_jwt, jti, nonce). state_jwt is HS256-signed with JWT_SECRET, exp 10min."""
    jti = str(uuid.uuid4())
    nonce = secrets.token_urlsafe(32)
    now = int(time.time())
    state_jwt = pyjwt.encode(
        {"jti": jti, "iat": now, "exp": now + _STATE_TTL_SECONDS},
        _state_secret(),
        algorithm=_STATE_ALG,
    )
    _OAUTH_STATE_CACHE[jti] = {
        "nonce": nonce,
        "created_at": now,
        "used": False,
    }
    _OAUTH_STATE_CACHE.move_to_end(jti, last=True)
    _evict_if_capped()
    return state_jwt, jti, nonce


def verify_oauth_state(state_jwt: str) -> dict[str, Any]:
    """Verify state JWT signature + exp + single-use. Mark used. Returns {nonce, used}.

    Raises OAuthStateInvalid / OAuthStateExpired / OAuthStateReplay on failure.
    """
    try:
        payload = pyjwt.decode(
            state_jwt,
            _state_secret(),
            algorithms=[_STATE_ALG],
            options={"verify_aud": False, "verify_iss": False, "require": ["jti", "exp"]},
        )
    except pyjwt.ExpiredSignatureError as exc:
        raise OAuthStateExpired("state expired") from exc
    except pyjwt.InvalidTokenError as exc:
        raise OAuthStateInvalid(str(exc)) from exc

    jti = payload.get("jti")
    if not jti:
        raise OAuthStateInvalid("missing jti")

    entry = _OAUTH_STATE_CACHE.get(jti)
    if entry is None:
        raise OAuthStateInvalid("unknown jti")
    if entry.get("used"):
        raise OAuthStateReplay("state already used")

    entry["used"] = True
    _OAUTH_STATE_CACHE.move_to_end(jti, last=True)
    return {"nonce": entry["nonce"], "used": True}
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — symbols: `generate_oauth_state`, `verify_oauth_state`, `_OAUTH_STATE_CACHE`

### Symbols
- `OAuthStateInvalid` (class) — lines 32-33
- `OAuthStateReplay` (class) — lines 36-37
- `OAuthStateExpired` (class) — lines 40-41
- `_state_secret` (function) — lines 44-48
- `_evict_if_capped` (function) — lines 51-58
- `generate_oauth_state` (function) — lines 61-78 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]
- `verify_oauth_state` (function) — lines 81-110 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]

### Imports
- `os`
- `secrets`
- `time`
- `uuid`
- `collections`
- `typing`
- `server.logging_config`
- `server.security.auth`

### Exports
- `OAuthStateInvalid`
- `OAuthStateReplay`
- `OAuthStateExpired`
- `_state_secret`
- `_evict_if_capped`
- `generate_oauth_state`
- `verify_oauth_state`
