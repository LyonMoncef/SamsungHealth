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

| Drift clock — animated playback | `static/dashboard.js`, `static/dashboard.css` | [`3cdb138`](#2026-04-22-3cdb138) |
| Drift clock — 3 radial views + demo toggle | `static/dashboard.js`, `static/dashboard.css` | [`1dd765e`](#2026-04-22-1dd765e) |
| Cards full ranking view + bedtime scatter 24h | `static/dashboard.js`, `static/dashboard.css` | [`4882564`](#2026-04-22-4882564) |
| Metrics month selector + 4 full-history views | `static/dashboard.js`, `static/dashboard.css` | [`7367906`](#2026-04-22-7367906) |
| Sleep debt — gap-based episode grouping + median target | `static/dashboard.js` | [`89ab953`](#2026-04-22-89ab953) |
| Chapter 10 — duration regularity (elasticity) | `static/dashboard.js`, `static/dashboard.css` | [`f99283b`](#2026-04-22-f99283b) |

---

## Checkpoint

### `nightfall-dataviz-v1` — 2026-04-22
[CHECKPOINT] Reference tag for all Nightfall dataviz chapters before refactor.
- Scope: `static/dashboard.js`, `static/dashboard.css`
- Chapters included: heatmap (01), stacked timeline (02), hypnogram (03), radial (04), small multiples (05), ridgeline (06), top nights cards (07), agenda (08), metrics + month selector + 4 full-history views (09), elasticity / duration regularity (10), drift clock with animated playback (11)
- Key features: gap-based episode grouping, personal median sleep target, lag-1 autocorrelation, cycle count, bedtime scatter 24h, drift clock demo mode + smooth interpolation

---

## Changelog

### 2026-04-22 `7367906`
feat(frontend): metrics month selector + 4 full-history views (scatter/debt/stages/hr)
- Chapter 07 "Top nights": removed erroneous "Full ranking →" button and ranking table code
- Added `metricsMonth` state + `metricsAvailableMonths()`, `sessionsForMetricsMonth()`, `computeMetricsSummary()` helpers
- Added `metricsMonthSelector()` — prev/next nav above the 4 metric cards, updates all 4 simultaneously
- `bedtimeScatterSVG`, `hrSparkSVG`, `debtBars`, `stageGauge` parameterized to accept filtered session arrays
- `chapterMetrics()` rewritten: triggers lazy full-session load, uses month-filtered data, each card clickable
- Sleep debt card: now shows last 30 nights (was 14)
- Added `renderHistoryScatter()` — X=date/time, Y=24h bedtime, all 1200 nights as dots with month ticks
- Added `renderHistoryDebt()` — all nights debt bars (scrollable) + cumulative debt summary
- Added `renderHistoryStages()` — 28-night rolling average of 4 stage % as line chart over full history
- Added `renderHistoryHR()` — HR sparkline for all available nights (area + line)
- Added `loadFull()` + `historyViewShell()` shared helpers for full-history views
- `renderApp()` routes `history/scatter|debt|stages|hr` to async render functions
- `bindEvents()` handles month prev/next + clickable card navigation
- CSS: `.metrics-month-nav`, clickable card `::after` arrow indicator, full-history view styles

### 2026-04-22 `4882564`
feat(frontend): cards full ranking view + bedtime scatter 24h axis
- `bedtimeScatterSVG()` rewritten: Y axis covers full 24h (18h–18h), `h=200`, ticks every 3h with grid lines and `dominant-baseline="middle"` for centering
- `nightCardHTML(s, rank)` extracted as standalone helper for reuse
- `chapterCards()` now uses `D.sessionsFull || D.sessions` for full history + added "Full ranking (N) →" button (`id="cards-to-history"`)
- `renderHistoryCards()` added: compact ranking table sorted by score, grid layout with 6 columns (rank / date / score / duration / bed→wake / stage bar)
- `renderHistoryCardsAsync()` added: loading state + lazy full-session load via `window.loadFullSessions()`
- `renderApp()` updated to dispatch `history/cards` → `renderHistoryCardsAsync()`
- `bindEvents()` updated: `#cards-to-history` click → `navigateTo("history/cards")`
- CSS: `.ranking-head`, `.ranking-table`, `.ranking-row`, `.ranking-rank`, `.ranking-date`, `.ranking-score`, `.ranking-dur`, `.ranking-times`, `.ranking-stages`

### 2026-04-22 `3cdb138`
feat(frontend): drift clock animated playback with scrubber and window size selector
- Replaced static rolling-avg view C with animated playback: ▶/⏸ button steps through all N-night rolling windows at ~150ms/frame
- Ghost trails (downsampled to ~60 arcs, 9% opacity) stay visible in background for context
- Active arc has glow effect (wide transparent stroke behind main stroke) and color-shifts violet→amber as time advances
- Bed/wake dot markers on arc endpoints update each frame
- Center SVG displays live BED → WAKE times (Geist Mono, updated via direct DOM)
- Scrubber (range input) lets user seek manually; dragging pauses playback
- Window size selector: 7n / 14n / 30n pills — changing window resets and re-renders
- Playback state (`driftPlayback`) survives demo-mode toggle but resets on source/window change
- RAF loop self-terminates when arc element disappears (page navigation) or last frame is reached

### 2026-04-22 `1dd765e`
feat(frontend): drift clock demo toggle with synthetic regular sleep data
- Added `generateDemoSessions(n)` — synthetic 365-night dataset with seasonal bedtime variation (±42min) and small jitter, for demo purposes
- Added `driftDemoMode` state variable and binding in `bindEvents()` for `#drift-demo-toggle`
- Toggle button switches between user's data and demo data in all 3 drift clock views (density, spaghetti, rolling avg)
- Added `.drift-demo-badge` CSS — small violet pill label shown when demo mode is active

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
