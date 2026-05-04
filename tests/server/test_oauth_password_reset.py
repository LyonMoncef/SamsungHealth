"""V2.3.2 — Sentinel password OAuth-only + reset/request silent on OAuth-only.

Tests RED-first contre `server.security.auth` (sentinel) + `server.routers.auth` (reset).

Spec: docs/vault/specs/2026-04-26-v2.3.2-google-oauth.md (#38, #39).
"""
from __future__ import annotations

import time

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


class TestSentinelConstantTime:
    def test_verify_password_with_sentinel_returns_false_no_raise(self):
        """given verify_password called with stored_hash == OAUTH_SENTINEL, when invoked, then returns False without raising (constant-time-ish via jitter).

        spec #38 — court-circuit explicite, jitter 80-120ms, return False.
        """
        from server.security.auth import OAUTH_SENTINEL, verify_password

        # Should not raise (argon2 would raise on non-parseable hash) and must return False.
        result = verify_password("any-plain-password", OAUTH_SENTINEL)
        assert result is False, f"expected False on sentinel, got {result!r}"

    def test_verify_password_sentinel_jitter_min_80ms(self):
        """given verify_password(plain, OAUTH_SENTINEL), when invoked 3x, then median wall-clock ≥ 80ms (jitter active).

        spec §Sentinel password — jitter random.uniform(0.080, 0.120) constant-time-ish.
        """
        import statistics

        from server.security.auth import OAUTH_SENTINEL, verify_password

        # Warm-up.
        verify_password("warm", OAUTH_SENTINEL)
        timings = []
        for _ in range(3):
            t0 = time.perf_counter()
            verify_password("any-test-pwd", OAUTH_SENTINEL)
            timings.append(time.perf_counter() - t0)
        med = statistics.median(timings)
        assert med >= 0.080, (
            f"sentinel jitter must be ≥ 80ms median, got {med*1000:.1f}ms"
        )


class TestResetOnOauthOnly:
    def test_password_reset_request_on_oauth_only_user_silent_no_token(
        self, client_pg_ready, db_session
    ):
        """given a user with password_hash == OAUTH_SENTINEL (OAuth-only), when POST /auth/password/reset/request, then 202 silent + 0 verification_tokens row created (skipped silently).

        spec #39 — reset request sur OAuth-only user retourne 202 anti-enum MAIS aucun token n'est émis.
        """
        from sqlalchemy import select, text

        from server.db.models import User, VerificationToken
        from server.security.auth import OAUTH_SENTINEL

        client = client_pg_ready
        # Create user directly in DB with sentinel hash.
        with db_session.begin():
            db_session.execute(
                text(
                    "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
                    "VALUES (gen_random_uuid(), :email, :h, true, now())"
                ),
                {"email": "oauth-only@example.com", "h": OAUTH_SENTINEL},
            )

        r = client.post(
            "/auth/password/reset/request",
            json={"email": "oauth-only@example.com"},
        )
        assert r.status_code == 202, f"expected 202 silent, got {r.status_code}: {r.text}"
        assert r.json() == {"status": "pending"}

        # Verify NO verification_token row was created for this user.
        user = db_session.execute(
            select(User).where(User.email == "oauth-only@example.com")
        ).scalar_one()
        rows = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "password_reset",
            )
        ).scalars().all()
        assert len(rows) == 0, (
            f"OAuth-only user must NOT receive a reset token, got {len(rows)} rows"
        )
