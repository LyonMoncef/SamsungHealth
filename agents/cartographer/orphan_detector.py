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
