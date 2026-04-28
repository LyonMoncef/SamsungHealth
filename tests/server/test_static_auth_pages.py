"""V2.3.3.2 — Static auth pages, security headers, CSP differentiated, no leaks.

Tests RED-first contre :
- `server.routers.static_pages` (NEW) — router GET pages auth → FileResponse HTML
- `server.middleware.security_headers` (NEW) — middleware ASGI applique X-Frame-Options,
  X-Content-Type-Options, Referrer-Policy, Permissions-Policy, HSTS conditionnel HTTPS,
  CSP différencié par path, strip server/via headers
- `server.main` — `app.include_router(static_pages.router)` + `app.add_middleware(SecurityHeadersMiddleware)`
- 9 fichiers HTML statiques sous `static/auth/` + 2 CSS sous `static/css/` + JS sous `static/js/`

Spec: docs/vault/specs/2026-04-27-v2.3.3.2-frontend-nightfall.md (#1-#14, "Security headers globaux", "CSP différencié par path", "Cache busting", "Anti-XSS frontend").
"""
from __future__ import annotations

import os
import re
from pathlib import Path

import pytest


_REPO_ROOT = Path(__file__).resolve().parent.parent.parent
_STATIC_DIR = _REPO_ROOT / "static"
_AUTH_DIR = _STATIC_DIR / "auth"
_CSS_DIR = _STATIC_DIR / "css"
_JS_DIR = _STATIC_DIR / "js"


_AUTH_PAGE_NAMES = (
    "login",
    "register",
    "reset-request",
    "reset-confirm",
    "verify-email",
    "oauth-link-confirm",
    "oauth-success",
    "oauth-error",
    "oauth-link-pending",
)


def _client():
    """V2.3.3.2 — TestClient that does NOT depend on PG fixture.

    Most tests in this file only need to inspect HTML/headers, not DB. Using
    a plain TestClient avoids the testcontainers dependency for unit-style
    static-asset tests. Requires `server.main.app` to import cleanly.
    """
    from fastapi.testclient import TestClient

    from server.main import app

    return TestClient(app)


# ── pages servies ──────────────────────────────────────────────────────────
class TestPagesServed:
    """spec #1 — GET /auth/{login,register,...} → 200 text/html."""

    @pytest.mark.parametrize("page", _AUTH_PAGE_NAMES)
    def test_auth_page_served_with_html_content_type(self, page):
        """given GET /auth/<page>, when called, then 200 + content-type text/html.

        spec #1, livrable `server/routers/static_pages.py` 9 endpoints GET.
        """
        client = _client()
        r = client.get(f"/auth/{page}")
        assert r.status_code == 200, (
            f"GET /auth/{page} expected 200, got {r.status_code}: {r.text[:200]}"
        )
        ct = r.headers.get("content-type", "")
        assert "text/html" in ct, (
            f"GET /auth/{page} content-type must contain text/html, got {ct!r}"
        )


# ── CSP différencié par path ───────────────────────────────────────────────
class TestCspDifferentiated:
    """spec #4 — CSP_AUTH_PAGES strict, CSP_DASHBOARD plus permissif, CSP_API minimal."""

    def test_csp_auth_pages_strict_no_unsafe_inline(self):
        """given GET /auth/login, when response inspected, then CSP present, has script-src 'self' WITHOUT 'unsafe-inline', has connect-src 'self', has frame-ancestors 'none'.

        spec #4 + "CSP différencié par path" CSP_AUTH_PAGES.
        """
        client = _client()
        r = client.get("/auth/login")
        csp = r.headers.get("content-security-policy", "")
        assert csp, f"CSP header missing on /auth/login: {dict(r.headers)}"
        assert "'unsafe-inline'" not in csp, (
            f"CSP_AUTH_PAGES must NOT contain 'unsafe-inline' (pentester #2/#3 fix): {csp}"
        )
        assert "script-src 'self'" in csp, f"missing script-src 'self': {csp}"
        assert "connect-src 'self'" in csp, (
            f"connect-src must be restricted to 'self' (pentester #4 fix): {csp}"
        )
        assert "frame-ancestors 'none'" in csp, f"missing frame-ancestors 'none': {csp}"

    def test_csp_dashboard_allows_unsafe_inline_style(self):
        """given GET / (dashboard), when response inspected, then CSP_DASHBOARD has style-src 'self' 'unsafe-inline' (toléré pour D3 inline style).

        spec #4 CSP_DASHBOARD.
        """
        client = _client()
        r = client.get("/")
        csp = r.headers.get("content-security-policy", "")
        assert csp, f"CSP header missing on / (dashboard): {dict(r.headers)}"
        assert "'unsafe-inline'" in csp, (
            f"CSP_DASHBOARD must allow 'unsafe-inline' for D3 styles: {csp}"
        )

    def test_csp_api_minimal_default_src_none(self):
        """given GET /api/... (or any /api/* path), when response inspected, then CSP_API = "default-src 'none'; frame-ancestors 'none';".

        spec #4 CSP_API.
        """
        client = _client()
        # Any /api/* path — even 404 — should still trigger the middleware-applied
        # CSP for that path branch. We hit a non-existent /api/ping; middleware
        # runs after route resolution but headers are still set on the response.
        r = client.get("/api/ping")
        csp = r.headers.get("content-security-policy", "")
        assert csp, f"CSP header missing on /api/* path: {dict(r.headers)}"
        assert "default-src 'none'" in csp, (
            f"CSP_API must use default-src 'none': {csp}"
        )


