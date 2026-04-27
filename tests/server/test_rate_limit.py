"""V2.3.3.1 — Rate-limit global (slowapi) + cap pur-IP + composite key.

Tests RED-first contre `server.security.rate_limit` + `server.security.rate_limit_storage`
(modules à créer). Couvre les caps multi-decorator par endpoint + la response shape +
les headers conditionnels prod/dev + le middleware order + l'OPTIONS exclus +
la LRU memory storage cap.

Spec: docs/vault/specs/2026-04-26-v2.3.3.1-rate-limit-lockout.md
  §slowapi setup, §Limites par endpoint, §Réponse 429, §Middleware order H1, §Memory bucket cap H2.
Tests d'acceptation : #1, #2, #3, #4, #5, #6, #9, #12, #13, #14, #24, #43.
"""
from __future__ import annotations

import pytest


_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"


def _register(client, email="rl@example.com", password="longpassword12345"):
    return client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )


class TestLoginRateLimit:
    def test_login_composite_key_5_per_30s_then_429(self, client_pg_ready):
        """given 5 POST /auth/login en 30s avec même (IP, email), when 6e tentative, then 429 rate_limit_exceeded.

        spec §Limites par endpoint — login composite (IP, email) 5/min.
        spec test #1.
        """
        client = client_pg_ready
        _register(client, email="rl-login-1@example.com")
        for _ in range(5):
            r = client.post(
                "/auth/login",
                json={"email": "rl-login-1@example.com", "password": "WRONG-PASSWORD-X"},
            )
            assert r.status_code in (401, 429), (
                f"unexpected status {r.status_code} during warmup: {r.text}"
            )
        sixth = client.post(
            "/auth/login",
            json={"email": "rl-login-1@example.com", "password": "WRONG-PASSWORD-X"},
        )
        assert sixth.status_code == 429, (
            f"expected 429 on 6th login attempt, got {sixth.status_code}: {sixth.text}"
        )
        assert sixth.json().get("detail") == "rate_limit_exceeded"

    def test_login_composite_key_isolation_different_email(self, client_pg_ready):
        """given 5 POST (IP1, email1) → 6e POST (IP1, email1) DOIT être 429, when 1 POST (IP1, email2) immédiatement après, then PAS 429 (composite key isolates).

        spec §Limites par endpoint — composite key (IP, email) ne bloque qu'un email.
        spec test #2.
        """
        # Sanity: la rate-limit module doit exister (sinon ce test ne prouve rien — pré-impl GREEN
        # serait un faux green car aucune limite n'est appliquée).
        from server.security import rate_limit  # noqa: F401  — RED tant que module manquant

        client = client_pg_ready
        _register(client, email="rl-iso-1@example.com")
        _register(client, email="rl-iso-2@example.com")
        for _ in range(5):
            client.post(
                "/auth/login",
                json={"email": "rl-iso-1@example.com", "password": "WRONG-X"},
            )
        # Précondition : email1 doit ÊTRE rate-limited (sinon le test ne prouve pas l'isolation).
        r1 = client.post(
            "/auth/login",
            json={"email": "rl-iso-1@example.com", "password": "WRONG-X"},
        )
        assert r1.status_code == 429, (
            f"PRE-CONDITION: email1 MUST be rate-limited at 6th attempt; got {r1.status_code}"
        )
        # email2 doit passer (composite isolation).
        r2 = client.post(
            "/auth/login",
            json={"email": "rl-iso-2@example.com", "password": "WRONG-X"},
        )
        assert r2.status_code != 429, (
            f"composite key MUST isolate per-email; got 429 on email2: {r2.text}"
        )


class TestPureIpCap:
    def test_login_pure_ip_cap_30_per_minute_blocks_scan(self, client_pg_ready):
        """given 30 POST /auth/login depuis IP1 avec emails distincts, when 31e tentative, then 429.

        spec §Limites par endpoint — login pure-IP cap 30/min anti scan multi-emails.
        spec test #3.
        """
        client = client_pg_ready
        for i in range(30):
            client.post(
                "/auth/login",
                json={"email": f"rl-scan-{i}@example.com", "password": "WRONG-X"},
            )
        r = client.post(
            "/auth/login",
            json={"email": "rl-scan-31@example.com", "password": "WRONG-X"},
        )
        assert r.status_code == 429, (
            f"expected 429 after 30 login attempts (pure-IP cap), got {r.status_code}"
        )


class TestRegisterRateLimit:
    def test_register_3_per_minute_per_ip(self, client_pg_ready):
        """given 3 POST /auth/register/min IP, when 4e, then 429.

        spec §Limites par endpoint — register 3/min par IP.
        spec test #4.
        """
        client = client_pg_ready
        headers = {"X-Registration-Token": _TEST_REGISTRATION_TOKEN}
        for i in range(3):
            client.post(
                "/auth/register",
                headers=headers,
                json={"email": f"rl-reg-{i}@example.com", "password": "longpassword12345"},
            )
        r = client.post(
            "/auth/register",
            headers=headers,
            json={"email": "rl-reg-4@example.com", "password": "longpassword12345"},
        )
        assert r.status_code == 429, (
            f"expected 429 on 4th register attempt, got {r.status_code}: {r.text}"
        )


