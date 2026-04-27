"""V2.3.2 — OAuth state CSRF + nonce generation + single-use + LRU cap.

Tests RED-first contre `server.security.auth_providers.state`:
- TestStateGeneration : (jwt, jti, nonce) distincts + JWT décodable + exp 10min
- TestStateValidation : single-use replay 400 + expired 400
- TestStateLruCap : flood > 10_000 → eviction LRU

Spec: docs/vault/specs/2026-04-26-v2.3.2-google-oauth.md (#4-#7).
"""
from __future__ import annotations

import time

import pytest


class TestStateGeneration:
    def test_state_returns_jwt_jti_nonce_distinct(self):
        """given two consecutive calls to generate_oauth_state, when invoked, then 6 distinct values across 2 (jwt, jti, nonce) tuples.

        spec #4 — (jwt, jti, nonce) distincts à chaque appel.
        """
        from server.security.auth_providers.state import generate_oauth_state

        t1 = generate_oauth_state()
        t2 = generate_oauth_state()

        # tuple of 3 each.
        assert len(t1) == 3 and len(t2) == 3
        # All 6 values distinct (each call returns 3 fresh randoms).
        all_values = list(t1) + list(t2)
        assert len(set(all_values)) == 6, (
            f"expected 6 distinct values across 2 calls, got: {all_values}"
        )

    def test_state_jwt_decodable_with_server_secret_and_exp_10min(self, monkeypatch):
        """given a fresh state JWT, when decoded with server JWT secret, then payload contains jti+iat+exp and exp is ≤ 10min in the future.

        spec #4 — JWT décodable avec server secret, exp à 10 min.
        """
        import os

        import jwt as pyjwt

        from server.security.auth_providers.state import generate_oauth_state

        secret = os.environ.get("SAMSUNGHEALTH_JWT_SECRET")
        assert secret, "JWT secret must be set in test env"

        state_jwt, state_jti, _nonce = generate_oauth_state()
        # Decode without iss/aud requirements (state is internal-only).
        payload = pyjwt.decode(
            state_jwt,
            secret,
            algorithms=["HS256"],
            options={"verify_aud": False, "verify_iss": False},
        )
        assert payload.get("jti") == state_jti, "jti claim must match returned jti"
        assert "iat" in payload and "exp" in payload
        ttl = payload["exp"] - payload["iat"]
        assert 0 < ttl <= 600, f"exp must be ≤ 10min from iat, got ttl={ttl}s"


class TestStateValidation:
    def test_state_replay_returns_false_or_raises(self):
        """given a state used once, when verify_oauth_state is called a second time with the same JWT, then returns False/raises (single-use).

        spec #5 — verify deux fois → second échoue (oauth_state_replay).
        """
        from server.security.auth_providers.state import (
            generate_oauth_state,
            verify_oauth_state,
        )

        state_jwt, _jti, nonce = generate_oauth_state()

        first = verify_oauth_state(state_jwt)
        assert first, "first verify must succeed"

        # Second verify must fail (single-use enforced).
        try:
            second = verify_oauth_state(state_jwt)
        except Exception:
            return  # raise is acceptable
        assert not second, f"replay must return falsy/raise, got {second!r}"

    def test_state_expired_returns_false_or_raises(self, monkeypatch):
        """given a forged state JWT with exp in the past, when verify_oauth_state is called, then False/raises.

        spec #6 — exp passé → 400.
        """
        import os
        import time as _time
        import uuid

        import jwt as pyjwt

        from server.security.auth_providers.state import verify_oauth_state

        secret = os.environ.get("SAMSUNGHEALTH_JWT_SECRET")
        assert secret
        now = int(_time.time())
        forged = pyjwt.encode(
            {
                "jti": str(uuid.uuid4()),
                "iat": now - 3600,
                "exp": now - 1,
            },
            secret,
            algorithm="HS256",
        )
        try:
            ok = verify_oauth_state(forged)
        except Exception:
            return
        assert not ok, f"expired state must fail, got {ok!r}"


class TestStateLruCap:
    def test_state_lru_cap_evicts_oldest_when_flooded(self, monkeypatch):
        """given a generate_oauth_state cap monkeypatched to 5, when called 10x, then cache size remains ≤ 5 (LRU eviction).

        spec #7 — DoS cap: len(cache) ≤ 10_000, eviction LRU si dépassé.
        """
        from server.security.auth_providers import state as state_mod

        # Reset / cap monkeypatch.
        if hasattr(state_mod, "_OAUTH_STATE_CACHE"):
            try:
                state_mod._OAUTH_STATE_CACHE.clear()
            except Exception:
                pass
        # Cap monkeypatch (impl uses a module-level constant or attr).
        monkeypatch.setattr(state_mod, "_OAUTH_STATE_CACHE_MAX", 5, raising=False)

        for _ in range(10):
            state_mod.generate_oauth_state()

        size = len(state_mod._OAUTH_STATE_CACHE)
        assert size <= 5, f"cache must be capped at 5, got size={size}"
