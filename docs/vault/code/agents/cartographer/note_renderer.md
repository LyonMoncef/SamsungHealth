---
type: code-source
language: python
file_path: agents/cartographer/note_renderer.py
git_blob: 11fd2a5cf925a610d1cb777186c8a3ac523aa52b
last_synced: '2026-04-23T09:31:47Z'
loc: 251
annotations: []
imports:
- dataclasses
- datetime
- typing
- yaml
- agents.cartographer.walker
exports:
- render_note
- "ontmatter(\n    lang"
- 'phan_warning(orphans: '
- "h_callouts(\n    code_lines"
- ': list[str], lan'
- ctiveAnnotation
- eSymbols) -> str
tags:
- code
- python
---

# agents/cartographer/note_renderer.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/note_renderer.py`](../../../agents/cartographer/note_renderer.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Render the full vault note for one source file.

Output structure:
1. YAML frontmatter
2. H1 with file path
3. "Code mirror" info callout
4. Optional `> [!error]+` orphan warning callout (if orphans exist)
5. Source code interleaved with `> [!note]+` callouts at marker positions
6. Appendix: symbols / imports / exports navigation
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Literal

import yaml

from agents.cartographer.walker import FileSymbols


@dataclass
class ActiveAnnotation:
    slug: str
    kind: Literal["single", "range"]
    body: str
    anchor_file: str  # path to the .md annotation source
    references: dict = field(default_factory=dict)
    line: int | None = None
    begin_line: int | None = None
    end_line: int | None = None


@dataclass
class OrphanAnnotation:
    slug: str
    last_seen_line: str = ""
    last_seen_commit: str = ""


def render_note(
    source_path: str,
    relative_path: str,
    file_symbols: FileSymbols,
    git_blob: str,
    active_annotations: list[ActiveAnnotation],
    orphans: list[OrphanAnnotation],
) -> str:
    with open(source_path, encoding="utf-8") as fp:
        source = fp.read()
    code_lines = source.splitlines()

    parts: list[str] = []

    # 1. Frontmatter
    parts.append(_render_frontmatter(
        language=file_symbols.language,
        file_path=relative_path,
        git_blob=git_blob,
        loc=file_symbols.loc,
        annotations=[a.slug for a in active_annotations],
        imports=file_symbols.imports,
        exports=file_symbols.exports,
    ))

    # 2. Header
    parts.append(f"# {relative_path}\n")

    # 3. Code mirror callout
    parts.append(
        "> [!info] Code mirror\n"
        f"> Ce fichier est un **miroir auto-généré** de [`{relative_path}`]"
        f"(../../../{relative_path}).\n"
        "> Code = source de vérité. Annotations dans `docs/vault/annotations/`.\n"
        "> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.\n"
    )

    # 4. Orphan warning
    if orphans:
        parts.append(_render_orphan_warning(orphans))

    # 5. Code + interleaved callouts
    parts.append(_render_code_with_callouts(
        code_lines=code_lines,
        language=file_symbols.language,
        annotations=active_annotations,
    ))

    # 6. Appendix
    parts.append(_render_appendix(file_symbols))

    return "\n".join(parts)


# ---------------------------------------------------------------------------
# frontmatter
# ---------------------------------------------------------------------------

def _render_frontmatter(
    language: str,
    file_path: str,
    git_blob: str,
    loc: int,
    annotations: list[str],
    imports: list[str],
    exports: list[str],
) -> str:
    meta = {
        "type": "code-source",
        "language": language,
        "file_path": file_path,
        "git_blob": git_blob,
        "last_synced": datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z"),
        "loc": loc,
        "annotations": annotations,
        "imports": imports,
        "exports": exports,
        "tags": ["code", language],
    }
    body = yaml.safe_dump(meta, sort_keys=False, allow_unicode=True).rstrip()
    return f"---\n{body}\n---\n"


# ---------------------------------------------------------------------------
# orphan warning callout
# ---------------------------------------------------------------------------

def _render_orphan_warning(orphans: list[OrphanAnnotation]) -> str:
    lines = [f"> [!error]+ ⚠️ {len(orphans)} orphan annotation(s)"]
    for o in orphans:
        bits = []
        if o.last_seen_line:
            bits.append(f"last seen at line {o.last_seen_line}")
        if o.last_seen_commit:
            bits.append(f"in commit `{o.last_seen_commit}`")
        suffix = " — " + ", ".join(bits) if bits else ""
        lines.append(
            f"> - `{o.slug}`{suffix}. Run `/anchor-review {o.slug}` to resolve."
        )
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# code + callouts interleaving
# ---------------------------------------------------------------------------

