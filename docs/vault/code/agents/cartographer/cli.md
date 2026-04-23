---
type: code-source
language: python
file_path: agents/cartographer/cli.py
git_blob: e4f5539a22aabd4e465c6b08a059f56a7fa64c3c
last_synced: '2026-04-23T09:44:51Z'
loc: 445
annotations: []
imports:
- argparse
- os
- shutil
- subprocess
- sys
- glob
- typing
- agents.cartographer.anchor_resolver
- agents.cartographer.annotation_io
- agents.cartographer.index_generator
- agents.cartographer.markers
- agents.cartographer.note_renderer
- agents.cartographer.orphan_detector
- agents.cartographer.walker
- agents.contracts.cartographer
exports:
- run
- _discover_sources
- _discover_annotations
- _render_one
- _build_active
- _empty_file_symbols
- _all_markers
- _write_indexes
- _strip_ext
- _load_coverage
- _mirror_vault
- _git_blob_sha
- _git_short_sha
- _parse_args
- main
tags:
- code
- python
coverage_pct: 67.05202312138728
---

# agents/cartographer/cli.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/cli.py`](../../../agents/cartographer/cli.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""CLI entry point for `code-cartographer`.

Usage:
    python -m agents.cartographer.cli --full
    python -m agents.cartographer.cli --diff server/x.py server/y.py
    python -m agents.cartographer.cli --check
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from glob import glob
from typing import Literal

from agents.cartographer.anchor_resolver import resolve_anchors_for_file
from agents.cartographer.annotation_io import read_annotation
from agents.cartographer.index_generator import (
    generate_coverage_index,
    generate_coverage_map_index,
    generate_orphans_index,
    generate_tags_index,
)
from agents.cartographer.markers import (
    MarkerParseError,
    infer_language,
    parse_markers,
)
from agents.cartographer.note_renderer import (
    ActiveAnnotation,
    OrphanAnnotation,
    render_note,
)
from agents.cartographer.orphan_detector import detect_orphans
from agents.cartographer.walker import walk_file
from agents.contracts.cartographer import CartographyReport


# Default globs match the SamsungHealth repo layout. Tests may override.
DEFAULT_SOURCE_GLOBS = [
    "server/**/*.py",
    "agents/**/*.py",
    "scripts/**/*.py",
    "tests/**/*.py",
    "static/**/*.js",
    "static/**/*.html",
    "static/**/*.css",
    "android-app/**/*.kt",
    "android-app/**/test/**/*.kt",
]


def run(
    mode: Literal["full", "diff", "check"],
    repo_root: str,
    vault_root: str,
    source_globs: list[str] | None = None,
    diff_files: list[str] | None = None,
    mirror_to: str | None = None,
) -> CartographyReport:
    source_globs = source_globs or DEFAULT_SOURCE_GLOBS

    coverage_manifest, coverage_raw = _load_coverage(vault_root, repo_root)

    if mode == "diff":
        source_files = [
            os.path.relpath(f, repo_root) if os.path.isabs(f) else f
            for f in (diff_files or [])
            if infer_language(f) is not None
        ]
    else:
        source_files = _discover_sources(repo_root, source_globs)

    annotation_paths = _discover_annotations(vault_root)

    notes_generated = 0
    notes_skipped = 0
    parse_errors: list[dict] = []

    if mode in ("full", "diff"):
        for rel in source_files:
            try:
                _render_one(
                    repo_root, vault_root, rel, annotation_paths,
                    coverage_manifest=coverage_manifest,
                    coverage_raw=coverage_raw,
                )
                notes_generated += 1
            except Exception as exc:
                parse_errors.append({"file": rel, "error": str(exc)})
                notes_skipped += 1

        # Indexes (best effort) — only in full mode (diff is incremental)
        if mode == "full":
            _write_indexes(
                repo_root=repo_root,
                vault_root=vault_root,
                source_files=source_files,
                annotation_paths=annotation_paths,
            )

    # Orphan diff (always)
    markers_per_file = _all_markers(repo_root, source_files)
    diff = detect_orphans(
        markers_per_file=markers_per_file,
        annotation_paths=annotation_paths,
    )

    if mode == "check":
        overall = "failed" if (diff.new_orphans or parse_errors) else "complete"
    else:
        overall = "complete" if not parse_errors else "partial"

    if mode in ("full", "diff") and mirror_to:
        _mirror_vault(vault_root, mirror_to)

    return CartographyReport(
        task_id=os.environ.get("CARTOGRAPHER_TASK_ID", "cli"),
        agent="code-cartographer",
        status="success" if overall != "failed" else "partial",
        summary=(
            f"mode={mode} files={len(source_files)} "
            f"notes={notes_generated} new_orphans={len(diff.new_orphans)}"
        )[:500],
        notes_generated=notes_generated,
        notes_updated=0,
        notes_skipped=notes_skipped,
        annotations_processed=len(annotation_paths),
        new_orphans=list(diff.new_orphans),
        resolved_orphans=list(diff.resolved_orphans),
        parse_errors=parse_errors,
        overall=overall,
        next_recommended=("anchor-review" if diff.new_orphans else "commit"),
    )


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

