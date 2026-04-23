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


class TestGenerateCoverageMapIndex:
    def test_renders_files_table_and_untested(self, tmp_path: Path):
        from agents.cartographer.index_generator import generate_coverage_map_index

        manifest = {
            "by_file": {
                "agents/x.py": {"pct": 100.0, "tests": ["t1", "t2"]},
                "agents/y.py": {"pct": 0.0, "tests": []},
            },
            "by_symbol": {
                "agents/x.py::add": {"tests": ["t1"], "pct": 100.0},
                "agents/y.py::dead": {"tests": [], "pct": 0.0},
            },
        }
        out_path = tmp_path / "coverage-map.md"
        generate_coverage_map_index(manifest, str(out_path))
        text = out_path.read_text()
        assert "agents/x.py" in text
        assert "100%" in text
        assert "Untested symbols" in text
        assert "agents/y.py::dead" in text


class TestGenerateSpecsIndex:
    def test_renders_table_and_untested_section(self, tmp_path: Path):
        from agents.cartographer.index_generator import generate_specs_index
        from agents.cartographer.spec_indexer import SpecIndex
        from agents.contracts.spec import SpecMeta, SpecTestedBy

        idx = SpecIndex()
        idx.by_slug["spec-tested"] = SpecMeta(
            type="spec", slug="spec-tested", status="ready",
            tested_by=[SpecTestedBy(file="tests/x.py")],
        )
        idx.by_slug["spec-untested"] = SpecMeta(
            type="spec", slug="spec-untested", status="draft",
        )
        out_path = tmp_path / "specs.md"
        generate_specs_index(idx, str(out_path))
        text = out_path.read_text()
        assert "spec-tested" in text
        assert "spec-untested" in text
        assert "Untested specs" in text
        # Untested mark in the table
        assert "⚠️ 0" in text


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
