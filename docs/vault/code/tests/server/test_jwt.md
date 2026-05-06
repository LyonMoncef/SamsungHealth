---
type: code-source
language: python
file_path: tests/server/test_jwt.py
git_blob: 47e0455aaeb3c4d8ef21cd7e54dc94c52efdab5b
last_synced: '2026-05-06T08:02:35Z'
loc: 242
annotations: []
imports:
- pytest
exports:
- TestJwtSecretValidation
- TestAccessToken
- TestJwtDecodeFootguns
- TestRefreshToken
tags:
- code
- python
---

# tests/server/test_jwt.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_jwt.py`](../../../tests/server/test_jwt.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3 — JWT (HS256) access + refresh tokens.

Tests RED-first contre `server.security.auth`:
- TestJwtSecretValidation : boot validation
- TestAccessToken : kid header
- TestRefreshToken : previous secret decode-only
- TestJwtDecodeFootguns : alg=none, missing claims, no PII

Spec: docs/vault/specs/2026-04-26-v2-auth-foundation.md (#7-#16)
"""
from __future__ import annotations

import pytest


# 32-byte base64-encoded test secret (≥ 256 bits ASCII)
_TEST_JWT_SECRET = "dGVzdC1qd3Qtc2VjcmV0LXdpdGgtMzItYnl0ZXMtbWluLW9rITE="
_PREVIOUS_JWT_SECRET = "cHJldmlvdXMtand0LXNlY3JldC10ZXN0LTMyLWJ5dGVzLW1pbi0xMg=="


class TestJwtSecretValidation:
    def test_jwt_secret_required_at_boot(self, monkeypatch):
        """given env var SAMSUNGHEALTH_JWT_SECRET unset, when boot validator runs, then raises.

        spec #7 — fail-fast on missing secret.
        """
        from server.security.auth import _validate_jwt_secret_at_boot

        monkeypatch.delenv("SAMSUNGHEALTH_JWT_SECRET", raising=False)
        with pytest.raises(Exception):
            _validate_jwt_secret_at_boot()

    def test_jwt_secret_min_256_bits(self, monkeypatch):
        """given a 16-char ASCII secret (128 bits), when boot validator runs, then raises (too short).

        spec #8 — secret must be ≥ 256 bits.
        """
        from server.security.auth import _validate_jwt_secret_at_boot

        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", "short16charsecre")  # 16 chars = 128 bits
        with pytest.raises(Exception):
            _validate_jwt_secret_at_boot()

    def test_jwt_secret_rejects_changeme(self, monkeypatch):
        """given secret 'changeme', when boot validator runs, then raises.

        spec #9 — explicit reject of trivial values (case-insensitive).
        """
        from server.security.auth import _validate_jwt_secret_at_boot

        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", "changeme")
        with pytest.raises(Exception):
            _validate_jwt_secret_at_boot()


class TestAccessToken:
    def test_create_access_token_includes_kid(self, monkeypatch):
        """given a freshly created access token, when reading the unverified header, then kid == 'v1'.

        spec #10 — kid injected from day 1 for future rotation.
        """
        import jwt as pyjwt

        from server.security.auth import create_access_token

        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)

        token = create_access_token(user_id="00000000-0000-0000-0000-000000000001")
        header = pyjwt.get_unverified_header(token)
        assert header.get("kid") == "v1", f"expected kid=v1, got header={header}"


class TestJwtDecodeFootguns:
    def test_decode_rejects_alg_none(self, monkeypatch):
        """given a token forged with alg='none', when decode_access_token runs, then raises.

        spec #11 — must never accept unsigned tokens.
        """
        import jwt as pyjwt

        from server.security.auth import decode_access_token

        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)

        forged = pyjwt.encode(
            {"sub": "x", "iat": 1, "exp": 9999999999, "iss": "samsunghealth", "aud": "samsunghealth-api", "typ": "access"},
            "",
            algorithm="none",
        )
        with pytest.raises(Exception):
            decode_access_token(forged)

    def test_decode_requires_explicit_algorithms(self, monkeypatch):
        """given source code of server/security/auth.py, when grepping jwt.decode call, then it passes algorithms=[...] explicitly.

        spec #12 — must never call jwt.decode without algorithms (alg confusion).
        """
        from pathlib import Path

        src = Path(__file__).resolve().parent.parent.parent / "server" / "security" / "auth.py"
        assert src.exists(), f"source not found: {src}"
        text = src.read_text()
        # Must contain a jwt.decode call with explicit algorithms parameter.
        # We check the literal substring "algorithms=" appears in any decode call context.
        assert "jwt.decode(" in text, "no jwt.decode call found"
        # Look for algorithms= within ~200 chars of any jwt.decode( occurrence.
        idx = 0
        while True:
            i = text.find("jwt.decode(", idx)
            if i == -1:
                break
            window = text[i : i + 400]
            assert "algorithms=" in window, f"jwt.decode call without explicit algorithms= found near char {i}"
            idx = i + 1

    def test_decode_requires_exp_iat_sub_claims(self, monkeypatch):
        """given a token missing 'exp', when decode_access_token runs, then raises (required claim).

        spec #13 — must require exp/iat/sub.
        """
        import jwt as pyjwt

        from server.security.auth import decode_access_token

        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)

        # Token with everything except exp.
        token = pyjwt.encode(
            {
                "sub": "00000000-0000-0000-0000-000000000001",
                "iat": 1,
                "iss": "samsunghealth",
                "aud": "samsunghealth-api",
                "typ": "access",
            },
            _TEST_JWT_SECRET,
            algorithm="HS256",
            headers={"kid": "v1"},
        )
        with pytest.raises(Exception):
            decode_access_token(token)

    def test_decode_rejects_missing_iss_aud(self, monkeypatch):
        """given a token missing 'iss', when decode_access_token runs, then raises.

        spec #14 — iss/aud must be validated.
        """
        import time as _time

        import jwt as pyjwt

        from server.security.auth import decode_access_token

        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)

        now = int(_time.time())
        token = pyjwt.encode(
            {
                "sub": "00000000-0000-0000-0000-000000000001",
                "iat": now,
                "exp": now + 1800,
                "aud": "samsunghealth-api",
                "typ": "access",
                # iss intentionally missing
            },
            _TEST_JWT_SECRET,
            algorithm="HS256",
            headers={"kid": "v1"},
        )
        with pytest.raises(Exception):
            decode_access_token(token)

    def test_payload_contains_only_sub_no_pii(self, monkeypatch):
        """given a real access token issued by create_access_token, when decoded, then payload keys ⊆ {sub, iat, exp, iss, aud, typ}.

        spec #15 — no email, no role, no PII.
        """
        from server.security.auth import create_access_token, decode_access_token

        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)

        token = create_access_token(user_id="00000000-0000-0000-0000-000000000001")
        payload = decode_access_token(token)

        allowed = {"sub", "iat", "exp", "iss", "aud", "typ"}
        unexpected = set(payload.keys()) - allowed
        assert not unexpected, f"unexpected (PII?) claims in access payload: {unexpected}"
        # Belt-and-braces : explicit check that no email-shaped value is in the payload.
        for v in payload.values():
            if isinstance(v, str):
                assert "@" not in v, f"email-shaped value found in payload: {v}"


class TestRefreshToken:
    def test_previous_secret_decode_only(self, monkeypatch):
        """given a refresh token signed with PREVIOUS secret, when decode_refresh_token runs, then accepted; new tokens always signed with current secret.

        spec #16 — rotation : previous secret accepted in decode but never used to sign.
        """
        import jwt as pyjwt

        from server.security.auth import create_refresh_token, decode_refresh_token

        # Set both current + previous.
        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)
        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET_PREVIOUS", _PREVIOUS_JWT_SECRET)

        # Manually forge a refresh token with the PREVIOUS secret (simulates pre-rotation).
        import time as _time

        now = int(_time.time())
        token_signed_with_previous = pyjwt.encode(
            {
                "sub": "00000000-0000-0000-0000-000000000001",
                "iat": now,
                "exp": now + 86400,
                "iss": "samsunghealth",
                "aud": "samsunghealth-api",
                "typ": "refresh",
                "jti": "11111111-1111-1111-1111-111111111111",
            },
            _PREVIOUS_JWT_SECRET,
            algorithm="HS256",
            headers={"kid": "v0"},
        )
        # decode_refresh_token must accept it (rotation tolerance).
        payload = decode_refresh_token(token_signed_with_previous)
        assert payload["sub"] == "00000000-0000-0000-0000-000000000001"

        # New tokens must be signed with CURRENT secret only — verifying with previous must fail.
        new_token = create_refresh_token(
            user_id="00000000-0000-0000-0000-000000000001",
            jti="22222222-2222-2222-2222-222222222222",
        )
        with pytest.raises(Exception):
            pyjwt.decode(
                new_token,
                _PREVIOUS_JWT_SECRET,
                algorithms=["HS256"],
                issuer="samsunghealth",
                audience="samsunghealth-api",
            )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestJwtSecretValidation` (class) — lines 21-53
- `TestAccessToken` (class) — lines 56-70
- `TestJwtDecodeFootguns` (class) — lines 73-191
- `TestRefreshToken` (class) — lines 194-242

### Imports
- `pytest`

### Exports
- `TestJwtSecretValidation`
- `TestAccessToken`
- `TestJwtDecodeFootguns`
- `TestRefreshToken`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2-auth-foundation]] — classes: `TestJwtSecretValidation`, `TestAccessToken`, `TestRefreshToken`, `TestJwtDecodeFootguns` · methods: `test_jwt_secret_required_at_boot`, `test_jwt_secret_min_256_bits`, `test_jwt_secret_rejects_changeme`, `test_create_access_token_includes_kid`, `test_decode_rejects_alg_none`, `test_decode_requires_explicit_algorithms`, `test_decode_requires_exp_iat_sub_claims`, `test_decode_rejects_missing_iss_aud`, `test_payload_contains_only_sub_no_pii`, `test_previous_secret_decode_only`
