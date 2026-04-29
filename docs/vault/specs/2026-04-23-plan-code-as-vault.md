---
title: Plan — Code-as-vault avec annotations ancrées
type: plan
project: SamsungHealth
phase: A.5
status: approved
created: 2026-04-23
updated: 2026-04-23
branch: refactor/phase-a-foundation-agents
tags: [samsunghealth, plan, code-as-vault, obsidian, annotations, tree-sitter, refactor, agents]
related_plans:
  - 2026-04-23-plan-v2-refactor-master.md
  - 2026-04-23-plan-v2-multi-agents-architecture.md
vault_links:
  claude_plan_file: /home/tats/.claude/plans/expressive-napping-blum.md
  parent_plan: 2026-04-23-plan-v2-multi-agents-architecture.md
---

# Plan — Code-as-vault avec annotations ancrées

> Plan approuvé le 2026-04-23. Source canonique : `/home/tats/.claude/plans/expressive-napping-blum.md`.
> Ce fichier est la copie Obsidian de référence pour navigation via graphe et backlinks.

## Vision

Chaque fichier source du repo a une **note Obsidian miroir** dans `docs/vault/code/` qui contient le **code complet à l'identique** + des **annotations** (texte rich, images, wikilinks, refs) **interleaved** au bon endroit. Les annotations sont ancrées via marqueurs explicites dans le code source (`# @vault:slug`) et stockées comme fichiers .md séparés dans `docs/vault/annotations/`.

## Use cases

- "Suite issue #142, on a remplacé l'héritage par tel module pour éviter side-effect X"
- "Algo tricky d'interpolation drift clock : référence Wikipedia + diagramme du raisonnement"
- "Cette fonction est lente exprès — la version optimisée nous a coûté un bug RGPD en pré-prod"
- "TODO post-V2 : refactor en streaming — voir [[../changelog/2026-04-22-cc50998]]"

## Décisions arbitrées

| Sujet | Choix | Raison |
|-------|-------|--------|
| **Vault location** | `docs/vault/` dans le repo | Versionné avec le code, simple, navigable Obsidian |
| **Couverture langages** | Python + JS + Kotlin dès Phase A.5 | Tree-sitter universel, couverture complète immédiate |
| **Ancrage annotations** | Marqueurs explicites `# @vault:slug` dans le code | Bullet-proof vs heuristics fragiles ; supporte tous scopes (single line / range / non-contigu / cross-file) |
| **IDs annotations** | Slugs descriptifs kebab-case (`@vault:sleep-perf-cap`) | Lisibles, évocateurs, pattern régex `[a-z0-9][a-z0-9-]{2,40}` |
| **Sync direction** | Code+annotations → note vault rendue | Code = source de vérité, vault jamais → code |
| **Note rendue** | Code complet à l'identique + callouts annotations interleaved + appendix symbols | Permet annotation arbitraire de n'importe quel bloc |

## Marqueurs par langage

| Langage | Single line | Range begin/end |
|---------|-------------|-----------------|
| Python | `# @vault:slug` | `# @vault:slug begin` ... `# @vault:slug end` |
| JavaScript | `// @vault:slug` | `// @vault:slug begin` ... `// @vault:slug end` |
| Kotlin | `// @vault:slug` | `// @vault:slug begin` ... `// @vault:slug end` |
| HTML | `<!-- @vault:slug -->` | `<!-- @vault:slug begin -->` ... `<!-- @vault:slug end -->` |
| CSS | `/* @vault:slug */` | `/* @vault:slug begin */` ... `/* @vault:slug end */` |

**4 scopes supportés** : 1 ligne, range begin/end, non-contigu (même slug ×N dans le fichier), cross-file (même slug dans plusieurs fichiers → annotation file dans `_cross/`).

## Cycle de vie d'une annotation

