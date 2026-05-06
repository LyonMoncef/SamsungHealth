---
type: code-source
language: python
file_path: tests/agents/test_markers.py
git_blob: e2c3346176b0e47d76de4b37b9b68e1b9c44f55d
last_synced: '2026-05-06T08:02:34Z'
loc: 206
annotations: []
imports:
- pytest
exports:
- TestParseMarkersPython
- TestParseMarkersJavaScript
- TestParseMarkersKotlin
- TestParseMarkersHTML
- TestParseMarkersCSS
- TestInferLanguage
tags:
- code
- python
---

# tests/agents/test_markers.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/agents/test_markers.py`](../../../tests/agents/test_markers.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""RED-first tests for agents.cartographer.markers.

Validates the parser of `@vault:<slug>` markers across:
- Python (`# @vault:slug`, `# @vault:slug begin/end`)
- JavaScript (`// @vault:slug`)
- Kotlin    (`// @vault:slug`)
- HTML      (`<!-- @vault:slug -->`)
- CSS       (`/* @vault:slug */`)

Cases covered:
- Single line markers (end-of-line comment OR own-line comment)
- Range markers (begin/end pairs)
- Non-contigu (same slug, multiple begin/end pairs in same file)
- Slug regex enforcement
- Mismatched begin/end → reported as parse error
"""

import pytest


# ---------------------------------------------------------------------------
# language detection / single-line markers
# ---------------------------------------------------------------------------

class TestParseMarkersPython:
    def test_single_line_eol(self):
        from agents.cartographer.markers import parse_markers

        src = "limit: int = Query(30, gt=0, le=1000),  # @vault:sleep-perf-cap\n"
        markers = parse_markers(src, language="python", file_path="server/x.py")
        assert len(markers) == 1
        m = markers[0]
        assert m.slug == "sleep-perf-cap"
        assert m.kind == "single"
        assert m.line == 1
        assert m.file == "server/x.py"

    def test_single_line_own_line(self):
        from agents.cartographer.markers import parse_markers

        src = "x = 1\n# @vault:my-note\ny = 2\n"
        markers = parse_markers(src, language="python", file_path="x.py")
        assert len(markers) == 1
        assert markers[0].slug == "my-note"
        assert markers[0].kind == "single"
        assert markers[0].line == 2

    def test_range_begin_end(self):
        from agents.cartographer.markers import parse_markers

        src = (
            "# @vault:sleep-pipeline begin\n"
            "async def get_sessions():\n"
            "    pass\n"
            "# @vault:sleep-pipeline end\n"
        )
        markers = parse_markers(src, language="python", file_path="x.py")
        assert len(markers) == 1
        m = markers[0]
        assert m.slug == "sleep-pipeline"
        assert m.kind == "range"
        assert m.begin_line == 1
        assert m.end_line == 4

    def test_range_non_contigu_same_slug(self):
        from agents.cartographer.markers import parse_markers

        src = (
            "# @vault:n1-query-risk begin\n"
            "class A: pass\n"
            "# @vault:n1-query-risk end\n"
            "x = 1\n"
            "# @vault:n1-query-risk begin\n"
            "def helper(): pass\n"
            "# @vault:n1-query-risk end\n"
        )
        markers = parse_markers(src, language="python", file_path="x.py")
        assert len(markers) == 2
        slugs = {m.slug for m in markers}
        assert slugs == {"n1-query-risk"}
        assert all(m.kind == "range" for m in markers)
        assert {(m.begin_line, m.end_line) for m in markers} == {(1, 3), (5, 7)}

    def test_mismatched_begin_without_end_is_error(self):
        from agents.cartographer.markers import parse_markers, MarkerParseError

        src = "# @vault:lonely begin\nx = 1\n"
        with pytest.raises(MarkerParseError) as exc:
            parse_markers(src, language="python", file_path="x.py")
        assert "lonely" in str(exc.value)

    def test_end_without_begin_is_error(self):
        from agents.cartographer.markers import parse_markers, MarkerParseError

        src = "x = 1\n# @vault:lonely end\n"
        with pytest.raises(MarkerParseError):
            parse_markers(src, language="python", file_path="x.py")

    def test_invalid_slug_is_skipped_or_errors(self):
        from agents.cartographer.markers import parse_markers, MarkerParseError

        # uppercase slug → invalid
        src = "x = 1  # @vault:BadSlug\n"
        with pytest.raises(MarkerParseError):
            parse_markers(src, language="python", file_path="x.py")

    def test_no_markers_returns_empty_list(self):
        from agents.cartographer.markers import parse_markers

        src = "x = 1\nprint(x)\n"
        assert parse_markers(src, language="python", file_path="x.py") == []


class TestParseMarkersJavaScript:
    def test_single_line_eol(self):
        from agents.cartographer.markers import parse_markers

        src = "const limit = 1000; // @vault:dashboard-perf\n"
        markers = parse_markers(src, language="javascript", file_path="static/x.js")
        assert len(markers) == 1
        assert markers[0].slug == "dashboard-perf"
        assert markers[0].kind == "single"

    def test_range_begin_end(self):
        from agents.cartographer.markers import parse_markers

        src = (
            "// @vault:render-loop begin\n"
            "function render() {}\n"
            "// @vault:render-loop end\n"
        )
        markers = parse_markers(src, language="javascript", file_path="x.js")
        assert len(markers) == 1
        assert markers[0].kind == "range"


class TestParseMarkersKotlin:
    def test_single_line(self):
        from agents.cartographer.markers import parse_markers

        src = "val limit = 1000 // @vault:android-batt\n"
        markers = parse_markers(src, language="kotlin", file_path="app/M.kt")
        assert len(markers) == 1
        assert markers[0].slug == "android-batt"

    def test_range_begin_end(self):
        from agents.cartographer.markers import parse_markers

        src = (
            "// @vault:sync-loop begin\n"
            "fun sync() {}\n"
            "// @vault:sync-loop end\n"
        )
        markers = parse_markers(src, language="kotlin", file_path="app/M.kt")
        assert markers[0].kind == "range"


class TestParseMarkersHTML:
    def test_single_line(self):
        from agents.cartographer.markers import parse_markers

        src = "<div>X</div> <!-- @vault:dashboard-layout -->\n"
        markers = parse_markers(src, language="html", file_path="static/i.html")
        assert len(markers) == 1
        assert markers[0].slug == "dashboard-layout"

    def test_range_begin_end(self):
        from agents.cartographer.markers import parse_markers

        src = (
            "<!-- @vault:hero-block begin -->\n"
            "<div>hero</div>\n"
            "<!-- @vault:hero-block end -->\n"
        )
        markers = parse_markers(src, language="html", file_path="x.html")
        assert markers[0].kind == "range"


class TestParseMarkersCSS:
    def test_single_line(self):
        from agents.cartographer.markers import parse_markers

        src = ".btn { color: red; } /* @vault:btn-color */\n"
        markers = parse_markers(src, language="css", file_path="static/x.css")
        assert len(markers) == 1
        assert markers[0].slug == "btn-color"


# ---------------------------------------------------------------------------
# language inference helper
# ---------------------------------------------------------------------------

class TestInferLanguage:
    def test_from_extension(self):
        from agents.cartographer.markers import infer_language

        assert infer_language("server/x.py") == "python"
        assert infer_language("static/x.js") == "javascript"
        assert infer_language("app/M.kt") == "kotlin"
        assert infer_language("templates/x.html") == "html"
        assert infer_language("static/x.css") == "css"

    def test_unknown_extension_returns_none(self):
        from agents.cartographer.markers import infer_language

        assert infer_language("x.txt") is None
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TestParseMarkersPython` (class) — lines 25-111
- `TestParseMarkersJavaScript` (class) — lines 114-134
- `TestParseMarkersKotlin` (class) — lines 137-155
- `TestParseMarkersHTML` (class) — lines 158-176
- `TestParseMarkersCSS` (class) — lines 179-186
- `TestInferLanguage` (class) — lines 193-206

### Imports
- `pytest`

### Exports
- `TestParseMarkersPython`
- `TestParseMarkersJavaScript`
- `TestParseMarkersKotlin`
- `TestParseMarkersHTML`
- `TestParseMarkersCSS`
- `TestInferLanguage`


## Exercises *(auto — this test file touches)*

### `test_markers.TestInferLanguage.test_from_extension`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`

### `test_markers.TestInferLanguage.test_unknown_extension_returns_none`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `infer_language`

### `test_markers.TestParseMarkersCSS.test_single_line`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersHTML.test_range_begin_end`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersHTML.test_single_line`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersJavaScript.test_range_begin_end`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersJavaScript.test_single_line_eol`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersKotlin.test_range_begin_end`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersKotlin.test_single_line`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersPython.test_end_without_begin_is_error`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersPython.test_invalid_slug_is_skipped_or_errors`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersPython.test_mismatched_begin_without_end_is_error`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersPython.test_no_markers_returns_empty_list`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersPython.test_range_begin_end`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersPython.test_range_non_contigu_same_slug`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersPython.test_single_line_eol`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`

### `test_markers.TestParseMarkersPython.test_single_line_own_line`
- [[../../code/agents/cartographer/markers|agents/cartographer/markers.py]] · `parse_markers`
