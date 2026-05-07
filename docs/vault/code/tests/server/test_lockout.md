---
type: code-source
language: python
file_path: tests/server/test_lockout.py
git_blob: e9050fc3725de7665baaec24df9f31831ad34f81
last_synced: '2026-05-06T08:02:35Z'
loc: 517
annotations: []
imports:
- time
- concurrent.futures
- pytest
exports:
- _register
- TestSoftBackoffGrowth
- TestAtomicCounter
- TestNoHardLockAuto
- TestRaceGuard
- TestCleanupStaleCounts
- TestAntiEnum
- TestRefreshLockout
- TestResetUnlocks
- TestOauthCallbackNoIncrement
tags:
- code
- python
---

# tests/server/test_lockout.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_lockout.py`](../../../tests/server/test_lockout.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.1 — Soft backoff exponentiel + atomic counter + admin-lockout enforcement.

Tests RED-first contre `server.security.lockout` (à créer). Pas de hard lockout
automatique (DoS risk). Soft delay 1s..60s exponentiel server-side. Admin lock
manuel uniquement.

Spec: docs/vault/specs/2026-04-26-v2.3.3.1-rate-limit-lockout.md
  §Soft backoff exponentiel, §Login handler intégration, §register_successful_login race-guard,
  §cleanup_stale_failed_login_counts, §/auth/refresh admin-lockout, §OAuth callback no-increment.
