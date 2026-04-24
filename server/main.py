from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from server.routers import exercise, heartrate, sleep, steps

app = FastAPI(title="SamsungHealth")
app.include_router(sleep.router)
app.include_router(steps.router)
app.include_router(heartrate.router)
app.include_router(exercise.router)

static_dir = Path(__file__).resolve().parent.parent / "static"
app.mount("/static", StaticFiles(directory=str(static_dir)), name="static")


@app.get("/")
def index():
    return FileResponse(str(static_dir / "index.html"))
