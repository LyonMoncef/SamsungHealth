---
title: Plan V2 Refactor — Master
type: plan
project: SamsungHealth
phase: master
status: approved
created: 2026-04-23
updated: 2026-04-23
branch: refactor/v2
milestones: [V2-P0, V2-P1, V2-P2, V2-P3, V2-P4, V2-P5, V2-P6]
tags: [samsunghealth, plan, v2, refactor, master, rgpd, hds]
related_plans:
  - 2026-04-23-plan-v2-multi-agents-architecture.md  # approuvé 2026-04-23
  - 2026-04-23-plan-code-as-vault.md                 # approuvé 2026-04-23 (Phase A.5)
vault_links:
  claude_plan_file: /home/tats/.claude/plans/glittery-napping-rossum.md
  parent_spec: 2026-04-21-spec-nightfall-sleep-dashboard.md
  csv_schema: samsung-health-csv-schema.md
---

# Plan V2 — SamsungHealth Refactor complet (master)

> Plan approuvé le 2026-04-23. Source dans Claude Code : `/home/tats/.claude/plans/glittery-napping-rossum.md`.
> Ce fichier est **versionné dans le repo** depuis Phase A.8 (`docs/vault/specs/`) — single source of truth.
> Visible dans Obsidian via le **vault dédié** `C:\Users\idsmf\Documents\Obsidian\SamsungHealth\` (mirror auto, separated from PKM perso).

## Contexte

Projet en refonte complète. État actuel : FastAPI + SQLite single-user local, aucune auth, dashboard dans le browser uniquement, app Android qui sync Samsung Health via Health Connect. La refonte cible :

1. **Deux environnements** : `dev` (docker-compose local, rendu WebView pour itérer vite) et `prod` (VPS self-hosted cloud-ready, rendu Compose natif dans l'APK)
2. **Multi-user** : auth email+password + Google OAuth + reset password (pattern DataSaillance), biométrie et passkeys en backlog
3. **Postgres** remplace SQLite (scalabilité, audit, future migration HDS cloud triviale via env vars)
4. **Import initial mobile** : l'utilisateur choisit un dossier Samsung Health extract sur son smartphone (Storage Access Framework), l'app parse et POST au backend
5. **Sync continu** : Health Connect Android → backend (comme actuellement mais auth + TLS)
6. **Rendu UI** : Compose shell natif (auth, settings, import, biométrie) + dashboard data-viz en dual-track (WebView `static/` en dev, Compose Canvas en prod)
7. **Logging intensif** : chaque transformation, changement d'état, rendu loggués, fichiers structurés queryables par datetime + keywords, pas de fichiers 50k lignes illisibles
8. **Spec Obsidian source of truth** : chaque écran / viz décrit avec contrat de données, layout, tokens, interactions, assertions. Les deux implémentations UI (WebView et Compose) découlent de la même spec.
9. **Cible technique HDS** : chiffrement at-rest, audit 12 mois, RGPD endpoints (export/erasure). Certification formelle différée au switch cloud HDS futur.

## Décisions techniques (arrêtées)

| Sujet | Choix | Raison |
|-------|-------|--------|
| **Backend hosting prod** | Self-hosted VPS, architecture 12-factor | Switch HDS cloud futur = changer env vars, pas le code |
| **DB** | Postgres 16 (dev + prod), `asyncpg` + SQLAlchemy 2.0 async | Multi-user, audit, concurrence, pattern DataSaillance |
| **Migrations** | Alembic async | Auditabilité schéma, standard Python |
| **Auth** | FastAPI + `authlib` + `bcrypt` + `PyJWT`, pattern `AuthProvider` interface | Swap Keycloak futur = ajouter 1 fichier + 1 env var |
| **Providers auth phase 1** | Email+password, Google OAuth, reset password par email (token verification) | Mail = standard, Google = 1-tap Android, reset = besoin UX basique |
| **Backlog auth** | Magic link, biométrie (BiometricPrompt), passkeys (WebAuthn), Apple Sign-in | Reporté post-V1 |
| **Session mobile** | JWT access token (15min) + refresh token (30j) en EncryptedDataStore | Standard OAuth 2.0 |
| **Chiffrement at-rest** | AES-256-GCM applicatif sur champs santé Art.9, clé en env var `APP_ENCRYPTION_KEY` | Switch vers `pgcrypto` ou TDE cloud HDS trivial |
| **Chiffrement transit** | TLS 1.3 via Caddy en reverse proxy (Let's Encrypt prod, mkcert dev) | Caddy fait HTTPS + HTTP/3 out-of-box |
| **UI Android prod** | Compose shell (auth/settings/import) + Compose Canvas (dashboard) | Natif complet, rendu système cohérent |
| **UI Android dev** | Compose shell + WebView chargé sur `static/` bundled | Itération rapide navigateur, même shell Compose |
| **Parité UI** | Spec Obsidian = source of truth, tests de parité visuelle (Playwright vs Compose screenshot) | Éviter dérive entre dev et prod |
| **Logging** | `structlog` JSON en prod, console humaine en dev, rotation par jour, sharding par keyword | Queryable par datetime + keyword, pas de 50k-lignes-dump |
| **Audit** | Dual sink : table `audit_log` Postgres + fichier JSON-L journalier | Queryable API + export légal |
| **Docker** | docker-compose pour backend (Postgres + FastAPI + Caddy + Mailpit + Adminer) | Android build natif via Gradle |
| **Tests** | pytest + Vitest + Compose UI tests + tests de parité visuelle | TDD RED-first, CI blocante |

## Architecture cible

```
DEV (PC local) :
  docker-compose up → Caddy (443) + FastAPI (8001) + Postgres (5432) + Mailpit + Adminer
  Android emulator → debug APK WebView → charge https://10.0.2.2/static/
  Browser localhost:8001 → dashboard dev

