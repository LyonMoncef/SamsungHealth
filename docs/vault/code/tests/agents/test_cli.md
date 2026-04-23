---
type: code-source
language: python
file_path: tests/agents/test_cli.py
git_blob: b4c0dc6f1ffd6cfbffca18e62fd74b5194e71f1a
last_synced: '2026-04-23T10:10:35Z'
loc: 173
annotations: []
imports:
- pathlib
- pytest
exports:
- TestRunFull
- TestRunDiff
- TestRunCheck
- TestMirror
tags:
- code
- python
---

# tests/agents/test_cli.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_cli.py`](../../../tests/agents/test_cli.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.cli.

The CLI orchestrates everything: walk source tree, parse markers, resolve
anchors, render notes, write `_index/`. Three modes:
- `--full`  : (re-)render every note, scan whole repo
- `--diff <files>` : only the listed files (used by pre-commit)
- `--check` : dry-run, exit non-zero if any orphan/unmatched marker found
"""

from pathlib import Path

import pytest


class TestRunFull:
    def test_full_creates_notes_for_each_source_file(self, tmp_path: Path):
        from agents.cartographer.cli import run

        # Mini repo: 1 python file + 1 js file
        (tmp_path / "server").mkdir()
        (tmp_path / "server/main.py").write_text("def hi(): return 1\n")
        (tmp_path / "static").mkdir()
        (tmp_path / "static/x.js").write_text("function r() { return 1; }\n")

        report = run(
            mode="full",
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            source_globs=["server/**/*.py", "static/**/*.js"],
        )
        assert report.overall == "complete"
        assert report.notes_generated == 2
        assert (tmp_path / "docs/vault/code/server/main.md").exists()
        assert (tmp_path / "docs/vault/code/static/x.md").exists()

        # Frontmatter + code preserved
        content = (tmp_path / "docs/vault/code/server/main.md").read_text()
        assert "type: code-source" in content
        assert "def hi(): return 1" in content

    def test_full_generates_index_files(self, tmp_path: Path):
        from agents.cartographer.cli import run

        (tmp_path / "server").mkdir()
        (tmp_path / "server/x.py").write_text("x = 1\n")

        run(
            mode="full",
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            source_globs=["server/**/*.py"],
        )
        assert (tmp_path / "docs/vault/_index/orphans.md").exists()
        assert (tmp_path / "docs/vault/_index/coverage.md").exists()
        assert (tmp_path / "docs/vault/_index/annotations-by-tag.md").exists()
        # Coverage lists the un-annotated file
        cov = (tmp_path / "docs/vault/_index/coverage.md").read_text()
        assert "server/x.py" in cov


class TestRunDiff:
    def test_diff_only_renders_listed_files(self, tmp_path: Path):
        from agents.cartographer.cli import run

        (tmp_path / "server").mkdir()
        (tmp_path / "server/a.py").write_text("a = 1\n")
        (tmp_path / "server/b.py").write_text("b = 1\n")

        report = run(
            mode="diff",
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            source_globs=["server/**/*.py"],
            diff_files=["server/a.py"],
        )
        assert report.notes_generated == 1
        assert (tmp_path / "docs/vault/code/server/a.md").exists()
        assert not (tmp_path / "docs/vault/code/server/b.md").exists()


class TestRunCheck:
    def test_check_returns_failed_on_orphan(self, tmp_path: Path):
        from agents.cartographer.cli import run

        # source file without the slug + annotation file referencing it = orphan
        (tmp_path / "server").mkdir()
        (tmp_path / "server/x.py").write_text("x = 1\n")
        ann_dir = tmp_path / "docs/vault/annotations/server/x"
        ann_dir.mkdir(parents=True)
        (ann_dir / "lost.md").write_text(
            "---\nslug: lost\nstatus: active\nanchors:\n"
            "  - file: server/x.py\n    kind: single\n    line: 1\n"
            "scope: single-file\ncreated_by: human\nreferences: {}\n---\nbody\n"
        )

        report = run(
            mode="check",
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            source_globs=["server/**/*.py"],
        )
        assert report.overall == "failed"
        assert "lost" in report.new_orphans

    def test_check_passes_when_clean(self, tmp_path: Path):
        from agents.cartographer.cli import run

        (tmp_path / "server").mkdir()
        (tmp_path / "server/x.py").write_text("x = 1\n")

        report = run(
            mode="check",
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            source_globs=["server/**/*.py"],
        )
        assert report.overall == "complete"


class TestMirror:
    def test_mirror_copies_vault_to_target(self, tmp_path: Path):
        from agents.cartographer.cli import run

        (tmp_path / "server").mkdir()
        (tmp_path / "server/x.py").write_text("x = 1\n")
        mirror = tmp_path / "windows-mirror"

        run(
            mode="full",
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            source_globs=["server/**/*.py"],
            mirror_to=str(mirror),
        )
        assert (mirror / "code/server/x.md").exists()
        assert (mirror / "_index/coverage.md").exists()

    def test_mirror_overwrites_existing(self, tmp_path: Path):
        from agents.cartographer.cli import run

        (tmp_path / "server").mkdir()
        (tmp_path / "server/x.py").write_text("x = 1\n")
        mirror = tmp_path / "mirror"
        mirror.mkdir()
        (mirror / "code").mkdir()
        (mirror / "code" / "STALE.md").write_text("old")

        run(
            mode="full",
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            source_globs=["server/**/*.py"],
            mirror_to=str(mirror),
        )
        # New content present
        assert (mirror / "code/server/x.md").exists()
        # Stale file from before is purged
        assert not (mirror / "code/STALE.md").exists()

    def test_mirror_skipped_when_none(self, tmp_path: Path):
        from agents.cartographer.cli import run

        (tmp_path / "server").mkdir()
        (tmp_path / "server/x.py").write_text("x = 1\n")

        # No mirror_to → should not raise, no mirror created
        run(
            mode="full",
            repo_root=str(tmp_path),
            vault_root=str(tmp_path / "docs/vault"),
            source_globs=["server/**/*.py"],
            mirror_to=None,
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestRunFull` (class) — lines 15-58
- `TestRunDiff` (class) — lines 61-78
- `TestRunCheck` (class) — lines 81-117
- `TestMirror` (class) — lines 120-173

### Imports
- `pathlib`
- `pytest`

### Exports
- `TestRunFull`
- `TestRunDiff`
- `TestRunCheck`
- `TestMirror`


## Exercises *(auto — this test file touches)*

### `test_cli.TestMirror.test_mirror_copies_vault_to_target`
- [[../../code/agents/cartographer/anchor_resolver|agents/cartographer/anchor_resolver.py]] · `resolve_anchors_for_file`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `run`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_sources`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_annotations`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_render_one`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_all_markers`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_write_indexes`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_strip_ext`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_mirror_vault`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_git_blob_sha`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_write`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_ts`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_orphans_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_coverage_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_tags_index`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `render_note`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_frontmatter`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_code_with_callouts`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_wrap_code_block`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_appendix`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`

### `test_cli.TestMirror.test_mirror_overwrites_existing`
- [[../../code/agents/cartographer/anchor_resolver|agents/cartographer/anchor_resolver.py]] · `resolve_anchors_for_file`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `run`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_sources`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_annotations`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_render_one`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_all_markers`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_write_indexes`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_strip_ext`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_mirror_vault`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_git_blob_sha`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_write`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_ts`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_orphans_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_coverage_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_tags_index`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `render_note`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_frontmatter`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_code_with_callouts`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_wrap_code_block`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_appendix`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`

### `test_cli.TestMirror.test_mirror_skipped_when_none`
- [[../../code/agents/cartographer/anchor_resolver|agents/cartographer/anchor_resolver.py]] · `resolve_anchors_for_file`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `run`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_sources`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_annotations`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_render_one`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_all_markers`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_write_indexes`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_strip_ext`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_git_blob_sha`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_write`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_ts`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_orphans_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_coverage_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_tags_index`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `render_note`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_frontmatter`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_code_with_callouts`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_wrap_code_block`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_appendix`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`

### `test_cli.TestRunCheck.test_check_passes_when_clean`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `run`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_sources`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_annotations`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_all_markers`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`

### `test_cli.TestRunCheck.test_check_returns_failed_on_orphan`
- [[../../code/agents/cartographer/annotation_io|agents/cartographer/annotation_io.py]] · `read_annotation`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `run`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_sources`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_annotations`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_all_markers`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`

### `test_cli.TestRunDiff.test_diff_only_renders_listed_files`
- [[../../code/agents/cartographer/anchor_resolver|agents/cartographer/anchor_resolver.py]] · `resolve_anchors_for_file`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `run`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_annotations`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_render_one`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_all_markers`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_strip_ext`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_git_blob_sha`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `render_note`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_frontmatter`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_code_with_callouts`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_wrap_code_block`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_appendix`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`

### `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file`
- [[../../code/agents/cartographer/anchor_resolver|agents/cartographer/anchor_resolver.py]] · `resolve_anchors_for_file`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `run`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_sources`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_annotations`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_render_one`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_all_markers`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_write_indexes`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_strip_ext`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_git_blob_sha`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_write`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_ts`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_orphans_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_coverage_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_tags_index`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `render_note`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_frontmatter`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_code_with_callouts`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_wrap_code_block`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_appendix`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_javascript`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_ts_field_text`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_node_text`

### `test_cli.TestRunFull.test_full_generates_index_files`
- [[../../code/agents/cartographer/anchor_resolver|agents/cartographer/anchor_resolver.py]] · `resolve_anchors_for_file`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `run`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_sources`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_discover_annotations`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_render_one`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_all_markers`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_write_indexes`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_strip_ext`
- [[../../code/agents/cartographer/cli|agents/cartographer/cli.py]] · `_git_blob_sha`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_write`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `_ts`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_orphans_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_coverage_index`
- [[../../code/agents/cartographer/index_generator|agents/cartographer/index_generator.py]] · `generate_tags_index`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `render_note`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_frontmatter`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_code_with_callouts`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_wrap_code_block`
- [[../../code/agents/cartographer/note_renderer|agents/cartographer/note_renderer.py]] · `_render_appendix`
- [[../../code/agents/cartographer/orphan_detector|agents/cartographer/orphan_detector.py]] · `detect_orphans`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`
