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
