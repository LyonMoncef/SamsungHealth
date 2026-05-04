---
type: code-source
language: python
file_path: server/db/uuid7.py
git_blob: f74460ed5d6661d7a2485cc38fe83ffd03329b54
last_synced: '2026-04-24T01:43:58Z'
loc: 33
annotations: []
imports:
- uuid_utils
- sqlalchemy
- sqlalchemy.dialects.postgresql
- sqlalchemy.types
exports:
- uuid7
- Uuid7
tags:
- code
- python
---

# server/db/uuid7.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/db/uuid7.py`](../../../server/db/uuid7.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""UUID v7 helper + SQLAlchemy TypeDecorator (spec V2.1 postgres-migration)."""
import uuid as _uuid

import uuid_utils
from sqlalchemy import CHAR
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.types import TypeDecorator


def uuid7() -> _uuid.UUID:
    return _uuid.UUID(bytes=uuid_utils.uuid7().bytes)


class Uuid7(TypeDecorator):
    impl = CHAR
    cache_ok = True

    def load_dialect_impl(self, dialect):
        if dialect.name == "postgresql":
            return dialect.type_descriptor(PG_UUID(as_uuid=True))
        return dialect.type_descriptor(CHAR(36))

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        if isinstance(value, _uuid.UUID):
            return value if dialect.name == "postgresql" else str(value)
        return _uuid.UUID(value) if dialect.name == "postgresql" else str(_uuid.UUID(value))

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        return value if isinstance(value, _uuid.UUID) else _uuid.UUID(value)
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-postgres-migration]] — symbols: `uuid7`

### Symbols
- `uuid7` (function) — lines 10-11 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]]
- `Uuid7` (class) — lines 14-33

### Imports
- `uuid_utils`
- `sqlalchemy`
- `sqlalchemy.dialects.postgresql`
- `sqlalchemy.types`

### Exports
- `uuid7`
- `Uuid7`
