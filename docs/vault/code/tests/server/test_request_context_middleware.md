---
type: code-source
language: python
file_path: tests/server/test_request_context_middleware.py
git_blob: 9b4584930eb77f523726d18d625e683a02a31054
last_synced: '2026-04-26T14:46:49Z'
loc: 218
annotations: []
imports:
- json
- re
- pytest
- fastapi.testclient
exports:
- _build_app
- TestRequestContext
- TestRequestIdHeader
- TestLatency
tags:
- code
- python
---

# tests/server/test_request_context_middleware.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_request_context_middleware.py`](../../../tests/server/test_request_context_middleware.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Tests RED — V2.0.5 structlog observability foundation : request context middleware.

Mappé sur frontmatter `tested_by:` du spec
docs/vault/specs/2026-04-26-v2-structlog-observability.md.

Classes:
- TestRequestContext
- TestRequestIdHeader
- TestLatency

Cible:
- `from server.middleware.request_context import RequestContextMiddleware, request_id_var, user_id_var`
- `from server.logging_config import configure_logging, get_logger`

Note RED: `server.middleware.request_context` n'existe pas encore (et le
package `server.middleware` non plus). `structlog` n'est pas encore dans
`requirements.txt`. Les tests doivent échouer à l'import — c'est le contrat RED.

