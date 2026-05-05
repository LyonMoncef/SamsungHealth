# SamsungHealth

## Problem

Samsung Health data is locked inside the mobile app with no open API. Visualizing sleep patterns over time requires manual CSV exports and external tools.

## Solution

An end-to-end pipeline that automatically syncs Samsung Health data via Android Health Connect, stores it in a database, and serves an interactive web dashboard with tabs for sleep, steps, heart rate, exercise, and trends.

## Features

| Feature | Files | Commit |
|---------|-------|--------|
| Sleep API + stage-aware calendar | `server/routers/sleep.py`, `static/app.js` | `8d5cfb0` |
| Steps API + bar chart | `server/routers/steps.py`, `static/app.js` | `242040a` |
| Heart rate API + range chart | `server/routers/heartrate.py`, `static/app.js` | `242040a` |
| Exercise API + card list | `server/routers/exercise.py`, `static/app.js` | `242040a` |
| Tabbed dashboard + trends | `static/index.html`, `static/app.js`, `static/style.css` | `242040a` |
| Android Health Connect sync | `android-app/` | `242040a` |
| Sample data generator | `scripts/generate_sample.py` | `242040a` |

## Architecture

```
Samsung Health → Health Connect ← Android App →HTTP→ FastAPI + SQLAlchemy →→ Postgres 16
                                                            ↓
                                                   Tabbed Web Dashboard
                                            (Sleep | Steps | HR | Exercise | Trends)
```

### Components

| Component | Tech | Status |
|-----------|------|--------|
| Backend API | Python, FastAPI, SQLAlchemy 2.x | Done |
| Database | Postgres 16 (Alembic-managed, UUID v7 PK) | Done (V2.1) |
| Web dashboard | HTML/CSS/JS (5 tabs) | Done |
| Android app | Kotlin, Jetpack Compose, Health Connect | Done |
| Sample data generator | Python (SQLAlchemy depuis V2.1.2) | Done |
| CSV import (Samsung Health export) | Python (SQLAlchemy + ON CONFLICT depuis V2.1.2) | Done |

## Setup

Prérequis : Docker (pour Postgres local).

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# Démarrer Postgres + appliquer le schema
make db-up
make db-migrate
```

Variables d'environnement :
- `DATABASE_URL` — URL SQLAlchemy (défaut : `postgresql+psycopg://samsung:samsung@localhost:5432/samsunghealth`)
- `APP_ENV` — `dev` (ConsoleRenderer humain) ou `prod` (JSON stdout). Défaut : `prod`.
- `LOG_LEVEL` — `DEBUG | INFO | WARNING | ERROR | CRITICAL`. Défaut : `INFO`.

## Auth setup (V2.3)

Multi-utilisateur avec JWT HS256 + argon2id password hashing. Toutes les routes santé (`/api/*`) exigent `Authorization: Bearer <access_token>`.

Variables d'environnement requises :

```bash
# Génère un secret 256-bit base64 :
python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
SAMSUNGHEALTH_JWT_SECRET=<256-bit-base64-secret>

# Optionnel — secret précédent (decode-only) pour rotation sans logout global.
SAMSUNGHEALTH_JWT_SECRET_PREVIOUS=

# Token statique pour gating de POST /auth/register (header X-Registration-Token).
# Sans ce token l'endpoint répond 403 (registration_disabled).
SAMSUNGHEALTH_REGISTRATION_TOKEN=<32-chars-random>
```

### Endpoints

```
POST /auth/register   headers: X-Registration-Token, body: {email, password}
                      201 → {id, email}    409 → email_already_exists    403 → registration_disabled
POST /auth/login      body: {email, password}
                      200 → {access_token, refresh_token, token_type, expires_in}
                      401 → invalid_credentials  (identique sur user inconnu OU password faux)
POST /auth/refresh    body: {refresh_token}
                      200 → {access_token, refresh_token, ...}  (NOUVEAU refresh, ancien révoqué)
                      401 → invalid_refresh
POST /auth/logout     Authorization: Bearer <access>, body: {refresh_token}
                      204 (idempotent)
```

### Post-migration legacy user

Lors de la première migration alembic vers V2.3, si la table `users` est vide ET les tables santé contiennent déjà des données, un utilisateur `legacy@samsunghealth.local` (`is_active=false`, password aléatoire impossible-to-login) est auto-créé pour absorber les rows existantes via `user_id` FK. Action admin post-migration : créer un vrai user via `/auth/register` puis migrer manuellement les rows si désiré.

## Logs

Pipeline de logs structurés via `structlog` (V2.0.5).

- **Format prod** : 1 ligne JSON par event sur stdout, capture/rotation déléguée au runtime (docker compose, systemd).
- **Format dev** : `ConsoleRenderer` lisible humain (couleurs).
- **Champs standards** : `timestamp` (ISO8601 UTC), `level`, `logger` (scope), `event`, `request_id`, `user_id`, `route`, `latency_ms`.
- **Corrélation** : chaque request HTTP reçoit un `X-Request-ID` (généré ou propagé depuis le client) bindé via `contextvars` à tous les logs émis pendant son traitement. Le middleware émet `event: "request.complete"` à la fin avec `latency_ms` et `route` (template FastAPI).