Tests d'acceptation : #25, #26, #27, #28, #29, #30, #31, #32, #33, #34.
"""
from __future__ import annotations

import time
from concurrent.futures import ThreadPoolExecutor

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


def _register(client, email="lockout@example.com", password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


class TestSoftBackoffGrowth:
    def test_backoff_exponential_growth_intercepted_via_sleep_monkeypatch(
        self, monkeypatch, db_session, default_user_db
    ):
        """given register_failed_login appelé 7 fois, when on capture les valeurs sleep, then la séquence est ~ [1, 2, 4, 8, 16, 32, 60] (cap à 60).

        spec §Soft backoff exponentiel — 1s, 2s, 4s, 8s, 16s, 32s, 60s, 60s, ...
        spec test #25.
        """
        import server.security.lockout as _lockout

        captured: list[float] = []

        def _fake_sleep(secs):
            captured.append(secs)

        monkeypatch.setattr(_lockout.time, "sleep", _fake_sleep)

        for _ in range(7):
            _lockout.register_failed_login(db_session, default_user_db)

        # On attend sleeps croissants jusqu'au cap 60.
        assert len(captured) == 7, f"expected 7 sleep calls, got {len(captured)}: {captured}"
        # 1s, 2s, 4s, 8s, 16s, 32s, 60s (cap)
        expected = [1, 2, 4, 8, 16, 32, 60]
        assert captured == expected, (
            f"expected exponential sequence {expected}, got {captured}"
        )


class TestAtomicCounter:
    def test_counter_atomic_under_concurrency_no_lost_update(
        self, schema_ready, default_user_db, engine
    ):
        """given 20 register_failed_login parallèles via 2 connexions distinctes, when joined, then user.failed_login_count == 20 (pas 1-19 = lost-update).

        spec §UPDATE atomique avec RETURNING (pentester HIGH #3 fix).
        spec test #26.
        """
        import server.security.lockout as _lockout
        from sqlalchemy.orm import sessionmaker

        SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)
        user_id = default_user_db.id

        # Pas de sleep réel pour ce test — on monkeypatch time.sleep
        original_sleep = _lockout.time.sleep
        _lockout.time.sleep = lambda *_a, **_k: None
        try:

            def _do_one_fail():
                from sqlalchemy import select

                from server.db.models import User

                sess = SessionLocal()
                try:
                    u = sess.execute(select(User).where(User.id == user_id)).scalar_one()
                    _lockout.register_failed_login(sess, u)
                finally:
                    sess.close()

            with ThreadPoolExecutor(max_workers=8) as pool:
                futs = [pool.submit(_do_one_fail) for _ in range(20)]
                for f in futs:
                    f.result()

            from sqlalchemy import select

            from server.db.models import User

            sess = SessionLocal()
            try:
                u = sess.execute(select(User).where(User.id == user_id)).scalar_one()
                assert u.failed_login_count == 20, (
                    f"expected atomic counter == 20, got {u.failed_login_count} (lost-update)"
                )
            finally:
                sess.close()
        finally:
            _lockout.time.sleep = original_sleep


class TestNoHardLockAuto:
    def test_50_failed_logins_never_set_locked_until(
        self, monkeypatch, db_session, default_user_db
    ):
        """given 50 register_failed_login, when inspecté, then user.locked_until reste None (pas de hard lock automatique).

        spec §Trade-off explicite — l'attaquant ne peut PAS lock un user (objectif).
        spec test #27.
        """
        import server.security.lockout as _lockout

        monkeypatch.setattr(_lockout.time, "sleep", lambda *_a, **_k: None)

        for _ in range(50):
            _lockout.register_failed_login(db_session, default_user_db)

        from sqlalchemy import select

        from server.db.models import User

        db_session.expire_all()
        u = db_session.execute(select(User).where(User.id == default_user_db.id)).scalar_one()
        assert u.locked_until is None, (
            f"failed_login_count must NEVER auto-set locked_until; got {u.locked_until!r}"
        )
        assert u.failed_login_count == 50


class TestRaceGuard:
    def test_register_successful_login_preserves_admin_lock(
        self, monkeypatch, db_session, default_user_db
    ):
        """given user admin-locked manuel (locked_until > now), when register_successful_login appelé, then locked_until inchangé (race guard pentester MED M2).

        spec §register_successful_login race-guard sur admin lock.
        spec test #28.
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import select, update

        import server.security.lockout as _lockout
        from server.db.models import User

        # Pose un lock admin manuel (locked_until = now + 1h)
        future = datetime.now(timezone.utc) + timedelta(hours=1)
        db_session.execute(
            update(User).where(User.id == default_user_db.id).values(locked_until=future, failed_login_count=3)
        )
        db_session.commit()

        u = db_session.execute(select(User).where(User.id == default_user_db.id)).scalar_one()
        _lockout.register_successful_login(db_session, u)

        db_session.expire_all()
        u_after = db_session.execute(select(User).where(User.id == default_user_db.id)).scalar_one()
        assert u_after.locked_until is not None, (
            "register_successful_login MUST NOT clear an active admin lock"
        )


class TestCleanupStaleCounts:
    def test_cleanup_resets_stale_users_only(self, db_session):
        """given 5 users avec last_failed_login_at > 24h ET 2 users récents, when cleanup_stale_failed_login_counts, then les 5 stale resettés à 0, les 2 récents inchangés.

        spec §cleanup_stale_failed_login_counts — auto-reset si pas de fail dans les 24h.
        spec test #29.
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import insert, select

        import server.security.lockout as _lockout
        from server.db.models import User

        old = datetime.now(timezone.utc) - timedelta(hours=48)
        recent = datetime.now(timezone.utc) - timedelta(hours=1)

        for i in range(5):
            db_session.execute(
                insert(User).values(
                    email=f"stale-{i}@example.com",
                    password_hash="$argon2id$v=19$m=46080,t=2,p=1$STALEDUMMY",
                    failed_login_count=4,
                    last_failed_login_at=old,
                    is_active=True,
                )
            )
        for i in range(2):
            db_session.execute(
                insert(User).values(
                    email=f"recent-{i}@example.com",
                    password_hash="$argon2id$v=19$m=46080,t=2,p=1$STALEDUMMY",
                    failed_login_count=4,
                    last_failed_login_at=recent,
                    is_active=True,
                )
            )
        db_session.commit()

        rowcount = _lockout.cleanup_stale_failed_login_counts(db_session)
        assert rowcount == 5, f"expected 5 stale rows reset, got {rowcount}"

        stale = db_session.execute(
            select(User).where(User.email.like("stale-%@example.com"))
        ).scalars().all()
        assert all(u.failed_login_count == 0 for u in stale), "all stale users must be reset to 0"

        rec = db_session.execute(
            select(User).where(User.email.like("recent-%@example.com"))
        ).scalars().all()
        assert all(u.failed_login_count == 4 for u in rec), "recent users must NOT be reset"


class TestAntiEnum:
    def test_login_admin_locked_returns_401_not_423(self, client_pg_ready, db_session):
        """given user admin-locked (locked_until > now), when POST /auth/login avec bon password, then 401 invalid_credentials (PAS 423, anti-enum).

        spec §Anti-énumération — réponses identiques (login_locked_attempt → 401 not 423).
        spec test #30.
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import select, update

        from server.db.models import AuthEvent, User

        client = client_pg_ready
        _register(client, email="anti-enum@example.com", password="goodpassword12345")

        u = db_session.execute(
            select(User).where(User.email == "anti-enum@example.com")
        ).scalar_one()
        future = datetime.now(timezone.utc) + timedelta(hours=1)
        db_session.execute(update(User).where(User.id == u.id).values(locked_until=future))
        db_session.commit()

        r = client.post(
            "/auth/login",
            json={"email": "anti-enum@example.com", "password": "goodpassword12345"},
        )
        assert r.status_code == 401, (
            f"admin-locked login MUST return 401 (anti-enum), got {r.status_code}: {r.text}"
        )
        assert r.json().get("detail") == "invalid_credentials"

        # Audit event login_locked_attempt
        events = db_session.execute(
            select(AuthEvent).where(AuthEvent.event_type == "login_locked_attempt")
        ).scalars().all()
        assert len(events) >= 1, (
            "expected ≥1 login_locked_attempt audit event"
        )


class TestRefreshLockout:
    def test_refresh_admin_locked_returns_423_and_revokes_only_used_token(
        self, client_pg_ready, db_session
    ):
        """given user dual-device admin-locked, when POST /auth/refresh avec token1, then 423 + token1 revoked + token2 INTACT (pentester #7 — pas all-revoke).

        spec §/auth/refresh admin-lockout — revoke uniquement le refresh_token utilisé.
        spec test #31.
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import select, update

        from server.db.models import RefreshToken, User

        client = client_pg_ready
        # Register + login fois 2 (= 2 devices = 2 refresh tokens)
        _register(client, email="dual-device@example.com", password="goodpassword12345")
        l1 = client.post(
            "/auth/login",
            json={"email": "dual-device@example.com", "password": "goodpassword12345"},
        )
        l2 = client.post(
            "/auth/login",
            json={"email": "dual-device@example.com", "password": "goodpassword12345"},
        )
        assert l1.status_code == 200 and l2.status_code == 200
        refresh1 = l1.json()["refresh_token"]

        # Pose un admin lock
        u = db_session.execute(
            select(User).where(User.email == "dual-device@example.com")
        ).scalar_one()
        future = datetime.now(timezone.utc) + timedelta(hours=1)
        db_session.execute(update(User).where(User.id == u.id).values(locked_until=future))
        db_session.commit()

        r = client.post("/auth/refresh", json={"refresh_token": refresh1})
        assert r.status_code == 423, (
            f"refresh on admin-locked user MUST return 423, got {r.status_code}: {r.text}"
        )

        # Token1 revoked, token2 NOT revoked.
        all_tokens = db_session.execute(
            select(RefreshToken).where(RefreshToken.user_id == u.id)
        ).scalars().all()
        revoked = [t for t in all_tokens if t.revoked_at is not None]
        not_revoked = [t for t in all_tokens if t.revoked_at is None]
        assert len(revoked) == 1, (
            f"only the USED refresh token must be revoked, got {len(revoked)} revoked / {len(all_tokens)} total"
        )
        assert len(not_revoked) >= 1, (
            "the OTHER device's refresh token MUST stay alive (not all-revoke)"
        )


class TestResetUnlocks:
    def test_password_reset_confirm_unlocks_admin_locked_user(
        self, client_pg_ready, db_session
    ):
        """given user admin-locked, when password reset confirm flow V2.3.1, then locked_until=NULL + audit password_reset_unlocked_user.

        spec §Reset password unlock le user (décision documentée).
        spec test #32.
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import select, update

        from server.db.models import AuthEvent, User, VerificationToken
        from server.security.email_outbound import _outbound_link_cache

        client = client_pg_ready
        _register(client, email="reset-unlock@example.com", password="goodpassword12345")

        u = db_session.execute(
            select(User).where(User.email == "reset-unlock@example.com")
        ).scalar_one()
        future = datetime.now(timezone.utc) + timedelta(hours=1)
        db_session.execute(
            update(User).where(User.id == u.id).values(locked_until=future, failed_login_count=5)
        )
        db_session.commit()

        client.post(
            "/auth/password/reset/request",
            json={"email": "reset-unlock@example.com"},
        )
        # Récupère le token brut depuis le cache outbound (pattern V2.3.1)
        cache_pairs = dict(_outbound_link_cache.items())
        # Trouve le token raw correspondant à un VerificationToken pour notre user
        rows = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == u.id,
                VerificationToken.purpose == "password_reset",
            )
        ).scalars().all()
        assert rows, "expected a verification_tokens row for reset"
        token_raw = None
        for vt in rows:
            if vt.token_hash in cache_pairs:
                token_raw = cache_pairs[vt.token_hash]
                break
        assert token_raw, "expected raw token in outbound cache"

        r = client.post(
            "/auth/password/reset/confirm",
            json={"token": token_raw, "new_password": "newGOODpassword12345"},
        )
        assert r.status_code == 200, f"reset confirm failed: {r.text}"

        db_session.expire_all()
        u_after = db_session.execute(
            select(User).where(User.email == "reset-unlock@example.com")
        ).scalar_one()
        assert u_after.locked_until is None, (
            "password reset MUST clear locked_until (proof of ownership)"
        )

        events = db_session.execute(
            select(AuthEvent).where(
                AuthEvent.event_type == "password_reset_unlocked_user",
                AuthEvent.user_id == u.id,
            )
        ).scalars().all()
        assert len(events) >= 1, (
            "expected ≥1 password_reset_unlocked_user audit event"
        )


