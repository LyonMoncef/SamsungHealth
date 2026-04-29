---
type: code-source
language: python
file_path: tests/server/test_login_email_verification_gate.py
git_blob: 1e3f18b06140999890a37302a46b8147279e2b44
last_synced: '2026-04-26T22:07:14Z'
loc: 88
annotations: []
imports:
- pytest
exports:
- _register
- TestRequireEmailVerificationFlag
tags:
- code
- python
---

# tests/server/test_login_email_verification_gate.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_login_email_verification_gate.py`](../../../tests/server/test_login_email_verification_gate.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.1 — Login gate via SAMSUNGHEALTH_REQUIRE_EMAIL_VERIFICATION flag.

Tests RED-first contre `server.routers.auth.login` qui doit lire l'env var
DYNAMIQUEMENT (pas en cache au boot) — pour que monkeypatch.setenv soit immédiat.

Spec: docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_v231_env(monkeypatch):
    monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", "http://localhost:8000")
    monkeypatch.setenv(
        "SAMSUNGHEALTH_EMAIL_HASH_SALT",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    )


def _register(client, email, password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


class TestRequireEmailVerificationFlag:
    def test_default_false_login_ok_with_warning(self, client_pg_ready, monkeypatch):
        """given SAMSUNGHEALTH_REQUIRE_EMAIL_VERIFICATION=false (default), when login of unverified user, then 200 + structlog warning 'auth.login.unverified_email'.

        spec §Flag config — Si false (default V2.3.1) : login OK, warning observatif.
        spec §Test d'acceptation #24 (false branch).
        """
        import structlog

        monkeypatch.setenv("SAMSUNGHEALTH_REQUIRE_EMAIL_VERIFICATION", "false")

        client = client_pg_ready
        reg = _register(client, "gate-default@example.com")
        assert reg.status_code == 201, f"register precondition failed: {reg.text}"

        with structlog.testing.capture_logs() as logs:
            r = client.post(
                "/auth/login",
                json={
                    "email": "gate-default@example.com",
                    "password": "longpassword12345",
                },
            )
        assert r.status_code == 200, f"login should succeed when flag=false: {r.text}"

        unverified_warns = [
            log for log in logs if log.get("event") == "auth.login.unverified_email"
        ]
        assert len(unverified_warns) >= 1, (
            f"expected auth.login.unverified_email warning when flag=false, got logs={logs}"
        )

    def test_true_login_unverified_returns_403(self, client_pg_ready, monkeypatch):
        """given SAMSUNGHEALTH_REQUIRE_EMAIL_VERIFICATION=true, when login of unverified user, then 403 email_not_verified.

        spec §Flag config — Si true : login → 403 email_not_verified si email_verified_at IS NULL.
        spec §Test d'acceptation #24 (true branch).
        """
        # Register FIRST (with flag=false to allow registration without verify pre-req).
        monkeypatch.setenv("SAMSUNGHEALTH_REQUIRE_EMAIL_VERIFICATION", "false")
        client = client_pg_ready
        reg = _register(client, "gate-strict@example.com")
        assert reg.status_code == 201, f"register precondition failed: {reg.text}"

        # Now flip the flag to true.
        monkeypatch.setenv("SAMSUNGHEALTH_REQUIRE_EMAIL_VERIFICATION", "true")

        r = client.post(
            "/auth/login",
            json={
                "email": "gate-strict@example.com",
                "password": "longpassword12345",
            },
        )
        assert r.status_code == 403, f"expected 403 when flag=true and unverified, got {r.status_code}: {r.text}"
        assert r.json() == {"detail": "email_not_verified"}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register` (function) — lines 25-30
- `TestRequireEmailVerificationFlag` (class) — lines 33-88

### Imports
- `pytest`

### Exports
- `_register`
- `TestRequireEmailVerificationFlag`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — classes: `TestRequireEmailVerificationFlag` · methods: `test_default_false_login_ok_with_warning`, `test_true_login_unverified_returns_403`