On construit une mini-app FastAPI inline dans chaque test (plutôt que de
réutiliser `server.main.app`) pour isoler le middleware et tester son
comportement seul.
"""
import json
import re

import pytest
from fastapi.testclient import TestClient


def _build_app():
    """Construit une app FastAPI minimale avec le RequestContextMiddleware
    monté + une route qui émet un log via structlog pendant la request, pour
    pouvoir asserter le binding contextvars → log entries.

    Lazy imports : tant que `server.logging_config` /
    `server.middleware.request_context` n'existent pas, l'appel à _build_app()
    lève ModuleNotFoundError → RED clair par test, pas en collection.
    """
    from fastapi import FastAPI

    from server.logging_config import configure_logging, get_logger
    from server.middleware.request_context import RequestContextMiddleware

    configure_logging()
    log = get_logger("server.test_app")

    app = FastAPI()
    app.add_middleware(RequestContextMiddleware)

    @app.get("/api/sleep")
    def get_sleep():
        log.info("inside_handler")
        return {"ok": True}

    @app.get("/api/error")
    def get_error():
        raise ValueError("boom")

    return app


class TestRequestContext:
    def test_request_id_generated_when_absent(self, monkeypatch):
        """
        given une request GET sans header X-Request-ID,
        when le middleware traite la request,
        then la response contient un header X-Request-ID au format [a-f0-9]{32}.
        """
        # spec §Tests d'acceptation #7
        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")

        app = _build_app()
        client = TestClient(app)

        resp = client.get("/api/sleep")
        assert resp.status_code == 200
        assert "x-request-id" in {k.lower() for k in resp.headers.keys()}
        rid = resp.headers["x-request-id"]
        assert re.match(r"^[a-f0-9]{32}$", rid), f"rid={rid!r} ne matche pas uuid4().hex"

    def test_request_id_bound_to_logs(self, monkeypatch, capsys):
        """
        given une request,
        when un log est émis pendant le handler,
        then l'entry de log contient la clé `request_id` égale à la valeur
             du header X-Request-ID renvoyé.
        """
        # spec §Tests d'acceptation #10
        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")

        app = _build_app()
        client = TestClient(app)
        resp = client.get("/api/sleep")
        rid = resp.headers["x-request-id"]

        captured = capsys.readouterr()
        out = (captured.out + captured.err)
        # On parse toutes les lignes JSON et on cherche celle du handler
        lines = [json.loads(ln) for ln in out.strip().splitlines() if ln.strip().startswith("{")]
        handler_lines = [ln for ln in lines if ln.get("event") == "inside_handler"]
        assert handler_lines, f"Aucun log `inside_handler` capturé. Lignes: {lines}"
        assert handler_lines[0].get("request_id") == rid

    def test_user_id_default_none(self, monkeypatch, capsys):
        """
        given une request sans auth (V2.0.5 = pas d'auth),
        when un log est émis,
        then l'entry contient `user_id: null` (placeholder pour V2.3 auth).
        """
        # spec §Tests d'acceptation #11
        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")

        app = _build_app()
        client = TestClient(app)
        client.get("/api/sleep")

        captured = capsys.readouterr()
        out = (captured.out + captured.err)
        lines = [json.loads(ln) for ln in out.strip().splitlines() if ln.strip().startswith("{")]
        handler_lines = [ln for ln in lines if ln.get("event") == "inside_handler"]
        assert handler_lines, f"Aucun log handler capturé. Lignes: {lines}"
        # null en JSON ↔ None en Python : la clé doit être présente avec valeur None.
        assert "user_id" in handler_lines[0], "Clé user_id absente du log"
        assert handler_lines[0]["user_id"] is None


class TestRequestIdHeader:
    def test_request_id_propagated_from_header(self, monkeypatch):
        """
        given une request GET avec header X-Request-ID: my-trace-123,
        when le middleware traite la request,
        then la response renvoie X-Request-ID: my-trace-123 (sanitisé alnum+tirets, max 64 chars).
        """
        # spec §Tests d'acceptation #8
        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")

        app = _build_app()
        client = TestClient(app)

        resp = client.get("/api/sleep", headers={"X-Request-ID": "my-trace-123"})
        assert resp.status_code == 200
        assert resp.headers["x-request-id"] == "my-trace-123"

    def test_request_id_present_in_response_header(self, monkeypatch):
        """
        given n'importe quelle request (succès ou erreur),
        when le middleware traite la response,
        then la response a toujours un header X-Request-ID non vide.
        """
        # spec §Tests d'acceptation #9
        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")

        app = _build_app()
        client = TestClient(app, raise_server_exceptions=False)

        # Succès
        ok = client.get("/api/sleep")
        assert ok.headers.get("x-request-id"), "X-Request-ID absent sur 200"

        # Erreur 500
        err = client.get("/api/error")
        assert err.headers.get("x-request-id"), "X-Request-ID absent sur 500"


class TestLatency:
    def test_latency_ms_logged_on_request_complete(self, monkeypatch, capsys):
        """
        given une request,
        when le middleware termine,
        then un log `event: "request.complete"` est émis avec une clé
             `latency_ms` (float >= 0).
        """
        # spec §Tests d'acceptation #12
        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")

        app = _build_app()
        client = TestClient(app)
        client.get("/api/sleep")

        captured = capsys.readouterr()
        out = (captured.out + captured.err)
        lines = [json.loads(ln) for ln in out.strip().splitlines() if ln.strip().startswith("{")]
        complete = [ln for ln in lines if ln.get("event") == "request.complete"]
        assert complete, f"Aucun log `request.complete` capturé. Lignes: {lines}"
        assert "latency_ms" in complete[0], "Clé latency_ms absente"
        latency = complete[0]["latency_ms"]
        assert isinstance(latency, (int, float)), f"latency_ms doit être numérique, pas {type(latency)}"
        assert float(latency) >= 0.0

    def test_route_template_logged(self, monkeypatch, capsys):
        """
        given une request GET /api/sleep?from=2026-01-01,
        when le middleware logge `request.complete`,
        then la clé `route` vaut "/api/sleep" (template FastAPI sans query string).
        """
        # spec §Tests d'acceptation #13
        monkeypatch.setenv("APP_ENV", "prod")
        monkeypatch.setenv("LOG_LEVEL", "INFO")

        app = _build_app()
        client = TestClient(app)
        client.get("/api/sleep", params={"from": "2026-01-01"})

        captured = capsys.readouterr()
        out = (captured.out + captured.err)
        lines = [json.loads(ln) for ln in out.strip().splitlines() if ln.strip().startswith("{")]
        complete = [ln for ln in lines if ln.get("event") == "request.complete"]
        assert complete, f"Aucun log `request.complete` capturé. Lignes: {lines}"
        assert complete[0].get("route") == "/api/sleep", (
            f"route attendue '/api/sleep', got {complete[0].get('route')!r}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_build_app` (function) — lines 31-60
- `TestRequestContext` (class) — lines 63-128
- `TestRequestIdHeader` (class) — lines 131-168
- `TestLatency` (class) — lines 171-218

### Imports
- `json`
- `re`
- `pytest`
- `fastapi.testclient`

### Exports
- `_build_app`
- `TestRequestContext`
- `TestRequestIdHeader`
- `TestLatency`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-26-v2-structlog-observability]] — classes: `TestRequestContext`, `TestRequestIdHeader`, `TestLatency` · methods: `test_request_id_generated_when_absent`, `test_request_id_propagated_from_header`, `test_request_id_present_in_response_header`, `test_request_id_bound_to_logs`, `test_user_id_default_none`, `test_latency_ms_logged_on_request_complete`, `test_route_template_logged`
