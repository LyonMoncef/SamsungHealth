---
type: code-source
language: python
file_path: tests/server/test_password_reset_routes.py
git_blob: f8ced5e4b87500fe332993877b3d0919a65fabf0
last_synced: '2026-04-27T17:56:06Z'
loc: 519
annotations: []
imports:
- statistics
- time
- datetime
- pytest
exports:
- _register
- _capture_raw_token
- TestRequestPasswordReset
- TestRaceCondition
- TestConfirmPasswordReset
- TestSecuritySideEffects
- TestTransactionAtomicity
tags:
- code
- python
---

# tests/server/test_password_reset_routes.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_password_reset_routes.py`](../../../tests/server/test_password_reset_routes.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 — Password reset routes (POST /auth/password/reset/request + /confirm).

Tests RED-first contre `server.routers.auth`. 12 tests : request/confirm + side-effects sécu
(jitter timing, race condition, atomicité audit, blocklist password).

Spec: docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md
"""
from __future__ import annotations

import statistics
import time
from datetime import datetime, timedelta, timezone

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_v231_env(monkeypatch):
    monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", "http://localhost:8000")
    monkeypatch.setenv(
        "SAMSUNGHEALTH_EMAIL_HASH_SALT",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    )


def _register(client, email="reset@example.com", password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


def _capture_raw_token(db_session, email, purpose="password_reset"):
    """Helper: récupère le raw token depuis la cache outbound."""
    from sqlalchemy import select

    from server.db.models import User, VerificationToken
    from server.security.email_outbound import _outbound_link_cache

    user = db_session.execute(select(User).where(User.email == email)).scalar_one()
    row = db_session.execute(
        select(VerificationToken)
        .where(
            VerificationToken.user_id == user.id,
            VerificationToken.purpose == purpose,
            VerificationToken.consumed_at.is_(None),
        )
        .order_by(VerificationToken.issued_at.desc())
    ).scalar()
    assert row is not None, f"no active verification_token row for {email}/{purpose}"
    raw = _outbound_link_cache.get(row.token_hash)
    assert raw is not None, "raw token must be in outbound cache (60s TTL)"
    return raw, user, row


class TestRequestPasswordReset:
    def test_request_returns_202_for_known_user(self, client_pg_ready):
        """given a registered email, when POST /auth/password/reset/request, then 202 + status=pending.

        spec §Endpoints — POST /auth/password/reset/request → 202.
        """
        client = client_pg_ready
        _register(client, email="known-reset@example.com")

        r = client.post(
            "/auth/password/reset/request",
            json={"email": "known-reset@example.com"},
        )
        assert r.status_code == 202, f"expected 202, got {r.status_code}: {r.text}"
        assert r.json() == {"status": "pending"}

    def test_request_returns_202_for_unknown_email_anti_enum(self, client_pg_ready):
        """given an unknown email, when POST request, then 202 (anti-enum).

        spec §Anti-énumération — 202 toujours.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/password/reset/request",
            json={"email": "ghost-reset@example.com"},
        )
        assert r.status_code == 202
        assert r.json() == {"status": "pending"}

    def test_request_jitter_within_80_120ms(self, client_pg_ready, monkeypatch):
        """given known vs unknown email, when POST request 5x each, then median latency ≥ 80ms (jitter active).

        spec §Anti-énumération — Jitter random.uniform(0.080, 0.120) AVANT 202.
        spec §Test d'acceptation #7 — latence ~ même ±150ms.
        Note: V2.3.3.1 ajoute un cap composite 3/5min (IP, email) qui sinon
        couperait la fenêtre de mesure (3 OK + 429 → médiane faussée). On
        bumpe le cap via env-override le temps du test pour mesurer le jitter
        sur les 6 appels par email (1 warm-up + 5 mesures).
        """
        monkeypatch.setenv("SAMSUNGHEALTH_RL_EMAIL_COMPOSITE_CAP", "100/5minutes")
        client = client_pg_ready
        _register(client, email="jitter-reset@example.com")

        # Warm-up
        client.post("/auth/password/reset/request", json={"email": "jitter-reset@example.com"})
        client.post("/auth/password/reset/request", json={"email": "ghost-jit@example.com"})

        known_times = []
        unknown_times = []
        for _ in range(5):
            t0 = time.perf_counter()
            client.post(
                "/auth/password/reset/request",
                json={"email": "jitter-reset@example.com"},
            )
            known_times.append(time.perf_counter() - t0)

            t0 = time.perf_counter()
            client.post(
                "/auth/password/reset/request",
                json={"email": "ghost-jit@example.com"},
            )
            unknown_times.append(time.perf_counter() - t0)

        med_known = statistics.median(known_times)
        med_unknown = statistics.median(unknown_times)
        # Jitter min = 80ms → median ≥ 80ms.
        assert med_known >= 0.080, (
            f"jitter not active for known email: median={med_known*1000:.1f}ms (expect ≥ 80ms)"
        )
        assert med_unknown >= 0.080, (
            f"jitter not active for unknown email: median={med_unknown*1000:.1f}ms (expect ≥ 80ms)"
        )

    def test_request_re_request_revokes_old_token(self, client_pg_ready, db_session):
        """given two consecutive POST request for same user, when 2nd is sent, then 1st row consumed_at IS NOT NULL + only 1 active.

        spec §Idempotence vs spam — un seul token actif par (user, purpose), ancien révoqué.
        spec §Test d'acceptation #8.
        """
        from sqlalchemy import select

        from server.db.models import User, VerificationToken

        client = client_pg_ready
        _register(client, email="revoke-old@example.com")

        client.post("/auth/password/reset/request", json={"email": "revoke-old@example.com"})
        client.post("/auth/password/reset/request", json={"email": "revoke-old@example.com"})

        user = db_session.execute(
            select(User).where(User.email == "revoke-old@example.com")
        ).scalar_one()
        rows = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "password_reset",
            )
        ).scalars().all()
        assert len(rows) == 2, f"expected 2 rows, got {len(rows)}"
        active = [r for r in rows if r.consumed_at is None]
        consumed = [r for r in rows if r.consumed_at is not None]
        assert len(active) == 1, f"only 1 active token allowed, got {len(active)}"
        assert len(consumed) == 1, f"old token must be consumed (revoked), got {len(consumed)}"


