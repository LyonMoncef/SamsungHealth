"""V2.3.3.1 — LRU-capped in-memory storage for slowapi.

slowapi `MemoryStorage` is a `Counter` with no eviction → DoS-able by an
attacker spamming N distinct keys (10M+) until OOM (pentester HIGH H2).
We subclass `MemoryStorage` and add a cap (default 100k entries) backed by
an `OrderedDict` for LRU eviction on each `incr`.
"""
from __future__ import annotations

import os
from collections import OrderedDict

from limits.storage.memory import MemoryStorage


_LRU_CAP = 100_000


def _resolve_cap_from_env() -> int:
    raw = os.environ.get("SAMSUNGHEALTH_RL_LRU_CAP")
    if not raw:
        return _LRU_CAP
    try:
        return max(1, int(raw))
    except ValueError:
        return _LRU_CAP


class LruMemoryStorage(MemoryStorage):
    """`MemoryStorage` with a LRU eviction cap (in-memory single-instance only).

    On each `incr`, mark the key as most-recently-used. When `len(keys) > cap`,
    evict the oldest. Reads (`get`) also bump LRU position.
    """

    STORAGE_SCHEME = ["lru-memory"]

    def __init__(
        self,
        uri: str | None = None,
        wrap_exceptions: bool = False,
        *,
        cap: int | None = None,
        **kwargs: str,
    ) -> None:
        super().__init__(uri, wrap_exceptions=wrap_exceptions, **kwargs)
        self._cap = cap if cap is not None else _resolve_cap_from_env()
        self._lru: OrderedDict[str, None] = OrderedDict()

    def _bump(self, key: str) -> None:
        if key in self._lru:
            self._lru.move_to_end(key)
        else:
            self._lru[key] = None

    def _evict_if_needed(self) -> None:
        while len(self._lru) > self._cap:
            oldest, _ = self._lru.popitem(last=False)
            self.storage.pop(oldest, None)
            self.expirations.pop(oldest, None)
            self.events.pop(oldest, None)
            self.locks.pop(oldest, None)

    def incr(self, key: str, expiry: float, amount: int = 1, **_: object) -> int:
        # **_ swallows older API kwargs (`elastic_expiry`) used by tests/older callers.
        result = super().incr(key, expiry, amount=amount)
        self._bump(key)
        self._evict_if_needed()
        return result

    def get(self, key: str) -> int:
        value = super().get(key)
        if key in self._lru:
            self._lru.move_to_end(key)
        return value

    def clear(self, key: str) -> None:
        super().clear(key)
        self._lru.pop(key, None)
