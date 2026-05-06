---
type: spec
title: "Phase 6 MVP — CI/CD multi-env (VPS perso)"
slug: 2026-04-30-phase6-cicd-mvp
status: delivered
created: 2026-04-30
delivered: 2026-04-30
priority: high
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs:
  - 2026-04-28-phase3-rgpd-endpoints
implements:
  - file: Dockerfile
    symbols: [builder, runner]
  - file: docker-compose.prod.yml
    symbols: [web, postgres]
  - file: .github/workflows/deploy-dev.yml
    symbols: [build, deploy]
  - file: .github/workflows/deploy-prod.yml
    symbols: [deploy]
  - file: server/routers/health.py
    symbols: [router, healthz, readyz]
  - file: server/main.py
    symbols: [app]
  - file: .env.prod.example
    symbols: []
tested_by:
  - file: tests/server/test_healthz.py
    classes: [TestHealthz, TestReadyz]
tags: [phase6, cicd, deploy, vps, infra, samsunghealth, spec]
---

# Phase 6 MVP — CI/CD multi-env (VPS perso)

## Pentester verdict — patch complet

**Statut** : pentester reviewé 2026-04-30 → verdict `WARN` (5 HIGH bloquants + 5 décisions design + 11 risques additionnels). Patch reconcilié 2026-04-30 — tous les HIGH intégrés + D et risques significatifs.

| Pentester | Section spec |
|-----------|--------------|
| HIGH 1 — SSH key dev↔prod separation | §6 (2 clés `VPS_SSH_KEY_DEV` / `VPS_SSH_KEY_PROD` scopés par environment) |
| HIGH 2 — Mutable tag `dev-latest` en prod fallback | §5 (`inputs.sha` obligatoire avec regex validation, pas de fallback `:latest` en prod) |
| HIGH 3 — Migration alembic sans backup ni health-gate | §4/§5 (pg_dump pré-upgrade + check `alembic current == expected_previous_head` + downtime fenêtre explicite) |
| HIGH 4 — Smoke test fail sans rollback | §4/§5 (capture `IMAGE_TAG_PREV` + rollback auto si smoke fail) |
| HIGH 5 — Pas de SCA/secrets-scan dans CI | §9 nouvelle (pip-audit + gitleaks + hash-pinned requirements via pip-compile lock) |
| Décision D-1 — Pas de cosign/SLSA | Out of scope MVP, doc Phase 6+ |
| Décision D-2 — `accept-new` MITM TOFU | §4 (commit `.github/known_hosts` puis `StrictHostKeyChecking=yes`) |
| Décision D-3 — `/readyz` info-leak | §3 (recommandation reverse-proxy `/readyz` localhost-only, doc README VPS) |
| Décision D-4 — Step `docker build` required check | §7 (liste explicite des required checks main post-merge) |
| Décision D-5 — Sémantique `paths-ignore` | §4 (commentaire explicatif) |
| R-H1 — `.env.prod` chmod 600 + rotation | §6 (instructions VPS + ADR-3 rotation différée) |
| R-H2 — Backup pg | Out of scope MVP, **NOTES.md ADR-3** + checklist VPS |
| R-M1 — GHCR permissions par-job | §4 (`permissions: { packages: write }` UNIQUEMENT sur job `build`) |
| R-M2 — Smoke prod single-shot | §5 (loop comme dev) |
| R-M3 — Postgres `ports:` jamais exposé | §2 (commentaire explicatif) |
| R-M4 — SSH command injection `inputs.sha` | §5 (regex validate `^[a-f0-9]{7,40}$` avant export) |
| R-M5 — Stack trace email leak en logs | §10 nouvelle (workflow logs : redaction + reminder ne pas log payload alembic) |
| R-L5 — `curl` absent python:3.12-slim | §1 (healthcheck via `python -c "import urllib.request..."` au lieu de curl) |

**Action prochaine** : TDD RED via test-writer agent (tests `/healthz` + `/readyz`).

## Préconditions de déploiement (D-04 — bloquant PR)

**`.github/known_hosts` doit être pré-rempli AVANT le 1er run de `deploy-dev.yml`.**

### Risque de blocage opérationnel

