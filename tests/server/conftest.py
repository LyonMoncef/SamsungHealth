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


@pytest.fixture
def client_pg_ready(schema_ready, client_pg):
    """TestClient + schema PG migré (combine schema_ready + client_pg)."""
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
