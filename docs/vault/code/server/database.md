---
type: code-source
language: python
file_path: server/database.py
git_blob: 82ba713b5b296ea5f53072aad8241981b0545d95
last_synced: '2026-04-24T02:34:57Z'
loc: 25
annotations: []
imports:
- os
- functools
- sqlalchemy
- sqlalchemy.orm
exports:
- get_session
tags:
- code
- python
coverage_pct: 100.0
---

# server/database.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/database.py`](../../../server/database.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
import os
from functools import lru_cache

from sqlalchemy import Engine, create_engine
from sqlalchemy.orm import Session, sessionmaker

_DEFAULT_PG_URL = "postgresql+psycopg://samsung:samsung@localhost:5432/samsunghealth"


@lru_cache(maxsize=1)
def get_engine() -> Engine:
    url = os.environ.get("DATABASE_URL", _DEFAULT_PG_URL)
    return create_engine(url, future=True, pool_pre_ping=True)


@lru_cache(maxsize=1)
def _session_factory() -> sessionmaker:
    return sessionmaker(bind=get_engine(), expire_on_commit=False, future=True)


SessionLocal = _session_factory


def get_session() -> Session:
    return _session_factory()()
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-postgres-migration]] — symbols: `get_engine`, `get_session`, `SessionLocal`
- [[../../specs/2026-04-24-v2-postgres-routers-cutover]] — symbols: `get_engine`, `get_session`, `SessionLocal`

### Symbols
- `get_session` (function) — lines 24-25 · **Specs**: [[../../specs/2026-04-24-v2-postgres-migration|2026-04-24-v2-postgres-migration]], [[../../specs/2026-04-24-v2-postgres-routers-cutover|2026-04-24-v2-postgres-routers-cutover]]

### Imports
- `os`
- `functools`
- `sqlalchemy`
- `sqlalchemy.orm`

### Exports
- `get_session`
