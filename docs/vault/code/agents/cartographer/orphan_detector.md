---
type: code-source
language: python
file_path: agents/cartographer/orphan_detector.py
git_blob: e35f0b6a39c0845f7b1a32d9f0b7dfa106c2328e
last_synced: '2026-04-23T09:31:47Z'
loc: 51
annotations: []
imports:
- dataclasses
- agents.cartographer.annotation_io
- agents.cartographer.markers
exports:
- detect_orphans
tags:
- code
- python
---

# agents/cartographer/orphan_detector.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/orphan_detector.py`](../../../agents/cartographer/orphan_detector.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Detect orphan transitions across the whole repo.

Given:
- a mapping `markers_per_file` : `{relative_path: [Marker, ...]}`
- the list of all annotation files on disk

Returns a `OrphanDiff` :
- `new_orphans`      : annotation slugs whose marker is no longer found
- `resolved_orphans` : annotation slugs that were `status: orphan` but now have
                        a matching marker again
"""

from __future__ import annotations

from dataclasses import dataclass, field

from agents.cartographer.annotation_io import read_annotation
from agents.cartographer.markers import Marker


@dataclass
class OrphanDiff:
    new_orphans: list[str] = field(default_factory=list)
    resolved_orphans: list[str] = field(default_factory=list)


def detect_orphans(
    markers_per_file: dict[str, list[Marker]],
    annotation_paths: list[str],
) -> OrphanDiff:
    diff = OrphanDiff()

    # Build the global set of slugs currently present in any source file.
    slugs_in_code: set[str] = set()
    for markers in markers_per_file.values():
        for m in markers:
            slugs_in_code.add(m.slug)

    for path in annotation_paths:
        meta, _ = read_annotation(path)
        slug = meta.get("slug")
        status = meta.get("status", "active")
        if not slug:
            continue

        if status == "active" and slug not in slugs_in_code:
            diff.new_orphans.append(slug)
        elif status == "orphan" and slug in slugs_in_code:
            diff.resolved_orphans.append(slug)

    return diff
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `detect_orphans` (function) — lines 27-51

### Imports
- `dataclasses`
- `agents.cartographer.annotation_io`
- `agents.cartographer.markers`

### Exports
- `detect_orphans`
