---
type: spec
title: "V2.0.5 — structlog observability foundation"
slug: 2026-04-26-v2-structlog-observability
status: delivered
created: 2026-04-26
delivered: 2026-04-26
priority: medium
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs: []
implements:
  - file: server/logging_config.py
    symbols: [configure_logging, get_logger, _processors]
  - file: server/middleware/request_context.py
    symbols: [RequestContextMiddleware, request_id_var, user_id_var]
  - file: server/main.py
    symbols: [app, lifespan]
  - file: requirements.txt
    symbols: [structlog]
tested_by:
  - file: tests/server/test_logging_config.py
    classes: [TestStructlogConfig, TestLogFields]
    methods:
      - test_logger_emits_json
      - test_logger_includes_timestamp_iso8601
      - test_logger_includes_level
      - test_logger_includes_scope
      - test_log_level_from_env
      - test_pretty_renderer_in_dev_mode
  - file: tests/server/test_request_context_middleware.py
    classes: [TestRequestContext, TestRequestIdHeader, TestLatency]
    methods:
      - test_request_id_generated_when_absent
      - test_request_id_propagated_from_header
      - test_request_id_present_in_response_header
      - test_request_id_bound_to_logs
      - test_user_id_default_none
      - test_latency_ms_logged_on_request_complete
      - test_route_template_logged
tags: [v2.0.5, structlog, observability, logging, foundation]
---

# Spec — V2.0.5 structlog observability foundation

## Vision

Mettre en place le pipeline de logs structurés (JSONL) **avant** V2.3 auth, pour que les events `login_success`/`login_fail`/`refresh`/`logout` soient loggables proprement dès leur introduction (pas de retrofit). On migre la stdlib `logging` vers `structlog` avec contexte (`request_id`, `user_id`, `route`, `latency_ms`), middleware FastAPI pour la corrélation, et un format unique JSON en prod (console pretty en dev).

Caddy reverse proxy et CLI `logq` restent **hors scope** — V2.0.6+ les traitera quand on aura besoin de TLS et d'investigation logs en volume.

## Décisions techniques

- **Lib** : `structlog>=24.1` (mature, processors composables, integration stdlib propre).
- **Format** :
  - **prod** (`APP_ENV=prod` ou non défini) : `JSONRenderer` → 1 ligne JSON par log, stdout.
  - **dev** (`APP_ENV=dev`) : `ConsoleRenderer(colors=True)` lisible humain.
