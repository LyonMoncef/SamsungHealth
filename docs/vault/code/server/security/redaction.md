---
type: code-source
language: python
file_path: server/security/redaction.py
git_blob: 915647d644cc90e9a9181d2864dd80c85e2140de
last_synced: '2026-04-27T07:34:23Z'
loc: 61
annotations: []
imports:
- typing
exports:
- _recurse_redact
- _redact_one
- redact_sensitive_keys
tags:
- code
- python
---

# server/security/redaction.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/security/redaction.py`](../../../server/security/redaction.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""V2.3 — structlog redaction processor.

Remplace les valeurs des clés sensibles par '[REDACTED]' dans event_dict
avant le rendu final. Recurse dans les dicts imbriqués. Match case-insensitive.
"""
from __future__ import annotations

from typing import Any


_SENSITIVE_KEYS: frozenset[str] = frozenset(
    {
        "password",
        "password_hash",
        "token",
        "refresh_token",
        "access_token",
        "authorization",
        "jwt",
        "secret",
        "cookie",
        "x-registration-token",
        # V2.3.2 — OAuth flow secrets / opaque payloads.
        "code",
        "state",
        "id_token",
        "nonce",
        "error_description",
    }
)


_REDACTED = "[REDACTED]"


def _recurse_redact(value: Any) -> Any:
    if isinstance(value, dict):
        return {k: _redact_one(k, v) for k, v in value.items()}
    return value


def _redact_one(key: str, value: Any) -> Any:
    if isinstance(key, str) and key.lower() in _SENSITIVE_KEYS:
        return _REDACTED
    if isinstance(value, dict):
        return _recurse_redact(value)
    return value


def redact_sensitive_keys(logger, method_name, event_dict):
    """structlog processor — replace sensitive values by '[REDACTED]'.

    Signature: (logger, method_name, event_dict) -> event_dict
    """
    for key in list(event_dict.keys()):
        value = event_dict[key]
        if isinstance(key, str) and key.lower() in _SENSITIVE_KEYS:
            event_dict[key] = _REDACTED
        elif isinstance(value, dict):
            event_dict[key] = _recurse_redact(value)
    return event_dict
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2-auth-foundation]] — symbols: `redact_sensitive_keys`, `_SENSITIVE_KEYS`
- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — symbols: `_SENSITIVE_KEYS`

### Symbols
- `_recurse_redact` (function) — lines 36-39
- `_redact_one` (function) — lines 42-47
- `redact_sensitive_keys` (function) — lines 50-61 · **Specs**: [[../../specs/2026-04-26-v2-auth-foundation|2026-04-26-v2-auth-foundation]]

### Imports
- `typing`

### Exports
- `_recurse_redact`
- `_redact_one`
- `redact_sensitive_keys`
