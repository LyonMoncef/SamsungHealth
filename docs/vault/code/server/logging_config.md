---
type: code-source
language: python
file_path: server/logging_config.py
git_blob: f8eab5110198064041899bb935e15942a2ea25b7
last_synced: '2026-04-26T14:46:49Z'
loc: 80
annotations: []
imports:
- logging
- os
- sys
- structlog
exports:
- _resolve_env
- _resolve_level
- _build_processors
- configure_logging
- get_logger
tags:
- code
- python
---

# server/logging_config.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/logging_config.py`](../../../server/logging_config.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Structlog observability foundation (V2.0.5).

Pipeline de logs structurés JSONL en prod, ConsoleRenderer en dev.
Bridge stdlib → structlog pour que uvicorn/sqlalchemy passent par le même rendu.
ContextVars (request_id, user_id) injectées automatiquement via processor.
"""
from __future__ import annotations

import logging
import os
import sys

import structlog


_VALID_LEVELS = {"DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"}


def _resolve_env(env: str | None) -> str:
    if env is not None:
        return env.lower()
    return os.environ.get("APP_ENV", "prod").lower()


def _resolve_level(level: str | None) -> int:
    raw = (level if level is not None else os.environ.get("LOG_LEVEL", "INFO")).upper()
    if raw not in _VALID_LEVELS:
        logging.getLogger(__name__).warning(
            "LOG_LEVEL invalide '%s', fallback INFO", raw
        )
        raw = "INFO"
    return getattr(logging, raw)


def _build_processors(env: str) -> list:
    """Compose la chaîne de processors structlog. Ordre = important."""
    shared: list = [
        structlog.contextvars.merge_contextvars,
        structlog.processors.add_log_level,
        structlog.processors.TimeStamper(fmt="iso", utc=True, key="timestamp"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
    ]
    if env == "dev":
        shared.append(structlog.dev.ConsoleRenderer(colors=True))
    else:
        shared.append(structlog.processors.JSONRenderer())
    return shared


def configure_logging(env: str | None = None, level: str | None = None) -> None:
    """Configure structlog + bridge stdlib. Idempotent — peut être ré-appelé."""
    resolved_env = _resolve_env(env)
    resolved_level = _resolve_level(level)

    processors = _build_processors(resolved_env)

    # Reset handlers du root logger pour éviter doublons quand on re-configure
    # (les tests appellent configure_logging à chaque test).
    root = logging.getLogger()
    for handler in list(root.handlers):
        root.removeHandler(handler)

    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(logging.Formatter("%(message)s"))
    root.addHandler(handler)
    root.setLevel(resolved_level)

    structlog.configure(
        processors=processors,
        wrapper_class=structlog.make_filtering_bound_logger(resolved_level),
        context_class=dict,
        logger_factory=structlog.PrintLoggerFactory(file=sys.stdout),
        cache_logger_on_first_use=False,
    )


def get_logger(name: str) -> structlog.stdlib.BoundLogger:
    """Retourne un logger structlog avec scope (`logger` field) bindé sur `name`."""
    return structlog.get_logger(name).bind(logger=name)
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-26-v2-structlog-observability]] — symbols: `configure_logging`, `get_logger`, `_processors`

### Symbols
- `_resolve_env` (function) — lines 19-22
- `_resolve_level` (function) — lines 25-32
- `_build_processors` (function) — lines 35-48
- `configure_logging` (function) — lines 51-75 · **Specs**: [[../../specs/2026-04-26-v2-structlog-observability|2026-04-26-v2-structlog-observability]]
- `get_logger` (function) — lines 78-80 · **Specs**: [[../../specs/2026-04-26-v2-structlog-observability|2026-04-26-v2-structlog-observability]]

### Imports
- `logging`
- `os`
- `sys`
- `structlog`

### Exports
- `_resolve_env`
- `_resolve_level`
- `_build_processors`
- `configure_logging`
- `get_logger`
