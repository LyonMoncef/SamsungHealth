---
type: code-source
language: python
file_path: server/main.py
git_blob: bb74d634963f61c056466f567debc6c0894f5af4
last_synced: '2026-04-24T02:34:57Z'
loc: 21
annotations: []
imports:
- pathlib
- fastapi
- fastapi.responses
- fastapi.staticfiles
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
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-24-v2-postgres-routers-cutover]] — symbols: `app`, `startup`

### Imports
- `pathlib`
- `fastapi`
- `fastapi.responses`
- `fastapi.staticfiles`
- `server.routers`
