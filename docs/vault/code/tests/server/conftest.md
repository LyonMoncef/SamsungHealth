---
type: code-source
language: python
file_path: tests/server/conftest.py
git_blob: 5266560d90849a167e090f570cfa9adcd9aa5c04
last_synced: '2026-04-24T01:54:48Z'
loc: 47
annotations: []
imports:
- pytest
exports: []
tags:
- code
- python
---

# tests/server/conftest.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`tests/server/conftest.py`](../../../tests/server/conftest.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""
Fixtures Postgres pour la spec V2.1.

Requiert testcontainers[postgres] et Docker. Si l'un des deux manque,
les tests dépendant de `pg_url` sont skip avec message clair.
"""
import pytest


@pytest.fixture(scope="session")
def pg_container():
    try:
        from testcontainers.postgres import PostgresContainer
    except ImportError:
        pytest.skip("testcontainers[postgres] non installé — `pip install testcontainers[postgres]`")
    try:
        with PostgresContainer("postgres:16-alpine") as pg:
            yield pg
    except Exception as exc:  # pragma: no cover — Docker indispo / bind fail
        pytest.skip(f"Docker Postgres indisponible : {exc}")


@pytest.fixture
def pg_url(pg_container):
    raw = pg_container.get_connection_url()
    # Force psycopg 3 driver (testcontainers default = psycopg2 which n'est pas installé)
    return raw.replace("postgresql+psycopg2://", "postgresql+psycopg://").replace(
        "postgresql://", "postgresql+psycopg://"
    )


@pytest.fixture
def engine(pg_url):
    from sqlalchemy import create_engine
    eng = create_engine(pg_url, future=True)
    try:
        yield eng
    finally:
        eng.dispose()


@pytest.fixture
def db_session(engine):
    from sqlalchemy.orm import sessionmaker
    SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)
    with SessionLocal() as sess:
        yield sess
```

---

## Appendix — symbols & navigation *(auto)*

### Imports
- `pytest`
