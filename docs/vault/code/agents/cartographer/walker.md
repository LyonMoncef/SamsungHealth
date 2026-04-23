---
type: code-source
language: python
file_path: agents/cartographer/walker.py
git_blob: e624d754e6623065eb2080b20b04d6c95d3fc656
last_synced: '2026-04-23T10:40:53Z'
loc: 193
annotations: []
imports:
- dataclasses
- typing
exports:
- walk_file
- _walk_python
- _walk_javascript
- _walk_kotlin
- _ts_field_text
- _node_text
tags:
- code
- python
coverage_pct: 91.2621359223301
---

# agents/cartographer/walker.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/walker.py`](../../../agents/cartographer/walker.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Universal AST walker via tree-sitter.

Extracts top-level symbols (functions, classes), imports, and exports from
Python / JavaScript / Kotlin source files.

Best-effort: any parse failure (broken syntax, fragile Kotlin grammar) returns
a partial `FileSymbols` rather than raising. The line count (`loc`) is always
present, computed from the raw text.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Literal


Language = Literal["python", "javascript", "kotlin"]


@dataclass
class Symbol:
    name: str
    kind: Literal["function", "class", "method", "other"]
    begin_line: int
    end_line: int


@dataclass
class FileSymbols:
    file: str
    language: Language
    loc: int
    symbols: list[Symbol] = field(default_factory=list)
    imports: list[str] = field(default_factory=list)
    exports: list[str] = field(default_factory=list)


def walk_file(file_path: str, language: Language) -> FileSymbols:
    with open(file_path, encoding="utf-8") as fp:
        source = fp.read()
    loc = len(source.splitlines())

    fs = FileSymbols(file=file_path, language=language, loc=loc)

    try:
        if language == "python":
            _walk_python(source, fs)
        elif language == "javascript":
            _walk_javascript(source, fs)
        elif language == "kotlin":
            _walk_kotlin(source, fs)
    except Exception:
        # Best effort: any parse failure returns whatever we already collected.
        return fs

    return fs


# ---------------------------------------------------------------------------
# Python
# ---------------------------------------------------------------------------

def _walk_python(source: str, fs: FileSymbols) -> None:
    import tree_sitter_python
    from tree_sitter import Language as TSLanguage, Parser

    parser = Parser(TSLanguage(tree_sitter_python.language()))
    tree = parser.parse(source.encode("utf-8"))
    root = tree.root_node

    for child in root.children:
        if child.type == "function_definition":
            name = _ts_field_text(child, "name", source)
            if name:
                fs.symbols.append(
                    Symbol(
                        name=name, kind="function",
                        begin_line=child.start_point[0] + 1,
                        end_line=child.end_point[0] + 1,
                    )
                )
                fs.exports.append(name)
        elif child.type == "class_definition":
            name = _ts_field_text(child, "name", source)
            if name:
                fs.symbols.append(
                    Symbol(
                        name=name, kind="class",
                        begin_line=child.start_point[0] + 1,
                        end_line=child.end_point[0] + 1,
                    )
                )
                fs.exports.append(name)
        elif child.type == "import_statement":
            for grand in child.children:
                if grand.type == "dotted_name":
                    fs.imports.append(_node_text(grand, source))
        elif child.type == "import_from_statement":
            mod = _ts_field_text(child, "module_name", source)
            if mod:
                fs.imports.append(mod)


# ---------------------------------------------------------------------------
# JavaScript
# ---------------------------------------------------------------------------

