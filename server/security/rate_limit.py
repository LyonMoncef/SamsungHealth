"""V2.3.3.1 — slowapi rate-limit setup + IP resolution + audit helper.

- `_resolve_client_ip` implements the right-most-untrusted XFF pattern (nginx
  `real_ip_recursive on` / Express `trust proxy`) — anti spoofing classique.
- `limiter` is a slowapi `Limiter` backed by `LruMemoryStorage` (cap LRU 100k
  entries) so that a flood of distinct keys cannot OOM the process.
- `_rate_limit_exceeded_handler` returns the uniform `{"detail":"rate_limit_exceeded"}`
  body + `Retry-After` with random jitter (+0-5s, anti-sync attacker) and only
  emits `X-RateLimit-*` headers when not in production (anti-leak bucket state).
- `_validate_trusted_proxies_at_boot` parses `SAMSUNGHEALTH_TRUSTED_PROXIES` and
  fails fast in production if the env var is empty (else all clients hit the
  same bucket — DoS auth global).
- `audit_rate_limit_exceeded` samples 1/10 hits and stores the IP HMACed.
"""
from __future__ import annotations

import hashlib
import hmac
import ipaddress
import os
import random
from typing import Any

from fastapi import Request
from fastapi.responses import JSONResponse
from limits.storage import SCHEMES
from slowapi import Limiter
from slowapi.errors import RateLimitExceeded

from server.logging_config import get_logger
from server.security.rate_limit_storage import LruMemoryStorage


_log = get_logger(__name__)


_TRUSTED_PROXIES_ENV = "SAMSUNGHEALTH_TRUSTED_PROXIES"
_ENV_ENV = "SAMSUNGHEALTH_ENV"


# Register custom storage scheme so `Limiter(storage_uri="lru-memory://")` works.
SCHEMES.setdefault("lru-memory", LruMemoryStorage)
# Force-replace in case import order changed.
SCHEMES["lru-memory"] = LruMemoryStorage


class RateLimitConfigError(RuntimeError):
    """Raised when SAMSUNGHEALTH_TRUSTED_PROXIES is malformed or absent in prod."""


# Module-level state: parsed trusted proxies (list of ip_network).
_trusted_proxies: list[ipaddress.IPv4Network | ipaddress.IPv6Network] = []


def _parse_trusted_proxies(raw: str | None) -> list:
    """Parse the env value. Each entry must be a valid CIDR or IP. Empty → []."""
    if not raw:
        return []
    nets: list = []
    for entry in raw.split(","):
        entry = entry.strip()
        if not entry:
            continue
        nets.append(ipaddress.ip_network(entry, strict=False))
    return nets


def _validate_trusted_proxies_at_boot() -> list:
    """Validate env at boot. Side-effect: populate `_trusted_proxies` global.

    - prod + empty → RateLimitConfigError (pentester HIGH #4 fix).
    - malformed entry → RateLimitConfigError.
    """
    global _trusted_proxies
    raw = os.environ.get(_TRUSTED_PROXIES_ENV)
    env = os.environ.get(_ENV_ENV, "development")

    try:
        nets = _parse_trusted_proxies(raw)
    except (ValueError, TypeError) as exc:
        raise RateLimitConfigError(
            f"{_TRUSTED_PROXIES_ENV} contains a malformed CIDR/IP entry: {exc}"
        ) from exc

    if env == "production" and not nets:
        raise RateLimitConfigError(
            f"In production, {_TRUSTED_PROXIES_ENV} must be set or rate-limiter "
            "treats all users as the same client (DoS via shared bucket)."
        )

    _trusted_proxies = nets
    if not nets:
        _log.info("rate_limit.no_trusted_proxies", env=env)
    return nets


def _resolve_client_ip(
    request: Any, trusted_proxies: list | None = None
) -> str:
    """Right-most-untrusted: walk XFF from right → return first untrusted IP.

    - No trusted proxies configured → use `request.client.host` (dev/single-host).
    - Direct peer NOT trusted → request didn't transit a proxy → ignore XFF.
    - Direct peer IS trusted → walk XFF from right, return first untrusted.
    - All XFF entries trusted → fallback leftmost.
    """
    if trusted_proxies is None:
        trusted_proxies = _trusted_proxies

    client = getattr(request, "client", None)
    direct_peer = getattr(client, "host", None) if client is not None else None

    if not trusted_proxies:
        return direct_peer or "unknown"

    if direct_peer is None:
        return "unknown"

    try:
        peer_ip = ipaddress.ip_address(direct_peer)
        if not any(peer_ip in net for net in trusted_proxies):
            return direct_peer
    except (ValueError, TypeError):
        return direct_peer

    xff = None
    headers = getattr(request, "headers", None)
    if headers is not None:
        xff = headers.get("X-Forwarded-For") or headers.get("x-forwarded-for")
    if not xff:
        return direct_peer

    ips = [ip.strip() for ip in xff.split(",") if ip.strip()]
    for ip_str in reversed(ips):
        try:
            ip = ipaddress.ip_address(ip_str)
        except ValueError:
            continue
        if not any(ip in net for net in trusted_proxies):
            return ip_str
    # All XFF entries trusted → leftmost fallback.
    return ips[0] if ips else direct_peer