# ── security headers globaux ───────────────────────────────────────────────
class TestSecurityHeadersGlobal:
    """spec #5 — X-Frame-Options DENY + X-Content-Type-Options nosniff + Referrer-Policy + Permissions-Policy sur TOUS les paths."""

    @pytest.mark.parametrize("path", ["/auth/login", "/", "/api/ping"])
    def test_x_frame_options_deny(self, path):
        """given a GET on auth, dashboard, or api path, when response inspected, then X-Frame-Options: DENY.

        spec #5 anti-clickjacking global.
        """
        client = _client()
        r = client.get(path)
        xfo = r.headers.get("x-frame-options", "")
        assert xfo == "DENY", (
            f"X-Frame-Options must be DENY on {path}, got {xfo!r}: headers={dict(r.headers)}"
        )

    @pytest.mark.parametrize("path", ["/auth/login", "/", "/api/ping"])
    def test_x_content_type_options_nosniff(self, path):
        """given a GET on any path, when response inspected, then X-Content-Type-Options: nosniff.

        spec #5 anti MIME sniff global.
        """
        client = _client()
        r = client.get(path)
        xcto = r.headers.get("x-content-type-options", "")
        assert xcto == "nosniff", (
            f"X-Content-Type-Options must be nosniff on {path}, got {xcto!r}"
        )

    @pytest.mark.parametrize("path", ["/auth/login", "/", "/api/ping"])
    def test_referrer_policy_strict_origin(self, path):
        """given a GET, when response inspected, then Referrer-Policy: strict-origin-when-cross-origin.

        spec #5.
        """
        client = _client()
        r = client.get(path)
        rp = r.headers.get("referrer-policy", "")
        assert rp == "strict-origin-when-cross-origin", (
            f"Referrer-Policy on {path} must be strict-origin-when-cross-origin, got {rp!r}"
        )

    @pytest.mark.parametrize("path", ["/auth/login", "/", "/api/ping"])
    def test_permissions_policy_disables_camera_mic_geo(self, path):
        """given a GET, when response inspected, then Permissions-Policy disables camera/microphone/geolocation.

        spec #5 anti hijack hardware.
        """
        client = _client()
        r = client.get(path)
        pp = r.headers.get("permissions-policy", "")
        assert "camera=()" in pp, f"Permissions-Policy must include camera=(): {pp!r}"
        assert "microphone=()" in pp, (
            f"Permissions-Policy must include microphone=(): {pp!r}"
        )
        assert "geolocation=()" in pp, (
            f"Permissions-Policy must include geolocation=(): {pp!r}"
        )


