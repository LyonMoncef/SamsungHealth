---
type: code-source
language: python
file_path: tests/server/conftest.py
git_blob: 70f48eaf6b034962dfaa191b5f463a85a20fe8d4
last_synced: '2026-04-27T17:56:06Z'
loc: 523
annotations: []
imports:
- base64
- os
- secrets
- pytest
exports:
- _ensure_orm_default_user
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
        # V2.3.1 — reset password + email verification (flows nus, sans Bearer)
        "test_verification_tokens.py",
        "test_email_verify_routes.py",
        "test_password_reset_routes.py",
        "test_admin_pending_verifications.py",
        "test_password_blocklist.py",
        "test_alembic_0006.py",
        "test_host_header_injection.py",
        "test_email_hash_salt.py",
        "test_login_email_verification_gate.py",
        # V2.3.2 — Google OAuth (callback non authentifié via Bearer)
        "test_oauth_state.py",
        "test_google_provider.py",
        "test_return_to_validator.py",
        "test_oauth_routes.py",
        "test_oauth_link_confirm.py",
        "test_oauth_password_reset.py",
        "test_oauth_redaction.py",
        "test_alembic_0007.py",
        # V2.3.3.1 — rate-limit + lockout + admin lock + IP resolution
        "test_rate_limit.py",
        "test_ip_resolution.py",
        "test_lockout.py",
        "test_admin_lock.py",
        "test_email_global_cap.py",
        "test_alembic_0008.py",
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


_TEST_PUBLIC_BASE_URL = "http://localhost:8000"
_TEST_EMAIL_HASH_SALT = (
    "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
)

# V2.3.2 — Google OAuth env defaults (tests).
_TEST_GOOGLE_CLIENT_ID = "test-client-id-123.apps.googleusercontent.com"
_TEST_GOOGLE_CLIENT_SECRET = "GOCSPX-test-secret"
_TEST_OAUTH_RETURN_TO_ALLOWED = (
    "https://app.samsunghealth.com,http://localhost:3000"
)
_TEST_OAUTH_AUTO_REGISTER = "true"


@pytest.fixture(autouse=True)
def _reset_oauth_caches():
    """V2.3.2 — clear in-memory OAuth state cache + JWKS cache between tests.

    Both caches are module-level globals; otherwise an RSA keypair forged by
    test #1 leaks into the JWKS resolution of test #2 (signature mismatch).
    """
    yield
    try:
        from server.security.auth_providers import google as _g
        _g._JWKS_CACHE = None
        _g._JWKS_CACHE_EXPIRES_AT = None
    except ImportError:
        pass
    try:
        from server.security.auth_providers import state as _s
        _s._OAUTH_STATE_CACHE.clear()
    except ImportError:
        pass


@pytest.fixture(autouse=True)
def _set_auth_env_defaults(monkeypatch):
    """V2.3 — set JWT + registration env to test values for ALL tests in tests/server/.

    V2.3.1 — also seed PUBLIC_BASE_URL + EMAIL_HASH_SALT defaults (lifespan validates these).
    V2.3.2 — also seed Google OAuth client_id/secret + return_to whitelist + auto_register.
    """
    if not os.environ.get("SAMSUNGHEALTH_JWT_SECRET"):
        monkeypatch.setenv("SAMSUNGHEALTH_JWT_SECRET", _TEST_JWT_SECRET)
    if not os.environ.get("SAMSUNGHEALTH_REGISTRATION_TOKEN"):
        monkeypatch.setenv("SAMSUNGHEALTH_REGISTRATION_TOKEN", _TEST_REGISTRATION_TOKEN)
    if not os.environ.get("SAMSUNGHEALTH_PUBLIC_BASE_URL"):
        monkeypatch.setenv("SAMSUNGHEALTH_PUBLIC_BASE_URL", _TEST_PUBLIC_BASE_URL)
    if not os.environ.get("SAMSUNGHEALTH_EMAIL_HASH_SALT"):
        monkeypatch.setenv("SAMSUNGHEALTH_EMAIL_HASH_SALT", _TEST_EMAIL_HASH_SALT)
    if not os.environ.get("SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_ID"):
        monkeypatch.setenv(
            "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_ID", _TEST_GOOGLE_CLIENT_ID
        )
    if not os.environ.get("SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_SECRET"):
        monkeypatch.setenv(
            "SAMSUNGHEALTH_GOOGLE_OAUTH_CLIENT_SECRET", _TEST_GOOGLE_CLIENT_SECRET
        )
    if not os.environ.get("SAMSUNGHEALTH_OAUTH_RETURN_TO_ALLOWED"):
        monkeypatch.setenv(
            "SAMSUNGHEALTH_OAUTH_RETURN_TO_ALLOWED", _TEST_OAUTH_RETURN_TO_ALLOWED
        )
    if not os.environ.get("SAMSUNGHEALTH_OAUTH_AUTO_REGISTER"):
        monkeypatch.setenv(
            "SAMSUNGHEALTH_OAUTH_AUTO_REGISTER", _TEST_OAUTH_AUTO_REGISTER
        )
    # V2.3.3.1 — rate-limit + lockout test defaults.
    if not os.environ.get("SAMSUNGHEALTH_ENV"):
        monkeypatch.setenv("SAMSUNGHEALTH_ENV", "test")
    if not os.environ.get("SAMSUNGHEALTH_TRUSTED_PROXIES"):
        monkeypatch.setenv("SAMSUNGHEALTH_TRUSTED_PROXIES", "127.0.0.1,::1")
    yield


