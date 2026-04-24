---
type: spec
title: "V2.1 — Migration SQLite → Postgres + Alembic + UUID v7"
slug: 2026-04-24-v2-postgres-migration
status: delivered
created: 2026-04-24
delivered: 2026-04-24
priority: high
related_plans:
  - 2026-04-23-plan-v2-refactor-master
implements:
  - file: docker-compose.yml
    symbols: [postgres]
  - file: server/database.py
    symbols: [get_engine, get_session, SessionLocal]
  - file: server/db/models.py
    symbols: [Base, SleepSession, SleepStage, HeartRateHourly, StepsDaily, StepsHourly, ExerciseSession, ActivityDaily, Stress, Spo2, RespiratoryRate, Hrv, SkinTemperature, Weight, Height, BloodPressure, Mood, WaterIntake, VitalityScore, FloorsDaily, ActivityLevel, Ecg]
  - file: server/db/uuid7.py
    symbols: [uuid7]
  - file: alembic/env.py
    symbols: []
  - file: alembic/versions/0001_initial.py
    symbols: [upgrade, downgrade]
  - file: alembic.ini
    symbols: []
tested_by:
  - file: tests/server/test_postgres_bootstrap.py
    classes: [TestBootstrap]
    methods: [test_alembic_upgrade_creates_schema, test_alembic_idempotent, test_alembic_downgrade_reverses]
  - file: tests/server/test_uuid7.py
    classes: [TestUuid7]
    methods: [test_monotonic_within_ms, test_version_field_is_7, test_timestamp_extractable]
  - file: tests/server/test_models_postgres.py
    classes: [TestSleepSessionPersistence, TestApiBackCompat]
    methods:
      - test_insert_sleep_session_assigns_uuid_and_created_at
      - test_read_sleep_session_by_uuid
      - test_get_sleep_period_6m_response_shape_unchanged
      - test_sleep_session_with_stages_atomic
tags: [v2, postgres, alembic, migration, backend, infrastructure]
---

# Spec — V2.1 Migration SQLite → Postgres + Alembic + UUID v7

## Vision

Sortir SQLite (single-file, dev-only, schéma fragile via `_add_col` runtime) au profit de Postgres 16 versionné par Alembic, avec UUID v7 partout pour préparer la suite (chiffrement champ-par-champ, multi-user, audit log, sync mobile). Cette spec ne livre **aucune feature produit** — elle pose la fondation que les specs V2.2+ (AES-GCM, JWT, structlog) consommeront.

Pas de port de données : SQLite était dev-local seulement, on repart sur un schéma neuf et l'utilisateur ré-importe ses CSV via le pipeline existant.

## Décisions techniques

- **PG 16** (mature, largement supporté en cloud managé) — pas PG 17, on attend la maturité écosystème extensions.
- **Tout côté Python, DB agnostique au-dessus de SQL standard** : UUID v7 généré app via `uuid_utils` (lib pure Python), chiffrement AES-256-GCM via `cryptography` (specs futures). Pas d'extension `pgcrypto` ni `uuid-ossp` requise → on peut basculer vers un autre RDBMS sans build custom.
- **SQLAlchemy 2.x ORM** (typing-friendly, `Mapped[...]`), pool via `psycopg[binary]` (driver synchrone — async différé, pas requis V2.1).
- **Alembic autogenerate baseline + revue manuelle systématique** : `0001_initial.py` régénéré à chaud puis relu ligne à ligne avant commit. Migrations manuelles obligatoires dès qu'un type custom (UUID v7, BYTEA chiffré) entre en jeu.
- **UUID v7 en PK partout** (timestamp-sortable → indices `created_at` redondants évitables sur certaines tables). FK reliées en UUID. `created_at`/`updated_at` (timestamptz) systématiques sur toutes les tables (futur audit + sync).
- **Pas de port de données** : on drop SQLite, on régénère, l'utilisateur ré-importe ses CSV. Documenté dans `README.md` post-merge.
- **Tests d'intégration via `testcontainers-postgres`** (spin un PG 16 jetable par session pytest) — pas de PG fixture partagée, isolation forte.
- **API publique inchangée** : les endpoints `GET /api/sleep`, `GET /api/heart-rate`, etc. retournent le même JSON shape (frontend Nightfall ne bouge pas). Les UUID sortent en `id` string ISO mais le frontend ne les exploite pas (juste le passe-plat).
- **Suppression complète du code SQLite** : `health.db`, imports `sqlite3`, `_add_col`, `init_db()`. Pas de double-write transitoire.

## Livrables

