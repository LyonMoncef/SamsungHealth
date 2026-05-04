"""RED-first tests for agents.cartographer.anchor_resolver.

Resolves the bidirectional state between a source file's markers and the
annotations that exist on disk. Returns:
- `active`   : annotations whose marker is still present (line number maybe shifted)
- `orphans`  : annotations whose marker has disappeared
- `unmatched`: markers in the code with no annotation file yet (newly added)
"""

from pathlib import Path

import pytest


class TestResolveAnchorsForFile:
    def test_active_annotation_with_marker_present(self, tmp_path: Path):
        from agents.cartographer.anchor_resolver import resolve_anchors_for_file
        from agents.cartographer.markers import Marker

        # Annotation on disk
        ann_path = tmp_path / "vault/annotations/x/abc-def.md"
        ann_path.parent.mkdir(parents=True)
        ann_path.write_text(
            "---\nslug: abc-def\nstatus: active\nanchors:\n"
            "  - file: x.py\n    kind: single\n    line: 5\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n---\n\nbody\n"
        )

        markers = [Marker(slug="abc-def", file="x.py", kind="single", line=10)]
        result = resolve_anchors_for_file(
            file="x.py",
            markers=markers,
            annotation_paths=[str(ann_path)],
        )
        assert "abc-def" in result.active
        assert result.active["abc-def"].line == 10  # updated to current
        assert result.orphans == []
        assert result.unmatched == []

    def test_orphan_when_marker_missing(self, tmp_path: Path):
        from agents.cartographer.anchor_resolver import resolve_anchors_for_file

        ann_path = tmp_path / "vault/annotations/x/lost.md"
        ann_path.parent.mkdir(parents=True)
        ann_path.write_text(
            "---\nslug: lost\nstatus: active\nanchors:\n"
            "  - file: x.py\n    kind: single\n    line: 5\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n---\n\nbody\n"
        )

        result = resolve_anchors_for_file(
            file="x.py",
            markers=[],
            annotation_paths=[str(ann_path)],
        )
        assert "lost" in [o.slug for o in result.orphans]
        assert result.active == {}

    def test_unmatched_when_marker_without_annotation(self):
        from agents.cartographer.anchor_resolver import resolve_anchors_for_file
        from agents.cartographer.markers import Marker

        markers = [Marker(slug="brand-new", file="x.py", kind="single", line=1)]
        result = resolve_anchors_for_file(
            file="x.py",
            markers=markers,
            annotation_paths=[],
        )
        assert "brand-new" in result.unmatched

    def test_range_marker_updates_begin_end_lines(self, tmp_path: Path):
        from agents.cartographer.anchor_resolver import resolve_anchors_for_file
        from agents.cartographer.markers import Marker

        ann_path = tmp_path / "ann.md"
        ann_path.write_text(
            "---\nslug: rng\nstatus: active\nanchors:\n"
            "  - file: x.py\n    kind: range\n    begin_line: 1\n    end_line: 5\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n---\n\nbody\n"
        )
        markers = [Marker(slug="rng", file="x.py", kind="range", begin_line=12, end_line=20)]
        result = resolve_anchors_for_file(
            file="x.py", markers=markers, annotation_paths=[str(ann_path)],
        )
        assert result.active["rng"].begin_line == 12
        assert result.active["rng"].end_line == 20
