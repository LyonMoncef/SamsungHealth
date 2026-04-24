---
type: code-source
language: python
file_path: server/db/encrypted.py
git_blob: 7e51c2243b3188075e921cc730b4efab6ce4bbb3
last_synced: '2026-04-24T03:44:01Z'
loc: 64
annotations: []
imports:
- sqlalchemy
- sqlalchemy.types
- server.security.crypto
exports:
- EncryptedBytes
- EncryptedString
- EncryptedInt
tags:
- code
- python
---

# server/db/encrypted.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/db/encrypted.py`](../../../server/db/encrypted.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""SQLAlchemy TypeDecorator wrappers pour chiffrement transparent (V2.2)."""
from __future__ import annotations

from sqlalchemy import LargeBinary
from sqlalchemy.types import TypeDecorator

from server.security.crypto import decrypt_field, encrypt_field


class EncryptedBytes(TypeDecorator):
    """Stocke `bytes` chiffrés en BYTEA. Les valeurs Python restent des bytes."""

    impl = LargeBinary
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        if not isinstance(value, (bytes, bytearray, memoryview)):
            raise TypeError(f"EncryptedBytes attend bytes, got {type(value).__name__}")
        return encrypt_field(bytes(value))

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        return decrypt_field(bytes(value))


class EncryptedString(TypeDecorator):
    """Stocke `str` chiffrés en BYTEA. Sérialise UTF-8."""

    impl = LargeBinary
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        if not isinstance(value, str):
            raise TypeError(f"EncryptedString attend str, got {type(value).__name__}")
        return encrypt_field(value.encode("utf-8"))

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        return decrypt_field(bytes(value)).decode("utf-8")


class EncryptedInt(TypeDecorator):
    """Stocke `int` chiffrés en BYTEA. Sérialise via str(int).encode('ascii')."""

    impl = LargeBinary
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        if not isinstance(value, int) or isinstance(value, bool):
            raise TypeError(f"EncryptedInt attend int, got {type(value).__name__}")
        return encrypt_field(str(value).encode("ascii"))

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        return int(decrypt_field(bytes(value)).decode("ascii"))
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — symbols: `EncryptedBytes`, `EncryptedString`, `EncryptedInt`

### Symbols
- `EncryptedBytes` (class) — lines 10-26 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `EncryptedString` (class) — lines 29-45 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `EncryptedInt` (class) — lines 48-64 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]

### Imports
- `sqlalchemy`
- `sqlalchemy.types`
- `server.security.crypto`

### Exports
- `EncryptedBytes`
- `EncryptedString`
- `EncryptedInt`
