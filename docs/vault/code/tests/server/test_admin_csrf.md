---
type: code-source
language: python
file_path: tests/server/test_admin_csrf.py
git_blob: 826ca9c16fae31dce9f4e1e16209a24a1d857e0f
last_synced: '2026-04-28T14:04:54Z'
loc: 160
annotations: []
imports:
- pytest
exports:
- _register
- _user_id_from_email
- TestSecFetchSiteAdmin
tags:
- code
- python
---

# tests/server/test_admin_csrf.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_admin_csrf.py`](../../../tests/server/test_admin_csrf.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.3 — Sec-Fetch-Site CSRF check étendu à TOUS les POST admin (pentester HIGH H1 fix).

V2.3.3.2 a appliqué `check_sec_fetch_site(request)` aux endpoints `/auth/*` POST mais
a oublié `/admin/*`. V2.3.3.3 corrige : `check_sec_fetch_site(request)` en première
ligne de `lock_user`, `unlock_user`, `admin_probe`, et tout futur POST admin.

Tests RED-first contre :
- POST /admin/users/{id}/lock
- POST /admin/users/{id}/unlock
- POST /admin/probe (NEW V2.3.3.3)

Spec: docs/vault/specs/2026-04-28-v2.3.3.3-auth-finitions.md
  §"CSRF check `Sec-Fetch-Site` étendu admin (pentester HIGH H1 fix)"
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


def _register(client, email="admin-csrf@example.com", password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


def _user_id_from_email(db_session, email):
    from sqlalchemy import select

    from server.db.models import User

    return db_session.execute(select(User).where(User.email == email)).scalar_one().id


class TestSecFetchSiteAdmin:
    """spec §"CSRF check `Sec-Fetch-Site` étendu admin" — cross-site → 403 sur les 3 POST admin."""

    def test_admin_lock_cross_site_returns_403(self, client_pg_ready, db_session):
        """given POST /admin/users/{id}/lock with Sec-Fetch-Site: cross-site + valid token, when called, then 403 csrf_check_failed (CSRF check fires BEFORE business logic).

        spec §"CSRF check `Sec-Fetch-Site` étendu admin" — `check_sec_fetch_site(request)`
        en première ligne de `lock_user`.
        """
        client = client_pg_ready
        _register(client, email="csrf-lock@example.com")
        uid = _user_id_from_email(db_session, "csrf-lock@example.com")

        r = client.post(
            f"/admin/users/{uid}/lock",
            headers={
                "Sec-Fetch-Site": "cross-site",
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                "Content-Type": "application/json",
            },
            json={"duration_minutes": 30, "reason": "csrf-test"},
        )
        assert r.status_code == 403, (
            f"POST /admin/users/{uid}/lock cross-site MUST 403 csrf_check_failed, "
            f"got {r.status_code}: {r.text}"
        )
        try:
            detail = r.json().get("detail", "")
        except Exception:
            detail = r.text
        assert detail == "csrf_check_failed", (
            f"expected detail='csrf_check_failed', got {detail!r}"
        )

    def test_admin_unlock_cross_site_returns_403(self, client_pg_ready, db_session):
        """given POST /admin/users/{id}/unlock with Sec-Fetch-Site: cross-site + valid token, when called, then 403 csrf_check_failed.

        spec §"CSRF check `Sec-Fetch-Site` étendu admin" — `unlock_user` aussi protégé.
        """
        client = client_pg_ready
        _register(client, email="csrf-unlock@example.com")
        uid = _user_id_from_email(db_session, "csrf-unlock@example.com")

        r = client.post(
            f"/admin/users/{uid}/unlock",
            headers={
                "Sec-Fetch-Site": "cross-site",
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
            },
        )
        assert r.status_code == 403, (
            f"POST /admin/users/{uid}/unlock cross-site MUST 403, got {r.status_code}: {r.text}"
        )
        try:
            detail = r.json().get("detail", "")
        except Exception:
            detail = r.text
        assert detail == "csrf_check_failed", (
            f"expected detail='csrf_check_failed', got {detail!r}"
        )

    def test_admin_probe_cross_site_returns_403(self, client_pg_ready):
        """given POST /admin/probe with Sec-Fetch-Site: cross-site + valid token, when called, then 403 csrf_check_failed.

        spec §"Endpoint POST /admin/probe" — sentinel endpoint qui doit aussi
        être CSRF-protégé.
        """
        # RED-couple: admin_probe handler must exist.
        from server.routers.admin import admin_probe  # noqa: F401

        client = client_pg_ready
        r = client.post(
            "/admin/probe",
            headers={
                "Sec-Fetch-Site": "cross-site",
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                "Content-Type": "application/json",
            },
            json={},
        )
        assert r.status_code == 403, (
            f"POST /admin/probe cross-site MUST 403 csrf_check_failed, "
            f"got {r.status_code}: {r.text}"
        )
        try:
            detail = r.json().get("detail", "")
        except Exception:
            detail = r.text
        assert detail == "csrf_check_failed", (
            f"expected detail='csrf_check_failed', got {detail!r}"
        )

    def test_admin_lock_same_origin_passes_csrf_check(self, client_pg_ready, db_session):
        """given POST /admin/users/{id}/lock with Sec-Fetch-Site: same-origin + valid token, when called, then NOT 403 (CSRF passes).

        spec §"CSRF check `Sec-Fetch-Site` étendu admin" — same-origin = legit
        browser request → flow normal (200/204). Couples to admin_probe import to
        ensure the CSRF refactor was wired through.
        """
        # RED-couple : admin_probe must exist AND lock_user must use shared CSRF helper.
        from server.routers.admin import admin_probe  # noqa: F401

        client = client_pg_ready
        _register(client, email="csrf-lock-ok@example.com")
        uid = _user_id_from_email(db_session, "csrf-lock-ok@example.com")

        r = client.post(
            f"/admin/users/{uid}/lock",
            headers={
                "Sec-Fetch-Site": "same-origin",
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                "Content-Type": "application/json",
            },
            json={"duration_minutes": 30, "reason": "csrf-ok"},
        )
        assert r.status_code != 403, (
            f"POST /admin/users/{uid}/lock same-origin MUST NOT 403 (CSRF too aggressive), "
            f"got 403: {r.text}"
        )
        assert r.status_code in (200, 204), (
            f"POST /admin/users/{uid}/lock same-origin expected 200/204, got {r.status_code}: {r.text}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register` (function) — lines 23-28
- `_user_id_from_email` (function) — lines 31-36
- `TestSecFetchSiteAdmin` (class) — lines 39-160

### Imports
- `pytest`

### Exports
- `_register`
- `_user_id_from_email`
- `TestSecFetchSiteAdmin`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]] — classes: `TestSecFetchSiteAdmin`
