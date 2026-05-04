---
type: spec
title: "Specs in vault — bidirectional spec ↔ code ↔ tests linking"
slug: 2026-04-23-plan-specs-in-vault
phase: A.8
status: delivered
priority: medium
created: 2026-04-23
delivered: 2026-04-23
related_plans:
  - 2026-04-23-plan-v2-refactor-master
  - 2026-04-23-plan-v2-multi-agents-architecture
  - 2026-04-23-plan-test-code-links
implements:
  - file: agents/contracts/spec.py
    symbols: [SpecMeta, SpecImplements, SpecTestedBy, SpecType, SpecStatus]
  - file: agents/cartographer/spec_indexer.py
    symbols: [load_spec, build_index, discover_spec_paths, detect_implements_drift, untested_specs]
  - file: agents/cartographer/note_renderer.py
    symbols: [_render_appendix, _render_validates_section, _render_spec_targets]
  - file: agents/cartographer/index_generator.py
    symbols: [generate_specs_index]
  - file: agents/cartographer/cli.py
    symbols: [_load_spec_index, _render_spec_notes]
  - file: .claude/skills/spec/SKILL.md
  - file: .claude/agents/plan-keeper.md
  - file: agents/contracts/plan_keeper.py
    symbols: [DeviationType]
tested_by:
  - file: tests/agents/test_spec_indexer.py
    classes: [TestLoadSpec, TestBuildIndex, TestDriftDetection]
  - file: tests/agents/test_note_renderer.py
    classes: [TestCoverageIntegration]
    methods: [test_appendix_lists_implementing_specs, test_test_file_validates_section, test_spec_note_targets_section]
  - file: tests/agents/test_index_generator.py
    classes: [TestGenerateSpecsIndex]
  - file: tests/agents/test_contracts.py
    classes: [TestSpec, TestPlanKeeper, TestPackageReExports]
git_commits:
  - ac24832  # Bloc 1 : migration PKM → repo
  - 14a129a  # Bloc 2 : SpecMeta + spec_indexer
  - 728ea5d  # Bloc 3 : renderer + index integration
  - 1b82456  # Bloc 4 : plan-keeper +2 deviations
  - 52c5fc9  # Bloc 5 : skill /spec
tags: [phase-a.8, code-as-vault, specs, bidirectional-links]
---

# Phase A.8 — Specs in vault

## Vision

Specs deviennent first-class citizens du repo SamsungHealth. Discipline spec-first : avant chaque feature, écriture de la spec (slug + frontmatter + body) ; tests RED first ; impl ; mapping bidirectionnel spec ↔ code ↔ tests visible dans Obsidian.

## Décisions techniques

- **Specs versionnées dans le repo** (`docs/vault/specs/`) — single source of truth, mirror Windows existant les distribue à Obsidian. Stubs PKM avec wikilinks vers le repo.
- **Top-down** : spec déclare son `tested_by:` (pas de marker dans tests). Test orphelin = OK ; spec sans test = `untested_spec` deviation.
- **Granularité** : 1 spec ≈ 1 US/feature livrable < 1 semaine. Si > 1 semaine = `plan` qui se décompose en N specs enfants.
- **Vocabulaire** : `plan` (méta-architecture multi-semaines), `spec` (unitaire), `us` (user story), `feature` (groupe US), `stub` (placeholder PKM).
- **Drift detection** : `spec_implements_drift` (file/symbol disparu) + `untested_spec` (spec sans `tested_by:`).

## Livrables

### Bloc 1 — Migration specs PKM → repo
- 7 specs copiées vers `docs/vault/specs/`
- Frontmatter `type:` (spec|plan|stub|reference) ajouté
- Stubs PKM remplacés par redirections wikilink

### Bloc 2 — Contrat Pydantic + spec_indexer
- `agents/contracts/spec.py` — `SpecMeta`, `SpecImplements`, `SpecTestedBy`, `SpecType`, `SpecStatus`
- `agents/cartographer/spec_indexer.py` — `load_spec`, `build_index`, `discover_spec_paths`, `detect_implements_drift`, `untested_specs`
- 8 tests RED→GREEN

### Bloc 3 — Renderer + index integration
- `note_renderer` :
  - Code note → section "Implements specs" (file → specs)
  - Code note Symbols : "Specs:" annotation par symbol matching
  - Test note → section "Validates specs" (test → specs)
  - Spec note → section "Targets" (auto Implementation + Tests rendus depuis frontmatter)
- `cli` : `_load_spec_index()` + `_render_spec_notes()` (spec-summary mirror)
- `index_generator.generate_specs_index()` → `_index/specs.md` (table + Untested specs)

### Bloc 4 — Plan-keeper extension
- `DeviationType` += `spec_implements_drift`, `untested_spec`
- Subagent prompt enrichi avec heuristiques + snippet bash réutilisant `spec_indexer`

### Bloc 5 — Skill /spec
- Génère squelette spec : frontmatter pré-rempli + body (Vision/Décisions/Livrables/Tests d'acceptation/Suite)
- Refuse collisions, valide slug regex
- `next_default: /tdd`

## Métriques post-livraison

- 7 specs indexées (1 spec délivrée, 1 reference, 4 plans, 1 ready)
- 1 spec a `implements:` peuplé (nightfall-sleep-dashboard pointe vers 4 fichiers `static/`)
- 4 specs en `Untested specs` warning (action humaine recommandée)
- 175 tests GREEN
- 77 notes vault (70 code + 7 specs-summary)

## Suite naturelle

- **V2.1 Phase 1** : appliquer la discipline spec-first dès le premier vrai bloc produit (Postgres migration). Chaque bloc = 1 spec → tests RED → impl → tested_by:`peuplé.
- **Phase A.9 potentielle** : agent `spec-suggester` (analogue annotation-suggester) qui propose `implements:` à partir du diff de la PR
- **Phase A.10 potentielle** : skill `/align --specs` qui run plan-keeper avec focus sur les 2 nouvelles deviations
