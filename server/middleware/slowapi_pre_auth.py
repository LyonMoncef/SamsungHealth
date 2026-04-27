"""V2.3.3.1 — Per-route rate-limit check in middleware (BEFORE FastAPI deps).

slowapi's stock `SlowAPIMiddleware` only enforces application-level (default)
limits at middleware time. Per-route `@limiter.limit(...)` checks happen inside
the route wrapper, AFTER FastAPI resolved dependencies (auth/Depends). That
means an unauthenticated flood gets `401` on every call and never trips the
rate-limit (spec H1 / test #43).

This middleware calls `_check_request_limit(handler, in_middleware=False)` so
per-route limits are enforced before deps run. The `_rate_limiting_complete`
flag prevents the inner wrapper from double-counting.
"""
from __future__ import annotations

from slowapi import Limiter
from slowapi.errors import RateLimitExceeded
from slowapi.middleware import _find_route_handler
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response
from starlette.types import ASGIApp


def _should_exempt(limiter: Limiter, handler) -> bool:
    if handler is None:
        return True
    name = f"{handler.__module__}.{handler.__name__}"
    return name in getattr(limiter, "_exempt_routes", set())


class SlowAPIPreAuthMiddleware(BaseHTTPMiddleware):
    """Run per-route slowapi checks BEFORE FastAPI dep resolution."""

    def __init__(self, app: ASGIApp) -> None:
        super().__init__(app)

    async def dispatch(self, request: Request, call_next):
        app = request.app
        limiter: Limiter = getattr(app.state, "limiter", None)
        if limiter is None or not limiter.enabled:
            return await call_next(request)

        # OPTIONS preflight: never count.
        if request.method == "OPTIONS":
            return await call_next(request)

        handler = _find_route_handler(app.routes, request.scope)
        if _should_exempt(limiter, handler):
            return await call_next(request)

        try:
            limiter._check_request_limit(request, handler, False)
        except RateLimitExceeded as exc:
            handler_fn = app.exception_handlers.get(RateLimitExceeded)
            if handler_fn is None:
                raise
            response = handler_fn(request, exc)
            # Some handlers may be async coroutines.
            import inspect as _inspect
            if _inspect.iscoroutine(response):
                response = await response
            return response

        # Mark complete so the route's inner wrapper does not re-check / double-count.
        request.state._rate_limiting_complete = True
        response = await call_next(request)

        # Inject X-RateLimit-* headers (best-effort; mirrors slowapi behavior).
        view_rl = getattr(request.state, "view_rate_limit", None)
        if view_rl is not None:
            try:
                response = limiter._inject_headers(response, view_rl)
            except Exception:
                pass
        return response
