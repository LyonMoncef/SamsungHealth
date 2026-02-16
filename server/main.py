from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pathlib import Path
from server.database import init_db
from server.routers import sleep, steps, heartrate, exercise

app = FastAPI(title="SamsungHealth")
app.include_router(sleep.router)
app.include_router(steps.router)
app.include_router(heartrate.router)
app.include_router(exercise.router)

static_dir = Path(__file__).resolve().parent.parent / "static"
app.mount("/static", StaticFiles(directory=str(static_dir)), name="static")


@app.on_event("startup")
def startup():
    init_db()


@app.get("/")
def index():
    return FileResponse(str(static_dir / "index.html"))