# ── HSTS conditionnel HTTPS ────────────────────────────────────────────────
class TestHstsConditional:
    """spec #6 — HSTS UNIQUEMENT si HTTPS scheme OU SAMSUNGHEALTH_FORCE_HTTPS=true."""

    def test_hsts_absent_on_http_scheme(self):
        """given a GET on plain HTTP scheme (TestClient default), when response inspected, then NO Strict-Transport-Security header AND the security middleware is active (X-Frame-Options DENY present).

        spec #6 HSTS conditionnel — HTTP test must NOT emit HSTS (would break dev local).
        Couples to security middleware being active (X-Frame-Options) so the test
        only passes once the middleware exists AND correctly suppresses HSTS on HTTP.
        """
        client = _client()
        r = client.get("/auth/login")
        # Couple to middleware presence: X-Frame-Options must be there (proof
        # the middleware ran), AND HSTS must be absent (proof of conditional logic).
        xfo = r.headers.get("x-frame-options", "")
        assert xfo == "DENY", (
            f"middleware not active yet (X-Frame-Options missing), got {xfo!r}"
        )
        hsts = r.headers.get("strict-transport-security")
        assert hsts is None, (
            f"HSTS must be ABSENT on HTTP scheme, got {hsts!r}"
        )

    def test_hsts_present_when_force_https_env(self, monkeypatch):
        """given SAMSUNGHEALTH_FORCE_HTTPS=true env, when GET /auth/login, then Strict-Transport-Security: max-age=63072000; includeSubDomains.

        spec #6 — env override forces HSTS even on HTTP scheme (prod behind HTTPS proxy).
        """
        monkeypatch.setenv("SAMSUNGHEALTH_FORCE_HTTPS", "true")
        client = _client()
        r = client.get("/auth/login")
        hsts = r.headers.get("strict-transport-security", "")
        assert "max-age=63072000" in hsts, (
            f"HSTS must include max-age=63072000 (2y), got {hsts!r}"
        )
        assert "includeSubDomains" in hsts, (
            f"HSTS must include includeSubDomains, got {hsts!r}"
        )
        assert "preload" not in hsts, (
            f"HSTS must NOT include preload (irreversible) for V2.3.3.2: {hsts!r}"
        )


# ── Cache-Control ──────────────────────────────────────────────────────────
class TestCacheControl:
    """spec #7 — pages auth no-store, static no-cache (sans ?v=), static immutable (avec ?v=)."""

    def test_auth_html_no_store(self):
        """given GET /auth/login (HTML), when response inspected, then Cache-Control: no-store.

        spec #7 — anti-cache pour pages sensibles auth.
        """
        client = _client()
        r = client.get("/auth/login")
        cc = r.headers.get("cache-control", "")
        assert "no-store" in cc, (
            f"auth HTML must have Cache-Control: no-store, got {cc!r}"
        )

    def test_static_asset_without_version_no_cache(self):
        """given GET /static/css/ds-tokens.css (no ?v=), when response inspected, then Cache-Control: no-cache.

        spec #7 — sans versioning, force re-validate.
        """
        client = _client()
        r = client.get("/static/css/ds-tokens.css")
        # File may not exist yet (RED): the test still inspects header semantics.
        # If it 404s, the test still expects the middleware to set no-cache.
        cc = r.headers.get("cache-control", "")
        assert "no-cache" in cc, (
            f"static asset without ?v= must have Cache-Control: no-cache, got {cc!r} (status {r.status_code})"
        )

    def test_static_asset_with_version_immutable(self):
        """given GET /static/css/ds-tokens.css?v=2.3.3.2, when response inspected, then Cache-Control: public, max-age=31536000, immutable.

        spec #7 + "Cache busting" — versioned assets cached 1 year immutable.
        """
        client = _client()
        r = client.get("/static/css/ds-tokens.css?v=2.3.3.2")
        cc = r.headers.get("cache-control", "")
        assert "public" in cc, (
            f"versioned asset must be public, got {cc!r}"
        )
        assert "max-age=31536000" in cc, (
            f"versioned asset must have max-age=31536000, got {cc!r}"
        )
        assert "immutable" in cc, (
            f"versioned asset must be immutable, got {cc!r}"
        )


