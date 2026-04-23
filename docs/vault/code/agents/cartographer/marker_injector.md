---
type: code-source
language: python
file_path: agents/cartographer/marker_injector.py
git_blob: 4173ce569ac366b83b4f6a402ed6c98470900d2e
last_synced: '2026-04-23T10:49:29Z'
loc: 122
annotations: []
imports:
- re
- typing
exports:
- _comment
- _read_lines
- _write_lines
- inject_single
- inject_range
- remove_marker
- _leading_whitespace
tags:
- code
- python
coverage_pct: 98.0
---

# agents/cartographer/marker_injector.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/marker_injector.py`](../../../agents/cartographer/marker_injector.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Inject / remove `@vault:<slug>` markers in source files.

Per-language comment style for *injection* only. Parsing accepts every
common comment wrapper (handled in `markers.py`).
"""

from __future__ import annotations

import re
from typing import Literal


# Per-language comment templates
_COMMENT = {
    "python":     {"prefix": "# ",   "suffix": ""},
    "javascript": {"prefix": "// ",  "suffix": ""},
    "kotlin":     {"prefix": "// ",  "suffix": ""},
    "html":       {"prefix": "<!-- ", "suffix": " -->"},
    "css":        {"prefix": "/* ",  "suffix": " */"},
}

Language = Literal["python", "javascript", "kotlin", "html", "css"]


def _comment(slug_payload: str, language: Language) -> str:
    style = _COMMENT[language]
    return f"{style['prefix']}@vault:{slug_payload}{style['suffix']}"


def _read_lines(file_path: str) -> tuple[list[str], str]:
    """Return (lines_without_newline, line_terminator)."""
    with open(file_path, encoding="utf-8") as fp:
        text = fp.read()
    nl = "\n"
    if "\r\n" in text:
        nl = "\r\n"
    return text.splitlines(), nl


def _write_lines(file_path: str, lines: list[str], nl: str) -> None:
    with open(file_path, "w", encoding="utf-8") as fp:
        fp.write(nl.join(lines) + nl)


def inject_single(
    file_path: str,
    line: int,
    slug: str,
    language: Language,
) -> None:
    """Append an EOL marker comment at `line` (1-based). Idempotent."""
    lines, nl = _read_lines(file_path)
    idx = line - 1
    marker = _comment(slug, language)

    if marker in lines[idx]:
        return  # idempotent

    sep = "  " if language == "python" else " "
    lines[idx] = f"{lines[idx]}{sep}{marker}"
    _write_lines(file_path, lines, nl)


def inject_range(
    file_path: str,
    begin: int,
    end: int,
    slug: str,
    language: Language,
) -> None:
    """Wrap lines [begin..end] (1-based, inclusive) with begin/end marker comments.

    Indentation of the begin line is preserved on the inserted comments.
    """
    lines, nl = _read_lines(file_path)
    indent = _leading_whitespace(lines[begin - 1])
    begin_comment = f"{indent}{_comment(f'{slug} begin', language)}"
    end_comment = f"{indent}{_comment(f'{slug} end', language)}"

    new_lines = (
        lines[: begin - 1]
        + [begin_comment]
        + lines[begin - 1 : end]
        + [end_comment]
        + lines[end:]
    )
    _write_lines(file_path, new_lines, nl)


def remove_marker(
    file_path: str,
    slug: str,
    language: Language,
) -> None:
    """Strip every occurrence of `@vault:<slug>[ begin|end]` from the file.

    - EOL markers: comment is removed and trailing whitespace cleaned
    - Own-line markers (single, begin, end): the entire line is dropped
    """
    del language  # any comment style accepted

    lines, nl = _read_lines(file_path)
    pattern = re.compile(
        r"(\s*(?:#|//|<!--|/\*)\s*@vault:" + re.escape(slug) + r"(?:\s+(?:begin|end))?\s*(?:-->|\*/)?\s*)$"
    )
    own_line_pattern = re.compile(
        r"^\s*(?:#|//|<!--|/\*)\s*@vault:" + re.escape(slug) + r"(?:\s+(?:begin|end))?\s*(?:-->|\*/)?\s*$"
    )

    out: list[str] = []
    for ln in lines:
        if own_line_pattern.match(ln):
            continue
        out.append(pattern.sub("", ln))
    _write_lines(file_path, out, nl)


def _leading_whitespace(s: str) -> str:
    i = 0
    while i < len(s) and s[i] in (" ", "\t"):
        i += 1
    return s[:i]
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_comment` (function) — lines 25-27 · **Tested by (6)**: `test_marker_injector.TestInjectRangePython.test_begin_end_added_around_lines`, `test_marker_injector.TestInjectRangePython.test_range_indent_matches_line`, `test_marker_injector.TestInjectSingleHTML.test_eol_comment_html`, `test_marker_injector.TestInjectSingleJavaScript.test_eol_comment_js`, `test_marker_injector.TestInjectSinglePython.test_eol_comment_added` _+1_
- `_read_lines` (function) — lines 30-37 · **Tested by (10)**: `test_marker_injector.TestInjectRangePython.test_begin_end_added_around_lines`, `test_marker_injector.TestInjectRangePython.test_range_indent_matches_line`, `test_marker_injector.TestInjectSingleHTML.test_eol_comment_html`, `test_marker_injector.TestInjectSingleJavaScript.test_eol_comment_js`, `test_marker_injector.TestInjectSinglePython.test_eol_comment_added` _+5_
- `_write_lines` (function) — lines 40-42 · **Tested by (10)**: `test_marker_injector.TestInjectRangePython.test_begin_end_added_around_lines`, `test_marker_injector.TestInjectRangePython.test_range_indent_matches_line`, `test_marker_injector.TestInjectSingleHTML.test_eol_comment_html`, `test_marker_injector.TestInjectSingleJavaScript.test_eol_comment_js`, `test_marker_injector.TestInjectSinglePython.test_eol_comment_added` _+5_
- `inject_single` (function) — lines 45-61 · **Tested by (4)**: `test_marker_injector.TestInjectSingleHTML.test_eol_comment_html`, `test_marker_injector.TestInjectSingleJavaScript.test_eol_comment_js`, `test_marker_injector.TestInjectSinglePython.test_eol_comment_added`, `test_marker_injector.TestInjectSinglePython.test_idempotent`
- `inject_range` (function) — lines 64-87 · **Tested by (2)**: `test_marker_injector.TestInjectRangePython.test_begin_end_added_around_lines`, `test_marker_injector.TestInjectRangePython.test_range_indent_matches_line`
- `remove_marker` (function) — lines 90-115 · **Tested by (4)**: `test_marker_injector.TestRemoveMarker.test_idempotent_when_slug_absent`, `test_marker_injector.TestRemoveMarker.test_remove_does_not_touch_other_slugs`, `test_marker_injector.TestRemoveMarker.test_remove_range`, `test_marker_injector.TestRemoveMarker.test_remove_single`
- `_leading_whitespace` (function) — lines 118-122 · **Tested by (2)**: `test_marker_injector.TestInjectRangePython.test_begin_end_added_around_lines`, `test_marker_injector.TestInjectRangePython.test_range_indent_matches_line`

### Imports
- `re`
- `typing`

### Exports
- `_comment`
- `_read_lines`
- `_write_lines`
- `inject_single`
- `inject_range`
- `remove_marker`
- `_leading_whitespace`
