"""V2.3.3.2 — Sec-Fetch-Site CSRF check on auth POST endpoints.

Tests RED-first contre `server.security.csrf.check_sec_fetch_site` (NEW),
appliqué sur :
- POST /auth/login
- POST /auth/register
- POST /auth/password/reset/request
- POST /auth/verify-email/request
- POST /auth/oauth-link/confirm
- POST /auth/google/start (déjà fait V2.3.2, on vérifie la cohérence)

Cas couverts :
- Sec-Fetch-Site: cross-site → 403 csrf_check_failed sur les 6 endpoints
- Sec-Fetch-Site: same-origin → flow normal (status fonctionnel ≠ 403 CSRF)
- Sec-Fetch-Site: absent (legacy/curl) → flow normal (header toléré)
- Sec-Fetch-Site: same-site (autre subdomain) → 403 (rejeté)

Spec: docs/vault/specs/2026-04-27-v2.3.3.2-frontend-nightfall.md (#15-#17, "Login CSRF protection (pentester M1 fix)").
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


# Endpoints testés. (path, json body minimal valide pour passer la pydantic
# parse étape ; le check CSRF doit fire AVANT toute logique.)
_CSRF_PROTECTED_POST = [
    ("/auth/login", {"email": "csrf@example.com", "password": "longpassword12345"}),
    (
        "/auth/register",
        {"email": "csrf-reg@example.com", "password": "longpassword12345"},
    ),
    ("/auth/password/reset/request", {"email": "csrf-reset@example.com"}),
    ("/auth/verify-email/request", {"email": "csrf-verify@example.com"}),
    ("/auth/oauth-link/confirm", {"token": "x" * 32}),
    ("/auth/google/start", {}),
]


class TestSecFetchSiteCheck:
    """spec #15 + #17 — Sec-Fetch-Site cross-site → 403 csrf_check_failed."""

    @pytest.mark.parametrize("path,body", _CSRF_PROTECTED_POST)
    def test_cross_site_sec_fetch_returns_403_on_all_auth_post(
        self, client_pg_ready, path, body
    ):
        """given POST <auth path> with Sec-Fetch-Site: cross-site, when called, then 403 csrf_check_failed AND the new shared helper module exists.

        spec #15 — login-CSRF surface élargie de V2.3.2 (OAuth-only) à TOUS les POST auth.
        Couples to the new `server.security.csrf` module so /auth/google/start
        (already protected in V2.3.2) only passes once it has been refactored
        to use the shared helper (spec livrables Backend M1 fix).
        """
        # RED-couple: import the new shared module — fails until impl creates it.
        from server.security.csrf import check_sec_fetch_site  # noqa: F401

        client = client_pg_ready
        # Always include X-Registration-Token for /auth/register so the CSRF
        # check is the gating concern (not the registration token check).
        headers = {
            "Sec-Fetch-Site": "cross-site",
            "Content-Type": "application/json",
        }
        if path == "/auth/register":
            headers["X-Registration-Token"] = _TEST_REGISTRATION_TOKEN

        r = client.post(path, json=body, headers=headers)
        assert r.status_code == 403, (
            f"POST {path} with Sec-Fetch-Site: cross-site must 403, got {r.status_code}: {r.text}"
        )
        try:
            detail = r.json().get("detail", "")
        except Exception:
            detail = r.text
        # Allow either "csrf_check_failed" (uniform name spec #15) or
        # "oauth_csrf_check_failed" (legacy V2.3.2 name on /auth/google/start).
        assert detail in ("csrf_check_failed", "oauth_csrf_check_failed"), (
            f"POST {path} cross-site detail must be csrf_check_failed, got {detail!r}"
        )

    def test_same_site_sec_fetch_returns_403(self, client_pg_ready):
        """given POST /auth/login with Sec-Fetch-Site: same-site (different subdomain), when called, then 403.

        spec #15 — same-site (subdomain attack vector) doit aussi être refusé.
        """
        client = client_pg_ready
        r = client.post(
            "/auth/login",
            json={"email": "csrf-ss@example.com", "password": "longpassword12345"},
            headers={
                "Sec-Fetch-Site": "same-site",
                "Content-Type": "application/json",
            },
        )
        assert r.status_code == 403, (
            f"POST /auth/login with Sec-Fetch-Site: same-site must 403, got {r.status_code}: {r.text}"
        )

    def test_same_origin_sec_fetch_passes_csrf_check(self, client_pg_ready):
        """given the new shared csrf module exists AND POST /auth/login with Sec-Fetch-Site: same-origin + unknown email, when called, then NOT 403 (CSRF passes); response is the normal 401 invalid_credentials.

        spec #16 — same-origin = legitimate browser request → flow normal.
        Couples to the new `server.security.csrf` module so the test fails
        during RED phase (module missing) and only passes once the impl wires
        the helper into /auth/login.
        """
        # RED-couple: module must exist.
        from server.security.csrf import check_sec_fetch_site  # noqa: F401

        client = client_pg_ready
        r = client.post(
            "/auth/login",
            json={"email": "no-such@example.com", "password": "longpassword12345"},
            headers={
                "Sec-Fetch-Site": "same-origin",
                "Content-Type": "application/json",
            },
        )
        # The unknown email should yield 401, NOT 403 from the CSRF check.
        assert r.status_code != 403, (
            f"POST /auth/login same-origin must NOT 403, got 403 (CSRF check too aggressive): {r.text}"
        )
        assert r.status_code == 401, (
            f"POST /auth/login same-origin + unknown email expected 401, got {r.status_code}: {r.text}"
        )

    def test_absent_sec_fetch_passes_csrf_check(self, client_pg_ready):
        """given the new shared csrf module exists AND POST /auth/login WITHOUT Sec-Fetch-Site header (legacy / curl), when called, then NOT 403 (CSRF check tolerates absence).

        spec #17 — header absent toléré pour clients legacy/curl/non-browser.
        Couples to the new module presence so the test only passes once the
        impl is in place.
        """
        # RED-couple: module must exist.
        from server.security.csrf import check_sec_fetch_site  # noqa: F401

        client = client_pg_ready
        r = client.post(
            "/auth/login",
            json={"email": "no-curl@example.com", "password": "longpassword12345"},
            headers={"Content-Type": "application/json"},
        )
        assert r.status_code != 403, (
            f"POST /auth/login WITHOUT Sec-Fetch-Site must NOT 403, got 403: {r.text}"
        )
        assert r.status_code == 401, (
            f"POST /auth/login WITHOUT Sec-Fetch-Site + unknown email expected 401, got {r.status_code}: {r.text}"
        )

    def test_csrf_helper_exposed_in_csrf_module(self):
        """given import server.security.csrf, when introspected, then callable check_sec_fetch_site exists.

        spec livrables Backend — `server/security/csrf.py` (NEW) : `check_sec_fetch_site(request)` helper réutilisable.
        """
        from server.security.csrf import check_sec_fetch_site

        assert callable(check_sec_fetch_site), (
            "server.security.csrf.check_sec_fetch_site must be a callable helper"
        )