class TestRaceCondition:
    def test_request_concurrent_inserts_only_one_active(self, schema_ready, pg_url):
        """given 2 concurrent INSERTs of verification_tokens for same (user, purpose), when run, then only 1 active row + 2nd raises IntegrityError on unique partial index.

        spec §Schéma — UNIQUE INDEX ... WHERE consumed_at IS NULL (race condition protection).
        spec §Test d'acceptation #9.
        """
        import concurrent.futures
        import hashlib
        import secrets

        from sqlalchemy import create_engine, select, text
        from sqlalchemy.orm import sessionmaker

        from server.db.models import User, VerificationToken

        engine = create_engine(pg_url, future=True)
        SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)

        with engine.begin() as conn:
            conn.execute(
                text(
                    "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                    "VALUES (gen_random_uuid(), :email, 'x', true, now())"
                ),
                {"email": "race@example.com"},
            )

        with SessionLocal() as s:
            user = s.execute(select(User).where(User.email == "race@example.com")).scalar_one()
            user_id = user.id

        def _insert():
            with SessionLocal() as sess:
                raw = secrets.token_urlsafe(32)
                hashed = hashlib.sha256(raw.encode()).hexdigest()
                now = datetime.now(timezone.utc)
                sess.add(
                    VerificationToken(
                        user_id=user_id,
                        token_hash=hashed,
                        purpose="password_reset",
                        issued_at=now,
                        expires_at=now + timedelta(hours=1),
                    )
                )
                try:
                    sess.commit()
                    return "ok"
                except Exception as exc:
                    sess.rollback()
                    return f"fail:{type(exc).__name__}"

        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:
            results = list(ex.map(lambda _: _insert(), range(2)))

        # Au moins un OK + au moins un fail (IntegrityError sur unique partial index).
        oks = [r for r in results if r == "ok"]
        fails = [r for r in results if r.startswith("fail")]
        assert len(oks) == 1, f"expected exactly 1 successful insert, got {oks}"
        assert len(fails) == 1, f"expected exactly 1 failed insert (race), got {fails}"

        engine.dispose()


