---
type: code-source
language: python
file_path: agents/cartographer/index_generator.py
git_blob: 1bcaf8eb98d46cda35381598b939c9108e56f997
last_synced: '2026-04-23T09:31:47Z'
loc: 112
annotations: []
imports:
- os
- collections
- datetime
- agents.cartographer.annotation_io
exports:
- _write
- _ts
- generate_orphans_index
- "ate_coverage_index(\n   "
- e_tags_index(annota
tags:
- code
- python
---

# agents/cartographer/index_generator.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/index_generator.py`](../../../agents/cartographer/index_generator.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Generate `_index/orphans.md`, `_index/coverage.md`, `_index/annotations-by-tag.md`.

These files are auto-generated each sync to give a high-level view of vault
state from inside Obsidian.
"""

from __future__ import annotations

import os
from collections import defaultdict
from datetime import datetime, timezone

from agents.cartographer.annotation_io import read_annotation


def _write(path: str, content: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fp:
        fp.write(content)


def _ts() -> str:
    return (
        datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z")
    )


def generate_orphans_index(annotation_paths: list[str], output_path: str) -> None:
    rows: list[str] = []
    for path in annotation_paths:
        meta, _ = read_annotation(path)
        if meta.get("status") != "orphan":
            continue
        slug = meta.get("slug", "?")
        anchors = meta.get("anchors", [])
        if anchors:
            anchor = anchors[0]
            loc_bits = []
            if "line" in anchor:
                loc_bits.append(f"line {anchor['line']}")
            if "begin_line" in anchor and "end_line" in anchor:
                loc_bits.append(f"lines {anchor['begin_line']}-{anchor['end_line']}")
            loc = f" — last seen at {anchor.get('file', '?')}" + (
                f" {' '.join(loc_bits)}" if loc_bits else ""
            )
        else:
            loc = ""
        rows.append(
            f"- `{slug}`{loc} → [[../annotations/_orphans/{slug}]] · "
            f"`/anchor-review {slug}`"
        )

    body = "\n".join(rows) if rows else "_No orphan annotations._"
    _write(
        output_path,
        f"---\ntype: vault-index\nlast_synced: {_ts()}\n---\n\n"
        f"# Orphan annotations\n\n"
        f"Annotations whose source-code marker has disappeared. "
        f"Resolve with `/anchor-review <slug>`.\n\n"
        f"{body}\n",
    )


def generate_coverage_index(
    source_files: list[str],
    files_with_annotations: set[str],
    output_path: str,
) -> None:
    uncovered = sorted(set(source_files) - files_with_annotations)
    if uncovered:
        rows = "\n".join(f"- `{f}`" for f in uncovered)
    else:
        rows = "_All source files have at least one annotation._"

    _write(
        output_path,
        f"---\ntype: vault-index\nlast_synced: {_ts()}\n---\n\n"
        f"# Coverage — source files without annotation\n\n"
        f"{rows}\n",
    )


def generate_tags_index(annotation_paths: list[str], output_path: str) -> None:
    by_tag: dict[str, list[str]] = defaultdict(list)
    for path in annotation_paths:
        meta, _ = read_annotation(path)
        slug = meta.get("slug")
        if not slug:
            continue
        for tag in meta.get("tags", []) or []:
            by_tag[tag].append(slug)

    if not by_tag:
        body = "_No tagged annotations._"
    else:
        sections = []
        for tag in sorted(by_tag):
            slugs = sorted(by_tag[tag])
            section_lines = [f"## #{tag}", ""]
            for slug in slugs:
                section_lines.append(f"- `{slug}`")
            sections.append("\n".join(section_lines))
        body = "\n\n".join(sections)

    _write(
        output_path,
        f"---\ntype: vault-index\nlast_synced: {_ts()}\n---\n\n"
        f"# Annotations by tag\n\n{body}\n",
    )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_write` (function) — lines 16-19
- `_ts` (function) — lines 22-28
- `generate_orphans_index` (function) — lines 31-64
- `ate_coverage_index(
   ` (function) — lines 67-83
- `e_tags_index(annota` (function) — lines 86-112

### Imports
- `os`
- `collections`
- `datetime`
- `agents.cartographer.annotation_io`

### Exports
- `_write`
- `_ts`
- `generate_orphans_index`
- `ate_coverage_index(
   `
- `e_tags_index(annota`
