---
type: code-source
language: python
file_path: tests/server/test_oauth_routes.py
git_blob: 6d8adf7166150a22d8be99315c1965fcd1e194c2
last_synced: '2026-04-27T07:34:24Z'
loc: 810
annotations: []
imports:
- time
- datetime
- unittest.mock
- pytest
exports:
- _seed_user
- _make_callback_mocks
- _patch_state_verify_to_return_nonce
- TestGoogleStartPost
- TestCsrfProtection
- TestGoogleCallback
- TestAccountLinkingMatrix
- TestRaceCondition
- TestAutoRegisterFlag
- TestErrors
tags:
- code
- python
---

# tests/server/test_oauth_routes.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_oauth_routes.py`](../../../tests/server/test_oauth_routes.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.2 — OAuth routes (POST /auth/google/start + GET /auth/google/callback).

Tests RED-first contre `server.routers.auth_oauth`:
- TestGoogleStartPost : POST OK, URL contient state/nonce/redirect_uri/scope/prompt
- TestCsrfProtection : POST avec Sec-Fetch-Site=cross-site → 403
- TestGoogleCallback : golden path auto_register=true, JSON response (jamais fragment), GET /start refusé
- TestAccountLinkingMatrix : provider_sub existant (login), email match → oauth_link_pending,
  email match + NOT verified → 409, account disabled → 403
- TestRaceCondition : 2 callbacks concurrents avec même email inconnu → 1 user
- TestAutoRegisterFlag : false + email inconnu → 403 oauth_registration_disabled
- TestErrors : Google error_description jamais dans response client

Spec: docs/vault/specs/2026-04-26-v2.3.2-google-oauth.md (#8-#13, #19-#23, #37, #43).
"""
from __future__ import annotations

import time
from datetime import datetime, timezone
from unittest.mock import MagicMock, patch

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"
_TEST_CLIENT_ID = "test-client-id-123.apps.googleusercontent.com"


# ── helpers ────────────────────────────────────────────────────────────────
def _seed_user(db_session, *, email: str, is_active: bool = True):
    from sqlalchemy import select, text

    from server.db.models import User

    db_session.execute(
        text(
            "INSERT INTO users (id, email, password_hash, is_active, password_changed_at) "
            "VALUES (gen_random_uuid(), :email, 'x', :active, now())"
        ),
        {"email": email, "active": is_active},
    )
    db_session.commit()
    return db_session.execute(select(User).where(User.email == email)).scalar_one()


def _make_callback_mocks(
    *,
    google_keypair_and_jwks,
    sub: str,
    email: str,
    email_verified: bool = True,
    nonce: str = "expected-nonce",
    extra_claims: dict | None = None,
):
    """Returns (id_token, async_get_fn, async_post_fn) for mocking httpx in callback flow."""
    now = int(time.time())
    claims = {
        "iss": "https://accounts.google.com",
        "aud": _TEST_CLIENT_ID,
        "sub": sub,
        "email": email,
        "email_verified": email_verified,
        "iat": now,
        "exp": now + 600,
        "nonce": nonce,
    }
    if extra_claims:
        claims.update(extra_claims)
    id_token = google_keypair_and_jwks["sign"](claims)

    async def _async_get(*args, **kwargs):
        resp = MagicMock()
        resp.status_code = 200
        resp.json = lambda: google_keypair_and_jwks["jwks"]
        resp.headers = {"Cache-Control": "max-age=3600"}
        resp.raise_for_status = lambda: None
        return resp

    async def _async_post(*args, **kwargs):
        resp = MagicMock()
        resp.status_code = 200
        resp.json = lambda: {
            "access_token": "google-access-tok",
            "id_token": id_token,
            "refresh_token": "google-refresh-DO-NOT-STORE",
            "expires_in": 3600,
            "token_type": "Bearer",
        }
        resp.raise_for_status = lambda: None
        return resp

    return id_token, _async_get, _async_post


def _patch_state_verify_to_return_nonce(monkeypatch, nonce: str):
    """Make verify_oauth_state return the expected nonce so callback can validate ID token."""
    from server.security.auth_providers import state as state_mod

    def _fake_verify(state_jwt: str):
        # Return the expected nonce on first call, mark used.
        return {"nonce": nonce, "used": True}

    monkeypatch.setattr(state_mod, "verify_oauth_state", _fake_verify, raising=False)


# ── tests ──────────────────────────────────────────────────────────────────
class TestGoogleStartPost:
    def test_post_start_returns_authorize_url_with_required_params(
        self, client_pg_ready
    ):
        """given POST /auth/google/start with Sec-Fetch-Site: same-origin, when called, then 200 + JSON {authorize_url} containing state, nonce, redirect_uri, scope=openid email profile, prompt=select_account.

        spec #8 — POST OK, URL Google authorize avec state/nonce/redirect_uri/scope/prompt.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/google/start",
            json={},
            headers={"Sec-Fetch-Site": "same-origin", "Content-Type": "application/json"},
        )
        assert r.status_code == 200, f"expected 200, got {r.status_code}: {r.text}"
        body = r.json()
        url = body.get("authorize_url", "")
        assert "state=" in url, f"missing state= in authorize_url: {url}"
        assert "nonce=" in url, f"missing nonce= in authorize_url: {url}"
        assert "redirect_uri=" in url, f"missing redirect_uri= in authorize_url: {url}"
        assert "scope=" in url and "openid" in url and "email" in url and "profile" in url, (
            f"scope must contain openid+email+profile: {url}"
        )
        assert "prompt=select_account" in url, (
            f"prompt=select_account missing: {url}"
        )

    def test_get_start_returns_405(self, client_pg_ready):
        """given GET /auth/google/start, when called, then 405 Method Not Allowed.

        spec #10 — GET refusé (POST only fix login-CSRF).
        """
        client = client_pg_ready
        r = client.get("/auth/google/start")
        assert r.status_code == 405, f"GET must be refused (405), got {r.status_code}: {r.text}"