# ── key functions ─────────────────────────────────────────────────────────
def _pure_ip_key(request: Any) -> str:
    return _resolve_client_ip(request, _trusted_proxies)


def _login_composite_key(request: Any) -> str:
    ip = _resolve_client_ip(request, _trusted_proxies)
    email = getattr(getattr(request, "state", None), "rate_limit_email", None) or "_unknown"
    return f"login:{ip}:{email.lower()}"


def _refresh_composite_key(request: Any) -> str:
    ip = _resolve_client_ip(request, _trusted_proxies)
    user_id = getattr(getattr(request, "state", None), "rate_limit_user_id", None) or "_unknown"
    return f"refresh:{ip}:{user_id}"


def _email_composite_key(request: Any) -> str:
    ip = _resolve_client_ip(request, _trusted_proxies)
    email = getattr(getattr(request, "state", None), "rate_limit_email", None) or "_unknown"
    return f"email:{ip}:{email.lower()}"


def _user_id_key(request: Any) -> str:
    """Per-user cap for /api/* INSERT routes (data poisoning self-DoS)."""
    auth = None
    headers = getattr(request, "headers", None)
    if headers is not None:
        auth = headers.get("authorization") or headers.get("Authorization")
    if auth and auth.startswith("Bearer "):
        try:
            from server.security.auth import decode_access_token

            payload = decode_access_token(auth[7:])
            sub = payload.get("sub")
            if sub:
                return f"api:user:{sub}"
        except Exception:
            pass
    # Fallback to IP for unauthenticated requests (so rate-limit still bites).
    return f"api:ip:{_resolve_client_ip(request, _trusted_proxies)}"


# ── env-driven cap overrides (test monkeypatch) ───────────────────────────
def _api_post_cap() -> str:
    return os.environ.get("SAMSUNGHEALTH_RL_API_POST_CAP", "1000/hour")


def _email_request_composite_cap() -> str:
    """Composite (IP, email) cap for email-issuing endpoints (verify/reset request).

    Default 3/5min per spec §Limites par endpoint. Overridable via env so tests
    that need to call the route many times for the same email (e.g. timing/jitter
    measurement) can raise it without bypassing the limiter entirely.
    """
    return os.environ.get("SAMSUNGHEALTH_RL_EMAIL_COMPOSITE_CAP", "3/5minutes")


# ── limiter instance ──────────────────────────────────────────────────────
limiter = Limiter(
    key_func=_pure_ip_key,
    storage_uri="lru-memory://",
    headers_enabled=True,
)


# ── 429 response ──────────────────────────────────────────────────────────
def _rate_limit_exceeded_handler(request: Request, exc: RateLimitExceeded) -> JSONResponse:
    """Uniform 429 body + Retry-After with jitter + headers conditional on env."""
    # exc.limit is a slowapi Limit wrapping a limits RateLimitItem.
    try:
        retry_after = int(exc.limit.limit.get_expiry()) + random.randint(0, 5)
    except Exception:
        retry_after = 1 + random.randint(0, 5)
    headers = {"Retry-After": str(retry_after)}

    if os.environ.get(_ENV_ENV, "development") != "production":
        try:
            limit_item = exc.limit.limit
            headers["X-RateLimit-Limit"] = str(limit_item.amount)
            headers["X-RateLimit-Remaining"] = "0"
            headers["X-RateLimit-Reset"] = str(int(limit_item.get_expiry()))
        except Exception:
            pass

    # Best-effort sample audit (1/10) — never raise on audit failure.
    if random.random() < 0.1:
        try:
            from server.database import get_session

            db = get_session()
            try:
                audit_rate_limit_exceeded(
                    db, request, endpoint=str(request.url.path)
                )
                db.commit()
            finally:
                db.close()
        except Exception as audit_exc:  # pragma: no cover
            _log.warning("rate_limit.audit_failed", error=str(audit_exc))

    return JSONResponse(
        status_code=429,
        content={"detail": "rate_limit_exceeded"},
        headers=headers,
    )


# ── audit helper ──────────────────────────────────────────────────────────
def _ip_hash(ip: str) -> str:
    salt = os.environ.get("SAMSUNGHEALTH_EMAIL_HASH_SALT", "")
    return hmac.new(
        salt.encode("utf-8"),
        ip.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()[:16]


def audit_rate_limit_exceeded(db, request: Request, endpoint: str) -> None:
    """Insert a single auth_events row with HMACed IP. Caller commits."""
    from server.db.models import AuthEvent

    ip = _resolve_client_ip(request, _trusted_proxies)
    ip_hashed = _ip_hash(ip) if ip else None
    db.add(
        AuthEvent(
            event_type="rate_limit_exceeded",
            user_id=None,
            email_hash=None,
            request_id=f"endpoint:{endpoint}|ip_hash:{ip_hashed}",
        )
    )
