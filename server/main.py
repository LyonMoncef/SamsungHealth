import time
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from server.logging_config import configure_logging, get_logger
from server.middleware.request_context import RequestContextMiddleware


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
    configure_logging()
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
    wall_ms = _bench_argon2()
    get_logger("server.main").info("auth.argon2.bench", wall_ms=round(wall_ms, 1))
    yield


from server.routers import admin, auth, exercise, heartrate, mood, sleep, steps  # noqa: E402

app = FastAPI(title="SamsungHealth", lifespan=lifespan)
app.add_middleware(RequestContextMiddleware)
app.include_router(auth.router)
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
