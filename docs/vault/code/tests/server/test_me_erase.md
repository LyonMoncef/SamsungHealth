---
type: code-source
language: python
file_path: tests/server/test_me_erase.py
git_blob: 3f3a9fbbdadca11cb85f3ac86c65a8792723e990
last_synced: '2026-05-06T08:02:35Z'
loc: 579
annotations: []
imports:
- datetime
- unittest.mock
- pytest
exports:
- _register_and_login
- _bearer
- _request_erase_token
- _seed_health_data
- TestErasePreconditions
- _health_tables_param
- TestEraseCascadeAllTables
- TestEraseAuthEventsAnonymized
- TestEraseAntiAccident
- TestEraseOAuthOnly
- TestEraseTokenSingleUse
tags:
- code
- python
---

# tests/server/test_me_erase.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_me_erase.py`](../../../tests/server/test_me_erase.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Phase 3 RGPD — `/me/erase/{request,confirm}` (Art. 17 right to erasure).

Tests RED-first contre `server.routers.me` (NEW) + `server.security.rgpd` (NEW).

Couvre les cas spec §"Erase" #17-#27 :
- 2-step request (re-auth password OU oauth_nonce) → confirm.
- Cascade applicatif explicit sur les 21 tables santé + identity_providers + refresh_tokens + verification_tokens (HIGH 3).
- `auth_events` anonymisés (UPDATE user_id=NULL, email_hash=NULL, ip_hash=NULL, user_agent=NULL — HIGH 2).
- Single-use atomic UPDATE...RETURNING (LOW).
- `_safe_audit_event` no-op post-erase (HIGH 2).
- CSRF Sec-Fetch-Site cross-site → 403.
- Audit `rgpd.erase.confirmed` créé AVANT delete users (LOW).
- OAuth-only flow nonce.