class TestVerifyResetRateLimit:
    def test_verify_request_composite_3_per_5min(self, client_pg_ready):
        """given 3 POST /auth/verify-email/request en 5min même (IP, email), when 4e, then 429.

        spec §Limites par endpoint — verify request composite 3/5min.
        spec test #5.
        """
        client = client_pg_ready
        _register(client, email="rl-verify@example.com")
        for _ in range(3):
            client.post(
                "/auth/verify-email/request",
                json={"email": "rl-verify@example.com"},
            )
        r = client.post(
            "/auth/verify-email/request",
            json={"email": "rl-verify@example.com"},
        )
        assert r.status_code == 429, (
            f"expected 429 on 4th verify request, got {r.status_code}: {r.text}"
        )

    def test_verify_request_pure_ip_10_per_5min(self, client_pg_ready):
        """given 10 POST verify-request avec emails distincts, when 11e, then 429.

        spec §Limites par endpoint — verify pure-IP 10/5min.
        spec test #6.
        """
        client = client_pg_ready
        for i in range(10):
            _register(client, email=f"rl-vfy-{i}@example.com")
            client.post(
                "/auth/verify-email/request",
                json={"email": f"rl-vfy-{i}@example.com"},
            )
        r = client.post(
            "/auth/verify-email/request",
            json={"email": "rl-vfy-11@example.com"},
        )
        assert r.status_code == 429, (
            f"expected 429 on 11th verify request, got {r.status_code}"
        )

    def test_reset_request_composite_3_per_5min(self, client_pg_ready):
        """given 3 POST /auth/password/reset/request même (IP, email), when 4e, then 429.

        spec §Limites par endpoint — reset request composite 3/5min.
        spec test #7.
        """
        client = client_pg_ready
        _register(client, email="rl-reset@example.com")
        for _ in range(3):
            client.post(
                "/auth/password/reset/request",
                json={"email": "rl-reset@example.com"},
            )
        r = client.post(
            "/auth/password/reset/request",
            json={"email": "rl-reset@example.com"},
        )
        assert r.status_code == 429, (
            f"expected 429 on 4th reset request, got {r.status_code}"
        )


class TestOauthRateLimit:
    def test_oauth_start_10_per_min_per_ip(self, client_pg_ready):
        """given 10 POST /auth/google/start/min IP, when 11e, then 429.

        spec §Limites par endpoint — OAuth start 10/min.
        spec test #9.
        """
        client = client_pg_ready
        for _ in range(10):
            client.post("/auth/google/start", json={})
        r = client.post("/auth/google/start", json={})
        assert r.status_code == 429, (
            f"expected 429 on 11th oauth start, got {r.status_code}"
        )


class TestResponseShape:
    def test_429_response_body_and_retry_after_with_jitter(self, client_pg_ready):
        """given a 429 response from rate-limit handler, when inspected, then body == {"detail": "rate_limit_exceeded"} + Retry-After header présent (avec jitter potentiel +0-5s).

        spec §Réponse 429 Too Many Requests — body uniforme + header Retry-After + jitter.
        spec test #13.
        """
        client = client_pg_ready
        # Force 429 via OAuth start (10/min, simple à saturer)
        for _ in range(10):
            client.post("/auth/google/start", json={})
        r = client.post("/auth/google/start", json={})
        assert r.status_code == 429, (
            f"setup precondition: must hit 429, got {r.status_code}"
        )
        assert r.json().get("detail") == "rate_limit_exceeded", (
            f"expected detail='rate_limit_exceeded', got {r.json()}"
        )
        retry_after = r.headers.get("Retry-After") or r.headers.get("retry-after")
        assert retry_after is not None, "Retry-After header MUST be present on 429"
        # jitter +0-5s → la valeur doit être >= 1 (slowapi reset minimum)
        assert int(retry_after) >= 1, (
            f"Retry-After must be >= 1 (with jitter), got {retry_after!r}"
        )


