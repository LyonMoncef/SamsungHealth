# History

## Features

| Feature | Files | Commit |
|---------|-------|--------|
| Samsung Health CSV import — full DB schema (21 tables) | `server/database.py`, `scripts/import_samsung_csv.py`, `scripts/explore_samsung_export.py` | [`d032741`](#2026-04-21-d032741) |
| Dev mobile — WSL2 port forwarding + Android cleartext | `scripts/dev-mobile.ps1`, `Makefile`, `android-app/` | [`646aeaa`](#2026-04-21-646aeaa) |
| Nightfall sleep dashboard | `static/index.html`, `static/dashboard.css`, `static/dashboard.js`, `static/api.js` | [`b5cacc7`](#2026-04-21-b5cacc7) |
| Workflow bootstrap — CI, labels, hooks, tests | `.github/`, `.githooks/`, `Makefile`, `tests/` | [`939f5ef`](#2026-04-21-939f5ef) |
| Phase 3: Steps, heart rate, exercise + tabbed dashboard | `server/`, `static/`, `scripts/`, `android-app/` | [`242040a`](#2026-02-16-242040a) |
| Phase 2: Sleep stages + color-coded calendar + Android app | `server/`, `static/`, `scripts/`, `android-app/` | [`8d5cfb0`](#2026-02-16-8d5cfb0) |
| Phase 1: Backend + DB + UI + Scripts | `server/`, `static/`, `scripts/`, `requirements.txt` | [`6200a93`](#2026-02-16-6200a93) |
| Project scaffolding | `.gitignore`, `README.md`, `NOTES.md`, `HISTORY.md`, `ROADMAP.md` | [`6cc83dc`](#2026-02-16-6cc83dc) |

---

## Checkpoint

### `checkpoint-before-v2-refactor-2026-04-23` — 2026-04-23 → `5343c9b`
chore(checkpoint): safety tag before V2 full security/RGPD refactor
- Reason: Major refactor — migration SQLite → Postgres, multi-user auth (email+pwd + Google OAuth), chiffrement AES-256-GCM, endpoints RGPD, Android Compose shell + dual-track UI (WebView dev / Compose natif prod), logging structuré sharded, docker-compose dev/prod
- Scope: tout le repo — `server/`, `android-app/`, `static/`, `tests/`, `alembic/` (nouveau), `docker-compose*.yml` (nouveau), CI workflows
- Target branch : `refactor/v2` long-lived, target des PRs de la refonte
- Anchor: `5343c9b feat(data): Samsung Health CSV import pipeline (#5)` — origin/main HEAD au moment de la bifurcation
- Plan de référence : `vault/02_Projects/SamsungHealth/specs/2026-04-23-plan-v2-refactor-master.md`
- Plan multi-agents : `vault/02_Projects/SamsungHealth/specs/2026-04-23-plan-v2-multi-agents-architecture.md`
- Estimation : ~6-8 semaines, livraison incrémentale (V1 = WebView, V2 = Compose natif)
- [CHECKPOINT]

### `nightfall-loom-v1` — 2026-04-23 → `ee09025`
chore(release-archive): tag état de l'app au moment de l'enregistrement loom
- Scope: `static/dashboard.js`, `static/dashboard.css`, `static/index.html`, `static/api.js`
- Branche source : `feat/nightfall-history` (PR #6 closed sans merge le 2026-04-23 — refonte V2 priorisée)
- Differs from `nightfall-dataviz-v1` par les commits de polish loom :
  - Period filter bar (7j/30j/full) + radial sous l'hypnogram (cc50998, d146174)
  - Stage tooltips riches sur timeline et radial clock (c776c96)
  - Bed/wake times prominents au-dessus des KPIs hypnogram (551a17a)
  - Mobile spacing épuré (4a71955, a2f22d1)
  - Perf partial-render donut + labels donut + period 6m default (ee09025)
- Codex : `vault/02_Projects/SamsungHealth/codex/release/nightfall-loom-v1.md`
- Pour ressortir l'état : `git checkout nightfall-loom-v1 && make dev`
- [CHECKPOINT]

### `nightfall-dataviz-v1` — 2026-04-22 → `1631c80`
[CHECKPOINT] Reference tag for all Nightfall dataviz chapters before refactor.
- Branche source : `feat/nightfall-history` (archivée, PR #6 closed)
- Scope: `static/dashboard.js`, `static/dashboard.css`
- Chapters included: heatmap (01), stacked timeline (02), hypnogram (03), radial (04), small multiples (05), ridgeline (06), top nights cards (07), agenda (08), metrics + month selector + 4 full-history views (09), elasticity / duration regularity (10), drift clock with animated playback (11)
- Key features: gap-based episode grouping, personal median sleep target, lag-1 autocorrelation, cycle count, bedtime scatter 24h, drift clock demo mode + smooth interpolation

---

## Changelog

### 2026-04-23 `40a195f`
feat(phase-a.5): subagent code-cartographer + 3 skills (/sync-vault /annotate /anchor-review) avec linked-list pattern
- Added `.claude/agents/code-cartographer.md` — subagent (tools : Read/Write/Grep/Glob/Bash, model sonnet, color teal). Workflow : invoque le CLI cartographer pour `full|diff|check`, gère les `AnnotationOpBrief` (create/update/delete/anchor-review). Règle stricte : sync sens unique code+annotations → notes vault rendues, jamais l'inverse
- Added `.claude/skills/sync-vault/SKILL.md` — wrapper du CLI. Sans arg = mode diff sur staged+modified. Args : `--full`, `--diff <files>`, `--check`. Next default `/commit`
- Added `.claude/skills/annotate/SKILL.md` — CRUD annotation. Forms : `<slug> --at <file>:<line>`, `<slug> --range <file>:<start>-<end>`, `edit <slug>`, `delete <slug>`. Valide slug regex, refuse collisions, inject marker + crée annotation file + re-render. Next `/sync-vault --diff`
- Added `.claude/skills/anchor-review/SKILL.md` — résolution orphans. Sans arg = liste. Avec slug = fuzzy match candidats + validation humaine OBLIGATOIRE avant ré-injection. Déplace l'annotation hors `_orphans/` + status active. Next `/commit`
- Tests : 129/129 GREEN (pas de tests sur les .md de skills — invoqués manuellement)
- Phase A.5 task #8 ✓ ; reste #9 hook pre-commit, #10 changelog gen, #11 plan-keeper extension

### 2026-04-23 `46f85e4`
feat(phase-a.5): CLI code-cartographer (full/diff/check) + bootstrap initial 47 notes vault + 3 indexes (5 tests GREEN)
- Added `agents/cartographer/cli.py` — `run(mode, repo_root, vault_root, ...)` orchestrateur + `main()` argv (`--full`, `--diff <files>`, `--check`). Découvre sources via globs, parse markers, walk AST, résout anchors, render notes vault, écrit `_index/` (mode full uniquement), retourne `CartographyReport`. `--check` = dry-run, exit 1 si new orphans
- Added `tests/agents/test_cli.py` — 5 tests (full crée notes + indexes, diff filtre, check pass/fail)
- **Bootstrap initial exécuté** : `python -m agents.cartographer.cli --full` → 47 notes vault dans `docs/vault/code/` (modules : agents/, scripts/, server/, static/, android-app/) + 3 indexes (`orphans.md`, `coverage.md`, `annotations-by-tag.md`)
- Removed `docs/vault/.gitkeep-structure` placeholder (vault maintenant peuplé)
- Tests : 129/129 GREEN (124 + 5 CLI)
- Phase A.5 task #7 ✓ ; reste #8 subagent+skills, #9 hook pre-commit, #10 changelog gen, #11 plan-keeper extension

### 2026-04-23 `f803fd8`
feat(phase-a.5): modules render cartographer (anchor_resolver + note_renderer + orphan_detector + index_generator) + 14 tests GREEN
- Added `agents/cartographer/anchor_resolver.py` — `resolve_anchors_for_file()` retourne `ResolveResult` (3 buckets : `active` avec lignes refreshed, `orphans` avec last_seen, `unmatched` slugs en code sans annotation file)
- Added `agents/cartographer/note_renderer.py` — `render_note()` produit la note vault complète : frontmatter (type/language/git_blob/last_synced ISO-Z/loc/annotations/imports/exports/tags) + H1 + Code mirror callout + orphan warning callout (si orphans) + code interleaved avec `> [!note]+` callouts au bon endroit (single = après marker line, range = après end_line) + appendix symbols/imports/exports
- Added `agents/cartographer/orphan_detector.py` — `detect_orphans()` retourne `OrphanDiff` (new_orphans : annotation active mais slug absent du code ; resolved_orphans : annotation status=orphan mais marker revenu)
- Added `agents/cartographer/index_generator.py` — `generate_orphans_index()`, `generate_coverage_index()` (source files sans annotation), `generate_tags_index()` (groupe par tag)
- Tests : 124/124 GREEN (110 existants + 4 anchor_resolver + 4 note_renderer + 3 orphan_detector + 3 index_generator)
- Phase A.5 task #6 ✓ ; reste #7 CLI bootstrap, #8 subagent+skills, #9 hook, #10 changelog gen, #11 plan-keeper extension

### 2026-04-23 `3032836`
feat(phase-a.5): contrats Pydantic cartographer + 4 modules core (markers/walker/IO/injector) + 42 tests GREEN
- Added `agents/contracts/cartographer.py` (7 types) — `AnchorKind`, `AnchorLocation`, `Annotation` (slug regex `^[a-z0-9][a-z0-9-]{2,40}$`), `CartographyBrief` (mode full/diff/check), `CartographyReport` (overall complete/partial/failed, next_recommended commit/review/anchor-review/none), `AnnotationOpBrief`/`Report`
- Updated `agents/contracts/__init__.py` — +7 re-exports
- Added `agents/cartographer/markers.py` — `parse_markers()` détecte `@vault:slug` dans Python/JS/Kotlin/HTML/CSS, gère single + range begin/end + non-contigu (même slug ×N), `infer_language()`, `MarkerParseError` sur unbalanced begin/end ou slug invalide
- Added `agents/cartographer/annotation_io.py` — `resolve_annotation_path()` (single-file → `<pkg>/<file>/<slug>.md`, cross-file → `_cross/`, no anchor → `_orphans/`), `read_annotation()`, `write_annotation()` avec auto-frontmatter ISO-Z, `update_status()`
- Added `agents/cartographer/marker_injector.py` — `inject_single()` EOL idempotent, `inject_range()` avec préservation indentation, `remove_marker()` distingue own-line (drop) vs EOL (strip)
- Added `agents/cartographer/walker.py` — `walk_file()` universel via tree-sitter, retourne `FileSymbols` (loc + symbols + imports + exports), best-effort sur Kotlin/syntax errors
- Updated `requirements.txt` — ajout `PyYAML>=6.0` (frontmatter annotation)
- Tests : 110/110 GREEN (68 contrats existants + 21 nouveaux cartographer + 17 markers + 8 IO + 10 injector + 7 walker)
- Phase A.5 task #4 + #5 ✓ ; reste #6 render, #7 CLI, #8 subagent+skills, #9 hook, #10 changelog, #11 plan-keeper extension

### 2026-04-21 `d032741`
feat(data): Samsung Health CSV import pipeline + full DB schema (21 tables)
- Added `scripts/explore_samsung_export.py` — reads Samsung Health CSV export, outputs schema only (column names + type tokens), no personal values
- Added `scripts/import_samsung_csv.py` — idempotent bulk import (INSERT OR IGNORE / upsert) from CSV export into SQLite; handles Samsung's metadata-line-1 format, prefixed HC column names, Unix-ms timestamps, sleep stage code remapping (40001-40004 → awake/light/deep/rem)
- Extended `server/database.py` with 16 new tables: `steps_daily`, `stress`, `spo2`, `respiratory_rate`, `hrv`, `skin_temperature`, `weight`, `height`, `blood_pressure`, `mood`, `water_intake`, `activity_daily`, `vitality_score`, `floors_daily`, `activity_level`, `ecg`
- Added `_add_col()` migration helper for ALTER TABLE ADD COLUMN IF NOT EXISTS
- Extended `sleep_sessions` with 7 optional columns (sleep_score, efficiency, sleep_duration_min, sleep_cycle, mental_recovery, physical_recovery, sleep_type) via migration
- Extended `exercise_sessions` with 6 optional columns (calorie_kcal, distance_m, mean/max/min heart_rate, mean_speed_ms)
- Added `CREATE UNIQUE INDEX IF NOT EXISTS` on sleep_stages(stage_start, stage_end) for idempotent re-import

### 2026-04-21 `646aeaa`
chore(dev): dev-mobile script + fix Android cleartext HTTP
- Ajout `scripts/dev-mobile.ps1` — détecte auto les IPs WSL2/Windows, configure port forwarding 8001 et règle firewall
- Ajout `make dev-mobile` — affiche les instructions pour tester depuis le téléphone
- Ajout `android-app/res/xml/network_security_config.xml` — autorise HTTP cleartext (app locale uniquement, pas de données sensibles en transit)
- Référencé dans `AndroidManifest.xml` via `android:networkSecurityConfig`
- Fix `PreferencesManager.kt` — `DEFAULT_URL` sur port 8001

### 2026-04-21 `b5cacc7`
feat(frontend): Nightfall sleep dashboard branché sur l'API réelle
- Remplacé `static/index.html` par la structure Nightfall (fonts Instrument Serif + Geist, `#app`, `#cursor-glow`)
- Copié `dashboard.css` et `dashboard.js` du handoff Claude Design (inchangés)
- Exposé `window.render` dans `dashboard.js` pour découpler chargement des données et rendu
- Créé `static/api.js` — fetch `GET /api/sleep?include_stages=true`, agrège steps + HR, calcule `totals`/`efficiency`/`score`/`summary`, expose `window.SleepData` puis appelle `render()`
- Stratégie "30 dernières sessions disponibles" au lieu d'une fenêtre calendaire fixe
- État vide géré si DB sans données (message sync Android)
- Ajouté `tests/test_sleep_api_shape.py` — 7 tests de contrat sur le shape de l'API (ISO strings, ordre, filtre, stages)

### 2026-04-21 `939f5ef`
chore(workflow): bootstrap project template — CI, labels, hooks, tests
- Added `.github/workflows/ci.yml` — CI Python 3.12, runs `make ci-test` + `make ci-lint` on PR to main/dev
- Added `.github/workflows/review-complete.yml` — bloque les PR sans label `tested` (skip release/hotfix/docs-only)
- Added `.github/ISSUE_TEMPLATE/bug.yml` + `feat.yml` — templates avec labels auto (priorité + taille)
- Added `.github/pull_request_template.md` — format standard résumé / changements / test plan
- Added `.github/labels.json` — 28 labels (P0-P3, size/XS-XXL, types, statuts)
- Added `.githooks/pre-push` — enforce naming branches (feat/|fix/|chore/|hotfix/|release/)
- Added `.claude/settings.json` — permissions de base pour le projet
- Added `Makefile` — targets `dev`, `test`, `lint`, `ci-test`, `ci-lint`
- Added `tests/conftest.py` — TestClient FastAPI + DB de test isolée + fixture clean entre tests
- Added `tests/test_sleep.py` — 5 tests sleep (POST, dedup, GET, stages, not found) — tous verts

### 2026-02-16 `242040a`
Add Phase 3: steps, heart rate, exercise data types + tabbed dashboard
- Renamed DB from `sleep.db` to `health.db` in `server/database.py`
- Added `steps_hourly`, `heart_rate_hourly`, `exercise_sessions` tables with UNIQUE constraints
- Added 6 new Pydantic models (In/Out/Bulk) for steps, heart rate, exercise in `server/models.py`
- Created `server/routers/steps.py` — `POST /api/steps` (bulk INSERT OR IGNORE), `GET /api/steps?from=&to=`
- Created `server/routers/heartrate.py` — `POST /api/heartrate`, `GET /api/heartrate?from=&to=`
- Created `server/routers/exercise.py` — `POST /api/exercise`, `GET /api/exercise?from=&to=`
- Included all 3 new routers in `server/main.py`
- Updated `scripts/generate_sample.py` to generate 30 days of steps (hourly, realistic day/night patterns), heart rate (hourly with night/day variance), and 10-15 exercise sessions
- Added `READ_STEPS`, `READ_HEART_RATE`, `READ_EXERCISE` permissions to `AndroidManifest.xml`
- Extended `HealthConnectManager.kt` with `readSteps()`, `readHeartRate()`, `readExerciseSessions()` methods with hourly aggregation
- Renamed `SleepApi` to `HealthApi` in `ApiClient.kt`, added `postSteps()`, `postHeartRate()`, `postExercise()` endpoints
- Updated `SyncViewModel.kt` to sync all 4 data types sequentially with combined status messages
- Added 5-tab navigation (Sleep, Steps, Heart Rate, Exercise, Trends) in `static/index.html`
- Refactored `static/app.js` with shared month navigation, tab switching, and 5 render functions
- Steps tab: green horizontal bar chart per day showing daily totals
- Heart Rate tab: min-max range bars with avg marker per day
- Exercise tab: card list grouped by date with type, time, duration
- Trends tab: stat cards grid (avg sleep, daily step avg, resting HR, exercise count)
- Added tab styles, step bar, HR range bar, exercise card, and stat card styles in `static/style.css`

### 2026-02-16 `92eae41`
Fix Gradle sync and fetch all available sleep data on initial sync
- Fixed `settings.gradle.kts`: renamed `dependencyResolution` to `dependencyResolutionManagement` with `FAIL_ON_PROJECT_REPOS`
- Added `gradle-wrapper.properties` with Gradle 8.11.1
- Changed initial sync to fetch from epoch 0 (all available data) instead of last 30 days

### 2026-02-16 `8d5cfb0`
Add Phase 2: sleep stages, color-coded calendar, and Android Health Connect app
- Added `sleep_stages` table in `server/database.py` with FK to `sleep_sessions`, cascade delete, session index
- Added `UNIQUE(sleep_start, sleep_end)` constraint on `sleep_sessions` for duplicate handling
- Added `SleepStageIn`, `SleepStageOut` Pydantic models; added optional `stages` field to session models
- Updated `POST /api/sleep` to insert stages via `cursor.lastrowid`, uses `INSERT OR IGNORE` for dedup, returns `{inserted, skipped}`
- Updated `GET /api/sleep` with `?include_stages=true` query param to join stages per session
- Added CSS stage classes: `.stage-light` (blue), `.stage-deep` (dark blue), `.stage-rem` (purple), `.stage-awake` (orange)
- Updated `app.js` to fetch with `include_stages=true`, compute dominant stage per hour-cell by overlap duration, show stage breakdown in tooltips
- Updated `scripts/generate_sample.py` to generate realistic ~90min sleep cycles (light → deep → light → REM, occasional awake)
- Created `android-app/` Gradle project with Jetpack Compose, Health Connect client, Retrofit + kotlinx-serialization
- `HealthConnectManager.kt` reads `SleepSessionRecord` with stages, maps HC stage constants to string types
- `ApiClient.kt` provides configurable Retrofit client (default `http://10.0.2.2:8000` for emulator)
- `PreferencesManager.kt` persists backend URL and last sync timestamp via DataStore
- `SyncViewModel.kt` orchestrates read-from-HC → POST-to-backend flow (last 30 days or since last sync)
- `SyncScreen.kt` and `SettingsScreen.kt` Compose UI with sync button, progress, status, and backend URL config
- Updated `ROADMAP.md` to mark Phase 1 as done

### 2026-02-16 `6200a93`
Add Phase 1: FastAPI backend, SQLite DB, sleep calendar UI, and import scripts
- Added `server/database.py` with SQLite connection (WAL mode), `sleep_sessions` table and index
- Added `server/models.py` with Pydantic models for sleep session validation
- Added `server/routers/sleep.py` with POST/GET `/api/sleep` endpoints (bulk insert, date range query)
- Added `server/main.py` wiring FastAPI app, static file serving, DB init on startup
- Added `static/index.html`, `style.css`, `app.js` — sleep calendar grid (24h columns, per-day rows, month nav, hover tooltips)
- Added `scripts/generate_sample.py` — generates 30 days of realistic sleep data
- Added `scripts/import_csv.py` — parses Samsung Health CSV exports and POSTs to API
- Added `requirements.txt` (fastapi, uvicorn, pydantic)
- Updated `.gitignore` to include `.venv/`
- Updated `README.md` with features table, setup, and usage instructions

### 2026-02-16 `6cc83dc`
Initial project scaffolding with documentation
- Created GitHub repo (LyonMoncef/SamsungHealth)
- Added README with architecture overview (Android SDK → FastAPI → SQLite → Web UI)
- Added NOTES.md with backlog items
- Added ROADMAP.md with 3-phase plan (backend+viz → Android app → expansion)
- Added .gitignore for Python, Android, IDE, and DB files
