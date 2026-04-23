---
type: code-source
language: python
file_path: agents/cartographer/anchor_resolver.py
git_blob: 8ca261ec0ed4d7d43f38f8fe5678ec0fa22c5644
last_synced: '2026-04-23T10:21:38Z'
loc: 88
annotations: []
imports:
- dataclasses
- agents.cartographer.annotation_io
- agents.cartographer.markers
exports:
- resolve_anchors_for_file
tags:
- code
- python
coverage_pct: 95.45454545454545
---

# agents/cartographer/anchor_resolver.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/anchor_resolver.py`](../../../agents/cartographer/anchor_resolver.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Resolve marker ↔ annotation state for one source file.

Compares the markers found in the current source code with the annotations
already on disk for that file. Returns three buckets:

- `active`    : annotations whose marker is still present (line numbers are
                refreshed to the current location)
- `orphans`   : annotations whose marker has disappeared
- `unmatched` : marker slugs in the code that don't have an annotation file yet
"""

from __future__ import annotations

from dataclasses import dataclass, field

from agents.cartographer.annotation_io import read_annotation
from agents.cartographer.markers import Marker


@dataclass
class ResolvedAnchor:
    slug: str
    kind: str
    line: int | None = None
    begin_line: int | None = None
    end_line: int | None = None
    annotation_path: str = ""


@dataclass
class OrphanedAnnotation:
    slug: str
    annotation_path: str
    last_seen: dict


@dataclass
class ResolveResult:
    active: dict[str, ResolvedAnchor] = field(default_factory=dict)
    orphans: list[OrphanedAnnotation] = field(default_factory=list)
    unmatched: list[str] = field(default_factory=list)


def resolve_anchors_for_file(
    file: str,
    markers: list[Marker],
    annotation_paths: list[str],
) -> ResolveResult:
    result = ResolveResult()

    markers_by_slug: dict[str, Marker] = {m.slug: m for m in markers}
    annotations_by_slug: dict[str, tuple[str, dict]] = {}

    for path in annotation_paths:
        meta, _ = read_annotation(path)
        slug = meta.get("slug")
        if not slug:
            continue
        # only consider annotations that target this file
        if not any(a.get("file") == file for a in meta.get("anchors", [])):
            continue
        annotations_by_slug[slug] = (path, meta)

    for slug, marker in markers_by_slug.items():
        if slug in annotations_by_slug:
            path, _ = annotations_by_slug[slug]
            result.active[slug] = ResolvedAnchor(
                slug=slug,
                kind=marker.kind,
                line=marker.line,
                begin_line=marker.begin_line,
                end_line=marker.end_line,
                annotation_path=path,
            )
        else:
            result.unmatched.append(slug)

    for slug, (path, meta) in annotations_by_slug.items():
        if slug not in markers_by_slug:
            anchor = next(
                (a for a in meta.get("anchors", []) if a.get("file") == file),
                {},
            )
            result.orphans.append(
                OrphanedAnnotation(slug=slug, annotation_path=path, last_seen=anchor)
            )

    return result
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `resolve_anchors_for_file` (function) — lines 44-88 · **Tested by (10)**: `test_anchor_resolver.TestResolveAnchorsForFile.test_active_annotation_with_marker_present`, `test_anchor_resolver.TestResolveAnchorsForFile.test_orphan_when_marker_missing`, `test_anchor_resolver.TestResolveAnchorsForFile.test_range_marker_updates_begin_end_lines`, `test_anchor_resolver.TestResolveAnchorsForFile.test_unmatched_when_marker_without_annotation`, `test_cli.TestMirror.test_mirror_copies_vault_to_target` _+5_

### Imports
- `dataclasses`
- `agents.cartographer.annotation_io`
- `agents.cartographer.markers`

### Exports
- `resolve_anchors_for_file`
