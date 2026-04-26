---
type: code-source
language: python
file_path: tests/server/conftest.py
git_blob: 1dc4923e211faf3797d4b05cafde7c930cee5471
last_synced: '2026-04-26T16:48:28Z'
loc: 207
annotations: []
imports:
- base64
- os
- secrets
- pytest
exports:
- _register_default_user
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
Fixtures Postgres + chiffrement pour les specs server-side.

Requiert testcontainers[postgres] et Docker. Si l'un des deux manque,
les tests dépendant de `pg_url` sont skip avec message clair.
"""
import base64
import os
import secrets

import pytest


_TEST_KEY_B64 = base64.b64encode(b"v2_2_test_key_32_bytes_exactly__")[:44].decode("ascii")

# V2.3 — JWT + registration token defaults for tests.
_TEST_JWT_SECRET = "dGVzdC1qd3Qtc2VjcmV0LXdpdGgtMzItYnl0ZXMtbWluLW9rITE="
_TEST_REGISTRATION_TOKEN = "registration-token-32-chars-or-more-test1234"

# Test files where `client_pg_ready` should NOT auto-inject a default user
# Authorization header (those test the auth flow itself, or test 401-without-token).
_NO_AUTO_AUTH_FILES = frozenset(
    {
        "test_auth_routes.py",
        "test_auth_events.py",
        "test_health_routes_auth.py",
        "test_password_hashing.py",
        "test_jwt.py",
        "test_redaction.py",
    }
)


@pytest.fixture(autouse=True)
def _set_test_encryption_key(monkeypatch):
    """V2.2 — clé de test stable, set avant chaque test, reset le cache lru."""
    monkeypatch.setenv("SAMSUNGHEALTH_ENCRYPTION_KEY", _TEST_KEY_B64)
    try:
        from server.security.crypto import reset_key_cache
        reset_key_cache()
        yield
        reset_key_cache()
    except ImportError:
        yield  # pas encore implémenté, OK pour les tests RED de la fondation


@pytest.fixture(autouse=True)
def _set_auth_env_defaults(monkeypatch):
    """V2.3 — set JWT + registration env to test values for ALL tests in tests/server/."""
    if not os.environ.get("SAMSUNGHEALTH_JWT_SECRET"):
        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)
    if not os.environ.get("SAMSUNGHEALTH_REGISTRATION_TOKEN"):
        monkeypatch.setenv("SAMSUNGHEALTH_REGISTRATION_TOKEN", _TEST_REGISTRATION_TOKEN)
    yield


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
    from sqlalchemy import text
    from sqlalchemy.orm import sessionmaker

    SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)
    with SessionLocal() as sess:
        yield sess
        sess.rollback()
    # Isolation forte entre tests : truncate cascade sur toutes les tables non-alembic
    with engine.begin() as conn:
        rows = conn.execute(
            text("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename != 'alembic_version'")
        ).fetchall()
        if rows:
            tables = ", ".join(r[0] for r in rows)
            conn.execute(text(f"TRUNCATE TABLE {tables} RESTART IDENTITY CASCADE"))


@pytest.fixture(autouse=True)
def _pg_truncate_between_tests(request, engine):
    """Truncate cascade entre chaque test PG. Skip si test ne touche pas PG (pg_container pas demandé)."""
    yield
    if "pg_container" not in request.fixturenames:
        return
    from sqlalchemy import text
    try:
        with engine.begin() as conn:
            rows = conn.execute(
                text("SELECT tablename FROM pg_tables WHERE schemaname='public' AND tablename != 'alembic_version'")
            ).fetchall()
            if rows:
                tables = ", ".join(r[0] for r in rows)
                conn.execute(text(f"TRUNCATE TABLE {tables} RESTART IDENTITY CASCADE"))
    except Exception:
        pass  # PG pas dispo pour ce test (skip)


@pytest.fixture
def schema_ready(pg_url):
    import os
    import subprocess
    env = os.environ.copy()
    env["DATABASE_URL"] = pg_url
    res = subprocess.run(
        ["alembic", "upgrade", "head"],
        capture_output=True, text=True, env=env, check=False,
    )
    assert res.returncode == 0, f"alembic upgrade head échoué : {res.stderr}"
    yield pg_url


def _register_default_user(client) -> str | None:
    """V2.3 — register + login a default test user, return access_token (None on failure)."""
    email = "default-test-user@samsunghealth.local"
    password = "default-test-password-1234"
    reg = client.post(
        "/auth/register",
        headers={"X-Registration-Token": _TEST_REGISTRATION_TOKEN},
        json={"email": email, "password": password},
    )
    if reg.status_code not in (201, 409):
        return None
    log = client.post("/auth/login", json={"email": email, "password": password})
    if log.status_code != 200:
        return None
    return log.json().get("access_token")


@pytest.fixture
def client_pg_ready(request, schema_ready, client_pg):
    """TestClient + schema PG migré.

    V2.3 — pour les tests pré-V2.3 (sleep/heartrate/steps/exercise/mood routers,
    encryption tests), on register/login un user par défaut et on injecte
    automatiquement le header `Authorization: Bearer <access>` sur toutes les
    requêtes. Les tests V2.3 (auth_routes, health_routes_auth, auth_events,
    password_hashing, jwt, redaction) ne reçoivent PAS ce header (ils
    construisent leur propre flow ou testent le 401-sans-token).

    Détection : par basename du fichier de test (cf. `_NO_AUTO_AUTH_FILES`).
    """
    test_file = os.path.basename(str(request.node.fspath))
    if test_file in _NO_AUTO_AUTH_FILES:
        return client_pg

    access = _register_default_user(client_pg)
    if access is None:
        # No-op fallback — leaves client_pg as-is (tests will fail loudly).
        return client_pg
    client_pg.headers.update({"Authorization": f"Bearer {access}"})
    return client_pg


@pytest.fixture
def client_pg(pg_url, engine):
    """TestClient FastAPI avec dependency_override sur get_session vers le testcontainer PG.

    Requiert que le schema soit déjà migré (utiliser conjointement avec un fixture qui run alembic upgrade head).
    """
    from fastapi.testclient import TestClient
    from sqlalchemy.orm import sessionmaker

    from server.database import get_session
    from server.main import app

    SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)

    def _override():
        sess = SessionLocal()
        try:
            yield sess
        finally:
            sess.close()

    app.dependency_overrides[get_session] = _override
    with TestClient(app) as client:
        yield client
    app.dependency_overrides.clear()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `_register_default_user` (function) — lines 141-155

### Imports
- `base64`
- `os`
- `secrets`
- `pytest`

### Exports
- `_register_default_user`
