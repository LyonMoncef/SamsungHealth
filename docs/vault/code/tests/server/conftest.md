---
type: code-source
language: python
file_path: tests/server/conftest.py
git_blob: 1a40d6b679ba94363dfc8fe364e2025755eac13d
last_synced: '2026-04-24T01:39:50Z'
loc: 43
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
    return pg_container.get_connection_url()


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