1. **Création** — humain via `/annotate <slug>` OU agent `annotation-suggester` (auto sur diff)
2. **Resync au commit** — `code-cartographer --diff` détecte marqueurs, recalcule positions, update `last_verified`
3. **Détection orphan** — marqueur perdu → status `orphan`, déplacé `_orphans/`, callout warning visible
4. **Résolution** — manuel (remettre marqueur) ou skill `/anchor-review` (propose candidats fuzzy match)
5. **Suppression** — `/annotate delete <slug>` retire annotation file + marqueurs

## Architecture sync

```
source code (markers)  +  annotations/<slug>.md  →  code-cartographer  →  vault/code/<file>.md (rendu)
```

Triggers :
- **Pre-commit hook** `.githooks/pre-commit` → mode `--diff`
- **Skill manuel** `/sync-vault` → mode `--full`
- **Skill** `/annotate` → CRUD annotations + injection marqueurs

## Agents

| Agent | Rôle | Phase |
|-------|------|-------|
| `code-cartographer` | Régénère notes vault depuis (code + annotations) ; détecte orphans | A.5 |
| `annotation-suggester` | Propose annotations sur diffs récents (lien issue, complexité, refactor) | A.6 |
| `anchor-rescuer` | Tente re-anchor auto orphans simples | A.7 (optionnel) |
| `log-cartographer` | Ingère JSONL → notes `logs/` avec cross-link code ↔ logs | B+ (post-structlog) |

## Skills

| Skill | Rôle | Next default |
|-------|------|--------------|
| `/sync-vault [--full\|--diff\|--check]` | Régénère notes vault | `/commit` |
| `/annotate <slug> [--at <file:line>\|--range <file:start-end>]` | Crée annotation + injecte marqueur | `/sync-vault --diff` |
| `/annotate edit\|delete <slug>` | CRUD annotations | `/sync-vault --diff` |
| `/anchor-review [<slug>]` | Résout orphans | `/commit` |

## Phasing

### Phase A.5 — Cartographer foundation (4-5 jours)

**Bloque P0 master ? Non** — peut démarrer en parallèle des hooks Phase A étapes 3-7.

11 livrables : setup vault structure, install tree-sitter, contrats Pydantic + tests TDD, modules core (markers/walker/IO/injector), modules render (anchor_resolver/note_renderer/orphan_detector/index_generator), CLI + bootstrap initial, subagent + skills, hook pre-commit, changelog initial generator, plan-keeper extension (3 deviation_types vault), patch master + multi-agents plans.

### Phase A.6 — Annotation suggester (2-3 jours, après A.5)

Subagent + hook post-commit + heuristics (commit refs issue, complexité diff, mots-clés workaround/perf/rgpd/fix).

### Phase A.7 — Anchor rescuer (1 jour, optionnel)

Re-anchor auto orphans simples si content similarity > 95%.

### Phase B+ — Logs en vault (1-2 jours, après P0 master + structlog)

Logs JSONL → notes `docs/vault/logs/` + cross-link bidirectionnel code ↔ logs via champ `symbol` Python qualifié.

## Plan-keeper extension

Étendre `DeviationType` avec :
- `vault_orphan_annotation` — annotation orpheline non résolue
- `vault_missing_note` — fichier source sans note vault correspondante
- `vault_outdated` — note vault dont `git_blob` ne matche pas le code actuel

## Vérification end-to-end

16 critères listés dans le plan source. Highlights :
1. Tree-sitter installé et importable Python+JS+Kotlin
2. Bootstrap `--full` génère ~34 notes vault
3. Création annotation injecte marqueur correctement par langage
4. Pre-commit hook resync au commit
5. Détection orphan + résolution via `/anchor-review`
6. Performance : full < 5 sec, diff < 1 sec
7. Tests Pydantic GREEN

## Risques principaux

- Tree-sitter ~30 MB (acceptable, Kotlin parser plus fragile)
- Race condition IDE editing pendant injection marqueur → CLI bloque si dirty tree
- Conflit slug sur 2 branches parallèles → validation au commit + suggestion suffix
- Vault régénéré pollue diffs PR → `.gitattributes linguist-generated`
