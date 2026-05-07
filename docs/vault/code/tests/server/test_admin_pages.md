---
type: code-source
language: python
file_path: tests/server/test_admin_pages.py
git_blob: c9607e18f4a7482994730662298488a1272ae060
last_synced: '2026-05-06T08:02:34Z'
loc: 310
annotations: []
imports:
- os
- re
- pathlib
- pytest
exports:
- _client
- TestAdminPagesServed
- TestAdminCspStrictTrustedTypes
- TestAdminCacheControl
- TestAdminAutocompleteOff
- TestNoTokenLeakHtml
- TestNoReturnToBypass
tags:
- code
- python
---

# tests/server/test_admin_pages.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_admin_pages.py`](../../../tests/server/test_admin_pages.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.3 — Static admin pages, CSP_ADMIN strict (trusted-types), no leaks.

Tests RED-first contre :
- `server.routers.static_pages` (extension) — 4 nouveaux endpoints GET admin pages
- `server.middleware.security_headers` (extension) — `_csp_for_path` étendu pour
  `/admin/*` → `CSP_ADMIN_PAGES` (CSP_AUTH_PAGES + `require-trusted-types-for 'script'`
  + `trusted-types default;`). `_cache_control_for` étendu pour `/admin/*` → `no-store`.
- 4 fichiers HTML statiques sous `static/admin/` : login, users, user-detail, pending-verifications