Exemple ligne :
```json
{"event":"request.complete","logger":"server.middleware.request_context","route":"/api/sleep","method":"GET","status":200,"latency_ms":12.345,"request_id":"a1b2…","user_id":null,"level":"info","timestamp":"2026-04-26T10:00:00.000Z"}
```

## Usage

```bash
# Lancer le serveur
make dev

# Ouvrir http://localhost:8001 dans le navigateur

# Reset complet de la DB (DESTRUCTIVE — drop + recreate)
make db-reset
```

⚠️ **Migration depuis V1 (SQLite)** : V2.1 supprime SQLite. Si tu as un `health.db` legacy, sauvegarde-le séparément ; les CSV Samsung peuvent être ré-importés via le pipeline (refonte SQLAlchemy en cours via spec V2.1.2 — attendre avant ré-import).

## Déploiement

### Vue d'ensemble

Phase 6 déploie SamsungHealth sur un VPS perso via Docker Compose, avec un pipeline CI/CD GitHub Actions orchestrant :
- **Dev** : push sur `main` → build GHCR `dev-<sha>` → SSH VPS dev → docker compose up → smoke tests + auto-rollback
- **Prod** : workflow_dispatch avec approval GitHub environment → SSH VPS prod → deploy + smoke + auto-rollback

Aucun PaaS (Render, Fly.io, etc.) — coût nul, full control, acceptable pour usage perso 1 utilisateur. Trade-off conscient : ops à charge (sécu, backups, fail2ban, rotation clés).

### Architecture réseau (VPS prod)

```
External clients
        ↓
[Caddy/reverse-proxy:80/443]
        ↓
[/healthz OK publicly + /readyz BLOCKED external IPs]
        ↓
Docker network (localhost:8001)
        ├─ web (app FastAPI)
        └─ postgres (non-exposed, réseau interne)
```

**Caddy snippet** (exemple) :

```caddy
samsunghealth.example.com {
    reverse_proxy localhost:8001

    # Bloquer /readyz aux IPs externes — exposition uniquement monitoring/LB interne
    # Mitige D-3: /readyz expose l'état alembic (oracle migrations en cours)
    @readyz_external {
        path /readyz
        not remote_ip 127.0.0.1 ::1 10.0.0.0/8 192.168.0.0/16
    }
    respond @readyz_external 404
}
```

**Avertissement `/readyz` — reverse-proxy OBLIGATOIRE** :
- `/readyz` retourne 503 si migration en cours, exposant `reason: "alembic_mismatch"` (oracle DB version)
- Exposer publiquement = leak sur l'état des migrations (gating pour attaque timing)
- **Mitigation non-optionnelle** : reverse-proxy (Caddy/nginx) filter `/readyz` via remote_ip, jamais expose sans ce gate
- `/healthz` (app alive check) peut être public (utilisé par GitHub Actions smoke tests)

### Bascule local ↔ VPS

Aucun hardcode dans le code — tout env var. Trois modes :

**Mode 1 : Local dev (Postgres docker-compose.yml local)**
```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env  # remplir localement
make db-up && make db-migrate
make dev  # uvicorn :8001
# Ouvrir http://localhost:8001
```

**Mode 2 : Local docker-compose (Postgres + app conteneurisés)**
```bash
cp .env.example .env.prod
# remplir .env.prod (notamment IMAGE_TAG=local, DATABASE_URL=postgresql+psycopg://...)
docker compose -f docker-compose.prod.yml up
# .env.prod doit exister (template : .env.prod.example)
```

**Mode 3 : VPS staging/prod (CI/CD auto)**
- Secrets GitHub configurés (VPS_SSH_KEY_DEV, VPS_SSH_KEY_PROD, etc.)
- `.github/known_hosts` pré-rempli avec empreintes SSH VPS (voir Préconditions ci-dessous)
- Push main → auto-deploy dev ; workflow_dispatch → approval + deploy prod
- Chaque VPS a `.env.prod` avec credentials (DATABASE_URL, SAMSUNGHEALTH_JWT_SECRET, etc.) — jamais en GitHub Secrets

### Préconditions de déploiement (bloquant PR Phase 6)

**`.github/known_hosts` — SSH host key verification sans TOFU**

Avec `StrictHostKeyChecking=yes` dans les workflows (mitigation MITM—TOFU), le 1er déploiement échoue si le VPS n'est pas dans `.github/known_hosts`. Proc obligatoire exécuter une fois par VPS :

```bash
# Sur chaque VPS (dev, prod séparément)
ssh-keyscan -H <VPS_HOST> >> ~/.ssh/known_hosts

# Exemple
ssh-keyscan -H dev.samsunghealth.example >> ~/.ssh/known_hosts
ssh-keyscan -H samsunghealth.example >> ~/.ssh/known_hosts

# Copier au repo
cp ~/.ssh/known_hosts .github/known_hosts

# Committer dans PR séparée (n'active pas les workflows — paths-ignore)
git add .github/known_hosts
git commit -m "chore(infra): add SSH known_hosts for VPS"
git push

# Merger AVANT Phase 6 PR
```