def _walk_javascript(source: str, fs: FileSymbols) -> None:
    import tree_sitter_javascript
    from tree_sitter import Language as TSLanguage, Parser

    parser = Parser(TSLanguage(tree_sitter_javascript.language()))
    tree = parser.parse(source.encode("utf-8"))
    root = tree.root_node

    for child in root.children:
        if child.type == "function_declaration":
            name = _ts_field_text(child, "name", source)
            if name:
                fs.symbols.append(
                    Symbol(
                        name=name, kind="function",
                        begin_line=child.start_point[0] + 1,
                        end_line=child.end_point[0] + 1,
                    )
                )
                fs.exports.append(name)
        elif child.type == "class_declaration":
            name = _ts_field_text(child, "name", source)
            if name:
                fs.symbols.append(
                    Symbol(
                        name=name, kind="class",
                        begin_line=child.start_point[0] + 1,
                        end_line=child.end_point[0] + 1,
                    )
                )
                fs.exports.append(name)
        elif child.type == "import_statement":
            src = _ts_field_text(child, "source", source)
            if src:
                fs.imports.append(src.strip("'\""))


# ---------------------------------------------------------------------------
# Kotlin (best effort — grammar is less mature)
# ---------------------------------------------------------------------------

def _walk_kotlin(source: str, fs: FileSymbols) -> None:
    try:
        import tree_sitter_kotlin
        from tree_sitter import Language as TSLanguage, Parser

        parser = Parser(TSLanguage(tree_sitter_kotlin.language()))
        tree = parser.parse(source.encode("utf-8"))
        root = tree.root_node
    except Exception:
        return

    def visit(node) -> None:
        if node.type in ("function_declaration", "class_declaration"):
            name = _ts_field_text(node, "name", source) or _ts_field_text(node, "simple_identifier", source)
            if name:
                fs.symbols.append(
                    Symbol(
                        name=name,
                        kind="function" if node.type == "function_declaration" else "class",
                        begin_line=node.start_point[0] + 1,
                        end_line=node.end_point[0] + 1,
                    )
                )
        for c in node.children:
            visit(c)

    visit(root)


# ---------------------------------------------------------------------------
# tree-sitter helpers
# ---------------------------------------------------------------------------

def _ts_field_text(node, field_name: str, source: str) -> str | None:
    child = node.child_by_field_name(field_name)
    if child is None:
        return None
    return _node_text(child, source)


def _node_text(node, source: str) -> str:
    # tree-sitter's start_byte/end_byte are offsets into the UTF-8 encoded
    # source. Slice via bytes to be safe even when source has multi-byte chars.
    src_bytes = source.encode("utf-8") if isinstance(source, str) else source
    return src_bytes[node.start_byte : node.end_byte].decode("utf-8", errors="replace")
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `walk_file` (function) — lines 38-56 · **Tested by (13)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunDiff.test_diff_only_renders_listed_files`, `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file` _+8_
- `_walk_python` (function) — lines 63-101 · **Tested by (11)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunDiff.test_diff_only_renders_listed_files`, `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file` _+6_
- `_walk_javascript` (function) — lines 108-142 · **Tested by (2)**: `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file`, `test_walker.TestWalkJavaScript.test_extracts_function_decl`
- `_walk_kotlin` (function) — lines 149-175 · **Tested by (1)**: `test_walker.TestWalkKotlin.test_does_not_raise_on_minimal_kotlin`
- `_ts_field_text` (function) — lines 182-186 · **Tested by (6)**: `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file`, `test_walker.TestWalkJavaScript.test_extracts_function_decl`, `test_walker.TestWalkKotlin.test_does_not_raise_on_minimal_kotlin`, `test_walker.TestWalkPython.test_extracts_class`, `test_walker.TestWalkPython.test_extracts_top_level_function` _+1_
- `_node_text` (function) — lines 189-193 · **Tested by (6)**: `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file`, `test_walker.TestWalkJavaScript.test_extracts_function_decl`, `test_walker.TestWalkKotlin.test_does_not_raise_on_minimal_kotlin`, `test_walker.TestWalkPython.test_extracts_class`, `test_walker.TestWalkPython.test_extracts_top_level_function` _+1_

### Imports
- `dataclasses`
- `typing`

### Exports
- `walk_file`
- `_walk_python`
- `_walk_javascript`
- `_walk_kotlin`
- `_ts_field_text`
- `_node_text`