@pytest.fixture(autouse=True)
def _reset_rate_limit_state():
    """V2.3.3.1 — clear slowapi in-memory storage between tests.

    Without this, a 5/min cap from test A leaks into test B and produces
    flaky 429s. The module + storage may not exist yet during RED phase,
    so we no-op cleanly on ImportError / AttributeError.
    """
    yield
    try:
        from server.security import rate_limit as _rl

        # Limiter exposes a storage we can reset (slowapi MemoryStorage has
        # `.storage` dict). The implementation may also expose a helper.
        limiter = getattr(_rl, "limiter", None)
        if limiter is not None:
            for attr in ("reset", "_storage_reset"):
                fn = getattr(limiter, attr, None)
                if callable(fn):
                    try:
                        fn()
                    except Exception:
                        pass
            storage = getattr(limiter, "_storage", None) or getattr(
                limiter, "storage", None
            )
            if storage is not None:
                for attr in ("storage", "_data", "data"):
                    obj = getattr(storage, attr, None)
                    if hasattr(obj, "clear"):
                        try:
                            obj.clear()
                        except Exception:
                            pass
    except Exception:
        pass


# V2.3.2 — RSA keypair + JWKS helper for forging Google ID tokens in tests.
@pytest.fixture
def google_keypair_and_jwks():
    """Generate an RSA keypair + return helpers to sign ID tokens / mock JWKS.

    Returns dict with:
      - `private_pem`: PEM-serialized private key bytes
      - `public_pem`: PEM-serialized public key bytes
      - `kid`: a stable kid string ("test-kid-1")
      - `jwks`: the JWKS dict mock (for httpx GET response)
      - `sign(claims, headers=None)`: return a signed RS256 JWT string
    """
    import base64 as _b64
    import json as _json

    from cryptography.hazmat.primitives import serialization
    from cryptography.hazmat.primitives.asymmetric import rsa

    priv = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    pub = priv.public_key()
    private_pem = priv.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    )
    public_pem = pub.public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    pub_numbers = pub.public_numbers()

    def _b64u(value: int) -> str:
        b = value.to_bytes((value.bit_length() + 7) // 8, "big")
        return _b64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")

    kid = "test-kid-1"
    jwks = {
        "keys": [
            {
                "kty": "RSA",
                "alg": "RS256",
                "use": "sig",
                "kid": kid,
                "n": _b64u(pub_numbers.n),
                "e": _b64u(pub_numbers.e),
            }
        ]
    }

    def _sign(claims: dict, headers: dict | None = None) -> str:
        import jwt as _jwt

        hdr = {"kid": kid}
        if headers:
            hdr.update(headers)
        return _jwt.encode(claims, private_pem, algorithm="RS256", headers=hdr)

    return {
        "private_pem": private_pem,
        "public_pem": public_pem,
        "kid": kid,
        "jwks": jwks,
        "jwks_json": _json.dumps(jwks),
        "sign": _sign,
    }


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


_ORM_DEFAULT_USER_EMAIL = "default-orm-user@samsunghealth.local"
_ORM_DEFAULT_USER_HASH = "$argon2id$v=19$m=46080,t=2,p=1$ORMTESTUSERNOLOGINPOSSIBLE"


def _ensure_orm_default_user(connection):
    """Crée (ou récupère) le user par défaut pour les inserts ORM-only en test."""
    from sqlalchemy import text

    connection.execute(
        text(
            """
            INSERT INTO users (id, email, password_hash, is_active, password_changed_at)
            VALUES (gen_random_uuid(), :email, :hash, false, now())
            ON CONFLICT (email) DO NOTHING
            """
        ),
        {"email": _ORM_DEFAULT_USER_EMAIL, "hash": _ORM_DEFAULT_USER_HASH},
    )
    row = connection.execute(
        text("SELECT id FROM users WHERE email = :email"),
        {"email": _ORM_DEFAULT_USER_EMAIL},
    ).fetchone()
    return row[0]


@pytest.fixture
def default_user_db(schema_ready, db_session):
    """V2.3.0.1 — user d'ancrage pour les tests ORM-only (sans booter le client HTTP).

    Tous les inserts ORM de tables santé peuvent assigner `user_id=default_user_db.id`.
    Le hook `_auto_inject_user_id_for_health_inserts` (autouse) fait l'injection
    automatique pour les tests qui ne définissent pas `user_id`.

    V2.3.3.1 — depend on `schema_ready` so the schema is migrated before insert
    (some test files use `default_user_db` without explicitly listing `schema_ready`).
    """
    from sqlalchemy import select

    from server.db.models import User

    user_id = _ensure_orm_default_user(db_session.connection())
    db_session.commit()
    return db_session.execute(select(User).where(User.id == user_id)).scalar_one()


@pytest.fixture(autouse=True)
def _auto_inject_user_id_for_health_inserts(request):
    """V2.3.0.1 — auto-injection user_id sur inserts ORM des tables santé.

    Évite de patcher chaque test ORM-only qui ferait `db_session.add(SleepSession(...))`
    sans user_id. Hook désactivé pour les tests qui ne touchent pas Postgres.
    """
    if "pg_container" not in request.fixturenames and "schema_ready" not in request.fixturenames:
        yield
        return

    from sqlalchemy import event

    try:
        from server.db.models import Base
    except ImportError:
        yield
        return

    cached_user_id: list[str | None] = [None]

    def _inject(mapper, connection, target):
        if not hasattr(target, "user_id"):
            return
        if getattr(target, "user_id", None) is not None:
            return
        if "user_id" not in {c.name for c in mapper.columns}:
            return
        if cached_user_id[0] is None:
            cached_user_id[0] = _ensure_orm_default_user(connection)
        target.user_id = cached_user_id[0]

    event.listen(Base, "before_insert", _inject, propagate=True)
    yield
    event.remove(Base, "before_insert", _inject)


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


@pytest.fixture(autouse=True)
def _expire_sibling_sessions_on_commit():
    """V2.3.1 — Cross-session identity-map coherence for tests.

    `db_session` and the API session (via `get_session` override) are different
    SA Session instances bound to the same engine. With `expire_on_commit=False`,
    a commit in one session leaves entities cached in the other one — re-querying
    returns stale state. Tests can call `expire_all()` manually but several
    new tests don't. We register an `after_commit` listener that expires
    entities in all *other* tracked sessions, mirroring single-session semantics.
    """
    import weakref

    from sqlalchemy import event
    from sqlalchemy.orm import Session

    active: "weakref.WeakSet[Session]" = weakref.WeakSet()

    def _track(session, transaction, connection):
        active.add(session)

    def _expire_others(session):
        for s in list(active):
            if s is session:
                continue
            try:
                s.expire_all()
            except Exception:
                pass

    event.listen(Session, "after_begin", _track)
    event.listen(Session, "after_commit", _expire_others)
    try:
        yield
    finally:
        event.remove(Session, "after_commit", _expire_others)
        event.remove(Session, "after_begin", _track)


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
- `_ensure_orm_default_user` (function) — lines 340-358
- `_register_default_user` (function) — lines 418-432

### Imports
- `base64`
- `os`
- `secrets`
- `pytest`

### Exports
- `_ensure_orm_default_user`
- `_register_default_user`
