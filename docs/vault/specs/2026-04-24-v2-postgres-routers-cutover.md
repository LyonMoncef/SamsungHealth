---
type: spec
title: "V2.1.1 — Cutover routers SQLite → SQLAlchemy + suppression code SQLite"
slug: 2026-04-24-v2-postgres-routers-cutover
status: delivered
created: 2026-04-24
delivered: 2026-04-24
priority: high
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs:
  - 2026-04-24-v2-postgres-migration
implements:
  - file: server/routers/sleep.py
    symbols: [router, create_sleep_sessions, get_sleep_sessions]
  - file: server/routers/heartrate.py
    symbols: [router]
  - file: server/routers/steps.py
    symbols: [router]
  - file: server/routers/exercise.py
    symbols: [router]
  - file: server/main.py
    symbols: [app, startup]
  - file: server/database.py
    symbols: [get_engine, get_session, SessionLocal]
  - file: server/models.py
    symbols: [SleepSessionOut, SleepStageOut]
tested_by:
  - file: tests/server/test_models_postgres.py
    classes: [TestApiBackCompat]
    methods: [test_get_sleep_period_6m_response_shape_unchanged]
  - file: tests/test_sleep.py
    classes: []
    methods: []
  - file: tests/test_sleep_api_shape.py
    classes: []
    methods: []
  - file: tests/server/test_routers_cutover.py
    classes: [TestHeartRateRouter, TestStepsRouter, TestExerciseRouter, TestNoSqliteResidual]
    methods:
      - test_post_get_heart_rate_round_trip
      - test_post_get_steps_round_trip
      - test_post_get_exercise_round_trip
      - test_no_sqlite_imports_in_server
      - test_no_health_db_in_repo
      - test_get_connection_removed
tags: [v2.1, postgres, sqlalchemy, routers, cutover, cleanup]
---

# Spec — V2.1.1 Cutover routers SQLite → SQLAlchemy + suppression code SQLite

## Vision

La fondation PG (V2.1) est livrée mais les 4 routers continuent à utiliser `sqlite3.Connection` via `get_connection()`. Cette spec finalise la migration : tous les routers passent en SQLAlchemy session contre PG, le code legacy SQLite disparaît complètement, et la suite de tests est cohérente (0 fixture SQLite résiduelle).

Une fois cette PR mergée, `health.db` n'existe plus, `import sqlite3` n'apparait nulle part dans `server/`, et la spec parente V2.1 passe `delivered`.

## Décisions techniques

- **Cutover atomique par router** : un commit par router (sleep, heartrate, steps, exercise) — chacun migre les tests existants vers PG dans le même commit pour éviter une fenêtre où la suite est rouge.
- **Tests legacy migrés vers PG via testcontainers** : `tests/test_sleep.py` + `tests/test_sleep_api_shape.py` doivent utiliser le `db_session` de `tests/server/conftest.py`. Soit on les déplace dans `tests/server/`, soit on remonte le fixture PG au niveau `tests/conftest.py`. **Choix** : on les déplace dans `tests/server/` pour cohérence (tout ce qui exige PG vit dans `tests/server/`).
- **`server/main.py::startup`** : `init_db()` SQLite supprimé, remplacé par un check `alembic check` (warning si migration en attente, pas de blocage app pour permettre dev).
- **`server/database.py`** : suppression de `get_connection`, `DB_PATH`, `init_db`, `_add_col`, et `import sqlite3`. Garde uniquement `get_engine`/`get_session`/`SessionLocal`/`_DEFAULT_PG_URL`.
- **`server/models.py` Pydantic** : `SleepSessionOut.id` passe de `int` à `str` (UUID sérialisé). `SleepStageOut.id` + `session_id` idem. Front Nightfall n'utilise les ids qu'en passe-plat — non bloquant.
- **`scripts/import_samsung_csv.py`** : si utilise `sqlite3` direct, à migrer vers SQLAlchemy session aussi (audit + adaptation dans le commit cleanup).
- **Pre-existing front URL `?period=6m`** : n'existe pas dans Nightfall (frontend utilise `from`/`to`). Le test back-compat sera adapté pour utiliser `from`/`to` plutôt qu'inventer un paramètre. Documenté.

