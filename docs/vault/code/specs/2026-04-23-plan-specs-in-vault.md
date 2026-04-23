---
type: spec-summary
slug: 2026-04-23-plan-specs-in-vault
original_type: spec
status: delivered
source: ../../specs/2026-04-23-plan-specs-in-vault
---

# Spec — 2026-04-23-plan-specs-in-vault

Source : [[../../specs/2026-04-23-plan-specs-in-vault]]


## Targets *(auto — from frontmatter)*

### Implementation
- [[../code/agents/contracts/spec|agents/contracts/spec.py]] — symbols: `SpecMeta`, `SpecImplements`, `SpecTestedBy`, `SpecType`, `SpecStatus`
- [[../code/agents/cartographer/spec_indexer|agents/cartographer/spec_indexer.py]] — symbols: `load_spec`, `build_index`, `discover_spec_paths`, `detect_implements_drift`, `untested_specs`
- [[../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] — symbols: `_render_appendix`, `_render_validates_section`, `_render_spec_targets`
- [[../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] — symbols: `generate_specs_index`
- [[../code/agents/cartographer/cli|agents/cartographer/cli.py]] — symbols: `_load_spec_index`, `_render_spec_notes`
- [[../code/.claude/skills/spec/SKILL.md|.claude/skills/spec/SKILL.md]]
- [[../code/.claude/agents/plan-keeper.md|.claude/agents/plan-keeper.md]]
- [[../code/agents/contracts/plan_keeper|agents/contracts/plan_keeper.py]] — symbols: `DeviationType`

### Tests
- [[../code/tests/agents/test_spec_indexer|tests/agents/test_spec_indexer.py]] — classes: `TestLoadSpec`, `TestBuildIndex`, `TestDriftDetection`
- [[../code/tests/agents/test_note_renderer|tests/agents/test_note_renderer.py]] — classes: `TestCoverageIntegration` · methods: `test_appendix_lists_implementing_specs`, `test_test_file_validates_section`, `test_spec_note_targets_section`
- [[../code/tests/agents/test_index_generator|tests/agents/test_index_generator.py]] — classes: `TestGenerateSpecsIndex`
- [[../code/tests/agents/test_contracts|tests/agents/test_contracts.py]] — classes: `TestSpec`, `TestPlanKeeper`, `TestPackageReExports`
