---
type: code-source
language: python
file_path: server/services/deprecation.py
git_blob: 30b4cf56879d9fc0215eef7920ee17483bd480ad
last_synced: '2026-05-09T16:40:48Z'
loc: 37
annotations: []
imports:
- fastapi
- server.logging_config
exports:
- mark_deprecated
tags:
- code
- python
---

# server/services/deprecation.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/services/deprecation.py`](../../../server/services/deprecation.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Phase D — déprécier les endpoints CSV import sans casser la rétrocompat.

L'app Android ne les appelle plus depuis Phase B (LocalImportService écrit
directement en Room). Ils restent disponibles pour clients tiers / scripts /
migration jusqu'à la `SUNSET_DATE` puis seront supprimés.

Cf. RFC 8594 (Sunset) + IETF draft `Deprecation` header.
"""
from __future__ import annotations

from fastapi import Response

from server.logging_config import get_logger

_log = get_logger(__name__)

# Date de retrait planifié — 6 mois après la mise en service de Phase B
# (référence : 2026-05-09 ; cible 2026-11-09).
SUNSET_DATE = "Mon, 09 Nov 2026 00:00:00 GMT"


def mark_deprecated(response: Response, endpoint: str, user_id: str | None = None) -> None:
    """Ajoute les headers `Deprecation` + `Sunset` et log un warning structuré."""
    response.headers["Deprecation"] = "true"
    response.headers["Sunset"] = SUNSET_DATE
    response.headers["Link"] = (
        '<https://github.com/LyonMoncef/SamsungHealth/blob/main/docs/vault/specs/'
        '2026-05-09-local-first-migration.md>; rel="deprecation"; type="text/markdown"'
    )
    _log.warning(
        "deprecated_endpoint_called",
        endpoint=endpoint,
        sunset=SUNSET_DATE,
        user_id=user_id,
        guidance="Use Android local imports (LocalImportService) instead",
    )
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `mark_deprecated` (function) — lines 23-37

### Imports
- `fastapi`
- `server.logging_config`

### Exports
- `mark_deprecated`
