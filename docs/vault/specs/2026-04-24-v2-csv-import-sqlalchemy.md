---
type: spec
title: "V2.1.2 — Refonte scripts CSV import + sample generator vers SQLAlchemy/PG"
slug: 2026-04-24-v2-csv-import-sqlalchemy
status: delivered
created: 2026-04-24
delivered: 2026-04-24
priority: medium
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs:
  - 2026-04-24-v2-postgres-migration
  - 2026-04-24-v2-postgres-routers-cutover
implements:
  - file: scripts/import_samsung_csv.py
    symbols: [main, import_sleep, import_sleep_stages, import_steps_hourly, import_steps_daily, import_heart_rate_hourly, import_exercise, import_stress, import_spo2, import_respiratory_rate, import_hrv, import_skin_temperature, import_weight, import_height, import_blood_pressure, import_mood, import_water_intake, import_activity_daily, import_vitality_score, import_floors_daily, import_activity_level, import_ecg]
  - file: scripts/generate_sample.py
    symbols: [main, generate_sleep_sessions, generate_steps_hourly, generate_heart_rate_hourly, generate_exercise_sessions]
tested_by:
  - file: tests/server/test_scripts_csv_import.py
    classes: [TestImportSamsungCsv, TestGenerateSample]
    methods:
      - test_import_sleep_round_trip_pg
      - test_import_idempotent_second_run_zero_inserts
      - test_generate_sample_creates_30d_data
      - test_generate_sample_idempotent
tags: [v2.1, scripts, csv, import, sqlalchemy, cleanup]
---

# Spec — V2.1.2 Refonte scripts CSV import + sample generator vers SQLAlchemy

## Vision

V2.1.1 a coupé `scripts/import_samsung_csv.py` + `generate_sample.py` (raise SystemExit). Cette spec les remet en marche en SQLAlchemy/PG. Plus aucun `INSERT OR IGNORE` SQLite ; tout passe par `pg_insert(...).on_conflict_do_nothing(...).returning(<PK>)` via les models existants. Pas de logique nouvelle — refactor strict.

## Décisions techniques

- **Helper générique `_upsert(db, Model, rows, conflict_cols)`** au top de chaque script : prend une liste de dicts, exécute un seul `pg_insert` par row avec `on_conflict_do_nothing(index_elements=conflict_cols).returning(Model.id)`. Retourne `(inserted, skipped)`. Évite la duplication 21× dans `import_samsung_csv.py`.
- **Pas de batch insert** pour V2.1.2 (perf non-critique, scripts ad-hoc). Si volumétrie devient un souci → spec V2.x avec `pg_insert(...).values([...])` bulk.
- **Sleep stages mapping** : `import_sleep` retourne `{ (sleep_start, sleep_end): uuid }` (pas l'ancien int id, l'UUID v7 directement). `import_sleep_stages` lookup via cette map.
- **`generate_sample.py`** : utilise `get_session()` direct, pas le helper (peu de tables, peu de complexité).
- **Pas de migration de données existantes** : si `health.db` SQLite existe, l'utilisateur doit le réimporter via les CSV. Documenté dans le warning du script.
- **Tests via testcontainers** : déjà setup en V2.1.1, on consomme `pg_url`/`schema_ready`/`db_session`.
- **CSV de test minimal** : pour `TestImportSamsungCsv`, on crée un CSV factice de 3 sleep sessions + 5 stages dans un `tmp_path` pytest et on pointe le script dessus.

## Livrables

- [ ] `scripts/_db_helpers.py` : helper partagé `pg_upsert(db, Model, rows, conflict_cols) -> tuple[int, int]`
- [ ] `scripts/import_samsung_csv.py` : 21 fonctions `import_*` migrées vers SQLAlchemy + main() utilise `get_session()`. Suppression complète `sqlite3`/`INSERT OR IGNORE`/`DB_PATH` inliné. Suppression de `init_db()` (Alembic gère)
- [ ] `scripts/generate_sample.py` : refactor SQLAlchemy + helpers `generate_*` via session.add()
- [ ] `tests/server/test_scripts_csv_import.py` : 4 tests d'acceptation GREEN
- [ ] `README.md` : section Usage mise à jour (commandes `python scripts/generate_sample.py` + `python scripts/import_samsung_csv.py <dir>` réactivées)

## Tests d'acceptation

1. **Import CSV round-trip** — `TestImportSamsungCsv.test_import_sleep_round_trip_pg` : given un CSV de 3 sleep sessions dans `tmp_path`, when `import_sleep(db_session)` est appelé, then 3 rows en `sleep_sessions` PG avec UUID v7 + sleep_start/sleep_end corrects.
2. **Import idempotent** — `TestImportSamsungCsv.test_import_idempotent_second_run_zero_inserts` : given un CSV déjà importé une fois, when on relance `import_sleep`, then 0 inserted, 3 skipped (ON CONFLICT DO NOTHING).
3. **Generate sample 30 jours** — `TestGenerateSample.test_generate_sample_creates_30d_data` : when `main()` est appelé contre un PG vide, then ≥ 30 sleep_sessions + ≥ 30×24 steps_hourly + ≥ 30×24 heart_rate_hourly insérés.
4. **Generate sample idempotent** — `TestGenerateSample.test_generate_sample_idempotent` : appel 2× consécutif → 2e appel retourne 0 inserted (les UNIQUE constraints sleep_start/sleep_end + date/hour empêchent les doublons).

## Out of scope V2.1.2

- Refonte des autres scripts (`scripts/explore_samsung_export.py`, `scripts/dev-mobile.ps1`)
- Optimisation perf (batch insert, asyncio)
- Migration des données SQLite existantes
- Refonte de la logique de parsing CSV (regex, mapping codes Samsung) — strictement copy-paste
