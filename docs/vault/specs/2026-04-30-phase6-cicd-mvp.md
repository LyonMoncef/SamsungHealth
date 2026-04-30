---
type: spec
title: "Phase 6 MVP — CI/CD multi-env (VPS perso)"
slug: 2026-04-30-phase6-cicd-mvp
status: ready
created: 2026-04-30
delivered: null
priority: high
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs:
  - 2026-04-28-phase3-rgpd-endpoints
implements:
  - file: Dockerfile
    symbols: [base, builder, runner]
  - file: docker-compose.prod.yml
    symbols: [web, postgres]
  - file: .github/workflows/deploy-dev.yml
    symbols: [job-deploy]
  - file: .github/workflows/deploy-prod.yml
    symbols: [job-deploy]
  - file: server/routers/health.py
    symbols: [router, healthz, readyz]
  - file: server/main.py
    symbols: [app]
  - file: .env.example
    symbols: []
tested_by:
  - file: tests/server/test_healthz.py
    classes: [TestHealthz, TestReadyz]
tags: [phase6, cicd, deploy, vps, infra, samsunghealth, spec]
---

# Phase 6 MVP — CI/CD multi-env (VPS perso)

## Vision

Livrer un pipeline CI/CD minimal mais complet permettant :
1. **Déploiement auto dev** sur push `main` → VPS staging
2. **Déploiement manuel prod** via GitHub environment + approval → VPS prod
3. **Bascule local ↔ VPS** sans changer le code (juste env vars)
4. **Branch protection main** : require CI green (+ optionnel : 1 review)

Différé Phase 6+ (master plan) : frontend Vitest, Android lint/unit, UI parity Compose↔WebView, Codex pipeline agents CI.

## Décisions techniques

### 1. Containerisation backend — Dockerfile multi-stage

```dockerfile
# Stage 1: builder
FROM python:3.12-slim AS builder
WORKDIR /app
RUN pip install --no-cache-dir uv
COPY requirements.txt .
RUN uv pip install --system --no-cache -r requirements.txt

# Stage 2: runner (slim runtime)
FROM python:3.12-slim AS runner
WORKDIR /app
RUN useradd -m -u 1000 app
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin
COPY server/ ./server/
COPY alembic/ ./alembic/
COPY alembic.ini .
USER app
EXPOSE 8001
CMD ["uvicorn", "server.main:app", "--host", "0.0.0.0", "--port", "8001"]
```

Image taille cible : ~150 MB (slim + no-cache).

### 2. `docker-compose.prod.yml` — service web + postgres

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      POSTGRES_DB: ${POSTGRES_DB}
    volumes:
      - pgdata:/var/lib/postgresql/data
    restart: unless-stopped
  web:
    image: ghcr.io/lyonmoncef/samsunghealth:${IMAGE_TAG:-latest}
    env_file: .env.prod
    depends_on: [postgres]
    ports: ["8001:8001"]
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8001/healthz"]
      interval: 30s
      timeout: 5s
      retries: 3
volumes:
  pgdata:
```

Bascule local : `cp .env.prod.example .env.prod` + `docker compose -f docker-compose.prod.yml up`. Bascule VPS : même fichier, env vars différentes.

### 3. Endpoints `/healthz` et `/readyz`

`server/routers/health.py` (NEW) :
- `GET /healthz` → 200 `{"status":"ok"}` (process alive, pas de check DB — répond toujours si app boot)
- `GET /readyz` → 200 `{"status":"ready"}` si DB ping OK + alembic head matches expected ; sinon 503

Cohérent avec K8s/Docker healthcheck convention. Pas de bearer requis (pour le LB / monitoring externe).

### 4. Workflow `deploy-dev.yml` — auto sur push main

```yaml
on:
  push:
    branches: [main]
    paths-ignore: ["**/*.md", ".claude/**", "docs/vault/**"]

