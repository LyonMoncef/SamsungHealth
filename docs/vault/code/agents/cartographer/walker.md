---
type: code-source
language: python
file_path: agents/cartographer/walker.py
git_blob: 002780f058d0329d13c558a1d8f3e2a8b5801466
last_synced: '2026-04-23T08:13:16Z'
loc: 190
annotations: []
imports:
- dataclasses
- typing
exports:
- walk_file
- _walk_python
- _walk_javascript
- alk_kotlin(s
- s_field_text(n
- ode_text(n
tags:
- code
- python
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
    return source[node.start_byte : node.end_byte]
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `walk_file` (function) — lines 38-56
- `_walk_python` (function) — lines 63-101
- `_walk_javascript` (function) — lines 108-142
- `alk_kotlin(s` (function) — lines 149-175
- `s_field_text(n` (function) — lines 182-186
- `ode_text(n` (function) — lines 189-190

### Imports
- `dataclasses`
- `typing`

### Exports
- `walk_file`
- `_walk_python`
- `_walk_javascript`
- `alk_kotlin(s`
- `s_field_text(n`
- `ode_text(n`
