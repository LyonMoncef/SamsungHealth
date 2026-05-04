"""V2.3.1 — Email outbound (log-only transport, dual-sink architecture).

Pas de SMTP/SES. Le mail "envoyé" se résume à :
1. Cache mémoire 60s `(token_hash → token_raw)` pour reconstruction admin.
2. Log structlog `event="email.outbound"` SANS le verify_link (audit-only).

Le verify_link complet est reconstruit uniquement par l'endpoint admin
`/admin/pending-verifications` à partir du `token_raw` en cache + PUBLIC_BASE_URL.
"""
from __future__ import annotations

import hashlib
import hmac
import os
import threading
import time
from datetime import datetime
from typing import Any

from server.logging_config import get_logger


def _log():
    return get_logger(__name__)

_EMAIL_HASH_SALT_ENV = "SAMSUNGHEALTH_EMAIL_HASH_SALT"
_CACHE_TTL_SECONDS = 60


class _OutboundLinkCache:
    """In-memory `(token_hash → token_raw)` cache with 60s TTL, lazy eviction.

    Thread-safe (admin endpoint may read while a request handler writes).
    """

    def __init__(self, ttl_seconds: int = _CACHE_TTL_SECONDS) -> None:
        self._ttl = ttl_seconds
        self._store: dict[str, tuple[str, float]] = {}
        self._lock = threading.Lock()

    def _evict_expired(self, now: float) -> None:
        # Caller must hold the lock.
        expired = [k for k, (_, ts) in self._store.items() if now - ts > self._ttl]
        for k in expired:
            del self._store[k]

    def put(self, token_hash: str, token_raw: str) -> None:
        with self._lock:
            now = time.monotonic()
            self._evict_expired(now)
            self._store[token_hash] = (token_raw, now)

    def get(self, token_hash: str) -> str | None:
        with self._lock:
            now = time.monotonic()
            self._evict_expired(now)
            entry = self._store.get(token_hash)
            if entry is None:
                return None
            return entry[0]

    def items(self) -> list[tuple[str, str]]:
        """Snapshot of currently-live (token_hash, token_raw) pairs."""
        with self._lock:
            now = time.monotonic()
            self._evict_expired(now)
            return [(k, v[0]) for k, v in self._store.items()]

    def clear(self) -> None:
        with self._lock:
            self._store.clear()


_outbound_link_cache = _OutboundLinkCache()


def _email_salt_bytes() -> bytes:
    """Read `SAMSUNGHEALTH_EMAIL_HASH_SALT` and return raw bytes (utf-8 fallback)."""
    raw = os.environ.get(_EMAIL_HASH_SALT_ENV)
    if not raw:
        # Boot validator must have caught this. If we reach here in test/dev,
        # signal loudly.
        raise RuntimeError(f"{_EMAIL_HASH_SALT_ENV} not set")
    return raw.encode("utf-8")


def hmac_email_hash(email: str) -> str:
    """HMAC-SHA256(email.lower(), server_salt) → 64 chars hex.

    Stable cross-events (corrélation possible) mais infaisable à inverser
    en dictionnaire offline sans le sel serveur (≥ 32 bytes).
    """
    canonical = email.lower().encode("utf-8")
    return hmac.new(_email_salt_bytes(), canonical, hashlib.sha256).hexdigest()


def _emit_outbound_log(
    *,
    purpose: str,
    to_email: str,
    token_hash: str,
    expires_at: datetime,
    extra: dict[str, Any] | None = None,
) -> None:
    payload: dict[str, Any] = {
        "purpose": purpose,
        "to_hash": hmac_email_hash(to_email),
        # Short hash prefix = corrélation debug, pas le secret.
        "verify_link_id": token_hash[:12],
        "expires_at": expires_at.isoformat(),
    }
    if extra:
        payload.update(extra)
    _log().info("email.outbound", **payload)


def send_verification_email(
    *,
    to_email: str,
    token_raw: str,
    token_hash: str,
    expires_at: datetime,
    purpose: str = "email_verification",
) -> None:
    """Cache the (hash → raw) link 60s + emit structlog event WITHOUT verify_link."""
    _outbound_link_cache.put(token_hash, token_raw)
    _emit_outbound_log(
        purpose=purpose,
        to_email=to_email,
        token_hash=token_hash,
        expires_at=expires_at,
    )


def send_password_reset_email(
    *,
    to_email: str,
    token_raw: str,
    token_hash: str,
    expires_at: datetime,
    purpose: str = "password_reset",
) -> None:
    """Symmetric to send_verification_email — same dual-sink architecture."""
    _outbound_link_cache.put(token_hash, token_raw)
    _emit_outbound_log(
        purpose=purpose,
        to_email=to_email,
        token_hash=token_hash,
        expires_at=expires_at,
    )