# ── server fingerprint ─────────────────────────────────────────────────────
class TestServerHeaderStripped:
    """spec #8 — middleware retire `server` et `via` headers de tous les responses."""

    def test_server_header_absent(self):
        """given any GET, when response inspected, then no `server` header AND middleware is active (X-Frame-Options present).

        spec #8 — strip server fingerprint (pentester L2). Couples to middleware
        presence (X-Frame-Options) so the test only passes once the middleware
        exists AND correctly strips the server header.
        """
        client = _client()
        r = client.get("/auth/login")
        # Couple to middleware presence: X-Frame-Options proves the middleware ran.
        xfo = r.headers.get("x-frame-options", "")
        assert xfo == "DENY", (
            f"middleware not active yet (X-Frame-Options missing), got {xfo!r}"
        )
        assert "server" not in {k.lower() for k in r.headers.keys()}, (
            f"`server` header must be stripped: {dict(r.headers)}"
        )

    def test_via_header_absent(self):
        """given any GET, when response inspected, then no `via` header AND middleware is active.

        spec #8 — strip via fingerprint. Couples to middleware presence so the
        test only passes once the middleware exists AND strips `via`.
        """
        client = _client()
        r = client.get("/auth/login")
        xfo = r.headers.get("x-frame-options", "")
        assert xfo == "DENY", (
            f"middleware not active yet (X-Frame-Options missing), got {xfo!r}"
        )
        assert "via" not in {k.lower() for k in r.headers.keys()}, (
            f"`via` header must be stripped: {dict(r.headers)}"
        )


# ── No inline style / script ───────────────────────────────────────────────
class TestNoInlineStyleScript:
    """spec #9 + #10 — grep `<style|style=` et `<script>` (sans src=) dans 9 HTML auth → 0 match."""

    def test_no_inline_style_in_auth_html(self):
        """given the 9 static/auth/*.html files, when grepped for `<style` block or `style=` attribute, then 0 matches.

        spec #9 — no inline style (CSP_AUTH_PAGES forbids 'unsafe-inline' style-src).
        """
        # Files MUST exist first — else RED is for missing files (the impl's job to create).
        for name in _AUTH_PAGE_NAMES:
            html_path = _AUTH_DIR / f"{name}.html"
            assert html_path.exists(), (
                f"static/auth/{name}.html missing — impl must create it (spec livrables Frontend code)"
            )

        violations = []
        # Pattern: opening <style> tag OR style="..." attribute on an element.
        pat = re.compile(r"<style\b|\sstyle\s*=", re.IGNORECASE)
        for name in _AUTH_PAGE_NAMES:
            html_path = _AUTH_DIR / f"{name}.html"
            content = html_path.read_text(encoding="utf-8")
            for m in pat.finditer(content):
                violations.append(f"{name}.html: {m.group(0)!r}")
        assert not violations, (
            f"Inline style violations (spec #9): {violations}"
        )

    def test_no_inline_script_in_auth_html(self):
        """given the 9 static/auth/*.html files, when grepped for `<script>` (without src=), then 0 matches.

        spec #10 — only `<script src=...>` allowed.
        """
        for name in _AUTH_PAGE_NAMES:
            html_path = _AUTH_DIR / f"{name}.html"
            assert html_path.exists(), (
                f"static/auth/{name}.html missing — impl must create it"
            )

        # Pattern: <script ...> tag where the open tag does NOT contain src=.
        pat = re.compile(r"<script\b([^>]*)>", re.IGNORECASE)
        violations = []
        for name in _AUTH_PAGE_NAMES:
            html_path = _AUTH_DIR / f"{name}.html"
            content = html_path.read_text(encoding="utf-8")
            for m in pat.finditer(content):
                attrs = m.group(1)
                if "src=" not in attrs.lower():
                    violations.append(f"{name}.html: <script{attrs}>")
        assert not violations, (
            f"Inline <script> violations (spec #10 — only src= allowed): {violations}"
        )