jobs:
  build:
    runs-on: ubuntu-latest
    permissions: { packages: write, contents: read }
    steps:
      - uses: actions/checkout@v4
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v5
        with:
          push: true
          tags: ghcr.io/lyonmoncef/samsunghealth:dev-${{ github.sha }},ghcr.io/lyonmoncef/samsunghealth:dev-latest

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: dev   # GitHub environment "dev" — non-protected
    steps:
      - uses: webfactory/ssh-agent@v0.9.0
        with: { ssh-private-key: ${{ secrets.VPS_SSH_KEY }} }
      - name: Deploy to VPS dev
        run: |
          ssh -o StrictHostKeyChecking=accept-new ${{ secrets.VPS_DEV_HOST }} '
            cd /srv/samsunghealth-dev &&
            export IMAGE_TAG=dev-${{ github.sha }} &&
            docker compose -f docker-compose.prod.yml pull web &&
            docker compose -f docker-compose.prod.yml run --rm web alembic upgrade head &&
            docker compose -f docker-compose.prod.yml up -d web
          '
      - name: Smoke test
        run: |
          for i in {1..30}; do
            if curl -fs https://${{ secrets.VPS_DEV_DOMAIN }}/healthz; then exit 0; fi
            sleep 2
          done
          exit 1
```

### 5. Workflow `deploy-prod.yml` — manuel + approval

```yaml
on:
  workflow_dispatch:
    inputs:
      sha:
        description: "Commit SHA to deploy (default: latest dev image)"
        required: false

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production   # GitHub environment "production" — protected, manual approval
    steps:
      - uses: webfactory/ssh-agent@v0.9.0
        with: { ssh-private-key: ${{ secrets.VPS_SSH_KEY }} }
      - name: Deploy to VPS prod
        run: |
          TAG="${{ github.event.inputs.sha || 'dev-latest' }}"
          ssh ${{ secrets.VPS_PROD_HOST }} '
            cd /srv/samsunghealth-prod &&
            export IMAGE_TAG="'$TAG'" &&
            docker compose -f docker-compose.prod.yml pull web &&
            docker compose -f docker-compose.prod.yml run --rm web alembic upgrade head &&
            docker compose -f docker-compose.prod.yml up -d web
          '
      - name: Smoke test
        run: curl -fs https://${{ secrets.VPS_PROD_DOMAIN }}/healthz || exit 1
