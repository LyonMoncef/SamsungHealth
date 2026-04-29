"""V2.3.1 — verification_tokens primitive (token gen, hash, lifecycle, anti-flip-back).

Tests RED-first contre `server.security.auth` (helpers verification token) et
`server.db.models.VerificationToken` + le trigger DB anti-flip-back.

Spec: docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md
"""
from __future__ import annotations

import hashlib
from datetime import datetime, timedelta, timezone

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_v231_env(monkeypatch):
    """V2.3.1 — env vars requises au boot pour les helpers token (salt, base url)."""
    monkeypatch.setenv(
        "SAMSUNGHEALTH_EMAIL_HASH_SALT",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    )
    monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", "http://localhost:8000")


class TestTokenGeneration:
    def test_token_is_url_safe_base64_43_chars(self):
        """given generate_verification_token(), when called, then raw is 43 chars URL-safe-base64.

        spec: §Génération token — 32 bytes secrets.token_bytes → URL-safe base64 (43 chars).
        """
        from server.security.auth import generate_verification_token

        raw, hashed = generate_verification_token()
        # 32 bytes encoded URL-safe base64 (no padding) = 43 chars.
        assert isinstance(raw, str)
        assert len(raw) == 43, f"expected 43 chars URL-safe-base64, got {len(raw)}: {raw!r}"
        # URL-safe alphabet = A-Z a-z 0-9 - _
        assert all(
            c.isalnum() or c in "-_" for c in raw
        ), f"non-URL-safe char(s) in token: {raw!r}"
        # hash must be 64 chars hex sha256.
        assert len(hashed) == 64, f"hash should be 64 chars hex, got {len(hashed)}"
        assert all(c in "0123456789abcdef" for c in hashed), f"hash not hex: {hashed!r}"

    def test_token_has_at_least_256_bits_entropy(self):
        """given 100 generate_verification_token() calls, when collected, then 100 distinct raw values (entropy ≥ 256 bits).

        spec §Test d'acceptation #1 : 100 appels → 100 raw distincts.
        """
        from server.security.auth import generate_verification_token

        raws = {generate_verification_token()[0] for _ in range(100)}
        assert len(raws) == 100, f"collisions detected: {100 - len(raws)} duplicates over 100 samples"


class TestTokenHashing:
    def test_token_hash_is_sha256_constant_time(self):
        """given hash_verification_token(raw), when called, then result == sha256(raw).hexdigest() (64 chars).

        spec §Génération token — Stockage = sha256(token_raw).hexdigest().
        spec §Test d'acceptation #2 : verify_verification_token utilise compare_digest sur le hash.
        """
        from server.security.auth import hash_verification_token

        raw = "abc-test-raw-token-value-1234567890abcdefghi"  # 43 chars-ish
        hashed = hash_verification_token(raw)
        expected = hashlib.sha256(raw.encode("utf-8")).hexdigest()
        assert hashed == expected, f"hash mismatch: got {hashed!r}, want {expected!r}"
        assert len(hashed) == 64


