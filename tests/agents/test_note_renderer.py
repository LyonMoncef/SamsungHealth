"""RED-first tests for agents.cartographer.note_renderer.

Renders the full vault note for one source file, interleaving:
- code blocks (full source preserved verbatim)
- Obsidian callouts at marker positions (single line OR around range)
- frontmatter (type, language, file_path, git_blob, last_synced, loc, annotations[], imports[], exports[], tags)
- top header + "code mirror" callout
- appendix with symbol/import/export navigation
- orphan warning callout at top if any orphan annotation for this file
"""

from pathlib import Path

import pytest


class TestRenderNoteBasic:
    def test_minimal_python_no_annotations(self, tmp_path: Path):
        from agents.cartographer.note_renderer import render_note
        from agents.cartographer.walker import FileSymbols, Symbol

        src_path = tmp_path / "x.py"
        src_path.write_text("def hello():\n    return 'hi'\n")
        fs = FileSymbols(
            file=str(src_path), language="python", loc=2,
            symbols=[Symbol(name="hello", kind="function", begin_line=1, end_line=2)],
            imports=[], exports=["hello"],
        )

        out = render_note(
            source_path=str(src_path),
            relative_path="x.py",
            file_symbols=fs,
            git_blob="abc123",
            active_annotations=[],
            orphans=[],
        )
        assert out.startswith("---\n")
        assert "type: code-source" in out
        assert "language: python" in out
        assert "file_path: x.py" in out
        assert "git_blob: abc123" in out
        assert "loc: 2" in out
        assert "# x.py" in out
        # full source code preserved
        assert "def hello():" in out
        assert "return 'hi'" in out
        # appendix
        assert "Symbols" in out
        assert "hello" in out

    def test_callout_inserted_at_single_line_marker(self, tmp_path: Path):
        from agents.cartographer.note_renderer import (
            ActiveAnnotation,
            render_note,
        )
        from agents.cartographer.walker import FileSymbols

        src_path = tmp_path / "x.py"
        src_path.write_text(
            "x = 1\n"
            "y = 2  # @vault:my-note\n"
            "z = 3\n"
        )
        fs = FileSymbols(file=str(src_path), language="python", loc=3)

        ann = ActiveAnnotation(
            slug="my-note",
            kind="single",
            line=2,
            body="# Why y=2\n\nBecause.\n",
            anchor_file="docs/vault/annotations/x/my-note.md",
            references={"issue": 142},
        )
        out = render_note(
            source_path=str(src_path),
            relative_path="x.py",
            file_symbols=fs,
            git_blob="abc",
            active_annotations=[ann],
            orphans=[],
        )

        # Code block split: lines 1-2 then callout then line 3
        # Callout uses Obsidian "> [!note]" syntax
        assert "> [!note]+" in out
        assert "my-note" in out
        assert "Why y=2" in out
        # Anchor link to source annotation file
        assert "[[../../annotations/x/my-note]]" in out or "annotations/x/my-note" in out

    def test_orphan_warning_at_top(self, tmp_path: Path):
        from agents.cartographer.note_renderer import (
            OrphanAnnotation,
            render_note,
        )
        from agents.cartographer.walker import FileSymbols

        src_path = tmp_path / "x.py"
        src_path.write_text("x = 1\n")
        fs = FileSymbols(file=str(src_path), language="python", loc=1)

        orphan = OrphanAnnotation(
            slug="lost-note",
            last_seen_line="5",
            last_seen_commit="def5678",
        )
        out = render_note(
            source_path=str(src_path),
            relative_path="x.py",
            file_symbols=fs,
            git_blob="abc",
            active_annotations=[],
            orphans=[orphan],
        )
        assert "> [!error]+" in out
        assert "orphan" in out.lower()
        assert "lost-note" in out

    def test_range_callout_after_range(self, tmp_path: Path):
        from agents.cartographer.note_renderer import (
            ActiveAnnotation,
            render_note,
        )
        from agents.cartographer.walker import FileSymbols

        src_path = tmp_path / "x.py"
        src_path.write_text(
            "x = 1\n"
            "# @vault:block begin\n"
            "y = 2\n"
            "# @vault:block end\n"
            "z = 3\n"
        )
        fs = FileSymbols(file=str(src_path), language="python", loc=5)

        ann = ActiveAnnotation(
            slug="block",
            kind="range",
            begin_line=2,
            end_line=4,
            body="# Block",
            anchor_file="x.md",
            references={},
        )
        out = render_note(
            source_path=str(src_path),
            relative_path="x.py",
            file_symbols=fs,
            git_blob="abc",
            active_annotations=[ann],
            orphans=[],
        )
        assert "> [!note]+" in out
        assert "block" in out
        # callout sits after the end marker line
        assert out.find("@vault:block end") < out.find("> [!note]+")