- **Niveau** : `LOG_LEVEL` env var (défaut `INFO`). Validation au boot via `lifespan`.
- **Champs standards** émis par les processors : `timestamp` (ISO8601 UTC), `level`, `logger` (scope), `event` (message), `request_id`, `user_id` (None tant qu'auth absente), `route` (template FastAPI ex: `/api/sleep`), `latency_ms` (sur fin de request).
- **Corrélation request** : `contextvars.ContextVar` (`request_id_var`, `user_id_var`) bindées par middleware → `structlog.contextvars.merge_contextvars` injecte dans tout log émis pendant la request.
- **Middleware** :
  - Pure ASGI middleware (pas BaseHTTPMiddleware — perfs + accès propre au scope).
  - Génère `request_id = uuid4().hex` si header `X-Request-ID` absent (sinon réutilise la valeur entrante, max 64 chars sanitisés).
  - Renvoie `X-Request-ID` dans response headers.
  - Mesure `latency_ms` (perf_counter), log `request.complete` à la fin (level INFO succès / WARNING 4xx / ERROR 5xx).
  - Bind `user_id_var = None` par défaut (V2.3 auth dépendency override pour set la vraie valeur).
- **Stdlib bridge** : `structlog.stdlib.LoggerFactory` + propagate vers root logger configuré pour stdout. Toutes les libs tierces (uvicorn, sqlalchemy, alembic) émettent via stdlib → captées par même handler → format unifié.
- **Sanitisation** : pas de PII dans les logs par convention (champ `event` libre mais on évite emails/tokens). Pas de scrubber automatique en V2.0.5 — déclaré dans NOTES.md comme tech debt à reconsidérer V2.3+.
- **Migration progressive** : les `logging.getLogger(__name__)` existants continuent de marcher (bridge stdlib). On migre les call sites significatifs (routers, security, db) vers `get_logger(__name__)` structlog dans la même PR. Scripts `scripts/` restent stdlib pour cette PR.
- **Pas de fichier** : tout sur stdout. Capture/rotation = responsabilité du runtime (docker compose / systemd / Caddy plus tard).
- **Pas de OpenTelemetry** : trop lourd pour V2.0.5. `request_id` UUID suffit pour corrélation locale.

## Livrables

- [ ] `requirements.txt` : ajout `structlog>=24.1`
- [ ] `server/logging_config.py` :
  - `configure_logging(env: str | None = None, level: str | None = None) -> None`
  - `get_logger(name: str) -> structlog.BoundLogger`
  - Liste `_processors` (timestamp, contextvars merger, level adder, renderer JSON ou Console selon env)
  - Lecture `APP_ENV`, `LOG_LEVEL` env vars
- [ ] `server/middleware/__init__.py` (nouveau package)
- [ ] `server/middleware/request_context.py` :
  - `request_id_var: ContextVar[str | None]`
  - `user_id_var: ContextVar[str | None]`
  - `class RequestContextMiddleware` (ASGI pure)
  - Génération `request_id`, propagation header in/out, mesure `latency_ms`, log `request.complete`
- [ ] `server/main.py` :
  - `lifespan` appelle `configure_logging()` au boot
  - `app.add_middleware(RequestContextMiddleware)` (avant tout autre middleware)
  - Remplacement `print(...)` résiduels par logger structlog si présents
- [ ] Migration `logging.getLogger(__name__)` → `get_logger(__name__)` dans :
  - `server/security/crypto.py`
  - `server/database.py`
  - `server/routers/sleep.py`, `heartrate.py`, `steps.py`, `exercise.py`, `mood.py`
- [ ] Tests :
  - `tests/server/test_logging_config.py` (~6 tests)
  - `tests/server/test_request_context_middleware.py` (~7 tests)
- [ ] `.env.example` : ajout `APP_ENV=dev` et `LOG_LEVEL=INFO` (commentés avec defaults documentés)
- [ ] `README.md` : section "Logs" courte (format, env vars, exemple ligne JSON)
- [ ] `NOTES.md` : tech debt "PII scrubber automatique pour logs (V2.3+)"
- [ ] `HISTORY.md` : entry changelog

## Tests d'acceptation

1. **JSON output prod** — `TestStructlogConfig.test_logger_emits_json` : given `APP_ENV=prod`, when `get_logger("test").info("hello", foo=42)`, then la ligne capturée stdout est du JSON valide avec clés `timestamp`/`level`/`logger`/`event`/`foo`.
2. **Timestamp ISO8601 UTC** — `TestLogFields.test_logger_includes_timestamp_iso8601` : la clé `timestamp` matche `^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d+Z$`.
3. **Level présent** — `TestLogFields.test_logger_includes_level` : `level: "info"` (lowercase string).
4. **Scope présent** — `TestLogFields.test_logger_includes_scope` : `logger: "<module name>"`.
5. **LOG_LEVEL respecté** — `TestStructlogConfig.test_log_level_from_env` : `LOG_LEVEL=WARNING` filtre les `info()` (0 ligne capturée).
6. **Console renderer dev** — `TestStructlogConfig.test_pretty_renderer_in_dev_mode` : `APP_ENV=dev` → output **n'est pas** du JSON (pas de `{`/`}` brut), contient le mot `hello`.
7. **request_id généré** — `TestRequestContext.test_request_id_generated_when_absent` : GET sans header `X-Request-ID` → response header `X-Request-ID` présent, format `[a-f0-9]{32}`.
8. **request_id propagé** — `TestRequestIdHeader.test_request_id_propagated_from_header` : GET avec `X-Request-ID: my-trace-123` → response header `X-Request-ID: my-trace-123` (sanitisé : alnum + tirets, max 64 chars).
9. **request_id dans response** — `TestRequestIdHeader.test_request_id_present_in_response_header` : toute response a un `X-Request-ID`.
10. **request_id bindé aux logs** — `TestRequestContext.test_request_id_bound_to_logs` : un log émis pendant une request inclut la clé `request_id`.
11. **user_id None par défaut** — `TestRequestContext.test_user_id_default_none` : log inclut `user_id: null` (placeholder pour V2.3).
12. **latency_ms loggué** — `TestLatency.test_latency_ms_logged_on_request_complete` : à la fin d'une request, un log `event: "request.complete"` est émis avec `latency_ms` (float ≥ 0).
13. **route template loggué** — `TestLatency.test_route_template_logged` : pour `GET /api/sleep?from=...`, `route: "/api/sleep"` (sans query string).
14. **Suite globale** — `pytest tests/` ≥ 248 tests GREEN (235 actuels + 13 nouveaux), 0 régression.

## Out of scope V2.0.5

- Caddy reverse proxy + TLS (différé V2.0.6)
- CLI `logq` query JSONL (différé V2.0.7)
- OpenTelemetry / distributed tracing
- PII scrubber automatique (déclaré tech debt — V2.3+ avec auth)
- Rotation/archive logs (responsabilité runtime)
- Migration `scripts/*` vers structlog (stdlib OK pour CLI)
- Sentry / external sink

## Suite naturelle

V2.3 — Auth system (Phase 1 master plan) : les events auth (`auth.login.success`, `auth.login.fail`, `auth.refresh`, `auth.logout`, `auth.password_reset`) sont émis via `get_logger("server.auth").info(...)` et bénéficient automatiquement de `request_id` + (nouveau) `user_id` bindé via dependency override sur `request_context_var`.