class TestConfirmPasswordReset:
    def test_confirm_changes_password_hash(self, client_pg_ready, db_session):
        """given a fresh reset token, when POST confirm with new_password, then users.password_hash is changed.

        spec §Side-effects — Mettre à jour users.password_hash.
        """
        from sqlalchemy import select

        from server.db.models import User

        client = client_pg_ready
        _register(client, email="change-hash@example.com")
        client.post("/auth/password/reset/request", json={"email": "change-hash@example.com"})

        raw, _user, _row = _capture_raw_token(db_session, "change-hash@example.com")

        user_before = db_session.execute(
            select(User).where(User.email == "change-hash@example.com")
        ).scalar_one()
        old_hash = user_before.password_hash

        r = client.post(
            "/auth/password/reset/confirm",
            json={"token": raw, "new_password": "BrandNewSecurePassword2026!"},
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"

        db_session.expire_all()
        user_after = db_session.execute(
            select(User).where(User.email == "change-hash@example.com")
        ).scalar_one()
        assert user_after.password_hash != old_hash, "password_hash must change after reset"

    def test_confirm_updates_password_changed_at(self, client_pg_ready, db_session):
        """given a successful reset confirm, when DB row is read, then password_changed_at advanced.

        spec §Side-effects — password_changed_at = now() (lu par lockout V2.3.3).
        """
        from sqlalchemy import select

        from server.db.models import User

        client = client_pg_ready
        _register(client, email="changed-at@example.com")
        time.sleep(0.05)
        client.post("/auth/password/reset/request", json={"email": "changed-at@example.com"})
        raw, _, _ = _capture_raw_token(db_session, "changed-at@example.com")

        before = db_session.execute(
            select(User).where(User.email == "changed-at@example.com")
        ).scalar_one()
        old_pwd_changed = before.password_changed_at

        time.sleep(0.05)
        r = client.post(
            "/auth/password/reset/confirm",
            json={"token": raw, "new_password": "AnotherStrongPwd1234!"},
        )
        assert r.status_code == 200

        db_session.expire_all()
        after = db_session.execute(
            select(User).where(User.email == "changed-at@example.com")
        ).scalar_one()
        assert after.password_changed_at > old_pwd_changed, (
            f"password_changed_at must advance: before={old_pwd_changed}, after={after.password_changed_at}"
        )

    def test_confirm_revokes_all_refresh_tokens(self, client_pg_ready, db_session):
        """given a logged-in user (refresh token issued), when reset is confirmed, then all RefreshToken rows have revoked_at IS NOT NULL.

        spec §Side-effects — Révoquer tous les refresh_tokens actifs.
        spec §Test d'acceptation #10.
        """
        from sqlalchemy import select

        from server.db.models import RefreshToken, User

        client = client_pg_ready
        _register(client, email="revoke-refresh@example.com")
        login = client.post(
            "/auth/login",
            json={"email": "revoke-refresh@example.com", "password": "longpassword12345"},
        )
        assert login.status_code == 200, f"login precondition failed: {login.text}"

        user = db_session.execute(
            select(User).where(User.email == "revoke-refresh@example.com")
        ).scalar_one()

        # Verify there's an active refresh token before reset.
        active_before = db_session.execute(
            select(RefreshToken).where(
                RefreshToken.user_id == user.id, RefreshToken.revoked_at.is_(None)
            )
        ).scalars().all()
        assert len(active_before) >= 1, "should have an active refresh after login"

        client.post("/auth/password/reset/request", json={"email": "revoke-refresh@example.com"})
        raw, _, _ = _capture_raw_token(db_session, "revoke-refresh@example.com")

        r = client.post(
            "/auth/password/reset/confirm",
            json={"token": raw, "new_password": "PostResetPasswordSafe9!"},
        )
        assert r.status_code == 200, f"reset confirm failed: {r.text}"

        db_session.expire_all()
        active_after = db_session.execute(
            select(RefreshToken).where(
                RefreshToken.user_id == user.id, RefreshToken.revoked_at.is_(None)
            )
        ).scalars().all()
        assert len(active_after) == 0, (
            f"all refresh tokens must be revoked after reset, still active: {len(active_after)}"
        )

    def test_confirm_consumes_token_replay_returns_400(self, client_pg_ready, db_session):
        """given a successfully confirmed reset token, when re-POST confirm with same token, then 400 invalid_or_expired.

        spec §Single-use — re-confirm = 400.
        """
        client = client_pg_ready
        _register(client, email="reset-replay@example.com")
        client.post("/auth/password/reset/request", json={"email": "reset-replay@example.com"})
        raw, _, _ = _capture_raw_token(db_session, "reset-replay@example.com")

        first = client.post(
            "/auth/password/reset/confirm",
            json={"token": raw, "new_password": "FirstResetPwdSecure1234!"},
        )
        assert first.status_code == 200

        second = client.post(
            "/auth/password/reset/confirm",
            json={"token": raw, "new_password": "AnotherTrySecure567!"},
        )
        assert second.status_code == 400
        assert second.json() == {"detail": "invalid_or_expired"}

    def test_confirm_rejects_weak_password(self, client_pg_ready, db_session):
        """given a fresh reset token, when POST confirm with new_password 'short', then 400 weak_password + token NOT consumed + password_hash unchanged.

        spec §Test d'acceptation #12 — weak_password (min 12 chars), token NON consommé.
        """
        from sqlalchemy import select

        from server.db.models import User, VerificationToken

        client = client_pg_ready
        _register(client, email="weak-pwd@example.com")
        client.post("/auth/password/reset/request", json={"email": "weak-pwd@example.com"})
        raw, user, _row = _capture_raw_token(db_session, "weak-pwd@example.com")

        user_before = db_session.execute(
            select(User).where(User.email == "weak-pwd@example.com")
        ).scalar_one()
        old_hash = user_before.password_hash

        r = client.post(
            "/auth/password/reset/confirm",
            json={"token": raw, "new_password": "short"},
        )
        assert r.status_code == 400, f"expected 400, got {r.status_code}: {r.text}"
        assert r.json() == {"detail": "weak_password"}

        db_session.expire_all()
        user_after = db_session.execute(
            select(User).where(User.email == "weak-pwd@example.com")
        ).scalar_one()
        assert user_after.password_hash == old_hash, "password_hash must NOT change on weak"

        # Token NOT consumed → user can retry.
        rows = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "password_reset",
                VerificationToken.consumed_at.is_(None),
            )
        ).scalars().all()
        assert len(rows) == 1, "active token must remain (not consumed) after weak rejection"

    def test_confirm_rejects_blocklist_password(self, client_pg_ready, db_session):
        """given a fresh reset token, when POST confirm with new_password='password1234', then 400 weak_password + token NOT consumed.

        spec §Validation password — blocklist top-100 leaked passwords.
        spec §Test d'acceptation #13.
        """
        from sqlalchemy import select

        from server.db.models import User, VerificationToken

        client = client_pg_ready
        _register(client, email="blocklist-pwd@example.com")
        client.post("/auth/password/reset/request", json={"email": "blocklist-pwd@example.com"})
        raw, user, _ = _capture_raw_token(db_session, "blocklist-pwd@example.com")

        user_before = db_session.execute(
            select(User).where(User.email == "blocklist-pwd@example.com")
        ).scalar_one()
        old_hash = user_before.password_hash

        r = client.post(
            "/auth/password/reset/confirm",
            json={"token": raw, "new_password": "password1234"},  # blocklist match
        )
        assert r.status_code == 400, f"expected 400, got {r.status_code}: {r.text}"
        assert r.json() == {"detail": "weak_password"}

        db_session.expire_all()
        user_after = db_session.execute(
            select(User).where(User.email == "blocklist-pwd@example.com")
        ).scalar_one()
        assert user_after.password_hash == old_hash, "password_hash must NOT change on blocklist"

        # Token NOT consumed.
        rows = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "password_reset",
                VerificationToken.consumed_at.is_(None),
            )
        ).scalars().all()
        assert len(rows) == 1, "active token must remain after blocklist rejection"