- [ ] `docker-compose.yml` : service `postgres:16-alpine` + volume nommé + healthcheck + port 5432 exposé pour dev
- [ ] `requirements.txt` : ajout `sqlalchemy>=2.0`, `psycopg[binary]>=3.1`, `alembic>=1.13`, `uuid_utils>=0.10`, `testcontainers[postgres]>=4.0` (dev)
- [ ] `server/db/uuid7.py` : helper `uuid7() -> uuid.UUID` (wrapper sur `uuid_utils.uuid7`) + `Uuid7` SQLAlchemy `TypeDecorator`
- [ ] `server/database.py` : remplacé par `get_engine()` (singleton, lit `DATABASE_URL` env) + `get_session()` (FastAPI dep) + `SessionLocal`. Signature `get_connection()` legacy supprimée.
- [ ] `server/db/models.py` : tous les models SQLAlchemy 2.x (22 tables, `Mapped[UUID]` PK, `created_at`/`updated_at`)
- [ ] `alembic.ini` + `alembic/env.py` (lit `DATABASE_URL`) + `alembic/versions/0001_initial.py` (création schema complet, autogenerate validé manuellement)
- [ ] Refactor `server/routers/{sleep,heart_rate,steps,activity}.py` : queries via SQLAlchemy session, lectures/écritures UUID, response models inchangés
- [ ] Refactor `server/import_service.py` (et autres consumers SQLite) : passage SQLAlchemy
- [ ] Suppression `health.db` du repo + entrée `.gitignore` pour `*.db`
- [ ] `Makefile` : targets `db-up` (compose up postgres), `db-down`, `db-migrate` (alembic upgrade head), `db-reset` (drop + recreate)
- [ ] Tests : `tests/server/test_postgres_bootstrap.py`, `tests/server/test_uuid7.py`, `tests/server/test_models_postgres.py` GREEN
- [ ] `README.md` : section "Setup DB" mise à jour (prérequis Docker, commandes Make), mention "DB SQLite supprimée — ré-importer les CSV après migration"

## Tests d'acceptation

Décrits given/when/then pour mapper directement sur les classes/méthodes de `tested_by:`.

1. **Bootstrap idempotent** — `TestBootstrap.test_alembic_upgrade_creates_schema` : given un PG 16 vide (testcontainer), when on lance `alembic upgrade head`, then les 22 tables existent avec leurs PK UUID, indices déclarés, et `created_at`/`updated_at` non-NULL.
2. **Upgrade rejouable** — `TestBootstrap.test_alembic_idempotent` : given un PG déjà migré, when on relance `alembic upgrade head`, then no-op (exit 0, aucune migration appliquée).
3. **Downgrade réversible** — `TestBootstrap.test_alembic_downgrade_reverses` : given un PG migré, when on lance `alembic downgrade base`, then schéma vide, sans erreur (sécurité ops).
4. **UUID v7 monotone par milliseconde** — `TestUuid7.test_monotonic_within_ms` : given 100 UUID générés dans la même ms, when on les trie, then ordre = ordre de génération.
5. **UUID v7 version field** — `TestUuid7.test_version_field_is_7` : `uuid7().version == 7`.
6. **UUID v7 timestamp extractable** — `TestUuid7.test_timestamp_extractable` : on extrait les 48 bits timestamp d'un UUID v7 généré à `t`, et `|extracted - t| < 5ms`.
7. **Insert sleep session avec UUID + timestamps auto** — `TestSleepSessionPersistence.test_insert_sleep_session_assigns_uuid_and_created_at` : given une session valide via le service, when on l'insère, then row PG a `id` UUID v7 + `created_at` non-NULL + `updated_at == created_at`.
8. **Read by UUID** — `TestSleepSessionPersistence.test_read_sleep_session_by_uuid` : given une session insérée, when GET via repo `get_session_by_id(uuid)`, then retourne l'objet correct.
9. **Atomicité sleep_session + sleep_stages** — `TestSleepSessionPersistence.test_sleep_session_with_stages_atomic` : given un import qui insère une session + N stages, when le commit échoue mid-write (mock erreur sur stage[2]), then rollback complet (0 row dans aucune des 2 tables).
10. **Back-compat API frontend** — `TestApiBackCompat.test_get_sleep_period_6m_response_shape_unchanged` : given un set de sessions seedées, when GET `/api/sleep?period=6m`, then le shape JSON (clés, types) matche un snapshot capturé avant migration (régression Nightfall évitée).

## Suite naturelle

**Découpage en 2 PR** (décidé après livraison fondation, 2026-04-24) :

1. **PR `feat/v2-postgres-migration` (cette spec, fondation)** — livre engine + 21 models + alembic + docker + helper UUID v7. **9/10 tests** d'acceptation GREEN. État : `in_progress` jusqu'à la PR fille mergée.
2. **Spec fille `2026-04-24-v2-postgres-routers-cutover`** — finalise le refactor des 4 routers (`sleep`, `heart_rate`, `steps`, `exercise`) + suppression complète du code SQLite (`init_db`, `get_connection`, `_add_col`, `health.db`). Migre les 12 tests legacy SQLite vers PG (testcontainers). Fait passer le **10e test back-compat** GREEN. Quand mergée → spec V2.1 passe `delivered`.

Spec V2.2 (à créer après V2.1.1) : `2026-04-XX-v2-aes256-gcm-encrypted-fields` — premier endpoint chiffrant les champs santé sensibles via `cryptography` AES-GCM, exploitant le typage SQLAlchemy `Encrypted(BYTEA)` qui s'appuie sur l'infra UUID v7 + Alembic livrée ici.

## Out of scope V2.1 (à ne PAS faire dans cette PR)

- Chiffrement AES-256-GCM (spec V2.2)
- JWT auth + multi-user (spec V2.3)
- structlog observability (spec V2.4)
- Async SQLAlchemy (`asyncpg`) — restera synchrone tant que pas de hot path bloquant identifié
- Port des données SQLite existantes — l'utilisateur ré-importe ses CSV
