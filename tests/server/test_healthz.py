"""Phase 6 CI/CD MVP — tests RED→GREEN pour /healthz et /readyz.

Spec: docs/vault/specs/2026-04-30-phase6-cicd-mvp.md §3 + §"Tests d'acceptation" #1-#4.

Endpoints publics (pas de Bearer requis) :
- GET /healthz  → liveness probe, toujours 200 si app boot.
- GET /readyz   → readiness probe : 200 si DB ping OK + alembic head match,
                  503 sinon avec `reason` informatif (mais sans info-leak interne).
"""
from __future__ import annotations


class TestHealthz:
    def test_healthz_returns_200_no_auth(self, client_pg):
        # spec: §3 test #1 — GET /healthz sans bearer → 200 {"status":"ok"}.
        response = client_pg.get("/healthz")
        assert response.status_code == 200, response.text
        assert response.json() == {"status": "ok"}


class TestReadyz:
    def test_readyz_returns_200_when_db_and_alembic_ok(
        self, client_pg_ready, db_session
    ):
        # spec: §3 test #2 — DB up + alembic head match → 200 {"status":"ready"}.
        response = client_pg_ready.get("/readyz")
        assert response.status_code == 200, response.text
        body = response.json()
        assert body["status"] == "ready"

    def test_readyz_returns_503_when_db_down(self, client_pg_ready):
        # spec: §3 test #3 — DB down (mock) → 503 + reason: "db_unreachable".
        from server.database import get_session
        from server.main import app

        def _broken_session():
            class _BrokenSession:
                def execute(self, *args, **kwargs):
                    raise Exception("DB down")

                def close(self):
                    pass

            sess = _BrokenSession()
            try:
                yield sess
            finally:
                sess.close()

        previous = app.dependency_overrides.get(get_session)
        app.dependency_overrides[get_session] = _broken_session
        try:
            response = client_pg_ready.get("/readyz")
        finally:
            if previous is None:
                app.dependency_overrides.pop(get_session, None)
            else:
                app.dependency_overrides[get_session] = previous
        assert response.status_code == 503, response.text
        body = response.json()
        assert body["status"] == "not_ready"
        assert body["reason"] == "db_unreachable"

    def test_readyz_returns_503_when_alembic_mismatch(
        self, client_pg_ready, monkeypatch
    ):
        # spec: §3 test #4 — alembic head mismatch (mock) →
        #     503 {"status":"not_ready","reason":"alembic_mismatch"}.
        from server.routers import health as health_module

        def _wrong_head(self):
            return "WRONG_HEAD_xxx"

        monkeypatch.setattr(
            health_module.ScriptDirectory, "get_current_head", _wrong_head
        )
        response = client_pg_ready.get("/readyz")
        assert response.status_code == 503, response.text
        body = response.json()
        assert body["status"] == "not_ready"
        assert body["reason"] == "alembic_mismatch"

    def test_readyz_no_info_leak_for_private_internals(self, client_pg_ready):
        # spec: §3 D-3 + §10 logs PII — la response ne contient JAMAIS de
        # stack trace, db credentials, ou file paths internes. Shape stricte
        # = {"status", "reason"} (reason peut être null).
        response = client_pg_ready.get("/readyz")
        body = response.json()
        assert set(body.keys()) <= {"status", "reason"}, (
            f"Unexpected keys leaking internals: {set(body.keys())}"
        )
        # Aucun stack trace ni path absolu ne doit apparaître dans les valeurs.
        for value in body.values():
            if value is None:
                continue
            assert "Traceback" not in str(value)
            assert "/home/" not in str(value)
            assert "postgresql" not in str(value).lower()
