---
type: code-source
language: python
file_path: tests/server/test_me_export.py
git_blob: c1801dafff6ae2dfcf3be51b419c821a6b380cb2
last_synced: '2026-04-28T23:12:43Z'
loc: 561
annotations: []
imports:
- io
- re
- zipfile
- datetime
- unittest.mock
- pytest
exports:
- _register_and_login
- _bearer
- TestExportRequestReauth
- _request_export_token
- TestExportConfirmZip
- TestExportContent
- TestUserIsolation
- TestExportRateLimit
- TestExportRaceWithErase
tags:
- code
- python
---

# tests/server/test_me_export.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_me_export.py`](../../../tests/server/test_me_export.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Phase 3 RGPD — `/me/export/{request,confirm}` (Art. 20 portability).

Tests RED-first contre `server.routers.me` (NEW) + `server.security.rgpd` (NEW).

Couvre les cas spec §"Export — 2-step" #1-#16 :
- 2-step re-auth (request → token → confirm → ZIP)
- ZIP structure (manifest + user.json + identity_providers + auth_events + 21×csv+json)
- Filename générique (sans user_id leak)
- user.json sans password_hash
- Mood déchiffré dans CSV
- Multi-user isolation
- Rate-limit 5/h/user
- Race vs erase (SELECT FOR UPDATE assertion)

Spec : docs/vault/specs/2026-04-28-phase3-rgpd-endpoints.md §1, §1.bis, §11, §12.