def _discover_sources(repo_root: str, source_globs: list[str]) -> list[str]:
    out: set[str] = set()
    for pattern in source_globs:
        full = os.path.join(repo_root, pattern)
        for match in glob(full, recursive=True):
            rel = os.path.relpath(match, repo_root)
            if infer_language(rel) is not None:
                out.add(rel)
    return sorted(out)


def _discover_annotations(vault_root: str) -> list[str]:
    base = os.path.join(vault_root, "annotations")
    if not os.path.isdir(base):
        return []
    out: list[str] = []
    for root, _dirs, files in os.walk(base):
        for f in files:
            if f.endswith(".md"):
                out.append(os.path.join(root, f))
    return sorted(out)


def _render_one(
    repo_root: str,
    vault_root: str,
    rel: str,
    annotation_paths: list[str],
    coverage_manifest: dict | None = None,
    coverage_raw: dict | None = None,
) -> None:
    src_abs = os.path.join(repo_root, rel)
    language = infer_language(rel)
    if language is None:
        return

    with open(src_abs, encoding="utf-8") as fp:
        source = fp.read()

    try:
        markers = parse_markers(source, language=language, file_path=rel)
    except MarkerParseError:
        markers = []

    fs = walk_file(src_abs, language=language) if language in ("python", "javascript", "kotlin") \
        else _empty_file_symbols(src_abs, language, source)

    resolved = resolve_anchors_for_file(
        file=rel, markers=markers, annotation_paths=annotation_paths,
    )

    actives = [
        _build_active(slug, anchor, annotation_paths)
        for slug, anchor in resolved.active.items()
    ]
    orphans = [
        OrphanAnnotation(
            slug=o.slug,
            last_seen_line=str(o.last_seen.get("line", "")) or
                           f"{o.last_seen.get('begin_line', '')}-{o.last_seen.get('end_line', '')}".strip("-"),
            last_seen_commit=_git_short_sha(repo_root) or "",
        )
        for o in resolved.orphans
    ]

    out_path = os.path.join(vault_root, "code", _strip_ext(rel) + ".md")
    os.makedirs(os.path.dirname(out_path), exist_ok=True)

    note = render_note(
        source_path=src_abs,
        relative_path=rel,
        file_symbols=fs,
        git_blob=_git_blob_sha(repo_root, rel) or "",
        active_annotations=actives,
        orphans=orphans,
        coverage_manifest=coverage_manifest,
        coverage_raw=coverage_raw,
    )
    with open(out_path, "w", encoding="utf-8") as fp:
        fp.write(note)


def _build_active(slug, anchor, annotation_paths) -> ActiveAnnotation:
    body = ""
    refs: dict = {}
    if anchor.annotation_path:
        meta, body = read_annotation(anchor.annotation_path)
        refs = meta.get("references", {}) or {}
    return ActiveAnnotation(
        slug=slug,
        kind=anchor.kind,
        body=body,
        anchor_file=anchor.annotation_path,
        references=refs,
        line=anchor.line,
        begin_line=anchor.begin_line,
        end_line=anchor.end_line,
    )


def _empty_file_symbols(src_abs: str, language: str, source: str):
    from agents.cartographer.walker import FileSymbols
    return FileSymbols(file=src_abs, language=language, loc=len(source.splitlines()))  # type: ignore[arg-type]


def _all_markers(repo_root: str, source_files: list[str]) -> dict:
    markers_per_file: dict = {}
    for rel in source_files:
        full = os.path.join(repo_root, rel)
        if not os.path.isfile(full):
            continue
        lang = infer_language(rel)
        if lang is None:
            continue
        with open(full, encoding="utf-8") as fp:
            source = fp.read()
        try:
            markers_per_file[rel] = parse_markers(source, language=lang, file_path=rel)
        except MarkerParseError:
            markers_per_file[rel] = []
    return markers_per_file


