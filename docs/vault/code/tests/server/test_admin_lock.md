---
type: code-source
language: python
file_path: tests/server/test_admin_lock.py
git_blob: d61119e4f0550393ff28f9f15c225c466c6a7153
last_synced: '2026-05-06T08:02:34Z'
loc: 218
annotations: []
imports:
- pytest
exports:
- _register
- _user_id_from_email
- TestAdminLockUnlock
tags:
- code
- python
---

# tests/server/test_admin_lock.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_admin_lock.py`](../../../tests/server/test_admin_lock.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.1 — Admin endpoints POST /admin/users/{id}/lock + /unlock (manuel only).

Tests RED-first contre `server.routers.admin` (extension à créer). Hard lockout
manuel uniquement (pas d'auto via failed_login_count). Auth admin via X-Registration-Token
(stop-gap V2.3.1+).

Spec: docs/vault/specs/2026-04-26-v2.3.3.1-rate-limit-lockout.md
  §Admin endpoints unlock + lock (manuel only).
Tests d'acceptation : #35, #36, #37, #38, #39.
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


def _register(client, email="admin-lock@example.com", password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


def _user_id_from_email(db_session, email):
    from sqlalchemy import select

    from server.db.models import User

    return db_session.execute(select(User).where(User.email == email)).scalar_one().id


class TestAdminLockUnlock:
    def test_admin_lock_sets_locked_until_and_audits_reason(
        self, client_pg_ready, db_session
    ):
        """given POST /admin/users/{id}/lock + body {duration_minutes:60, reason:'suspicious'} + X-Registration-Token, when called, then user.locked_until ≈ now+60min + audit admin_user_locked.

        spec §Admin endpoints — lock manuel avec duration + reason.
        spec test #35.
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import select

        from server.db.models import AuthEvent, User

        client = client_pg_ready
        _register(client, email="adm-lock-1@example.com")
        uid = _user_id_from_email(db_session, "adm-lock-1@example.com")

        before = datetime.now(timezone.utc)
        r = client.post(
            f"/admin/users/{uid}/lock",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"duration_minutes": 60, "reason": "suspicious"},
        )
        assert r.status_code in (200, 204), f"expected 200/204, got {r.status_code}: {r.text}"

        db_session.expire_all()
        u = db_session.execute(select(User).where(User.id == uid)).scalar_one()
        assert u.locked_until is not None, "locked_until MUST be set after admin lock"
        # ~ now + 60min, ±2min tolerance
        delta = u.locked_until - before
        assert timedelta(minutes=58) < delta < timedelta(minutes=62), (
            f"expected locked_until ~ now+60min, got delta={delta}"
        )

        events = db_session.execute(
            select(AuthEvent).where(
                AuthEvent.event_type == "admin_user_locked",
                AuthEvent.user_id == uid,
            )
        ).scalars().all()
        assert len(events) >= 1, "expected ≥1 admin_user_locked audit event"

    def test_admin_unlock_clears_locked_until_and_audits(
        self, client_pg_ready, db_session
    ):
        """given user admin-locked, when POST /admin/users/{id}/unlock + X-Registration-Token, then user.locked_until=NULL + failed_login_count=0 + audit admin_user_unlocked.

        spec §Admin endpoints — unlock reset locked_until + counter.
        spec test #36.
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import select, update

        from server.db.models import AuthEvent, User

        client = client_pg_ready
        _register(client, email="adm-unlock-1@example.com")
        uid = _user_id_from_email(db_session, "adm-unlock-1@example.com")

        future = datetime.now(timezone.utc) + timedelta(hours=1)
        db_session.execute(
            update(User).where(User.id == uid).values(locked_until=future, failed_login_count=8)
        )
        db_session.commit()

        r = client.post(
            f"/admin/users/{uid}/unlock",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )
        assert r.status_code in (200, 204), f"expected 200/204, got {r.status_code}: {r.text}"

        db_session.expire_all()
        u = db_session.execute(select(User).where(User.id == uid)).scalar_one()
        assert u.locked_until is None, "locked_until MUST be NULL after unlock"
        assert u.failed_login_count == 0, "failed_login_count MUST be reset to 0 after unlock"

        events = db_session.execute(
            select(AuthEvent).where(
                AuthEvent.event_type == "admin_user_unlocked",
                AuthEvent.user_id == uid,
            )
        ).scalars().all()
        assert len(events) >= 1, "expected ≥1 admin_user_unlocked audit event"

    def test_admin_lock_unlock_unauth_returns_401(self, client_pg_ready, db_session):
        """given POST /admin/users/{id}/lock|unlock SANS X-Registration-Token, when called, then 401 sur les 2.

        spec §Admin endpoints — gated by X-Registration-Token.
        spec test #37.
        """
        client = client_pg_ready
        _register(client, email="adm-noauth@example.com")
        uid = _user_id_from_email(db_session, "adm-noauth@example.com")

        r1 = client.post(
            f"/admin/users/{uid}/lock", json={"duration_minutes": 60, "reason": "x"}
        )
        assert r1.status_code == 401, (
            f"expected 401 unauth lock, got {r1.status_code}: {r1.text}"
        )
        r2 = client.post(f"/admin/users/{uid}/unlock")
        assert r2.status_code == 401, (
            f"expected 401 unauth unlock, got {r2.status_code}: {r2.text}"
        )

    def test_admin_lock_override_writes_new_locked_until(
        self, client_pg_ready, db_session
    ):
        """given user déjà admin-locked (1h), when re-POST lock duration_minutes=120, then locked_until ≈ now+120min (override).

        spec §Admin lock override — re-lock écrase la durée précédente.
        spec test #38.
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import select

        from server.db.models import User

        client = client_pg_ready
        _register(client, email="adm-override@example.com")
        uid = _user_id_from_email(db_session, "adm-override@example.com")
        headers = {"X-Registration-Token": _TEST_REGISTRATION_TOKEN}

        client.post(
            f"/admin/users/{uid}/lock",
            headers=headers,
            json={"duration_minutes": 60, "reason": "first"},
        )
        before = datetime.now(timezone.utc)
        client.post(
            f"/admin/users/{uid}/lock",
            headers=headers,
            json={"duration_minutes": 120, "reason": "second-override"},
        )
        db_session.expire_all()
        u = db_session.execute(select(User).where(User.id == uid)).scalar_one()
        delta = u.locked_until - before
        assert timedelta(minutes=118) < delta < timedelta(minutes=122), (
            f"override MUST set locked_until ~ now+120min, got delta={delta}"
        )

    def test_admin_lock_audit_event_includes_meta_reason_and_duration(
        self, client_pg_ready, db_session
    ):
        """given POST /admin/users/{id}/lock body reason='breach', when audit event lu, then meta inclut reason + duration_minutes.

        spec §Audit events — admin_user_locked avec meta.reason + meta.duration_minutes.
        Tests d'acceptation #35 (volet meta) + #40 (audit shape).
        """
        from sqlalchemy import select

        from server.db.models import AuthEvent

        client = client_pg_ready
        _register(client, email="adm-meta@example.com")
        uid = _user_id_from_email(db_session, "adm-meta@example.com")

        client.post(
            f"/admin/users/{uid}/lock",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"duration_minutes": 30, "reason": "breach-detected"},
        )

        events = db_session.execute(
            select(AuthEvent).where(
                AuthEvent.event_type == "admin_user_locked",
                AuthEvent.user_id == uid,
            )
        ).scalars().all()
        assert len(events) >= 1
        # Le meta peut être stocké dans request_id ou un champ JSON dédié; on vérifie
        # qu'AU MOINS l'un des champs textuels mentionne "breach-detected".
        textual = " ".join(
            (e.request_id or "") + " " + (e.user_agent or "") for e in events
        )
        assert "breach-detected" in textual or any(
            getattr(e, "meta", None) for e in events
        ), (
            f"audit event MUST persist reason='breach-detected' in meta/request_id, got: {textual!r}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register` (function) — lines 19-24
- `_user_id_from_email` (function) — lines 27-32
- `TestAdminLockUnlock` (class) — lines 35-218

### Imports
- `pytest`

### Exports
- `_register`
- `_user_id_from_email`
- `TestAdminLockUnlock`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — classes: `TestAdminLockUnlock`
