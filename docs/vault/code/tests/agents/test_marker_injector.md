---
type: code-source
language: python
file_path: tests/agents/test_marker_injector.py
git_blob: ac1dafe4d918b3ed815dc3c73816fa0a0024d787
last_synced: '2026-04-23T10:17:20Z'
loc: 124
annotations: []
imports:
- pathlib
- pytest
exports:
- TestInjectSinglePython
- TestInjectSingleJavaScript
- TestInjectSingleHTML
- TestInjectRangePython
- TestRemoveMarker
tags:
- code
- python
---

# tests/agents/test_marker_injector.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_marker_injector.py`](../../../tests/agents/test_marker_injector.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.marker_injector.

Inject / remove `@vault:<slug>` markers in source code, per language.
- inject_single(file, line, slug, language) → writes EOL comment
- inject_range(file, begin, end, slug, language) → writes own-line begin/end
- remove_marker(file, slug, language) → removes single + all begin/end pairs
- All operations are idempotent (re-inject same slug at same line = no change)
- Operations preserve trailing newline at EOF
"""

from pathlib import Path

import pytest


class TestInjectSinglePython:
    def test_eol_comment_added(self, tmp_path: Path):
        from agents.cartographer.marker_injector import inject_single

        f = tmp_path / "x.py"
        f.write_text("x = 1\ny = 2\nz = 3\n")
        inject_single(str(f), line=2, slug="my-note", language="python")
        lines = f.read_text().splitlines()
        assert lines[1] == "y = 2  # @vault:my-note"
        assert lines[0] == "x = 1"
        assert lines[2] == "z = 3"

    def test_idempotent(self, tmp_path: Path):
        from agents.cartographer.marker_injector import inject_single

        f = tmp_path / "x.py"
        f.write_text("y = 2\n")
        inject_single(str(f), line=1, slug="my-note", language="python")
        before = f.read_text()
        inject_single(str(f), line=1, slug="my-note", language="python")
        assert f.read_text() == before


class TestInjectSingleJavaScript:
    def test_eol_comment_js(self, tmp_path: Path):
        from agents.cartographer.marker_injector import inject_single

        f = tmp_path / "x.js"
        f.write_text("const x = 1;\n")
        inject_single(str(f), line=1, slug="abc-def", language="javascript")
        assert f.read_text().rstrip() == "const x = 1; // @vault:abc-def"


class TestInjectSingleHTML:
    def test_eol_comment_html(self, tmp_path: Path):
        from agents.cartographer.marker_injector import inject_single

        f = tmp_path / "x.html"
        f.write_text("<div>X</div>\n")
        inject_single(str(f), line=1, slug="abc-def", language="html")
        assert f.read_text().rstrip() == "<div>X</div> <!-- @vault:abc-def -->"


class TestInjectRangePython:
    def test_begin_end_added_around_lines(self, tmp_path: Path):
        from agents.cartographer.marker_injector import inject_range

        f = tmp_path / "x.py"
        f.write_text("x = 1\ndef a(): pass\nreturn None\nx = 4\n")
        inject_range(str(f), begin=2, end=3, slug="block", language="python")
        lines = f.read_text().splitlines()
        assert lines[0] == "x = 1"
        assert lines[1] == "# @vault:block begin"
        assert lines[2] == "def a(): pass"
        assert lines[3] == "return None"
        assert lines[4] == "# @vault:block end"
        assert lines[5] == "x = 4"

    def test_range_indent_matches_line(self, tmp_path: Path):
        from agents.cartographer.marker_injector import inject_range

        f = tmp_path / "x.py"
        f.write_text("class A:\n    def m(self):\n        pass\n")
        inject_range(str(f), begin=2, end=3, slug="block", language="python")
        lines = f.read_text().splitlines()
        assert lines[1] == "    # @vault:block begin"
        assert lines[2] == "    def m(self):"
        assert lines[3] == "        pass"
        assert lines[4] == "    # @vault:block end"


class TestRemoveMarker:
    def test_remove_single(self, tmp_path: Path):
        from agents.cartographer.marker_injector import remove_marker

        f = tmp_path / "x.py"
        f.write_text("x = 1  # @vault:my-note\ny = 2\n")
        remove_marker(str(f), slug="my-note", language="python")
        assert f.read_text() == "x = 1\ny = 2\n"

    def test_remove_range(self, tmp_path: Path):
        from agents.cartographer.marker_injector import remove_marker

        f = tmp_path / "x.py"
        f.write_text(
            "x = 1\n"
            "# @vault:block begin\n"
            "y = 2\n"
            "# @vault:block end\n"
            "z = 3\n"
        )
        remove_marker(str(f), slug="block", language="python")
        assert f.read_text() == "x = 1\ny = 2\nz = 3\n"

    def test_remove_does_not_touch_other_slugs(self, tmp_path: Path):
        from agents.cartographer.marker_injector import remove_marker

        f = tmp_path / "x.py"
        f.write_text("x = 1  # @vault:keep\ny = 2  # @vault:gone\n")
        remove_marker(str(f), slug="gone", language="python")
        assert f.read_text() == "x = 1  # @vault:keep\ny = 2\n"

    def test_idempotent_when_slug_absent(self, tmp_path: Path):
        from agents.cartographer.marker_injector import remove_marker

        f = tmp_path / "x.py"
        f.write_text("x = 1\n")
        remove_marker(str(f), slug="absent", language="python")
        assert f.read_text() == "x = 1\n"
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestInjectSinglePython` (class) — lines 16-36
- `TestInjectSingleJavaScript` (class) — lines 39-46
- `TestInjectSingleHTML` (class) — lines 49-56
- `TestInjectRangePython` (class) — lines 59-84
- `TestRemoveMarker` (class) — lines 87-124

### Imports
- `pathlib`
- `pytest`

### Exports
- `TestInjectSinglePython`
- `TestInjectSingleJavaScript`
- `TestInjectSingleHTML`
- `TestInjectRangePython`
- `TestRemoveMarker`


## Exercises *(auto — this test file touches)*

### `test_marker_injector.TestInjectRangePython.test_begin_end_added_around_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_comment`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `inject_range`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_leading_whitespace`

### `test_marker_injector.TestInjectRangePython.test_range_indent_matches_line`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_comment`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `inject_range`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_leading_whitespace`

### `test_marker_injector.TestInjectSingleHTML.test_eol_comment_html`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_comment`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `inject_single`

### `test_marker_injector.TestInjectSingleJavaScript.test_eol_comment_js`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_comment`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `inject_single`

### `test_marker_injector.TestInjectSinglePython.test_eol_comment_added`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_comment`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `inject_single`

### `test_marker_injector.TestInjectSinglePython.test_idempotent`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_comment`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `inject_single`

### `test_marker_injector.TestRemoveMarker.test_idempotent_when_slug_absent`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `remove_marker`

### `test_marker_injector.TestRemoveMarker.test_remove_does_not_touch_other_slugs`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `remove_marker`

### `test_marker_injector.TestRemoveMarker.test_remove_range`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `remove_marker`

### `test_marker_injector.TestRemoveMarker.test_remove_single`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_read_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `_write_lines`
- [[../../code/agents/cartographer/marker_injector|agents/cartographer/marker_injector.py]] · `remove_marker`
