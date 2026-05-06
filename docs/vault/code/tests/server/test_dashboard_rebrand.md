---
type: code-source
language: python
file_path: tests/server/test_dashboard_rebrand.py
git_blob: 14aa9cf7d959a94a9ddec09eb2658e611c710925
last_synced: '2026-05-06T08:02:35Z'
loc: 174
annotations: []
imports:
- re
- pathlib
- pytest
exports:
- _client
- TestDashboardCssTokens
- TestDashboardCspCompatible
tags:
- code
- python
---

# tests/server/test_dashboard_rebrand.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/test_dashboard_rebrand.py`](../../../tests/server/test_dashboard_rebrand.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.3 — Dashboard rebrand : migration des `#hex` literals → tokens `--ds-*`.

Tests RED-first contre :
- `static/dashboard.css` : ne doit plus contenir aucun accent `#hex` literal
  (FCBF0E, 3BE5E7, 854808, D37C04, 07BCD3) — uniquement neutres ink/paper tolérés.
- `static/index.html` : doit inclure `<link href=".../ds-tokens.css?v=...">` AVANT
  `dashboard.css` (cascade tokens d'abord) + theme toggle (`data-theme-toggle`).
- Dashboard sert OK (`GET /` → 200 text/html).

Cas couverts (spec §"3. Dashboard rebrand migration" + tests #16, #17, #18) :
- No accent #hex literals in dashboard.css
- ds-tokens.css linked in index.html before dashboard.css
- theme toggle present
- Dashboard root served

Spec: docs/vault/specs/2026-04-28-v2.3.3.3-auth-finitions.md
"""
from __future__ import annotations

import re
from pathlib import Path

import pytest


_REPO_ROOT = Path(__file__).resolve().parent.parent.parent
_STATIC_DIR = _REPO_ROOT / "static"
_DASHBOARD_CSS = _STATIC_DIR / "dashboard.css"
_INDEX_HTML = _STATIC_DIR / "index.html"


def _client():
    """V2.3.3.3 — TestClient bypass PG fixture (static asset / dashboard root only)."""
    from fastapi.testclient import TestClient

    from server.main import app

    return TestClient(app)


# Accent palette literals from ds-tokens.css that must NOT be hardcoded in dashboard.css.
# Le grep est case-insensitive (CSS = case-insensitive sur les hex).
_ACCENT_HEX_PALETTE = (
    "#FCBF0E",  # accent-warm dark
    "#3BE5E7",  # accent-cool legacy
    "#854808",  # accent-warm shade
    "#D37C04",  # accent-warm light
    "#07BCD3",  # accent-cool light
)


class TestDashboardCssTokens:
    """spec test #16 — `static/dashboard.css` n'a plus de `#hex` accent literals."""

    def test_no_accent_hex_literals_in_dashboard_css(self):
        """given static/dashboard.css, when grepped for accent #hex palette literals (case-insensitive), then 0 match AND dashboard.css uses `var(--ds-accent-*)` for at least one accent (proof of migration completed).

        spec test #16 + §"3. Dashboard rebrand migration" — accents migrés vers
        var(--ds-accent-warm) / var(--ds-accent-cool). Les `#hex` neutres (gris ink)
        restent OK pour V2.3.3.3.

        Coupled assertion : the absence-of-literals check is a regression guard,
        but on its own would pass GREEN if dashboard.css simply happens to lack
        those literals. We couple it to the positive migration evidence
        (`var(--ds-accent-*)` present) so the test is RED pre-impl AND a real
        regression guard post-impl.
        """
        assert _DASHBOARD_CSS.exists(), (
            "static/dashboard.css missing"
        )
        content = _DASHBOARD_CSS.read_text(encoding="utf-8")
        # 1) regression guard: no accent literals.
        pattern = re.compile(
            "|".join(re.escape(h) for h in _ACCENT_HEX_PALETTE),
            re.IGNORECASE,
        )
        violations = []
        for m in pattern.finditer(content):
            start = content.rfind("\n", 0, m.start()) + 1
            end = content.find("\n", m.end())
            if end == -1:
                end = len(content)
            line = content[start:end].strip()
            violations.append(f"{m.group(0)!r} on line: {line!r}")
        assert not violations, (
            f"static/dashboard.css MUST NOT contain hardcoded accent #hex literals "
            f"(spec test #16). Migrate to var(--ds-accent-*). Violations: {violations}"
        )
        # 2) positive migration evidence (RED until impl migrates).
        assert re.search(r"var\(\s*--ds-accent-(warm|cool)\b", content), (
            f"static/dashboard.css MUST use var(--ds-accent-warm) or var(--ds-accent-cool) "
            f"for at least one accent property after rebrand migration "
            f"(spec §3. Dashboard rebrand migration — proof of completed migration). "
            f"This couples the regression guard to the positive migration evidence so "
            f"the test is RED pre-impl."
        )

    def test_dashboard_css_uses_ds_token_for_at_least_one_accent(self):
        """given static/dashboard.css, when grepped for `var(--ds-accent-`, then ≥1 match.

        spec §"3. Dashboard rebrand migration" — "AVANT (hardcoded) / APRÈS (token)" :
        au moins une propriété d'accent doit être migrée vers `var(--ds-accent-warm)`
        ou `var(--ds-accent-cool)` pour prouver la migration.
        """
        assert _DASHBOARD_CSS.exists()
        content = _DASHBOARD_CSS.read_text(encoding="utf-8")
        assert re.search(r"var\(\s*--ds-accent-(warm|cool)\b", content), (
            f"static/dashboard.css MUST use var(--ds-accent-warm) or var(--ds-accent-cool) "
            f"for at least one accent property after rebrand migration "
            f"(spec §3. Dashboard rebrand migration). No occurrence found."
        )


class TestDashboardCspCompatible:
    """spec test #17 + #18 — ds-tokens.css inclus dans index.html avant dashboard.css + theme toggle + dashboard sert OK."""

    def test_ds_tokens_linked_in_index_html_before_dashboard(self):
        """given static/index.html, when grepped, then `<link ... ds-tokens.css>` présent ET appears BEFORE `dashboard.css` (cascade tokens d'abord).

        spec test #17 + §"3. Dashboard rebrand migration" — Ajout `<link>` ds-tokens.css
        dans `static/index.html` AVANT `dashboard.css`.
        """
        assert _INDEX_HTML.exists(), "static/index.html missing"
        content = _INDEX_HTML.read_text(encoding="utf-8")

        ds_match = re.search(r'<link[^>]*ds-tokens\.css', content, re.IGNORECASE)
        assert ds_match is not None, (
            f"static/index.html MUST contain <link ... ds-tokens.css> "
            f"(spec test #17), content head: {content[:600]!r}"
        )

        dash_match = re.search(r'<link[^>]*dashboard\.css', content, re.IGNORECASE)
        assert dash_match is not None, (
            f"static/index.html MUST contain <link ... dashboard.css>, content head: {content[:600]!r}"
        )

        assert ds_match.start() < dash_match.start(), (
            f"ds-tokens.css <link> MUST appear BEFORE dashboard.css <link> in index.html "
            f"(spec §3 — cascade tokens d'abord). ds-tokens at offset {ds_match.start()}, "
            f"dashboard at offset {dash_match.start()}."
        )

    def test_theme_toggle_present_in_index_html(self):
        """given static/index.html, when grepped for `data-theme-toggle`, then ≥1 match.

        spec test #18 — Ajout du theme toggle dans le header dashboard.
        """
        assert _INDEX_HTML.exists()
        content = _INDEX_HTML.read_text(encoding="utf-8")
        assert "data-theme-toggle" in content, (
            f"static/index.html MUST contain `data-theme-toggle` attribute "
            f"(spec test #18 — theme toggle dashboard header), content head: {content[:600]!r}"
        )

    def test_dashboard_root_served_ok(self):
        """given GET /, when called, then 200 + text/html (dashboard index served after rebrand).

        spec §non-régression — le rebrand ne casse pas la livraison du dashboard.
        Couples to the ds-tokens link being added (the header content is then served).
        """
        client = _client()
        r = client.get("/")
        assert r.status_code == 200, (
            f"GET / dashboard expected 200, got {r.status_code}: {r.text[:200]}"
        )
        ct = r.headers.get("content-type", "")
        assert "text/html" in ct, (
            f"GET / content-type must contain text/html, got {ct!r}"
        )
        # Couple to the rebrand: served body must include the new ds-tokens.css link.
        assert "ds-tokens.css" in r.text, (
            f"GET / served body MUST include ds-tokens.css <link> after rebrand "
            f"(spec test #17). Body head: {r.text[:600]!r}"
        )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_client` (function) — lines 32-38
- `TestDashboardCssTokens` (class) — lines 52-111
- `TestDashboardCspCompatible` (class) — lines 114-174

### Imports
- `re`
- `pathlib`
- `pytest`

### Exports
- `_client`
- `TestDashboardCssTokens`
- `TestDashboardCspCompatible`


## Validates specs *(auto — declared by spec)*

- [[../../specs/2026-04-28-v2.3.3.3-auth-finitions]] — classes: `TestDashboardCssTokens`, `TestDashboardCspCompatible`
