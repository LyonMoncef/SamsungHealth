---
type: code-source
language: python
file_path: server/main.py
git_blob: a14c382065d9c20ef6a99b7221c984b7e8df38ea
last_synced: '2026-04-26T14:46:49Z'
loc: 41
annotations: []
imports:
- contextlib
- pathlib
- fastapi
- fastapi.responses
- fastapi.staticfiles
- server.logging_config
- server.middleware.request_context
- server.routers
exports:
- _validate_encryption_at_boot
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
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles

from server.logging_config import configure_logging
from server.middleware.request_context import RequestContextMiddleware


def _validate_encryption_at_boot() -> None:
    """V2.2 fail-fast — appelée au lifespan startup. Le test_boot_validation l'invoque directement."""
    from server.security.crypto import load_encryption_key
    load_encryption_key()


@asynccontextmanager
async def lifespan(app: FastAPI):
    configure_logging()
    _validate_encryption_at_boot()
    yield


from server.routers import exercise, heartrate, mood, sleep, steps  # noqa: E402

app = FastAPI(title="SamsungHealth", lifespan=lifespan)
app.add_middleware(RequestContextMiddleware)
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
- [[../../specs/2026-04-26-v2-structlog-observability]] — symbols: `app`, `lifespan`

### Symbols
- `_validate_encryption_at_boot` (function) — lines 12-15 · **Specs**: [[../../specs/2026-04-24-v2-aes256-gcm-encrypted-fields|2026-04-24-v2-aes256-gcm-encrypted-fields]]

### Imports
- `contextlib`
- `pathlib`
- `fastapi`
- `fastapi.responses`
- `fastapi.staticfiles`
- `server.logging_config`
- `server.middleware.request_context`
- `server.routers`

### Exports
- `_validate_encryption_at_boot`
