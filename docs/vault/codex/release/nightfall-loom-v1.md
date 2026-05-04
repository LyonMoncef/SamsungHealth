---
type: release-archive
title: Nightfall dataviz — état au moment du loom
project: SamsungHealth
created: 2026-04-23
status: archived
tags: [samsunghealth, codex, release, nightfall, dataviz, loom, archive]
git_tags:
  - nightfall-dataviz-v1
  - nightfall-loom-v1
  - checkpoint-before-v2-refactor-2026-04-23
git_commits:
  nightfall-dataviz-v1: 1631c80
  nightfall-loom-v1: ee09025
  checkpoint-before-v2-refactor-2026-04-23: 5343c9b
git_branch: feat/nightfall-history (closed on origin, preserved)
pr: "#6 (closed without merge, 2026-04-23)"
related_dailies:
  - 2026-04-19-daily.md
  - 2026-04-21-daily.md
  - 2026-04-22-daily.md
  - 2026-04-23-daily.md
related_plans:
  - 2026-04-23-plan-v2-refactor-master.md
  - 2026-04-23-plan-v2-multi-agents-architecture.md
---

# Nightfall dataviz — release archive (loom recording)

## Pourquoi cette fiche existe

PR #6 fermée sans merge le 2026-04-23 : pivot vers refonte V2 multi-user/RGPD/Compose. Cette fiche permet de retrouver précisément l'état de l'app au moment de l'enregistrement loom, sans devoir grep le repo ou se souvenir des hash.

## Comment ressortir l'état

```bash
# État dataviz complet (chapitres 01-11), pré-polish loom
git checkout nightfall-dataviz-v1

# État au moment de l'enregistrement loom (polish UX inclus)
git checkout nightfall-loom-v1

# État de main avant bifurcation v2-refactor
git checkout checkpoint-before-v2-refactor-2026-04-23
```

Pour relancer l'app sur un de ces états :
```bash
make dev   # FastAPI sur :8001
# ouvrir http://localhost:8001
```

## Tags expliqués

### `nightfall-dataviz-v1` → 1631c80 (2026-04-22)
Premier point stable. Tous les chapitres dataviz sont là, mais sans le polish UX de fin de cycle. Référence pour comparer l'évolution depuis le 22/04.

### `nightfall-loom-v1` → ee09025 (2026-04-23)
**État effectivement enregistré dans le loom**. Différences vs `nightfall-dataviz-v1` :
- Period filter bar (7j/30j/full) + radial sous l'hypnogram
- Suppression chapitre duration regularity (épuration)
- Stage tooltips riches sur timeline et radial clock
- Bed/wake times prominents au-dessus des KPIs hypnogram
- Mobile spacing épuré (560px) — app/panel padding réduits, cards-duel gap, times stackés vertical
- Perf : partial-render sur le donut + labels donut, period par défaut 6m

### `checkpoint-before-v2-refactor-2026-04-23` → 5343c9b (origin/main HEAD)
État de main avant bifurcation `v2-refactor`. Le commit `5343c9b feat(data): Samsung Health CSV import pipeline (#5)` est la dernière contribution mergée sur main avant la refonte.

## Inventaire dataviz (tels que dans nightfall-loom-v1)

| Chapitre | Description | Source |
|----------|-------------|--------|
| 01 ~~heatmap~~ | Retiré (cf. f818ae4) | — |
| 02 timeline | Stacked timeline avec depth overlay | `static/dashboard.js` |
| 03 hypnogram | Hypnogram + bed/wake prominents + KPIs | `static/dashboard.js` |
| 04 radial clock | Radial 3h tick labels + tooltips riches | `static/dashboard.js` |
| 05 ~~small multiples~~ | Retiré (cf. f818ae4) | — |
| 06 ~~ridgeline~~ | Retiré (cf. ef19349) | — |
| 07 cards | Best vs worst 2+2 duel + scatter bedtime 24h | `static/dashboard.js` |
| 08 ~~agenda~~ | Retiré (cf. feb13af) | — |
| 09 metrics | Sélecteur de mois + 4 vues full-history (scatter bedtime, dette, stages rolling, HR) | `static/dashboard.js` |
| 10 ~~elasticity~~ | Retiré (cf. bb8395c — décidé pendant l'épuration loom) | — |
| 11 drift clock | 3 vues radiales (densité, spaghetti, playback animé) + démo toggle | `static/dashboard.js` |
| Period filter | Bar 7j/30j/full + period 6m par défaut (loom polish) | `static/dashboard.js` |

## Fichiers clés du livrable

- `static/index.html` — shell dashboard
- `static/dashboard.js` — logique de render (~tous les chapitres)
- `static/dashboard.css` — styles
- `static/api.js` — fetch + transforms

## Branche de travail

`feat/nightfall-history` — head `ee09025`, ouverte le 22/04, fermée (PR closed) le 23/04 sans merge. Conservée sur origin pour redondance.

## Question typique à laquelle cette fiche répond

> "Tats : montre-moi l'état de l'app au moment où on a enregistré le loom"

Réponse : `git checkout nightfall-loom-v1`, puis `make dev`, puis http://localhost:8001 → tu retombes pile sur l'état enregistré.

## Suite

Le travail Nightfall est repris en **Phase 5** du master plan V2 (Compose Canvas natif), avec parité visuelle browser ↔ Compose enforced par `ui-parity-checker`. Le tag `nightfall-loom-v1` sert de référence visuelle pour cette parité (tolerance < 2% diff).
