---
type: code-source
language: python
file_path: tests/server/test_me_audit_log.py
git_blob: 1b10fd6832a00ab9d434558c6aba4634f8aa6d5e
last_synced: '2026-04-28T23:12:43Z'
loc: 372
annotations: []
imports:
- datetime
- pytest
exports:
- _register_and_login
- _bearer
- _seed_auth_event
- TestAuditLogScope
- TestAuditLogAdminFilter
- TestAuditLogPagination
- TestAuditLogIpHmac
- TestAuditLogMetaCap
- TestAuditLogRateLimit
tags:
- code
- python
---

# tests/server/test_me_audit_log.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_me_audit_log.py`](../../../tests/server/test_me_audit_log.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Phase 3 RGPD — `GET /me/audit-log` (Art. 15 right of access).

Tests RED-first contre `server.routers.me` (NEW) + `server.security.rgpd` (NEW).

Couvre les cas spec §"Audit-log" #28-#37 :
- Scope user (UNIQUEMENT events `user_id == current_user.id`).
- Filter `admin_*` par défaut (Décision 6) ; `?include_admin=true` les inclut.
- Pagination limit/offset, total count global.
- `ip_hash` HMAC truncated (16 hex), jamais d'IP brute.
- Cap meta size 4KB (truncate + flag).
- Rate-limit 60/h/user, 61e → 429.
- Ordering DESC par `created_at`.