PROD (VPS self-hosted) :
  docker-compose.prod.yml → Caddy (443 Let's Encrypt) + FastAPI + Postgres + SMTP relay
  Android release signée → Compose natif (auth + dashboard Canvas + import SAF + Health Connect sync)
```

## Stratégie de logging

Logs structurés JSON-L, un fichier par scope par jour, queryable par `logq` CLI.

```
~/.local/share/samsunghealth/logs/2026-04-23/
├── app.jsonl          ← lifecycle
├── auth.jsonl         ← login/signup/oauth/refresh
├── sync.jsonl         ← imports Android → backend
├── data-<type>.jsonl  ← transformations par scope
├── rgpd.jsonl         ← exports/erasures
├── errors.jsonl       ← ERROR+ tous scopes
└── verbose-dev.jsonl  ← entry/exit fonctions, dev only
```

Format : `{"ts", "level", "scope", "event", "trace_id", ...champs libres}` — jamais de valeur santé brute.

Rotation : gzip > 7j, suppression > 90j (sauf `auth.jsonl` 13 mois, `rgpd-audit.jsonl` 10 ans).

CLI :
```bash
python -m server.tools.logq --date 2026-04-23 --scope auth --level WARN
python -m server.tools.logq --trace 0a1b2c3d
python -m server.tools.logq --event login_failure --since "1h ago"
```

Android : `Timber` + `FileLogTree` → `filesDir/logs/YYYY-MM-DD/` + upload optionnel via `POST /api/client-logs`.
Frontend JS : wrapper `console.log` en dev uniquement avec batch push.

Audit RGPD (séparé) : table `audit_log` Postgres + fichier `logs/.../rgpd-audit.jsonl`.

## Phases d'exécution

### Phase 0 — Infra foundation (3-5j)

`spec-p0-infra-foundation.md`, `spec-p0-logging-strategy.md`

Docker-compose, Postgres 16, Caddy TLS, Alembic async, structlog, CLI `logq`, migration SQLite → Postgres, Makefile, tests TDD RED.

### Phase 1 — Auth system (4-6j)

`spec-p1-auth-system.md`

Email+password, Google OAuth, reset password, JWT access/refresh, pattern `AuthProvider` interface (swap Keycloak futur trivial). Rate limiting login. 20+ tests TDD.

Schéma : `users`, `verification_tokens`, `refresh_tokens` (adapté DataSaillance).

### Phase 2 — Data layer + routers TDD (5-7j)

`spec-p2-sleep.md`, `spec-p2-steps.md`, `spec-p2-heartrate.md`, `spec-p2-exercise.md`

Routers SQLAlchemy async, `Depends(get_current_user)`, multi-user, chiffrement AES-256-GCM champs Art.9, logging par scope, 10+ tests/router.

### Phase 3 — RGPD endpoints (2-3j)

`spec-p3-rgpd.md`

`GET /api/rgpd/export` (ZIP JSON-LD), `DELETE /api/rgpd/erase` (DELETE + rotation clé = effacement cryptographique), consent management, certificat destruction.

### Phase 4 — Android shell + WebView dashboard (5-8j)

`spec-p4-android-shell.md`, `spec-p4-android-import.md`, `spec-p4-android-auth-screens.md`, `spec-p4-webview-bridge.md`

Écrans Compose natifs (auth/import/settings/consent), dashboard WebView en `assets/` bundled, SAF folder picker + parser Samsung Health, Health Connect sync avec JWT, Timber logging.

Build flavors : `webview` (dev) + `native` (prod).

### Phase 5 — Compose Canvas natif dashboard (10-15j)

5 specs UI par viz : `spec-p5-dashboard-{hypnogram,radial,timeline,cards,metrics}.md`

Chaque viz : spec → test parité (Playwright vs Paparazzi) → impl Compose Canvas → tolerance < 2% diff.

Ordre : night cards → hypnogram → timeline → radial clock → metrics cards.

### Phase 6 — CI/CD multi-env (2-3j)

`spec-p6-ci-cd.md`

7 jobs CI (backend lint/test/security, frontend Vitest, Android lint/unit, UI parity). Deploy auto dev, manuel prod avec approval.

## Workflow GitHub + Obsidian

### Repo : on continue dans `SamsungHealth` existant

Branche long-lived `refactor/v2` (renommée depuis `chore/v2-refactor` après extension du pre-push hook avec `refactor/`+`spike/`). Checkpoint tag avant démarrage : `checkpoint-before-v2-refactor-2026-04-23` (sur origin/main HEAD `5343c9b`).

### Milestones GitHub

`V2-P0` → `V2-P6`, un par phase, chaque issue attachée à son milestone.

### Labels

- Type : `feat|fix|chore|refactor|spike|ux|arch|docs-only`
- Priorité : `P0|P1|P2|P3`
- Domaine : `backend|android-compose|android-webview|dashboard|auth|rgpd|logging|ci|infra`
- Status : `blocked|in-review|tested`
- RGPD : `rgpd-critical|adr|decision|security`
- Recurring : `audit:deps|audit:security|audit:perf`

### Issue + PR templates

`.github/ISSUE_TEMPLATE/{feature,bug,adr}.md` + `.github/pull_request_template.md` → chaque issue référence une spec Obsidian (`Spec: vault/...`), chaque PR liste tests RED/GREEN, migrations Alembic, impact logging.

### Codex / Vault linking (pattern DataSaillance)

`codex-jobs.yml` à la racine + `.github/workflows/codex-pipeline.yml` :
- PR `feat` mergée → `vault/.../codex/feature/{N}-{slug}.md`
- PR `fix` mergée → `vault/.../codex/bug/{N}-{slug}.md`
- PR touchant `alembic/versions/**` → `vault/.../codex/migration/{rev}.md`
- Issue `adr|decision` closée → `vault/.../codex/adr/issue-{N}.md`
- PR fermant issue `has-spec` → update frontmatter spec (status, commits, PRs)

### Frontmatter spec standard

```yaml
---
title: Spec — <Module>
phase: P2
slug: sleep
status: draft | ready | in-progress | implemented | archived
branch: feat/p2-sleep
issues: [#142]
prs: [#148]
commits: [abc1234]
tags: [samsunghealth, spec, p2, sleep, backend]
---
```

### Skills Claude Code à activer / créer

`/daily`, `/wrap`, `/review`, `/pr`, `/triage`, `/issue`, `/spec` (nouveau).

### Workflows à copier/adapter

`ci.yml`, `codex-pipeline.yml`, `review-complete.yml`, `vault-sync.yml`, `recurring-{security,deps}-audit.yml`, `deploy-{dev,prod}.yml`.

### Hook git

`.githooks/pre-push` — enforce `^(feat|fix|chore|hotfix|release|refactor|spike)/`.

## Architecture multi-agents

**Plans dédiés approuvés** :
- [[2026-04-23-plan-v2-multi-agents-architecture]] — orchestration agents
- [[2026-04-23-plan-code-as-vault]] — Phase A.5 méta-projet : miroir Obsidian de la codebase + annotations ancrées

**Résumé** :
- 13 subagents locaux Claude Code (`.claude/agents/`) — spec-writer, test-writer, coder-{backend,android,frontend}, reviewer, documenter, logq-analyst, migration-writer, ui-parity-checker, **git-steward**, **pentester**, **plan-keeper**, **code-cartographer** (+ `annotation-suggester` Phase A.6, `anchor-rescuer` Phase A.7 optionnel)
- 4+ agents CI (Anthropic SDK via `.github/scripts/`) — pr-review, codex-*, audit-security, ui-parity
- Contrats Pydantic versionnés dans `agents/contracts/`
- Handoff par fichiers dans `work/<task-id>/`
- **Observabilité** : Arize Phoenix self-hosted (Docker 1 container), format OTel GenAI semantic conventions 2026
- **Git-graph analogy** : orchestrateur = main, chaque agent = branche, tool calls = commits, human approval = merge, rendu Mermaid `gitGraph`
- Ingestion JSONL Claude Code + traces OTel → tables Postgres `harness_runs`, `harness_spans`, `harness_messages`, `harness_entities`, `harness_alerts`
- Dashboard admin `/admin/harness/*` pour naviguer runs/spans/messages (port DataSaillance `dev_tracking/`)
- Budget enforcement pré-exécution (leçon $47k : alerts ≠ enforcement)
- Alerts : retry >= 2, budget exceeded, loop detected, stuck run, review blocker age, human review pending
- Canaux : GitHub issue comment, email digest, Slack/Discord webhook optionnel

Phasing méta-projet :
- Phase A (3-4j) : foundation agents (8 subagents : spec-writer, test-writer, coder-backend, reviewer, documenter, git-steward, pentester, plan-keeper) — **avant P0 master**
- **Phase A.5 (4-5j) : code-as-vault** (cartographer + miroir docs/vault/code/ + annotations ancrées + skills /sync-vault, /annotate, /anchor-review + hook pre-commit) — peut démarrer en parallèle de A étapes 3+
- Phase A.6 (2-3j) : annotation-suggester — **après A.5** — ✅ livré 2026-04-23
- **Phase A.7 (~2h) : test ↔ code linking** (coverage_map + skill /sync-coverage + note_renderer intégration + CI workflow) — ✅ livré 2026-04-23
- **Phase A.8 (~3h) : specs in vault** (migration PKM → repo + spec_indexer + bidirectional links spec ↔ code ↔ tests + skill /spec + plan-keeper +2 deviations + discipline spec-first) — ✅ livré 2026-04-23
- Phase B (4-5j) : observabilité + Phoenix + dashboard harness — pendant P1 master
- Phase C (2-3j) : agents CI — pendant P2 master
- Phase D (1-2j) : agents Android — avant P4 master
- Phase E (2-3j) : obs avancée (replay, diff, webhooks) — pendant P5 master

Total méta-projet : ~17-23 jours étalés sur les 6-8 semaines du master plan.

## Vérification end-to-end (post-impl)

1. `make up` → stack dev up, `curl https://localhost/health` → 200
2. `make migrate-sqlite` → data legacy sous user `legacy@local`
3. `make logs --scope auth --since 10m` → 20 lignes JSON formatées
4. Signup → mail Mailpit → click → `email_verified_at` set
5. Google OAuth Android → JWT reçu
6. Import folder Android → logs `sync.jsonl` + data en DB
7. `SELECT min_bpm FROM heart_rate_hourly` → ciphertext illisible
8. Isolation multi-user user A ≠ user B
9. `GET /api/rgpd/export` → ZIP JSON-LD
10. `DELETE /api/rgpd/erase` → reçu + tables vidées + audit conservé
11. Dashboard browser dev + WebView emulator + Compose natif prod : parité visuelle < 2% diff
12. CI 7 jobs green
13. `logq --trace <id>` → 12 lignes qui expliquent un bug, pas 50k

## Estimation totale

~6-8 semaines de travail cumulé. V1 prod = P0→P4 (WebView dashboard). V2 = + P5 (Compose natif).
