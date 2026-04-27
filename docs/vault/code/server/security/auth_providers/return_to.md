---
type: code-source
language: python
file_path: server/security/auth_providers/return_to.py
git_blob: f0cc3e5d3f38c378497070f868c7671e15b3ef01
last_synced: '2026-04-27T07:34:23Z'
loc: 65
annotations: []
imports:
- os
- urllib.parse
exports:
- InvalidReturnTo
- _allowed_origins_from_env
- validate_return_to
tags:
- code
- python
---

# server/security/auth_providers/return_to.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/security/auth_providers/return_to.py`](../../../server/security/auth_providers/return_to.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.2 — return_to whitelist validator (anti open-redirect).

7-step strict validation per spec §return_to whitelist:
1. Empty → None.
2. Scheme must be https or http.
3. Refuse userinfo (`@` in netloc).
4. Netloc non-empty.
5. IDN punycode normalize (anti-homograph).
6. Exact origin match against whitelist.
7. Reconstruct URL with path/query (fragment stripped).
"""
from __future__ import annotations

import os
import urllib.parse


_ALLOWED_ENV = "SAMSUNGHEALTH_OAUTH_RETURN_TO_ALLOWED"


class InvalidReturnTo(ValueError):
    """Raised when a return_to URL fails any of the 7 validation rules."""

    def __init__(self, *, reason: str) -> None:
        self.reason = reason
        super().__init__(reason)


def _allowed_origins_from_env() -> set[str]:
    raw = os.environ.get(_ALLOWED_ENV, "")
    if not raw:
        return set()
    return {o.strip() for o in raw.split(",") if o.strip()}


def validate_return_to(
    return_to: str | None, allowed_origins: set[str]
) -> str | None:
    """Validate `return_to` URL against `allowed_origins`. See module docstring."""
    if not return_to:
        return None

    parts = urllib.parse.urlsplit(return_to)

    if parts.scheme not in {"https", "http"}:
        raise InvalidReturnTo(reason="bad_scheme")

    netloc = parts.netloc or ""
    if "@" in netloc:
        raise InvalidReturnTo(reason="userinfo_present")
    if not netloc:
        raise InvalidReturnTo(reason="empty_netloc")

    try:
        idna_netloc = netloc.encode("idna").decode("ascii")
    except UnicodeError as exc:
        raise InvalidReturnTo(reason="bad_idna") from exc

    origin = f"{parts.scheme}://{idna_netloc}"
    if origin not in allowed_origins:
        raise InvalidReturnTo(reason="not_in_whitelist")

    return urllib.parse.urlunsplit(
        (parts.scheme, idna_netloc, parts.path or "/", parts.query, "")
    )
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — symbols: `validate_return_to`, `InvalidReturnTo`

### Symbols
- `InvalidReturnTo` (class) — lines 21-26 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]
- `_allowed_origins_from_env` (function) — lines 29-33
- `validate_return_to` (function) — lines 36-65 · **Specs**: [[../../specs/2026-04-26-v2.3.2-google-oauth|2026-04-26-v2.3.2-google-oauth]]

### Imports
- `os`
- `urllib.parse`

### Exports
- `InvalidReturnTo`
- `_allowed_origins_from_env`
- `validate_return_to`
