---
type: code-source
language: python
file_path: tests/server/test_admin_users_endpoint.py
git_blob: 185bec72eea6b3efd788433317fc10b4778f5a6a
last_synced: '2026-04-28T14:04:54Z'
loc: 432
annotations: []
imports:
- re
- pytest
exports:
- _register
- _user_id_from_email
- TestListUsers
- TestGetUserDetail
- TestPasswordHashNeverExposed
- TestLastLoginIpHmacOnly
- TestAuditTrail
- TestRecentEventsSanitized
tags:
- code
- python
---

# tests/server/test_admin_users_endpoint.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_admin_users_endpoint.py`](../../../tests/server/test_admin_users_endpoint.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.3 — Admin endpoints GET /admin/users + GET /admin/users/{id}.

Tests RED-first contre `server.routers.admin.list_users` + `get_user_detail` (NEW)
+ Pydantic `UserSummary` + `UserDetail` (NEW).

Cas couverts (spec §"Endpoints backend nouveaux" + tests #1-#8) :
- 401 sans X-Registration-Token (list + detail)
- 200 avec token, JSON shape attendue
- `password_hash` JAMAIS exposé (HIGH spec §1)
- `last_login_ip` brut JAMAIS exposé (HIGH spec §pentester #2)
  → uniquement `last_login_ip_hash` HMAC 16 hex chars
- 404 sur user inexistant
- audit `admin_users_list` row inséré dans `auth_events`
- audit `admin_user_detail_read` row inséré
- recent_events sanitize `request_id` field abuse (V2.3.3.1 lock event reason)

Spec: docs/vault/specs/2026-04-28-v2.3.3.3-auth-finitions.md
  §"Endpoints backend nouveaux"
  §"Decisions techniques HIGH bloquants" #2 (last_login_ip HMAC)
  §"request_id field abuse refactor" (sanitize recent_events)
  §Tests d'acceptation #1-#8.
"""
from __future__ import annotations

import re

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


def _register(client, email="admin-users@example.com", password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


def _user_id_from_email(db_session, email):
    from sqlalchemy import select

    from server.db.models import User

    return db_session.execute(select(User).where(User.email == email)).scalar_one().id


# ── List users ─────────────────────────────────────────────────────────────
class TestListUsers:
    """spec test #1 + #2 — GET /admin/users sans/avec X-Registration-Token."""

    def test_list_users_without_token_returns_401(self, client_pg_ready):
        """given GET /admin/users sans X-Registration-Token, when called, then 401.

        spec test #1 — admin gated.
        """
        client = client_pg_ready
        r = client.get("/admin/users")
        assert r.status_code == 401, (
            f"expected 401 unauth, got {r.status_code}: {r.text}"
        )

    def test_list_users_with_token_returns_200_list(self, client_pg_ready):
        """given GET /admin/users + valid token, when called, then 200 + JSON list of UserSummary dicts.

        spec test #2 — JSON `[{id, email, is_active, ...}]`.
        Couples to `server.routers.admin.list_users` symbol existence (RED if missing).
        """
        # RED-couple: symbol must exist.
        from server.routers.admin import list_users  # noqa: F401

        client = client_pg_ready
        _register(client, email="list-1@example.com")
        _register(client, email="list-2@example.com")

        r = client.get(
            "/admin/users",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )
        assert r.status_code == 200, (
            f"expected 200 with token, got {r.status_code}: {r.text}"
        )
        data = r.json()
        assert isinstance(data, list), f"expected list response, got {type(data).__name__}"
        assert len(data) >= 2, f"expected ≥2 users (just registered), got {len(data)}: {data}"
        # Each row is a UserSummary-like dict.
        for entry in data:
            assert isinstance(entry, dict), f"each row must be a dict, got {type(entry).__name__}"
            assert "id" in entry, f"row missing 'id': {entry}"
            assert "email" in entry, f"row missing 'email': {entry}"
            assert "is_active" in entry, f"row missing 'is_active': {entry}"


# ── Get user detail ────────────────────────────────────────────────────────
class TestGetUserDetail:
    """spec test #3 + #4 + #5 — GET /admin/users/{id}."""

    def test_get_user_detail_without_token_returns_401(self, client_pg_ready, db_session):
        """given GET /admin/users/{id} sans token, when called, then 401.

        spec test #3 — admin gated.
        """
        client = client_pg_ready
        _register(client, email="detail-noauth@example.com")
        uid = _user_id_from_email(db_session, "detail-noauth@example.com")
        r = client.get(
            f"/admin/users/{uid}",
            headers={"Accept": "application/json"},
        )
        assert r.status_code == 401, (
            f"expected 401 unauth on user detail, got {r.status_code}: {r.text}"
        )

    def test_get_user_detail_with_token_returns_user_providers_recent_events(
        self, client_pg_ready, db_session
    ):
        """given GET /admin/users/{id} + valid token, when called, then 200 + {user, providers, recent_events}.

        spec test #4 — `{user, providers: [...], recent_events: [...]}`.
        Couples to `server.routers.admin.get_user_detail` symbol existence.
        """
        # RED-couple: symbol must exist.
        from server.routers.admin import get_user_detail  # noqa: F401

        client = client_pg_ready
        _register(client, email="detail-ok@example.com")
        uid = _user_id_from_email(db_session, "detail-ok@example.com")

        r = client.get(
            f"/admin/users/{uid}",
            headers={
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                "Accept": "application/json",
            },
        )
        assert r.status_code == 200, (
            f"expected 200 with token, got {r.status_code}: {r.text}"
        )
        data = r.json()
        assert isinstance(data, dict), f"expected dict, got {type(data).__name__}"
        assert "user" in data, f"response missing 'user' key: {data}"
        assert "providers" in data, f"response missing 'providers' key: {data}"
        assert "recent_events" in data, f"response missing 'recent_events' key: {data}"
        assert isinstance(data["providers"], list), "providers must be list"
        assert isinstance(data["recent_events"], list), "recent_events must be list"

    def test_get_user_detail_nonexistent_uuid_returns_404(self, client_pg_ready):
        """given GET /admin/users/<random-uuid> + valid token, when called, then 404 user_not_found.

        spec test #5.
        """
        from server.routers.admin import get_user_detail  # noqa: F401

        client = client_pg_ready
        random_uuid = "00000000-0000-7000-8000-000000000fff"
        r = client.get(
            f"/admin/users/{random_uuid}",
            headers={
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                "Accept": "application/json",
            },
        )
        assert r.status_code == 404, (
            f"expected 404 on nonexistent user, got {r.status_code}: {r.text}"
        )
        try:
            detail = r.json().get("detail", "")
        except Exception:
            detail = r.text
        assert "user_not_found" in detail, (
            f"expected detail='user_not_found', got {detail!r}"
        )


# ── password_hash NEVER exposed ────────────────────────────────────────────
class TestPasswordHashNeverExposed:
    """spec test #7 + §HIGH bloquants — `password_hash` JAMAIS exposé via /admin/users[/<id>]."""

    def test_password_hash_not_in_list_users_response(self, client_pg_ready):
        """given GET /admin/users + token, when response inspected, then no `password_hash` key in any user dict + no `password_hash` substring in raw text.

        spec test #7 + §UserSummary.
        """
        from server.routers.admin import list_users  # noqa: F401

        client = client_pg_ready
        _register(client, email="ph-list@example.com")
        r = client.get(
            "/admin/users",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        # 1) Raw text grep — defense in depth (catches any nested object).
        assert "password_hash" not in r.text, (
            f"GET /admin/users response leaks 'password_hash' substring: {r.text[:500]!r}"
        )
        # 2) Parse JSON and assert no key in any user dict.
        data = r.json()
        for entry in data:
            assert "password_hash" not in entry, (
                f"UserSummary leaks password_hash field: {entry}"
            )

    def test_password_hash_not_in_user_detail_response(self, client_pg_ready, db_session):
        """given GET /admin/users/{id} + token, when response inspected, then no `password_hash` substring nor key.

        spec test #7 + §UserSummary (réutilisé dans UserDetail.user).
        """
        from server.routers.admin import get_user_detail  # noqa: F401

        client = client_pg_ready
        _register(client, email="ph-detail@example.com")
        uid = _user_id_from_email(db_session, "ph-detail@example.com")
        r = client.get(
            f"/admin/users/{uid}",
            headers={
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                "Accept": "application/json",
            },
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        assert "password_hash" not in r.text, (
            f"GET /admin/users/{uid} leaks 'password_hash' substring: {r.text[:500]!r}"
        )
        data = r.json()
        user_obj = data.get("user", {})
        assert "password_hash" not in user_obj, (
            f"UserDetail.user leaks password_hash key: {user_obj}"
        )


# ── last_login_ip HMAC only — never raw ────────────────────────────────────
class TestLastLoginIpHmacOnly:
    """spec §"HIGH bloquants" #2 — `last_login_ip` brut JAMAIS exposé ; uniquement `last_login_ip_hash` HMAC 16 hex."""

    def test_last_login_ip_raw_not_exposed_in_list_users(
        self, client_pg_ready, db_session
    ):
        """given user with non-null `last_login_ip` in DB, when GET /admin/users + token, then row NO `last_login_ip` key + the raw IP value (e.g. "127.0.0.1") not in response text.

        spec §HIGH bloquants #2 — UserSummary expose `last_login_ip_hash`, jamais brut.
        """
        from sqlalchemy import update

        from server.db.models import User
        from server.routers.admin import list_users  # noqa: F401

        client = client_pg_ready
        _register(client, email="lip-list@example.com")
        uid = _user_id_from_email(db_session, "lip-list@example.com")
        # Force a last_login_ip in DB so the field is non-null.
        db_session.execute(
            update(User).where(User.id == uid).values(last_login_ip="203.0.113.42")
        )
        db_session.commit()

        r = client.get(
            "/admin/users",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        # Raw IP must not be in the body.
        assert "203.0.113.42" not in r.text, (
            f"GET /admin/users leaks raw last_login_ip '203.0.113.42': {r.text[:500]!r}"
        )
        data = r.json()
        target = next((u for u in data if u.get("email") == "lip-list@example.com"), None)
        assert target is not None, (
            f"user lip-list@example.com missing from list response: {data}"
        )
        assert "last_login_ip" not in target, (
            f"UserSummary MUST NOT have last_login_ip key (raw INET leak), got {target}"
        )

    def test_last_login_ip_hash_present_and_hmac_16_hex(
        self, client_pg_ready, db_session
    ):
        """given user with `last_login_ip` set, when GET /admin/users + token, then the user row has key `last_login_ip_hash` = 16 lowercase hex chars (HMAC truncated).

        spec §HIGH bloquants #2 + §UserSummary — `last_login_ip_hash` calculé via
        `_ip_hash()` helper réutilisé depuis rate_limit.py (HMAC-SHA256, 16 hex chars).
        """
        from sqlalchemy import update

        from server.db.models import User
        from server.routers.admin import list_users  # noqa: F401

        client = client_pg_ready
        _register(client, email="lip-hash@example.com")
        uid = _user_id_from_email(db_session, "lip-hash@example.com")
        db_session.execute(
            update(User).where(User.id == uid).values(last_login_ip="198.51.100.7")
        )
        db_session.commit()

        r = client.get(
            "/admin/users",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        data = r.json()
        target = next((u for u in data if u.get("email") == "lip-hash@example.com"), None)
        assert target is not None, f"user not found in list: {data}"
        assert "last_login_ip_hash" in target, (
            f"UserSummary MUST have last_login_ip_hash key (HMAC), got keys={list(target.keys())}"
        )
        ip_hash = target["last_login_ip_hash"]
        assert isinstance(ip_hash, str), (
            f"last_login_ip_hash must be str, got {type(ip_hash).__name__}"
        )
        assert re.fullmatch(r"[0-9a-f]{16}", ip_hash), (
            f"last_login_ip_hash must be 16 lowercase hex chars (HMAC truncated), got {ip_hash!r}"
        )


# ── Audit trail ────────────────────────────────────────────────────────────
class TestAuditTrail:
    """spec test #8 + §pentester #4 — chaque appel admin écrit un auth_event."""

    def test_list_users_inserts_admin_users_list_audit_event(
        self, client_pg_ready, db_session
    ):
        """given GET /admin/users + valid token, when called, then ≥1 row in auth_events with event_type='admin_users_list'.

        spec test #8 — audit trail des admins.
        """
        from sqlalchemy import select

        from server.db.models import AuthEvent
        from server.routers.admin import list_users  # noqa: F401

        client = client_pg_ready
        client.get(
            "/admin/users",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )

        events = db_session.execute(
            select(AuthEvent).where(AuthEvent.event_type == "admin_users_list")
        ).scalars().all()
        assert len(events) >= 1, (
            f"expected ≥1 admin_users_list audit event, got {len(events)}"
        )

    def test_get_user_detail_inserts_admin_user_detail_read_audit_event(
        self, client_pg_ready, db_session
    ):
        """given GET /admin/users/{id} + valid token, when called, then ≥1 row in auth_events with event_type='admin_user_detail_read'.

        spec §pentester #4 + audit trail per call.
        """
        from sqlalchemy import select

        from server.db.models import AuthEvent
        from server.routers.admin import get_user_detail  # noqa: F401

        client = client_pg_ready
        _register(client, email="audit-detail@example.com")
        uid = _user_id_from_email(db_session, "audit-detail@example.com")
        client.get(
            f"/admin/users/{uid}",
            headers={
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                "Accept": "application/json",
            },
        )

        events = db_session.execute(
            select(AuthEvent).where(AuthEvent.event_type == "admin_user_detail_read")
        ).scalars().all()
        assert len(events) >= 1, (
            f"expected ≥1 admin_user_detail_read audit event, got {len(events)}"
        )


# ── recent_events sanitize request_id field abuse ──────────────────────────
class TestRecentEventsSanitized:
    """spec §"request_id field abuse refactor" — recent_events strip le request_id pour les events admin_user_locked/unlocked qui contenait reason+duration brut V2.3.3.1."""

    def test_recent_events_does_not_leak_request_id_with_lock_reason(
        self, client_pg_ready, db_session
    ):
        """given user admin-locked with reason='breach-PII-leak', when GET /admin/users/{id} + token, then `recent_events` n'expose PAS `reason:breach-PII-leak` brut dans request_id field.

        spec §"request_id field abuse refactor" — sanitize recent_events ;
        request_id stripped et meta.reason / meta.duration_minutes proprement séparés.
        Le reason ne doit JAMAIS leak en clair dans le champ `request_id` de la réponse JSON.
        """
        from server.routers.admin import get_user_detail  # noqa: F401

        client = client_pg_ready
        _register(client, email="reason-leak@example.com")
        uid = _user_id_from_email(db_session, "reason-leak@example.com")

        # Use V2.3.3.1 lock_user endpoint that put `reason:...` in request_id.
        client.post(
            f"/admin/users/{uid}/lock",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"duration_minutes": 30, "reason": "breach-PII-leak"},
        )

        r = client.get(
            f"/admin/users/{uid}",
            headers={
                "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
                "Accept": "application/json",
            },
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        data = r.json()
        events = data.get("recent_events", [])
        assert len(events) >= 1, (
            f"expected ≥1 recent_event after lock, got {events}"
        )

        # Sanitize check #1 : the `reason:breach-PII-leak` blob must NOT appear in the
        # request_id field of any event in recent_events.
        for ev in events:
            req_id = ev.get("request_id")
            if req_id is None:
                continue
            assert "reason:breach-PII-leak" not in str(req_id), (
                f"recent_events leaks lock reason in request_id field "
                f"(spec §request_id field abuse refactor): {ev}"
            )

        # Sanitize check #2 : even raw text grep — the literal V2.3.3.1 blob format
        # `reason:breach-PII-leak|duration_minutes:30` must NOT be present anywhere.
        assert "reason:breach-PII-leak|duration_minutes" not in r.text, (
            f"recent_events response leaks raw V2.3.3.1 request_id blob format: {r.text[:600]!r}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register` (function) — lines 33-38
- `_user_id_from_email` (function) — lines 41-46
- `TestListUsers` (class) — lines 50-92
- `TestGetUserDetail` (class) — lines 96-173
- `TestPasswordHashNeverExposed` (class) — lines 177-230
- `TestLastLoginIpHmacOnly` (class) — lines 234-314
- `TestAuditTrail` (class) — lines 318-374
- `TestRecentEventsSanitized` (class) — lines 378-432

### Imports
- `re`
- `pytest`

### Exports
- `_register`
- `_user_id_from_email`
- `TestListUsers`
- `TestGetUserDetail`
- `TestPasswordHashNeverExposed`
- `TestLastLoginIpHmacOnly`
- `TestAuditTrail`
- `TestRecentEventsSanitized`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]] — classes: `TestListUsers`, `TestGetUserDetail`, `TestPasswordHashNeverExposed`, `TestLastLoginIpHmacOnly`, `TestAuditTrail`, `TestRecentEventsSanitized`
