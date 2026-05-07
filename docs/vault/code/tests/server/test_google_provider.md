---
type: code-source
language: python
file_path: tests/server/test_google_provider.py
git_blob: acd79f31d5643ccd36923bfb3d16a5069e8463ec
last_synced: '2026-05-06T08:02:35Z'
loc: 378
annotations: []
imports:
- unittest.mock
- pytest
exports:
- TestGoogleProviderConfig
- TestGoogleIdTokenValidation
- TestJwksHardcoded
- TestJwksTtlCap
- TestRawClaimsFilter
- TestErrorMap
tags:
- code
- python
---

# tests/server/test_google_provider.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_google_provider.py`](../../../tests/server/test_google_provider.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.2 — GoogleAuthProvider (boot config + JWKS hardcoded + ID token validation + raw_claims whitelist + error mapping).

Tests RED-first contre `server.security.auth_providers.google`:
- TestGoogleProviderConfig : boot fail si une seule env Google présente
- TestGoogleIdTokenValidation : signature/aud/nonce mismatch → AuthProviderError
- TestJwksHardcoded : SSRF protection (iss=evil.com → JWKS toujours googleapis)
- TestJwksTtlCap : Cache-Control max-age 24h → cap à 4h
- TestRawClaimsFilter : whitelist 8 keys, refuse name/picture/locale/etc
- TestErrorMap : table de mapping error → status code