## Livrables

- [ ] `server/routers/sleep.py` migré vers `Session = Depends(get_session)`, requêtes SQLAlchemy 2.x style `select(SleepSession).where(...)`, response models adaptés (id en `str`)
- [ ] `server/routers/heartrate.py` idem
- [ ] `server/routers/steps.py` idem
- [ ] `server/routers/exercise.py` idem
- [ ] `server/main.py` : suppression `init_db()` startup hook, remplacé par log info "Postgres ready (alembic head: <rev>)"
- [ ] `server/database.py` : suppression complète SQLite (legacy `get_connection`/`DB_PATH`/`init_db`/`_add_col`/`import sqlite3`)
- [ ] `server/models.py` : `SleepSessionOut`/`SleepStageOut` ids `str`, autres response models adaptés si nécessaire
- [ ] `scripts/import_samsung_csv.py` audit + migration SQLAlchemy si utilise sqlite3
- [ ] `tests/conftest.py` : suppression du monkey-patch SQLite (`HEALTH_TEST_DB`, `db_module.DB_PATH`, `init_db()` dans `clean_db` fixture). Le `client` fixture utilise `app.dependency_overrides[get_session]` pour rediriger vers le testcontainer PG
- [ ] `tests/test_sleep.py` + `tests/test_sleep_api_shape.py` : déplacés dans `tests/server/` ou adaptés pour utiliser le fixture PG
- [ ] `tests/server/test_routers_cutover.py` : nouveaux tests round-trip pour heartrate/steps/exercise + sentinelle "no SQLite residual"
- [ ] `health.db` supprimé du repo + `*.db` ajouté à `.gitignore`
- [ ] `README.md` : suppression mention SQLite, ajout setup PG (prérequis Docker, `make db-up && make db-migrate`)
- [ ] Spec parente V2.1 passe `status: delivered`, `delivered: 2026-04-24` dans le commit final

## Tests d'acceptation

1. **Back-compat sleep** — `TestApiBackCompat.test_get_sleep_period_6m_response_shape_unchanged` (renommé → `test_get_sleep_response_shape_unchanged`, params adaptés `from`/`to`/`include_stages`) : given des sessions seedées via SQLAlchemy, when GET `/api/sleep?from=...&to=...&include_stages=true`, then shape JSON identique à la réponse SQLite legacy (clés `id` (str UUID), `sleep_start`, `sleep_end`, `created_at`, `stages[]`).
2. **Round-trip heartrate** — `TestHeartRateRouter.test_post_get_heart_rate_round_trip` : POST bulk → GET filter → payload identique aux records insérés.
3. **Round-trip steps** — `TestStepsRouter.test_post_get_steps_round_trip` : idem.
4. **Round-trip exercise** — `TestExerciseRouter.test_post_get_exercise_round_trip` : idem.
5. **Pas de SQLite résiduel** — `TestNoSqliteResidual.test_no_sqlite_imports_in_server` : grep `^(import sqlite3|from sqlite3 )` dans `server/**/*.py` → 0 match.
6. **Pas de health.db** — `TestNoSqliteResidual.test_no_health_db_in_repo` : `health.db` ne doit pas exister à la racine du repo.
7. **get_connection supprimé** — `TestNoSqliteResidual.test_get_connection_removed` : `from server.database import get_connection` → `ImportError`.
8. **Suite legacy intacte** — `pytest tests/test_sleep.py tests/test_sleep_api_shape.py` (ou leur emplacement migré) : 12 tests GREEN contre PG.
9. **Suite globale** — `pytest tests/` (full repo) : ≥ 187 tests GREEN, 0 erreur SQLite.

## Out of scope V2.1.1 (à ne PAS faire dans cette PR)

- Toute feature produit (chiffrement, JWT, etc.) — domaine V2.2+
- Refactor `import_samsung_csv.py` au-delà de la suppression `sqlite3` (refonte algo = autre spec)
- Async SQLAlchemy
- Optimisations perf (pool tuning, index supplémentaires)
