---
type: code-source
language: python
file_path: server/main.py
git_blob: 022cbb69ff95996b4765e4df08fd0c8a8d6c32ee
last_synced: '2026-04-27T17:56:06Z'
loc: 113
annotations: []
imports:
- time
- contextlib
- pathlib
- fastapi
- fastapi.responses
- fastapi.staticfiles
- slowapi.errors
- server.logging_config
- server.middleware.rate_limit_context
- server.middleware.request_context
- server.middleware.slowapi_pre_auth
- server.security.rate_limit
- server.routers
exports:
- _validate_encryption_at_boot
- _bench_argon2
tags:
- code
- python
coverage_pct: 94.73684210526316
---

# server/main.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/main.py`](../../../server/main.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
import time
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from slowapi.errors import RateLimitExceeded

from server.logging_config import configure_logging, get_logger
from server.middleware.rate_limit_context import RateLimitContextMiddleware
from server.middleware.request_context import RequestContextMiddleware
from server.middleware.slowapi_pre_auth import SlowAPIPreAuthMiddleware
from server.security.rate_limit import (
    _rate_limit_exceeded_handler,
    _validate_trusted_proxies_at_boot,
    limiter,
)


def _validate_encryption_at_boot() -> None:
    """V2.2 fail-fast — appelée au lifespan startup. Le test_boot_validation l'invoque directement."""
    from server.security.crypto import load_encryption_key
    load_encryption_key()


def _bench_argon2() -> float:
    """V2.3 — bench argon2 wall-clock at boot (info log only, no fail)."""
    from server.security.auth import hash_password
    t0 = time.perf_counter()
    hash_password("____boot_bench_unused____")
    return (time.perf_counter() - t0) * 1000.0


@asynccontextmanager
async def lifespan(app: FastAPI):
    import os as _os

    configure_logging()
    # V2.3.3.1 — fail fast on rate-limit env BEFORE other validators.
    _validate_trusted_proxies_at_boot()
    _validate_encryption_at_boot()
    # V2.3 — validate JWT secret + registration token (warning if reg absent).
    from server.security.auth import (
        _validate_email_hash_salt_at_boot,
        _validate_jwt_secret_at_boot,
        _validate_public_base_url_at_boot,
        _validate_registration_token,
    )
    _validate_jwt_secret_at_boot()
    _validate_registration_token()
    # V2.3.1 — validate the two new env vars (PUBLIC_BASE_URL + EMAIL_HASH_SALT).
    _validate_public_base_url_at_boot()
    _validate_email_hash_salt_at_boot()
    # V2.3.2 — Google OAuth boot validation (raise if partial env, ok if both/neither).
    from server.security.auth_providers.google import (
        _validate_google_oauth_env_at_boot,
    )
    _validate_google_oauth_env_at_boot()
    # V2.3.2 — warning if multi-instance (in-memory state cache is single-instance).
    try:
        instances = int(_os.environ.get("SAMSUNGHEALTH_DEPLOYMENT_INSTANCES", "1"))
    except ValueError:
        instances = 1
    if instances > 1:
        get_logger("server.main").warning(
            "oauth.state_cache.multi_instance_unsafe", instances=instances
        )
    wall_ms = _bench_argon2()
    get_logger("server.main").info("auth.argon2.bench", wall_ms=round(wall_ms, 1))
    yield


from server.routers import (  # noqa: E402
    admin,
    auth,
    auth_oauth,
    exercise,
    heartrate,
    mood,
    sleep,
    steps,
)

app = FastAPI(title="SamsungHealth", lifespan=lifespan)

# V2.3.3.1 — wire slowapi.
# Starlette wraps middlewares: last add_middleware → outermost (runs first on request).
# Order so that body-peek for composite keys runs OUTSIDE slowapi (so request.state
# is set before slowapi key_func is invoked), and per-route slowapi check runs
# BEFORE FastAPI dependency resolution (so rate-limit fires before auth — H1 / #43).
app.state.limiter = limiter
app.add_middleware(RequestContextMiddleware)       # innermost
app.add_middleware(SlowAPIPreAuthMiddleware)       # middle — per-route check pre-deps
app.add_middleware(RateLimitContextMiddleware)     # outermost — peeks body BEFORE slowapi
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.include_router(auth.router)
app.include_router(auth_oauth.router)
app.include_router(admin.router)
app.include_router(sleep.router)
app.include_router(steps.router)
app.include_router(heartrate.router)
app.include_router(exercise.router)
app.include_router(mood.router)

static_dir = Path(__file__).resolve().parent.parent / "static"
app.mount("/static", StaticFiles(directory=str(static_dir)), name="static")


@app.get("/")
def index():
    return FileResponse(str(static_dir / "index.html"))
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields]] — symbols: `app`, `_validate_encryption_at_boot`
- [[../../specs/2026-04-24-v2-postgres-routers-cutover]] — symbols: `app`, `startup`
- [[../../specs/2026-04-26-v2-auth-foundation]] — symbols: `app`, `lifespan`
- [[../../specs/2026-04-26-v2-structlog-observability]] — symbols: `app`, `lifespan`
- [[../../specs/2026-04-26-v2.3.1-reset-password-email-verify]] — symbols: `lifespan`
- [[../../specs/2026-04-26-v2.3.2-google-oauth]] — symbols: `lifespan`
- [[../../specs/2026-04-26-v2.3.3.1-rate-limit-lockout]] — symbols: `app`, `lifespan`

### Symbols
- `_validate_encryption_at_boot` (function) — lines 21-24 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]
- `_bench_argon2` (function) — lines 27-32

### Imports
- `time`
- `contextlib`
- `pathlib`
- `fastapi`
- `fastapi.responses`
- `fastapi.staticfiles`
- `slowapi.errors`
- `server.logging_config`
- `server.middleware.rate_limit_context`
- `server.middleware.request_context`
- `server.middleware.slowapi_pre_auth`
- `server.security.rate_limit`
- `server.routers`

### Exports
- `_validate_encryption_at_boot`
- `_bench_argon2`
