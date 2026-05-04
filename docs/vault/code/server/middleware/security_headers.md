---
type: code-source
language: python
file_path: server/middleware/security_headers.py
git_blob: 29afa8ed9ed6b6802d92a2d876db23d19953ab90
last_synced: '2026-05-01T12:19:09Z'
loc: 117
annotations: []
imports:
- os
- starlette.middleware.base
- starlette.requests
- starlette.responses
exports:
- _csp_for_path
- _cache_control_for
- _force_https
- SecurityHeadersMiddleware
tags:
- code
- python
---

# server/middleware/security_headers.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/middleware/security_headers.py`](../../../server/middleware/security_headers.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.2 — Global security headers middleware (post-pentester #2/#3/#4/#11/#12).

Applique sur TOUS les responses :
- X-Frame-Options: DENY                                      (anti-clickjacking)
- X-Content-Type-Options: nosniff                            (anti MIME sniff)
- Referrer-Policy: strict-origin-when-cross-origin           (limit Referer leak)
- Permissions-Policy: camera=(), microphone=(), geolocation=()
- Strict-Transport-Security: max-age=63072000; includeSubDomains
  UNIQUEMENT si `request.url.scheme == "https"` OU env `SAMSUNGHEALTH_FORCE_HTTPS=true`
- Strip `server` et `via` headers (anti fingerprint)
- Content-Security-Policy différencié par path (auth strict / dashboard tolère
  unsafe-inline pour D3 / api minimal)
- Cache-Control sur pages auth (no-store) et statics (no-cache si pas de ?v=,
  immutable si versionné)
"""
from __future__ import annotations

import os

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response


_HSTS_VALUE = "max-age=63072000; includeSubDomains"


CSP_AUTH_PAGES = (
    "default-src 'self'; "
    "script-src 'self'; "
    "style-src 'self'; "
    "font-src 'self'; "
    "img-src 'self' data:; "
    "connect-src 'self'; "
    "form-action 'self'; "
    "frame-ancestors 'none'; "
    "base-uri 'self'"
)

CSP_DASHBOARD = (
    "default-src 'self'; "
    "script-src 'self'; "
    "style-src 'self' 'unsafe-inline'; "
    "font-src 'self'; "
    "img-src 'self' data:; "
    "connect-src 'self'; "
    "frame-ancestors 'none'"
)

CSP_API = "default-src 'none'; frame-ancestors 'none'"

# V2.3.3.3 — Admin pages get CSP_AUTH_PAGES base + Trusted Types (pentester #9).
CSP_ADMIN_PAGES = (
    CSP_AUTH_PAGES
    + "; require-trusted-types-for 'script'; trusted-types default"
)


def _csp_for_path(path: str) -> str:
    if path.startswith("/admin/") or path.startswith("/static/admin/"):
        return CSP_ADMIN_PAGES
    if path.startswith("/auth/") or path.startswith("/static/auth/"):
        return CSP_AUTH_PAGES
    if path.startswith("/api/"):
        return CSP_API
    return CSP_DASHBOARD


def _cache_control_for(path: str, query: str) -> str | None:
    """Return the Cache-Control value to apply, or None to leave as-is."""
    # V2.3.3.3 — admin pages no-store (pentester L3).
    if path.startswith("/admin/") and not path.startswith("/static/"):
        return "no-store"
    if path.startswith("/auth/") and not path.startswith("/static/"):
        # Pages HTML auth — no-store (sensitive surface).
        return "no-store"
    if path.startswith("/static/"):
        # Versioned static asset (?v=...) → cached 1 year immutable.
        if query and "v=" in query:
            return "public, max-age=31536000, immutable"
        return "no-cache"
    return None


def _force_https() -> bool:
    return os.environ.get("SAMSUNGHEALTH_FORCE_HTTPS", "false").lower() == "true"


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response: Response = await call_next(request)

        path = request.url.path
        query = request.url.query
        scheme = request.url.scheme

        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        response.headers["Permissions-Policy"] = (
            "camera=(), microphone=(), geolocation=()"
        )

        if scheme == "https" or _force_https():
            response.headers["Strict-Transport-Security"] = _HSTS_VALUE

        response.headers["Content-Security-Policy"] = _csp_for_path(path)

        cc = _cache_control_for(path, query)
        if cc is not None:
            response.headers["Cache-Control"] = cc

        for h in ("server", "via"):
            if h in response.headers:
                del response.headers[h]

        return response
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall]] — symbols: `SecurityHeadersMiddleware`
- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]] — symbols: `_csp_for_admin_path`

### Symbols
- `_csp_for_path` (function) — lines 59-66
- `_cache_control_for` (function) — lines 69-82
- `_force_https` (function) — lines 85-86
- `SecurityHeadersMiddleware` (class) — lines 89-117 · **Specs**: [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall|2026-04-27-v2.3.3.2-frontend-nightfall]]

### Imports
- `os`
- `starlette.middleware.base`
- `starlette.requests`
- `starlette.responses`

### Exports
- `_csp_for_path`
- `_cache_control_for`
- `_force_https`
- `SecurityHeadersMiddleware`