**GitHub Secrets à configurer** (via GitHub UI, env-scoped) :

| Secret | Scope | Valeur |
|--------|-------|--------|
| `VPS_SSH_KEY_DEV` | env `dev` | Clé privée Ed25519 CI dev (autorisée VPS dev uniquement) |
| `VPS_SSH_KEY_PROD` | env `production` | Clé privée Ed25519 CI prod (autorisée VPS prod uniquement) |
| `VPS_DEV_HOST` | env `dev` | `user@dev.samsunghealth.example` |
| `VPS_DEV_DOMAIN` | env `dev` | `dev.samsunghealth.example` |
| `VPS_PROD_HOST` | env `production` | `user@samsunghealth.example` |
| `VPS_PROD_DOMAIN` | env `production` | `samsunghealth.example` |

**`.env.prod` sur chaque VPS** (template : `.env.prod.example`) :

```bash
# Sur le VPS, après git clone / setup initial
cp .env.prod.example .env.prod
chmod 600 .env.prod
chown app:app .env.prod  # (app = uid 1000 dans le container)

# Remplir les valeurs (vi .env.prod ou copier depuis ci-dessus)
# - POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB
# - DATABASE_URL (connection string interne docker network)
# - SAMSUNGHEALTH_JWT_SECRET (générer : python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())")
# - IMAGE_TAG (commencera par dev-<sha> en dev, replié par workflows)

# Vérifier permissions
ls -la .env.prod  # → -rw------- 1 app app ... (chmod 600 obligatoire)
```

### Régénération du lock dependencies

À chaque modification de `requirements.in` :

```bash
pip install pip-tools
pip-compile --generate-hashes requirements.in -o requirements.lock

# Vérifier que tous les hashes sont présents
grep --count "^# via" requirements.lock  # doit être > 0
head -20 requirements.lock  # doit montrer des sha256=...

git add requirements.in requirements.lock
git commit -m "chore(deps): update requirements.lock"
```

**Pourquoi hashes** (HIGH 5 Phase 6 spec) :
- Dockerfile run `pip install --require-hashes -r requirements.lock`
- Prévient les supply-chain attacks (dépendance override, cache poisoning)
- Garantit déterminisme builds

### Workflow VPS dev (auto sur push main)

1. GitHub Actions déclenche `deploy-dev.yml` (push main sauf si TOUS les fichiers = `.md` ou `.claude/` ou `docs/vault/`)
2. Build image GHCR `dev-<commit-sha>` (pas de tag mutable `:latest`)
3. SSH VPS dev (`VPS_SSH_KEY_DEV` + `StrictHostKeyChecking=yes` via `.github/known_hosts`)
4. Backup DB pré-upgrade (`pg_dump | gzip`)
5. Upgrade migrations alembic (avec check `alembic current == prev_head`)
6. `docker compose -f docker-compose.prod.yml up -d` web
7. Smoke test loop (curl `/healthz` pendant 60s)
8. Si smoke échoue → **auto-rollback** vers image précédente (capturée au step 2)

**Rollback automatique** : si smoke fail après 60s, restore IMAGE_TAG précédent + restart web. Workflow exit 1. Investigate offline.

### Workflow VPS prod (manuel, approval required)

1. Déclencher via `gh workflow run deploy-prod.yml --raw-field sha=<commit_sha>` (40 hex ou 7+ chars)
2. Validation : SHA format regex `^[a-f0-9]{7,40}$` (anti command injection)
3. GitHub environment `production` demande approval (manual gate, attendre 1+ reviewer humain)
4. Même flow que dev : backup, upgrade, up, smoke, auto-rollback si fail
5. Image déployée = `dev-<sha>` (du build dev GHCR, pas rebuild prod)

**Précaution** : chaque SHA doit avoir été builté + testé en dev d'abord. Prod redéploie des images prebuildes en dev, jamais de rebuild ad-hoc prod.

### Security & Hardening VPS

Voir `NOTES.md` ADR-3 pour la checklist complète. Résumé MVP :

- [ ] `.env.prod` chmod 600 + chown app
- [ ] SSH password disabled (`PasswordAuthentication no` sshd_config)
- [ ] `fail2ban` sur SSH
- [ ] Firewall UFW : 22 (SSH), 80 (Caddy), 443 (Caddy TLS) only
- [ ] Caddy/reverse-proxy frontline (TLS auto Let's Encrypt)
- [ ] Postgres jamais exposé réseau public (docker internal only)
- [ ] Backups Postgres scheduled (pg_dump cron, optionnel MVP)
- [ ] OS updates unattended (`unattended-upgrades`)
- [ ] Monitoring minimal (logs structlog → syslog, alerting `/healthz` 5xx)

## Related Projects

- [agent-schemas](https://github.com/LyonMoncef/agent-schemas) — Shared schema definitions
- [DocAgent](https://github.com/LyonMoncef/DocAgent) — Config query/edit agent
- [WidgetGenerator](https://github.com/LyonMoncef/WidgetGenerator) — JSON-to-Rainmeter generator
