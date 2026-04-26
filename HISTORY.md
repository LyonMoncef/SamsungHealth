# History

## Features

| Feature | Files | Commit |
|---------|-------|--------|
| V2.3.0.1 — `user_id NOT NULL` cleanup + scripts CSV multi-user | `alembic/versions/0005_user_id_not_null.py`, `server/db/models.py`, `scripts/import_samsung_csv.py`, `scripts/generate_sample.py` | [`PENDING`](#2026-04-26-PENDING) |
| V2.3 — Auth foundation atomique (users + JWT access+refresh + multi-user FK + redaction + audit) | `server/security/auth.py`, `server/security/redaction.py`, `server/routers/auth.py`, `server/db/models.py`, `alembic/versions/0004_auth_foundation.py` | [`e32801a`](#2026-04-26-e32801a) |
| V2.0.5 — structlog observability foundation (JSONL + request_id middleware) | `server/logging_config.py`, `server/middleware/request_context.py`, `server/main.py`, `requirements.txt` | [`f2c8cb2`](#2026-04-26-f2c8cb2) |
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

### 2026-04-26 `PENDING`
chore(V2.3.0.1): user_id NOT NULL sur 21 tables santé + scripts CSV multi-user
- `alembic/versions/0005_user_id_not_null.py` créé (revision `8c1d2e4f5a90`, parent `7a3b9c0e1d24`) — safety check raise si rows orphelines, ALTER COLUMN SET NOT NULL sur 21 tables, drop des index partiels `WHERE user_id IS NULL`. Downgrade restaure l'état nullable + index partiel.
- `server/db/models.py` — 21 occurrences de `user_id: Mapped[UUID | None]` + `nullable=True` → `Mapped[UUID]` + `nullable=False`. RefreshToken déjà NOT NULL, AuthEvent reste nullable (login_failure sur email inexistant).
- `scripts/import_samsung_csv.py` — argparse `--user-email` (default `legacy@samsunghealth.local`), helper `_resolve_target_user_id`, `_upsert` injecte `user_id=TARGET_USER_ID` côté serveur, drop `index_where=text("user_id IS NULL")`, l'unique constraint matchée est désormais `(user_id, ...cols)`.
- `scripts/generate_sample.py` — argparse `--user-email`, propagation `user_id` aux 5 call sites (sleep + stages + steps + heart_rate + exercise), drop des `index_where`.
- `alembic/versions/0004_auth_foundation.py` — fix bug downgrade : remplace `op.drop_constraint(uq_name, ...)` par `op.execute("DROP INDEX IF EXISTS ...")` (l'upgrade créait un INDEX partiel, pas un constraint, donc drop_constraint échouait). Drop aussi `uq_<table>_user_window` créé par l'upgrade.
- `tests/server/conftest.py` — fixture `default_user_db` + helper `_ensure_orm_default_user` (insertion idempotente raw SQL, évite argon2 200ms par test) + autouse hook `_auto_inject_user_id_for_health_inserts` (event `before_insert` SQLAlchemy qui injecte un user_id par défaut si absent — évite de patcher 30+ inserts ORM dans les tests existants).
- `tests/server/test_crypto_foundation.py` + `tests/server/test_scripts_csv_import.py` — adaptation aux nouvelles signatures CLI argparse + injection `TARGET_USER_ID` via le fixture `csv_export_dir`.
- 123 tests verts (`pytest tests/server/`).

### 2026-04-26 `2beb3c7`
chore(design): référence rebrand Data Saillance (palette + typo + logos, dark + light) pour le produit Nightfall
- `docs/vault/specs/2026-04-26-nightfall-rebrand-data-saillance.md` créé (type=reference) — palette canonique HEX, type scale Playfair Display + Inter, 5 variantes logo, double-stroke "halo plat" dark, tokens CSS vars `--ds-*`, convention `MaterialTheme` Compose, règles d'usage. Source of truth externe : `~/MyPersonalProjects/Vectorizer/`. Light mode obligatoire (Nightfall v1 était dark-only)
- `docs/vault/specs/nightfall-fullspectrum-design-brief.md` : `status: ready → superseded`, `superseded_by: 2026-04-26-nightfall-rebrand-data-saillance`, `superseded_on: 2026-04-26`
- `docs/vault/specs/2026-04-23-plan-v2-refactor-master.md` : Phase 4 + Phase 5 patchées avec note "voir spec rebrand Data Saillance, référencement obligatoire dans frontmatter `related_specs:` des futures specs frontend"
- Convention : `code-cartographer` rendra le link automatique ; `plan-keeper` flaggera comme déviation toute spec frontend qui ne référence pas cette ref
- Aucun changement code : pure documentation/référence design

### 2026-04-26 `e32801a`
feat(V2.3): auth foundation atomique — users + JWT access+refresh + multi-user FK + redaction + audit
- **Atomique non-splittable** (imposé audit pentester) : 50 tests RED → GREEN dans une seule PR pour éviter brèche Art.9 RGPD entre split partiel
- `server/security/auth.py` : argon2id RFC 9106 profile #2 (m=46MB, t=2, p=1), PyJWT HS256 (`kid: "v1"`, `algorithms=["HS256"]` strict, `require=[exp,iat,sub,iss,aud,typ]`, validation iss/aud, `_DUMMY_HASH` pour timing equalization, `SAMSUNGHEALTH_JWT_SECRET_PREVIOUS` decode-only pour rotation), `get_current_user` dependency
- `server/security/redaction.py` : structlog processor `redact_sensitive_keys` (password, token, authorization, jwt, secret, cookie + nested + case-insensitive)
- `server/routers/auth.py` : `POST /auth/{register,login,refresh,logout}` — register admin-gated via `X-Registration-Token` (constant-time compare), refresh rotation avec détection replay (log `auth.refresh.replay_attempt` ERROR), logout idempotent, audit table `auth_events` (sha256 email_hash, jamais email plain)
- `server/db/models.py` : nouveaux models `User` (CITEXT email, failed_login_count, locked_until, last_login_at/ip, password_changed_at, is_active, email_verified_at TIMESTAMPTZ), `RefreshToken` (jti UNIQUE, replaced_by FK, revoked_at), `AuthEvent` ; `user_id UUID NULL FK` ajouté sur 21 tables santé
- `alembic/versions/0004_auth_foundation.py` : `CREATE EXTENSION citext`, schémas auth, partial unique index `WHERE user_id IS NULL` pour back-compat scripts CSV legacy + nouveau unique sur `(user_id, ...)` pour multi-user, backfill conditionnel d'un user `legacy@samsunghealth.local` (is_active=false) si données legacy détectées
- `server/routers/{sleep,heartrate,steps,exercise,mood}.py` : `Depends(get_current_user)` partout, filtering `where(<Model>.user_id == current_user.id)` sur SELECT, injection serveur `user_id=current_user.id` sur INSERT (jamais accepté du body)
- `server/main.py` : validation JWT secret au boot (présence + 256 bits + reject "changeme/secret/test/password/default" + Shannon entropy ≥ 4.0), bench argon2 au boot (`auth.argon2.bench` info)
- `server/logging_config.py` : plug `redact_sensitive_keys` après `merge_contextvars`
- `tests/server/conftest.py` : `_set_auth_env_defaults` autouse pour env vars JWT/registration ; `client_pg_ready` enrichi pour auto-auth des tests existants (avec opt-out par fichier pour les tests V2.3 auth nus)
- `scripts/{import_samsung_csv,generate_sample}.py` : `index_where=text("user_id IS NULL")` ajouté aux ON CONFLICT pour préserver idempotence avec partial index
- 50 tests RED → GREEN dans 6 fichiers (`test_password_hashing`, `test_jwt`, `test_auth_routes`, `test_health_routes_auth`, `test_redaction`, `test_auth_events`) — total **298/298 GREEN**, 0 régression
- Dépendances : `argon2-cffi>=23.1`, `PyJWT>=2.8.0`
- Spec : `docs/vault/specs/2026-04-26-v2-auth-foundation.md` (status delivered)
- Hors scope V2.3 (différés) : V2.3.0.1 `user_id NOT NULL`, V2.3.1 reset password + email verification, V2.3.2 Google OAuth, V2.3.3 rate limiting + lockout enforcement + frontend Nightfall login

### 2026-04-26 `f2c8cb2`
feat(V2.0.5): structlog observability foundation — JSONL logs + request_id middleware
- `server/logging_config.py` créé : `configure_logging()` + `get_logger()` + chaîne de processors structlog (timestamp ISO8601 UTC, level, logger scope, contextvars merger), JSONRenderer en prod / ConsoleRenderer(colors) en dev
- `server/middleware/request_context.py` créé : ASGI pure middleware, `request_id_var` + `user_id_var` ContextVars, sanitisation X-Request-ID (alnum+tirets, max 64), header in/out, log `request.complete` avec `latency_ms` (perf_counter) + `route` template + niveau INFO/WARNING/ERROR selon status
- `server/main.py` : `configure_logging()` dans lifespan, `RequestContextMiddleware` mounté avant routers
- 7 fichiers `server/{database,security/crypto,routers/*}.py` : `_log = get_logger(__name__)` ajouté pour V2.3+ auth events
- Env vars : `APP_ENV` (prod|dev) + `LOG_LEVEL` (DEBUG/INFO/WARNING/ERROR/CRITICAL avec fallback INFO si invalide)
- 13 tests RED → GREEN (`tests/server/test_logging_config.py` 6 tests, `tests/server/test_request_context_middleware.py` 7 tests) — total 248/248 GREEN, 0 régression
- Dépendance ajoutée : `structlog>=24.1`
- README section "Logs" + NOTES.md tech debt "PII scrubber automatique (V2.3+)"
- Spec : `docs/vault/specs/2026-04-26-v2-structlog-observability.md` (status delivered)
- Hors scope V2.0.5 : Caddy reverse proxy, CLI logq, OpenTelemetry, PII scrubber auto, migration scripts/* (différés V2.0.6+)

### 2026-04-26 `7135a5f`
chore(audit): snapshot V2 progress 2026-04-26 + fix V2.1.1 status delivered
- `docs/vault/audits/2026-04-26-v2-progress-audit.md` créé (nouveau dossier `audits/` pour les snapshots roadmap, distincts des `specs/`)
- Bilan : 11 PRs mergées, 235/235 tests GREEN, **3 phases V2 entamées** (P0 ~70%, P2 ~50%, Phase A 100%), 4 phases à 0% (P1 auth, P3 RGPD, P4 Android, P5 Compose Canvas)
- Anomalie corrigée : spec `2026-04-24-v2-postgres-routers-cutover.md` `status: ready → delivered`, `delivered: null → 2026-04-24` (PR #8 mergée le 2026-04-24, statut frontmatter pas mis à jour à l'époque)

### 2026-04-24 V2.2.1 — étendre chiffrement aux 9 tables Art.9 restantes

`729c3b1` spec V2.2.1 (12 tests d'acceptation, 9 sentinelles BYTEA + 2 round-trip critiques + 1 sentinelle _crypto_v default).

`28e2c32` test(v2.2.1): 12 tests RED — `TestSentinelleBytea` (1 par table : sleep_sessions, weight, blood_pressure, stress, spo2, heart_rate_hourly, respiratory_rate, skin_temperature, ecg) + `TestRoundTripCritique` (sleep_score Int + weight_kg Float + crypto_v default).

`d7e47d8` feat(v2.2.1): chiffrement étendu → 12/12 V2.2.1 GREEN, 235 suite globale.
- `server/db/encrypted.py` : ajout `EncryptedFloat` `TypeDecorator` (sérialisation `repr(float).encode('ascii')` pour préserver IEEE 754, `float(decoded)` au déchiffrement, accepte int en input)
- `server/db/models.py` : 9 models patchés en série mécanique — pour chaque colonne Art.9 : `Mapped[type | None] = mapped_column(EncryptedInt|EncryptedFloat)` + colonne `<col>_crypto_v: Mapped[int] = mapped_column(Integer, NOT NULL, server_default="1")`. Total : 33 colonnes chiffrées + 33 `_crypto_v`
  - `sleep_sessions` 7 cols : sleep_score/efficiency/sleep_duration_min/sleep_cycle/mental_recovery/physical_recovery/sleep_type
  - `weight` 7 cols : weight_kg/body_fat_pct/skeletal_muscle_pct/skeletal_muscle_mass_kg/fat_free_mass_kg/basal_metabolic_rate/total_body_water_kg
  - `blood_pressure` 4 cols : systolic/diastolic/pulse/mean_bp
  - `stress` 1 col : score
  - `spo2` 4 cols : spo2/min_spo2/max_spo2/low_duration_s
  - `heart_rate_hourly` 3 cols : min_bpm/max_bpm/avg_bpm (NOT NULL conservé)
  - `respiratory_rate` 3 cols : average/lower_limit/upper_limit
  - `skin_temperature` 3 cols : temperature/min_temp/max_temp
  - `ecg` 2 cols : mean_heart_rate/classification (sample_frequency/sample_count restent en clair — métadonnées non Art.9)
- `alembic/versions/0003_encrypt_remaining_art9.py` : migration unique pour les 9 tables (~370 lignes, autogenerated puis sed `postgresql_using='NULL'` sur tous les `alter_column`). Fresh DB requise (heart_rate_hourly.min_bpm NOT NULL → USING NULL fail si rows existantes)
- Pattern transparent confirmé : aucun changement aux routers, aux scripts d'import, aux tests V2.1.x — le chiffrement est invisible côté ORM (insert/read transparent) et côté API (JSON shape inchangé)
- Spec V2.2.1 → `delivered: 2026-04-24`. Spec parente V2.2 reste `delivered`, V2.2.1 = extension
- **Suite globale : 235 tests GREEN** (223 + 12 V2.2.1)
- Tous les champs Art.9 du schéma SamsungHealth sont maintenant chiffrés AES-256-GCM at-rest (10 tables, 37 colonnes total : 4 mood + 33 V2.2.1)

### 2026-04-24 V2.2 — fondation chiffrement AES-256-GCM at-rest + table pilote mood

`085dbcd` spec V2.2 (16 tests d'acceptation, fondation crypto + table pilote mood, scope strict Art.9 RGPD).

`1b9c5c9` test(v2.2): 16 tests RED — `TestLoadEncryptionKey` ×5 + `TestEncryptDecryptField` ×3 + `TestEncryptedTypeDecorator` ×1 + `TestBootValidation` ×1 + `TestMoodPersistenceEncrypted` ×3 + `TestMoodApiBackCompat` ×2 + `TestMoodErrorSanitization` ×1.

`66de248` feat(v2.2): fondation crypto AES-256-GCM → 9 tests fondation GREEN.
- `server/security/crypto.py` : `load_encryption_key()` validation stricte (présence + base64 + 32 bytes + non-zero), `encrypt_field`/`decrypt_field` (nonce 12 + ciphertext+tag concaténés), exceptions `EncryptionConfigError` et `DecryptionError` (wrap `cryptography.exceptions.InvalidTag`)
- `server/db/encrypted.py` : `EncryptedBytes`/`EncryptedString`/`EncryptedInt` SQLAlchemy `TypeDecorator` (transparent à l'ORM, sérialisation UTF-8 / ASCII int, BYTEA storage)
- `tests/server/conftest.py` : fixture autouse `_set_test_encryption_key` (clé test stable, reset cache lru entre tests)
- `.env.example` : template clé maître + commande de génération + warning sauvegarde
- `requirements.txt` : `cryptography>=42.0` (déjà 46.0.7 installé localement)

`5df4b2c` feat(v2.2): mood router + model chiffré → 16/16 V2.2 GREEN, 223 suite globale.
- `server/db/models.py::Mood` : `notes`/`emotions`/`factors` en `EncryptedString`, `mood_type` en `EncryptedInt`. Ajout `notes_crypto_v`/`emotions_crypto_v`/`factors_crypto_v`/`mood_type_crypto_v` (smallint NOT NULL default=1) — versionning chiffrement pour rotation future sans perte
- `alembic/versions/0002_encrypt_mood.py` : `op.alter_column` TEXT/INT → BYTEA via `postgresql_using='NULL'` (fresh DB assumée, downgrade non-data-preserving documenté)
- `server/main.py` : `lifespan` async context manager appelle `_validate_encryption_at_boot()` au démarrage uvicorn (pas à l'import — pour ne pas casser pytest collect)
- `server/routers/mood.py` : POST/GET `/api/mood`, `Depends(get_session)`, `pg_insert.on_conflict_do_nothing(start_time)`, wrap `DecryptionError` → `HTTPException(500, "internal_decryption_error")` opaque (sanitization spec §16)
- `server/models.py` : `MoodIn`/`MoodOut`/`MoodBulkIn` Pydantic (types python natifs, pas de bytes leak côté API)
- **Pattern transparent à valider en V2.2.1** : changer `Mapped[str]` → `mapped_column(EncryptedString)` + ajouter colonne `_crypto_v` + migration Alembic = ~2 min par champ. Reproductible mécaniquement sur les 9 tables Art.9 restantes
- **Suite globale : 223 tests GREEN** (207 + 16 V2.2)

### 2026-04-24 V2.1.2 — refonte scripts CSV import + sample generator vers SQLAlchemy/PG

`cf78667` spec V2.1.2 (4 tests d'acceptation, 21 importers à migrer).

`033515c` test(v2.1.2): 4 tests RED — `TestImportSamsungCsv` (round-trip + idempotent) + `TestGenerateSample` (30d + idempotent).

`898eab7` feat(v2.1.2): refactor `scripts/generate_sample.py` vers SQLAlchemy/PG → 2/4 GREEN.
- Pattern décisif pour l'idempotence : `random.seed(42)` au top de `main()` + **séparation génération vs insertion** (Phase 1 = sleep_plan en mémoire consomme tout le random linéairement, Phase 2 = pg_insert ON CONFLICT DO NOTHING). Sans ça, le `continue` après ON CONFLICT skippe `generate_stages` au 2e run → random consommé différemment → jours suivants divergent → 30 → 59 sessions
- Use `pg_insert(...).on_conflict_do_nothing(index_elements=...).returning(<PK>)` partout (steps_hourly, heart_rate_hourly, exercise_sessions, sleep_sessions, sleep_stages)
- `base_date` passé en `datetime(..., tzinfo=timezone.utc)` pour matcher PG `DateTime(timezone=True)` exactement entre runs
- Suppression complète SQLite (`sqlite3`, `DB_PATH`, `init_db()`, `get_connection()` inlinés)

`cc468d1` feat(v2.1.2): refactor `scripts/import_samsung_csv.py` vers SQLAlchemy/PG → 4/4 GREEN.
- **21 importers** migrés de `INSERT OR IGNORE` SQLite vers `pg_insert(Model).on_conflict_do_nothing(index_elements=...).returning(Model.id)`
- Helper unique `_upsert(db, model, values, conflict_cols) -> bool` au top du module — élimine la duplication 21×
- `parse_dt()` retourne maintenant `datetime` aware UTC (au lieu de string ISO) — préserve les égalités ON CONFLICT pour les colonnes `DateTime(timezone=True)`
- Helper `parse_day()` factorise les conversions `day_time` → `YYYY-MM-DD` string (cas `parse_dt` + `ms_to_date_str` selon source CSV)
- `import_sleep_stages` utilise `select(SleepSession.id, SleepSession.sleep_start, SleepSession.sleep_end)` au lieu d'un `SELECT *` (~10× plus rapide sur 30k+ sleep sessions)
- `main()` utilise `get_session()`, suppression de `init_db()`/`get_connection()` (Alembic gère le schema)
- README mis à jour : Components table → "Done (SQLAlchemy depuis V2.1.2)" pour `generate_sample` et `import_samsung_csv`
- Spec V2.1.2 → `delivered: 2026-04-24`
- **Suite globale : 207/207 tests GREEN** (203 + 4 V2.1.2)

### 2026-04-24 `1483873` (V2.1.1 cleanup final)
feat(v2.1.1): cleanup SQLite — purge legacy, scripts isolés, README PG, V2.1 delivered
- `server/database.py` : purge complète (`sqlite3`, `DB_PATH`, `_add_col`, `init_db`, `get_connection` supprimés). Garde `get_engine`/`get_session`/`SessionLocal`/`_DEFAULT_PG_URL`
- `server/main.py` : suppression hook `@app.on_event("startup") def startup(): init_db()` — schema géré par Alembic uniquement
- `scripts/import_samsung_csv.py` + `scripts/generate_sample.py` : helpers SQLite inlinés (autonomes, plus d'import depuis `server.database`). `init_db()` raise `SystemExit` avec message pédagogique pointant spec V2.1.2 (refonte SQLAlchemy à venir)
- `tests/conftest.py` : supprimé (le `client`/`clean_db` fixture SQLite n'a plus de consumer après migration tests sleep vers `tests/server/`)
- `README.md` : section Setup réécrite (Postgres + Docker prérequis, `make db-up && make db-migrate`), Components table mise à jour (SQLAlchemy 2.x, PG 16 Alembic UUID v7), warning migration depuis V1
- Spec `docs/vault/specs/2026-04-24-v2-postgres-migration.md` : `status: delivered`, `delivered: 2026-04-24`
- **Suite globale : 203 tests GREEN, 0 failed** (175 pré-existants + 12 sleep migrés vers PG + 6 round-trip cutover + 10 V2.1 fondation)
- 6 tests sentinelles cutover GREEN (`TestNoSqliteResidual` × 3 + 3 round-trip routers)

### 2026-04-24 `12140e8`
feat(v2.1.1): refactor heartrate + steps + exercise routers → SQLAlchemy + on_conflict_do_nothing returning id (3 round-trip GREEN)
- 3 routers convertis en SessionLocal + `pg_insert(...).on_conflict_do_nothing(index_elements=...).returning(<PK>)`
- Pattern `RETURNING id` requis car `result.rowcount` retourne -1 sur ON CONFLICT — la présence d'un row dans `RETURNING` indique l'insertion effective vs le skip
- Helper `_to_dt`/`_iso` dans exercise pour normaliser timezone UTC (les datetimes PG sont timezone-aware)
- Tests round-trip GREEN : POST 3 records → GET filter → assertions sur shape + count + valeurs croisées avec PG via `db_session`

### 2026-04-24 `b75482f`
feat(v2.1.1): refactor sleep router → SQLAlchemy + 12 tests legacy migrés vers tests/server (PG via testcontainers)
- `server/routers/sleep.py` : POST utilise `select` pour dedup (idempotent) puis `db.add(SleepSession)` + flush + add stages ; GET utilise `selectinload(SleepSession.stages)` quand `include_stages=true`
- `server/models.py` : `SleepSessionOut.id`/`SleepStageOut.id`/`session_id` passent `int → str` (UUID sérialisé). `created_at: str | None = None` (PG renvoie datetime, pas string)
- `tests/test_sleep.py` + `tests/test_sleep_api_shape.py` déplacés dans `tests/server/` (12 tests). Le `client` fixture remplacé par `client_pg_ready` (TestClient + `app.dependency_overrides[get_session]` vers le testcontainer)
- `tests/server/conftest.py` : remontée des fixtures `schema_ready` (alembic upgrade head sur testcontainer) + `client_pg_ready` (combine schema + client_pg). Fixture autouse `_pg_truncate_between_tests` (skip si test ne demande pas pg_container)
- 16 tests sleep-related GREEN (12 legacy + 3 persistence + 1 back-compat)

### 2026-04-24 `37d38fb`
test(v2.1.1): 7 tests RED — back-compat sleep adapté + 3 round-trip routers + 3 sentinelles SQLite cleanup
- `tests/server/test_routers_cutover.py` : `TestHeartRateRouter`/`TestStepsRouter`/`TestExerciseRouter` round-trip POST→GET avec assert via `db_session` PG (preuve que le router tape vraiment PG, pas SQLite résiduel)
- `TestNoSqliteResidual` × 3 : grep `^(import sqlite3|from sqlite3 )` dans `server/**/*.py`, présence `health.db`, `from server.database import get_connection` raise ImportError
- Test back-compat sleep adapté : params `from`/`to`/`include_stages` (réels frontend Nightfall) au lieu de `period=6m` (faux paramètre inventé)

### 2026-04-24 `b0edf6e`
spec(v2.1.1): cutover routers SQLite → SQLAlchemy + suppression code SQLite (spec fille de V2.1) + V2.1 in_progress
- Décision en cours de session : V2.1 splitée en 2 PR (option B). PR fondation = engine + models + alembic + docker + uuid7 (mergeable indépendamment). PR fille (V2.1.1) = refactor 4 routers + suppression complète SQLite + 12 tests legacy migrés
- `docs/vault/specs/2026-04-24-v2-postgres-routers-cutover.md` créée (status: ready, 8 livrables, 9 tests d'acceptation)
- Spec parente `2026-04-24-v2-postgres-migration.md` passée `status: in_progress` + section "Suite naturelle" mise à jour avec le découpage

### 2026-04-24 `9c4f87c`
fix(tests): truncate cascade entre tests pour isolation forte → 9/10 tests V2.1 GREEN
- Tests `TestSleepSessionPersistence` partageaient le testcontainer PG session-scoped → state résiduel entre tests (tests insert+read laissaient des rows, atomic test échouait sur "0 rows attendus")
- Fix : fixture `db_session` truncate cascade toutes les tables non-alembic en teardown
- Bilan tests V2.1 : **9 PASSED / 1 FAILED** (uuid7 ×3 + bootstrap ×3 + persistence ×3 GREEN ; back-compat HTTP `test_get_sleep_period_6m_response_shape_unchanged` reste RED car router refactor pas fait)

### 2026-04-24 `45cc18f`
feat(v2.1): server/database.py refactor (get_engine/get_session) + 21 SQLAlchemy models + alembic init + 0001_initial migration → 6/10 tests GREEN
- `server/database.py` : ajout `get_engine()` (lru_cache, lit `DATABASE_URL` ou défaut local), `get_session()`, `SessionLocal`. Legacy `get_connection()`/`DB_PATH`/`init_db()` conservés pour back-compat des 175+ tests SQLite existants (suppression dans impl 7/7)
- `server/db/models.py` : 21 tables SQLAlchemy 2.x avec `Mapped[]`, `Uuid7PkMixin` + `TimestampedMixin`, FK UUID + ondelete CASCADE, contraintes UNIQUE conservées (sleep_sessions, sleep_stages, steps_*, heart_rate_hourly, exercise_sessions, stress, spo2, respiratory_rate, hrv, skin_temperature, weight, height, blood_pressure, mood, water_intake, activity_daily, vitality_score, floors_daily, activity_level, ecg)
- `alembic/env.py` : lit `DATABASE_URL` env, target_metadata = `Base.metadata`
- `alembic/versions/0001_initial.py` : autogenerate complet + ajout `import server.db.uuid7` (pour résoudre le `server.db.uuid7.Uuid7()` référencé)
- `tests/server/conftest.py` : forcer driver psycopg 3 dans l'URL testcontainers (`postgresql://` → `postgresql+psycopg://`)
- 0 régression sur les 175 tests existants (legacy `get_connection`/`init_db` SQLite intact)

### 2026-04-24 `9491013`
feat(v2.1): docker-compose PG 16-alpine + Makefile db-up/db-down/db-migrate/db-reset
- `docker-compose.yml` : service `postgres:16-alpine` + volume nommé `pgdata` + healthcheck `pg_isready` + port 5432 exposé
- `Makefile` 4 targets : `db-up` (idempotent + wait-ready), `db-down` (volume préservé), `db-migrate` (DATABASE_URL local par défaut si absent), `db-reset` (DESTRUCTIVE — drop volume + recreate + migrate)

### 2026-04-24 `35c559e`
feat(v2.1): uuid7 helper + Uuid7 TypeDecorator + deps PG → 3 tests uuid7 GREEN
- `server/db/uuid7.py` : `uuid7()` (wrapper sur `uuid_utils.uuid7`, retourne `_uuid.UUID` standard) + `Uuid7` TypeDecorator (PG → `UUID(as_uuid=True)`, autres dialects → `CHAR(36)` string)
- `requirements.txt` : ajout `sqlalchemy>=2.0`, `psycopg[binary]>=3.1`, `alembic>=1.13`, `uuid_utils>=0.10`, `testcontainers[postgres]>=4.0`
- 3 tests `TestUuid7` (version 7, monotone par ms, timestamp extractable) GREEN immédiats — pas de DB requise

### 2026-04-24 `161aa86`
test(v2.1): 10 tests RED pour spec postgres-migration
- Première application réelle de la boucle TDD V2.1 — exécution `/tdd` inline (pas de subagent project-local exposé via Agent tool, comme noté wrap V2 foundation)
- `tests/server/conftest.py` — fixtures session : `pg_container` (testcontainers-postgres PG 16-alpine, skip si Docker indispo), `pg_url`, `engine`, `db_session` (SessionLocal SQLAlchemy)
- `tests/server/test_uuid7.py` — `TestUuid7` ×3 : `test_version_field_is_7`, `test_monotonic_within_ms` (100 UUID consécutifs en ordre strict), `test_timestamp_extractable` (extraction 48 bits hex, |Δ| < 5ms)
- `tests/server/test_postgres_bootstrap.py` — `TestBootstrap` ×3 : alembic upgrade head crée 22 tables avec PK UUID + created_at/updated_at obligatoires, idempotent (no "Running upgrade" en 2e run), downgrade base réversible (toutes tables sauf alembic_version disparaissent)
- `tests/server/test_models_postgres.py` — `TestSleepSessionPersistence` ×3 + `TestApiBackCompat` ×1 : insert assigne UUID v7 + created_at == updated_at, read by UUID, atomicité session+stages (FK invalide → rollback complet), back-compat shape JSON GET /api/sleep?period=6m (Nightfall ne bouge pas)
- Bilan RED : **10 tests, 0 GREEN, 4 FAILED + 6 ERROR setup**, tous attribuables à du code/infra manquant déclaré par la spec (`server.db.uuid7`, `server.db.models`, `alembic.ini`, `alembic/versions/0001_initial.py`)
- Stack runtime installé pour valider RED (sera réinstallé via `requirements.txt` à l'étape /impl) : `sqlalchemy>=2.0`, `psycopg[binary]>=3.1`, `alembic>=1.13`, `uuid_utils>=0.10`, `testcontainers[postgres]>=4.0`
- Découverte fix incidente : un `tests/server/__init__.py` créé par mégarde fait que pytest l'importe en tant que package `server` (collision avec `/server/`). Suppression → fix
- 4 notes vault auto-générées par pre-commit hook (cartographer)

### 2026-04-24 `e9c727e`
feat(skill): /tdd (test-writer) — bouclage du chaînon manquant /spec → /tdd → /impl
- Découvert pendant V2.1 kickoff : `/tdd` était référencé partout (`/spec` next_default, `/impl` prev) mais jamais créé
- `.claude/skills/tdd/SKILL.md` — délègue à `test-writer` subagent. Valide prérequis (spec status ≥ ready, frontmatter `tested_by:` non vide). Auto-détecte `target_test_dir` depuis `dirname(tested_by[0].file)`. Prépare `brief.json` (contrat `TestBrief`). Garde-fou : si `tests_green_count > 0` avant impl → faux test, signale au lieu de continuer
- Linked-list maintenant complète : `/spec` → `/tdd` → `/impl` → `/review` → `/align` → `/commit`

### 2026-04-24 `fa2906a`
feat(spec): v2-postgres-migration (status ready, 22 tables, 10 tests d'acceptation, UUID v7 + Alembic + testcontainers)
- Première vraie spec V2.1 produit, branche `feat/v2-postgres-migration` (depuis umbrella `refactor/phase-a-foundation-agents`)
- `docs/vault/specs/2026-04-24-v2-postgres-migration.md`
- Vision : sortir SQLite (single-file, schéma fragile via `_add_col` runtime) au profit de PG 16 + Alembic + UUID v7. Pas de feature produit livrée — fondation pour V2.2+ (chiffrement AES-GCM, JWT, structlog)
- Décisions : PG 16 + tout côté Python (UUID v7 via `uuid_utils`, AES-GCM via `cryptography` futur), SQLAlchemy 2.x ORM + `psycopg[binary]` synchrone, Alembic autogenerate baseline + revue manuelle, `testcontainers-postgres` pour intégration, API publique inchangée (Nightfall ne bouge pas), pas de port de données SQLite
- Livrables : `docker-compose.yml` PG 16-alpine, `server/db/{uuid7.py,models.py}`, `server/database.py` refactor (`get_engine`/`get_session`), `alembic/{env.py,versions/0001_initial.py}`, refactor 4 routers (`sleep`/`heart_rate`/`steps`/`activity`), suppression code SQLite, Makefile targets `db-up`/`db-down`/`db-migrate`/`db-reset`
- 10 tests d'acceptation given/when/then mappés sur 3 fichiers de test : `tests/server/{test_postgres_bootstrap,test_uuid7,test_models_postgres}.py` (bootstrap idempotent, downgrade réversible, UUID v7 monotone/version/timestamp, insert sleep session avec UUID + timestamps auto, read by UUID, atomicité session+stages, back-compat API frontend)
- Out-of-scope verrouillé : AES-GCM (V2.2), JWT (V2.3), structlog (V2.4), async SQLAlchemy, port données

### 2026-04-24 `086c597`
feat(skills): /impl (coder-backend) + /align (plan-keeper) pour boucle TDD V2.1
- Skills manquants identifiés dans le wrap V2 foundation, créés en début de V2.1
- `.claude/skills/impl/SKILL.md` — délègue au subagent `coder-<target>` (backend par défaut). Valide prérequis (spec ≥ ready, tests RED présents et bien rouges), résout `tests_red_path` via frontmatter `tested_by:` de la spec, prépare `brief.json` (contrat `CodeBrief`), délègue. Linked-list : prev `/tdd`, next `/review`
- `.claude/skills/align/SKILL.md` — délègue à `plan-keeper` pour audit déviations plans ↔ livraison. Read-only strict. Compare plans de la branche courante à l'état actuel des fichiers (snapshot ici-et-maintenant, pas de diff vs main). Prépare `brief.json` (contrat `PlanAuditBrief`) avec `triggered_by`/`recent_changes`/`severity_threshold`. Premier vrai test du plan-keeper en conditions réelles (deferred Option C dans wrap V2 foundation). Linked-list : prev `/review`, next `/commit`

### 2026-04-23 `bcde863`
docs(plans/agents): patch tous les paths vault/02_Projects/SamsungHealth/ → docs/vault/ suite vault Obsidian dédié
- `.claude/agents/spec-writer.md` — `vault/02_Projects/.../specs/` → `docs/vault/specs/` (×2 occurrences)
- `.claude/agents/documenter.md` — `vault/02_Projects/.../codex/` → `docs/vault/codex/` (×3 occurrences)
- `.claude/agents/plan-keeper.md` — discovery glob `vault/02_Projects/.../specs/` → `docs/vault/specs/` (×2 occurrences)
- `agents/README.md` — Master plan + Plan multi-agents pointers (×2)
- `docs/vault/README.md` — top-level link plan code-as-vault → `specs/` interne
- `docs/vault/specs/2026-04-23-plan-v2-multi-agents-architecture.md` — header + section codex CI (×2)
- `docs/vault/specs/2026-04-23-plan-v2-refactor-master.md` — header note ajoutée (specs versionnées + vault dédié)
- Skills `.claude/skills/**/*.md` audités → 0 référence à patcher (skills déjà cohérents)
- Re-bootstrap mirror : 78 notes vault, structure inchangée, paths internes propres
- 175/175 tests GREEN

### 2026-04-23 `48e5876`
feat(vault): vault Obsidian dédié pour SamsungHealth (séparé du PKM perso)
- **Path final** : `C:\Users\idsmf\Documents\Obsidian\SamsungHealth\` = `/mnt/c/Users/idsmf/Documents/Obsidian/SamsungHealth/`
- **Avant** : mirror posé dans `PKM/vault/02_Projects/SamsungHealth/` (intégré au vault PKM perso)
- **Maintenant** : vault Obsidian distinct, ouvert via `File → Open vault → Open folder as vault`
- `vault/02_Projects/SamsungHealth/` côté PKM **supprimé entièrement** (plus rien là-bas)
- `CARTOGRAPHER_MIRROR_TO` mis à jour : `~/.zshrc`, `Makefile` exemple, `docs/vault/README.md` setup
- Bootstrap : 78 notes vault dans le nouveau path, structure identique au repo

### 2026-04-23 `4c5c8db`
feat(vault): sortir SamsungHealth du PKM (le mirror EST le dossier 02_Projects/SamsungHealth/) + migrate codex/release + work template vers repo
- **Cleanup PKM** : suppression `vault/02_Projects/SamsungHealth/` entier (codex/, work/, specs/ stubs, code-vault/) — structure remplacée par mirror direct
- Migration `codex/release/nightfall-loom-v1.md` PKM → `docs/vault/codex/release/nightfall-loom-v1.md` (versionné dans le repo)
- Migration `work/_template.md` PKM → `docs/vault/_template/work-session.md`
- `CARTOGRAPHER_MIRROR_TO` mis à jour : `/mnt/c/.../PKM/vault/02_Projects/SamsungHealth/` (sans le sous-dossier `code-vault/`) — env var permanente dans `~/.zshrc`
- `Makefile` target `vault-mirror` exemple de path mis à jour
- `docs/vault/README.md` setup réécrit : "le mirror EST 02_Projects/SamsungHealth/", warning édition manuelle perdue
- Bootstrap mirror : 78 notes vault dans le nouveau path. Structure PKM identique au repo : `annotations/`, `assets/`, `changelog/`, `code/`, `codex/`, `_index/`, `specs/`, `_template/`

### 2026-04-23 `ac24832` → `52c5fc9` (Phase A.8 — specs in vault, 5 blocs)

Specs deviennent first-class dans le vault. Discipline spec-first adoptée : `plan` (méta-architecture) vs `spec` (unitaire < 1 semaine). Top-down `tested_by:` côté spec.

**Bloc 1** `ac24832` — migration 7 specs PKM → `docs/vault/specs/` (single source of truth dans le repo) + frontmatter `type:spec/plan` + stubs PKM avec wikilinks vers le repo.

**Bloc 2** `14a129a` — contrat Pydantic + spec_indexer
- `agents/contracts/spec.py` — `SpecMeta`, `SpecImplements`, `SpecTestedBy`, `SpecType` Literal (spec/plan/us/feature/stub), `SpecStatus` Literal (draft/ready/approved/in_progress/delivered/superseded/reference)
- `agents/cartographer/spec_indexer.py` — `load_spec`, `build_index`, `discover_spec_paths`, `detect_implements_drift`, `untested_specs` (returns list[str] de slugs spec sans tested_by)
- 8 tests + 5 re-exports
- Tolérance YAML : `created` accepte `str | date`, `extra: ignore` sur SpecMeta

**Bloc 3** `728ea5d` — note_renderer + index_generator integration
- `note_renderer` accepte `spec_index`, ajoute :
  - "Implements specs" section dans appendix code (file → specs)
  - "Specs:" annotation per-symbol (matching impl symbols)
  - "Validates specs" section sur tests (test → specs)
  - "Targets" section sur spec notes (auto Implementation + Tests rendus depuis frontmatter)
- `cli` : `_load_spec_index()` + `_render_spec_notes()` (spec-summary mirror dans `code/specs/`)
- `index_generator.generate_specs_index()` → `_index/specs.md` (table 5 colonnes + Untested specs section avec ⚠️)
- 4 nouveaux tests

**Bloc 4** `1b82456` — plan-keeper +2 deviations
- `DeviationType` += `spec_implements_drift`, `untested_spec` (Literal passe à 13 valeurs)
- Subagent prompt enrichi : table de détection + snippet bash réutilisant `spec_indexer`
- 1 test étendu (couverture des 13 valeurs)

**Bloc 5** `52c5fc9` — skill `/spec`
- `.claude/skills/spec/SKILL.md` génère squelette frontmatter pré-rempli (type/status/created/related_plans/implements []/tested_by [])
- Body template : Vision (2-3 phrases) / Décisions techniques / Livrables (checklist) / Tests d'acceptation (given/when/then) / Suite naturelle
- Refuse collisions, valide slug regex `^[a-z0-9][a-z0-9-]{2,40}$`
- `next_default: /tdd`

**État post-A.8** : 78 notes vault (+8 specs incl. nouvelle spec A.8), 7 specs indexées, 1 spec délivrée (nightfall) + 4 plans + 1 reference + 1 ready, 4 specs warning "untested" (action humaine), 175/175 tests GREEN. Code → spec → tests bidirectionnel visible dans Obsidian (mirror Windows).

**Plans patchés** : master + multi-agents + nouveau `2026-04-23-plan-specs-in-vault.md` dans le vault.

### 2026-04-23 `5108be3` → `6bbbdb2` (Phase A.7 — test↔code linking, 4 blocs)

**Bloc 1** `5108be3` — `tests/**`, `android-app/**/test/**` ajoutés à `DEFAULT_SOURCE_GLOBS` du cartographer + re-bootstrap (49 → 65 notes vault, +15 tests/conftest)

**Bloc 2** `0bcd477` — coverage manifest generator (115 symbols / 90 tests / 38 files)
- Added `agents/cartographer/coverage_map.py` — `run_pytest_cov()` (subprocess pytest avec dynamic_context + show_contexts), `parse_coverage()` (normalise JSON coverage.py → manifest 3 vues : by_symbol/by_test/by_file), `tests_for_range()` (intersection lignes ↔ contexts), `write_manifest()`. CLI `python -m agents.cartographer.coverage_map`
- Added `.claude/skills/sync-coverage/SKILL.md` — wrapper skill, next_default `/sync-vault --full`
- Updated `.gitignore` — `docs/vault/_index/coverage-map.json`, `coverage.json`, `.coverage*`, `.coveragerc-cartographer`
- Updated `requirements.txt` — `pytest-cov>=5.0`, `coverage>=7.0`
- Tests : 153/153 GREEN (146 + 7 nouveaux : parse fixture + smoke pytest réel)

**Bloc 3** `049b24f` — note_renderer intègre coverage + fix walker UTF-8 byte slicing
- Updated `agents/cartographer/note_renderer.py` — `render_note()` accepte `coverage_manifest` + `coverage_raw`, frontmatter expose `coverage_pct`, appendix Symbols liste `Tested by (N): test_X, ...`, callout annotation reçoit sub-callout `> [!test]+ Tested by` (intersection range), test files reçoivent section `## Exercises` (inverse map)
- Updated `agents/cartographer/cli.py` — `_load_coverage()` charge manifest + raw, propage à `_render_one`
- Updated `agents/cartographer/index_generator.py` — `generate_coverage_map_index()` génère `_index/coverage-map.md` (table fichiers + section "Untested symbols")
- **Fix walker** — `_node_text()` utilisait `source[node.start_byte:node.end_byte]` (slice CHAR avec offsets BYTES) → cassait sur multi-byte. Fix : `source.encode('utf-8')[start:end].decode()`. Symboles affichés correctement maintenant (avant : "erParseError(Val", après : "MarkerParseError")
- Tests : 158/158 GREEN (153 + 4 TestCoverageIntegration + 1 TestGenerateCoverageMapIndex)

**Bloc 4** `6bbbdb2` — CI workflow `.github/workflows/coverage.yml`
- Run sur push/PR vers main+dev (paths-ignore vault+md+claude)
- `python -m agents.cartographer.coverage_map` → manifest gitignored
- Threshold gate opt-in via `vars.COVERAGE_MIN_PCT` (off par défaut)
- Upload artifact `coverage-map` (manifest + raw, retention 14j)

**État post-A.7** : 67 notes vault, 90 tests indexés, 115 symbols mappés, coverage par symbole + par range + par fichier visible dans Obsidian. Mirror Windows synchronisé.

### 2026-04-23 `a3f9c30`
feat(phase-a.6): annotation-suggester subagent + contrat Pydantic + hook post-commit opt-in (CARTOGRAPHER_SUGGEST=1) (8 tests GREEN)
- Added `agents/contracts/annotation_suggester.py` — `AnnotationSuggestionBrief` (triggered_by post_commit/manual/skill, max_suggestions=5, confidence_threshold), `SuggestedAnnotation` (slug regex + file/line ou begin_line/end_line + rationale + body_draft + confidence low/medium/high + triggers liste), `AnnotationSuggestionReport` (overall suggestions_pending/no_suggestion/failed, next_recommended annotate/commit/none)
- Updated `agents/contracts/__init__.py` — +4 re-exports
- Added `.claude/agents/annotation-suggester.md` — subagent (Read/Grep/Bash, sonnet, color cyan). Heuristiques 8 triggers (issue_ref/pr_ref/kw:workaround/perf/security/semantic, complexity, regression_zone). Filtre dédoublonnage marqueurs existants + cap max_suggestions. Output `${WORK_DIR}/suggestions.md` pour review humaine. **JAMAIS d'écriture sur code/annotations** — propose seulement
- Added `.githooks/post-commit` (executable, opt-in via `$CARTOGRAPHER_SUGGEST=1`) — enregistre `work/post-commit-suggest/<sha>/brief.json` que l'humain materialise via `/annotate suggest --brief <path>`. Hook ne lance pas Claude Code lui-même (incompatible avec git hook context)
- Tests : 146/146 GREEN (139 + 7 nouveaux TestAnnotationSuggester + 1 re-export check)
- **Phase A.6 ✓** — A.5 + A.6 = code-vault complet avec proposition d'annotations automatique

### 2026-04-23 `dc2f108`
feat(cartographer): --mirror-to + bootstrap miroir Windows pour Obsidian (workaround EISDIR WSL chokidar) + Makefile target vault-mirror + doc
- **Contexte** : Obsidian Windows échoue avec `EISDIR: illegal operation on a directory, watch \\wsl.localhost\...` quand on essaie d'ouvrir un dossier sous WSL — bug connu chokidar/Electron sur les UNC paths WSL. Solution adoptée : mirror sens unique vers `/mnt/c/.../PKM/vault/02_Projects/SamsungHealth/code-vault/`
- Updated `agents/cartographer/cli.py` — `run()` accepte `mirror_to`, `_mirror_vault()` fait `rmtree+copytree` (purge stale notes) + écrit `MIRROR-README.md` warning. CLI flag `--mirror-to`, défaut env var `$CARTOGRAPHER_MIRROR_TO`
- Updated `tests/agents/test_cli.py` — `TestMirror` (3 tests : copy, overwrite stale, no-op si None)
- Updated `.githooks/pre-commit` — passe `--mirror-to $CARTOGRAPHER_MIRROR_TO` si l'env var est définie
- Updated `Makefile` — target `vault-mirror` (full re-render + copy)
- Updated `docs/vault/README.md` — section "Comment ouvrir" remplacée par setup mirror Windows + règles read-only
- Updated `.gitignore` — `docs/vault/.obsidian/` (config locale Obsidian, pollue les diffs)
- Persisté `export CARTOGRAPHER_MIRROR_TO=/mnt/c/Users/idsmf/Documents/PKM/vault/02_Projects/SamsungHealth/code-vault` dans `~/.zshrc`
- Bootstrap mirror exécuté : 50 notes code + 30 changelog + 3 indexes + MIRROR-README.md visibles dans le PKM Obsidian Windows
- Tests : 139/139 GREEN (136 + 3 mirror)

### 2026-04-23 `8f08068`
feat(plan-keeper): +3 deviation_types vault (vault_orphan_annotation/vault_missing_note/vault_outdated) + détection détaillée dans subagent prompt
- Updated `agents/contracts/plan_keeper.py` — `DeviationType` Literal étendu à 11 valeurs (8 originaux + 3 vault_*)
- Updated `tests/agents/test_contracts.py::TestPlanKeeper::test_deviation_type_literal` — couvre les 11 valeurs (8/8 GREEN)
- Updated `.claude/agents/plan-keeper.md` — table de détection enrichie avec les 3 types vault et leurs heuristiques (severity scaling sur ancienneté pour orphan, comparaison frontmatter `git_blob` vs `git ls-files -s` pour outdated, glob mismatch pour missing_note) + section "Détections vault — détail technique" avec snippets bash réutilisant `python3 -m agents.cartographer.cli --check`
- Tests : 136/136 GREEN
- Hook pre-commit live : 2 sources stagées → 2 notes vault auto-stagées (création de `docs/vault/code/tests/agents/test_contracts.md` — révèle que le glob bootstrap manque `tests/**` à inclure plus tard)
- **Phase A.5 task #11 ✓ — Phase A.5 100% complète (11/11 tasks)**

### 2026-04-23 `25f51de`
feat(phase-a.5): générateur changelog vault (1 note par commit, frontmatter+body) + bootstrap 30 dernières notes (7 tests GREEN)
- Added `agents/cartographer/changelog_generator.py` — `parse_git_log_records()` (NUL-delimited+RS format), `load_recent_commits()` (git log + git show --name-only par commit), `render_changelog_note()` (frontmatter type/sha/full_sha/date/author/commit_type/scope/files/tags + body Files touched), `generate_changelog()` idempotent (skip si fichier existe sauf `regenerate=True`), CLI `python -m agents.cartographer.changelog_generator --limit N --regenerate`
- Parse Conventional Commits (extrait `commit_type` et `scope` depuis `feat(scope): subject`)
- Tests : 136/136 GREEN (129 + 7 nouveaux ; mocking via `monkeypatch.setattr` sur `load_recent_commits`)
- **Bootstrap exécuté** : 30 notes dans `docs/vault/changelog/` (filename `<YYYY-MM-DD>-<short-sha>.md`)
- **Hook pre-commit déclenché en live** : 2 fichiers staged → 2 notes vault re-rendues + auto-stagées ✓
- Phase A.5 task #10 ✓ ; reste #11 plan-keeper extension (3 deviation_types vault)

### 2026-04-23 `d6f030d`
feat(phase-a.5): hook pre-commit cartographer (re-render notes vault sur staged sources) + make setup-hooks active core.hooksPath
- Added `.githooks/pre-commit` — filtre les fichiers staged par extensions source (`.py|.js|.mjs|.cjs|.kt|.kts|.html|.htm|.css`) ou path `docs/vault/annotations/`. Si annotation seule → `--full` (re-scan complet 5s). Si sources staged → `--diff <files>` (incrémental). Auto-stage `docs/vault/code/` + `docs/vault/_index/` régénérés. Skippable via `--no-verify` (déconseillé)
- Updated `Makefile` — ajout target `setup-hooks` (`git config core.hooksPath .githooks`), invoqué automatiquement par `install`. Active aussi le pre-push existant (branch naming check)
- Hook activé pour la session courante : `git config core.hooksPath .githooks`
- Test à blanc : modif de `agents/cartographer/__init__.py` → 1 note vault re-rendue + auto-stagée ✓
- Phase A.5 task #9 ✓ ; reste #10 changelog gen, #11 plan-keeper extension

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
