---
type: code-source
language: python
file_path: tests/agents/test_coverage_map.py
git_blob: 93842c9f71662e875645d5b2f646996d7b08dffb
last_synced: '2026-04-23T10:10:35Z'
loc: 180
annotations: []
imports:
- json
- pathlib
- pytest
exports:
- TestParseCoverage
- TestLinesInRange
- TestWriteManifest
- TestRunPytestCov
tags:
- code
- python
---

# tests/agents/test_coverage_map.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_coverage_map.py`](../../../tests/agents/test_coverage_map.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.coverage_map.

The module:
- parses coverage.py JSON output (with contexts) into a normalized manifest
- builds by_symbol mapping (symbol → tests that touch it)
- builds inverse by_test mapping (test → symbols it exercises)
- writes the manifest to docs/vault/_index/coverage-map.json (gitignored)
"""

import json
from pathlib import Path

import pytest


# Fixture: a synthetic coverage.json with 2 functions + 2 tests
SAMPLE_COV = {
    "meta": {"version": "7.x"},
    "files": {
        "agents/x.py": {
            "executed_lines": [1, 2, 4, 5],
            "missing_lines": [],
            "excluded_lines": [],
            "summary": {
                "covered_lines": 4,
                "num_statements": 4,
                "percent_covered": 100.0,
            },
            "contexts": {
                "1": [""],
                "2": ["tests.test_x.test_add"],
                "4": [""],
                "5": ["tests.test_x.test_sub"],
            },
            "functions": {
                "add": {
                    "executed_lines": [2],
                    "missing_lines": [],
                    "start_line": 1,
                    "summary": {"covered_lines": 1, "num_statements": 1, "percent_covered": 100.0},
                },
                "sub": {
                    "executed_lines": [5],
                    "missing_lines": [],
                    "start_line": 4,
                    "summary": {"covered_lines": 1, "num_statements": 1, "percent_covered": 100.0},
                },
            },
        },
        "agents/y.py": {
            "executed_lines": [],
            "missing_lines": [1, 2, 3],
            "excluded_lines": [],
            "summary": {
                "covered_lines": 0,
                "num_statements": 3,
                "percent_covered": 0.0,
            },
            "contexts": {},
            "functions": {
                "untested": {
                    "executed_lines": [],
                    "missing_lines": [1, 2, 3],
                    "start_line": 1,
                    "summary": {"covered_lines": 0, "num_statements": 3, "percent_covered": 0.0},
                },
            },
        },
    },
    "totals": {"covered_lines": 4, "num_statements": 7, "percent_covered": 57.1},
}


class TestParseCoverage:
    def test_by_symbol_links_function_to_tests(self, tmp_path: Path):
        from agents.cartographer.coverage_map import parse_coverage

        cov_file = tmp_path / "coverage.json"
        cov_file.write_text(json.dumps(SAMPLE_COV))

        manifest = parse_coverage(str(cov_file))

        assert "by_symbol" in manifest
        add = manifest["by_symbol"]["agents/x.py::add"]
        assert "tests.test_x.test_add" in add["tests"]
        assert add["covered_lines"] == 1
        assert add["total_lines"] == 1
        assert add["pct"] == 100.0

    def test_untested_symbol_has_empty_tests(self, tmp_path: Path):
        from agents.cartographer.coverage_map import parse_coverage

        cov_file = tmp_path / "coverage.json"
        cov_file.write_text(json.dumps(SAMPLE_COV))

        manifest = parse_coverage(str(cov_file))
        untested = manifest["by_symbol"]["agents/y.py::untested"]
        assert untested["tests"] == []
        assert untested["pct"] == 0.0

    def test_by_test_inverse_mapping(self, tmp_path: Path):
        from agents.cartographer.coverage_map import parse_coverage

        cov_file = tmp_path / "coverage.json"
        cov_file.write_text(json.dumps(SAMPLE_COV))

        manifest = parse_coverage(str(cov_file))
        assert "by_test" in manifest
        test_add = manifest["by_test"]["tests.test_x.test_add"]
        assert {"file": "agents/x.py", "symbol": "add"} in test_add

    def test_by_file_aggregates(self, tmp_path: Path):
        from agents.cartographer.coverage_map import parse_coverage

        cov_file = tmp_path / "coverage.json"
        cov_file.write_text(json.dumps(SAMPLE_COV))

        manifest = parse_coverage(str(cov_file))
        assert manifest["by_file"]["agents/x.py"]["pct"] == 100.0
        assert manifest["by_file"]["agents/y.py"]["pct"] == 0.0
        # tests aggregated across all symbols of the file
        assert "tests.test_x.test_add" in manifest["by_file"]["agents/x.py"]["tests"]


class TestLinesInRange:
    def test_intersect_test_set_for_range(self):
        from agents.cartographer.coverage_map import tests_for_range

        cov_data_for_file = {
            "contexts": {
                "10": ["tests.X.test_a"],
                "11": ["tests.X.test_a"],
                "20": ["tests.X.test_b"],
                "30": ["tests.Y.test_c", "tests.X.test_a"],
            },
        }
        # range 10-15 → only test_a
        assert tests_for_range(cov_data_for_file, 10, 15) == ["tests.X.test_a"]
        # range 25-35 → test_c + test_a
        result = tests_for_range(cov_data_for_file, 25, 35)
        assert "tests.Y.test_c" in result
        assert "tests.X.test_a" in result


class TestWriteManifest:
    def test_writes_json_to_target(self, tmp_path: Path):
        from agents.cartographer.coverage_map import write_manifest

        target = tmp_path / "vault/_index/coverage-map.json"
        manifest = {"by_symbol": {}, "by_test": {}, "by_file": {}}
        write_manifest(manifest, str(target))
        assert target.exists()
        loaded = json.loads(target.read_text())
        assert loaded == manifest


class TestRunPytestCov:
    """Smoke test — actually runs pytest-cov in a tmp project."""

    def test_runs_and_produces_coverage_json(self, tmp_path: Path):
        from agents.cartographer.coverage_map import run_pytest_cov

        # Mini project
        (tmp_path / "mod.py").write_text("def f(x): return x + 1\n")
        (tmp_path / "test_mod.py").write_text(
            "from mod import f\ndef test_f(): assert f(1) == 2\n"
        )
        (tmp_path / ".coveragerc").write_text(
            "[run]\ndynamic_context = test_function\n[json]\nshow_contexts = True\n"
        )

        cov_path = run_pytest_cov(
            repo_root=str(tmp_path),
            cov_targets=["mod"],
            test_paths=["test_mod.py"],
        )
        assert Path(cov_path).exists()
        d = json.loads(Path(cov_path).read_text())
        assert "files" in d
        assert "mod.py" in d["files"]
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestParseCoverage` (class) — lines 74-122
- `TestLinesInRange` (class) — lines 125-142
- `TestWriteManifest` (class) — lines 145-154
- `TestRunPytestCov` (class) — lines 157-180

### Imports
- `json`
- `pathlib`
- `pytest`

### Exports
- `TestParseCoverage`
- `TestLinesInRange`
- `TestWriteManifest`
- `TestRunPytestCov`


## Exercises *(auto — this test file touches)*

### `test_coverage_map.TestLinesInRange.test_intersect_test_set_for_range`
- [[../../code/agents/cartographer/coverage_map|agents/cartographer/coverage_map.py]] · `tests_for_range`

### `test_coverage_map.TestParseCoverage.test_by_file_aggregates`
- [[../../code/agents/cartographer/coverage_map|agents/cartographer/coverage_map.py]] · `parse_coverage`

### `test_coverage_map.TestParseCoverage.test_by_symbol_links_function_to_tests`
- [[../../code/agents/cartographer/coverage_map|agents/cartographer/coverage_map.py]] · `parse_coverage`

### `test_coverage_map.TestParseCoverage.test_by_test_inverse_mapping`
- [[../../code/agents/cartographer/coverage_map|agents/cartographer/coverage_map.py]] · `parse_coverage`

### `test_coverage_map.TestParseCoverage.test_untested_symbol_has_empty_tests`
- [[../../code/agents/cartographer/coverage_map|agents/cartographer/coverage_map.py]] · `parse_coverage`

### `test_coverage_map.TestRunPytestCov.test_runs_and_produces_coverage_json`
- [[../../code/agents/cartographer/coverage_map|agents/cartographer/coverage_map.py]] · `run_pytest_cov`

### `test_coverage_map.TestWriteManifest.test_writes_json_to_target`
- [[../../code/agents/cartographer/coverage_map|agents/cartographer/coverage_map.py]] · `write_manifest`
