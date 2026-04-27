---
type: code-source
language: python
file_path: server/middleware/rate_limit_context.py
git_blob: d313d288a26c7bde09d6ebe6bf2345462666195c
last_synced: '2026-04-27T17:56:06Z'
loc: 78
annotations: []
imports:
- json
- starlette.middleware.base
- starlette.requests
- starlette.types
exports:
- RateLimitContextMiddleware
tags:
- code
- python
---

# server/middleware/rate_limit_context.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/middleware/rate_limit_context.py`](../../../server/middleware/rate_limit_context.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.1 — Body-peek middleware for slowapi composite keys.

The slowapi `key_func` receives the `Request` BEFORE the route handler reads
the body. To support composite keys like `(IP, email)` for /auth/login, we
peek the JSON body here, set `request.state.rate_limit_email`, and replay the
body so the handler can re-read it.

Only paths in `_BODY_PEEK_PATHS` are peeked (cost: ~few hundred bytes JSON).
"""
from __future__ import annotations

import json

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.types import ASGIApp


# Routes where we want to extract `email` for composite key (IP, email).
_EMAIL_BODY_PEEK_PATHS = frozenset(
    {
        "/auth/login",
        "/auth/verify-email/request",
        "/auth/password/reset/request",
    }
)

# Routes where we want to extract `refresh_token`'s sub for composite (IP, user).
_REFRESH_BODY_PEEK_PATHS = frozenset({"/auth/refresh"})


class RateLimitContextMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        method = request.scope.get("method", "GET")
        path = request.scope.get("path", "")

        if method == "POST" and (
            path in _EMAIL_BODY_PEEK_PATHS or path in _REFRESH_BODY_PEEK_PATHS
        ):
            body = b""
            try:
                body = await request.body()
            except Exception:
                body = b""

            if body:
                try:
                    parsed = json.loads(body)
                    if isinstance(parsed, dict):
                        if path in _EMAIL_BODY_PEEK_PATHS:
                            email = parsed.get("email")
                            if isinstance(email, str) and email:
                                request.state.rate_limit_email = email
                        if path in _REFRESH_BODY_PEEK_PATHS:
                            token = parsed.get("refresh_token")
                            if isinstance(token, str) and token:
                                # Decode minimal sub claim. Failures are fine: fall back to "_unknown".
                                try:
                                    from server.security.auth import (
                                        decode_refresh_token,
                                    )

                                    payload = decode_refresh_token(token)
                                    sub = payload.get("sub")
                                    if sub:
                                        request.state.rate_limit_user_id = str(sub)
                                except Exception:
                                    pass
                except (json.JSONDecodeError, ValueError):
                    pass

            # Replay the body so downstream can re-read it.
            async def receive():
                return {"type": "http.request", "body": body, "more_body": False}

            request._receive = receive

        return await call_next(request)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `RateLimitContextMiddleware` (class) — lines 32-78

### Imports
- `json`
- `starlette.middleware.base`
- `starlette.requests`
- `starlette.types`

### Exports
- `RateLimitContextMiddleware`
