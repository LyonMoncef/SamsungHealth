---
type: spec
phase: A.7
status: delivered
priority: medium
created: 2026-04-23
delivered: 2026-04-23
related_plans:
  - 2026-04-23-plan-v2-refactor-master.md
  - 2026-04-23-plan-v2-multi-agents-architecture.md
  - 2026-04-23-plan-code-as-vault.md
claude_plan_file: ~/.claude/plans/expressive-napping-blum.md  # plan A.5 parent
git_commits:
  - 5108be3   # Bloc 1 : tests/** dans globs
  - 0bcd477   # Bloc 2 : coverage_map + /sync-coverage
  - 049b24f   # Bloc 3 : note_renderer + walker fix
  - 6bbbdb2   # Bloc 4 : CI workflow
  - affbaaa   # HISTORY
tags: [phase-a.7, code-as-vault, coverage, tests, obsidian]
---

# Phase A.7 — Test ↔ code linking

## Vision

Quand on navigue dans une note vault de code dans Obsidian, on voit directement quels tests touchent chaque symbole et chaque range annoté. Inverse aussi : sur un test, on voit quel code source il exerce.

## Architecture

3 sources combinées :

1. **AST walker** (Phase A.5, déjà existant) — extrait les symboles top-level (functions, classes) avec leurs lignes
2. **Coverage runtime** (nouveau A.7) — `pytest --cov` avec `dynamic_context = test_function` produit un mapping `{file: {line: [test_ids]}}`
3. **Coverage manifest** (nouveau A.7) — combine 1+2 en 3 vues normalisées :
   - `by_symbol` : `{<file>::<symbol>: {tests, covered_lines, total_lines, pct}}`
   - `by_test` : `{<test_id>: [{file, symbol}, ...]}` (inverse)
   - `by_file` : `{<file>: {pct, tests}}` (aggregate)

## Décisions techniques

- **Scope** : tous les tests (Python pour l'instant, JS/Kotlin Phase B+ via c8/kover qui émettront le même format de manifest)
- **Manifest gitignored** (`docs/vault/_index/coverage-map.json`, `coverage.json`, `.coverage*`) — regénéré à la demande, pas de noise dans les diffs
- **Trigger** : (b) CI nightly + (c) skill manuel `/sync-coverage`. Pas d'opt-in dans pre-commit (trop lent)
- **Granularité** : per-symbol par défaut (appendix Symbols), per-range pour les callouts annotations (intersection)
- **Tests en vault** : `tests/**/*.py`, `android-app/**/test/**/*.kt` ajoutés à `DEFAULT_SOURCE_GLOBS`

## Livrables

### Bloc 1 — Tests en vault
- `agents/cartographer/cli.py` : `DEFAULT_SOURCE_GLOBS` étendu (`tests/**/*.py`, `android-app/**/test/**/*.kt`)
- Re-bootstrap : 49 → 65 notes vault (+15 tests + conftest)

### Bloc 2 — Coverage manifest generator
- `agents/cartographer/coverage_map.py` : `run_pytest_cov()` + `parse_coverage()` + `tests_for_range()` + `write_manifest()` + CLI
- `.claude/skills/sync-coverage/SKILL.md` : wrapper, next_default `/sync-vault --full`
- `.gitignore` : manifest + raw coverage exclus
- `requirements.txt` : `pytest-cov>=5.0`, `coverage>=7.0`

### Bloc 3 — Note renderer integration
- `note_renderer.render_note()` accepte `coverage_manifest` + `coverage_raw`
- Frontmatter `coverage_pct` exposé
- Appendix Symbols : `Tested by (N): test_X, ...` par symbole (ou `⚠️ no test`)
- Annotation callouts : sub-callout `> [!test]+ Tested by` (intersection range ↔ contexts)
- Test files : section auto `## Exercises` (inverse map test → code)
- `index_generator.generate_coverage_map_index()` → `_index/coverage-map.md` (table fichiers + Untested symbols)
- **Bonus fix walker** : `_node_text()` corrigé pour UTF-8 byte slicing

### Bloc 4 — CI
- `.github/workflows/coverage.yml` : run `coverage_map`, threshold gate opt-in (`vars.COVERAGE_MIN_PCT`), upload artifact 14j

## Tests

158/158 GREEN. 7 tests `test_coverage_map.py` + 4 `TestCoverageIntegration` dans `test_note_renderer.py` + 1 `TestGenerateCoverageMapIndex`.

## Métriques post-livraison

- 67 notes vault (49 code + 15 tests + 1 conftest + 2 vault navigation)
- 115 symbols mappés
- 90 tests indexés
- 38 fichiers couverts
- Manifest size : ~100 KB (gitignored)

## Suite naturelle

- Phase B+ : ajouter c8 (JS) + kover (Kotlin) → mêmes 3 vues, plug-and-play renderer
- Idée : badge "Untested symbols: N" en frontmatter pour CI gate sémantique