class TestOauthCallbackNoIncrement:
    def test_oauth_callback_failures_do_not_touch_failed_login_count(
        self, client_pg_ready, db_session
    ):
        """given 10 OAuth callback fails (id_token forgé), when inspecté, then user.failed_login_count == 0 ET le 6e callback fail est 429 (rate-limit serré 5/min sur fail, pentester #11).

        Combine deux assertions :
        1. Counter never incremented by OAuth fails (sinon attaquant peut lock un user dual-auth).
        2. Rate-limit serré sur callback fail empêche brute-force d'id_tokens forgés.
        spec §OAuth callback failures (pentester #11) + §Limites endpoint OAuth callback fail 5/min.
        spec test #33 + #10.
        """
        # Sanity : le rate-limit module doit exister, sinon le test est trivialement vrai.
        from server.security import rate_limit  # noqa: F401

        from sqlalchemy import select

        from server.db.models import User

        client = client_pg_ready
        _register(client, email="oauth-noinc@example.com", password="goodpassword12345")

        statuses = []
        for _ in range(6):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code-xxx", "state": "fake-state-yyy"},
            )
            statuses.append(r.status_code)

        # 6e fail DOIT être 429 (rate-limit serré).
        assert statuses[-1] == 429, (
            f"6th OAuth callback fail MUST be 429 (rate-limit serré 5/min on fail); got statuses={statuses}"
        )

        db_session.expire_all()
        u = db_session.execute(
            select(User).where(User.email == "oauth-noinc@example.com")
        ).scalar_one()
        assert u.failed_login_count == 0, (
            f"OAuth callback failures MUST NOT increment failed_login_count; got {u.failed_login_count}"
        )

    def test_register_successful_login_resets_counter_when_no_admin_lock(
        self, monkeypatch, db_session, default_user_db
    ):
        """given user avec failed_login_count=5 ET locked_until=NULL, when register_successful_login appelé, then failed_login_count reset à 0.

        spec §Login handler — register_successful_login reset counter + last_login_at.
        Tests d'acceptation #25 (volet succès).
        """
        from sqlalchemy import select, update

        import server.security.lockout as _lockout
        from server.db.models import User

        monkeypatch.setattr(_lockout.time, "sleep", lambda *_a, **_k: None)

        db_session.execute(
            update(User).where(User.id == default_user_db.id).values(failed_login_count=5)
        )
        db_session.commit()

        u = db_session.execute(select(User).where(User.id == default_user_db.id)).scalar_one()
        _lockout.register_successful_login(db_session, u)

        db_session.expire_all()
        u_after = db_session.execute(select(User).where(User.id == default_user_db.id)).scalar_one()
        assert u_after.failed_login_count == 0, (
            f"successful login MUST reset failed_login_count, got {u_after.failed_login_count}"
        )

    def test_is_user_locked_only_true_for_admin_set_locked_until(
        self, db_session, default_user_db
    ):
        """given user avec failed_login_count=50 mais locked_until=NULL, when is_user_locked(user), then False (jamais True via failed_login_count seul).

        spec §is_user_locked — True UNIQUEMENT si admin a posé un locked_until manuel.
        Tests d'acceptation #27 (volet helper).
        """
        from datetime import datetime, timedelta, timezone

        from sqlalchemy import select, update

        import server.security.lockout as _lockout
        from server.db.models import User

        # Cas 1 : counter=50 mais locked_until=NULL → is_user_locked=False
        db_session.execute(
            update(User)
            .where(User.id == default_user_db.id)
            .values(failed_login_count=50, locked_until=None)
        )
        db_session.commit()
        db_session.expire_all()
        u = db_session.execute(select(User).where(User.id == default_user_db.id)).scalar_one()
        assert _lockout.is_user_locked(u) is False, (
            "is_user_locked MUST be False when only failed_login_count is high (no admin lock)"
        )

        # Cas 2 : locked_until > now → is_user_locked=True
        future = datetime.now(timezone.utc) + timedelta(hours=1)
        db_session.execute(update(User).where(User.id == u.id).values(locked_until=future))
        db_session.commit()
        db_session.expire_all()
        u2 = db_session.execute(select(User).where(User.id == default_user_db.id)).scalar_one()
        assert _lockout.is_user_locked(u2) is True, (
            "is_user_locked MUST be True when locked_until > now()"
        )

        # Cas 3 : locked_until in the past → is_user_locked=False (lock expiré)
        past = datetime.now(timezone.utc) - timedelta(hours=1)
        db_session.execute(update(User).where(User.id == u.id).values(locked_until=past))
        db_session.commit()
        db_session.expire_all()
        u3 = db_session.execute(select(User).where(User.id == default_user_db.id)).scalar_one()
        assert _lockout.is_user_locked(u3) is False, (
            "is_user_locked MUST be False when locked_until is in the past"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register` (function) — lines 23-28
- `TestSoftBackoffGrowth` (class) — lines 31-58
- `TestAtomicCounter` (class) — lines 61-111
- `TestNoHardLockAuto` (class) — lines 114-139
- `TestRaceGuard` (class) — lines 142-172
- `TestCleanupStaleCounts` (class) — lines 175-225
- `TestAntiEnum` (class) — lines 228-266
- `TestRefreshLockout` (class) — lines 269-322
- `TestResetUnlocks` (class) — lines 325-396
- `TestOauthCallbackNoIncrement` (class) — lines 399-517

### Imports
- `time`
- `concurrent.futures`
- `pytest`

### Exports
- `_register`
- `TestSoftBackoffGrowth`
- `TestAtomicCounter`
- `TestNoHardLockAuto`
- `TestRaceGuard`
- `TestCleanupStaleCounts`
- `TestAntiEnum`
- `TestRefreshLockout`
- `TestResetUnlocks`
- `TestOauthCallbackNoIncrement`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — classes: `TestSoftBackoffGrowth`, `TestAtomicCounter`, `TestNoHardLockAuto`, `TestRaceGuard`, `TestCleanupStaleCounts`, `TestAntiEnum`, `TestRefreshLockout`, `TestResetUnlocks`, `TestOauthCallbackNoIncrement`
