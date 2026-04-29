---
type: code-source
language: python
file_path: tests/server/test_email_verify_routes.py
git_blob: e150d0e85e0a76107dc3d1abe0b6f442f7dbed13
last_synced: '2026-04-26T22:07:14Z'
loc: 267
annotations: []
imports:
- datetime
- pytest
exports:
- _register
- TestRequestEmailVerification
- TestConfirmEmailVerification
- TestAntiEnumeration
tags:
- code
- python
---

# tests/server/test_email_verify_routes.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_email_verify_routes.py`](../../../tests/server/test_email_verify_routes.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 — Email verification routes (POST /auth/verify-email/request + /confirm).

Tests RED-first contre `server.routers.auth` (4 nouveaux endpoints V2.3.1).

Spec: docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md
"""
from __future__ import annotations

from datetime import datetime, timedelta, timezone

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_v231_env(monkeypatch):
    """V2.3.1 — env vars boot (PUBLIC_BASE_URL + EMAIL_HASH_SALT)."""
    monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", "http://localhost:8000")
    monkeypatch.setenv(
        "SAMSUNGHEALTH_EMAIL_HASH_SALT",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    )
    # Default flag = false (warning only on login of unverified).
    monkeypatch.setenv("SAMSUNGHEALTH_REQUIRE_EMAIL_VERIFICATION", "false")


def _register(client, email="verify@example.com", password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


class TestRequestEmailVerification:
    def test_request_returns_202_for_known_user(self, client_pg_ready):
        """given a registered email, when POST /auth/verify-email/request, then 202 + body status=pending.

        spec §Endpoints — POST /auth/verify-email/request → 202 {"status":"pending"}.
        """
        client = client_pg_ready
        reg = _register(client, email="known-verify@example.com")
        assert reg.status_code == 201, f"register failed: {reg.text}"

        r = client.post(
            "/auth/verify-email/request",
            json={"email": "known-verify@example.com"},
        )
        assert r.status_code == 202, f"expected 202, got {r.status_code}: {r.text}"
        assert r.json() == {"status": "pending"}

    def test_request_returns_202_for_unknown_email_anti_enum(self, client_pg_ready):
        """given an unknown email, when POST /auth/verify-email/request, then 202 (anti-énumération).

        spec §Anti-énumération : 202 toujours, qu'on connaisse l'email ou pas.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/verify-email/request",
            json={"email": "ghost-verify@example.com"},
        )
        assert r.status_code == 202, f"expected 202, got {r.status_code}: {r.text}"
        assert r.json() == {"status": "pending"}

    def test_request_emits_email_outbound_log_event_without_link(self, client_pg_ready, db_session):
        """given a known email, when POST /auth/verify-email/request, then 1 row in verification_tokens AND structlog event 'email.outbound' WITHOUT verify_link in payload.

        spec §Email = log + table dédiée — log SANS verify_link, hash to_hash uniquement.
        spec §Test d'acceptation #16 : logs JSONL ne contiennent JAMAIS le verify_link.
        """
        import structlog
        from sqlalchemy import select

        from server.db.models import User, VerificationToken

        client = client_pg_ready
        _register(client, email="logged-verify@example.com")

        with structlog.testing.capture_logs() as logs:
            r = client.post(
                "/auth/verify-email/request",
                json={"email": "logged-verify@example.com"},
            )
        assert r.status_code == 202

        # 1 row in verification_tokens
        user = db_session.execute(
            select(User).where(User.email == "logged-verify@example.com")
        ).scalar_one()
        rows = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "email_verification",
            )
        ).scalars().all()
        assert len(rows) == 1, f"expected 1 verification_token row, got {len(rows)}"

        # structlog event 'email.outbound' captured.
        outbound = [log for log in logs if log.get("event") == "email.outbound"]
        assert len(outbound) >= 1, f"expected email.outbound log event, got: {logs}"
        evt = outbound[-1]
        # No verify_link, no plain email.
        assert "verify_link" not in evt, f"verify_link must NOT be in log payload: {evt}"
        for value in evt.values():
            if isinstance(value, str):
                assert "logged-verify@example.com" not in value, (
                    f"plain email leaked in log: {evt}"
                )

    def test_request_does_not_send_if_already_verified(self, client_pg_ready, db_session):
        """given an email already verified (email_verified_at IS NOT NULL), when POST request, then no new verification_token row inserted.

        spec §Anti-énumération — Si email connu ET déjà vérifié → no-op silencieux.
        """
        from sqlalchemy import select

        from server.db.models import User, VerificationToken

        client = client_pg_ready
        _register(client, email="already-verified@example.com")

        # Force email_verified_at.
        user = db_session.execute(
            select(User).where(User.email == "already-verified@example.com")
        ).scalar_one()
        user.email_verified_at = datetime.now(timezone.utc)
        db_session.commit()

        r = client.post(
            "/auth/verify-email/request",
            json={"email": "already-verified@example.com"},
        )
        assert r.status_code == 202  # toujours 202 (anti-enum)

        # Mais 0 row verification_token (purpose=email_verification) inséré.
        rows = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "email_verification",
            )
        ).scalars().all()
        assert len(rows) == 0, (
            f"already-verified user should NOT generate token, got {len(rows)} rows"
        )


