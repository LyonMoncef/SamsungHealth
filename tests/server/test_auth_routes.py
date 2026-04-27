"""V2.3 — Auth routes (register, login, refresh, logout).

Tests RED-first contre `server.routers.auth` (montés dans `server.main.app`):
- TestRegister : admin token, success, duplicate, invalid token
- TestLogin : success, identical 401 message on wrong-pwd vs unknown user, timing
- TestRefresh : rotation + revoked
- TestLogout : revokes + idempotent
- TestUserEnumeration : implicit via identical 401 messages

Spec: docs/vault/specs/2026-04-26-v2-auth-foundation.md (#17-#28)
"""
from __future__ import annotations

import statistics
import time

import pytest


_TEST_JWT_SECRET = "dGVzdC1qd3Qtc2VjcmV0LXdpdGgtMzItYnl0ZXMtbWluLW9rITE="
_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_auth_env(monkeypatch):
    """Set JWT + registration env per test (autouse — auth routes need both)."""
    monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)
    monkeypatch.setenv("SAMSUNGHEALTH_REGISTRATION_TOKEN", _TEST_REGISTRATION_TOKEN)


class TestRegister:
    def test_register_requires_admin_token(self, client_pg_ready):
        """given POST /auth/register without X-Registration-Token header, when called, then 403 registration_disabled.

        spec #17.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/register",
            json={"email": "alice@example.com", "password": "longpassword12345"},
        )
        assert r.status_code == 403, f"expected 403, got {r.status_code}: {r.text}"

    def test_register_creates_user_returns_201(self, client_pg_ready):
        """given POST /auth/register with valid token + body, when called, then 201 + user row + auth_event.

        spec #18.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": "alice@example.com", "password": "longpassword12345"},
        )
        assert r.status_code == 201, f"expected 201, got {r.status_code}: {r.text}"
        body = r.json()
        assert "id" in body
        assert body["email"] == "alice@example.com"

    def test_register_duplicate_email_409(self, client_pg_ready):
        """given an already-registered email, when re-POST /auth/register, then 409.

        spec #19.
        """
        client = client_pg_ready
        headers = {"X-Registration-Token": _TEST_REGISTRATION_TOKEN}
        body = {"email": "dup@example.com", "password": "longpassword12345"}
        first = client.post("/auth/register", headers=headers, json=body)
        assert first.status_code == 201, f"first register failed: {first.text}"
        second = client.post("/auth/register", headers=headers, json=body)
        assert second.status_code == 409, f"expected 409 on dup, got {second.status_code}: {second.text}"

    def test_register_invalid_token_403(self, client_pg_ready):
        """given POST /auth/register with wrong X-Registration-Token, when called, then 403.

        spec #20.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/register",
            headers={"X-Registration-Token": "wrong-token"},
            json={"email": "alice@example.com", "password": "longpassword12345"},
        )
        assert r.status_code == 403


class TestLogin:
    def _register(self, client, email="user@example.com", password="longpassword12345"):
        return client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": email, "password": password},
        )

    def test_login_returns_access_and_refresh(self, client_pg_ready):
        """given a registered user, when POST /auth/login with correct creds, then 200 with access + refresh tokens.

        spec #21.
        """
        client = client_pg_ready
        self._register(client)
        r = client.post(
            "/auth/login",
            json={"email": "user@example.com", "password": "longpassword12345"},
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        body = r.json()
        assert "access_token" in body
        assert "refresh_token" in body
        assert body.get("token_type") == "bearer"

    def test_login_wrong_password_401_identical_message(self, client_pg_ready):
        """given a registered user, when POST /auth/login with wrong password, then 401 + body {"detail": "invalid_credentials"}.

        spec #22.
        """
        client = client_pg_ready
        self._register(client)
        r = client.post(
            "/auth/login",
            json={"email": "user@example.com", "password": "wrongpassword12345"},
        )
        assert r.status_code == 401
        assert r.json() == {"detail": "invalid_credentials"}

    def test_login_unknown_user_401_identical_message(self, client_pg_ready):
        """given an unknown email, when POST /auth/login, then 401 + body identical to wrong-password case.

        spec #23 — no user enumeration.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/login",
            json={"email": "ghost@example.com", "password": "anypassword12345"},
        )
        assert r.status_code == 401
        assert r.json() == {"detail": "invalid_credentials"}

    @pytest.mark.skip(
        reason=(
            "V2.3.3.1 soft backoff (sleep exponentiel sur wrong password) brise "
            "volontairement l'égalité de timing avec la branche user inexistant. "
            "Trade-off documenté dans "
            "docs/vault/specs/2026-04-26-v2.3.3.1-rate-limit-lockout.md "
            "§Anti-énumération."
        )
    )
    def test_login_response_time_constant_within_tolerance(self, client_pg_ready):
        """given an unknown email vs a registered user with wrong-pwd, when POST /auth/login 10× each, then median ratio < 1.5.

        spec V2.3 #24 — timing equalization (dummy hash).
        SUPERSEDED par V2.3.3.1 soft backoff.
        """
        client = client_pg_ready
        reg = self._register(client, email="timing@example.com")
        assert reg.status_code == 201, f"register precondition failed: {reg.status_code} {reg.text}"

        # Warm-up
        client.post("/auth/login", json={"email": "timing@example.com", "password": "wrongwrongwrong1"})
        client.post("/auth/login", json={"email": "ghost@example.com", "password": "wrongwrongwrong1"})

        wrong_pwd_times: list[float] = []
        unknown_times: list[float] = []
        for _ in range(10):
            t0 = time.perf_counter()
            client.post("/auth/login", json={"email": "timing@example.com", "password": "wrongwrongwrong1"})
            wrong_pwd_times.append(time.perf_counter() - t0)

            t0 = time.perf_counter()
            client.post("/auth/login", json={"email": "ghost@example.com", "password": "wrongwrongwrong1"})
            unknown_times.append(time.perf_counter() - t0)

        med_wrong = statistics.median(wrong_pwd_times)
        med_unknown = statistics.median(unknown_times)
        ratio = max(med_wrong, med_unknown) / max(min(med_wrong, med_unknown), 1e-9)
        assert ratio < 1.5, f"timing ratio too large: {ratio:.3f} (wrong_pwd={med_wrong*1000:.1f}ms, unknown={med_unknown*1000:.1f}ms)"