Avec `StrictHostKeyChecking=yes` dans les workflows (D-2 — TOFU mitigation), le 1er déploiement échoue systématiquement si le VPS n'est pas dans `.github/known_hosts` :
```
ssh: Host key verification failed.
```
Sous pression (déploiement urgent), cette friction peut amener à désactiver `StrictHostKeyChecking`, recréant exactement le TOFU (`accept-new`) que D-2 cherche à éviter. **C'est inacceptable.**

### Procédure obligatoire (exécuter une fois par VPS)

1. **Sur chaque VPS** (`dev` et `prod` séparément), récupérer l'empreinte SSH :
   ```bash
   ssh-keyscan -H <VPS_HOST> >> ~/.ssh/known_hosts
   # Exemple : ssh-keyscan -H dev.samsunghealth.example
   ```

2. **Vérifier visuellement** que la ligne ajoutée est valide (format : `<host> <algo> <fingerprint>`) :
   ```bash
   tail -n 1 ~/.ssh/known_hosts
   ```

3. **Copier l'empreinte** dans le repo :
   ```bash
   cp ~/.ssh/known_hosts .github/known_hosts
   ```

4. **Committer dans une PR séparée** :
   ```bash
   git add .github/known_hosts
   git commit -m "chore(infra): add SSH known_hosts for dev + prod VPS"
   git push
   ```
   Cette PR n'active PAS les workflows deploy-dev/deploy-prod (elle ne touche que `.github/known_hosts` → paths-ignore les protège). Merger indépendamment.

5. **Merge au `main` AVANT de merger la PR Phase 6**.

### Bloquant : rule de merge

- [ ] PR Phase 6 **ne peut être mergée que si** `.github/known_hosts` contient déjà (au moins) les empreintes pour `VPS_DEV_HOST` et `VPS_PROD_HOST` (vérifier : `grep -c "dev\.samsunghealth\.example\|samsunghealth\.example" .github/known_hosts` ≥ 2).
- [ ] Si `.github/known_hosts` ne contient que des commentaires/lignes vides au moment de la PR review, **refuser le merge** et demander à exécuter la procédure ci-dessus.

### Lien avec D-2 (TOFU)

D-2 décide `StrictHostKeyChecking=yes` justement pour éviter le TOFU. Mais cette décision n'a de sens que si `.github/known_hosts` est PRÉ-rempli. Sans ça, c'est une fausse sécurité. **Désactiver `StrictHostKeyChecking` même en urgence est interdit** — faire un hotfix d'une minute pour pré-remplir known_hosts plutôt que perdre des heures à debugger une compromise supply-chain.

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
COPY requirements.txt requirements.lock .
# --require-hashes : pip refuse d'installer si le hash ne correspond pas (HIGH 5)
RUN uv pip install --system --no-cache --require-hashes -r requirements.lock

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
# python urllib (pas de curl dans python:3.12-slim — R-L5)
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD python -c "import urllib.request, sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8001/healthz', timeout=3).status == 200 else 1)"
CMD ["uvicorn", "server.main:app", "--host", "0.0.0.0", "--port", "8001"]
```

Image taille cible : ~150 MB (slim + no-cache). `requirements.lock` généré via `pip-compile --generate-hashes requirements.in` (HIGH 5 — supply chain integrity).

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
    # ⚠️ JAMAIS de section `ports:` sur postgres (R-M3) — accès uniquement
    # via le réseau Docker interne, pas exposé sur l'IP publique du VPS.
    restart: unless-stopped
  web:
    image: ghcr.io/lyonmoncef/samsunghealth:${IMAGE_TAG:?IMAGE_TAG must be a SHA tag from .env.prod}
    env_file: .env.prod
    depends_on:
      postgres:
        condition: service_healthy
    ports: ["8001:8001"]
    restart: unless-stopped
    healthcheck:
      # urllib stdlib — pas de curl dans python:3.12-slim (R-L5)
      test:
        - "CMD"
        - "python"
        - "-c"
        - "import urllib.request, sys; sys.exit(0 if urllib.request.urlopen('http://localhost:8001/healthz', timeout=3).status == 200 else 1)"
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

**`/readyz` info-leak (D-3)** : la response 503 expose `reason: "alembic_mismatch"` qui peut indiquer qu'une migration tourne (info-leak modéré : version DB inferable). **Recommandation reverse-proxy** : exposer `/healthz` publiquement (LB GitHub Actions smoke), restreindre `/readyz` à `127.0.0.1` uniquement (interne docker / monitoring local). Exemple Caddy :
```
samsunghealth.example {
    @internal {
        path /readyz
        not remote_ip private_ranges 127.0.0.1
    }
    handle @internal { respond 404 }
    reverse_proxy localhost:8001
}
```
Documenté dans `README.md` section déploiement.

### 4. Workflow `deploy-dev.yml` — auto sur push main

**Note `paths-ignore` (D-5)** : un push qui touche À LA FOIS un `.md` ET un `.py` déclenche le workflow (paths-ignore = "skip si TOUS les fichiers matchent"). Sémantique GitHub Actions confirmée. Donc commit mixte = workflow tourne (safe).

```yaml
on:
  push:
    branches: [main]
    paths-ignore: ["**/*.md", ".claude/**", "docs/vault/**"]