```

### 6. Secrets GitHub à configurer

| Secret | Scope | Contenu |
|--------|-------|---------|
| `VPS_SSH_KEY` | repo | Clé privée Ed25519 dédiée CI (autorisée sur dev + prod via authorized_keys) |
| `VPS_DEV_HOST` | env `dev` | `user@dev.samsunghealth.example` |
| `VPS_DEV_DOMAIN` | env `dev` | `dev.samsunghealth.example` |
| `VPS_PROD_HOST` | env `production` | `user@samsunghealth.example` |
| `VPS_PROD_DOMAIN` | env `production` | `samsunghealth.example` |

Pas de DATABASE_URL/SAMSUNGHEALTH_* en GitHub Secrets : ils vivent dans `.env.prod` sur chaque VPS, jamais en CI.

### 7. Branch protection `main`

Activée via `gh api`. Règles :
- Require status checks before merge : `Tests`, `Lint`, `Coverage` (existants ci.yml)
- Require linear history (rebase/squash only)
- Disallow force-push
- Disallow direct push
- Pas de "require N reviews" (solo dev) — peut être ajouté plus tard

### 8. Bascule local ↔ VPS

Le code ne contient AUCUNE référence hardcodée à un host/port/db. Tout via env vars (DATABASE_URL, SAMSUNGHEALTH_*, etc.). Pour run en local sans VPS :
```bash
docker compose up -d postgres
cp .env.example .env
# remplir les valeurs
make dev   # uvicorn local
```
Pour run en local avec docker compose prod-style :
```bash
cp .env.example .env.prod
docker compose -f docker-compose.prod.yml up
```

## Livrables

### Backend
- [ ] `server/routers/health.py` (NEW) : 2 endpoints `/healthz`, `/readyz` + Pydantic
- [ ] `server/main.py` : `app.include_router(health.router)` + helper alembic head check pour `/readyz`
- [ ] `Dockerfile` (NEW) : multi-stage builder + runner non-root
- [ ] `docker-compose.prod.yml` (NEW) : web + postgres + healthcheck
- [ ] `.dockerignore` (NEW) : exclure tests, .git, docs, .claude, etc.
- [ ] `.env.prod.example` (NEW) : template var prod (POSTGRES_*, DATABASE_URL, IMAGE_TAG, SAMSUNGHEALTH_*)

### Workflows
- [ ] `.github/workflows/deploy-dev.yml` (NEW) : build image GHCR + deploy SSH dev + smoke
- [ ] `.github/workflows/deploy-prod.yml` (NEW) : workflow_dispatch + environment production approval + smoke
- [ ] (optionnel) `.github/workflows/ci.yml` : ajouter step `docker build` pour valider le Dockerfile

### Tests
- [ ] `tests/server/test_healthz.py` (~5 tests : `/healthz` 200 sans auth, `/readyz` 200 si DB ping + alembic head OK, `/readyz` 503 si DB down [mock])

### Docs
- [ ] `README.md` : section "Déploiement" (workflow VPS + bascule local)
- [ ] `NOTES.md` : ADR-3 "Pourquoi VPS perso vs PaaS"
- [ ] `HISTORY.md` : entry post-merge

### GitHub config (manuel post-merge)
- [ ] Créer environments `dev` (no protection) et `production` (required reviewers = self, wait timer 0)
- [ ] Configurer secrets dans chaque environment (pas en repo-level)
- [ ] Activer branch protection main : `gh api repos/.../branches/main/protection` (commande dans README)

## Tests d'acceptation

### Healthz / Readyz
1. `GET /healthz` sans bearer → 200 `{"status":"ok"}`.
2. `GET /readyz` sans bearer + DB up + alembic head OK → 200 `{"status":"ready"}`.
3. `GET /readyz` + DB down (mock) → 503.
4. `GET /readyz` + alembic mismatch (mock) → 503 `{"status":"not_ready","reason":"alembic_mismatch"}`.

### Container
5. `docker build .` → succeeds, image < 200 MB.
6. `docker run -e DATABASE_URL=... <image>` → app boot, `/healthz` returns 200 dans 5 sec.
7. `docker compose -f docker-compose.prod.yml up` (local avec .env.prod test) → web + postgres up, `/healthz` OK.

### Workflows (test sur PR via push branch + workflow_dispatch override)
8. Push sur branche `feat/phase6-cicd-mvp` → `ci.yml` PASSE (existing) + nouveau step docker build PASSE.
9. Trigger manuel `deploy-dev.yml` (post-merge) → image GHCR construite + tag `dev-<sha>` poussé.
10. Smoke test `deploy-dev.yml` → curl `/healthz` retourne 200 sur le domain dev.
11. Trigger `deploy-prod.yml` workflow_dispatch → wait approval (GitHub UI) → après approval, deploy + smoke.

### Branch protection
12. Tentative `git push origin main` direct → REJETÉ (require PR).
13. PR sans CI green → "Merge" button DISABLED.

## Out of scope Phase 6 MVP

- **Frontend Vitest** : différé Phase 4 (frontend)
- **Android lint + unit** : différé Phase 4 (Android)
- **UI parity Compose↔WebView** : différé Phase 4
- **Codex pipeline (agents CI Anthropic SDK)** : différé Phase A++
- **Recurring security/deps audit workflows** : différé Phase 6+
- **Vault sync workflow** : déjà présent via pre-commit hook code-cartographer (pas besoin de CI)
- **Reverse proxy / TLS** : assumé géré sur le VPS (caddy/nginx + Let's Encrypt) — hors scope CI/CD
- **Backup PG** : hors scope, à documenter dans NOTES.md
- **Monitoring/alerting** : hors scope MVP, peut venir avec structlog → Loki/Grafana plus tard

## Suite naturelle

- **Phase 4 master plan** : Android Compose shell + WebView dashboard + tests unit/lint Android
- **Phase 6+ (extensions)** : Codex pipeline agents CI, recurring audit workflows, monitoring stack
