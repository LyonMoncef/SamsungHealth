---
type: spec-summary
slug: 2026-04-30-phase6-cicd-mvp
original_type: spec
status: delivered
source: ../../specs/2026-04-30-phase6-cicd-mvp
---

# Spec — 2026-04-30-phase6-cicd-mvp

Source : [[../../specs/2026-04-30-phase6-cicd-mvp]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/Dockerfile|Dockerfile]] — symbols: `builder`, `runner`
- [[../code/docker-compose.prod.yml|docker-compose.prod.yml]] — symbols: `web`, `postgres`
- [[../code/.github/workflows/deploy-dev.yml|.github/workflows/deploy-dev.yml]] — symbols: `build`, `deploy`
- [[../code/.github/workflows/deploy-prod.yml|.github/workflows/deploy-prod.yml]] — symbols: `deploy`
- [[../code/server/routers/health|server/routers/health.py]] — symbols: `router`, `healthz`, `readyz`
- [[../code/server/main|server/main.py]] — symbols: `app`
- [[../code/.env.prod.example|.env.prod.example]]

### Tests
- [[../code/tests/server/test_healthz|tests/server/test_healthz.py]] — classes: `TestHealthz`, `TestReadyz`