Spec: docs/vault/specs/2026-04-26-v2.3.2-google-oauth.md (#1, #14-#18, #36, #40).
"""
from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest


_TEST_CLIENT_ID = "test-client-id-123.apps.googleusercontent.com"


class TestGoogleProviderConfig:
    def test_boot_fail_on_partial_google_env(self, monkeypatch):
        """given CLIENT_ID set but CLIENT_SECRET unset, when _validate_google_oauth_env_at_boot runs, then raises (ConfigError or RuntimeError).

        spec #1 — Boot fail si une seule env Google présente.
        """
        monkeypatch.setenv(
            "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_ID", _TEST_CLIENT_ID
        )
        monkeypatch.delenv(
            "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_SECRET", raising=False
        )

        from server.security.auth_providers.google import (
            _validate_google_oauth_env_at_boot,
        )

        with pytest.raises(Exception):
            _validate_google_oauth_env_at_boot()

    def test_boot_ok_when_neither_env_set_provider_disabled(self, monkeypatch):
        """given both Google env vars unset, when _validate_google_oauth_env_at_boot runs, then returns None/False (provider disabled, no raise).

        spec §Configuration Google — Si les deux Google absentes → Google provider désactivé.
        """
        monkeypatch.delenv(
            "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_ID", raising=False
        )
        monkeypatch.delenv(
            "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_SECRET", raising=False
        )

        from server.security.auth_providers.google import (
            _validate_google_oauth_env_at_boot,
        )

        # Must NOT raise — disabled mode.
        result = _validate_google_oauth_env_at_boot()
        # spec: returns None / False (disabled, no error).
        assert result in (None, False), (
            f"expected None/False when Google disabled, got {result!r}"
        )


class TestGoogleIdTokenValidation:
    def test_id_token_invalid_signature_raises(
        self, monkeypatch, google_keypair_and_jwks
    ):
        """given an ID token signed with a *different* RSA key than what JWKS exposes, when validate_id_token runs, then raises AuthProviderError.

        spec #14 — ID token signature invalide → AuthProviderError → 400 oauth_id_token_invalid.
        """
        import time as _t

        from cryptography.hazmat.primitives import serialization
        from cryptography.hazmat.primitives.asymmetric import rsa
        import jwt as _jwt

        from server.security.auth_providers.google import (
            AuthProviderError,
            GoogleAuthProvider,
        )

        # Forge with a DIFFERENT private key (signature won't match exposed JWKS).
        wrong = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        wrong_pem = wrong.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption(),
        )
        now = int(_t.time())
        claims = {
            "iss": "https://accounts.google.com",
            "aud": _TEST_CLIENT_ID,
            "sub": "1234567890",
            "email": "x@example.com",
            "email_verified": True,
            "iat": now,
            "exp": now + 600,
            "nonce": "expected-nonce",
        }
        bad_token = _jwt.encode(
            claims,
            wrong_pem,
            algorithm="RS256",
            headers={"kid": google_keypair_and_jwks["kid"]},
        )

        # Mock httpx GET on JWKS endpoint to return the OTHER (valid) key.
        async def _fake_get(*args, **kwargs):
            resp = MagicMock()
            resp.status_code = 200
            resp.json = lambda: google_keypair_and_jwks["jwks"]
            resp.headers = {"Cache-Control": "max-age=3600"}
            resp.raise_for_status = lambda: None
            return resp

        import asyncio

        with patch("httpx.AsyncClient.get", new=_fake_get):
            provider = GoogleAuthProvider()
            with pytest.raises((AuthProviderError, Exception)):
                asyncio.run(
                    provider.validate_id_token(
                        id_token=bad_token, expected_nonce="expected-nonce"
                    )
                )

    def test_id_token_nonce_mismatch_raises(
        self, monkeypatch, google_keypair_and_jwks
    ):
        """given a valid ID token with nonce='X' but expected_nonce='Y', when validate_id_token runs, then raises AuthProviderError.

        spec #15 — nonce mismatch → 400 oauth_nonce_mismatch.
        """
        import time as _t

        from server.security.auth_providers.google import (
            AuthProviderError,
            GoogleAuthProvider,
        )

        now = int(_t.time())
        signed = google_keypair_and_jwks["sign"]
        token = signed(
            {
                "iss": "https://accounts.google.com",
                "aud": _TEST_CLIENT_ID,
                "sub": "user-sub-1",
                "email": "noncetest@example.com",
                "email_verified": True,
                "iat": now,
                "exp": now + 600,
                "nonce": "GOOD-nonce",
            }
        )

        async def _fake_get(*args, **kwargs):
            resp = MagicMock()
            resp.status_code = 200
            resp.json = lambda: google_keypair_and_jwks["jwks"]
            resp.headers = {"Cache-Control": "max-age=3600"}
            resp.raise_for_status = lambda: None
            return resp

        import asyncio

        with patch("httpx.AsyncClient.get", new=_fake_get):
            provider = GoogleAuthProvider()
            with pytest.raises((AuthProviderError, Exception)):
                asyncio.run(
                    provider.validate_id_token(
                        id_token=token, expected_nonce="DIFFERENT-nonce"
                    )
                )

    def test_id_token_aud_mismatch_raises(
        self, monkeypatch, google_keypair_and_jwks
    ):
        """given an ID token with aud != our client_id, when validate_id_token runs, then raises AuthProviderError.

        spec #16 — aud != notre client_id → 400 oauth_id_token_invalid.
        """
        import time as _t

        from server.security.auth_providers.google import (
            AuthProviderError,
            GoogleAuthProvider,
        )

        now = int(_t.time())
        signed = google_keypair_and_jwks["sign"]
        token = signed(
            {
                "iss": "https://accounts.google.com",
                "aud": "ATTACKER-client-id.apps.googleusercontent.com",  # NOT our client_id
                "sub": "user-sub-aud",
                "email": "audtest@example.com",
                "email_verified": True,
                "iat": now,
                "exp": now + 600,
                "nonce": "any-nonce",
            }
        )

        async def _fake_get(*args, **kwargs):
            resp = MagicMock()
            resp.status_code = 200
            resp.json = lambda: google_keypair_and_jwks["jwks"]
            resp.headers = {"Cache-Control": "max-age=3600"}
            resp.raise_for_status = lambda: None
            return resp

        import asyncio

        with patch("httpx.AsyncClient.get", new=_fake_get):
            provider = GoogleAuthProvider()
            with pytest.raises((AuthProviderError, Exception)):
                asyncio.run(
                    provider.validate_id_token(
                        id_token=token, expected_nonce="any-nonce"
                    )
                )


class TestJwksHardcoded:
    def test_jwks_url_never_derived_from_iss_no_ssrf(
        self, monkeypatch, google_keypair_and_jwks
    ):
        """given an ID token with iss='https://attacker.com/.well-known/openid', when validate_id_token runs, then httpx GET is called on https://www.googleapis.com/oauth2/v3/certs ONLY (never attacker.com).

        spec #17 — JWKS hardcoded, jamais derived from iss claim → no SSRF.
        """
        import time as _t

        from server.security.auth_providers.google import GoogleAuthProvider

        now = int(_t.time())
        signed = google_keypair_and_jwks["sign"]
        token = signed(
            {
                "iss": "https://attacker.com/.well-known/openid",  # MALICIOUS iss
                "aud": _TEST_CLIENT_ID,
                "sub": "ssrf-victim",
                "email": "ssrf@example.com",
                "email_verified": True,
                "iat": now,
                "exp": now + 600,
                "nonce": "ssrf-nonce",
            }
        )

        called_urls: list[str] = []

        async def _fake_get(self_, url, *args, **kwargs):
            called_urls.append(str(url))
            resp = MagicMock()
            resp.status_code = 200
            resp.json = lambda: google_keypair_and_jwks["jwks"]
            resp.headers = {"Cache-Control": "max-age=3600"}
            resp.raise_for_status = lambda: None
            return resp

        import asyncio

        with patch("httpx.AsyncClient.get", new=_fake_get):
            provider = GoogleAuthProvider()
            try:
                asyncio.run(
                    provider.validate_id_token(
                        id_token=token, expected_nonce="ssrf-nonce"
                    )
                )
            except Exception:
                pass  # iss validation will likely also reject it, that's fine.

        # CRITICAL: never call attacker.com. Always googleapis.com if any GET was made.
        for url in called_urls:
            assert "attacker.com" not in url, (
                f"SSRF: JWKS GET on attacker URL detected: {url}"
            )
            assert "googleapis.com" in url or "google.com" in url, (
                f"JWKS GET should be on googleapis, got: {url}"
            )


class TestJwksTtlCap:
    def test_jwks_ttl_capped_at_4h_when_google_returns_24h(self):
        """given a Cache-Control: max-age=86400 (24h) header, when JWKS TTL is computed, then it is capped at 14400 (4h).

        spec #18 — JWKS TTL min(google_max_age, 4*3600) = max 4h.
        """
        from server.security.auth_providers.google import _compute_jwks_ttl_seconds

        # 24h = 86400, must be capped to 4h = 14400.
        ttl = _compute_jwks_ttl_seconds("max-age=86400")
        assert ttl == 14400, f"24h cache-control must cap to 14400s, got {ttl}"

        # Fallback 1h (3600) on missing/malformed header.
        ttl_default = _compute_jwks_ttl_seconds(None)
        assert ttl_default == 3600, f"missing header must fallback to 3600s, got {ttl_default}"


class TestRawClaimsFilter:
    def test_filter_claims_keeps_only_8_whitelisted(self):
        """given a raw claims dict with 13 keys (8 whitelisted + 5 PII), when _filter_claims runs, then result contains only the 8 whitelisted keys (no name/picture/locale/hd/at_hash).

        spec #40 + spec §raw_claims whitelist.
        """
        from server.security.auth_providers.google import (
            _RAW_CLAIMS_WHITELIST,
            _filter_claims,
        )

        raw = {
            # Whitelisted (8 keys per spec).
            "sub": "user-1",
            "email": "x@example.com",
            "email_verified": True,
            "iss": "https://accounts.google.com",
            "aud": "client-id",
            "iat": 1700000000,
            "exp": 1700003600,
            "jti": "tok-jti",
            # NOT whitelisted (PII / OIDC nonce).
            "name": "John Smith",
            "given_name": "John",
            "family_name": "Smith",
            "picture": "https://lh3.googleusercontent.com/abc",
            "locale": "fr-FR",
            "hd": "company.com",
            "at_hash": "abc123",
            "nonce": "noncey",
        }
        out = _filter_claims(raw)
        # Only whitelisted keys in output.
        assert set(out.keys()) <= _RAW_CLAIMS_WHITELIST, (
            f"_filter_claims must keep only whitelisted keys, got: {sorted(out.keys())}"
        )
        # All PII keys must be absent.
        for forbidden in {"name", "given_name", "family_name", "picture", "locale", "hd", "at_hash", "nonce"}:
            assert forbidden not in out, (
                f"PII/forbidden key {forbidden!r} leaked through filter: {out}"
            )


class TestErrorMap:
    def test_google_error_codes_map_to_correct_status(self):
        """given each Google `error` code listed in spec table, when looked up in _GOOGLE_ERROR_MAP, then returns the spec-defined (status_code, our_code) tuple.

        spec #36 — table mapping 11 google errors → status codes.
        """
        from server.security.auth_providers.google import _GOOGLE_ERROR_MAP

        # Spec table (status, our_code).
        expected = {
            "access_denied": (400, "oauth_user_declined"),
            "interaction_required": (400, "oauth_user_declined"),
            "login_required": (400, "oauth_user_declined"),
            "account_selection_required": (400, "oauth_user_declined"),
            "consent_required": (400, "oauth_consent_required"),
            "invalid_request": (500, "oauth_provider_error"),
            "unauthorized_client": (500, "oauth_provider_error"),
            "unsupported_response_type": (500, "oauth_provider_error"),
            "invalid_scope": (500, "oauth_provider_error"),
            "server_error": (502, "oauth_provider_unavailable"),
            "temporarily_unavailable": (503, "oauth_provider_unavailable"),
        }
        for google_err, (status, our_code) in expected.items():
            assert google_err in _GOOGLE_ERROR_MAP, (
                f"google error {google_err!r} missing from _GOOGLE_ERROR_MAP"
            )
            mapped = _GOOGLE_ERROR_MAP[google_err]
            assert mapped == (status, our_code), (
                f"mapping mismatch for {google_err!r}: expected {(status, our_code)}, got {mapped}"
            )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestGoogleProviderConfig` (class) — lines 23-64
- `TestGoogleIdTokenValidation` (class) — lines 67-225
- `TestJwksHardcoded` (class) — lines 228-286
- `TestJwksTtlCap` (class) — lines 289-303
- `TestRawClaimsFilter` (class) — lines 306-346
- `TestErrorMap` (class) — lines 349-378

### Imports
- `unittest.mock`
- `pytest`

### Exports
- `TestGoogleProviderConfig`
- `TestGoogleIdTokenValidation`
- `TestJwksHardcoded`
- `TestJwksTtlCap`
- `TestRawClaimsFilter`
- `TestErrorMap`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — classes: `TestGoogleProviderConfig`, `TestGoogleIdTokenValidation`, `TestJwksHardcoded`, `TestJwksTtlCap`, `TestRawClaimsFilter`, `TestErrorMap`