Spec : docs/vault/specs/2026-04-28-phase3-rgpd-endpoints.md §2, §11, §12.
"""
from __future__ import annotations

from datetime import datetime, timezone
from unittest.mock import patch

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


def _request_erase_token(client, access: str, password: str = _DEFAULT_PASSWORD) -> str:
    r = client.post(
        "/me/erase/request",
        headers=_bearer(access),
        json={"password": password},
    )
    assert r.status_code == 200, f"erase/request failed: {r.text}"
    return r.json()["erase_token"]


def _seed_health_data(db_session, user_id) -> None:
    """Insert 1 row in chaque table santé pour vérifier ensuite que cascade les supprime."""
    from server.db.models import (
        ActivityDaily,
        ActivityLevel,
        BloodPressure,
        Ecg,
        ExerciseSession,
        FloorsDaily,
        HeartRateHourly,
        Height,
        Hrv,
        Mood,
        RespiratoryRate,
        SkinTemperature,
        SleepSession,
        SleepStage,
        Spo2,
        StepsDaily,
        StepsHourly,
        Stress,
        VitalityScore,
        WaterIntake,
        Weight,
    )

    t0 = datetime(2026, 4, 28, 12, 0, 0, tzinfo=timezone.utc)
    t1 = datetime(2026, 4, 28, 13, 0, 0, tzinfo=timezone.utc)
    day_str = "2026-04-28"

    sleep = SleepSession(user_id=user_id, sleep_start=t0, sleep_end=t1)
    db_session.add(sleep)
    db_session.flush()
    db_session.add_all(
        [
            SleepStage(
                user_id=user_id,
                session_id=sleep.id,
                stage_type="REM",
                stage_start=t0,
                stage_end=t1,
            ),
            StepsHourly(user_id=user_id, date=day_str, hour=12, step_count=100),
            StepsDaily(user_id=user_id, day_date=day_str),
            HeartRateHourly(
                user_id=user_id,
                date=day_str,
                hour=12,
                min_bpm=60,
                max_bpm=80,
                avg_bpm=70,
                sample_count=10,
            ),
            ExerciseSession(
                user_id=user_id,
                exercise_type="run",
                exercise_start=t0,
                exercise_end=t1,
                duration_minutes=60.0,
            ),
            Stress(user_id=user_id, start_time=t0, end_time=t1),
            Spo2(user_id=user_id, start_time=t0, end_time=t1),
            RespiratoryRate(user_id=user_id, start_time=t0, end_time=t1),
            Hrv(user_id=user_id, start_time=t0, end_time=t1),
            SkinTemperature(user_id=user_id, start_time=t0, end_time=t1),
            Weight(user_id=user_id, start_time=t0),
            Height(user_id=user_id, start_time=t0),
            BloodPressure(user_id=user_id, start_time=t0),
            Mood(user_id=user_id, start_time=t0, notes="seed"),
            WaterIntake(user_id=user_id, start_time=t0, amount_ml=250.0),
            ActivityDaily(user_id=user_id, day_date=day_str),
            VitalityScore(user_id=user_id, day_date=day_str),
            FloorsDaily(user_id=user_id, day_date=day_str),
            ActivityLevel(user_id=user_id, start_time=t0),
            Ecg(user_id=user_id, start_time=t0, end_time=t1),
        ]
    )
    db_session.commit()


# ── TestErasePreconditions (3 tests) ──────────────────────────────────────


class TestErasePreconditions:
    # spec test #17
    def test_request_without_bearer_returns_401(self, client_pg_ready):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        r = client.post("/me/erase/request", json={"password": _DEFAULT_PASSWORD})
        assert r.status_code == 401, f"expected 401, got {r.status_code}: {r.text}"

    # spec test #19
    def test_request_wrong_password_returns_401_with_soft_backoff(self, client_pg_ready):
        import time

        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "wrongpwd-erase@example.com")
        t0 = time.monotonic()
        r = client.post(
            "/me/erase/request",
            headers=_bearer(access),
            json={"password": "WRONG_PASSWORD"},
        )
        elapsed = time.monotonic() - t0
        assert r.status_code == 401, f"expected 401, got {r.status_code}: {r.text}"
        assert elapsed >= 0.05, f"soft backoff missing, elapsed={elapsed*1000:.0f}ms"

    # spec test #27
    def test_csrf_cross_site_request_returns_403(self, client_pg_ready):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "csrf-erase@example.com")
        r = client.post(
            "/me/erase/request",
            headers={**_bearer(access), "Sec-Fetch-Site": "cross-site"},
            json={"password": _DEFAULT_PASSWORD},
        )
        assert r.status_code == 403, (
            f"cross-site Sec-Fetch-Site must 403, got {r.status_code}: {r.text}"
        )


# ── TestEraseCascadeAllTables (parametrized 21 tables) ────────────────────


@pytest.fixture
def _erase_user_seeded(client_pg_ready, db_session):
    """Setup helper: register user, seed 1 row per health table, perform full erase."""
    from sqlalchemy import select

    from server.db.models import User
    from server.routers.me import router  # noqa: F401

    client = client_pg_ready
    access = _register_and_login(client, "cascade-erase@example.com")
    user = db_session.execute(
        select(User).where(User.email == "cascade-erase@example.com")
    ).scalar_one()
    user_id = user.id
    _seed_health_data(db_session, user_id)

    erase_token = _request_erase_token(client, access)
    r_confirm = client.post(
        "/me/erase/confirm",
        headers=_bearer(access),
        json={"erase_token": erase_token},
    )
    assert r_confirm.status_code == 204, (
        f"erase/confirm failed: {r_confirm.status_code} {r_confirm.text}"
    )
    db_session.expire_all()
    return user_id


def _health_tables_param():
    try:
        from server.security.rgpd import HEALTH_TABLES

        return list(HEALTH_TABLES)
    except ImportError:
        # RED phase : module absent → param avec liste vide ; pytest générera un
        # message clair "no tests collected for parametrize" qu'on transforme en
        # un seul test placeholder failing.
        return ["__rgpd_module_missing__"]


class TestEraseCascadeAllTables:
    # spec test #21 — paramétré sur les 21 tables santé.
    @pytest.mark.parametrize("table", _health_tables_param())
    def test_health_table_user_id_count_zero_after_erase(
        self, table, _erase_user_seeded, db_session
    ):
        from sqlalchemy import text

        if table == "__rgpd_module_missing__":
            pytest.fail(
                "server.security.rgpd.HEALTH_TABLES missing — RED expected pre-impl"
            )

        user_id = _erase_user_seeded
        count = db_session.execute(
            text(f"SELECT COUNT(*) FROM {table} WHERE user_id = :uid"),
            {"uid": str(user_id)},
        ).scalar_one()
        assert count == 0, f"cascade incomplete: {count} rows remain in {table}"

    # spec test #21 (autres tables auth)
    def test_identity_providers_refresh_tokens_verification_tokens_deleted(
        self, _erase_user_seeded, db_session
    ):
        from sqlalchemy import text

        user_id = _erase_user_seeded
        for table in ("identity_providers", "refresh_tokens", "verification_tokens"):
            count = db_session.execute(
                text(f"SELECT COUNT(*) FROM {table} WHERE user_id = :uid"),
                {"uid": str(user_id)},
            ).scalar_one()
            assert count == 0, f"{table} not cleaned, {count} rows remain"

    # spec test #21 (users row)
    def test_users_row_deleted(self, _erase_user_seeded, db_session):
        from sqlalchemy import text

        user_id = _erase_user_seeded
        count = db_session.execute(
            text("SELECT COUNT(*) FROM users WHERE id = :uid"),
            {"uid": str(user_id)},
        ).scalar_one()
        assert count == 0, "users row should be deleted post-erase"


# ── TestEraseAuthEventsAnonymized (HIGH 2) ────────────────────────────────


class TestEraseAuthEventsAnonymized:
    # spec test #21 (auth_events anonymisation)
    def test_auth_events_user_id_email_ip_user_agent_all_null(
        self, _erase_user_seeded, db_session
    ):
        """Tous les champs identifiants doivent être NULL post-erase (HIGH 2)."""
        from sqlalchemy import text

        # On query les rows qui auraient pu appartenir à ce user (par anonymisation
        # via UPDATE, donc rien ne reste avec user_id=cet_uid). On vérifie qu'il
        # n'existe AUCUNE row avec user_id IS NOT NULL ET email_hash IS NOT NULL
        # ET les autres champs non-null pour des events qui auraient appartenu
        # au user supprimé. La contrainte exacte : pour TOUTES les rows qui ont
        # eu user_id=<uid> avant erase, après erase elles doivent avoir les 4
        # colonnes NULL.
        user_id = _erase_user_seeded
        # 1) Aucune row n'a encore notre user_id (anonymisé en UPDATE NULL).
        count_with_uid = db_session.execute(
            text("SELECT COUNT(*) FROM auth_events WHERE user_id = :uid"),
            {"uid": str(user_id)},
        ).scalar_one()
        assert count_with_uid == 0, (
            f"auth_events still has {count_with_uid} rows with user_id={user_id}"
        )
        # 2) Au moins une row anonymisée existe (user_id NULL + email_hash NULL).
        count_anon = db_session.execute(
            text(
                """
                SELECT COUNT(*) FROM auth_events
                WHERE user_id IS NULL
                  AND email_hash IS NULL
                  AND ip_hash IS NULL
                  AND user_agent IS NULL
                """
            )
        ).scalar_one()
        assert count_anon >= 1, (
            f"expected ≥1 fully-anonymized auth_events row post-erase, got {count_anon}"
        )

    # spec test #24 — audit `rgpd.erase.confirmed` créé AVANT delete users
    def test_audit_event_rgpd_erase_confirmed_exists_post_erase(
        self, _erase_user_seeded, db_session
    ):
        from sqlalchemy import text

        # L'event a été créé avec user_id=cet_uid puis anonymisé → on cherche
        # un event_type=rgpd.erase.confirmed avec user_id IS NULL (anonymisé).
        count = db_session.execute(
            text(
                """
                SELECT COUNT(*) FROM auth_events
                WHERE event_type = 'rgpd.erase.confirmed'
                  AND user_id IS NULL
                """
            )
        ).scalar_one()
        assert count >= 1, (
            f"rgpd.erase.confirmed audit row missing post-erase (cnt={count})"
        )


# ── TestEraseAntiAccident (3 tests) ───────────────────────────────────────


class TestEraseAntiAccident:
    # spec test #22 — token expiré OU mauvais purpose
    def test_confirm_with_expired_token_returns_400(self, client_pg_ready, db_session):
        from datetime import timedelta

        from sqlalchemy import select

        from server.db.models import User, VerificationToken
        from server.routers.me import router  # noqa: F401
        from server.security.auth import (
            generate_verification_token,
            hash_verification_token,
        )

        client = client_pg_ready
        access = _register_and_login(client, "expired-erase@example.com")
        user = db_session.execute(
            select(User).where(User.email == "expired-erase@example.com")
        ).scalar_one()
        raw, _ = generate_verification_token()
        now = datetime.now(timezone.utc)
        db_session.add(
            VerificationToken(
                user_id=user.id,
                token_hash=hash_verification_token(raw),
                purpose="account_erase_confirm",
                issued_at=now - timedelta(hours=1),
                expires_at=now - timedelta(seconds=1),
            )
        )
        db_session.commit()

        r = client.post(
            "/me/erase/confirm",
            headers=_bearer(access),
            json={"erase_token": raw},
        )
        assert r.status_code == 400, f"expired token must 400, got {r.status_code}"

    # spec test #22 — wrong purpose (cross-purpose token reuse blocked)
    def test_confirm_with_wrong_purpose_token_returns_400(
        self, client_pg_ready, db_session
    ):
        from datetime import timedelta

        from sqlalchemy import select

        from server.db.models import User, VerificationToken
        from server.routers.me import router  # noqa: F401
        from server.security.auth import (
            generate_verification_token,
            hash_verification_token,
        )

        client = client_pg_ready
        access = _register_and_login(client, "wrongpurpose-erase@example.com")
        user = db_session.execute(
            select(User).where(User.email == "wrongpurpose-erase@example.com")
        ).scalar_one()
        # Forge a token with a different purpose (e.g. email_verification).
        raw, _ = generate_verification_token()
        now = datetime.now(timezone.utc)
        db_session.add(
            VerificationToken(
                user_id=user.id,
                token_hash=hash_verification_token(raw),
                purpose="email_verification",  # WRONG purpose
                issued_at=now,
                expires_at=now + timedelta(minutes=5),
            )
        )
        db_session.commit()

        r = client.post(
            "/me/erase/confirm",
            headers=_bearer(access),
            json={"erase_token": raw},
        )
        assert r.status_code == 400, (
            f"wrong-purpose token must 400 (cross-purpose reuse blocked), "
            f"got {r.status_code}: {r.text}"
        )

    # spec test #23 — single-use replay
    def test_confirm_replay_after_consume_returns_400(self, client_pg_ready, db_session):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "replay-erase@example.com")
        erase_token = _request_erase_token(client, access)

        first = client.post(
            "/me/erase/confirm",
            headers=_bearer(access),
            json={"erase_token": erase_token},
        )
        # Le 1er call peut 204 (success) — le user disparaît, donc l'access
        # token devient orphelin. Les calls suivants seront 401 OU 400.
        assert first.status_code in (204, 200), (
            f"first call should succeed: {first.status_code} {first.text}"
        )
        # 2e call : le token est consommé → 400 (ou 401 si user gone).
        second = client.post(
            "/me/erase/confirm",
            headers=_bearer(access),
            json={"erase_token": erase_token},
        )
        assert second.status_code in (400, 401), (
            f"replay should fail (400 consumed OR 401 user gone), "
            f"got {second.status_code}: {second.text}"
        )


# ── TestEraseOAuthOnly (1 test) ───────────────────────────────────────────


class TestEraseOAuthOnly:
    # spec test #20
    def test_oauth_only_user_password_rejected_nonce_accepted(
        self, client_pg_ready, db_session
    ):
        from sqlalchemy import select

        from server.db.models import User
        from server.routers.me import router  # noqa: F401
        from server.security.auth import OAUTH_SENTINEL

        client = client_pg_ready
        access = _register_and_login(client, "oauth-erase@example.com")
        user = db_session.execute(
            select(User).where(User.email == "oauth-erase@example.com")
        ).scalar_one()
        user.password_hash = OAUTH_SENTINEL
        db_session.commit()

        # Password rejected.
        r_pwd = client.post(
            "/me/erase/request",
            headers=_bearer(access),
            json={"password": _DEFAULT_PASSWORD},
        )
        assert r_pwd.status_code == 401, (
            f"OAuth-only password must be rejected, got {r_pwd.status_code}: {r_pwd.text}"
        )
        # Nonce accepted (mock Google verification).
        with patch(
            "server.security.rgpd._verify_oauth_nonce", return_value=True, create=True
        ):
            r_nonce = client.post(
                "/me/erase/request",
                headers=_bearer(access),
                json={"oauth_nonce": "valid-google-signed-nonce"},
            )
        assert r_nonce.status_code == 200, (
            f"OAuth-only nonce must be accepted, got {r_nonce.status_code}: {r_nonce.text}"
        )
        assert "erase_token" in r_nonce.json()


# ── TestEraseTokenSingleUse + _safe_audit_event (2 tests) ────────────────


class TestEraseTokenSingleUse:
    # spec test #25 — _safe_audit_event no-op post-erase (HIGH 2)
    def test_safe_audit_event_skips_when_user_id_missing(
        self, client_pg_ready, db_session
    ):
        """Appel `_safe_audit_event(db, "test", user_id=non_existent_uid)` →
        0 row insérée en auth_events (HIGH 2 — pas de re-création email_hash post-erase)."""
        import uuid

        from sqlalchemy import text

        from server.routers.me import router  # noqa: F401
        from server.security.rgpd import _safe_audit_event

        # User_id qui n'existe pas en DB.
        ghost_uid = uuid.uuid4()
        before = db_session.execute(
            text(
                "SELECT COUNT(*) FROM auth_events WHERE event_type = 'test.safe_audit'"
            )
        ).scalar_one()
        _safe_audit_event(
            db_session, "test.safe_audit", user_id=ghost_uid, meta={"x": 1}
        )
        db_session.commit()
        after = db_session.execute(
            text(
                "SELECT COUNT(*) FROM auth_events WHERE event_type = 'test.safe_audit'"
            )
        ).scalar_one()
        assert after == before, (
            f"_safe_audit_event must skip when user_id missing, before={before} after={after}"
        )

    # spec test #26 — atomic single-use (UPDATE...RETURNING)
    def test_concurrent_confirm_only_one_succeeds(self, client_pg_ready, db_session):
        """Simulate 2 confirms en // : un seul réussit (atomic UPDATE...RETURNING).

        On utilise `_consume_verification_token_atomic` directement pour vérifier
        que le 2e call retourne None (rowcount=0)."""
        from sqlalchemy import select

        from server.db.models import User, VerificationToken
        from server.routers.me import router  # noqa: F401
        from server.security.rgpd import _consume_verification_token_atomic

        client = client_pg_ready
        access = _register_and_login(client, "atomic-erase@example.com")
        erase_token = _request_erase_token(client, access)
        user = db_session.execute(
            select(User).where(User.email == "atomic-erase@example.com")
        ).scalar_one()

        # 1st consume → returns the row.
        first = _consume_verification_token_atomic(
            db_session, user, erase_token, purpose="account_erase_confirm"
        )
        db_session.commit()
        assert first is not None, "first atomic consume should return the row"

        # 2nd consume on same token → returns None (already consumed).
        second = _consume_verification_token_atomic(
            db_session, user, erase_token, purpose="account_erase_confirm"
        )
        assert second is None, (
            f"second atomic consume must return None (already consumed), got {second}"
        )

        # Token row exists but consumed_at is set.
        row = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "account_erase_confirm",
            )
        ).scalar_one()
        assert row.consumed_at is not None, "token row consumed_at must be set"
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register_and_login` (function) — lines 32-41
- `_bearer` (function) — lines 44-45
- `_request_erase_token` (function) — lines 48-55
- `_seed_health_data` (function) — lines 58-135
- `TestErasePreconditions` (class) — lines 141-181
- `_health_tables_param` (function) — lines 216-225
- `TestEraseCascadeAllTables` (class) — lines 228-271
- `TestEraseAuthEventsAnonymized` (class) — lines 277-336
- `TestEraseAntiAccident` (class) — lines 342-452
- `TestEraseOAuthOnly` (class) — lines 458-498
- `TestEraseTokenSingleUse` (class) — lines 504-579

### Imports
- `datetime`
- `unittest.mock`
- `pytest`

### Exports
- `_register_and_login`
- `_bearer`
- `_request_erase_token`
- `_seed_health_data`
- `TestErasePreconditions`
- `_health_tables_param`
- `TestEraseCascadeAllTables`
- `TestEraseAuthEventsAnonymized`
- `TestEraseAntiAccident`
- `TestEraseOAuthOnly`
- `TestEraseTokenSingleUse`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-28-phase3-rgpd-endpoints]] — classes: `TestErasePreconditions`, `TestEraseCascadeAllTables`, `TestEraseAuthEventsAnonymized`, `TestEraseAntiAccident`, `TestEraseOAuthOnly`, `TestEraseTokenSingleUse`
