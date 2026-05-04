"""CRUD on annotation markdown files (`docs/vault/annotations/...`).

Frontmatter is YAML between two `---` fences. Body is everything after.
The functions here are the only writers of annotation files outside of
`marker_injector` (which only edits source code, not annotations).
"""

from __future__ import annotations

import os
from datetime import date, datetime, timezone
from typing import Any

import yaml

from agents.contracts.cartographer import AnchorLocation


_FENCE = "---"


def resolve_annotation_path(
    slug: str,
    anchors: list[AnchorLocation],
    vault_root: str,
) -> str:
    """Compute the canonical on-disk path for an annotation file.

    - 0 anchors           → `<vault_root>/annotations/_orphans/<slug>.md`
    - 1 anchor            → `<vault_root>/annotations/<dirname>/<basename_no_ext>/<slug>.md`
    - 2+ different files  → `<vault_root>/annotations/_cross/<slug>.md`
    """
    base = f"{vault_root}/annotations"

    if not anchors:
        return f"{base}/_orphans/{slug}.md"

    files = {a.file for a in anchors}
    if len(files) > 1:
        return f"{base}/_cross/{slug}.md"

    src = next(iter(files))
    dirname, fname = os.path.split(src)
    stem, _ = os.path.splitext(fname)
    return f"{base}/{dirname}/{stem}/{slug}.md"


def read_annotation(path: str) -> tuple[dict[str, Any], str]:
    """Return `(frontmatter_dict, body_str)`. Raises FileNotFoundError if missing."""
    with open(path, encoding="utf-8") as fp:
        text = fp.read()

    if not text.startswith(_FENCE + "\n"):
        return {}, text

    end = text.find("\n" + _FENCE + "\n", len(_FENCE) + 1)
    if end == -1:
        return {}, text

    raw_meta = text[len(_FENCE) + 1 : end]
    body = text[end + len(_FENCE) + 2 :]
    meta = yaml.safe_load(raw_meta) or {}
    return meta, body


def write_annotation(
    path: str,
    slug: str,
    anchors: list[AnchorLocation],
    scope: str,
    created_by: str,
    body: str,
    *,
    references: dict | None = None,
    tags: list[str] | None = None,
    status: str = "active",
) -> None:
    """Create (or overwrite) an annotation file with auto frontmatter."""
    os.makedirs(os.path.dirname(path), exist_ok=True)

    meta = {
        "slug": slug,
        "type": "annotation",
        "created": date.today().isoformat(),
        "created_by": created_by,
        "last_verified": datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z"),
        "status": status,
        "anchors": [_anchor_to_dict(a) for a in anchors],
        "scope": scope,
        "references": references or {},
        "tags": tags or [],
    }

    serialized = yaml.safe_dump(meta, sort_keys=False, allow_unicode=True).rstrip()
    with open(path, "w", encoding="utf-8") as fp:
        fp.write(f"{_FENCE}\n{serialized}\n{_FENCE}\n\n{body.rstrip()}\n")


def update_status(path: str, new_status: str) -> None:
    """Patch `status:` in the frontmatter of an existing annotation."""
    meta, body = read_annotation(path)
    meta["status"] = new_status
    serialized = yaml.safe_dump(meta, sort_keys=False, allow_unicode=True).rstrip()
    with open(path, "w", encoding="utf-8") as fp:
        fp.write(f"{_FENCE}\n{serialized}\n{_FENCE}\n\n{body.rstrip()}\n")


def _anchor_to_dict(a: AnchorLocation) -> dict:
    out: dict = {"file": a.file, "kind": a.kind}
    if a.line is not None:
        out["line"] = a.line
    if a.begin_line is not None:
        out["begin_line"] = a.begin_line
    if a.end_line is not None:
        out["end_line"] = a.end_line
    return out
