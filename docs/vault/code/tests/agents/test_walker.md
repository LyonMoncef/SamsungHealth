---
type: code-source
language: python
file_path: tests/agents/test_walker.py
git_blob: 55b459455a9bd8ddf7dcfaec473e79db60ea2ec1
last_synced: '2026-04-23T10:31:18Z'
loc: 101
annotations: []
imports:
- pytest
exports:
- TestWalkPython
- TestWalkJavaScript
- TestWalkKotlin
tags:
- code
- python
---

# tests/agents/test_walker.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_walker.py`](../../../tests/agents/test_walker.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.walker.

Universal AST walker via tree-sitter (Python / JavaScript / Kotlin).
Output: a structured `FileSymbols` object listing imports, exports, and
top-level symbols (functions, classes) with line ranges.

Walker is best-effort — Kotlin parser is fragile, so we accept partial output
when AST parsing degrades, but the call must never raise.
"""

import pytest


class TestWalkPython:
    def test_extracts_top_level_function(self, tmp_path):
        from agents.cartographer.walker import walk_file

        f = tmp_path / "x.py"
        f.write_text(
            "import os\n"
            "from collections import deque\n"
            "\n"
            "def hello(name: str) -> str:\n"
            "    return f'hi {name}'\n"
        )
        sym = walk_file(str(f), language="python")
        names = {s.name for s in sym.symbols}
        assert "hello" in names
        assert "os" in sym.imports
        assert "collections" in sym.imports
        # exports: top-level names
        assert "hello" in sym.exports

    def test_extracts_class(self, tmp_path):
        from agents.cartographer.walker import walk_file

        f = tmp_path / "x.py"
        f.write_text("class Foo:\n    def m(self):\n        pass\n")
        sym = walk_file(str(f), language="python")
        names = {s.name for s in sym.symbols}
        assert "Foo" in names

    def test_lines_are_set(self, tmp_path):
        from agents.cartographer.walker import walk_file

        f = tmp_path / "x.py"
        f.write_text("def a():\n    pass\n\ndef b():\n    pass\n")
        sym = walk_file(str(f), language="python")
        a = next(s for s in sym.symbols if s.name == "a")
        b = next(s for s in sym.symbols if s.name == "b")
        assert a.begin_line == 1
        assert a.end_line >= 2
        assert b.begin_line == 4

    def test_loc(self, tmp_path):
        from agents.cartographer.walker import walk_file

        f = tmp_path / "x.py"
        f.write_text("x = 1\ny = 2\nz = 3\n")
        sym = walk_file(str(f), language="python")
        assert sym.loc == 3

    def test_syntax_error_returns_partial_not_raise(self, tmp_path):
        from agents.cartographer.walker import walk_file

        f = tmp_path / "x.py"
        f.write_text("def broken(\n  this is not python\n")
        # must not raise — best effort
        sym = walk_file(str(f), language="python")
        assert sym.loc == 2  # raw line count


class TestWalkJavaScript:
    def test_extracts_function_decl(self, tmp_path):
        from agents.cartographer.walker import walk_file

        f = tmp_path / "x.js"
        f.write_text(
            "import { foo } from './foo';\n"
            "function render() { return 1; }\n"
        )
        sym = walk_file(str(f), language="javascript")
        names = {s.name for s in sym.symbols}
        assert "render" in names


class TestWalkKotlin:
    def test_does_not_raise_on_minimal_kotlin(self, tmp_path):
        from agents.cartographer.walker import walk_file

        f = tmp_path / "M.kt"
        f.write_text(
            "package com.example\n"
            "\n"
            "fun greet(name: String): String {\n"
            "    return \"hello $name\"\n"
            "}\n"
        )
        sym = walk_file(str(f), language="kotlin")
        # Kotlin parser may be partial — we only require: no crash + loc set
        assert sym.loc == 5
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestWalkPython` (class) — lines 14-70
- `TestWalkJavaScript` (class) — lines 73-84
- `TestWalkKotlin` (class) — lines 87-101

### Imports
- `pytest`

### Exports
- `TestWalkPython`
- `TestWalkJavaScript`
- `TestWalkKotlin`


## Exercises *(auto — this test file touches)*

### `test_walker.TestWalkJavaScript.test_extracts_function_decl`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_javascript`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_ts_field_text`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_node_text`

### `test_walker.TestWalkKotlin.test_does_not_raise_on_minimal_kotlin`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_kotlin`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_kotlin.visit`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_ts_field_text`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_node_text`

### `test_walker.TestWalkPython.test_extracts_class`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_ts_field_text`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_node_text`

### `test_walker.TestWalkPython.test_extracts_top_level_function`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_ts_field_text`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_node_text`

### `test_walker.TestWalkPython.test_lines_are_set`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_ts_field_text`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_node_text`

### `test_walker.TestWalkPython.test_loc`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`

### `test_walker.TestWalkPython.test_syntax_error_returns_partial_not_raise`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `walk_file`
- [[../../code/agents/cartographer/walker|agents/cartographer/walker.py]] · `_walk_python`
