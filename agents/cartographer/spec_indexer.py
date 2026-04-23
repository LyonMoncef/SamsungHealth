"""Index `docs/vault/specs/*.md` frontmatter into a global lookup.

The index supports three navigation directions in the rendered vault notes:
- code note → specs that implement (`by_file_to_specs`)
- test note → specs validated  (`by_test_to_specs`)
- spec note → its declared targets (`by_slug`)

Drift detection (`detect_implements_drift`) returns deviations to feed
plan-keeper's `spec_implements_drift` deviation type. `untested_specs(idx)`
returns the list of slugs without any `tested_by:` entry — feeds plan-keeper's
`untested_spec` deviation.
"""

from __future__ import annotations

import os
from dataclasses import dataclass, field
from glob import glob

from agents.cartographer.annotation_io import read_annotation
from agents.cartographer.walker import walk_file
from agents.contracts.spec import SpecMeta


@dataclass
class SpecIndex:
    by_slug: dict[str, SpecMeta] = field(default_factory=dict)
    by_file_to_specs: dict[str, list[dict]] = field(default_factory=dict)
    by_test_to_specs: dict[str, list[dict]] = field(default_factory=dict)


def load_spec(path: str) -> SpecMeta | None:
    meta_dict, _ = read_annotation(path)
    if not meta_dict:
        return None
    if meta_dict.get("type") == "stub":
        return SpecMeta(type="stub")  # signals exclusion to caller
    # Derive slug from filename if absent
    if "slug" not in meta_dict:
        base = os.path.basename(path).removesuffix(".md")
        meta_dict["slug"] = base
    try:
        return SpecMeta(**meta_dict)
    except Exception:
        return None


def build_index(spec_paths: list[str]) -> SpecIndex:
    idx = SpecIndex()
    for path in spec_paths:
        meta = load_spec(path)
        if meta is None or meta.type == "stub":
            continue
        slug = meta.slug or os.path.basename(path).removesuffix(".md")
        idx.by_slug[slug] = meta

        for impl in meta.implements:
            idx.by_file_to_specs.setdefault(impl.file, []).append({
                "slug": slug,
                "symbols": impl.symbols,
                "line_range": impl.line_range,
            })

        for tb in meta.tested_by:
            idx.by_test_to_specs.setdefault(tb.file, []).append({
                "slug": slug,
                "classes": tb.classes,
                "methods": tb.methods,
            })
    return idx


def discover_spec_paths(vault_root: str) -> list[str]:
    return sorted(glob(os.path.join(vault_root, "specs", "*.md")))


def detect_implements_drift(
    spec_slug: str,
    spec_meta: SpecMeta,
    repo_root: str,
) -> list[dict]:
    """For each `implements:` entry, verify file + symbols still exist.

    Returns a list of drift records: `{spec_slug, target_file, missing_symbol?}`.
    File missing → 1 record per implements entry.
    Symbol missing in existing file → 1 record per missing symbol.
    """
    out: list[dict] = []
    for impl in spec_meta.implements:
        full = os.path.join(repo_root, impl.file)
        if not os.path.isfile(full):
            out.append({
                "spec_slug": spec_slug,
                "target_file": impl.file,
                "reason": "file_missing",
            })
            continue
        if impl.symbols:
            language = _infer_language_for_walker(impl.file)
            if language is None:
                continue
            try:
                fs = walk_file(full, language=language)
            except Exception:
                continue
            present = {s.name for s in fs.symbols}
            for sym in impl.symbols:
                if sym not in present:
                    out.append({
                        "spec_slug": spec_slug,
                        "target_file": impl.file,
                        "missing_symbol": sym,
                        "reason": "symbol_missing",
                    })
    return out


def untested_specs(idx: SpecIndex) -> list[str]:
    return sorted(
        slug for slug, meta in idx.by_slug.items()
        if not meta.tested_by and meta.type == "spec"
    )


def _infer_language_for_walker(file_path: str) -> str | None:
    if file_path.endswith(".py"):
        return "python"
    if file_path.endswith((".js", ".mjs", ".cjs")):
        return "javascript"
    if file_path.endswith((".kt", ".kts")):
        return "kotlin"
    return None
