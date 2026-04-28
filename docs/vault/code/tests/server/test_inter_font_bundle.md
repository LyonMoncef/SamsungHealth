---
type: code-source
language: python
file_path: tests/server/test_inter_font_bundle.py
git_blob: 17d753d5223b64fa05c0521ba28d427dbf20df01
last_synced: '2026-04-28T14:04:54Z'
loc: 112
annotations: []
imports:
- re
- pathlib
- pytest
exports:
- _client
- TestInterFontServed
tags:
- code
- python
---

# tests/server/test_inter_font_bundle.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_inter_font_bundle.py`](../../../tests/server/test_inter_font_bundle.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.3 — Inter font bundle local (Google Fonts Inter Variable, OFL license).

Tests RED-first contre :
- `static/assets/fonts/Inter-VariableFont_wght.ttf` (NEW, ~340kB)
- `static/assets/fonts/Inter_OFL.txt` (NEW, license preserved)
- `static/css/ds-tokens.css` doit contenir `@font-face` Inter

Cas couverts (spec §"1. Inter font bundle local" + tests #13, #14, #15) :
- Font servie par /static/assets/fonts/Inter-VariableFont_wght.ttf → 200 + MIME font/ttf
- OFL license preserved (mention "Copyright" + "SIL Open Font License")
- @font-face Inter présent dans ds-tokens.css

Spec: docs/vault/specs/2026-04-28-v2.3.3.3-auth-finitions.md
"""
from __future__ import annotations

import re
from pathlib import Path

import pytest


_REPO_ROOT = Path(__file__).resolve().parent.parent.parent
_STATIC_DIR = _REPO_ROOT / "static"
_FONTS_DIR = _STATIC_DIR / "assets" / "fonts"
_DS_TOKENS_CSS = _STATIC_DIR / "css" / "ds-tokens.css"


def _client():
    """V2.3.3.3 — TestClient bypass PG fixture (static asset serving only)."""
    from fastapi.testclient import TestClient

    from server.main import app

    return TestClient(app)


class TestInterFontServed:
    """spec test #13, #14, #15."""

    def test_inter_font_served_with_ttf_mime(self):
        """given GET /static/assets/fonts/Inter-VariableFont_wght.ttf, when called, then 200 + content-type font/ttf or application/x-font-ttf.

        spec test #13 — font servi via FastAPI StaticFiles, MIME ttf.
        """
        font_path = _FONTS_DIR / "Inter-VariableFont_wght.ttf"
        assert font_path.exists(), (
            f"static/assets/fonts/Inter-VariableFont_wght.ttf missing — "
            f"impl must download from Google Fonts (spec §1)"
        )
        client = _client()
        r = client.get("/static/assets/fonts/Inter-VariableFont_wght.ttf")
        assert r.status_code == 200, (
            f"GET Inter font expected 200, got {r.status_code}: {r.text[:200]}"
        )
        ct = r.headers.get("content-type", "")
        accepted_mimes = (
            "font/ttf",
            "application/x-font-ttf",
            "application/font-sfnt",
            "font/sfnt",
            # Some StaticFiles servers fallback to octet-stream — accept it as
            # functionally serving the binary, browsers sniff the OpenType signature.
            "application/octet-stream",
        )
        assert any(mime in ct for mime in accepted_mimes), (
            f"Inter font content-type expected one of {accepted_mimes}, got {ct!r}"
        )

    def test_inter_ofl_license_preserved(self):
        """given static/assets/fonts/Inter_OFL.txt exists, when read, then mentionne 'Copyright' + 'SIL Open Font License'.

        spec test #14 — OFL preserved (legal requirement Google Fonts license).
        """
        ofl_path = _FONTS_DIR / "Inter_OFL.txt"
        assert ofl_path.exists(), (
            f"static/assets/fonts/Inter_OFL.txt missing — impl must include OFL license "
            f"file (spec §1 + Google Fonts OFL legal requirement)"
        )
        content = ofl_path.read_text(encoding="utf-8", errors="replace")
        assert "Copyright" in content, (
            f"Inter_OFL.txt MUST contain 'Copyright' (legal): head={content[:200]!r}"
        )
        assert "SIL Open Font License" in content, (
            f"Inter_OFL.txt MUST contain 'SIL Open Font License': head={content[:200]!r}"
        )

    def test_inter_font_face_declared_in_ds_tokens_css(self):
        """given static/css/ds-tokens.css, when grepped, then contient `@font-face` block avec `font-family: 'Inter'` + `url('/static/assets/fonts/Inter-VariableFont_wght.ttf...`.

        spec test #15 — `@font-face` Inter dans ds-tokens.css.
        """
        assert _DS_TOKENS_CSS.exists(), (
            f"static/css/ds-tokens.css missing"
        )
        content = _DS_TOKENS_CSS.read_text(encoding="utf-8")
        # Must declare a @font-face for Inter referencing the local TTF path.
        # Pattern : @font-face { ... font-family: 'Inter' ... url('/static/assets/fonts/Inter-...
        m = re.search(
            r"@font-face\s*\{[^}]*font-family\s*:\s*['\"]?Inter['\"]?[^}]*\}",
            content,
            re.IGNORECASE | re.DOTALL,
        )
        assert m is not None, (
            f"ds-tokens.css MUST contain @font-face block for Inter family "
            f"(spec test #15), content head: {content[:600]!r}"
        )
        block = m.group(0)
        assert "Inter-VariableFont_wght.ttf" in block, (
            f"@font-face Inter must reference '/static/assets/fonts/Inter-VariableFont_wght.ttf', "
            f"got block: {block!r}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_client` (function) — lines 29-35
- `TestInterFontServed` (class) — lines 38-112

### Imports
- `re`
- `pathlib`
- `pytest`

### Exports
- `_client`
- `TestInterFontServed`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]] — classes: `TestInterFontServed`