def _render_code_with_callouts(
    code_lines: list[str],
    language: str,
    annotations: list[ActiveAnnotation],
) -> str:
    # Map insertion-line → list of callouts that fire AFTER that line
    # - single  → after the marker line
    # - range   → after end_line
    insertions: dict[int, list[str]] = {}
    for ann in annotations:
        if ann.kind == "single" and ann.line is not None:
            insertions.setdefault(ann.line, []).append(_render_callout(ann))
        elif ann.kind == "range" and ann.end_line is not None:
            insertions.setdefault(ann.end_line, []).append(_render_callout(ann))

    if not insertions:
        return _wrap_code_block(code_lines, language)

    out_parts: list[str] = []
    cursor = 0
    sorted_insertions = sorted(insertions.keys())
    for ins_line in sorted_insertions:
        chunk = code_lines[cursor:ins_line]
        if chunk:
            out_parts.append(_wrap_code_block(chunk, language))
        for callout in insertions[ins_line]:
            out_parts.append(callout)
        cursor = ins_line

    tail = code_lines[cursor:]
    if tail:
        out_parts.append(_wrap_code_block(tail, language))

    return "\n".join(out_parts)


def _wrap_code_block(lines: list[str], language: str) -> str:
    return f"```{language}\n" + "\n".join(lines) + "\n```\n"


def _render_callout(ann: ActiveAnnotation) -> str:
    # Build pretty location label
    if ann.kind == "single":
        loc = f"line {ann.line}"
    else:
        loc = f"range line {ann.begin_line}-{ann.end_line}"

    refs_line = ""
    if ann.references:
        bits = []
        for k, v in ann.references.items():
            bits.append(f"{k}: {v}")
        refs_line = "*Refs : " + ", ".join(bits) + "*"

    body_lines = ann.body.rstrip().splitlines()
    quoted_body = "\n".join(f"> {ln}" if ln else ">" for ln in body_lines)

    # Wikilink to the annotation source file (without .md suffix)
    wikilink_target = ann.anchor_file.removesuffix(".md")
    if wikilink_target.startswith("docs/vault/"):
        wikilink_target = wikilink_target[len("docs/vault/"):]
    wikilink = f"[[../../{wikilink_target}]]"

    parts = [
        f"> [!note]+ ⚓ {ann.slug} *({loc})*",
        quoted_body,
    ]
    if refs_line:
        parts.append(f"> {refs_line}")
    parts.append(f"> *Source : {wikilink}*")
    return "\n".join(parts) + "\n"


# ---------------------------------------------------------------------------
# appendix
# ---------------------------------------------------------------------------

def _render_appendix(fs: FileSymbols) -> str:
    parts = ["---", "", "## Appendix — symbols & navigation *(auto)*", ""]

    if fs.symbols:
        parts.append("### Symbols")
        for s in fs.symbols:
            parts.append(
                f"- `{s.name}` ({s.kind}) — lines {s.begin_line}-{s.end_line}"
            )
        parts.append("")

    if fs.imports:
        parts.append("### Imports")
        for imp in fs.imports:
            parts.append(f"- `{imp}`")
        parts.append("")

    if fs.exports:
        parts.append("### Exports")
        for exp in fs.exports:
            parts.append(f"- `{exp}`")
        parts.append("")

    return "\n".join(parts)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `render_note` (function) — lines 42-93
- `ontmatter(
    lang` (function) — lines 100-125
- `phan_warning(orphans: ` (function) — lines 132-144
- `h_callouts(
    code_lines` (function) — lines 151-184
- `: list[str], lan` (function) — lines 187-188
- `ctiveAnnotation` (function) — lines 191-221
- `eSymbols) -> str` (function) — lines 228-251

### Imports
- `dataclasses`
- `datetime`
- `typing`
- `yaml`
- `agents.cartographer.walker`

### Exports
- `render_note`
- `ontmatter(
    lang`
- `phan_warning(orphans: `
- `h_callouts(
    code_lines`
- `: list[str], lan`
- `ctiveAnnotation`
- `eSymbols) -> str`
