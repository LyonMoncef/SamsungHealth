from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles


def _validate_encryption_at_boot() -> None:
    """V2.2 fail-fast — appelée au lifespan startup. Le test_boot_validation l'invoque directement."""
    from server.security.crypto import load_encryption_key
    load_encryption_key()


@asynccontextmanager
async def lifespan(app: FastAPI):
    _validate_encryption_at_boot()
    yield


from server.routers import exercise, heartrate, mood, sleep, steps  # noqa: E402

app = FastAPI(title="SamsungHealth", lifespan=lifespan)
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
