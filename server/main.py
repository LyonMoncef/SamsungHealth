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
from server.middleware.security_headers import SecurityHeadersMiddleware
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
    me as me_router,
    mood,
    sleep,
    static_pages,
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
# V2.3.3.2 — security headers wrap the response BEFORE slowapi can inject its own
# rate-limit headers; mounted between request_context (innermost) and slowapi
# layers so all responses (404, 4xx, 5xx) carry CSP/XFO/etc.
app.add_middleware(SecurityHeadersMiddleware)
app.add_middleware(SlowAPIPreAuthMiddleware)       # middle — per-route check pre-deps
app.add_middleware(RateLimitContextMiddleware)     # outermost — peeks body BEFORE slowapi
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# V2.3.3.2 — static_pages router AVANT auth router pour que GET /auth/login
# soit servi par la page HTML (et non un 405 du POST /auth/login).
app.include_router(static_pages.router)
app.include_router(auth.router)
app.include_router(auth_oauth.router)
app.include_router(admin.router)
app.include_router(sleep.router)
app.include_router(steps.router)
app.include_router(heartrate.router)
app.include_router(exercise.router)
app.include_router(mood.router)
app.include_router(me_router.router)

static_dir = Path(__file__).resolve().parent.parent / "static"
app.mount("/static", StaticFiles(directory=str(static_dir)), name="static")


@app.get("/")
def index():
    return FileResponse(str(static_dir / "index.html"))
