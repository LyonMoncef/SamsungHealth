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
