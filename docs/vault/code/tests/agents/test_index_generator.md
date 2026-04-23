---
type: code-source
language: python
file_path: tests/agents/test_index_generator.py
git_blob: edb6279fe870ae6654a47cf965406abeb7693924
last_synced: '2026-04-23T09:31:47Z'
loc: 83
annotations: []
imports:
- pathlib
- pytest
exports:
- TestGenerateOrphansIndex
- TestGenerateCoverageIndex
- TestGenerateTagsIndex
tags:
- code
- python
---

# tests/agents/test_index_generator.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_index_generator.py`](../../../tests/agents/test_index_generator.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.index_generator.

Generates `_index/orphans.md`, `_index/coverage.md`, `_index/annotations-by-tag.md`
from the current vault state.
"""

from pathlib import Path

import pytest


class TestGenerateOrphansIndex:
    def test_lists_all_orphan_annotations(self, tmp_path: Path):
        from agents.cartographer.index_generator import generate_orphans_index

        # 1 active + 1 orphan
        active = tmp_path / "vault/annotations/x/ok.md"
        active.parent.mkdir(parents=True)
        active.write_text(
            "---\nslug: ok\nstatus: active\nanchors: []\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n---\nbody\n"
        )
        orphan = tmp_path / "vault/annotations/_orphans/lost.md"
        orphan.parent.mkdir(parents=True)
        orphan.write_text(
            "---\nslug: lost\nstatus: orphan\nanchors:\n"
            "  - file: x.py\n    kind: single\n    line: 5\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n---\nbody\n"
        )

        out_path = tmp_path / "vault/_index/orphans.md"
        generate_orphans_index(
            annotation_paths=[str(active), str(orphan)],
            output_path=str(out_path),
        )
        assert out_path.exists()
        text = out_path.read_text()
        assert "lost" in text
        assert "ok" not in text


class TestGenerateCoverageIndex:
    def test_lists_files_without_annotations(self, tmp_path: Path):
        from agents.cartographer.index_generator import generate_coverage_index

        out_path = tmp_path / "vault/_index/coverage.md"
        generate_coverage_index(
            source_files=["a.py", "b.py", "c.py"],
            files_with_annotations={"a.py"},
            output_path=str(out_path),
        )
        text = out_path.read_text()
        assert "b.py" in text
        assert "c.py" in text
        assert "a.py" not in text


class TestGenerateTagsIndex:
    def test_groups_annotations_by_tag(self, tmp_path: Path):
        from agents.cartographer.index_generator import generate_tags_index

        a1 = tmp_path / "a1.md"
        a1.write_text(
            "---\nslug: alpha\nstatus: active\nanchors: []\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n"
            "tags: [perf, sleep]\n---\nbody\n"
        )
        a2 = tmp_path / "a2.md"
        a2.write_text(
            "---\nslug: beta\nstatus: active\nanchors: []\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n"
            "tags: [perf]\n---\nbody\n"
        )
        out_path = tmp_path / "tags.md"
        generate_tags_index(
            annotation_paths=[str(a1), str(a2)],
            output_path=str(out_path),
        )
        text = out_path.read_text()
        assert "perf" in text
        assert "sleep" in text
        assert "alpha" in text
        assert "beta" in text
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestGenerateOrphansIndex` (class) — lines 12-39
- `TestGenerateCoverageIndex` (class) — lines 42-55
- `TestGenerateTagsIndex` (class) — lines 58-83

### Imports
- `pathlib`
- `pytest`

### Exports
- `TestGenerateOrphansIndex`
- `TestGenerateCoverageIndex`
- `TestGenerateTagsIndex`