def _write_indexes(
    repo_root: str,
    vault_root: str,
    source_files: list[str],
    annotation_paths: list[str],
) -> None:
    files_with_annotations: set[str] = set()
    for path in annotation_paths:
        meta, _ = read_annotation(path)
        for a in meta.get("anchors", []) or []:
            f = a.get("file")
            if f:
                files_with_annotations.add(f)

    generate_orphans_index(
        annotation_paths=annotation_paths,
        output_path=os.path.join(vault_root, "_index", "orphans.md"),
    )
    generate_coverage_index(
        source_files=source_files,
        files_with_annotations=files_with_annotations,
        output_path=os.path.join(vault_root, "_index", "coverage.md"),
    )
    generate_tags_index(
        annotation_paths=annotation_paths,
        output_path=os.path.join(vault_root, "_index", "annotations-by-tag.md"),
    )

    # Coverage map index (only if manifest exists)
    manifest_path = os.path.join(vault_root, "_index", "coverage-map.json")
    if os.path.isfile(manifest_path):
        try:
            import json
            with open(manifest_path, encoding="utf-8") as fp:
                manifest = json.load(fp)
            generate_coverage_map_index(
                coverage_manifest=manifest,
                output_path=os.path.join(vault_root, "_index", "coverage-map.md"),
            )
        except Exception:
            pass


def _strip_ext(path: str) -> str:
    return os.path.splitext(path)[0]


def _load_coverage(vault_root: str, repo_root: str) -> tuple[dict | None, dict | None]:
    """Load `coverage-map.json` (manifest) + raw `coverage.json` if present.

    Both are gitignored. Renderer degrades gracefully when absent.
    The raw file holds per-line `contexts` used for annotation range sub-callouts.
    """
    import json

    manifest = None
    manifest_path = os.path.join(vault_root, "_index", "coverage-map.json")
    if os.path.isfile(manifest_path):
        try:
            with open(manifest_path, encoding="utf-8") as fp:
                manifest = json.load(fp)
        except Exception:
            manifest = None

    raw = None
    raw_path = os.path.join(repo_root, "coverage.json")
    if os.path.isfile(raw_path):
        try:
            with open(raw_path, encoding="utf-8") as fp:
                raw_full = json.load(fp)
            raw = raw_full.get("files", {})
        except Exception:
            raw = None

    return manifest, raw


def _mirror_vault(vault_root: str, mirror_to: str) -> None:
    """One-way copy of `vault_root/` → `mirror_to/`.

    Replaces the target directory entirely so that stale notes (deleted source
    files, renamed slugs) don't linger in the mirror. Read-only target —
    edits in the mirror are lost on next sync.
    """
    if not os.path.isdir(vault_root):
        return
    if os.path.exists(mirror_to):
        shutil.rmtree(mirror_to)
    shutil.copytree(vault_root, mirror_to)
    # Drop a README at the root so opening the mirror in Obsidian shows the warning
    readme = os.path.join(mirror_to, "MIRROR-README.md")
    with open(readme, "w", encoding="utf-8") as fp:
        fp.write(
            "# ⚠️ Read-only mirror\n\n"
            f"Ce dossier est un **miroir auto-généré** de `{vault_root}` "
            f"(repo WSL). Toute édition ici est **perdue** au prochain `code-cartographer` sync.\n\n"
            "Pour éditer une annotation, utiliser `/annotate edit <slug>` "
            "côté repo WSL (ou éditer `docs/vault/annotations/...md` directement).\n"
        )


def _git_blob_sha(repo_root: str, rel: str) -> str | None:
    try:
        out = subprocess.check_output(
            ["git", "ls-files", "-s", rel],
            cwd=repo_root, stderr=subprocess.DEVNULL,
        ).decode().strip()
        if not out:
            return None
        # format: "<mode> <sha> <stage>\t<path>"
        return out.split()[1]
    except Exception:
        return None


def _git_short_sha(repo_root: str) -> str | None:
    try:
        out = subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=repo_root, stderr=subprocess.DEVNULL,
        ).decode().strip()
        return out or None
    except Exception:
        return None


# ---------------------------------------------------------------------------
# argv entry point
# ---------------------------------------------------------------------------