class TestConfirmEmailVerification:
    def _request_and_capture_token(self, client, db_session, email):
        """Helper: register + request + extract raw token via outbound cache."""
        from sqlalchemy import select

        from server.db.models import User, VerificationToken
        from server.security.email_outbound import _outbound_link_cache

        _register(client, email=email)
        client.post("/auth/verify-email/request", json={"email": email})

        user = db_session.execute(select(User).where(User.email == email)).scalar_one()
        row = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "email_verification",
            )
        ).scalar_one()
        # Récup token raw via la cache outbound (TTL 60s).
        raw = _outbound_link_cache.get(row.token_hash)
        assert raw is not None, "raw token should be in outbound cache (fresh request)"
        return raw, user

    def test_confirm_sets_email_verified_at(self, client_pg_ready, db_session):
        """given a fresh verify token, when POST /auth/verify-email/confirm, then 200 + users.email_verified_at IS NOT NULL.

        spec §Side-effects de confirm email verification — email_verified_at = now().
        """
        from sqlalchemy import select

        from server.db.models import User

        client = client_pg_ready
        raw, _ = self._request_and_capture_token(
            client, db_session, "confirm-set@example.com"
        )

        r = client.post("/auth/verify-email/confirm", json={"token": raw})
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        assert r.json() == {"status": "verified"}

        user = db_session.execute(
            select(User).where(User.email == "confirm-set@example.com")
        ).scalar_one()
        assert user.email_verified_at is not None, "email_verified_at must be set"

    def test_confirm_consumes_token_replay_returns_400(self, client_pg_ready, db_session):
        """given a successfully confirmed token, when POST confirm again with same token, then 400 invalid_or_expired.

        spec §Single-use — replay = 400.
        """
        client = client_pg_ready
        raw, _ = self._request_and_capture_token(
            client, db_session, "confirm-replay@example.com"
        )

        first = client.post("/auth/verify-email/confirm", json={"token": raw})
        assert first.status_code == 200

        replay = client.post("/auth/verify-email/confirm", json={"token": raw})
        assert replay.status_code == 400, (
            f"replay should 400, got {replay.status_code}: {replay.text}"
        )
        assert replay.json() == {"detail": "invalid_or_expired"}

    def test_confirm_invalid_token_returns_400(self, client_pg_ready):
        """given a random/non-existent token, when POST confirm, then 400 invalid_or_expired.

        spec §Endpoints — 400 invalid_or_expired sur token inconnu.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/verify-email/confirm",
            json={"token": "this-is-a-bogus-token-that-does-not-exist-43c"},
        )
        assert r.status_code == 400, f"expected 400, got {r.status_code}: {r.text}"
        assert r.json() == {"detail": "invalid_or_expired"}

    def test_confirm_expired_token_returns_400(self, client_pg_ready, db_session):
        """given an expired verify token, when POST confirm, then 400 invalid_or_expired.

        spec §Test d'acceptation #6 — expires_at < now() → 400.
        """
        from sqlalchemy import select

        from server.db.models import User, VerificationToken
        from server.security.auth import (
            generate_verification_token,
            hash_verification_token,
        )

        client = client_pg_ready
        _register(client, email="expired-confirm@example.com")
        user = db_session.execute(
            select(User).where(User.email == "expired-confirm@example.com")
        ).scalar_one()

        raw, _ = generate_verification_token()
        hashed = hash_verification_token(raw)
        now = datetime.now(timezone.utc)
        db_session.add(
            VerificationToken(
                user_id=user.id,
                token_hash=hashed,
                purpose="email_verification",
                issued_at=now - timedelta(hours=48),
                expires_at=now - timedelta(seconds=1),
            )
        )
        db_session.commit()

        r = client.post("/auth/verify-email/confirm", json={"token": raw})
        assert r.status_code == 400
        assert r.json() == {"detail": "invalid_or_expired"}


class TestAntiEnumeration:
    """Coverage class for spec table — actual checks live in TestRequestEmailVerification."""
    pass
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register` (function) — lines 29-34
- `TestRequestEmailVerification` (class) — lines 37-146
- `TestConfirmEmailVerification` (class) — lines 149-262
- `TestAntiEnumeration` (class) — lines 265-267

### Imports
- `datetime`
- `pytest`

### Exports
- `_register`
- `TestRequestEmailVerification`
- `TestConfirmEmailVerification`
- `TestAntiEnumeration`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — classes: `TestRequestEmailVerification`, `TestConfirmEmailVerification`, `TestAntiEnumeration` · methods: `test_request_returns_202_for_known_user`, `test_request_returns_202_for_unknown_email_anti_enum`, `test_request_emits_email_outbound_log_event_without_link`, `test_request_does_not_send_if_already_verified`, `test_confirm_sets_email_verified_at`, `test_confirm_consumes_token_replay_returns_400`, `test_confirm_invalid_token_returns_400`, `test_confirm_expired_token_returns_400`