Spec : docs/vault/specs/2026-04-28-phase3-rgpd-endpoints.md §3.
"""
from __future__ import annotations

from datetime import datetime, timedelta, timezone

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"
_DEFAULT_PASSWORD = "longpassword12345"


# ── helpers ───────────────────────────────────────────────────────────────


def _register_and_login(client, email: str, password: str = _DEFAULT_PASSWORD) -> str:
    reg = client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )
    assert reg.status_code in (201, 409), f"register failed: {reg.text}"
    log = client.post("/auth/login", json={"email": email, "password": password})
    assert log.status_code == 200, f"login failed: {log.text}"
    return log.json()["access_token"]


def _bearer(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}"}


def _seed_auth_event(db_session, user_id, event_type: str, meta: dict | None = None):
    from server.db.models import AuthEvent

    db_session.add(
        AuthEvent(
            user_id=user_id,
            event_type=event_type,
            ip_hash="a" * 16,  # 16 hex truncated HMAC (cohérent V2.3.3.1)
            user_agent="pytest-client",
            email_hash=None,
            meta=meta or {},
            created_at=datetime.now(timezone.utc),
        )
    )
    db_session.commit()


# ── TestAuditLogScope (3 tests) ───────────────────────────────────────────


class TestAuditLogScope:
    # spec test #28
    def test_audit_log_without_bearer_returns_401(self, client_pg_ready):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        r = client.get("/me/audit-log")
        assert r.status_code == 401, f"expected 401, got {r.status_code}: {r.text}"

    # spec test #29
    def test_audit_log_returns_payload_shape(self, client_pg_ready):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "shape-audit@example.com")
        r = client.get("/me/audit-log", headers=_bearer(access))
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        body = r.json()
        for key in ("events", "total", "limit", "offset"):
            assert key in body, f"missing {key} in body: {body.keys()}"
        assert isinstance(body["events"], list)
        assert isinstance(body["total"], int)

    # spec test #30
    def test_audit_log_scope_only_current_user(self, client_pg_ready, db_session):
        from sqlalchemy import select

        from server.db.models import User
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access_a = _register_and_login(client, "scope-a@example.com")
        # Register user B (without logging in as them).
        client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": "scope-b@example.com", "password": _DEFAULT_PASSWORD},
        )
        user_b = db_session.execute(
            select(User).where(User.email == "scope-b@example.com")
        ).scalar_one()
        # Add a custom event for user B.
        _seed_auth_event(db_session, user_b.id, "scope.b.private")

        r = client.get("/me/audit-log", headers=_bearer(access_a))
        assert r.status_code == 200
        events = r.json()["events"]
        for ev in events:
            assert ev.get("event_type") != "scope.b.private", (
                f"USER B EVENT LEAKED IN USER A AUDIT LOG: {ev}"
            )


# ── TestAuditLogAdminFilter (2 tests) ─────────────────────────────────────


class TestAuditLogAdminFilter:
    # spec test #31
    def test_audit_log_admin_events_excluded_by_default(
        self, client_pg_ready, db_session
    ):
        from sqlalchemy import select

        from server.db.models import User
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "admin-filter@example.com")
        user = db_session.execute(
            select(User).where(User.email == "admin-filter@example.com")
        ).scalar_one()
        _seed_auth_event(db_session, user.id, "admin_user_lock")
        _seed_auth_event(db_session, user.id, "login.success")

        r = client.get("/me/audit-log", headers=_bearer(access))
        assert r.status_code == 200
        events = r.json()["events"]
        types = [e.get("event_type") for e in events]
        assert "admin_user_lock" not in types, (
            f"admin_* events must be excluded by default, got types={types}"
        )
        assert "login.success" in types, (
            f"non-admin events must be present, got types={types}"
        )

    # spec test #32
    def test_audit_log_admin_events_included_when_flag_set(
        self, client_pg_ready, db_session
    ):
        from sqlalchemy import select

        from server.db.models import User
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "admin-include@example.com")
        user = db_session.execute(
            select(User).where(User.email == "admin-include@example.com")
        ).scalar_one()
        _seed_auth_event(db_session, user.id, "admin_user_unlock")

        r = client.get(
            "/me/audit-log",
            headers=_bearer(access),
            params={"include_admin": "true"},
        )
        assert r.status_code == 200
        types = [e.get("event_type") for e in r.json()["events"]]
        assert "admin_user_unlock" in types, (
            f"admin_* events must be included with include_admin=true, got types={types}"
        )


# ── TestAuditLogPagination (2 tests) ──────────────────────────────────────


class TestAuditLogPagination:
    # spec test #34
    def test_pagination_limit_offset_and_total(self, client_pg_ready, db_session):
        from sqlalchemy import select

        from server.db.models import User
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "paginate@example.com")
        user = db_session.execute(
            select(User).where(User.email == "paginate@example.com")
        ).scalar_one()
        # Seed 30 non-admin events.
        for i in range(30):
            _seed_auth_event(db_session, user.id, f"custom.event.{i:02d}")

        r = client.get(
            "/me/audit-log",
            headers=_bearer(access),
            params={"limit": 10, "offset": 5},
        )
        assert r.status_code == 200
        body = r.json()
        assert len(body["events"]) <= 10, (
            f"limit=10 should cap events at 10, got {len(body['events'])}"
        )
        # Total = global count (≥ 30 + login events seeded by register).
        assert body["total"] >= 30, (
            f"total must reflect global count (≥30), got {body['total']}"
        )
        assert body["offset"] == 5
        assert body["limit"] == 10

    # spec test #35
    def test_ordering_desc_by_created_at(self, client_pg_ready, db_session):
        from sqlalchemy import select

        from server.db.models import AuthEvent, User
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "order@example.com")
        user = db_session.execute(
            select(User).where(User.email == "order@example.com")
        ).scalar_one()
        # Insert events at increasing created_at.
        base = datetime.now(timezone.utc)
        for i in range(3):
            db_session.add(
                AuthEvent(
                    user_id=user.id,
                    event_type=f"custom.order.{i}",
                    ip_hash="b" * 16,
                    user_agent="pytest",
                    email_hash=None,
                    meta={},
                    created_at=base + timedelta(seconds=i),
                )
            )
        db_session.commit()

        r = client.get("/me/audit-log", headers=_bearer(access))
        assert r.status_code == 200
        events = r.json()["events"]
        timestamps = [datetime.fromisoformat(e["created_at"].replace("Z", "+00:00")) for e in events if e.get("created_at")]
        # Ordering must be DESC.
        for prev, nxt in zip(timestamps, timestamps[1:]):
            assert prev >= nxt, (
                f"events must be ordered DESC by created_at: {prev} should be >= {nxt}"
            )


# ── TestAuditLogIpHmac (1 test) ───────────────────────────────────────────


class TestAuditLogIpHmac:
    # spec test #33
    def test_ip_hash_is_16_hex_never_raw_ip(self, client_pg_ready, db_session):
        from sqlalchemy import select

        from server.db.models import User
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "iphmac@example.com")
        user = db_session.execute(
            select(User).where(User.email == "iphmac@example.com")
        ).scalar_one()
        _seed_auth_event(db_session, user.id, "custom.iphmac")

        r = client.get("/me/audit-log", headers=_bearer(access))
        assert r.status_code == 200
        events = r.json()["events"]
        assert events, "expected at least 1 event"
        for ev in events:
            ip_hash = ev.get("ip_hash")
            if ip_hash is None:
                continue  # may be NULL legitimately (anonymized)
            assert len(ip_hash) == 16, (
                f"ip_hash must be 16-hex truncated HMAC, got len={len(ip_hash)}: {ip_hash!r}"
            )
            assert all(c in "0123456789abcdef" for c in ip_hash.lower()), (
                f"ip_hash must be hex, got: {ip_hash!r}"
            )
            # Never a raw IP-looking value.
            assert "." not in ip_hash and ":" not in ip_hash, (
                f"ip_hash looks like a raw IP: {ip_hash!r}"
            )
            # Common "ip" / "last_login_ip" raw-IP keys MUST NOT be present.
            assert "ip" not in ev or ev.get("ip") is None
            assert "last_login_ip" not in ev or ev.get("last_login_ip") is None


# ── TestAuditLogMetaCap (1 test) ──────────────────────────────────────────


class TestAuditLogMetaCap:
    # spec test #37 (insertion side : > 4KB → truncate + flag)
    def test_meta_over_4kb_is_truncated_and_flagged(self, client_pg_ready, db_session):
        """Insert event with meta > 4KB via `audit_event` (V2.3) — expect row
        stored with `meta.truncated == True` + content tronqué (≤ 4096 bytes
        once serialized).

        spec MED — Cap meta size 4KB côté insertion (anti DB blow-up).
        """
        import json

        from sqlalchemy import select

        from server.db.models import AuthEvent, User
        from server.routers.me import router  # noqa: F401
        from server.security.audit import audit_event

        client = client_pg_ready
        access = _register_and_login(client, "metacap@example.com")
        user = db_session.execute(
            select(User).where(User.email == "metacap@example.com")
        ).scalar_one()

        big_meta = {"k": "x" * 5000}  # > 4KB once serialized
        audit_event(
            db_session,
            "custom.bigmeta",
            user_id=user.id,
            meta=big_meta,
        )
        db_session.commit()

        row = db_session.execute(
            select(AuthEvent).where(AuthEvent.event_type == "custom.bigmeta")
        ).scalar_one()
        meta_dumped = json.dumps(row.meta).encode("utf-8")
        assert len(meta_dumped) <= 4096, (
            f"meta should be capped at 4KB, got {len(meta_dumped)} bytes"
        )
        assert row.meta.get("truncated") is True, (
            f"truncated meta must carry meta.truncated=true, got: {row.meta}"
        )

        # Sanity : also reachable through /me/audit-log.
        r = client.get("/me/audit-log", headers=_bearer(access))
        assert r.status_code == 200
        events = r.json()["events"]
        big = [e for e in events if e.get("event_type") == "custom.bigmeta"]
        assert big, "custom.bigmeta event missing in /me/audit-log"
        assert big[0]["meta"].get("truncated") is True


# ── TestAuditLogRateLimit (1 test) ────────────────────────────────────────


class TestAuditLogRateLimit:
    # spec test #36 (60/h/user, 61e → 429)
    @pytest.mark.slow
    def test_rate_limit_60_per_hour_then_429(self, client_pg_ready):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "rlaudit@example.com")

        statuses = []
        for _ in range(61):
            r = client.get("/me/audit-log", headers=_bearer(access))
            statuses.append(r.status_code)

        ok_count = sum(1 for s in statuses[:60] if s == 200)
        assert ok_count == 60, f"first 60 must 200, got statuses[:60]={statuses[:60]}"
        assert statuses[60] == 429, (
            f"61st request must 429, got {statuses[60]} (full sequence: {statuses})"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register_and_login` (function) — lines 30-39
- `_bearer` (function) — lines 42-43
- `_seed_auth_event` (function) — lines 46-60
- `TestAuditLogScope` (class) — lines 66-116
- `TestAuditLogAdminFilter` (class) — lines 122-176
- `TestAuditLogPagination` (class) — lines 182-252
- `TestAuditLogIpHmac` (class) — lines 258-293
- `TestAuditLogMetaCap` (class) — lines 299-348
- `TestAuditLogRateLimit` (class) — lines 354-372

### Imports
- `datetime`
- `pytest`

### Exports
- `_register_and_login`
- `_bearer`
- `_seed_auth_event`
- `TestAuditLogScope`
- `TestAuditLogAdminFilter`
- `TestAuditLogPagination`
- `TestAuditLogIpHmac`
- `TestAuditLogMetaCap`
- `TestAuditLogRateLimit`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-28-phase3-rgpd-endpoints]] — classes: `TestAuditLogScope`, `TestAuditLogAdminFilter`, `TestAuditLogPagination`, `TestAuditLogIpHmac`, `TestAuditLogMetaCap`