Spec: docs/vault/specs/2026-04-28-v2.3.3.3-auth-finitions.md
  §"CSP différencié /admin/* + Trusted Types (pentester #9)"
  §"Cache-Control /admin/* (pentester L3 fix)"
  §"autocomplete admin token field" (pentester #10)
  §"Open redirect prevention `?return_to=`" (pentester M1 fix)
  §Tests d'acceptation #9-#12.
"""
from __future__ import annotations

import os
import re
from pathlib import Path

import pytest


_REPO_ROOT = Path(__file__).resolve().parent.parent.parent
_STATIC_DIR = _REPO_ROOT / "static"
_ADMIN_DIR = _STATIC_DIR / "admin"

_ADMIN_PAGE_NAMES = ("login", "users", "user-detail", "pending-verifications")


def _client():
    """V2.3.3.3 — TestClient that does NOT depend on PG fixture.

    Inspects HTML/headers without booting testcontainers Postgres.
    """
    from fastapi.testclient import TestClient

    from server.main import app

    return TestClient(app)


# ── pages servies ──────────────────────────────────────────────────────────
class TestAdminPagesServed:
    """spec test #9 — GET /admin/{login,users,users/<id>,pending-verifications} → 200 text/html."""

    @pytest.mark.parametrize("page", ["login", "users", "pending-verifications"])
    def test_admin_page_served_with_html_content_type(self, page):
        """given GET /admin/<page>, when called, then 200 + content-type text/html.

        spec test #9 — pages admin servies par `static_pages.py` (extension).
        """
        client = _client()
        r = client.get(f"/admin/{page}", headers={"Accept": "text/html"})
        assert r.status_code == 200, (
            f"GET /admin/{page} expected 200, got {r.status_code}: {r.text[:200]}"
        )
        ct = r.headers.get("content-type", "")
        assert "text/html" in ct, (
            f"GET /admin/{page} content-type must contain text/html, got {ct!r}"
        )

    def test_admin_user_detail_page_served(self):
        """given GET /admin/users/<some-uuid>, when called, then 200 text/html (page user-detail.html).

        spec §Pages HTML — `static/admin/user-detail.html` servi sur `/admin/users/{id}`.
        Distinct du JSON endpoint `/admin/users/{id}` (qui retourne JSON sous le router admin.py).
        Le routing distingue : Accept text/html → page, Accept application/json → API.
        """
        client = _client()
        r = client.get(
            "/admin/users/00000000-0000-0000-0000-000000000001",
            headers={"Accept": "text/html"},
        )
        assert r.status_code == 200, (
            f"GET /admin/users/<id> with Accept text/html expected 200, got {r.status_code}: {r.text[:200]}"
        )
        ct = r.headers.get("content-type", "")
        assert "text/html" in ct, (
            f"GET /admin/users/<id> Accept text/html → content-type must be text/html, got {ct!r}"
        )


# ── CSP admin strict avec Trusted Types ────────────────────────────────────
class TestAdminCspStrictTrustedTypes:
    """spec test #10 + §"CSP différencié /admin/* + Trusted Types" — CSP_ADMIN_PAGES = CSP_AUTH_PAGES + require-trusted-types-for 'script'; trusted-types default."""

    @pytest.mark.parametrize("page", ["login", "users", "pending-verifications"])
    def test_admin_csp_includes_require_trusted_types_for_script(self, page):
        """given GET /admin/<page>, when CSP header inspected, then contient `require-trusted-types-for 'script'`.

        spec §"CSP différencié /admin/* + Trusted Types" — anti-XSS hardening :
        bloque .innerHTML = userInput sauf via policy enregistrée.
        """
        client = _client()
        r = client.get(f"/admin/{page}", headers={"Accept": "text/html"})
        csp = r.headers.get("content-security-policy", "")
        assert csp, f"CSP header missing on /admin/{page}: {dict(r.headers)}"
        assert "require-trusted-types-for 'script'" in csp, (
            f"CSP_ADMIN_PAGES must contain require-trusted-types-for 'script' "
            f"(spec §CSP différencié /admin/* + Trusted Types), got: {csp}"
        )

    @pytest.mark.parametrize("page", ["login", "users", "pending-verifications"])
    def test_admin_csp_includes_trusted_types_default(self, page):
        """given GET /admin/<page>, when CSP header inspected, then contient `trusted-types default`.

        spec §"CSP différencié /admin/* + Trusted Types" — `trusted-types default;`
        directive complète l'API Trusted Types.
        """
        client = _client()
        r = client.get(f"/admin/{page}", headers={"Accept": "text/html"})
        csp = r.headers.get("content-security-policy", "")
        assert csp, f"CSP header missing on /admin/{page}"
        assert "trusted-types default" in csp, (
            f"CSP_ADMIN_PAGES must contain trusted-types default directive, got: {csp}"
        )

    def test_admin_csp_inherits_no_unsafe_inline_from_auth(self):
        """given GET /admin/login, when CSP inspected, then NO 'unsafe-inline' (CSP_ADMIN_PAGES inherits CSP_AUTH_PAGES strict).

        spec §"CSP différencié /admin/* + Trusted Types" — base = CSP_AUTH_PAGES.
        """
        client = _client()
        r = client.get("/admin/login")
        csp = r.headers.get("content-security-policy", "")
        assert csp
        assert "'unsafe-inline'" not in csp, (
            f"CSP_ADMIN_PAGES must NOT contain 'unsafe-inline' (inherits CSP_AUTH_PAGES strict): {csp}"
        )


# ── Cache-Control no-store admin ───────────────────────────────────────────
class TestAdminCacheControl:
    """spec test #12 + §"Cache-Control /admin/* (pentester L3 fix)" — pages admin no-store."""

    @pytest.mark.parametrize("page", ["login", "users", "pending-verifications"])
    def test_admin_pages_have_cache_control_no_store(self, page):
        """given GET /admin/<page>, when response inspected, then Cache-Control: no-store.

        spec §"Cache-Control /admin/* (pentester L3 fix)" — `_cache_control_for`
        étendu pour `/admin/*` → no-store. Évite que pages admin restent en cache
        après logout.
        """
        client = _client()
        r = client.get(f"/admin/{page}", headers={"Accept": "text/html"})
        cc = r.headers.get("cache-control", "")
        assert cc == "no-store", (
            f"GET /admin/{page} Cache-Control must be exactly 'no-store', got {cc!r}"
        )


# ── autocomplete=off sur token field ───────────────────────────────────────
class TestAdminAutocompleteOff:
    """spec §"autocomplete admin token field" (pentester #10) — form admin token a autocomplete=off."""

    def test_admin_login_html_has_autocomplete_off_on_form(self):
        """given static/admin/login.html, when grepped, then `<form ... autocomplete="off">` AND `<input type="password" autocomplete="off"`.

        spec §"autocomplete admin token field" — empêche browser de mémoriser
        le token admin (sécurité accrue self-host).
        """
        login_path = _ADMIN_DIR / "login.html"
        assert login_path.exists(), (
            f"static/admin/login.html missing — impl must create it (spec livrables Frontend)"
        )
        content = login_path.read_text(encoding="utf-8")
        assert re.search(
            r'<form[^>]*autocomplete\s*=\s*"off"', content, re.IGNORECASE
        ), (
            f"static/admin/login.html <form> must have autocomplete=\"off\" "
            f"(spec §autocomplete admin token field), content head: {content[:500]!r}"
        )
        assert re.search(
            r'<input[^>]*type\s*=\s*"password"[^>]*autocomplete\s*=\s*"off"',
            content,
            re.IGNORECASE,
        ), (
            f"static/admin/login.html <input type=password> must have autocomplete=\"off\""
        )


# ── No token leak / no env var leak in HTML ────────────────────────────────
class TestNoTokenLeakHtml:
    """spec test #11 + §pentester #13 — pages admin ne leak ni le nom env var ni la valeur."""

    @pytest.mark.parametrize("page", ["login", "users", "pending-verifications"])
    def test_no_secret_env_name_in_admin_pages(self, page):
        """given GET /admin/<page>, when body inspected, then NO `SAMSUNGHEALTH_REGISTRATION_TOKEN` substring.

        spec §pentester #13 — UI label = "Token admin", JAMAIS "X-Registration-Token"
        (pas leak du nom env var).
        """
        client = _client()
        r = client.get(f"/admin/{page}", headers={"Accept": "text/html"})
        assert r.status_code == 200, (
            f"GET /admin/{page} expected 200, got {r.status_code} (impl must serve page)"
        )
        body = r.text
        # Bloque le nom env var ET le nom du header (qui leak l'env var indirectement).
        assert "SAMSUNGHEALTH_REGISTRATION_TOKEN" not in body, (
            f"page /admin/{page} leaks env var name SAMSUNGHEALTH_REGISTRATION_TOKEN"
        )

    @pytest.mark.parametrize("page", ["login", "users", "pending-verifications"])
    def test_no_x_registration_token_header_name_in_admin_pages(self, page):
        """given GET /admin/<page> served (200 text/html), when body inspected, then NO `X-Registration-Token` header name leaked.

        spec §pentester #13 — UI doit afficher "Token admin" / "Mot de passe administrateur"
        et jamais "X-Registration-Token". Le frontend l'utilise dans fetch() mais pas
        dans le HTML rendu.

        Couples to the page being served (status 200 + text/html) so the assertion
        is meaningful and not trivially passing on a 404 stub response.
        """
        client = _client()
        r = client.get(f"/admin/{page}", headers={"Accept": "text/html"})
        # RED-couple : la page doit d'abord exister (impl) avant que le grep ait du sens.
        assert r.status_code == 200, (
            f"GET /admin/{page} expected 200 (impl must serve page), got {r.status_code}: {r.text[:200]}"
        )
        ct = r.headers.get("content-type", "")
        assert "text/html" in ct, (
            f"GET /admin/{page} content-type must be text/html, got {ct!r}"
        )
        body = r.text
        assert "X-Registration-Token" not in body, (
            f"page /admin/{page} leaks header name X-Registration-Token in HTML "
            f"(spec §pentester #13 — must use UI label 'Token admin' instead)"
        )

    @pytest.mark.parametrize("page", ["login", "users", "pending-verifications"])
    def test_no_secret_value_in_admin_pages(self, page):
        """given GET /admin/<page> served (200 text/html), when body inspected, then NO secret token VALUE substring.

        spec §pentester #13 + defense in depth. Couples to the page being served
        (status 200) so the test is RED until the impl serves the page.
        """
        client = _client()
        r = client.get(f"/admin/{page}", headers={"Accept": "text/html"})
        assert r.status_code == 200, (
            f"GET /admin/{page} expected 200 (impl must serve page), got {r.status_code}"
        )
        ct = r.headers.get("content-type", "")
        assert "text/html" in ct, (
            f"GET /admin/{page} content-type must be text/html, got {ct!r}"
        )
        body = r.text
        secret = os.environ.get("SAMSUNGHEALTH_REGISTRATION_TOKEN", "")
        assert secret and len(secret) > 8, (
            "test fixture must set SAMSUNGHEALTH_REGISTRATION_TOKEN (>8 chars)"
        )
        assert secret not in body, (
            f"page /admin/{page} leaks SAMSUNGHEALTH_REGISTRATION_TOKEN value (length={len(secret)})"
        )


# ── Open-redirect prevention return_to ─────────────────────────────────────
class TestNoReturnToBypass:
    """spec §"Open redirect prevention `/admin/login?return_to=`" (pentester M1) — strict allowlist regex."""

    def test_admin_login_returns_no_unsafe_external_redirect(self):
        """given GET /admin/login?return_to=https://evil.example served (200 text/html), when response inspected, then body / Location header n'embarque PAS l'URL externe brute.

        spec §"Open redirect prevention" — `_validate_admin_return_to` regex strict
        `^/admin/[a-z0-9/-]+$` ; sinon ignore silencieusement.

        Couples to the page being served (200 text/html) so the test is RED until
        the impl serves the page (and then the open-redirect prevention has surface).
        """
        client = _client()
        r = client.get("/admin/login?return_to=https://evil.example/phish")
        assert r.status_code == 200, (
            f"GET /admin/login expected 200 (impl must serve page), got {r.status_code}"
        )
        ct = r.headers.get("content-type", "")
        assert "text/html" in ct, (
            f"GET /admin/login content-type must be text/html, got {ct!r}"
        )
        # Body must NOT contain the unsafe URL embedded as JS literal / form action.
        body = r.text
        assert "evil.example" not in body, (
            f"GET /admin/login?return_to=https://evil.example body must NOT embed unsafe URL "
            f"(spec §Open redirect prevention), body head: {body[:500]!r}"
        )

    def test_admin_login_returns_no_javascript_scheme_redirect(self):
        """given GET /admin/login?return_to=javascript:alert(1) served (200 text/html), when response inspected, then `javascript:` scheme not embedded anywhere.

        spec §"Open redirect prevention" — XSS via `return_to=javascript:` doit être bloqué.
        Couples to page existence so the test is RED until the impl serves the page.
        """
        client = _client()
        r = client.get("/admin/login?return_to=javascript:alert(1)")
        assert r.status_code == 200, (
            f"GET /admin/login expected 200, got {r.status_code}"
        )
        ct = r.headers.get("content-type", "")
        assert "text/html" in ct, f"content-type must be text/html, got {ct!r}"
        body = r.text
        loc = r.headers.get("location", "")
        assert "javascript:" not in body.lower(), (
            f"GET /admin/login?return_to=javascript:alert(1) body must NOT embed javascript: scheme, "
            f"body head: {body[:500]!r}"
        )
        assert "javascript:" not in loc.lower(), (
            f"Location header must not contain javascript: scheme, got {loc!r}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_client` (function) — lines 33-42
- `TestAdminPagesServed` (class) — lines 46-83
- `TestAdminCspStrictTrustedTypes` (class) — lines 87-132
- `TestAdminCacheControl` (class) — lines 136-152
- `TestAdminAutocompleteOff` (class) — lines 156-182
- `TestNoTokenLeakHtml` (class) — lines 186-257
- `TestNoReturnToBypass` (class) — lines 261-310

### Imports
- `os`
- `re`
- `pathlib`
- `pytest`

### Exports
- `_client`
- `TestAdminPagesServed`
- `TestAdminCspStrictTrustedTypes`
- `TestAdminCacheControl`
- `TestAdminAutocompleteOff`
- `TestNoTokenLeakHtml`
- `TestNoReturnToBypass`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]] — classes: `TestAdminPagesServed`, `TestAdminCspStrictTrustedTypes`, `TestAdminCacheControl`, `TestAdminAutocompleteOff`, `TestNoTokenLeakHtml`