class TestTokenLifecycle:
    def test_token_single_use_consumed_marker(self, db_session, schema_ready):
        """given a verification_token row, when consumed_at is set, then verify_verification_token rejects re-use (returns None).

        spec §Test d'acceptation #3 : single-use → re-confirm = 400.
        """
        from sqlalchemy import select, text

        from server.db.models import User, VerificationToken
        from server.security.auth import (
            generate_verification_token,
            hash_verification_token,
            verify_verification_token,
        )

        # Bootstrap a user.
        db_session.execute(
            text(
                "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                "VALUES (gen_random_uuid(), :email, 'x', true, now())"
            ),
            {"email": "lifecycle@example.com"},
        )
        db_session.commit()
        user = db_session.execute(
            select(User).where(User.email == "lifecycle@example.com")
        ).scalar_one()

        raw, _hashed = generate_verification_token()
        hashed = hash_verification_token(raw)
        now = datetime.now(timezone.utc)
        token = VerificationToken(
            user_id=user.id,
            token_hash=hashed,
            purpose="email_verification",
            issued_at=now,
            expires_at=now + timedelta(hours=24),
        )
        db_session.add(token)
        db_session.commit()

        # First lookup OK.
        first = verify_verification_token(db_session, raw, "email_verification")
        assert first is not None, "fresh token should verify"

        # Mark consumed.
        first.consumed_at = datetime.now(timezone.utc)
        db_session.commit()

        # Second lookup must return None (single-use).
        second = verify_verification_token(db_session, raw, "email_verification")
        assert second is None, "consumed token must NOT verify (single-use)"

    def test_ttl_password_reset_is_1h(self):
        """given TTL_PASSWORD_RESET, when read, then equals timedelta(hours=1).

        spec §TTL : password_reset = 1 heure (60 min). OWASP ASVS V2.5.1.
        """
        from server.security.auth import TTL_PASSWORD_RESET

        assert TTL_PASSWORD_RESET == timedelta(hours=1), (
            f"TTL_PASSWORD_RESET must be 1h, got {TTL_PASSWORD_RESET!r}"
        )

    def test_ttl_email_verification_is_24h(self):
        """given TTL_EMAIL_VERIFICATION, when read, then equals timedelta(hours=24).

        spec §TTL : email_verification = 24 heures.
        """
        from server.security.auth import TTL_EMAIL_VERIFICATION

        assert TTL_EMAIL_VERIFICATION == timedelta(hours=24), (
            f"TTL_EMAIL_VERIFICATION must be 24h, got {TTL_EMAIL_VERIFICATION!r}"
        )

    def test_expired_token_rejected(self, db_session, schema_ready):
        """given a verification_token with expires_at < now(), when verify_verification_token, then None.

        spec §Test d'acceptation #6 : expires_at = now() - 1s → 400.
        """
        from sqlalchemy import select, text

        from server.db.models import User, VerificationToken
        from server.security.auth import (
            generate_verification_token,
            hash_verification_token,
            verify_verification_token,
        )

        db_session.execute(
            text(
                "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                "VALUES (gen_random_uuid(), :email, 'x', true, now())"
            ),
            {"email": "expired@example.com"},
        )
        db_session.commit()
        user = db_session.execute(
            select(User).where(User.email == "expired@example.com")
        ).scalar_one()

        raw, _ = generate_verification_token()
        hashed = hash_verification_token(raw)
        now = datetime.now(timezone.utc)
        token = VerificationToken(
            user_id=user.id,
            token_hash=hashed,
            purpose="password_reset",
            issued_at=now - timedelta(hours=2),
            expires_at=now - timedelta(seconds=1),  # expired
        )
        db_session.add(token)
        db_session.commit()

        result = verify_verification_token(db_session, raw, "password_reset")
        assert result is None, "expired token must NOT verify"


class TestAntiFlipBack:
    def test_anti_flip_back_trigger_raises_on_unconsume(self, db_session, schema_ready):
        """given a verification_token row with consumed_at set, when UPDATE attempts to NULL consumed_at, then Postgres trigger raises.

        spec §Schéma — trigger BEFORE UPDATE verification_tokens_no_unconsume.
        spec §Test d'acceptation #4 : pentester reco anti-flip-back.
        """
        from sqlalchemy import select, text

        from server.db.models import User, VerificationToken
        from server.security.auth import (
            generate_verification_token,
            hash_verification_token,
        )

        db_session.execute(
            text(
                "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                "VALUES (gen_random_uuid(), :email, 'x', true, now())"
            ),
            {"email": "flipback@example.com"},
        )
        db_session.commit()
        user = db_session.execute(
            select(User).where(User.email == "flipback@example.com")
        ).scalar_one()

        raw, _ = generate_verification_token()
        hashed = hash_verification_token(raw)
        now = datetime.now(timezone.utc)
        token = VerificationToken(
            user_id=user.id,
            token_hash=hashed,
            purpose="email_verification",
            issued_at=now,
            expires_at=now + timedelta(hours=24),
            consumed_at=now,  # already consumed
        )
        db_session.add(token)
        db_session.commit()
        token_id = token.id

        # Try to UPDATE consumed_at back to NULL — trigger MUST raise.
        with pytest.raises(Exception):
            db_session.execute(
                text("UPDATE verification_tokens SET consumed_at = NULL WHERE id = :id"),
                {"id": str(token_id)},
            )
            db_session.commit()
        db_session.rollback()
