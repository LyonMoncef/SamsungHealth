"""V2.3 — Argon2id password hashing.

Tests RED-first contre `server.security.auth`:
- TestArgon2Params : algo + RFC 9106 profile #2 params
- TestVerifyPassword : verify happy/unhappy
- TestTimingEqualization : dummy hash + timing equalization

Spec: docs/vault/specs/2026-04-26-v2-auth-foundation.md (#1-#6)
"""
from __future__ import annotations

import time
import statistics

import pytest


class TestArgon2Params:
    def test_hash_uses_argon2id(self):
        """given a plain password, when hash_password runs, then the encoded hash starts with $argon2id$.

        spec #1 — argon2id obligatoire (pas argon2i ni argon2d).
        """
        from server.security.auth import hash_password

        encoded = hash_password("plaintext-password")
        assert isinstance(encoded, str)
        assert encoded.startswith("$argon2id$"), f"expected argon2id prefix, got: {encoded[:30]}"

    def test_hash_params_match_rfc9106_profile2(self):
        """given a fresh hash, when parsed, then params match m=46080,t=2,p=1 (RFC 9106 profile #2).

        spec #2 — pentester reco : single-thread, ~250-350ms wall clock.
        """
        from server.security.auth import hash_password

        encoded = hash_password("any-password")
        # Format: $argon2id$v=19$m=46080,t=2,p=1$<salt>$<hash>
        assert "m=46080" in encoded, f"memory_cost mismatch in: {encoded}"
        assert "t=2" in encoded, f"time_cost mismatch in: {encoded}"
        assert "p=1" in encoded, f"parallelism mismatch in: {encoded}"


class TestVerifyPassword:
    def test_verify_correct_password(self):
        """given hash of password X, when verify(X, hash), then True.

        spec #3.
        """
        from server.security.auth import hash_password, verify_password

        encoded = hash_password("correct-horse-battery-staple")
        assert verify_password("correct-horse-battery-staple", encoded) is True

    def test_verify_wrong_password_returns_false(self):
        """given hash of password X, when verify(Y, hash), then False (no exception).

        spec #4.
        """
        from server.security.auth import hash_password, verify_password

        encoded = hash_password("right-password")
        assert verify_password("wrong-password", encoded) is False


class TestTimingEqualization:
    def test_verify_nonexistent_user_runs_dummy_hash(self):
        """given _DUMMY_HASH module-level constant, when verify_password(plain, _DUMMY_HASH), then runs without raising and returns False.

        spec #5 — dummy hash precomputed at module load. Used by login when user is unknown
        so the wall-clock wrong-user vs wrong-password is indistinguishable (timing equalization).
        """
        from server.security.auth import _DUMMY_HASH, verify_password

        # _DUMMY_HASH is a real argon2id encoded hash — must be present and well-formed.
        assert isinstance(_DUMMY_HASH, str)
        assert _DUMMY_HASH.startswith("$argon2id$"), f"_DUMMY_HASH must be argon2id: {_DUMMY_HASH[:30]}"

        # verify against an arbitrary plain — must complete (no raise) and return False
        # (since the plain doesn't match the dummy salt+hash).
        result = verify_password("any-plain-password", _DUMMY_HASH)
        assert result is False

    def test_verify_constant_time_within_tolerance(self):
        """given hash of real password and _DUMMY_HASH, when verifying wrong-pwd vs dummy 10× each, then median wall_ms ratio < 1.5.

        spec #6 — constant-time login (10 runs each, median ratio ∈ [~0.5, 1.5]).
        """
        from server.security.auth import _DUMMY_HASH, hash_password, verify_password

        real_hash = hash_password("real-password")

        wrong_pwd_times: list[float] = []
        dummy_times: list[float] = []

        # Warm-up to avoid first-call overhead (lib init, etc.)
        verify_password("warm", real_hash)
        verify_password("warm", _DUMMY_HASH)

        for _ in range(10):
            t0 = time.perf_counter()
            verify_password("wrong-pwd", real_hash)
            wrong_pwd_times.append(time.perf_counter() - t0)

            t0 = time.perf_counter()
            verify_password("wrong-pwd", _DUMMY_HASH)
            dummy_times.append(time.perf_counter() - t0)

        med_real = statistics.median(wrong_pwd_times)
        med_dummy = statistics.median(dummy_times)
        ratio = max(med_real, med_dummy) / max(min(med_real, med_dummy), 1e-9)
        assert ratio < 1.5, f"timing ratio too large: {ratio:.3f} (real={med_real*1000:.1f}ms, dummy={med_dummy*1000:.1f}ms)"
