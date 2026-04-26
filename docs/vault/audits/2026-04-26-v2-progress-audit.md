---
type: audit
title: "Audit V2 progress — état des plans"
created: 2026-04-26
scope: master plan V2 + plan-v2-multi-agents + plans Phase A
status: snapshot
related_plans:
  - 2026-04-23-plan-v2-refactor-master
  - 2026-04-23-plan-v2-multi-agents-architecture
  - 2026-04-23-plan-code-as-vault
  - 2026-04-23-plan-test-code-links
  - 2026-04-23-plan-specs-in-vault
tags: [audit, roadmap, v2, status]
---

# Audit V2 progress — 2026-04-26

> Snapshot état des plans V2 après V2.1, V2.1.1, V2.1.2, V2.2, V2.2.1 mergées (PRs #7 → #11).
> Date snapshot : 2026-04-26. Servir de base pour décider des prochaines étapes.

## 📋 Plans méta-projet (Phase A — outillage agents/vault)

| Plan | Status spec | Réalité |
|------|-------------|---------|
| `plan-code-as-vault` (cartographer) | approved | ✅ **livré** (Phase A.5, 11 commits) |
| `plan-test-code-links` (coverage map) | delivered | ✅ |
| `plan-specs-in-vault` (spec_indexer) | delivered | ✅ |
| `plan-v2-multi-agents-architecture` | approved | ✅ **livré** (8 subagents Pydantic + 7 skills + 13 deviation_types plan-keeper) |

## 🚧 Master plan V2 (7 phases, ~6-8 semaines cumulées)

### Phase 0 — Infra foundation (estimée 3-5j)

| Item | État |
|------|------|
| Postgres 16 + Alembic + UUID v7 | ✅ V2.1 mergée (PR #7) |
| Refactor 4 routers SQLAlchemy + suppression SQLite | ✅ V2.1.1 mergée (PR #8) |
| Refonte scripts CSV import SQLAlchemy | ✅ V2.1.2 mergée (PR #9) |
| **structlog observability** | ❌ pas commencé |
| **Caddy TLS reverse proxy + Let's Encrypt + mkcert dev** | ❌ pas commencé |
| **CLI `logq` (query JSONL logs)** | ❌ pas commencé |
| Async SQLAlchemy (`asyncpg`) | ⏸ différé hors scope V2.1 (sync conservé) |

### Phase 1 — Auth system (4-6j) → ce sera V2.3

| Item | État |
|------|------|
| Table `users` + `verification_tokens` + `refresh_tokens` | ❌ |
| Email + password (argon2id) | ❌ |
| Google OAuth | ❌ |
| Reset password flow | ❌ |
| JWT access (15min) + refresh (30j) | ❌ |
| Pattern `AuthProvider` interface (swap Keycloak futur) | ❌ |
| Rate limiting login | ❌ |
| 20+ tests TDD | ❌ |

### Phase 2 — Data layer + routers TDD (5-7j)

| Item | État |
|------|------|
| Routers async SQLAlchemy | ⏸ sync (différé) |
| **Chiffrement AES-256-GCM champs Art.9** | ✅ V2.2 (PR #10) + V2.2.1 (PR #11) — **37 colonnes / 10 tables** |
| `Depends(get_current_user)` partout | ❌ depends on Phase 1 |
| Multi-user (user_id FK sur tables santé) | ❌ depends on Phase 1 |
| Logging par scope | ❌ depends on structlog (P0) |
| 10+ tests/router | ⚠️ partiel (44 tests/server existants, mais pas org "10/router") |

### Phase 3 — RGPD endpoints (2-3j)

| Item | État |
|------|------|
| `GET /api/rgpd/export` (ZIP JSON-LD) | ❌ |
| `DELETE /api/rgpd/erase` (rotation clé = effacement crypto) | ❌ depends on V2.2 ✅ |
| Consent management | ❌ |
| Certificat destruction | ❌ |

### Phase 4 — Android shell + WebView (5-8j)

| Item | État |
|------|------|
| Compose shell (auth/import/settings/consent) | ❌ |
| Dashboard WebView en `assets/` bundled | ❌ |
| SAF folder picker + parser Samsung Health | ❌ |
| Health Connect sync avec JWT | ❌ depends on Phase 1 |
| Timber logging | ❌ |
| Build flavors `webview` + `native` | ❌ |

### Phase 5 — Compose Canvas natif dashboard (10-15j)

| Item | État |
|------|------|
| Hypnogram, radial, timeline, cards, metrics | ❌ |
| Tests parité Playwright vs Paparazzi (< 2% diff) | ❌ |
| Note : `nightfall-fullspectrum-design-brief.md` (status `ready`) pourrait nourrir cette phase | 📝 spec design dispo non-implémentée |

### Phase 6 — CI/CD multi-env (2-3j)

| Item | État |
|------|------|
| Workflow CI coverage (`.github/workflows/coverage.yml`) | ✅ partiel (généré en Phase A.7) |
| 7 jobs (backend lint/test/security, frontend Vitest, Android lint/unit, UI parity) | ❌ partiel (1/7) |
| Deploy auto dev | ❌ |
| Deploy manuel prod avec approval | ❌ |

## 📦 Specs hors plan V2

- `nightfall-sleep-dashboard` (delivered ✅) — pré-V2 (loom Nightfall)
- `samsung-health-csv-schema` (reference) — doc référence Samsung
- `nightfall-fullspectrum-design-brief` (ready, jamais impl) — design doc Phase 5

## 📊 Résumé chiffré

- **11 PRs mergées** (7-11 = V2.1 → V2.2.1, plus #5/#6 pré-V2)
- **235/235 tests GREEN**, 0 régression
- **3 phases V2 entamées** : P0 (~70% — manque structlog/Caddy/logq), P2 (~50% — chiffrement OK, manque multi-user/logging), Phase A méta 100%
- **4 phases V2 à 0%** : P1 auth, P3 RGPD, P4 Android, P5 Compose Canvas
- **Phase 6 CI à ~15%** (1 workflow sur 7)
- **Estimation reste** : ~25-35j de travail cumulé pour atteindre V2 complet

## 🎯 Pistes de continuation

- **A** — Continuer Phase 1 (auth) maintenant. P3 RGPD, P4 Android sync, multi-user P2 dépendent tous de l'auth → c'est le plus gros bloqueur.
- **B** — Boucler Phase 0 d'abord (structlog + Caddy + logq), faisable en ~1-2j, propre avant d'entamer le gros chantier auth.
- **C** — (clôturé par cet audit) Corriger l'anomalie V2.1.1 status `ready→delivered` + commit "audit".

## 🔧 Anomalies à corriger (option C)

- Spec `2026-04-24-v2-postgres-routers-cutover.md` : `status: ready` → doit passer `delivered: 2026-04-24` (PR #8 mergée le 2026-04-24).
