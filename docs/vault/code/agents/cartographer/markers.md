---
type: code-source
language: python
file_path: agents/cartographer/markers.py
git_blob: 7a9ec31fc129c575d3229f078402ffd2ce21669e
last_synced: '2026-04-23T10:10:35Z'
loc: 125
annotations: []
imports:
- re
- dataclasses
- typing
exports:
- MarkerParseError
- infer_language
- parse_markers
tags:
- code
- python
coverage_pct: 100.0
---

# agents/cartographer/markers.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/markers.py`](../../../agents/cartographer/markers.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Parse `@vault:<slug>` markers from source files.

Supports Python / JavaScript / Kotlin / HTML / CSS by mapping each language
to its single-line + block comment syntax. Returns a list of `Marker` objects
(single or range) and raises `MarkerParseError` on malformed input
(unmatched begin/end, invalid slug).
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Literal


SLUG_RE = re.compile(r"^[a-z0-9][a-z0-9-]{2,40}$")


# Per-language comment prefix used to detect markers.
# A marker is recognised inside ANY comment that contains "@vault:slug[ begin|end]".
# The prefix is only used when injecting (not parsing) — parsing accepts both
# inline EOL comments and own-line comments.
_LANG_TO_EXT = {
    ".py": "python",
    ".js": "javascript",
    ".mjs": "javascript",
    ".cjs": "javascript",
    ".kt": "kotlin",
    ".kts": "kotlin",
    ".html": "html",
    ".htm": "html",
    ".css": "css",
}


# Regex matches any vault marker comment, regardless of host language —
# we extract the slug (and optional begin/end keyword) from a wrapper comment.
# Examples matched:
#   "# @vault:my-slug"
#   "// @vault:my-slug begin"
#   "<!-- @vault:my-slug end -->"
#   "/* @vault:my-slug */"
_MARKER_RE = re.compile(
    r"@vault:(?P<slug>[A-Za-z0-9_\-]+)(?:\s+(?P<kw>begin|end))?"
)


class MarkerParseError(ValueError):
    """Raised when markers cannot be reconciled (invalid slug, unbalanced begin/end)."""


@dataclass
class Marker:
    slug: str
    file: str
    kind: Literal["single", "range"]
    line: int | None = None
    begin_line: int | None = None
    end_line: int | None = None


def infer_language(file_path: str) -> str | None:
    """Return language name from file extension, or None if unknown."""
    for ext, lang in _LANG_TO_EXT.items():
        if file_path.endswith(ext):
            return lang
    return None


def parse_markers(source: str, language: str, file_path: str) -> list[Marker]:
    """Scan `source` for vault markers and return a list of resolved Marker objects.

    - lines indexed 1-based
    - begin/end pairs are matched per slug in source order;
      each `begin` must be followed by a matching `end` lower in the file
    - a slug may have multiple begin/end pairs (non-contigu) — each pair is
      returned as its own Marker
    """
    del language  # currently we accept any comment style; kept for forward-compat

    markers: list[Marker] = []
    pending_begins: dict[str, list[int]] = {}  # slug -> stack of begin lines

    for lineno, raw in enumerate(source.splitlines(), start=1):
        for match in _MARKER_RE.finditer(raw):
            slug = match.group("slug")
            kw = match.group("kw")

            if not SLUG_RE.match(slug):
                raise MarkerParseError(
                    f"{file_path}:{lineno}: invalid slug '{slug}' "
                    f"(must match {SLUG_RE.pattern})"
                )

            if kw is None:
                markers.append(
                    Marker(slug=slug, file=file_path, kind="single", line=lineno)
                )
            elif kw == "begin":
                pending_begins.setdefault(slug, []).append(lineno)
            elif kw == "end":
                stack = pending_begins.get(slug)
                if not stack:
                    raise MarkerParseError(
                        f"{file_path}:{lineno}: '@vault:{slug} end' without matching begin"
                    )
                begin_line = stack.pop()
                markers.append(
                    Marker(
                        slug=slug,
                        file=file_path,
                        kind="range",
                        begin_line=begin_line,
                        end_line=lineno,
                    )
                )

    # any slug still pending is unmatched
    for slug, stack in pending_begins.items():
        if stack:
            raise MarkerParseError(
                f"{file_path}:{stack[0]}: '@vault:{slug} begin' without matching end"
            )

    return markers
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `MarkerParseError` (class) — lines 48-49
- `infer_language` (function) — lines 62-67 · **Tested by (10)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunCheck.test_check_passes_when_clean`, `test_cli.TestRunCheck.test_check_returns_failed_on_orphan` _+5_
- `parse_markers` (function) — lines 70-125 · **Tested by (23)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunCheck.test_check_passes_when_clean`, `test_cli.TestRunCheck.test_check_returns_failed_on_orphan` _+18_

### Imports
- `re`
- `dataclasses`
- `typing`

### Exports
- `MarkerParseError`
- `infer_language`
- `parse_markers`
