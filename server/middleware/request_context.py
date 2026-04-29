"""Request context middleware (V2.0.5).

Pure ASGI middleware qui :
- Génère ou réutilise un `request_id` (X-Request-ID), sanitisé alnum+tirets, max 64 chars.
- Bind `request_id_var` et `user_id_var` (ContextVar) → injectés dans tous logs structlog
  émis pendant la request via `structlog.contextvars.merge_contextvars`.
- Renvoie le `X-Request-ID` dans response headers.
- Mesure la latence (`perf_counter`) et émet `request.complete` (INFO/WARNING/ERROR
  selon status code) avec `latency_ms` + `route` (template FastAPI).
"""
from __future__ import annotations

import re
import time
import uuid
from contextvars import ContextVar

import structlog

from server.logging_config import get_logger


request_id_var: ContextVar[str | None] = ContextVar("request_id", default=None)
user_id_var: ContextVar[str | None] = ContextVar("user_id", default=None)

_REQUEST_ID_MAX_LEN = 64
_REQUEST_ID_RE = re.compile(r"[^A-Za-z0-9-]")


def _sanitize_request_id(raw: str) -> str:
    """Garde alnum + tirets, tronque à 64. Si vide après nettoyage → uuid4().hex."""
    cleaned = _REQUEST_ID_RE.sub("", raw)[:_REQUEST_ID_MAX_LEN]
    return cleaned or uuid.uuid4().hex


def _resolve_route_template(scope) -> str:
    """Retourne le template FastAPI (`/api/sleep`) si match, sinon le path brut."""
    route = scope.get("route")
    if route is not None:
        path = getattr(route, "path", None)
        if path:
            return path
    return scope.get("path", "")


def _level_for_status(status: int) -> str:
    if status >= 500:
        return "error"
    if status >= 400:
        return "warning"
    return "info"


class RequestContextMiddleware:
    """ASGI pure middleware. Monte-le AVANT tout autre middleware applicatif."""

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        # Header lookup — ASGI scope.headers est list[tuple[bytes, bytes]]
        headers = scope.get("headers") or []
        incoming_rid: str | None = None
        for k, v in headers:
            if k.lower() == b"x-request-id":
                incoming_rid = v.decode("latin-1", errors="ignore")
                break

        if incoming_rid:
            request_id = _sanitize_request_id(incoming_rid)
        else:
            request_id = uuid.uuid4().hex

        # Bind contextvars (structlog merger les injecte dans chaque log emit)
        token_rid = request_id_var.set(request_id)
        token_uid = user_id_var.set(None)
        structlog.contextvars.bind_contextvars(
            request_id=request_id,
            user_id=None,
        )

        start = time.perf_counter()
        status_holder: dict = {"code": 500, "started": False}

        async def send_wrapper(message):
            if message["type"] == "http.response.start":
                status_holder["code"] = message["status"]
                status_holder["started"] = True
                # Inject X-Request-ID header
                raw_headers = list(message.get("headers") or [])
                # Drop any existing x-request-id (case-insensitive) pour éviter doublon
                raw_headers = [
                    (k, v) for k, v in raw_headers if k.lower() != b"x-request-id"
                ]
                raw_headers.append(
                    (b"x-request-id", request_id.encode("latin-1"))
                )
                message["headers"] = raw_headers
            await send(message)

        try:
            try:
                await self.app(scope, receive, send_wrapper)
            except Exception:
                # App raised avant d'envoyer une response → on émet un 500
                # avec X-Request-ID pour préserver la corrélation.
                if not status_holder["started"]:
                    status_holder["code"] = 500
                    await send(
                        {
                            "type": "http.response.start",
                            "status": 500,
                            "headers": [
                                (b"content-type", b"text/plain; charset=utf-8"),
                                (b"content-length", b"21"),
                                (b"x-request-id", request_id.encode("latin-1")),
                            ],
                        }
                    )
                    await send(
                        {
                            "type": "http.response.body",
                            "body": b"Internal Server Error",
                        }
                    )
                raise
        finally:
            latency_ms = (time.perf_counter() - start) * 1000.0
            route = _resolve_route_template(scope)
            level = _level_for_status(status_holder["code"])
            log = get_logger("server.middleware.request_context")
            log_method = getattr(log, level)
            log_method(
                "request.complete",
                route=route,
                method=scope.get("method"),
                status=status_holder["code"],
                latency_ms=round(latency_ms, 3),
            )
            structlog.contextvars.unbind_contextvars("request_id", "user_id")
            request_id_var.reset(token_rid)
            user_id_var.reset(token_uid)