class TestRefresh:
    def _register_login(self, client, email="refresh@example.com", password="longpassword12345"):
        client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": email, "password": password},
        )
        r = client.post("/auth/login", json={"email": email, "password": password})
        return r.json()

    def test_refresh_rotates_token(self, client_pg_ready):
        """given a valid refresh token, when POST /auth/refresh, then a new refresh is issued and the old one is revoked.

        spec #25 — rotation : ancien JTI marqué revoked, replay du vieux → 401.
        """
        client = client_pg_ready
        tokens = self._register_login(client)
        old_refresh = tokens["refresh_token"]

        r = client.post("/auth/refresh", json={"refresh_token": old_refresh})
        assert r.status_code == 200, f"refresh failed: {r.text}"
        new_tokens = r.json()
        assert "access_token" in new_tokens
        assert "refresh_token" in new_tokens
        assert new_tokens["refresh_token"] != old_refresh, "refresh must be rotated (new opaque jti)"

        # Replay the old refresh — must fail.
        replay = client.post("/auth/refresh", json={"refresh_token": old_refresh})
        assert replay.status_code == 401, f"replay should fail with 401, got {replay.status_code}"

    def test_refresh_revoked_token_401(self, client_pg_ready):
        """given a refresh token that has been rotated (revoked), when reused, then 401 invalid_refresh.

        spec #26.
        """
        client = client_pg_ready
        tokens = self._register_login(client, email="rev@example.com")
        old = tokens["refresh_token"]

        # First refresh — rotates (revokes old).
        client.post("/auth/refresh", json={"refresh_token": old})
        # Second use of old → revoked.
        r = client.post("/auth/refresh", json={"refresh_token": old})
        assert r.status_code == 401


class TestLogout:
    def _register_login(self, client, email="logout@example.com", password="longpassword12345"):
        client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": email, "password": password},
        )
        r = client.post("/auth/login", json={"email": email, "password": password})
        return r.json()

    def test_logout_revokes_refresh(self, client_pg_ready):
        """given a logged-in user, when POST /auth/logout with refresh_token, then 204 + the refresh is no longer usable.

        spec #27.
        """
        client = client_pg_ready
        tokens = self._register_login(client)
        access = tokens["access_token"]
        refresh = tokens["refresh_token"]

        r = client.post(
            "/auth/logout",
            headers={"Authorization": f"Bearer {access}"},
            json={"refresh_token": refresh},
        )
        assert r.status_code == 204, f"expected 204, got {r.status_code}: {r.text}"

        # Refresh is now revoked.
        replay = client.post("/auth/refresh", json={"refresh_token": refresh})
        assert replay.status_code == 401

    def test_logout_idempotent(self, client_pg_ready):
        """given an already-revoked refresh token, when POST /auth/logout again, then 204 (idempotent).

        spec #28.
        """
        client = client_pg_ready
        tokens = self._register_login(client, email="idemp@example.com")
        access = tokens["access_token"]
        refresh = tokens["refresh_token"]

        # First logout
        first = client.post(
            "/auth/logout",
            headers={"Authorization": f"Bearer {access}"},
            json={"refresh_token": refresh},
        )
        assert first.status_code == 204
        # Second logout — must also return 204 (idempotent).
        second = client.post(
            "/auth/logout",
            headers={"Authorization": f"Bearer {access}"},
            json={"refresh_token": refresh},
        )
        assert second.status_code == 204


class TestUserEnumeration:
    """Wrapper class for spec table — actual checks live in TestLogin (#22 + #23)."""
    pass
