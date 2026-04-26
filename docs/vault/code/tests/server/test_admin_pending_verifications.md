---
type: code-source
language: python
file_path: tests/server/test_admin_pending_verifications.py
git_blob: 26e758acf58f9a1494295c7a9fd1fe4bd755fc19
last_synced: '2026-04-26T22:07:14Z'
loc: 132
annotations: []
imports:
- time
- pytest
exports:
- _register
- TestAdminAuth
- TestAdminCacheLifecycle
tags:
- code
- python
---

# tests/server/test_admin_pending_verifications.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_admin_pending_verifications.py`](../../../tests/server/test_admin_pending_verifications.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 — Admin endpoint GET /admin/pending-verifications.

Tests RED-first contre `server.routers.admin`. Auth admin via X-Registration-Token
(stop-gap V2.3.1 jusqu'à un vrai admin role en V2.3.2+).

Spec: docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md
"""
from __future__ import annotations

import time

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_v231_env(monkeypatch):
    monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", "http://localhost:8000")
    monkeypatch.setenv(
        "SAMSUNGHEALTH_EMAIL_HASH_SALT",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    )


def _register(client, email="admin-pv@example.com", password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


class TestAdminAuth:
    def test_get_without_admin_token_returns_401(self, client_pg_ready):
        """given GET /admin/pending-verifications without X-Registration-Token, when called, then 401.

        spec §Endpoint admin — exige header X-Registration-Token.
        spec §Test d'acceptation #17 — sans X-Registration-Token → 401.
        """
        client = client_pg_ready
        r = client.get("/admin/pending-verifications")
        assert r.status_code == 401, f"expected 401, got {r.status_code}: {r.text}"

    def test_get_with_admin_token_returns_active_tokens(self, client_pg_ready):
        """given a fresh password reset request, when GET /admin/pending-verifications with admin token, then JSON list contains an entry with verify_link reconstructed.

        spec §Endpoint admin — Retourne tokens actifs avec verify_link reconstruit (cache 60s).
        """
        client = client_pg_ready
        _register(client, email="admin-list@example.com")
        client.post(
            "/auth/password/reset/request",
            json={"email": "admin-list@example.com"},
        )

        r = client.get(
            "/admin/pending-verifications",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        data = r.json()
        assert isinstance(data, list), f"expected list, got {type(data).__name__}"
        assert len(data) >= 1, "expected at least one active pending verification"
        entry = data[0]
        assert "verify_link" in entry, f"entry missing verify_link: {entry}"
        # verify_link reconstructs from PUBLIC_BASE_URL, not from request host.
        assert entry["verify_link"].startswith("http://localhost:8000/"), (
            f"verify_link must use SAMSUNGHEALTH_PUBLIC_BASE_URL, got: {entry['verify_link']!r}"
        )
        assert "purpose" in entry
        assert entry["purpose"] in ("password_reset", "email_verification")


class TestAdminCacheLifecycle:
    def test_get_after_60s_cache_expiry_returns_empty(self, client_pg_ready, monkeypatch):
        """given a token issued >60s ago (cache expired), when GET /admin/pending-verifications, then entry is purged from response.

        spec §Compromis explicite — cache TTL 60s ; au-delà, plus de verify_link reconstructible.
        """
        client = client_pg_ready
        _register(client, email="cache-expire@example.com")
        client.post(
            "/auth/password/reset/request",
            json={"email": "cache-expire@example.com"},
        )

        # Force monkeypatch on _outbound_link_cache to simulate expiry (TTL 60s).
        from server.security.email_outbound import _outbound_link_cache

        # Wipe cache to simulate >60s elapsed.
        if hasattr(_outbound_link_cache, "clear"):
            _outbound_link_cache.clear()
        elif hasattr(_outbound_link_cache, "_store"):
            _outbound_link_cache._store.clear()

        r = client.get(
            "/admin/pending-verifications",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )
        assert r.status_code == 200
        data = r.json()
        # All cached entries gone → list empty (DB row exists but no verify_link reconstructible).
        assert data == [], (
            f"expected empty list after cache purge, got {data}"
        )

    def test_get_emits_admin_audit_event(self, client_pg_ready, db_session):
        """given GET /admin/pending-verifications with valid admin token, when called, then 1 row in auth_events with event_type='admin_pending_verifications_read'.

        spec §Endpoint admin — Loggé en auth_events à chaque appel (audit trail des admins).
        spec §Test d'acceptation #18.
        """
        from sqlalchemy import select

        from server.db.models import AuthEvent

        client = client_pg_ready
        client.get(
            "/admin/pending-verifications",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )

        events = db_session.execute(
            select(AuthEvent).where(
                AuthEvent.event_type == "admin_pending_verifications_read"
            )
        ).scalars().all()
        assert len(events) >= 1, (
            f"expected ≥1 admin_pending_verifications_read auth_event row, got {len(events)}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register` (function) — lines 27-32
- `TestAdminAuth` (class) — lines 35-73
- `TestAdminCacheLifecycle` (class) — lines 76-132

### Imports
- `time`
- `pytest`

### Exports
- `_register`
- `TestAdminAuth`
- `TestAdminCacheLifecycle`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — classes: `TestAdminAuth`, `TestAdminCacheLifecycle` · methods: `test_get_without_admin_token_returns_401`, `test_get_with_admin_token_returns_active_tokens`, `test_get_after_60s_cache_expiry_returns_empty`, `test_get_emits_admin_audit_event`