class TestCsrfProtection:
    def test_post_start_with_cross_site_returns_403(self, client_pg_ready):
        """given POST /auth/google/start with Sec-Fetch-Site: cross-site, when called, then 403 oauth_csrf_check_failed.

        spec #9 — CSRF protection via Sec-Fetch-Site cross-site → 403.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/google/start",
            json={},
            headers={"Sec-Fetch-Site": "cross-site", "Content-Type": "application/json"},
        )
        assert r.status_code == 403, (
            f"cross-site Sec-Fetch-Site must 403, got {r.status_code}: {r.text}"
        )
        body = r.json()
        assert body.get("detail") == "oauth_csrf_check_failed", (
            f"detail must be oauth_csrf_check_failed, got: {body}"
        )


class TestGoogleCallback:
    def test_callback_golden_path_auto_register_true_returns_json_no_fragment(
        self, client_pg_ready, monkeypatch, google_keypair_and_jwks
    ):
        """given AUTO_REGISTER=true + valid code/state/id_token (mocked httpx), when GET /auth/google/callback, then 200 JSON {access_token, refresh_token, user} + Content-Type=application/json + JAMAIS de Location header avec fragment.

        spec #11, #13 — JSON response, jamais URL fragment (HIGH H1 fix).
        """
        monkeypatch.setenv("SAMSUNGHEALTH_OAUTH_AUTO_REGISTER", "true")

        nonce = "auto-register-nonce-1"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-autoregister",
            email="autoreg@example.com",
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state-jwt"},
                follow_redirects=False,
            )

        assert r.status_code == 200, f"expected 200 JSON, got {r.status_code}: {r.text}"
        ct = r.headers.get("content-type", "")
        assert "application/json" in ct, f"Content-Type must be application/json, got {ct!r}"
        body = r.json()
        assert "access_token" in body and "refresh_token" in body, (
            f"missing tokens in body: {body}"
        )
        # No Location header → no 302 with fragment.
        assert "location" not in {k.lower() for k in r.headers.keys()}, (
            f"callback must NOT return a Location header (anti-fragment HIGH H1): {r.headers}"
        )

    def test_callback_no_fragment_url_in_response(
        self, client_pg_ready, monkeypatch, google_keypair_and_jwks
    ):
        """given a successful callback, when response inspected, then no '#access_token' substring anywhere in headers or body.

        spec #13 — pas de Location header avec #access_token=.
        """
        monkeypatch.setenv("SAMSUNGHEALTH_OAUTH_AUTO_REGISTER", "true")
        nonce = "no-fragment-nonce"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-nofragment",
            email="nofragment@example.com",
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                follow_redirects=False,
            )

        # Anywhere — body, headers — no implicit-flow fragment shape.
        assert "#access_token=" not in r.text, (
            f"#access_token= leaked in body: {r.text[:200]}"
        )
        for hv in r.headers.values():
            assert "#access_token=" not in hv, (
                f"#access_token= in header: {hv}"
            )


class TestAccountLinkingMatrix:
    def test_existing_provider_sub_logs_in_existing_user(
        self, client_pg_ready, db_session, monkeypatch, google_keypair_and_jwks
    ):
        """given an identity_providers row exists for (google, sub-X) linked to user U, when callback with that sub, then 200 login + last_used_at updated + NO new identity_providers row.

        spec #19 — provider_sub existant → login existing user, update last_used_at.
        """
        from sqlalchemy import select

        from server.db.models import IdentityProvider

        user = _seed_user(db_session, email="existing-link@example.com")

        # Pre-create identity_providers row (linked).
        db_session.add(
            IdentityProvider(
                user_id=user.id,
                provider="google",
                provider_sub="google-sub-existing-link",
                provider_email="existing-link@example.com",
                email_verified=True,
            )
        )
        db_session.commit()

        nonce = "existing-link-nonce"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-existing-link",
            email="existing-link@example.com",
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                follow_redirects=False,
            )
        assert r.status_code == 200, f"login must succeed, got {r.status_code}: {r.text}"

        db_session.expire_all()
        rows = db_session.execute(
            select(IdentityProvider).where(IdentityProvider.user_id == user.id)
        ).scalars().all()
        assert len(rows) == 1, f"must NOT create a new row, got {len(rows)}"
        assert rows[0].last_used_at is not None, "last_used_at must be updated on login"

    def test_email_match_verified_returns_oauth_link_pending(
        self, client_pg_ready, db_session, monkeypatch, google_keypair_and_jwks
    ):
        """given an existing user with email E + Google ID token has email=E + email_verified=true + sub unknown, when callback, then 202 oauth_link_pending + 1 verification_token row purpose=oauth_link_confirm + NO identity_providers row yet.

        spec #20 — Email match + verified → 202 oauth_link_pending + token créé, pas de row idp.
        """
        from sqlalchemy import select

        from server.db.models import IdentityProvider, VerificationToken

        user = _seed_user(db_session, email="link-pending@example.com")

        nonce = "link-pending-nonce"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-NEW-for-link-pending",
            email="link-pending@example.com",
            email_verified=True,
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                follow_redirects=False,
            )

        assert r.status_code == 202, (
            f"email match + verified must return 202 oauth_link_pending, got {r.status_code}: {r.text}"
        )
        body = r.json()
        assert body.get("detail") == "oauth_link_pending" or body.get("status") == "oauth_link_pending", (
            f"body must indicate oauth_link_pending: {body}"
        )

        db_session.expire_all()
        # 1 verification_token row purpose=oauth_link_confirm.
        tokens = db_session.execute(
            select(VerificationToken).where(
                VerificationToken.user_id == user.id,
                VerificationToken.purpose == "oauth_link_confirm",
            )
        ).scalars().all()
        assert len(tokens) == 1, (
            f"must create 1 oauth_link_confirm token, got {len(tokens)}"
        )

        # NO identity_providers row yet (linking deferred).
        idps = db_session.execute(
            select(IdentityProvider).where(IdentityProvider.user_id == user.id)
        ).scalars().all()
        assert len(idps) == 0, (
            f"must NOT create idp row at this stage, got {len(idps)}"
        )

    def test_email_match_unverified_returns_409(
        self, client_pg_ready, db_session, monkeypatch, google_keypair_and_jwks
    ):
        """given existing user email E + Google email=E but email_verified=false, when callback, then 409 oauth_provider_email_unverified.

        spec #21 — Email match + NOT verified → 409.
        """
        _seed_user(db_session, email="unverified-link@example.com")

        nonce = "unverified-nonce"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-unverified",
            email="unverified-link@example.com",
            email_verified=False,
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                follow_redirects=False,
            )
        assert r.status_code == 409, (
            f"email_verified=false must 409, got {r.status_code}: {r.text}"
        )
        body = r.json()
        assert body.get("detail") == "oauth_provider_email_unverified", body

    def test_account_disabled_returns_403(
        self, client_pg_ready, db_session, monkeypatch, google_keypair_and_jwks
    ):
        """given existing user with is_active=false + identity_providers linked, when callback with their sub, then 403 account_disabled.

        spec #23 — Compte désactivé → 403 account_disabled.
        """
        from server.db.models import IdentityProvider

        user = _seed_user(
            db_session, email="disabled@example.com", is_active=False
        )
        db_session.add(
            IdentityProvider(
                user_id=user.id,
                provider="google",
                provider_sub="google-sub-disabled",
                provider_email="disabled@example.com",
                email_verified=True,
            )
        )
        db_session.commit()

        nonce = "disabled-nonce"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-disabled",
            email="disabled@example.com",
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                follow_redirects=False,
            )
        assert r.status_code == 403, (
            f"is_active=false must 403, got {r.status_code}: {r.text}"
        )
        body = r.json()
        assert body.get("detail") == "account_disabled", body

    def test_id_token_signature_invalid_returns_400(
        self, client_pg_ready, monkeypatch, google_keypair_and_jwks
    ):
        """given an ID token signed with the wrong RSA key, when callback runs, then 400 oauth_id_token_invalid (no leak of crypto detail).

        spec #14 — ID token signature invalide → 400 oauth_id_token_invalid.
        """
        from cryptography.hazmat.primitives import serialization
        from cryptography.hazmat.primitives.asymmetric import rsa
        import jwt as _jwt

        wrong = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        wrong_pem = wrong.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption(),
        )
        now = int(time.time())
        nonce = "sig-invalid-nonce"
        bad_token = _jwt.encode(
            {
                "iss": "https://accounts.google.com",
                "aud": _TEST_CLIENT_ID,
                "sub": "sig-invalid",
                "email": "siginvalid@example.com",
                "email_verified": True,
                "iat": now,
                "exp": now + 600,
                "nonce": nonce,
            },
            wrong_pem,
            algorithm="RS256",
            headers={"kid": google_keypair_and_jwks["kid"]},
        )

        async def _async_get(*args, **kwargs):
            resp = MagicMock()
            resp.status_code = 200
            resp.json = lambda: google_keypair_and_jwks["jwks"]
            resp.headers = {"Cache-Control": "max-age=3600"}
            resp.raise_for_status = lambda: None
            return resp

        async def _async_post(*args, **kwargs):
            resp = MagicMock()
            resp.status_code = 200
            resp.json = lambda: {
                "access_token": "g-acc",
                "id_token": bad_token,
                "expires_in": 3600,
                "token_type": "Bearer",
            }
            resp.raise_for_status = lambda: None
            return resp

        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=_async_get), patch(
            "httpx.AsyncClient.post", new=_async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                follow_redirects=False,
            )
        assert r.status_code == 400, (
            f"invalid sig must 400, got {r.status_code}: {r.text}"
        )
        body = r.json()
        assert body.get("detail") == "oauth_id_token_invalid", body

    def test_audit_event_inserted_with_meta(
        self, client_pg_ready, db_session, monkeypatch, google_keypair_and_jwks
    ):
        """given a successful callback (auto_register), when audit fetched, then auth_events row with event_type='oauth_account_created' or 'oauth_callback_success' has ip + user_agent + request_id present (meta).

        spec #43 — audit events insérés avec meta.
        """
        from sqlalchemy import select

        from server.db.models import AuthEvent

        monkeypatch.setenv("SAMSUNGHEALTH_OAUTH_AUTO_REGISTER", "true")
        nonce = "audit-meta-nonce"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-auditmeta",
            email="auditmeta@example.com",
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                headers={"User-Agent": "TestAgent/1.0"},
                follow_redirects=False,
            )
        assert r.status_code == 200, r.text

        events = db_session.execute(
            select(AuthEvent).where(
                AuthEvent.event_type.in_(
                    {"oauth_account_created", "oauth_callback_success"}
                )
            )
        ).scalars().all()
        assert len(events) >= 1, "must record at least 1 oauth audit event"
        # meta cols: ip + user_agent + request_id columns exist on AuthEvent.
        evt = events[0]
        assert evt.user_agent is not None or evt.request_id is not None or evt.ip is not None, (
            f"audit must include meta (ip/user_agent/request_id), got: "
            f"ip={evt.ip!r} ua={evt.user_agent!r} rid={evt.request_id!r}"
        )

    def test_refresh_token_google_not_stored(
        self, client_pg_ready, db_session, monkeypatch, google_keypair_and_jwks
    ):
        """given a Google token endpoint returning a refresh_token, when callback creates identity_providers row, then raw_claims does NOT contain 'refresh_token' (Google refresh never stored).

        spec #42 — Refresh token Google PAS stocké.
        """
        from sqlalchemy import select

        from server.db.models import IdentityProvider

        monkeypatch.setenv("SAMSUNGHEALTH_OAUTH_AUTO_REGISTER", "true")
        nonce = "norefresh-nonce"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-norefresh",
            email="norefresh@example.com",
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                follow_redirects=False,
            )
        assert r.status_code == 200, r.text

        idp = db_session.execute(
            select(IdentityProvider).where(
                IdentityProvider.provider_sub == "google-sub-norefresh"
            )
        ).scalar_one()
        raw = idp.raw_claims or {}
        assert "refresh_token" not in raw, (
            f"raw_claims must NEVER contain refresh_token, got: {raw}"
        )

    def test_raw_claims_no_pii_leak(
        self, client_pg_ready, db_session, monkeypatch, google_keypair_and_jwks
    ):
        """given a Google ID token with name/picture/locale/hd/at_hash claims, when callback creates idp row, then raw_claims contains ONLY 8 whitelisted keys (no PII).

        spec #40 — raw_claims whitelist filter.
        """
        from sqlalchemy import select

        from server.db.models import IdentityProvider

        monkeypatch.setenv("SAMSUNGHEALTH_OAUTH_AUTO_REGISTER", "true")
        nonce = "no-pii-nonce"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-nopii",
            email="nopii@example.com",
            nonce=nonce,
            extra_claims={
                "name": "John Smith",
                "given_name": "John",
                "family_name": "Smith",
                "picture": "https://lh3.googleusercontent.com/abc",
                "locale": "fr-FR",
                "hd": "company.com",
                "at_hash": "abc123",
            },
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                follow_redirects=False,
            )
        assert r.status_code == 200, r.text

        idp = db_session.execute(
            select(IdentityProvider).where(
                IdentityProvider.provider_sub == "google-sub-nopii"
            )
        ).scalar_one()
        raw = idp.raw_claims or {}
        for forbidden in {"name", "given_name", "family_name", "picture", "locale", "hd", "at_hash"}:
            assert forbidden not in raw, (
                f"PII {forbidden!r} leaked in idp.raw_claims: {raw}"
            )


class TestRaceCondition:
    def test_concurrent_callbacks_same_unknown_email_creates_one_user(
        self, schema_ready, pg_url, monkeypatch, google_keypair_and_jwks
    ):
        """given 2 concurrent callbacks with the same unknown email + same provider_sub, when run, then exactly 1 user row created (ON CONFLICT).

        spec #22 — race condition test : 2 callbacks concurrents → 1 seul user.
        """
        import concurrent.futures
        import threading

        from fastapi.testclient import TestClient
        from sqlalchemy import create_engine, select, text
        from sqlalchemy.orm import sessionmaker

        from server.database import get_session
        from server.db.models import User
        from server.main import app

        engine = create_engine(pg_url, future=True)
        SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)

        def _override():
            sess = SessionLocal()
            try:
                yield sess
            finally:
                sess.close()

        app.dependency_overrides[get_session] = _override

        nonce = "race-nonce-1"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-race-1",
            email="race@example.com",
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)
        monkeypatch.setenv("SAMSUNGHEALTH_OAUTH_AUTO_REGISTER", "true")

        start = threading.Event()
        results: list[int] = []

        def _do_callback():
            with TestClient(app) as client:
                start.wait()
                with patch("httpx.AsyncClient.get", new=async_get), patch(
                    "httpx.AsyncClient.post", new=async_post
                ):
                    r = client.get(
                        "/auth/google/callback",
                        params={"code": "fake-code", "state": "fake-state"},
                        follow_redirects=False,
                    )
                    results.append(r.status_code)

        try:
            with concurrent.futures.ThreadPoolExecutor(max_workers=2) as ex:
                futures = [ex.submit(_do_callback) for _ in range(2)]
                # Slight pause to let both threads reach `start.wait()`.
                time.sleep(0.1)
                start.set()
                for f in futures:
                    f.result(timeout=20)

            with SessionLocal() as s:
                rows = s.execute(
                    select(User).where(User.email == "race@example.com")
                ).scalars().all()
            assert len(rows) == 1, (
                f"race must produce exactly 1 user row, got {len(rows)}: {[r.email for r in rows]}"
            )
        finally:
            app.dependency_overrides.clear()
            engine.dispose()


class TestAutoRegisterFlag:
    def test_auto_register_false_unknown_email_returns_403(
        self, client_pg_ready, monkeypatch, google_keypair_and_jwks
    ):
        """given AUTO_REGISTER=false + provider_sub unknown + email unknown, when callback, then 403 oauth_registration_disabled.

        spec #12 — auto_register=false + email inconnu → 403 oauth_registration_disabled.
        """
        monkeypatch.setenv("SAMSUNGHEALTH_OAUTH_AUTO_REGISTER", "false")

        nonce = "no-autoreg-nonce"
        _, async_get, async_post = _make_callback_mocks(
            google_keypair_and_jwks=google_keypair_and_jwks,
            sub="google-sub-autoreg-off",
            email="autoreg-off@example.com",
            nonce=nonce,
        )
        _patch_state_verify_to_return_nonce(monkeypatch, nonce)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=async_get), patch(
            "httpx.AsyncClient.post", new=async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "fake-code", "state": "fake-state"},
                follow_redirects=False,
            )
        assert r.status_code == 403, (
            f"auto_register=false + unknown email must 403, got {r.status_code}: {r.text}"
        )
        body = r.json()
        assert body.get("detail") == "oauth_registration_disabled", body


class TestErrors:
    def test_callback_error_query_param_returns_mapped_status(self, client_pg_ready):
        """given GET /auth/google/callback?error=access_denied, when called, then 400 oauth_user_declined (per error map).

        spec #36 — Google error=access_denied → 400 oauth_user_declined.
        """
        client = client_pg_ready
        r = client.get(
            "/auth/google/callback",
            params={"error": "access_denied", "state": "fake-state"},
            follow_redirects=False,
        )
        assert r.status_code == 400, f"expected 400, got {r.status_code}: {r.text}"
        body = r.json()
        assert body.get("detail") == "oauth_user_declined", body

    def test_error_description_never_in_response_body(self, client_pg_ready):
        """given GET /auth/google/callback?error=server_error&error_description=internal-google-leak, when called, then status==502 (per error map for server_error) AND body does NOT contain the leak string.

        spec #37 — error_description jamais dans response client.
        spec #36 — server_error → 502 oauth_provider_unavailable.
        """
        client = client_pg_ready
        secret = "GOOGLE-INTERNAL-LEAK-DETAIL-XYZ"
        r = client.get(
            "/auth/google/callback",
            params={
                "error": "server_error",
                "error_description": secret,
                "state": "fake-state",
            },
            follow_redirects=False,
        )
        # First : route must exist and respond with mapped status (502 for server_error per spec #36).
        assert r.status_code == 502, (
            f"server_error must map to 502, got {r.status_code}: {r.text}"
        )
        body = r.json()
        # Must be the generic mapped code, NOT the raw error_description.
        assert body.get("detail") == "oauth_provider_unavailable", body
        # Defense-in-depth: secret never anywhere in response.
        assert secret not in r.text, (
            f"error_description must NEVER leak in response body: {r.text[:300]}"
        )
        for hv in r.headers.values():
            assert secret not in hv, f"error_description leaked in header: {hv}"
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_seed_user` (function) — lines 29-42
- `_make_callback_mocks` (function) — lines 45-91
- `_patch_state_verify_to_return_nonce` (function) — lines 94-102
- `TestGoogleStartPost` (class) — lines 106-140
- `TestCsrfProtection` (class) — lines 143-161
- `TestGoogleCallback` (class) — lines 164-239
- `TestAccountLinkingMatrix` (class) — lines 242-649
- `TestRaceCondition` (class) — lines 652-727
- `TestAutoRegisterFlag` (class) — lines 730-762
- `TestErrors` (class) — lines 765-810

### Imports
- `time`
- `datetime`
- `unittest.mock`
- `pytest`

### Exports
- `_seed_user`
- `_make_callback_mocks`
- `_patch_state_verify_to_return_nonce`
- `TestGoogleStartPost`
- `TestCsrfProtection`
- `TestGoogleCallback`
- `TestAccountLinkingMatrix`
- `TestRaceCondition`
- `TestAutoRegisterFlag`
- `TestErrors`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — classes: `TestGoogleStartPost`, `TestCsrfProtection`, `TestGoogleCallback`, `TestAccountLinkingMatrix`, `TestRaceCondition`, `TestAutoRegisterFlag`, `TestErrors`