class TestSecuritySideEffects:
    """Cover class — spec §Side-effects de confirm password reset (audit + transaction)."""
    pass


class TestTransactionAtomicity:
    def test_confirm_atomic_rollback_on_audit_fail(self, client_pg_ready, db_session, monkeypatch):
        """given a reset confirm where INSERT auth_events fails, when transaction commits, then password_hash unchanged + token NOT consumed (full rollback).

        spec §Side-effects — Atomicité critique : si INSERT auth_events échoue, ROLLBACK total.
        spec §Test d'acceptation #14.
        """
        from sqlalchemy import event, select

        from server.db.models import AuthEvent, User, VerificationToken

        client = client_pg_ready
        _register(client, email="atomic@example.com")
        client.post("/auth/password/reset/request", json={"email": "atomic@example.com"})
        raw, user, _ = _capture_raw_token(db_session, "atomic@example.com")

        user_before = db_session.execute(
            select(User).where(User.email == "atomic@example.com")
        ).scalar_one()
        old_hash = user_before.password_hash

        # Force INSERT auth_events to RAISE for the password_reset_confirm event.
        def _raise_on_audit(mapper, connection, target):
            if getattr(target, "event_type", None) == "password_reset_confirm":
                raise RuntimeError("forced audit failure for atomicity test")

        event.listen(AuthEvent, "before_insert", _raise_on_audit)
        try:
            r = client.post(
                "/auth/password/reset/confirm",
                json={"token": raw, "new_password": "ShouldRollbackPwd1234!"},
            )
            # The reset must NOT succeed — server returns 5xx (or any non-200) due to forced fail.
            assert r.status_code != 200, (
                f"reset must fail due to audit insert raise, got 200: {r.text}"
            )
        finally:
            event.remove(AuthEvent, "before_insert", _raise_on_audit)

        db_session.expire_all()
        user_after = db_session.execute(
            select(User).where(User.email == "atomic@example.com")
        ).scalar_one()
        assert user_after.password_hash == old_hash, (
            "password_hash must remain unchanged on audit fail (atomic rollback)"
        )

        # Token must NOT be consumed.
        active_rows = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "password_reset",
                VerificationToken.consumed_at.is_(None),
            )
        ).scalars().all()
        assert len(active_rows) == 1, (
            "verification_token must NOT be consumed on audit fail (atomic rollback)"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register` (function) — lines 29-34
- `_capture_raw_token` (function) — lines 37-57
- `TestRequestPasswordReset` (class) — lines 60-163
- `TestRaceCondition` (class) — lines 166-228
- `TestConfirmPasswordReset` (class) — lines 231-454
- `TestSecuritySideEffects` (class) — lines 457-459
- `TestTransactionAtomicity` (class) — lines 462-519

### Imports
- `statistics`
- `time`
- `datetime`
- `pytest`

### Exports
- `_register`
- `_capture_raw_token`
- `TestRequestPasswordReset`
- `TestRaceCondition`
- `TestConfirmPasswordReset`
- `TestSecuritySideEffects`
- `TestTransactionAtomicity`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — classes: `TestRequestPasswordReset`, `TestConfirmPasswordReset`, `TestSecuritySideEffects`, `TestRaceCondition`, `TestTransactionAtomicity` · methods: `test_request_returns_202_for_known_user`, `test_request_returns_202_for_unknown_email_anti_enum`, `test_request_jitter_within_80_120ms`, `test_request_re_request_revokes_old_token`, `test_request_concurrent_inserts_only_one_active`, `test_confirm_changes_password_hash`, `test_confirm_updates_password_changed_at`, `test_confirm_revokes_all_refresh_tokens`, `test_confirm_consumes_token_replay_returns_400`, `test_confirm_rejects_weak_password`, `test_confirm_rejects_blocklist_password`, `test_confirm_atomic_rollback_on_audit_fail`