# ── No dangerous JS patterns ───────────────────────────────────────────────
class TestNoDangerousJs:
    """spec #11 — grep `\\.innerHTML\\s*=` ou `eval(` ou `Function(` ou `document\\.write` dans `static/js/*.js` → 0 match."""

    def test_no_dangerous_js_patterns_in_static_js(self):
        """given all static/js/*.js files (must exist), when grepped for dangerous patterns, then 0 matches.

        spec #11 + "Anti-XSS frontend" — no `.innerHTML =`, `eval(`, `Function(`, `document.write`.
        """
        # All JS files expected from spec livrables Frontend code.
        expected_js = (
            "api-client.js",
            "auth-form.js",
            "theme.js",
        )
        for name in expected_js:
            js_path = _JS_DIR / name
            assert js_path.exists(), (
                f"static/js/{name} missing — impl must create it (spec livrables Frontend code)"
            )

        # Patterns: innerHTML assignment, eval(, Function(, document.write
        bad = re.compile(
            r"\.innerHTML\s*=|\beval\s*\(|\bFunction\s*\(|\bdocument\.write\b",
        )
        violations = []
        for js_path in _JS_DIR.glob("*.js"):
            # Skip dashboard.js / app.js / api.js — pre-V2.3.3.2 dashboard
            # code is out of scope for this spec.
            if js_path.name in {"dashboard.js", "app.js", "api.js"}:
                continue
            content = js_path.read_text(encoding="utf-8")
            for m in bad.finditer(content):
                violations.append(f"{js_path.name}: {m.group(0)!r}")
        assert not violations, (
            f"Dangerous JS patterns (spec #11): {violations}"
        )


# ── No external resources ──────────────────────────────────────────────────
class TestNoExternalResources:
    """spec #12 — grep `https://fonts.googleapis.com|fonts.gstatic.com|cdn\\.|googleapis\\.com` dans 9 HTML + 2 CSS → 0 match."""

    def test_no_external_resources_in_auth_html_and_css(self):
        """given 9 HTML auth + ds-tokens.css + auth.css (must exist), when grepped for external CDN URLs, then 0 matches.

        spec #12 — assets self-hosted only (CSP connect-src 'self').
        """
        # Files must exist.
        for name in _AUTH_PAGE_NAMES:
            html_path = _AUTH_DIR / f"{name}.html"
            assert html_path.exists(), (
                f"static/auth/{name}.html missing — impl must create it"
            )
        for css_name in ("ds-tokens.css", "auth.css"):
            css_path = _CSS_DIR / css_name
            assert css_path.exists(), (
                f"static/css/{css_name} missing — impl must create it"
            )

        # Pattern: external URLs.
        pat = re.compile(
            r"https?://(?:fonts\.googleapis\.com|fonts\.gstatic\.com|"
            r"cdn\.|googleapis\.com)"
        )
        targets = []
        for name in _AUTH_PAGE_NAMES:
            targets.append(_AUTH_DIR / f"{name}.html")
        for css_name in ("ds-tokens.css", "auth.css"):
            targets.append(_CSS_DIR / css_name)

        violations = []
        for path in targets:
            content = path.read_text(encoding="utf-8")
            for m in pat.finditer(content):
                violations.append(f"{path.name}: {m.group(0)!r}")
        assert not violations, (
            f"External resource references (spec #12): {violations}"
        )


# ── No token leak in HTML ──────────────────────────────────────────────────
class TestNoTokenLeakInHtml:
    """spec #13 — aucune des 9 pages ne contient en clair les noms ENV des secrets."""

    def test_no_secret_names_in_rendered_html(self):
        """given GET each auth page, when body inspected, then NO `SAMSUNGHEALTH_REGISTRATION_TOKEN`, `SAMSUNGHEALTH_JWT_SECRET`, `SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_SECRET` substring (nor their values).

        spec #13 — defense in depth: server-side templating must never leak secret env var names or values into HTML.
        """
        client = _client()
        forbidden_names = (
            "SAMSUNGHEALTH_REGISTRATION_TOKEN",
            "SAMSUNGHEALTH_JWT_SECRET",
            "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_SECRET",
        )
        forbidden_values = (
            os.environ.get("SAMSUNGHEALTH_REGISTRATION_TOKEN", "__never_match_xxx__"),
            os.environ.get("SAMSUNGHEALTH_JWT_SECRET", "__never_match_xxx__"),
            os.environ.get(
                "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_SECRET", "__never_match_xxx__"
            ),
        )
        for page in _AUTH_PAGE_NAMES:
            r = client.get(f"/auth/{page}")
            assert r.status_code == 200, (
                f"GET /auth/{page} expected 200, got {r.status_code} (impl must serve page)"
            )
            body = r.text
            for name in forbidden_names:
                assert name not in body, (
                    f"page {page} leaks secret env var name {name!r}"
                )
            for value in forbidden_values:
                if value and len(value) > 8:  # avoid false positives on short defaults
                    assert value not in body, (
                        f"page {page} leaks a secret value (length={len(value)})"
                    )
