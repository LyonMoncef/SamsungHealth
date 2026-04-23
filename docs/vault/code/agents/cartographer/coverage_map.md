---
type: code-source
language: python
file_path: agents/cartographer/coverage_map.py
git_blob: 0806443a75fa81684b276329eaa9f07b97f643f3
last_synced: '2026-04-23T10:10:35Z'
loc: 200
annotations: []
imports:
- json
- os
- subprocess
- datetime
- typing
exports:
- run_pytest_cov
- parse_coverage
- tests_for_range
- write_manifest
- main
tags:
- code
- python
coverage_pct: 73.95833333333333
---

# agents/cartographer/coverage_map.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`agents/cartographer/coverage_map.py`](../../../agents/cartographer/coverage_map.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Coverage manifest generator.

Pipeline:
1. `run_pytest_cov(repo_root, cov_targets, test_paths)` invokes pytest with
   `--cov=<targets> --cov-context=test --cov-report=json:<path>` and a
   `.coveragerc` that enables `dynamic_context = test_function` and
   `show_contexts = True` in the JSON report.
2. `parse_coverage(coverage_json_path)` normalises the raw output into a
   manifest with three views:
       - `by_symbol`: `{<file>::<symbol>: {tests, covered_lines, total_lines, pct}}`
       - `by_test`  : `{<test_id>: [{file, symbol}, ...]}` (inverse map)
       - `by_file`  : `{<file>: {pct, tests}}` (aggregate)
3. `tests_for_range(file_data, begin, end)` returns the union of tests that hit
   any line in `[begin..end]` — used by `note_renderer` for annotation callouts.
