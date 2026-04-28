---
type: code-source
language: python
file_path: server/routers/static_pages.py
git_blob: fc48a0b4c5a7dccd29a81eec7f98694b9392d08d
last_synced: '2026-04-28T14:04:54Z'
loc: 86
annotations: []
imports:
- pathlib
- fastapi
- fastapi.responses
exports:
- _serve
- _serve_admin
tags:
- code
- python
---

# server/routers/static_pages.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/static_pages.py`](../../../server/routers/static_pages.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.2 — Static auth pages router (GET /auth/*).

Sert les 9 pages HTML auth statiques sous `static/auth/`. FastAPI distingue
GET (page HTML) vs POST (API auth.py) sur les mêmes paths (/auth/login etc).

V2.3.3.3 — ajout des 4 pages admin sous `/admin/{login,pending-verifications}`.
Pour `/admin/users` et `/admin/users/{id}` (collision avec API JSON), le routing
est fait dans `admin.py` via content negotiation Accept header.
"""
from __future__ import annotations

from pathlib import Path

from fastapi import APIRouter
from fastapi.responses import FileResponse


router = APIRouter()

_STATIC_DIR = Path(__file__).resolve().parent.parent.parent / "static"
_AUTH_DIR = _STATIC_DIR / "auth"
_ADMIN_DIR = _STATIC_DIR / "admin"

_NO_STORE = {"Cache-Control": "no-store"}


def _serve(name: str) -> FileResponse:
    return FileResponse(_AUTH_DIR / f"{name}.html", media_type="text/html", headers=_NO_STORE)


def _serve_admin(name: str) -> FileResponse:
    return FileResponse(_ADMIN_DIR / f"{name}.html", media_type="text/html", headers=_NO_STORE)


@router.get("/auth/login", include_in_schema=False)
async def login_page() -> FileResponse:
    return _serve("login")


@router.get("/auth/register", include_in_schema=False)
async def register_page() -> FileResponse:
    return _serve("register")


@router.get("/auth/reset-request", include_in_schema=False)
async def reset_request_page() -> FileResponse:
    return _serve("reset-request")


@router.get("/auth/reset-confirm", include_in_schema=False)
async def reset_confirm_page() -> FileResponse:
    return _serve("reset-confirm")


@router.get("/auth/verify-email", include_in_schema=False)
async def verify_email_page() -> FileResponse:
    return _serve("verify-email")


@router.get("/auth/oauth-link-confirm", include_in_schema=False)
async def oauth_link_confirm_page() -> FileResponse:
    return _serve("oauth-link-confirm")


@router.get("/auth/oauth-success", include_in_schema=False)
async def oauth_success_page() -> FileResponse:
    return _serve("oauth-success")


@router.get("/auth/oauth-error", include_in_schema=False)
async def oauth_error_page() -> FileResponse:
    return _serve("oauth-error")


@router.get("/auth/oauth-link-pending", include_in_schema=False)
async def oauth_link_pending_page() -> FileResponse:
    return _serve("oauth-link-pending")


# ── V2.3.3.3 admin pages ───────────────────────────────────────────────────
# Pages admin HTML uniquement. Les routes qui collision avec une API JSON
# (`/admin/users`, `/admin/users/{id}`, `/admin/pending-verifications`) sont
# gérées dans `admin.py` via content negotiation Accept header.
@router.get("/admin/login", include_in_schema=False)
async def admin_login_page() -> FileResponse:
    return _serve_admin("login")
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall]] — symbols: `router`
- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]] — symbols: `admin_users_page`, `admin_pending_verifications_page`

### Symbols
- `_serve` (function) — lines 27-28
- `_serve_admin` (function) — lines 31-32

### Imports
- `pathlib`
- `fastapi`
- `fastapi.responses`

### Exports
- `_serve`
- `_serve_admin`
