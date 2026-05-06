---
type: code-source
language: python
file_path: tests/server/test_refresh_cookie.py
git_blob: 48cc89de8dc506ed8679afe29584716732208e76
last_synced: '2026-05-06T08:02:35Z'
loc: 329
annotations: []
imports:
- re
- time
- unittest.mock
- pytest
exports:
- _register_and_login
- _parse_set_cookie_attrs
- _find_set_cookie
- TestRefreshTokenCookie
tags:
- code
- python
---

# tests/server/test_refresh_cookie.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_refresh_cookie.py`](../../../tests/server/test_refresh_cookie.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.2 — Refresh token cookie httpOnly + SameSite + Path scope.

Tests RED-first contre les modifs `server/routers/auth.py` (login, refresh, logout)
et `server/routers/auth_oauth.py` (google_callback) qui doivent émettre
`Set-Cookie: refresh_token=...; HttpOnly; SameSite=Strict; Path=/auth/refresh; Max-Age=...`
EN PLUS du JSON existant (back-compat).

Cas couverts :
1. POST /auth/login succès → Set-Cookie httpOnly + SameSite=Strict + Path=/auth/refresh + Max-Age=2592000
2. POST /auth/refresh avec cookie (sans body refresh_token) → 200 + new tokens
3. POST /auth/refresh avec body refresh_token (sans cookie) → 200 + new tokens (fallback back-compat)
4. POST /auth/logout → response delete_cookie (Set-Cookie expired sur refresh_token)
5. Cookie scope is restreint à Path=/auth/refresh (pas envoyé sur /api/*)
6. OAuth callback (success) émet aussi Set-Cookie refresh_token httpOnly

Spec: docs/vault/specs/2026-04-27-v2.3.3.2-frontend-nightfall.md (#18-#22, "Token storage").
"""
from __future__ import annotations

import re
import time
from unittest.mock import patch

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"
_TEST_CLIENT_ID = "test-client-id-123.apps.googleusercontent.com"


def _register_and_login(client, email: str, password: str = "longpassword12345"):
    """Register + login helper, returns the login Response object."""
    reg = client.post(
        "/auth/register",
        headers={
            "X-Registration-Token": _TEST_REGISTRATION_TOKEN,
            "Sec-Fetch-Site": "same-origin",
        },
        json={"email": email, "password": password},
    )
    assert reg.status_code in (201, 409), f"register failed: {reg.text}"
    return client.post(
        "/auth/login",
        json={"email": email, "password": password},
        headers={"Sec-Fetch-Site": "same-origin"},
    )


def _parse_set_cookie_attrs(set_cookie_value: str) -> dict:
    """Parse a Set-Cookie header value into a lowercased attr dict.

    Example: "refresh_token=ABC; HttpOnly; SameSite=Strict; Path=/auth/refresh; Max-Age=2592000"
    → {"name": "refresh_token", "value": "ABC", "httponly": True, "samesite": "strict", ...}
    """
    parts = [p.strip() for p in set_cookie_value.split(";")]
    name, _, value = parts[0].partition("=")
    attrs = {"name": name.strip(), "value": value.strip()}
    for raw in parts[1:]:
        if "=" in raw:
            k, _, v = raw.partition("=")
            attrs[k.strip().lower()] = v.strip()
        else:
            attrs[raw.strip().lower()] = True
    return attrs


def _find_set_cookie(response, cookie_name: str) -> str | None:
    """Return the raw Set-Cookie header value matching `cookie_name=`, or None.

    httpx's `response.headers.get_list("set-cookie")` is the multi-valued accessor.
    """
    raw_list = response.headers.get_list("set-cookie") if hasattr(
        response.headers, "get_list"
    ) else [
        v for k, v in response.headers.multi_items() if k.lower() == "set-cookie"
    ]
    for raw in raw_list:
        if raw.lstrip().lower().startswith(f"{cookie_name.lower()}="):
            return raw
    return None


class TestRefreshTokenCookie:
    """spec #18-#22 — Set-Cookie refresh_token httpOnly + SameSite=Strict + Path=/auth/refresh."""

    def test_login_sets_refresh_token_cookie_httponly_samesite_path(
        self, client_pg_ready
    ):
        """given POST /auth/login success, when response inspected, then Set-Cookie has refresh_token + HttpOnly + SameSite=Strict + Path=/auth/refresh + Max-Age ~ 2592000.

        spec #18 — login set Set-Cookie httpOnly + SameSite + Path + Max-Age.
        """
        client = client_pg_ready
        r = _register_and_login(client, "cookie-login@example.com")
        assert r.status_code == 200, f"login failed: {r.status_code} {r.text}"

        raw = _find_set_cookie(r, "refresh_token")
        assert raw is not None, (
            f"login MUST emit Set-Cookie: refresh_token=...; got headers={dict(r.headers)}"
        )
        attrs = _parse_set_cookie_attrs(raw)
        assert attrs.get("httponly") is True, (
            f"refresh_token cookie MUST be HttpOnly: {raw}"
        )
        samesite = str(attrs.get("samesite", "")).lower()
        assert samesite == "strict", (
            f"refresh_token cookie MUST be SameSite=Strict, got {samesite!r}: {raw}"
        )
        assert attrs.get("path") == "/auth/refresh", (
            f"refresh_token cookie Path MUST be /auth/refresh, got {attrs.get('path')!r}: {raw}"
        )
        max_age = attrs.get("max-age")
        assert max_age is not None, (
            f"refresh_token cookie MUST include Max-Age, got: {raw}"
        )
        # 30d = 30 * 86400 = 2592000
        assert int(max_age) == 30 * 86400, (
            f"refresh_token Max-Age must be 30 days (2592000), got {max_age!r}"
        )

    def test_refresh_reads_cookie_when_body_missing(self, client_pg_ready):
        """given a successful login (cookie + JSON refresh_token), when POST /auth/refresh with cookie only (no body refresh_token), then 200 + new TokenPair.

        spec #19 — refresh lit cookie quand body absent.
        """
        client = client_pg_ready
        login = _register_and_login(client, "cookie-refresh@example.com")
        assert login.status_code == 200, f"login: {login.text}"

        # The TestClient automatically persists cookies on the client. We POST
        # /auth/refresh with empty JSON to assert the server reads from cookie.
        r = client.post(
            "/auth/refresh",
            json={},
            headers={"Sec-Fetch-Site": "same-origin"},
        )
        assert r.status_code == 200, (
            f"/auth/refresh with cookie-only must succeed, got {r.status_code}: {r.text}"
        )
        body = r.json()
        assert "access_token" in body and "refresh_token" in body, (
            f"/auth/refresh must return new TokenPair: {body}"
        )

    def test_refresh_falls_back_to_body_when_no_cookie(self, client_pg_ready):
        """given login emits Set-Cookie refresh_token (proof V2.3.3.2 impl) AND a successful login, when POST /auth/refresh with body refresh_token but NO cookie, then 200 + new TokenPair.

        spec #20 — back-compat: clients legacy (Android pre-V2.3.3.2) qui n'utilisent pas cookies.
        Couples to the new login Set-Cookie behaviour (otherwise this test
        passes trivially against the V2.3 baseline where refresh always reads body).
        """
        client = client_pg_ready
        login = _register_and_login(client, "cookie-fallback@example.com")
        assert login.status_code == 200, f"login: {login.text}"

        # RED-couple: assert the Set-Cookie header is emitted (proof of V2.3.3.2
        # impl). If the impl is not yet in place, this fails RED before the
        # back-compat assertion below would trivially pass.
        raw = _find_set_cookie(login, "refresh_token")
        assert raw is not None, (
            f"login MUST emit Set-Cookie refresh_token (V2.3.3.2 impl missing); headers={dict(login.headers)}"
        )

        token_body = login.json()
        body_refresh = token_body["refresh_token"]

        # Clear cookies on the TestClient so the body fallback is exercised.
        client.cookies.clear()

        r = client.post(
            "/auth/refresh",
            json={"refresh_token": body_refresh},
            headers={"Sec-Fetch-Site": "same-origin"},
        )
        assert r.status_code == 200, (
            f"/auth/refresh body-fallback must succeed, got {r.status_code}: {r.text}"
        )
        body = r.json()
        assert "access_token" in body and "refresh_token" in body, (
            f"/auth/refresh body-fallback must return TokenPair: {body}"
        )

    def test_logout_deletes_refresh_cookie(self, client_pg_ready):
        """given a logged-in user, when POST /auth/logout, then response Set-Cookie expires the refresh_token cookie (Max-Age=0 OR expires=Thu, 01 Jan 1970).

        spec #21 — logout delete_cookie.
        """
        client = client_pg_ready
        login = _register_and_login(client, "cookie-logout@example.com")
        assert login.status_code == 200, f"login: {login.text}"
        token_body = login.json()
        access = token_body["access_token"]
        refresh = token_body["refresh_token"]

        r = client.post(
            "/auth/logout",
            json={"refresh_token": refresh},
            headers={
                "Authorization": f"Bearer {access}",
                "Sec-Fetch-Site": "same-origin",
            },
        )
        assert r.status_code == 204, (
            f"/auth/logout must 204, got {r.status_code}: {r.text}"
        )
        raw = _find_set_cookie(r, "refresh_token")
        assert raw is not None, (
            f"/auth/logout must emit Set-Cookie to delete refresh_token, got headers={dict(r.headers)}"
        )
        attrs = _parse_set_cookie_attrs(raw)
        # delete_cookie semantics: Max-Age=0 OR expires in the past (1970).
        max_age = attrs.get("max-age")
        expires = str(attrs.get("expires", "")).lower()
        is_deleted = (
            (max_age is not None and int(max_age) == 0)
            or "1970" in expires
            or attrs.get("value", "") == ""
        )
        assert is_deleted, (
            f"/auth/logout must delete refresh_token (Max-Age=0 or expires 1970 or empty value): {raw}"
        )
        # Path scope must match the original (delete_cookie requires same Path).
        assert attrs.get("path") == "/auth/refresh", (
            f"/auth/logout delete must specify Path=/auth/refresh, got {attrs.get('path')!r}: {raw}"
        )

    def test_refresh_cookie_scope_restricted_to_auth_refresh(
        self, client_pg_ready
    ):
        """given a logged-in client (cookie set with Path=/auth/refresh), when GET /api/* (other path), then the refresh_token cookie is NOT sent (browser-side scoping respected by httpx TestClient via cookie Path attribute).

        spec #22 + "Token storage" — Cookie Path=/auth/refresh limite le scope aux endpoints qui en ont besoin.
        """
        client = client_pg_ready
        login = _register_and_login(client, "cookie-scope@example.com")
        assert login.status_code == 200, f"login: {login.text}"

        # Inspect the cookie jar: the cookie should be registered under
        # path /auth/refresh, so it must NOT be sent for /api/* requests.
        # httpx Cookies expose `.jar` (cookielib.CookieJar).
        jar = getattr(client.cookies, "jar", None)
        assert jar is not None, "client.cookies.jar missing — cannot inspect Path scope"
        scoped = [
            c
            for c in jar
            if c.name == "refresh_token" and (c.path or "/").startswith("/auth/refresh")
        ]
        assert scoped, (
            f"refresh_token cookie must be registered with Path=/auth/refresh in the jar; "
            f"got cookies={[(c.name, c.path) for c in jar]}"
        )

    def test_oauth_google_callback_sets_refresh_cookie_on_success(
        self, client_pg_ready, monkeypatch, google_keypair_and_jwks
    ):
        """given a successful Google OAuth callback (auto_register), when response inspected, then Set-Cookie refresh_token httpOnly is present.

        spec #22 — OAuth callback set cookie.
        """
        from server.security.auth_providers import state as state_mod

        monkeypatch.setenv("SAMSUNGHEALTH_OAUTH_AUTO_REGISTER", "true")

        nonce = "cookie-oauth-nonce-1"
        now = int(time.time())
        claims = {
            "iss": "https://accounts.google.com",
            "aud": _TEST_CLIENT_ID,
            "sub": "google-sub-cookie-test",
            "email": "cookie-oauth@example.com",
            "email_verified": True,
            "iat": now,
            "exp": now + 600,
            "nonce": nonce,
        }
        id_token = google_keypair_and_jwks["sign"](claims)

        async def _async_get(*args, **kwargs):
            from unittest.mock import MagicMock

            resp = MagicMock()
            resp.status_code = 200
            resp.json = lambda: google_keypair_and_jwks["jwks"]
            resp.headers = {"Cache-Control": "max-age=3600"}
            resp.raise_for_status = lambda: None
            return resp

        async def _async_post(*args, **kwargs):
            from unittest.mock import MagicMock

            resp = MagicMock()
            resp.status_code = 200
            resp.json = lambda: {
                "access_token": "g-access",
                "id_token": id_token,
                "refresh_token": "g-refresh-DO-NOT-STORE",
                "expires_in": 3600,
                "token_type": "Bearer",
            }
            resp.raise_for_status = lambda: None
            return resp

        def _fake_verify(state_jwt: str):
            return {"nonce": nonce, "used": True}

        monkeypatch.setattr(state_mod, "verify_oauth_state", _fake_verify, raising=False)

        client = client_pg_ready
        with patch("httpx.AsyncClient.get", new=_async_get), patch(
            "httpx.AsyncClient.post", new=_async_post
        ):
            r = client.get(
                "/auth/google/callback",
                params={"code": "cookie-code", "state": "cookie-state"},
                follow_redirects=False,
            )
        # Whether the impl returns 200 JSON or 302 redirect (post-pentester
        # spec revision), the Set-Cookie MUST be present on the success path.
        assert r.status_code in (200, 302, 303), (
            f"google callback success must be 200 or 302/303, got {r.status_code}: {r.text[:200]}"
        )
        raw = _find_set_cookie(r, "refresh_token")
        assert raw is not None, (
            f"google callback success MUST emit Set-Cookie: refresh_token=...; got headers={dict(r.headers)}"
        )
        attrs = _parse_set_cookie_attrs(raw)
        assert attrs.get("httponly") is True, (
            f"OAuth refresh_token cookie MUST be HttpOnly: {raw}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register_and_login` (function) — lines 31-46
- `_parse_set_cookie_attrs` (function) — lines 49-64
- `_find_set_cookie` (function) — lines 67-80
- `TestRefreshTokenCookie` (class) — lines 83-329

### Imports
- `re`
- `time`
- `unittest.mock`
- `pytest`

### Exports
- `_register_and_login`
- `_parse_set_cookie_attrs`
- `_find_set_cookie`
- `TestRefreshTokenCookie`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall]] — classes: `TestRefreshTokenCookie`
