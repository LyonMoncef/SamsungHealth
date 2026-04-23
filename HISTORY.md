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