Note conventions :
- Imports lazy DANS chaque test (RED clair sur module manquant)
- Comment `# spec: §X.Y test #N` au-dessus de chaque test
- Ce fichier est dans `_NO_AUTO_AUTH_FILES` — on gère le login manuellement.
"""
from __future__ import annotations

import io
import re
import zipfile
from datetime import date, datetime, timedelta, timezone
from unittest.mock import MagicMock, patch

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"
_DEFAULT_PASSWORD = "longpassword12345"


# ── helpers ───────────────────────────────────────────────────────────────


def _register_and_login(client, email: str, password: str = _DEFAULT_PASSWORD) -> str:
    """Register + login → return access_token (Bearer)."""
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


# ── TestExportRequestReauth (4 tests) ─────────────────────────────────────


class TestExportRequestReauth:
    # spec: §1 / "Export — 2-step" test #1
    def test_request_without_bearer_returns_401(self, client_pg_ready):
        client = client_pg_ready
        from server.routers.me import router  # noqa: F401 — RED si module absent

        r = client.post("/me/export/request", json={"password": _DEFAULT_PASSWORD})
        assert r.status_code == 401, f"expected 401, got {r.status_code}: {r.text}"

    # spec: §1 / "Export — 2-step" test #2
    def test_request_wrong_password_returns_401_with_soft_backoff(
        self, client_pg_ready
    ):
        import time

        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "wrongpwd-export@example.com")

        t0 = time.monotonic()
        r = client.post(
            "/me/export/request",
            headers=_bearer(access),
            json={"password": "definitely-not-the-right-password"},
        )
        elapsed = time.monotonic() - t0

        assert r.status_code == 401, f"expected 401, got {r.status_code}: {r.text}"
        # Soft backoff V2.3.3.1 : sleep observable (≥ 100ms typ. exponential).
        assert elapsed >= 0.05, (
            f"soft backoff should add measurable delay, got {elapsed*1000:.0f}ms"
        )

    # spec: §1 / "Export — 2-step" test #3
    def test_request_ok_returns_export_token_persisted_with_5min_ttl(
        self, client_pg_ready, db_session
    ):
        from sqlalchemy import select

        from server.db.models import User, VerificationToken
        from server.routers.me import router  # noqa: F401
        from server.security.auth import hash_verification_token

        client = client_pg_ready
        access = _register_and_login(client, "ok-export@example.com")

        r = client.post(
            "/me/export/request",
            headers=_bearer(access),
            json={"password": _DEFAULT_PASSWORD},
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"

        body = r.json()
        assert "export_token" in body, f"missing export_token: {body}"
        assert "expires_at" in body, f"missing expires_at: {body}"
        assert isinstance(body["export_token"], str)
        assert isinstance(body["expires_at"], str)

        # Token persisté en DB avec purpose=account_export_confirm.
        user = db_session.execute(
            select(User).where(User.email == "ok-export@example.com")
        ).scalar_one()
        token_hash = hash_verification_token(body["export_token"])
        row = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.token_hash == token_hash,
                VerificationToken.user_id == user.id,
            )
        ).scalar_one()
        assert row.purpose == "account_export_confirm", (
            f"wrong purpose: {row.purpose}"
        )
        # TTL 5 min : expires_at - issued_at ≈ 300s.
        ttl = (row.expires_at - row.issued_at).total_seconds()
        assert 290 <= ttl <= 310, f"TTL should be ~300s, got {ttl}s"

    # spec: §1 / "Export — 2-step" test #16 (OAuth-only)
    def test_request_oauth_only_user_rejects_password_accepts_oauth_nonce(
        self, client_pg_ready, db_session
    ):
        from sqlalchemy import select

        from server.db.models import User
        from server.routers.me import router  # noqa: F401
        from server.security.auth import OAUTH_SENTINEL

        client = client_pg_ready
        # Register a user with normal password to obtain a Bearer ; then flip
        # password_hash to OAUTH_SENTINEL so re-auth must use oauth_nonce.
        access = _register_and_login(client, "oauth-export@example.com")
        user = db_session.execute(
            select(User).where(User.email == "oauth-export@example.com")
        ).scalar_one()
        user.password_hash = OAUTH_SENTINEL
        db_session.commit()

        # 1. Password rejected (OAuth-only must NOT accept password).
        r_pwd = client.post(
            "/me/export/request",
            headers=_bearer(access),
            json={"password": _DEFAULT_PASSWORD},
        )
        assert r_pwd.status_code == 401, (
            f"OAuth-only must reject password, got {r_pwd.status_code}: {r_pwd.text}"
        )

        # 2. oauth_nonce body field accepted (we mock the Google verify).
        with patch(
            "server.security.rgpd._verify_oauth_nonce", return_value=True, create=True
        ):
            r_nonce = client.post(
                "/me/export/request",
                headers=_bearer(access),
                json={"oauth_nonce": "valid-google-signed-nonce"},
            )
        assert r_nonce.status_code == 200, (
            f"OAuth-only with valid nonce should 200, got "
            f"{r_nonce.status_code}: {r_nonce.text}"
        )
        assert "export_token" in r_nonce.json()


# ── TestExportConfirmZip (3 tests) ────────────────────────────────────────


def _request_export_token(client, access: str) -> str:
    r = client.post(
        "/me/export/request",
        headers=_bearer(access),
        json={"password": _DEFAULT_PASSWORD},
    )
    assert r.status_code == 200, f"request failed: {r.text}"
    return r.json()["export_token"]


class TestExportConfirmZip:
    # spec: §1 / "Export — 2-step" test #4
    def test_confirm_invalid_token_returns_400(self, client_pg_ready):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "invalid-tok@example.com")

        r = client.get(
            "/me/export/confirm",
            params={"export_token": "x" * 43},
            headers=_bearer(access),
        )
        assert r.status_code == 400, f"expected 400, got {r.status_code}: {r.text}"
        assert r.json() == {"detail": "invalid_or_consumed_token"}

    # spec: §1 / "Export — 2-step" test #5
    def test_confirm_consumed_token_second_call_returns_400(self, client_pg_ready):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "single-use@example.com")
        export_token = _request_export_token(client, access)

        first = client.get(
            "/me/export/confirm",
            params={"export_token": export_token},
            headers=_bearer(access),
        )
        assert first.status_code == 200, f"1st call should 200, got {first.text}"

        second = client.get(
            "/me/export/confirm",
            params={"export_token": export_token},
            headers=_bearer(access),
        )
        assert second.status_code == 400, (
            f"2nd call should 400, got {second.status_code}: {second.text}"
        )
        assert second.json() == {"detail": "invalid_or_consumed_token"}

    # spec: §1 / "Export — 2-step" test #6 (filename générique)
    def test_confirm_ok_returns_zip_with_generic_filename(self, client_pg_ready):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "zip-ok@example.com")
        export_token = _request_export_token(client, access)

        r = client.get(
            "/me/export/confirm",
            params={"export_token": export_token},
            headers=_bearer(access),
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        assert r.headers["content-type"].startswith("application/zip"), (
            f"wrong content-type: {r.headers.get('content-type')}"
        )
        cd = r.headers.get("content-disposition", "")
        # Filename générique `export_my_data_<YYYY-MM-DD>.zip` — pas de user_id leak.
        assert re.match(
            r'attachment;\s*filename="export_my_data_\d{4}-\d{2}-\d{2}\.zip"', cd
        ), f"filename should be generic, got: {cd}"
        # Et concrètement, ne contient PAS un substring user_id (UUID-like).
        assert not re.search(r"[0-9a-f]{8}-[0-9a-f]{4}-", cd), (
            f"filename leaks user_id-like UUID: {cd}"
        )


# ── TestExportContent (3 tests) ───────────────────────────────────────────


class TestExportContent:
    # spec: §1 / "Export — 2-step" test #7 (ZIP structure 21 tables)
    def test_zip_contains_manifest_user_identity_authevents_and_21_health_tables(
        self, client_pg_ready
    ):
        from server.routers.me import router  # noqa: F401
        from server.security.rgpd import HEALTH_TABLES

        client = client_pg_ready
        access = _register_and_login(client, "structure@example.com")
        export_token = _request_export_token(client, access)

        r = client.get(
            "/me/export/confirm",
            params={"export_token": export_token},
            headers=_bearer(access),
        )
        assert r.status_code == 200, f"expected 200, got {r.text}"
        zf = zipfile.ZipFile(io.BytesIO(r.content))
        names = set(zf.namelist())

        # Top-level metadata files.
        for required in (
            "manifest.json",
            "user.json",
            "identity_providers.json",
            "auth_events.json",
        ):
            assert required in names, f"missing {required} in ZIP, got: {sorted(names)}"

        # 21 health tables — chacune en .csv ET .json.
        assert len(HEALTH_TABLES) == 21, (
            f"HEALTH_TABLES should list 21 tables, got {len(HEALTH_TABLES)}"
        )
        for table in HEALTH_TABLES:
            assert f"health/{table}.csv" in names, (
                f"missing health/{table}.csv in ZIP"
            )
            assert f"health/{table}.json" in names, (
                f"missing health/{table}.json in ZIP"
            )

    # spec: §1 / "Export — 2-step" test #8 (no password_hash leak)
    def test_user_json_does_not_contain_password_hash(self, client_pg_ready):
        import json

        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "nohash@example.com")
        export_token = _request_export_token(client, access)

        r = client.get(
            "/me/export/confirm",
            params={"export_token": export_token},
            headers=_bearer(access),
        )
        assert r.status_code == 200
        zf = zipfile.ZipFile(io.BytesIO(r.content))
        user_json = json.loads(zf.read("user.json").decode("utf-8"))

        # `user_json` peut être un dict ou une liste à 1 dict — supporter les 2.
        records = user_json if isinstance(user_json, list) else [user_json]
        for rec in records:
            assert "password_hash" not in rec, (
                f"password_hash MUST NOT leak in user.json: {rec.keys()}"
            )

    # spec: §1 / "Export — 2-step" test #10 (mood déchiffré dans CSV)
    def test_mood_csv_contains_decrypted_values(
        self, client_pg_ready, db_session
    ):
        import csv

        from sqlalchemy import select

        from server.db.models import Mood, User
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "mood-decrypt@example.com")

        # Insert a mood row via ORM (chiffrement V2.2.1 transparent).
        user = db_session.execute(
            select(User).where(User.email == "mood-decrypt@example.com")
        ).scalar_one()
        secret_note = "feeling-clear-text-marker-9482"
        m = Mood(
            user_id=user.id,
            start_time=datetime(2026, 4, 28, 12, 0, 0, tzinfo=timezone.utc),
            mood_type=3,
            notes=secret_note,
        )
        db_session.add(m)
        db_session.commit()

        export_token = _request_export_token(client, access)
        r = client.get(
            "/me/export/confirm",
            params={"export_token": export_token},
            headers=_bearer(access),
        )
        assert r.status_code == 200
        zf = zipfile.ZipFile(io.BytesIO(r.content))
        csv_bytes = zf.read("health/mood.csv").decode("utf-8")
        reader = csv.DictReader(io.StringIO(csv_bytes))
        rows = list(reader)
        assert any(secret_note in (row.get("notes") or "") for row in rows), (
            f"decrypted mood note ({secret_note!r}) missing from CSV — "
            f"got rows: {rows}"
        )


# ── TestUserIsolation (1 test) ────────────────────────────────────────────


class TestUserIsolation:
    # spec: §1 / "Export — 2-step" test #11
    def test_export_user_a_does_not_contain_user_b_data(
        self, client_pg_ready, db_session
    ):
        import csv

        from sqlalchemy import select

        from server.db.models import Mood, User
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        # User A.
        access_a = _register_and_login(client, "isol-a@example.com")
        user_a = db_session.execute(
            select(User).where(User.email == "isol-a@example.com")
        ).scalar_one()
        marker_a = "marker-user-A-XXXX-only"
        db_session.add(
            Mood(
                user_id=user_a.id,
                start_time=datetime(2026, 4, 28, 10, 0, 0, tzinfo=timezone.utc),
                notes=marker_a,
            )
        )
        # User B (note: client.headers may be sticky from auto-auth ; we use
        # explicit Authorization header per request).
        client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": "isol-b@example.com", "password": _DEFAULT_PASSWORD},
        )
        user_b = db_session.execute(
            select(User).where(User.email == "isol-b@example.com")
        ).scalar_one()
        marker_b = "marker-user-B-YYYY-only"
        db_session.add(
            Mood(
                user_id=user_b.id,
                start_time=datetime(2026, 4, 28, 11, 0, 0, tzinfo=timezone.utc),
                notes=marker_b,
            )
        )
        db_session.commit()

        # Export user A only.
        export_token = _request_export_token(client, access_a)
        r = client.get(
            "/me/export/confirm",
            params={"export_token": export_token},
            headers=_bearer(access_a),
        )
        assert r.status_code == 200
        zf = zipfile.ZipFile(io.BytesIO(r.content))
        csv_bytes = zf.read("health/mood.csv").decode("utf-8")
        rows = list(csv.DictReader(io.StringIO(csv_bytes)))

        # User A marker présent, user B marker ABSENT.
        joined = " ".join((row.get("notes") or "") for row in rows)
        assert marker_a in joined, f"user A marker missing in own export: {rows}"
        assert marker_b not in joined, (
            f"USER B DATA LEAKED IN USER A EXPORT — found {marker_b!r}: {rows}"
        )
        # Sanity : aucune ligne avec user_id == user_b.id.
        for row in rows:
            assert str(user_b.id) not in (row.get("user_id") or ""), (
                f"user B user_id leaked in user A export: {row}"
            )


# ── TestExportRateLimit (1 test) ──────────────────────────────────────────


class TestExportRateLimit:
    # spec: §1 / "Export — 2-step" test #12 (5/h/user, 6e → 429)
    def test_request_over_5_per_hour_returns_429(self, client_pg_ready):
        from server.routers.me import router  # noqa: F401

        client = client_pg_ready
        access = _register_and_login(client, "ratelimit@example.com")

        # 5 requests OK.
        for i in range(5):
            r = client.post(
                "/me/export/request",
                headers=_bearer(access),
                json={"password": _DEFAULT_PASSWORD},
            )
            assert r.status_code == 200, (
                f"request #{i + 1}/5 should 200, got {r.status_code}: {r.text}"
            )

        # 6e → 429.
        r6 = client.post(
            "/me/export/request",
            headers=_bearer(access),
            json={"password": _DEFAULT_PASSWORD},
        )
        assert r6.status_code == 429, (
            f"6th request should 429, got {r6.status_code}: {r6.text}"
        )


# ── TestExportRaceWithErase (1 test, white-box mock) ──────────────────────


class TestExportRaceWithErase:
    # spec: §1.bis / "Export — 2-step" test #15
    def test_build_user_export_zip_acquires_select_for_update_first(
        self, client_pg_ready, db_session
    ):
        """White-box assertion : `build_user_export_zip` doit invoker
        `select(User).where(User.id == ...).with_for_update()` AVANT tout
        autre accès aux tables santé. On intercepte les calls via mock du DB
        session pour vérifier l'ordre.

        spec §1.bis : SELECT FOR UPDATE bloque l'erase concurrent.
        """
        from sqlalchemy import select

        from server.db.models import User
        from server.routers.me import router  # noqa: F401
        from server.security.rgpd import build_user_export_zip

        # Build a real user (we still need a real DB row to call build_user_export_zip).
        client = client_pg_ready
        _register_and_login(client, "race@example.com")
        user = db_session.execute(
            select(User).where(User.email == "race@example.com")
        ).scalar_one()

        # Wrap db_session.execute to record the order of select-for-update vs
        # health-table access.
        recorded: list[str] = []
        real_execute = db_session.execute

        def _spy_execute(stmt, *args, **kwargs):
            stmt_str = str(stmt).lower()
            if "for update" in stmt_str and "users" in stmt_str:
                recorded.append("select_for_update_users")
            elif any(
                f" {t}" in stmt_str or f"\n{t}" in stmt_str
                for t in (
                    "sleep_sessions",
                    "mood",
                    "steps_hourly",
                    "heart_rate_hourly",
                )
            ):
                recorded.append(f"health:{stmt_str[:40]}")
            return real_execute(stmt, *args, **kwargs)

        spy = MagicMock(side_effect=_spy_execute)
        with patch.object(db_session, "execute", spy):
            try:
                build_user_export_zip(db_session, user, full_audit=False)
            except Exception:
                # On ne se soucie pas du retour, juste de l'ordre des calls
                # avant que ça ne crashe (impl. peut être incomplète au RED).
                pass

        # `select_for_update_users` doit apparaître AU MOINS UNE FOIS et AVANT
        # tout `health:*` access.
        assert "select_for_update_users" in recorded, (
            f"build_user_export_zip must SELECT...FOR UPDATE on users.id ; "
            f"recorded calls: {recorded}"
        )
        first_for_update = recorded.index("select_for_update_users")
        first_health = next(
            (i for i, e in enumerate(recorded) if e.startswith("health:")), None
        )
        if first_health is not None:
            assert first_for_update < first_health, (
                f"SELECT FOR UPDATE must precede health-table access ; "
                f"recorded: {recorded}"
            )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register_and_login` (function) — lines 40-50
- `_bearer` (function) — lines 53-54
- `TestExportRequestReauth` (class) — lines 60-181
- `_request_export_token` (function) — lines 187-194
- `TestExportConfirmZip` (class) — lines 197-263
- `TestExportContent` (class) — lines 269-379
- `TestUserIsolation` (class) — lines 385-453
- `TestExportRateLimit` (class) — lines 459-486
- `TestExportRaceWithErase` (class) — lines 492-561

### Imports
- `io`
- `re`
- `zipfile`
- `datetime`
- `unittest.mock`
- `pytest`

### Exports
- `_register_and_login`
- `_bearer`
- `TestExportRequestReauth`
- `_request_export_token`
- `TestExportConfirmZip`
- `TestExportContent`
- `TestUserIsolation`
- `TestExportRateLimit`
- `TestExportRaceWithErase`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-28-phase3-rgpd-endpoints]] — classes: `TestExportRequestReauth`, `TestExportConfirmZip`, `TestExportContent`, `TestExportRateLimit`, `TestUserIsolation`, `TestExportRaceWithErase`