jobs:
  build:
    runs-on: ubuntu-latest
    permissions: { packages: write, contents: read }
    outputs:
      image_tag: ${{ steps.meta.outputs.tag }}
    steps:
      - uses: actions/checkout@v4
      - id: meta
        run: echo "tag=dev-${{ github.sha }}" >> "$GITHUB_OUTPUT"
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v5
        with:
          push: true
          tags: ghcr.io/lyonmoncef/samsunghealth:${{ steps.meta.outputs.tag }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: dev
    permissions: { contents: read }
    steps:
      - uses: actions/checkout@v4
      - uses: webfactory/ssh-agent@v0.9.0
        with:
          ssh-private-key: ${{ secrets.VPS_SSH_KEY_DEV }}
      
      - name: Trust known_hosts (D-2)
        run: |
          mkdir -p ~/.ssh
          cp .github/known_hosts ~/.ssh/known_hosts
          chmod 600 ~/.ssh/known_hosts
      
      - name: Capture previous image tag (HIGH 4)
        id: prev
        run: |
          PREV=$(ssh -o StrictHostKeyChecking=yes "${{ secrets.VPS_DEV_HOST }}" \
            'cd /srv/samsunghealth-dev && grep -E "^IMAGE_TAG=" .env.prod | cut -d= -f2 || echo "none"')
          echo "tag=$PREV" >> "$GITHUB_OUTPUT"
      
      - name: Backup & deploy (HIGH 3)
        run: |
          ssh -o StrictHostKeyChecking=yes "${{ secrets.VPS_DEV_HOST }}" bash << EOF
          set -euo pipefail
          cd /srv/samsunghealth-dev
          mkdir -p backups
          BACKUP=backups/pre-upgrade-\$(date -u +%Y%m%dT%H%M%SZ).sql.gz
          docker compose -f docker-compose.prod.yml exec -T postgres \
            pg_dump -U "\$POSTGRES_USER" "\$POSTGRES_DB" | gzip > "\$BACKUP"
          
          export IMAGE_TAG="${{ needs.build.outputs.image_tag }}"
          docker compose -f docker-compose.prod.yml pull web
          CURRENT=\$(docker compose -f docker-compose.prod.yml --env-file .env.prod exec -T web alembic current 2>/dev/null | awk 'NR==1{print \$1}')
          EXPECTED=\$(docker compose -f docker-compose.prod.yml --env-file .env.prod run --rm web alembic heads 2>/dev/null | awk 'NR==1{print \$1}')
          if [ "\$CURRENT" != "\$EXPECTED" ] && [ "\$CURRENT" != "(none)" ]; then
            echo "::error::Alembic drift — current=\$CURRENT expected=\$EXPECTED"
            exit 1
          fi
          docker compose -f docker-compose.prod.yml --env-file .env.prod run --rm web alembic upgrade head
          docker compose -f docker-compose.prod.yml --env-file .env.prod up -d web
          sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=\$IMAGE_TAG/" .env.prod
          EOF
      
      - name: Smoke test (loop, 60s max)
        id: smoke
        run: |
          for i in {1..30}; do
            if curl -fs https://${{ secrets.VPS_DEV_DOMAIN }}/healthz; then exit 0; fi
            sleep 2
          done
          exit 1
      
      - name: Auto-rollback on failure (HIGH 4)
        if: failure() && steps.smoke.outcome == 'failure'
        run: |
          set -euo pipefail
          PREV="${{ steps.prev.outputs.tag }}"
          if [ "$PREV" = "none" ] || [ -z "$PREV" ]; then
            echo "::error::no previous tag — manual recovery required"
            exit 1
          fi
          if ! [[ "$PREV" =~ ^dev-[a-f0-9]{7,40}$ ]]; then
            echo "::error::Suspicious PREV tag detected — rollback aborted"
            exit 1
          fi
          ssh -o StrictHostKeyChecking=yes "${{ secrets.VPS_DEV_HOST }}" bash << EOF
          set -euo pipefail
          cd /srv/samsunghealth-dev
          export IMAGE_TAG="$PREV"
          sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$PREV/" .env.prod
          docker compose -f docker-compose.prod.yml --env-file .env.prod up -d web
          EOF
          exit 1
```

Voir `.github/workflows/deploy-dev.yml` pour l'implémentation complète et canonique.

### 5. Workflow `deploy-prod.yml` — manuel + approval

```yaml
on:
  workflow_dispatch:
    inputs:
      sha:
        description: "Commit SHA to deploy (7+ hex chars, must exist as dev-<sha> in GHCR)"
        required: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    permissions: { contents: read }
    steps:
      - name: Validate SHA input (R-M4)
        env:
          SHA_INPUT: ${{ github.event.inputs.sha }}
        run: |
          SHA="$SHA_INPUT"
          if ! [[ "$SHA" =~ ^[a-f0-9]{7,40}$ ]]; then
            echo "::error::Invalid SHA format: $SHA"
            exit 1
          fi
      
      - uses: actions/checkout@v4
      - uses: webfactory/ssh-agent@v0.9.0
        with:
          ssh-private-key: ${{ secrets.VPS_SSH_KEY_PROD }}
      
      - name: Trust known_hosts (D-2)
        run: |
          mkdir -p ~/.ssh
          cp .github/known_hosts ~/.ssh/known_hosts
          chmod 600 ~/.ssh/known_hosts
      
      - name: Capture previous image tag (HIGH 4)
        id: prev
        run: |
          PREV=$(ssh -o StrictHostKeyChecking=yes "${{ secrets.VPS_PROD_HOST }}" \
            'cd /srv/samsunghealth-prod && grep -E "^IMAGE_TAG=" .env.prod | cut -d= -f2 || echo "none"')
          echo "tag=$PREV" >> "$GITHUB_OUTPUT"
      
      - name: Backup & deploy (HIGH 3)
        env:
          SHA_INPUT: ${{ github.event.inputs.sha }}
        run: |
          TAG="dev-$SHA_INPUT"
          ssh -o StrictHostKeyChecking=yes "${{ secrets.VPS_PROD_HOST }}" bash << EOF
          set -euo pipefail
          cd /srv/samsunghealth-prod
          mkdir -p backups
          BACKUP=backups/pre-upgrade-\$(date -u +%Y%m%dT%H%M%SZ).sql.gz
          docker compose -f docker-compose.prod.yml exec -T postgres \
            pg_dump -U "\$POSTGRES_USER" "\$POSTGRES_DB" | gzip > "\$BACKUP"
          
          export IMAGE_TAG="$TAG"
          docker compose -f docker-compose.prod.yml pull web
          CURRENT=\$(docker compose -f docker-compose.prod.yml exec -T web alembic current 2>/dev/null | awk 'NR==1{print \$1}')
          EXPECTED=\$(docker compose -f docker-compose.prod.yml run --rm web alembic heads 2>/dev/null | awk 'NR==1{print \$1}')
          if [ "\$CURRENT" != "\$EXPECTED" ] && [ "\$CURRENT" != "(none)" ]; then
            echo "::error::Alembic drift — current=\$CURRENT expected=\$EXPECTED"
            exit 1
          fi
          docker compose -f docker-compose.prod.yml run --rm web alembic upgrade head
          docker compose -f docker-compose.prod.yml up -d web
          sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$TAG/" .env.prod
          EOF
      
      - name: Smoke test (loop — R-M2)
        id: smoke
        run: |
          for i in {1..30}; do
            if curl -fs https://${{ secrets.VPS_PROD_DOMAIN }}/healthz; then exit 0; fi
            sleep 2
          done
          exit 1
      
      - name: Auto-rollback on failure (HIGH 4)
        if: failure() && steps.smoke.outcome == 'failure'
        run: |
          PREV="${{ steps.prev.outputs.tag }}"
          if [ "$PREV" = "none" ] || [ -z "$PREV" ]; then
            echo "::error::no previous tag — manual recovery required"
            exit 1
          fi
          if ! [[ "$PREV" =~ ^dev-[a-f0-9]{7,40}$ ]]; then
            echo "::error::Suspicious PREV tag detected — rollback aborted"
            exit 1
          fi
          ssh -o StrictHostKeyChecking=yes "${{ secrets.VPS_PROD_HOST }}" bash << EOF
          set -euo pipefail
          cd /srv/samsunghealth-prod
          export IMAGE_TAG="$PREV"
          docker compose -f docker-compose.prod.yml pull web
          docker compose -f docker-compose.prod.yml up -d web
          sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=$PREV/" .env.prod
          EOF
          echo "::error::rolled back prod to $PREV — manual investigation required"
          exit 1
```

Voir `.github/workflows/deploy-prod.yml` pour l'implémentation complète et canonique.

### 6. Secrets GitHub à configurer

**HIGH 1 — 2 clés SSH distinctes scopées par environment** (compromission d'une = pas d'accès à l'autre, préserve l'intérêt du gate prod approval) :

| Secret | Scope | Contenu |
|--------|-------|---------|
| `VPS_SSH_KEY_DEV` | env `dev` | Clé privée Ed25519 dédiée CI dev — autorisée UNIQUEMENT sur VPS dev |
| `VPS_SSH_KEY_PROD` | env `production` | Clé privée Ed25519 dédiée CI prod — autorisée UNIQUEMENT sur VPS prod |
| `VPS_DEV_HOST` | env `dev` | `user@dev.samsunghealth.example` |
| `VPS_DEV_DOMAIN` | env `dev` | `dev.samsunghealth.example` |
| `VPS_PROD_HOST` | env `production` | `user@samsunghealth.example` |
| `VPS_PROD_DOMAIN` | env `production` | `samsunghealth.example` |

Pas de DATABASE_URL/SAMSUNGHEALTH_* en GitHub Secrets : ils vivent dans `.env.prod` sur chaque VPS, jamais en CI.

**`.env.prod` sur VPS — durcissement (R-H1)** :
```bash
# sur chaque VPS, après config initiale
chmod 600 /srv/samsunghealth-{dev,prod}/.env.prod
chown app:app /srv/samsunghealth-{dev,prod}/.env.prod
# vérifier qu'aucun autre user n'a accès
ls -la /srv/samsunghealth-{dev,prod}/.env.prod
```
Génération de `SAMSUNGHEALTH_ENCRYPTION_KEY` :
```bash
python -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```
**Rotation** : différée Phase 6+ — ADR-3 (NOTES.md) à terme via SOPS/age. MVP : si compromis, opération manuelle (re-encrypt tables Art.9 V2.2.1 avec nouvelle clé).

### 7. Branch protection `main`

Activée via `gh api`. Règles :
- Require status checks before merge : `Tests`, `Lint`, `Coverage` (ci.yml existant) **+ `Docker build`, `Security gates` (D-4 — nouveaux jobs §9)**. Liste explicite des required checks ci-dessous.
- Require linear history (rebase/squash only)
- Disallow force-push
- Disallow direct push
- Pas de "require N reviews" (solo dev) — peut être ajouté plus tard

**Required status checks** (à configurer via `gh api repos/.../branches/main/protection`) :
- `Tests` (de `ci.yml`)
- `Lint` (de `ci.yml`)
- `Coverage` (de `coverage.yml`)
- `Docker build` (de `ci.yml` augmenté §9)
- `Security gates` (pip-audit + gitleaks, §9)

Commande type :
```bash
gh api -X PUT repos/LyonMoncef/SamsungHealth/branches/main/protection \
  --field required_status_checks='{"strict":true,"contexts":["Tests","Lint","Coverage","Docker build","Security gates"]}' \
  --field enforce_admins=false \
  --field required_pull_request_reviews=null \
  --field restrictions=null \
  --field required_linear_history=true \
  --field allow_force_pushes=false \
  --field allow_deletions=false
```

### 9. CI security gates (HIGH 5)

Jobs ajoutés à `.github/workflows/ci.yml` (run sur chaque PR) :

```yaml
  security:
    name: Security gates
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: { python-version: "3.12" }
      - name: pip-audit (SCA — CVE check Python deps)
        run: |
          pip install pip-audit
          pip-audit --requirement requirements.lock --strict
      - name: gitleaks (secrets scan)
        uses: gitleaks/gitleaks-action@v2
        env: { GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }} }

  docker-build:
    name: Docker build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - name: Build (no push) — validate Dockerfile + lock
        uses: docker/build-push-action@v5
        with:
          push: false
          load: true
          tags: samsunghealth:ci-${{ github.sha }}
      - name: Smoke run container
        run: |
          set -euo pipefail
          # Ephemeral throwaway secrets for smoke test (NOT real credentials)
          # NOTE: base64.b64encode (NOT urlsafe_b64encode) — validator app does b64decode(validate=True)
          # which rejects '-' and '_'. Similarly, SALT must be token_hex(32), not token_urlsafe(16).
          ENC_KEY=$(python -c "import secrets,base64;print(base64.b64encode(secrets.token_bytes(32)).decode())")
          JWT=$(python -c "import secrets;print(secrets.token_urlsafe(48))")
          SALT=$(python -c "import secrets;print(secrets.token_hex(32))")
          docker run -d --name smoke -p 8001:8001 \
            -e DATABASE_URL=sqlite:////tmp/test.db \
            -e SAMSUNGHEALTH_ENCRYPTION_KEY="$ENC_KEY" \
            -e SAMSUNGHEALTH_JWT_SECRET="$JWT" \
            -e SAMSUNGHEALTH_EMAIL_HASH_SALT="$SALT" \
            -e SAMSUNGHEALTH_PUBLIC_BASE_URL="http://localhost:8001" \
            samsunghealth:ci-${{ github.sha }}
          for i in {1..15}; do
            if curl -fs http://localhost:8001/healthz; then exit 0; fi
            sleep 1
          done
          docker logs smoke
          exit 1
