---
type: code-source
language: python
file_path: tests/agents/test_orphan_detector.py
git_blob: 938f741d8b206195febc9d30e7e27cdb070c7747
last_synced: '2026-04-23T10:40:54Z'
loc: 73
annotations: []
imports:
- pathlib
- pytest
exports:
- TestDetectOrphansAcrossRepo
tags:
- code
- python
---

# tests/agents/test_orphan_detector.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_orphan_detector.py`](../../../tests/agents/test_orphan_detector.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.orphan_detector.

Diff anchor state across files in the working tree:
- given the current set of `Marker` per file + the set of `Annotation` files on disk
- detect: new orphans (annotation present, no marker found anywhere), resolved orphans
"""

from pathlib import Path

import pytest


class TestDetectOrphansAcrossRepo:
    def test_annotation_with_no_matching_marker_is_new_orphan(self, tmp_path: Path):
        from agents.cartographer.orphan_detector import detect_orphans
        from agents.cartographer.markers import Marker

        ann = tmp_path / "vault/annotations/x/lost.md"
        ann.parent.mkdir(parents=True)
        ann.write_text(
            "---\nslug: lost\nstatus: active\nanchors:\n"
            "  - file: x.py\n    kind: single\n    line: 5\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n---\n\nbody\n"
        )

        markers_per_file = {}  # nothing in code
        result = detect_orphans(
            markers_per_file=markers_per_file,
            annotation_paths=[str(ann)],
        )
        assert "lost" in result.new_orphans
        assert result.resolved_orphans == []

    def test_orphan_with_marker_back_is_resolved(self, tmp_path: Path):
        from agents.cartographer.orphan_detector import detect_orphans
        from agents.cartographer.markers import Marker

        ann = tmp_path / "ann.md"
        ann.write_text(
            "---\nslug: back\nstatus: orphan\nanchors:\n"
            "  - file: x.py\n    kind: single\n    line: 5\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n---\n\nbody\n"
        )

        markers_per_file = {
            "x.py": [Marker(slug="back", file="x.py", kind="single", line=12)],
        }
        result = detect_orphans(
            markers_per_file=markers_per_file,
            annotation_paths=[str(ann)],
        )
        assert "back" in result.resolved_orphans
        assert "back" not in result.new_orphans

    def test_active_annotation_with_marker_unchanged(self, tmp_path: Path):
        from agents.cartographer.orphan_detector import detect_orphans
        from agents.cartographer.markers import Marker

        ann = tmp_path / "ann.md"
        ann.write_text(
            "---\nslug: ok\nstatus: active\nanchors:\n"
            "  - file: x.py\n    kind: single\n    line: 5\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n---\n\nbody\n"
        )
        markers_per_file = {
            "x.py": [Marker(slug="ok", file="x.py", kind="single", line=5)],
        }
        result = detect_orphans(
            markers_per_file=markers_per_file,
            annotation_paths=[str(ann)],
        )
        assert result.new_orphans == []
        assert result.resolved_orphans == []
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestDetectOrphansAcrossRepo` (class) — lines 13-73

### Imports
- `pathlib`
- `pytest`

### Exports
- `TestDetectOrphansAcrossRepo`


## Exercises *(auto — this test file touches)*

### `test_orphan_detector.TestDetectOrphansAcrossRepo.test_active_annotation_with_marker_unchanged`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `read_annotation`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`

### `test_orphan_detector.TestDetectOrphansAcrossRepo.test_annotation_with_no_matching_marker_is_new_orphan`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `read_annotation`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`

### `test_orphan_detector.TestDetectOrphansAcrossRepo.test_orphan_with_marker_back_is_resolved`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `read_annotation`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`
