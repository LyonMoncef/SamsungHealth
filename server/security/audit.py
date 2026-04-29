"""V2.3 — `audit_event` helper for `auth_events` table.

Single insertion point for application-level audit rows. Caps `meta` payload at
4KB serialized (pentester MED M4) so a misbehaving caller cannot bloat the
table with multi-MB blobs.

Phase 3 RGPD : `_safe_audit_event` (cf. `server.security.rgpd`) wraps this and
adds a "skip if user_id no longer exists" check so post-erase audit calls do
NOT recreate identifiers (HIGH 2 — RGPD Art. 17 irréversibilité).
"""
from __future__ import annotations

import json
from typing import Any
from uuid import UUID

from sqlalchemy.orm import Session

from server.db.models import AuthEvent
from server.logging_config import get_logger


_log = get_logger(__name__)


# Pentester MED M4 — bound the size of a single auth_events.meta payload.
META_MAX_BYTES = 4096


def _truncate_meta(meta: dict[str, Any]) -> dict[str, Any]:
    """Return a copy of `meta` whose JSON serialization is ≤ META_MAX_BYTES.

    Strategy: drop the largest values one by one until the payload fits, then
    flag `truncated=True`. Always returns a `dict` (never None).
    """
    if not isinstance(meta, dict):
        meta = {"value": meta}
    serialized = json.dumps(meta, default=str).encode("utf-8")
    if len(serialized) <= META_MAX_BYTES:
        return meta

    # Replace large string values with a length marker so structure stays
    # debuggable. Iterate over a key-size ranking (largest first).
    out: dict[str, Any] = dict(meta)
    out["truncated"] = True

    # Re-serialize in a loop, dropping/truncating the heaviest entry each pass.
    while len(json.dumps(out, default=str).encode("utf-8")) > META_MAX_BYTES:
        candidates = [
            (k, len(json.dumps(v, default=str).encode("utf-8")))
            for k, v in out.items()
            if k != "truncated"
        ]
        if not candidates:
            break
        heaviest_key, _ = max(candidates, key=lambda kv: kv[1])
        v = out[heaviest_key]
        if isinstance(v, str) and len(v) > 64:
            out[heaviest_key] = v[:64] + "...(truncated)"
        else:
            # Fall back to deletion for non-string heavy values.
            del out[heaviest_key]

    # Hard guarantee — if even after pruning we're still over the cap, replace
    # the payload with a minimal marker.
    if len(json.dumps(out, default=str).encode("utf-8")) > META_MAX_BYTES:
        out = {"truncated": True}
    return out


def audit_event(
    db: Session,
    event_type: str,
    user_id: UUID | str | None = None,
    *,
    email_hash: str | None = None,
    ip_hash: str | None = None,
    user_agent: str | None = None,
    request_id: str | None = None,
    meta: dict[str, Any] | None = None,
) -> AuthEvent | None:
    """Insert an `auth_events` row. Returns the new row (already added+flushed).

    `meta` is capped to 4KB serialized before insertion. Caller commits.
    Errors are caught + logged so an audit failure never crashes the request.
    """
    capped_meta = _truncate_meta(meta) if meta else (meta or {})
    try:
        row = AuthEvent(
            event_type=event_type,
            user_id=user_id,
            email_hash=email_hash,
            ip_hash=ip_hash,
            user_agent=user_agent,
            request_id=request_id,
            meta=capped_meta,
        )
        db.add(row)
        db.flush()
        return row
    except Exception as exc:  # pragma: no cover — defensive
        _log.warning(
            "audit.event.insert_failed", event_type=event_type, error=str(exc)
        )
        return None
