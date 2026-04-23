---
type: code-source
language: python
file_path: server/main.py
git_blob: 30cef3a0065aa5d95cacd4b32cdf6f13af7dc8e7
last_synced: '2026-04-23T10:49:30Z'
loc: 25
annotations: []
imports:
- fastapi
- fastapi.staticfiles
- fastapi.responses
- pathlib
- server.database
- server.routers
exports: []
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
```

---

## Appendix — symbols & navigation *(auto)*

### Imports
- `fastapi`
- `fastapi.staticfiles`
- `fastapi.responses`
- `pathlib`
- `server.database`
- `server.routers`
