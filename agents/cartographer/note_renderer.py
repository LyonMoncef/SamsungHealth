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
    coverage_manifest: dict | None = None,
    coverage_raw: dict | None = None,
    spec_index: object | None = None,
) -> str:
    with open(source_path, encoding="utf-8") as fp:
        source = fp.read()
    code_lines = source.splitlines()

    parts: list[str] = []

    file_coverage = (coverage_manifest or {}).get("by_file", {}).get(relative_path)
    file_raw = (coverage_raw or {}).get(relative_path)

    # 1. Frontmatter
    parts.append(_render_frontmatter(
        language=file_symbols.language,
        file_path=relative_path,
        git_blob=git_blob,
        loc=file_symbols.loc,
        annotations=[a.slug for a in active_annotations],
        imports=file_symbols.imports,
        exports=file_symbols.exports,
        coverage_pct=file_coverage["pct"] if file_coverage else None,
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

    # 5. Code + interleaved callouts (annotations + test sub-callouts)
    parts.append(_render_code_with_callouts(
        code_lines=code_lines,
        language=file_symbols.language,
        annotations=active_annotations,
        coverage_raw_for_file=file_raw,
    ))

    # 6. Appendix (symbols + tests-per-symbol if coverage present + specs links)
    parts.append(_render_appendix(
        file_symbols, relative_path, coverage_manifest, spec_index,
    ))

    # 7. Exercises section (only for test files)
    if coverage_manifest and _is_test_file(relative_path):
        ex = _render_exercises_section(relative_path, coverage_manifest)
        if ex:
            parts.append(ex)

    # 8. Validates section (test file → specs validated)
    if spec_index and _is_test_file(relative_path):
        vs = _render_validates_section(relative_path, spec_index)
        if vs:
            parts.append(vs)

    # 9. Spec note: auto-rendered Implementation + Tests sections
    if spec_index and relative_path.startswith("docs/vault/specs/"):
        spec_section = _render_spec_targets(relative_path, spec_index)
        if spec_section:
            parts.append(spec_section)

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
    coverage_pct: float | None = None,
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
    if coverage_pct is not None:
        meta["coverage_pct"] = coverage_pct
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
    coverage_raw_for_file: dict | None = None,
) -> str:
    # Map insertion-line → list of callouts that fire AFTER that line
    # - single  → after the marker line
    # - range   → after end_line
    insertions: dict[int, list[str]] = {}
    for ann in annotations:
        if ann.kind == "single" and ann.line is not None:
            insertions.setdefault(ann.line, []).append(
                _render_callout(ann, coverage_raw_for_file)
            )
        elif ann.kind == "range" and ann.end_line is not None:
            insertions.setdefault(ann.end_line, []).append(
                _render_callout(ann, coverage_raw_for_file)
            )

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


def _render_callout(ann: ActiveAnnotation, coverage_raw_for_file: dict | None = None) -> str:
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

    # Sub-callout: tests covering the annotated range/line
    if coverage_raw_for_file:
        from agents.cartographer.coverage_map import tests_for_range
        if ann.kind == "single" and ann.line is not None:
            tests = tests_for_range(coverage_raw_for_file, ann.line, ann.line)
        elif ann.kind == "range" and ann.begin_line is not None and ann.end_line is not None:
            tests = tests_for_range(coverage_raw_for_file, ann.begin_line, ann.end_line)
        else:
            tests = []
        if tests:
            parts.append("> ")
            parts.append(f"> > [!test]+ Tested by ({len(tests)})")
            for t in tests[:10]:
                parts.append(f"> > - `{t}`")
            if len(tests) > 10:
                parts.append(f"> > _… +{len(tests) - 10} more_")

    return "\n".join(parts) + "\n"


# ---------------------------------------------------------------------------
# appendix
# ---------------------------------------------------------------------------

def _render_appendix(
    fs: FileSymbols,
    relative_path: str = "",
    coverage_manifest: dict | None = None,
    spec_index: object | None = None,
) -> str:
    parts = ["---", "", "## Appendix — symbols & navigation *(auto)*", ""]

    by_symbol = (coverage_manifest or {}).get("by_symbol", {})
    by_file_to_specs = getattr(spec_index, "by_file_to_specs", {}) if spec_index else {}
    file_specs = by_file_to_specs.get(relative_path, [])

    if file_specs:
        parts.append("### Implements specs")
        for entry in file_specs:
            sym_part = (
                f" — symbols: " + ", ".join(f"`{s}`" for s in entry["symbols"])
                if entry.get("symbols") else ""
            )
            parts.append(f"- [[../../specs/{entry['slug']}]]{sym_part}")
        parts.append("")

    if fs.symbols:
        parts.append("### Symbols")
        symbol_to_specs: dict[str, list[str]] = {}
        for entry in file_specs:
            for sym in entry.get("symbols", []) or []:
                symbol_to_specs.setdefault(sym, []).append(entry["slug"])
        for s in fs.symbols:
            line = (
                f"- `{s.name}` ({s.kind}) — lines {s.begin_line}-{s.end_line}"
            )
            sym_cov = by_symbol.get(f"{relative_path}::{s.name}")
            if sym_cov:
                tests = sym_cov.get("tests", [])
                if tests:
                    line += f" · **Tested by ({len(tests)})**: " + ", ".join(
                        f"`{t}`" for t in tests[:5]
                    )
                    if len(tests) > 5:
                        line += f" _+{len(tests) - 5}_"
                else:
                    line += " · ⚠️ no test"
            if s.name in symbol_to_specs:
                line += " · **Specs**: " + ", ".join(
                    f"[[../../specs/{slug}|{slug}]]" for slug in symbol_to_specs[s.name]
                )
            parts.append(line)
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


# ---------------------------------------------------------------------------
# Test files: "Exercises" section (inverse map: this test → covered symbols)
# ---------------------------------------------------------------------------

def _is_test_file(relative_path: str) -> bool:
    base = relative_path.rsplit("/", 1)[-1]
    return base.startswith("test_") or base.endswith("_test.py")


def _render_validates_section(relative_path: str, spec_index: object) -> str:
    by_test_to_specs = getattr(spec_index, "by_test_to_specs", {})
    matches = by_test_to_specs.get(relative_path, [])
    if not matches:
        return ""
    parts = ["", "## Validates specs *(auto — declared by spec)*", ""]
    for entry in matches:
        details = []
        if entry.get("classes"):
            details.append("classes: " + ", ".join(f"`{c}`" for c in entry["classes"]))
        if entry.get("methods"):
            details.append("methods: " + ", ".join(f"`{m}`" for m in entry["methods"]))
        suffix = f" — {' · '.join(details)}" if details else ""
        parts.append(f"- [[../../specs/{entry['slug']}]]{suffix}")
    return "\n".join(parts) + "\n"


def _render_spec_targets(relative_path: str, spec_index: object) -> str:
    """For a spec note, render its declared `implements` + `tested_by`."""
    slug = relative_path.removesuffix(".md").rsplit("/", 1)[-1]
    by_slug = getattr(spec_index, "by_slug", {})
    meta = by_slug.get(slug)
    if meta is None:
        return ""
    parts = ["", "## Targets *(auto — from frontmatter)*", ""]

    if meta.implements:
        parts.append("### Implementation")
        for impl in meta.implements:
            sym_part = (
                f" — symbols: " + ", ".join(f"`{s}`" for s in impl.symbols)
                if impl.symbols else ""
            )
            range_part = (
                f" (lines {impl.line_range[0]}-{impl.line_range[1]})"
                if impl.line_range else ""
            )
            file_link = f"[[../code/{impl.file.removesuffix('.py').removesuffix('.js').removesuffix('.kt')}|{impl.file}]]"
            parts.append(f"- {file_link}{sym_part}{range_part}")
        parts.append("")

    if meta.tested_by:
        parts.append("### Tests")
        for tb in meta.tested_by:
            details = []
            if tb.classes:
                details.append("classes: " + ", ".join(f"`{c}`" for c in tb.classes))
            if tb.methods:
                details.append("methods: " + ", ".join(f"`{m}`" for m in tb.methods))
            suffix = f" — {' · '.join(details)}" if details else ""
            file_link = f"[[../code/{tb.file.removesuffix('.py').removesuffix('.js').removesuffix('.kt')}|{tb.file}]]"
            parts.append(f"- {file_link}{suffix}")
        parts.append("")

    return "\n".join(parts)


def _render_exercises_section(
    relative_path: str, coverage_manifest: dict
) -> str:
    by_test = coverage_manifest.get("by_test", {}) or {}
    # Test ID format from coverage.py: "<dotted_module>.<func>"
    # → match by stripped path (strip extension, replace / with .)
    stem = relative_path.removesuffix(".py").replace("/", ".")
    # Tolerant prefix match: a test_id starts with the file's dotted path
    matches: dict[str, list[dict[str, str]]] = {}
    for test_id, hits in by_test.items():
        # Normalise: drop leading "tests." if present (pytest's collected name varies)
        norm = test_id
        if norm.startswith("tests."):
            norm_short = norm[len("tests."):]
        else:
            norm_short = norm
        # Match by basename (last component): test_id contains the file basename
        file_base = stem.rsplit(".", 1)[-1]
        if file_base in norm or file_base in norm_short:
            matches[test_id] = hits

    if not matches:
        return ""

    parts = ["", "## Exercises *(auto — this test file touches)*", ""]
    for test_id in sorted(matches):
        parts.append(f"### `{test_id}`")
        for hit in matches[test_id]:
            parts.append(f"- [[../../code/{hit['file'].removesuffix('.py')}|{hit['file']}]] · `{hit['symbol']}`")
        parts.append("")
    return "\n".join(parts)