def _parse_args(argv: list[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(prog="code-cartographer")
    g = p.add_mutually_exclusive_group(required=True)
    g.add_argument("--full", action="store_true")
    g.add_argument("--diff", nargs="+", metavar="FILE")
    g.add_argument("--check", action="store_true")
    p.add_argument("--repo-root", default=os.getcwd())
    p.add_argument("--vault-root", default=os.path.join(os.getcwd(), "docs", "vault"))
    p.add_argument(
        "--mirror-to",
        default=os.environ.get("CARTOGRAPHER_MIRROR_TO"),
        help="One-way copy target (default: $CARTOGRAPHER_MIRROR_TO env var, none if unset)",
    )
    return p.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    ns = _parse_args(argv if argv is not None else sys.argv[1:])
    mode: Literal["full", "diff", "check"]
    if ns.full:
        mode = "full"
        diff_files = None
    elif ns.diff:
        mode = "diff"
        diff_files = ns.diff
    else:
        mode = "check"
        diff_files = None

    report = run(
        mode=mode,
        repo_root=ns.repo_root,
        vault_root=ns.vault_root,
        diff_files=diff_files,
        mirror_to=ns.mirror_to,
    )
    print(
        f"[code-cartographer] mode={mode} overall={report.overall} "
        f"notes={report.notes_generated}/{report.annotations_processed} "
        f"new_orphans={len(report.new_orphans)} resolved={len(report.resolved_orphans)}"
    )
    if report.parse_errors:
        for err in report.parse_errors:
            print(f"  ! parse error in {err['file']}: {err['error']}", file=sys.stderr)
    return 0 if report.overall != "failed" else 1


if __name__ == "__main__":
    sys.exit(main())
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `run` (function) — lines 56-137 · **Tested by (8)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunCheck.test_check_passes_when_clean`, `test_cli.TestRunCheck.test_check_returns_failed_on_orphan` _+3_
- `_discover_sources` (function) — lines 144-152 · **Tested by (7)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunCheck.test_check_passes_when_clean`, `test_cli.TestRunCheck.test_check_returns_failed_on_orphan` _+2_
- `_discover_annotations` (function) — lines 155-164 · **Tested by (8)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunCheck.test_check_passes_when_clean`, `test_cli.TestRunCheck.test_check_returns_failed_on_orphan` _+3_
- `_render_one` (function) — lines 167-223 · **Tested by (6)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunDiff.test_diff_only_renders_listed_files`, `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file` _+1_
- `_build_active` (function) — lines 226-241 · ⚠️ no test
- `_empty_file_symbols` (function) — lines 244-246 · ⚠️ no test
- `_all_markers` (function) — lines 249-264 · **Tested by (8)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunCheck.test_check_passes_when_clean`, `test_cli.TestRunCheck.test_check_returns_failed_on_orphan` _+3_
- `_write_indexes` (function) — lines 267-307 · **Tested by (5)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file`, `test_cli.TestRunFull.test_full_generates_index_files`
- `_strip_ext` (function) — lines 310-311 · **Tested by (6)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunDiff.test_diff_only_renders_listed_files`, `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file` _+1_
- `_load_coverage` (function) — lines 314-341
- `_mirror_vault` (function) — lines 344-365 · **Tested by (2)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`
- `_git_blob_sha` (function) — lines 368-379 · **Tested by (6)**: `test_cli.TestMirror.test_mirror_copies_vault_to_target`, `test_cli.TestMirror.test_mirror_overwrites_existing`, `test_cli.TestMirror.test_mirror_skipped_when_none`, `test_cli.TestRunDiff.test_diff_only_renders_listed_files`, `test_cli.TestRunFull.test_full_creates_notes_for_each_source_file` _+1_
- `_git_short_sha` (function) — lines 382-390 · ⚠️ no test
- `_parse_args` (function) — lines 397-410 · ⚠️ no test
- `main` (function) — lines 413-441 · ⚠️ no test

### Imports
- `argparse`
- `os`
- `shutil`
- `subprocess`
- `sys`
- `glob`
- `typing`
- `agents.cartographer.anchor_resolver`
- `agents.cartographer.annotation_io`
- `agents.cartographer.index_generator`
- `agents.cartographer.markers`
- `agents.cartographer.note_renderer`
- `agents.cartographer.orphan_detector`
- `agents.cartographer.walker`
- `agents.contracts.cartographer`

### Exports
- `run`
- `_discover_sources`
- `_discover_annotations`
- `_render_one`
- `_build_active`
- `_empty_file_symbols`
- `_all_markers`
- `_write_indexes`
- `_strip_ext`
- `_load_coverage`
- `_mirror_vault`
- `_git_blob_sha`
- `_git_short_sha`
- `_parse_args`
- `main`
