---
type: code-source
language: python
file_path: alembic/env.py
git_blob: f9daf7777db3b32228f5df740d48007bb8c88e25
last_synced: '2026-04-24T01:54:48Z'
loc: 48
annotations: []
imports:
- os
- logging.config
- alembic
- sqlalchemy
- server.db.models
exports:
- run_migrations_offline
- run_migrations_online
tags:
- code
- python
---

# alembic/env.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`alembic/env.py`](../../../alembic/env.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
import os
from logging.config import fileConfig

from alembic import context
from sqlalchemy import engine_from_config, pool

from server.db.models import Base

config = context.config

if config.config_file_name is not None:
    fileConfig(config.config_file_name)

db_url = os.environ.get("DATABASE_URL")
if db_url:
    config.set_main_option("sqlalchemy.url", db_url)

target_metadata = Base.metadata


def run_migrations_offline() -> None:
    url = config.get_main_option("sqlalchemy.url")
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
    )
    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    connectable = engine_from_config(
        config.get_section(config.config_ini_section, {}),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )
    with connectable.connect() as connection:
        context.configure(connection=connection, target_metadata=target_metadata)
        with context.begin_transaction():
            context.run_migrations()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-postgres-migration]]

### Symbols
- `run_migrations_offline` (function) — lines 21-30
- `run_migrations_online` (function) — lines 33-42

### Imports
- `os`
- `logging.config`
- `alembic`
- `sqlalchemy`
- `server.db.models`

### Exports
- `run_migrations_offline`
- `run_migrations_online`
