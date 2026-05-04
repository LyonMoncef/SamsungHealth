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
    from server.security.redaction import redact_sensitive_keys

    shared: list = [
        structlog.contextvars.merge_contextvars,
        redact_sensitive_keys,
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
