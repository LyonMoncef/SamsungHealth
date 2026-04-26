"""V2.3.1 — Host header injection protection (PUBLIC_BASE_URL).

Tests RED-first contre `server.security.auth._validate_public_base_url_at_boot`
+ pas de fallback request.headers["Host"] dans la génération de verify_link.

Spec: docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


@pytest.fixture(autouse=True)
def _set_v231_env(monkeypatch):
    monkeypatch.setenv(
        "SAMSUNGHEALTH_EMAIL_HASH_SALT",
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    )


class TestPublicBaseUrl:
    def test_verify_link_uses_env_var_not_request_host(self, client_pg_ready, monkeypatch):
        """given POST /auth/password/reset/request with Host: evil.com, when verify_link is reconstructed, then it uses SAMSUNGHEALTH_PUBLIC_BASE_URL, not 'evil.com'.

        spec §Génération token — public_base_url lu EXCLUSIVEMENT depuis env var, jamais Host header.
        spec §Test d'acceptation #19 — Host header injection.
        """
        monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", "http://localhost:8000")

        client = client_pg_ready
        client.post(
            "/auth/register",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
            json={"email": "host-inj@example.com", "password": "longpassword12345"},
        )

        # Inject malicious Host header.
        r = client.post(
            "/auth/password/reset/request",
            headers={"Host": "evil.com"},
            json={"email": "host-inj@example.com"},
        )
        assert r.status_code == 202

        # Now ask admin endpoint for the reconstructed verify_link.
        admin = client.get(
            "/admin/pending-verifications",
            headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        )
        assert admin.status_code == 200, f"admin endpoint failed: {admin.text}"
        entries = admin.json()
        assert len(entries) >= 1, "expected at least one pending entry"
        verify_link = entries[0]["verify_link"]
        assert verify_link.startswith("http://localhost:8000/"), (
            f"verify_link must use PUBLIC_BASE_URL, got: {verify_link!r}"
        )
        assert "evil.com" not in verify_link, (
            f"Host header injection: 'evil.com' leaked into verify_link: {verify_link!r}"
        )

    def test_boot_fails_without_public_base_url_env(self, monkeypatch):
        """given env var SAMSUNGHEALTH_PUBLIC_BASE_URL unset, when _validate_public_base_url_at_boot runs, then raises.

        spec §Test d'acceptation #20 — Boot fails sans SAMSUNGHEALTH_PUBLIC_BASE_URL.
        """
        from server.security.auth import _validate_public_base_url_at_boot

        monkeypatch.delenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", raising=False)
        with pytest.raises(Exception):
            _validate_public_base_url_at_boot()

        # Also fail on a non-https / non-localhost value.
        monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", "ftp://bad.example.com")
        with pytest.raises(Exception):
            _validate_public_base_url_at_boot()
