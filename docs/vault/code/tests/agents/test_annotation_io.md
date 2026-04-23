---
type: code-source
language: python
file_path: tests/agents/test_annotation_io.py
git_blob: 81ae225a845d3ea383700ecc71cf9fd46fdcf933
last_synced: '2026-04-23T10:21:39Z'
loc: 175
annotations: []
imports:
- pathlib
- pytest
exports:
- TestResolveAnnotationPath
- TestReadAnnotation
- TestWriteAnnotation
- TestUpdateStatus
tags:
- code
- python
---

# tests/agents/test_annotation_io.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_annotation_io.py`](../../../tests/agents/test_annotation_io.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.annotation_io.

CRUD on annotation files (`docs/vault/annotations/.../slug.md`):
- read frontmatter + body
- write new annotation with auto frontmatter
- update body preserving frontmatter (and vice versa)
- compute file path from anchors (single-file → annotations/<pkg>/<file>/<slug>.md, cross-file → _cross/<slug>.md)
- move to _orphans/<slug>.md
"""

from pathlib import Path

import pytest


# ---------------------------------------------------------------------------
# resolve_annotation_path
# ---------------------------------------------------------------------------

class TestResolveAnnotationPath:
    def test_single_file_anchor(self):
        from agents.cartographer.annotation_io import resolve_annotation_path
        from agents.contracts.cartographer import AnchorLocation

        anchors = [AnchorLocation(file="server/routers/sleep.py", kind="single", line=21)]
        p = resolve_annotation_path(
            slug="sleep-perf-cap",
            anchors=anchors,
            vault_root="docs/vault",
        )
        assert p == "docs/vault/annotations/server/routers/sleep/sleep-perf-cap.md"

    def test_cross_file_anchor(self):
        from agents.cartographer.annotation_io import resolve_annotation_path
        from agents.contracts.cartographer import AnchorLocation

        anchors = [
            AnchorLocation(file="server/routers/sleep.py", kind="single", line=10),
            AnchorLocation(file="server/database.py", kind="single", line=42),
        ]
        p = resolve_annotation_path(
            slug="sleep-stages-pattern",
            anchors=anchors,
            vault_root="docs/vault",
        )
        assert p == "docs/vault/annotations/_cross/sleep-stages-pattern.md"

    def test_orphan_path(self):
        from agents.cartographer.annotation_io import resolve_annotation_path

        p = resolve_annotation_path(
            slug="lost-note",
            anchors=[],
            vault_root="docs/vault",
        )
        assert p == "docs/vault/annotations/_orphans/lost-note.md"


# ---------------------------------------------------------------------------
# read_annotation
# ---------------------------------------------------------------------------

class TestReadAnnotation:
    def test_read_full_annotation(self, tmp_path: Path):
        from agents.cartographer.annotation_io import read_annotation

        f = tmp_path / "ann.md"
        f.write_text(
            "---\n"
            "slug: sleep-perf-cap\n"
            "type: annotation\n"
            "created: 2026-04-23\n"
            "created_by: human\n"
            "status: active\n"
            "anchors:\n"
            "  - file: server/routers/sleep.py\n"
            "    kind: single\n"
            "    line: 21\n"
            "scope: single-file\n"
            "references: {issue: 142}\n"
            "tags: [perf]\n"
            "---\n"
            "\n"
            "# Why limit\n"
            "\n"
            "body text\n"
        )
        meta, body = read_annotation(str(f))
        assert meta["slug"] == "sleep-perf-cap"
        assert meta["status"] == "active"
        assert meta["anchors"][0]["line"] == 21
        assert "Why limit" in body
        assert "body text" in body

    def test_missing_file_raises(self, tmp_path: Path):
        from agents.cartographer.annotation_io import read_annotation

        with pytest.raises(FileNotFoundError):
            read_annotation(str(tmp_path / "absent.md"))


# ---------------------------------------------------------------------------
# write_annotation
# ---------------------------------------------------------------------------

class TestWriteAnnotation:
    def test_write_creates_file_with_frontmatter(self, tmp_path: Path):
        from agents.cartographer.annotation_io import write_annotation
        from agents.contracts.cartographer import AnchorLocation

        target = tmp_path / "vault" / "annotations" / "x" / "my-note.md"
        write_annotation(
            path=str(target),
            slug="my-note",
            anchors=[AnchorLocation(file="x.py", kind="single", line=10)],
            scope="single-file",
            created_by="human",
            body="# Heading\n\nText.\n",
        )
        assert target.exists()
        content = target.read_text()
        assert content.startswith("---\n")
        assert "slug: my-note" in content
        assert "status: active" in content
        assert "created_by: human" in content
        assert "# Heading" in content

    def test_write_preserves_body_after_round_trip(self, tmp_path: Path):
        from agents.cartographer.annotation_io import (
            read_annotation,
            write_annotation,
        )
        from agents.contracts.cartographer import AnchorLocation

        target = tmp_path / "ann.md"
        body = "# Title\n\nLine 1.\n\nLine 2.\n"
        write_annotation(
            path=str(target),
            slug="abc-def",
            anchors=[AnchorLocation(file="x.py", kind="single", line=1)],
            scope="single-file",
            created_by="agent",
            body=body,
        )
        meta, got_body = read_annotation(str(target))
        assert meta["slug"] == "abc-def"
        assert meta["created_by"] == "agent"
        assert got_body.strip() == body.strip()


# ---------------------------------------------------------------------------
# update_status (orphan transition)
# ---------------------------------------------------------------------------

class TestUpdateStatus:
    def test_set_orphan_updates_frontmatter(self, tmp_path: Path):
        from agents.cartographer.annotation_io import (
            read_annotation,
            update_status,
            write_annotation,
        )
        from agents.contracts.cartographer import AnchorLocation

        target = tmp_path / "ann.md"
        write_annotation(
            path=str(target),
            slug="abc-def",
            anchors=[AnchorLocation(file="x.py", kind="single", line=1)],
            scope="single-file",
            created_by="human",
            body="x",
        )
        update_status(str(target), "orphan")
        meta, _ = read_annotation(str(target))
        assert meta["status"] == "orphan"
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestResolveAnnotationPath` (class) — lines 20-56
- `TestReadAnnotation` (class) — lines 63-99
- `TestWriteAnnotation` (class) — lines 106-148
- `TestUpdateStatus` (class) — lines 155-175

### Imports
- `pathlib`
- `pytest`

### Exports
- `TestResolveAnnotationPath`
- `TestReadAnnotation`
- `TestWriteAnnotation`
- `TestUpdateStatus`


## Exercises *(auto — this test file touches)*

### `test_annotation_io.TestReadAnnotation.test_missing_file_raises`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `read_annotation`

### `test_annotation_io.TestReadAnnotation.test_read_full_annotation`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `read_annotation`

### `test_annotation_io.TestResolveAnnotationPath.test_cross_file_anchor`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `resolve_annotation_path`

### `test_annotation_io.TestResolveAnnotationPath.test_orphan_path`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `resolve_annotation_path`

### `test_annotation_io.TestResolveAnnotationPath.test_single_file_anchor`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `resolve_annotation_path`

### `test_annotation_io.TestUpdateStatus.test_set_orphan_updates_frontmatter`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `read_annotation`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `write_annotation`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `update_status`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `_anchor_to_dict`

### `test_annotation_io.TestWriteAnnotation.test_write_creates_file_with_frontmatter`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `write_annotation`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `_anchor_to_dict`

### `test_annotation_io.TestWriteAnnotation.test_write_preserves_body_after_round_trip`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `read_annotation`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `write_annotation`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `_anchor_to_dict`
