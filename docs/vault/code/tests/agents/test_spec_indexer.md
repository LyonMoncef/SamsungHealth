---
type: code-source
language: python
file_path: tests/agents/test_spec_indexer.py
git_blob: 3f01d2c1d3360e44567b19495fe70c7096282cf4
last_synced: '2026-04-23T10:40:54Z'
loc: 151
annotations: []
imports:
- pathlib
- pytest
exports:
- _write_spec
- TestLoadSpec
- TestBuildIndex
- TestDriftDetection
tags:
- code
- python
---

# tests/agents/test_spec_indexer.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_spec_indexer.py`](../../../tests/agents/test_spec_indexer.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.spec_indexer.

Reads `docs/vault/specs/*.md` frontmatter, builds an index consumed by the
note_renderer to surface bidirectional spec ↔ code ↔ tests links.

Returns a `SpecIndex` with three views:
- by_file_to_specs : `{<file>: [{slug, symbols, line_range}]}` (code note → specs that implement)
- by_test_to_specs : `{<test_file>: [{slug, classes, methods}]}` (test note → specs validated)
- by_slug          : `{<slug>: SpecMeta}` (spec note → its declared targets)
"""

from pathlib import Path

import pytest


def _write_spec(tmp_path: Path, slug: str, body: str) -> Path:
    p = tmp_path / f"{slug}.md"
    p.write_text(body)
    return p


class TestLoadSpec:
    def test_minimal_spec_frontmatter(self, tmp_path: Path):
        from agents.cartographer.spec_indexer import load_spec

        p = _write_spec(tmp_path, "abc", (
            "---\n"
            "type: spec\n"
            "title: Foo\n"
            "status: ready\n"
            "implements:\n"
            "  - file: agents/x.py\n"
            "    symbols: [foo, bar]\n"
            "tested_by:\n"
            "  - file: tests/test_x.py\n"
            "    classes: [TestFoo]\n"
            "---\n\nbody\n"
        ))
        meta = load_spec(str(p))
        assert meta.type == "spec"
        assert meta.slug == "abc"
        assert meta.implements[0].file == "agents/x.py"
        assert meta.implements[0].symbols == ["foo", "bar"]
        assert meta.tested_by[0].classes == ["TestFoo"]

    def test_stub_returns_none(self, tmp_path: Path):
        """Stub specs (moved to repo) shouldn't be indexed."""
        from agents.cartographer.spec_indexer import load_spec

        p = _write_spec(tmp_path, "moved", (
            "---\ntype: stub\ncanonical: foo\n---\n\nstub body\n"
        ))
        meta = load_spec(str(p))
        assert meta is None or meta.type == "stub"


class TestBuildIndex:
    def test_by_file_to_specs(self, tmp_path: Path):
        from agents.cartographer.spec_indexer import build_index

        spec1 = _write_spec(tmp_path, "spec-x", (
            "---\ntype: spec\nimplements:\n"
            "  - file: agents/x.py\n    symbols: [foo]\n---\nbody\n"
        ))
        spec2 = _write_spec(tmp_path, "spec-y", (
            "---\ntype: spec\nimplements:\n"
            "  - file: agents/x.py\n    symbols: [bar]\n"
            "  - file: agents/y.py\n---\nbody\n"
        ))
        idx = build_index([str(spec1), str(spec2)])

        assert "agents/x.py" in idx.by_file_to_specs
        slugs = {entry["slug"] for entry in idx.by_file_to_specs["agents/x.py"]}
        assert slugs == {"spec-x", "spec-y"}

        assert "agents/y.py" in idx.by_file_to_specs

    def test_by_test_to_specs(self, tmp_path: Path):
        from agents.cartographer.spec_indexer import build_index

        spec = _write_spec(tmp_path, "spec-x", (
            "---\ntype: spec\ntested_by:\n"
            "  - file: tests/test_x.py\n    classes: [TestX]\n---\nbody\n"
        ))
        idx = build_index([str(spec)])
        assert "tests/test_x.py" in idx.by_test_to_specs
        assert idx.by_test_to_specs["tests/test_x.py"][0]["slug"] == "spec-x"

    def test_by_slug_lookup(self, tmp_path: Path):
        from agents.cartographer.spec_indexer import build_index

        spec = _write_spec(tmp_path, "spec-z", (
            "---\ntype: spec\ntitle: Z\nimplements: []\ntested_by: []\n---\nbody\n"
        ))
        idx = build_index([str(spec)])
        assert "spec-z" in idx.by_slug
        assert idx.by_slug["spec-z"].title == "Z"

    def test_stubs_excluded(self, tmp_path: Path):
        from agents.cartographer.spec_indexer import build_index

        stub = _write_spec(tmp_path, "moved", (
            "---\ntype: stub\ncanonical: foo\n---\nstub\n"
        ))
        idx = build_index([str(stub)])
        assert idx.by_slug == {}


class TestDriftDetection:
    def test_implements_drift_detects_missing_file(self, tmp_path: Path):
        """spec_implements_drift: spec declares file/symbol that no longer exists."""
        from agents.cartographer.spec_indexer import detect_implements_drift

        existing = tmp_path / "real.py"
        existing.write_text("def foo(): pass\n")

        spec_file = _write_spec(tmp_path, "spec", (
            "---\ntype: spec\nimplements:\n"
            "  - file: deleted.py\n"
            "  - file: real.py\n    symbols: [missing_func]\n"
            "  - file: real.py\n    symbols: [foo]\n---\nbody\n"
        ))
        from agents.cartographer.spec_indexer import build_index, load_spec
        spec_meta = load_spec(str(spec_file))

        drifts = detect_implements_drift(
            spec_slug="spec",
            spec_meta=spec_meta,
            repo_root=str(tmp_path),
        )
        # 1 drift for deleted.py + 1 for real.py::missing_func ; foo is OK
        files_in_drift = {d["target_file"] for d in drifts}
        assert "deleted.py" in files_in_drift
        # Symbol-level drift surfaced when file exists but symbol missing
        symbol_drifts = [d for d in drifts if d.get("missing_symbol")]
        assert any(d["missing_symbol"] == "missing_func" for d in symbol_drifts)

    def test_untested_specs_listing(self, tmp_path: Path):
        from agents.cartographer.spec_indexer import build_index, untested_specs

        s1 = _write_spec(tmp_path, "tested", (
            "---\ntype: spec\nimplements: []\n"
            "tested_by:\n  - file: tests/t.py\n---\nbody\n"
        ))
        s2 = _write_spec(tmp_path, "untested", (
            "---\ntype: spec\nimplements: []\ntested_by: []\n---\nbody\n"
        ))
        idx = build_index([str(s1), str(s2)])
        slugs = untested_specs(idx)
        assert slugs == ["untested"]
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_write_spec` (function) — lines 17-20
- `TestLoadSpec` (class) — lines 23-55
- `TestBuildIndex` (class) — lines 58-107
- `TestDriftDetection` (class) — lines 110-151

### Imports
- `pathlib`
- `pytest`

### Exports
- `_write_spec`
- `TestLoadSpec`
- `TestBuildIndex`
- `TestDriftDetection`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-23-plan-specs-in-vault]] — classes: `TestLoadSpec`, `TestBuildIndex`, `TestDriftDetection`
