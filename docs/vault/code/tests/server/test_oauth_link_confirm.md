---
type: code-source
language: python
file_path: tests/server/test_oauth_link_confirm.py
git_blob: 405d98feb403ebc19a4651998179f120ce6c7039
last_synced: '2026-05-06T08:02:35Z'
loc: 223
annotations: []
imports:
- json
- datetime
- pytest
exports:
- _seed_user
- _insert_oauth_link_token
- TestOauthLinkConfirm
tags:
- code
- python
---

# tests/server/test_oauth_link_confirm.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_oauth_link_confirm.py`](../../../tests/server/test_oauth_link_confirm.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.2 — POST /auth/oauth-link/confirm (account linking confirmation flow).

Tests RED-first contre `server.routers.auth_oauth.oauth_link_confirm`:
- Token oauth_link_confirm avec payload (provider+sub+email+raw_claims_filtered)
- TTL 1h, single-use
- Confirm crée identity_providers row, marque token consumed_at
- Replay = 400, expired = 400, invalid = 400

Spec: docs/vault/specs/2026-04-26-v2.3.2-google-oauth.md (#24-#27).
"""
from __future__ import annotations

import json
from datetime import datetime, timedelta, timezone

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


def _seed_user(db_session, email: str):
    from sqlalchemy import select, text

    from server.db.models import User

    db_session.execute(
        text(
            "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
            "VALUES (gen_random_uuid(), :email, 'x', true, now())"
        ),
        {"email": email},
    )
    db_session.commit()
    return db_session.execute(select(User).where(User.email == email)).scalar_one()


def _insert_oauth_link_token(db_session, user, *, provider_sub: str, provider_email: str):
    """Insert a verification_token row with purpose=oauth_link_confirm + payload."""
    from server.db.models import VerificationToken
    from server.security.auth import (
        generate_verification_token,
        hash_verification_token,
    )

    raw, hashed = generate_verification_token()
    now = datetime.now(timezone.utc)
    payload = {
        "provider": "google",
        "provider_sub": provider_sub,
        "provider_email": provider_email,
        "raw_claims_filtered": {
            "sub": provider_sub,
            "email": provider_email,
            "email_verified": True,
            "iss": "https://accounts.google.com",
            "aud": "test-client-id-123.apps.googleusercontent.com",
            "iat": int(now.timestamp()),
            "exp": int(now.timestamp()) + 3600,
        },
    }
    row = VerificationToken(
        user_id=user.id,
        token_hash=hashed,
        purpose="oauth_link_confirm",
        issued_at=now,
        expires_at=now + timedelta(hours=1),
        payload=payload,
    )
    db_session.add(row)
    db_session.commit()
    return raw, row


class TestOauthLinkConfirm:
    def test_confirm_creates_identity_providers_row_and_consumes_token(
        self, client_pg_ready, db_session
    ):
        """given a fresh oauth_link_confirm token (with payload), when POST /auth/oauth-link/confirm {token}, then 200 + identity_providers row created (provider, sub, email) + token consumed_at set + JWT pair returned.

        spec #24 — Confirm valide → identity_providers row + JWT + audit oauth_account_linked.
        """
        from sqlalchemy import select

        from server.db.models import VerificationToken

        client = client_pg_ready
        user = _seed_user(db_session, "link-confirm@example.com")
        raw, token_row = _insert_oauth_link_token(
            db_session,
            user,
            provider_sub="google-sub-link-1",
            provider_email="link-confirm@example.com",
        )

        r = client.post("/auth/oauth-link/confirm", json={"token": raw})
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        body = r.json()
        assert "access_token" in body, f"missing access_token: {body}"
        assert "refresh_token" in body, f"missing refresh_token: {body}"

        # Verify identity_providers row exists.
        from server.db.models import IdentityProvider

        idp_rows = db_session.execute(
            select(IdentityProvider).where(IdentityProvider.user_id == user.id)
        ).scalars().all()
        assert len(idp_rows) == 1, (
            f"expected 1 identity_providers row, got {len(idp_rows)}"
        )
        assert idp_rows[0].provider == "google"
        assert idp_rows[0].provider_sub == "google-sub-link-1"

        # Token consumed.
        db_session.expire_all()
        consumed_token = db_session.execute(
            select(VerificationToken).where(VerificationToken.id == token_row.id)
        ).scalar_one()
        assert consumed_token.consumed_at is not None, "token must be consumed"

    def test_confirm_replay_returns_400(self, client_pg_ready, db_session):
        """given a token already used by /auth/oauth-link/confirm, when re-POST the same token, then 400.

        spec #25 — Replay → 400.
        """
        client = client_pg_ready
        user = _seed_user(db_session, "link-replay@example.com")
        raw, _ = _insert_oauth_link_token(
            db_session,
            user,
            provider_sub="google-sub-link-replay",
            provider_email="link-replay@example.com",
        )
        first = client.post("/auth/oauth-link/confirm", json={"token": raw})
        assert first.status_code == 200, f"first must succeed: {first.text}"

        replay = client.post("/auth/oauth-link/confirm", json={"token": raw})
        assert replay.status_code == 400, (
            f"replay must 400, got {replay.status_code}: {replay.text}"
        )

    def test_confirm_expired_token_returns_400(self, client_pg_ready, db_session):
        """given an oauth_link_confirm token with expires_at in the past, when POST confirm, then 400.

        spec #26 — Expired → 400.
        """
        from server.db.models import VerificationToken
        from server.security.auth import (
            generate_verification_token,
            hash_verification_token,
        )

        client = client_pg_ready
        user = _seed_user(db_session, "link-expired@example.com")

        raw, hashed = generate_verification_token()
        now = datetime.now(timezone.utc)
        db_session.add(
            VerificationToken(
                user_id=user.id,
                token_hash=hashed,
                purpose="oauth_link_confirm",
                issued_at=now - timedelta(hours=2),
                expires_at=now - timedelta(seconds=1),
                payload={
                    "provider": "google",
                    "provider_sub": "google-sub-expired",
                    "provider_email": "link-expired@example.com",
                    "raw_claims_filtered": {},
                },
            )
        )
        db_session.commit()

        r = client.post("/auth/oauth-link/confirm", json={"token": raw})
        assert r.status_code == 400, (
            f"expired token must 400, got {r.status_code}: {r.text}"
        )

    def test_confirm_invalid_token_returns_400(self, client_pg_ready):
        """given a bogus/non-existent token, when POST confirm, then 400.

        spec #27 — Invalid → 400.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/oauth-link/confirm",
            json={"token": "this-token-does-not-exist-anywhere-bogus"},
        )
        assert r.status_code == 400, f"expected 400, got {r.status_code}: {r.text}"

    def test_confirm_wrong_purpose_token_returns_400(
        self, client_pg_ready, db_session
    ):
        """given a token of purpose='password_reset' (not oauth_link_confirm), when POST /auth/oauth-link/confirm, then 400 (purpose strict).

        spec §Stockage du token — purpose='oauth_link_confirm' strict.
        """
        from server.db.models import VerificationToken
        from server.security.auth import (
            generate_verification_token,
            hash_verification_token,
        )

        client = client_pg_ready
        user = _seed_user(db_session, "link-wrongpurpose@example.com")
        raw, hashed = generate_verification_token()
        now = datetime.now(timezone.utc)
        db_session.add(
            VerificationToken(
                user_id=user.id,
                token_hash=hashed,
                purpose="password_reset",  # WRONG purpose
                issued_at=now,
                expires_at=now + timedelta(hours=1),
            )
        )
        db_session.commit()

        r = client.post("/auth/oauth-link/confirm", json={"token": raw})
        assert r.status_code == 400, (
            f"wrong-purpose token must 400, got {r.status_code}: {r.text}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_seed_user` (function) — lines 22-35
- `_insert_oauth_link_token` (function) — lines 38-72
- `TestOauthLinkConfirm` (class) — lines 75-223

### Imports
- `json`
- `datetime`
- `pytest`

### Exports
- `_seed_user`
- `_insert_oauth_link_token`
- `TestOauthLinkConfirm`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — classes: `TestOauthLinkConfirm`