```

Hash-pinning : `requirements.lock` généré via `pip-compile --generate-hashes requirements.in`. Process documenté dans `README.md` : à régénérer à chaque update de `requirements.in`.

### 10. Workflow logs — protection PII (R-M5 RGPD)

Les stack traces alembic/uvicorn peuvent contenir `email_hash` ou rows JSON serialized. Les logs GitHub Actions sont privés (visible uniquement à des users avec read sur le repo) mais le risque RGPD demeure si quelqu'un fork ou si l'historique est exporté.

**Mitigations** :
- Workflows utilisent `set +x` par défaut (jamais `set -x` qui afficherait les variables expandées)
- Aucun `echo "$VAR"` sur secrets (déjà mute par `::add-mask::` GitHub natif)
- Migrations alembic : si une exception remonte, capter dans `step` avec `|| true` puis failer explicitement avec message court (sans stack trace) — pattern déjà appliqué Phase 3
- `structlog` redaction V2.3.x déjà active (PII scrubber sur logs runtime — réutilisé dans le container)
- Doc `NOTES.md` (ADR-3) : "Si une migration crash en CI, télécharger logs locaux pour diag, ne PAS coller dans une issue publique"

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
- [ ] `server/routers/health.py` (NEW) : 2 endpoints `/healthz`, `/readyz` + Pydantic (response models `HealthOut`, `ReadyOut`)
- [ ] `server/main.py` : `app.include_router(health.router)` + helper alembic head check pour `/readyz`
- [ ] `Dockerfile` (NEW) : multi-stage builder + runner non-root + HEALTHCHECK via python urllib (R-L5)
- [ ] `docker-compose.prod.yml` (NEW) : web + postgres (sans `ports:`) + healthcheck
- [ ] `.dockerignore` (NEW) : exclure `tests/`, `.git`, `docs/`, `.claude/`, `*.md`, `.env*`, `__pycache__`, `*.pyc`
- [ ] `.env.prod.example` (NEW) : template var prod (POSTGRES_*, DATABASE_URL, IMAGE_TAG, SAMSUNGHEALTH_*)
- [ ] `requirements.in` (NEW) + `requirements.lock` (NEW) : pip-compile hashes (HIGH 5)

### Workflows
- [ ] `.github/workflows/deploy-dev.yml` (NEW) : build GHCR + capture prev tag + pg_dump + alembic upgrade + smoke loop + auto-rollback
- [ ] `.github/workflows/deploy-prod.yml` (NEW) : workflow_dispatch SHA required + regex validate + env production approval + même flow + auto-rollback
- [ ] `.github/workflows/ci.yml` : ajouter jobs `Security gates` (pip-audit + gitleaks) et `Docker build` (HIGH 5 + D-4)
- [ ] `.github/known_hosts` (NEW) : SSH fingerprints VPS dev + prod (D-2 — pas de TOFU)

### Tests
- [ ] `tests/server/test_healthz.py` (~5 tests : `/healthz` 200 sans auth, `/readyz` 200 si DB ping + alembic head OK, `/readyz` 503 si DB down [mock])

### Docs
- [ ] `README.md` : section "Déploiement" (workflow VPS + bascule local + Caddy snippet `/readyz` localhost-only D-3 + commande `pip-compile` régénération lock)
- [ ] `NOTES.md` : ADR-3 "Pourquoi VPS perso vs PaaS + checklist hardening (chmod .env.prod, backups, fail2ban, SSH password disabled, rotation clé maître différée)"
- [ ] `HISTORY.md` : entry post-merge

### GitHub config (manuel post-merge)
- [ ] Créer environments `dev` (no protection, secrets : `VPS_SSH_KEY_DEV`, `VPS_DEV_HOST`, `VPS_DEV_DOMAIN`) et `production` (required reviewers = self, wait timer 0, secrets : `VPS_SSH_KEY_PROD`, `VPS_PROD_HOST`, `VPS_PROD_DOMAIN`)
- [ ] Aucun secret en repo-level (HIGH 1)
- [ ] Activer branch protection main avec required checks `Tests`, `Lint`, `Coverage`, `Docker build`, `Security gates` (commande dans README)

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
8. Push sur branche `feat/phase6-cicd-mvp` → `ci.yml` PASSE : `Tests`, `Lint`, `Coverage`, `Docker build`, `Security gates` (pip-audit + gitleaks).
9. Push main (post-merge) → `deploy-dev.yml` auto : build GHCR `dev-<sha>` (pas de `:latest` mutable) + capture prev tag + pg_dump + alembic upgrade + smoke loop.
10. Smoke test `deploy-dev.yml` fail (mock /healthz down) → auto-rollback vers `prev tag` + workflow exit 1.
11. Trigger `deploy-prod.yml` sans `inputs.sha` → workflow REJECTED (required input).
12. Trigger `deploy-prod.yml` avec `sha=zzz` (invalide) → step "Validate SHA" exit 1.
13. Trigger `deploy-prod.yml` avec sha valide → wait approval (GitHub UI environment `production`) → deploy + smoke + auto-rollback si fail.

### Sécurité supply chain
14. `pip install -r requirements.lock --require-hashes` dans Dockerfile passe ; alteration manuelle d'un hash dans `.lock` → build FAIL.
15. `gitleaks` détecte un secret commit accidental (ex: AWS key dans test fixture) → CI fail.
16. Secret leaked en push → 1 PR de revert + rotation immediate.

### Branch protection
17. Tentative `git push origin main` direct → REJETÉ (require PR).
18. PR sans CI green (1 des 5 required checks rouge) → "Merge" button DISABLED.

### Hardening VPS
19. `ls -la /srv/samsunghealth-prod/.env.prod` → `-rw------- 1 app app` (chmod 600, owner app).
20. `nmap` sur VPS public IP → port postgres 5432 NON ouvert (réseau docker interne uniquement).
21. SSH config VPS : `PasswordAuthentication no`, `PermitRootLogin no`, `fail2ban` actif (vérifier `systemctl status fail2ban`).

## Out of scope Phase 6 MVP

- **Split runtime/dev deps** (`requirements-dev.in` séparé de `requirements.in`) — différé vers `chore/split-deps`. Conséquence MVP : image prod inclut `tree-sitter*`, `pytest-cov`, `coverage`, `testcontainers[postgres]` (~XX MB extra, surface SCA élargie). Acceptable MVP perso (1 utilisateur, VPS perso). À corriger avant toute mise en prod réelle / multi-utilisateurs. Tracké via `chore/split-deps`. Au passage, supprimer le `requirements.txt` orphelin à la racine (vestige pré-migration vers `requirements.in`, non utilisé par Dockerfile mais COPY-é encore).
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
