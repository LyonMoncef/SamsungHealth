---
type: code-source
language: python
file_path: server/security/csrf.py
git_blob: 95d94f2955414a4fee1cdb86714494d387fe50a9
last_synced: '2026-04-27T20:51:40Z'
loc: 28
annotations: []
imports:
- fastapi
exports:
- check_sec_fetch_site
tags:
- code
- python
---

# server/security/csrf.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/security/csrf.py`](../../../server/security/csrf.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3.3.2 — Sec-Fetch-Site CSRF check helper (post-pentester M1 fix).

Refuse les requêtes POST cross-site / same-site (subdomain attack vector).
Tolère `same-origin`, `none` (direct nav), ou absent (legacy / curl / non-browser).

Appliqué sur les 6 POST auth sensibles : /auth/login, /auth/register,
/auth/password/reset/request, /auth/verify-email/request,
/auth/oauth-link/confirm, /auth/google/start.
"""
from __future__ import annotations

from fastapi import HTTPException, Request


_REJECTED = frozenset({"cross-site", "same-site"})


def check_sec_fetch_site(request: Request) -> None:
    """Raise HTTPException(403, 'csrf_check_failed') si Sec-Fetch-Site cross-site/same-site.

    Les browsers modernes envoient toujours ce header sur les requêtes initiées
    par fetch/XHR. Absence acceptée (curl, clients legacy, vieux browsers).
    """
    sfs = request.headers.get("sec-fetch-site")
    if sfs is None:
        return
    if sfs.lower() in _REJECTED:
        raise HTTPException(status_code=403, detail="csrf_check_failed")
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall]] — symbols: `check_sec_fetch_site`

### Symbols
- `check_sec_fetch_site` (function) — lines 18-28 · **Specs**: [[../../specs/2026-04-27-v2.3.3.2-frontend-nightfall|2026-04-27-v2.3.3.2-frontend-nightfall]]

### Imports
- `fastapi`

### Exports
- `check_sec_fetch_site`