class TestHeadersConditional:
    def test_x_ratelimit_headers_present_in_test_env(self, client_pg_ready, monkeypatch):
        """given SAMSUNGHEALTH_ENV=test, when 429 received, then X-RateLimit-* headers present (debug visibility en dev/test).

        spec §Réponse 429 — headers X-RateLimit-* uniquement en dev/test (pentester L1).
        spec test #14 (volet dev).
        """
        monkeypatch.setenv("SAMSUNGHEALTH_ENV", "test")
        client = client_pg_ready
        for _ in range(10):
            client.post("/auth/google/start", json={})
        r = client.post("/auth/google/start", json={})
        assert r.status_code == 429
        # X-RateLimit-Limit OU X-RateLimit-Remaining doit être présent en test.
        keys = {k.lower() for k in r.headers.keys()}
        assert "x-ratelimit-limit" in keys, (
            f"X-RateLimit-Limit must be present in test env, got headers: {dict(r.headers)}"
        )

    def test_x_ratelimit_headers_absent_in_production(self, client_pg_ready, monkeypatch):
        """given SAMSUNGHEALTH_ENV=production, when 429 received, then X-RateLimit-* headers absents (anti leak bucket state).

        spec §Réponse 429 — headers X-RateLimit-* uniquement en dev/test (pentester L1).
        spec test #14 (volet prod).
        """
        # En prod, on doit aussi avoir TRUSTED_PROXIES set (boot validator), déjà set par conftest.
        monkeypatch.setenv("SAMSUNGHEALTH_ENV", "production")
        client = client_pg_ready
        for _ in range(10):
            client.post("/auth/google/start", json={})
        r = client.post("/auth/google/start", json={})
        assert r.status_code == 429
        keys = {k.lower() for k in r.headers.keys()}
        assert "x-ratelimit-limit" not in keys, (
            f"X-RateLimit-* MUST be absent in production, got: {dict(r.headers)}"
        )


class TestOptionsExcluded:
    def test_options_preflight_not_counted(self, client_pg_ready):
        """given 50 OPTIONS /auth/login (CORS preflight) puis 5 POST /auth/login (sous le cap 5/min), when 1 POST de plus, then 429 (les 5 POST OK comptent, mais les OPTIONS NON).

        Si OPTIONS comptaient, on serait déjà 429 dès le 1er POST. Le 6e POST DOIT être 429
        prouvant que slowapi est actif ET que OPTIONS ne pollue pas le compteur.
        spec §Limites — OPTIONS preflight CORS ne compte pas.
        spec test #12.
        """
        # Sanity: rate-limit module doit exister.
        from server.security import rate_limit  # noqa: F401

        client = client_pg_ready
        _register(client, email="opt-excluded@example.com", password="goodpassword12345")
        for _ in range(50):
            client.options("/auth/login")
        # 5 POST autorisés (composite cap 5/min)
        for _ in range(5):
            client.post(
                "/auth/login",
                json={"email": "opt-excluded@example.com", "password": "WRONG-X"},
            )
        r6 = client.post(
            "/auth/login",
            json={"email": "opt-excluded@example.com", "password": "WRONG-X"},
        )
        assert r6.status_code == 429, (
            f"6th POST after 5 should be 429 (rate-limit active, OPTIONS excluded); got {r6.status_code}"
        )


class TestMiddlewareOrder:
    def test_slowapi_middleware_runs_before_auth_dep(self, client_pg_ready, monkeypatch):
        """given multiple POST /api/sleep sans token JWT (rate-limit cap monkeypatché à 5), when N+1e tentative, then 429 (PAS 401).

        Prouve que SlowAPIMiddleware catch AVANT le auth check (sinon on aurait N+1 × 401).
        spec §Middleware order (HIGH H1).
        spec test #43.
        """
        # Monkeypatch le cap pour /api/sleep à 5/min via env (impl doit lire SAMSUNGHEALTH_RL_API_POST_CAP).
        monkeypatch.setenv("SAMSUNGHEALTH_RL_API_POST_CAP", "5/minute")
        client = client_pg_ready
        # Strip auto-injected auth header (NO_AUTO_AUTH_FILES couvre ce fichier déjà).
        client.headers.pop("Authorization", None)

        statuses = []
        for _ in range(6):
            r = client.post(
                "/api/sleep",
                json={"start_dt": "2026-04-26T22:00:00Z", "end_dt": "2026-04-27T06:00:00Z"},
            )
            statuses.append(r.status_code)
        # Le 6e doit être 429 — pas 401.
        assert statuses[-1] == 429, (
            f"expected last status 429 (rate-limit AVANT auth), got statuses={statuses}"
        )


class TestMemoryStorageLru:
    def test_lru_cap_evicts_oldest_when_full(self, monkeypatch):
        """given LRU cap = 5, when 6 keys distinctes incrémentées, then la 1ère est évictée.

        spec §Memory bucket cap (HIGH H2) — cap LRU 100_000 entries pour éviter OOM.
        spec test #24.
        """
        from server.security.rate_limit_storage import LruMemoryStorage

        # Cap réduit à 5 pour le test (impl doit accepter override constructor ou ENV).
        storage = LruMemoryStorage(cap=5)
        for i in range(6):
            storage.incr(f"key-{i}", expiry=60, elastic_expiry=False)
        # key-0 doit avoir été évictée (LRU); key-5 doit être présente.
        assert storage.get("key-0") == 0, (
            "LruMemoryStorage MUST evict oldest key when cap exceeded"
        )
        assert storage.get("key-5") >= 1, (
            "newest key MUST still be tracked after eviction"
        )