4. `write_manifest(manifest, target_path)` persists the JSON.
"""

from __future__ import annotations

import json
import os
import subprocess
from datetime import datetime, timezone
from typing import Any


_DEFAULT_COVERAGERC = (
    "[run]\n"
    "dynamic_context = test_function\n"
    "\n"
    "[json]\n"
    "show_contexts = True\n"
)


def run_pytest_cov(
    repo_root: str,
    cov_targets: list[str],
    test_paths: list[str] | None = None,
) -> str:
    """Run pytest with coverage + dynamic context. Return path to coverage.json.

    Writes the JSON to `<repo_root>/coverage.json` (gitignored).
    """
    coveragerc = os.path.join(repo_root, ".coveragerc-cartographer")
    if not os.path.exists(coveragerc):
        with open(coveragerc, "w", encoding="utf-8") as fp:
            fp.write(_DEFAULT_COVERAGERC)

    cov_args = [f"--cov={t}" for t in cov_targets]
    json_path = os.path.join(repo_root, "coverage.json")

    cmd = [
        "python3", "-m", "pytest",
        *cov_args,
        f"--cov-config={coveragerc}",
        f"--cov-report=json:{json_path}",
        "-q",
        *(test_paths or []),
    ]
    # Tolerate non-zero exit (test failures) — we still want partial coverage
    subprocess.run(cmd, cwd=repo_root, check=False)
    return json_path


def parse_coverage(coverage_json_path: str) -> dict[str, Any]:
    with open(coverage_json_path, encoding="utf-8") as fp:
        raw = json.load(fp)

    by_symbol: dict[str, dict[str, Any]] = {}
    by_test: dict[str, list[dict[str, str]]] = {}
    by_file: dict[str, dict[str, Any]] = {}

    for file_path, fdata in raw.get("files", {}).items():
        contexts = fdata.get("contexts", {}) or {}
        functions = fdata.get("functions", {}) or {}
        file_tests: set[str] = set()

        for sym_name, sym in functions.items():
            if not sym_name:
                continue  # the "" key is module-level statements
            sym_lines = set(sym.get("executed_lines", []))
            tests_for_symbol: set[str] = set()
            for line in sym_lines:
                for ctx in contexts.get(str(line), []) or []:
                    if ctx and ctx != "":
                        tests_for_symbol.add(ctx)
                        file_tests.add(ctx)
                        by_test.setdefault(ctx, []).append(
                            {"file": file_path, "symbol": sym_name}
                        )

            summary = sym.get("summary", {})
            by_symbol[f"{file_path}::{sym_name}"] = {
                "tests": sorted(tests_for_symbol),
                "covered_lines": summary.get("covered_lines", 0),
                "total_lines": summary.get("num_statements", 0),
                "pct": summary.get("percent_covered", 0.0),
            }

        file_summary = fdata.get("summary", {})
        by_file[file_path] = {
            "pct": file_summary.get("percent_covered", 0.0),
            "tests": sorted(file_tests),
        }

    # Deduplicate by_test entries (each test → file:symbol pair only once)
    for test_id, hits in by_test.items():
        seen = set()
        deduped: list[dict[str, str]] = []
        for hit in hits:
            key = (hit["file"], hit["symbol"])
            if key in seen:
                continue
            seen.add(key)
            deduped.append(hit)
        by_test[test_id] = deduped

    return {
        "version": "1.0",
        "generated_at": datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z"),
        "by_symbol": by_symbol,
        "by_test": by_test,
        "by_file": by_file,
    }


def tests_for_range(file_data: dict[str, Any], begin: int, end: int) -> list[str]:
    """Return all tests that hit any line in [begin..end]."""
    contexts = file_data.get("contexts", {}) or {}
    out: set[str] = set()
    for line_str, ctxs in contexts.items():
        try:
            line = int(line_str)
        except (TypeError, ValueError):
            continue
        if begin <= line <= end:
            for ctx in ctxs or []:
                if ctx:
                    out.add(ctx)
    return sorted(out)


def write_manifest(manifest: dict[str, Any], target_path: str) -> None:
    os.makedirs(os.path.dirname(target_path), exist_ok=True)
    with open(target_path, "w", encoding="utf-8") as fp:
        json.dump(manifest, fp, indent=2, ensure_ascii=False)


# ---------------------------------------------------------------------------
# CLI entry point
# ---------------------------------------------------------------------------

DEFAULT_COV_TARGETS = ["agents", "server", "scripts"]
DEFAULT_TEST_PATHS = ["tests"]
DEFAULT_MANIFEST_PATH = "docs/vault/_index/coverage-map.json"


def main(argv: list[str] | None = None) -> int:
    import argparse
    import sys

    p = argparse.ArgumentParser(prog="coverage-map")
    p.add_argument("--repo-root", default=os.getcwd())
    p.add_argument("--cov-target", action="append", default=None,
                   help=f"Coverage target (default: {DEFAULT_COV_TARGETS})")
    p.add_argument("--test-path", action="append", default=None,
                   help=f"Test path (default: {DEFAULT_TEST_PATHS})")
    p.add_argument("--manifest", default=None,
                   help=f"Manifest output path (default: <repo>/{DEFAULT_MANIFEST_PATH})")
    ns = p.parse_args(argv if argv is not None else sys.argv[1:])

    targets = ns.cov_target or DEFAULT_COV_TARGETS
    test_paths = ns.test_path or DEFAULT_TEST_PATHS
    manifest_path = ns.manifest or os.path.join(ns.repo_root, DEFAULT_MANIFEST_PATH)

    cov_path = run_pytest_cov(
        repo_root=ns.repo_root,
        cov_targets=targets,
        test_paths=test_paths,
    )
    manifest = parse_coverage(cov_path)
    write_manifest(manifest, manifest_path)

    n_sym = len(manifest["by_symbol"])
    n_test = len(manifest["by_test"])
    n_file = len(manifest["by_file"])
    print(
        f"[coverage-map] {n_sym} symbols / {n_test} tests / {n_file} files "
        f"→ {manifest_path}"
    )
    return 0


if __name__ == "__main__":
    import sys
    sys.exit(main())
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `run_pytest_cov` (function) — lines 36-63 · **Tested by (1)**: `test_coverage_map.TestRunPytestCov.test_runs_and_produces_coverage_json`
- `parse_coverage` (function) — lines 66-128 · **Tested by (4)**: `test_coverage_map.TestParseCoverage.test_by_file_aggregates`, `test_coverage_map.TestParseCoverage.test_by_symbol_links_function_to_tests`, `test_coverage_map.TestParseCoverage.test_by_test_inverse_mapping`, `test_coverage_map.TestParseCoverage.test_untested_symbol_has_empty_tests`
- `tests_for_range` (function) — lines 131-144 · **Tested by (1)**: `test_coverage_map.TestLinesInRange.test_intersect_test_set_for_range`
- `write_manifest` (function) — lines 147-150 · **Tested by (1)**: `test_coverage_map.TestWriteManifest.test_writes_json_to_target`
- `main` (function) — lines 162-195 · ⚠️ no test

### Imports
- `json`
- `os`
- `subprocess`
- `datetime`
- `typing`

### Exports
- `run_pytest_cov`
- `parse_coverage`
- `tests_for_range`
- `write_manifest`
- `main`
