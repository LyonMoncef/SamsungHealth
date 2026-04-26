"""V2.3 — structlog redaction processor.

Remplace les valeurs des clés sensibles par '[REDACTED]' dans event_dict
avant le rendu final. Recurse dans les dicts imbriqués. Match case-insensitive.
"""
from __future__ import annotations

from typing import Any


_SENSITIVE_KEYS: frozenset[str] = frozenset(
    {
        "password",
        "password_hash",
        "token",
        "refresh_token",
        "access_token",
        "authorization",
        "jwt",
        "secret",
        "cookie",
        "x-registration-token",
    }
)


_REDACTED = "[REDACTED]"


def _recurse_redact(value: Any) -> Any:
    if isinstance(value, dict):
        return {k: _redact_one(k, v) for k, v in value.items()}
    return value


def _redact_one(key: str, value: Any) -> Any:
    if isinstance(key, str) and key.lower() in _SENSITIVE_KEYS:
        return _REDACTED
    if isinstance(value, dict):
        return _recurse_redact(value)
    return value


def redact_sensitive_keys(logger, method_name, event_dict):
    """structlog processor — replace sensitive values by '[REDACTED]'.

    Signature: (logger, method_name, event_dict) -> event_dict
    """
    for key in list(event_dict.keys()):
        value = event_dict[key]
        if isinstance(key, str) and key.lower() in _SENSITIVE_KEYS:
            event_dict[key] = _REDACTED
        elif isinstance(value, dict):
            event_dict[key] = _recurse_redact(value)
    return event_dict
