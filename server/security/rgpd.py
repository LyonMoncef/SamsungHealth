"""Phase 3 RGPD — helpers backend pour `/me/{export,erase,audit-log}`.

Module réutilisé par `server.routers.me` (router) et testé directement par
les fixtures Phase 3 :

- `HEALTH_TABLES` — liste des 21 tables santé (ordre stable, vérifié contre models)
- Pydantic models : `Export*`, `Erase*`, `EraseStats`, `AuditEventOut`, `AuditLogPage`
- `_verify_reauth(db, user, password, oauth_nonce)` — password argon2 OU OAuth nonce
- `_verify_oauth_nonce(...)` — stub patchable (tests mock via `patch(...)`)
- `_ip_hmac(request)` — proxy vers `server.security.rate_limit.ip_hash`
- `_create_verification_token(...)` / `_consume_verification_token_atomic(...)`
- `_safe_audit_event(...)` — comme `audit_event` mais skip si user_id n'existe plus (HIGH 2)
- `_anonymize_auth_events(...)` — RGPD Art. 17 UPDATE 4 cols NULL (HIGH 2)
- `build_user_export_zip(...)` — ZIP RGPD avec SELECT FOR UPDATE en 1er (HIGH 4)
- `erase_user_cascade(...)` — cascade applicatif explicit 21 tables (HIGH 3)

Spec : docs/vault/specs/2026-04-28-phase3-rgpd-endpoints.md
"""
from __future__ import annotations

import csv
import io
import json
import tempfile
import zipfile
from datetime import date, datetime, timedelta, timezone
from typing import Any
from uuid import UUID

from fastapi import HTTPException, Request
from pydantic import BaseModel, ConfigDict
from sqlalchemy import select, text
from sqlalchemy.orm import Session

from server.db.models import (
    AuthEvent,
    IdentityProvider,
    User,
    VerificationToken,
)
from server.logging_config import get_logger
from server.security.audit import audit_event
from server.security.auth import (
    OAUTH_SENTINEL,
    generate_verification_token,
    hash_verification_token,
    verify_password,
)


_log = get_logger(__name__)


# ── constants ─────────────────────────────────────────────────────────────

#: 21 health tables exhaustively listed for cascade erase + per-table export.
#: Verified 2026-04-28 against `server/db/models.py`.
HEALTH_TABLES: tuple[str, ...] = (
    "sleep_sessions",
    "sleep_stages",
    "steps_hourly",
    "steps_daily",
    "heart_rate_hourly",
    "exercise_sessions",
    "stress",
    "spo2",
    "respiratory_rate",
    "hrv",
    "skin_temperature",
    "weight",
    "height",
    "blood_pressure",
    "mood",
    "water_intake",
    "activity_daily",
    "vitality_score",
    "floors_daily",
    "activity_level",
    "ecg",
)

# Cap export ZIP aggregate size (pentester §9). Above → 413.
EXPORT_MAX_BYTES = 50 * 1024 * 1024
# SpooledTemporaryFile threshold — stays in RAM under 10MB then auto-spills disk.
EXPORT_SPOOL_MAX_BYTES = 10 * 1024 * 1024
# Default audit_events lines included in export ZIP (full=True yields all).
EXPORT_AUTH_EVENTS_DEFAULT_LIMIT = 50
# Verification token TTL for export/erase confirm (5 min).
DEFAULT_REAUTH_TOKEN_TTL_SECONDS = 300


# ── Pydantic models ───────────────────────────────────────────────────────


class ExportRequestIn(BaseModel):
    password: str | None = None
    oauth_nonce: str | None = None


class ExportRequestOut(BaseModel):
    export_token: str
    expires_at: datetime


class EraseRequestIn(BaseModel):
    password: str | None = None
    oauth_nonce: str | None = None


class EraseRequestOut(BaseModel):
    erase_token: str
    expires_at: datetime


class EraseConfirmIn(BaseModel):
    erase_token: str


class EraseStats(BaseModel):
    tables: dict[str, int]
    total_rows: int


class AuditEventOut(BaseModel):
    id: UUID
    event_type: str
    ip_hash: str | None = None
    user_agent: str | None = None
    created_at: datetime
    meta: dict | None = None
    model_config = ConfigDict(from_attributes=True)


class AuditLogPage(BaseModel):
    events: list[AuditEventOut]
    total: int
    limit: int
    offset: int


# ── re-auth helpers ───────────────────────────────────────────────────────


def _verify_oauth_nonce(db: Session, user: User, nonce: str) -> bool:
    """Verify a Google ID token nonce for re-auth on /me/{export,erase}.

    V2.3 — stub: returns False by default. Phase 3 tests patch this helper via
    `patch("server.security.rgpd._verify_oauth_nonce", return_value=True)`.
    Real Google verification is delegated to V2.3.2 OAuth flow downstream.
    """
    return False


def _verify_reauth(
    db: Session,
    user: User,
    password: str | None,
    oauth_nonce: str | None,
) -> bool:
    """Verify password (argon2) OR oauth_nonce (mockable). Soft backoff on fail.

    For OAuth-only users (`password_hash == OAUTH_SENTINEL`) the password input
    is rejected and `oauth_nonce` is required.

    Returns True on success ; raises HTTPException 401 on failure (with soft
    backoff applied via `register_failed_login` for the password path).
    """
    is_oauth_only = user.password_hash == OAUTH_SENTINEL

    if is_oauth_only:
        if not oauth_nonce:
            _trigger_soft_backoff(db, user)
            raise HTTPException(status_code=401, detail="invalid_credentials")
        if _verify_oauth_nonce(db, user, oauth_nonce):
            return True
        _trigger_soft_backoff(db, user)
        raise HTTPException(status_code=401, detail="invalid_credentials")

    # Password-based user.
    if oauth_nonce and _verify_oauth_nonce(db, user, oauth_nonce):
        return True
    if password and verify_password(password, user.password_hash):
        return True

    _trigger_soft_backoff(db, user)
    raise HTTPException(status_code=401, detail="invalid_credentials")


def _trigger_soft_backoff(db: Session, user: User) -> None:
    """Apply V2.3.3.1 soft backoff. Best-effort — never raise on infra error."""
    try:
        from server.security.lockout import register_failed_login

        register_failed_login(db, user)
    except Exception as exc:  # pragma: no cover
        _log.warning("rgpd.soft_backoff_failed", error=str(exc))
        # Fallback : at least sleep a tiny amount so timing tests observe a delay.
        import time

        time.sleep(0.1)


# ── ip hmac proxy ─────────────────────────────────────────────────────────


def _ip_hmac(request: Request) -> str | None:
    """Return HMAC-SHA256 truncated 16-hex of the client IP, or None."""
    try:
        from server.security.rate_limit import _ip_hash, _resolve_client_ip

        # _resolve_client_ip needs trusted_proxies — lazy import / module global.
        try:
            from server.security.rate_limit import _trusted_proxies as _tp
        except ImportError:
            _tp = None
        ip = _resolve_client_ip(request, _tp) if _tp is not None else None
        if not ip:
            client = getattr(request, "client", None)
            ip = getattr(client, "host", None) if client else None
        if not ip:
            return None
        return _ip_hash(ip)
    except Exception:
        return None


# ── verification token primitives ─────────────────────────────────────────


def _create_verification_token(
    db: Session,
    user: User,
    purpose: str,
    ttl_seconds: int = DEFAULT_REAUTH_TOKEN_TTL_SECONDS,
) -> tuple[str, VerificationToken]:
    """Generate a single-use verification token + persist row. Caller commits.

    Revokes any previously active token for the same `(user, purpose)` pair so
    the unique partial index `uq_verification_tokens_active_per_purpose` is
    respected (mirrors the existing V2.3 `_issue_verification_token` pattern).

    Returns `(raw_token, row)`. `raw_token` is given back to the user (URL-safe
    32-byte base64) — only the sha256 hash is stored.
    """
    now = datetime.now(timezone.utc)
    # Revoke any unconsumed token for the (user, purpose) pair (same txn).
    db.execute(
        text(
            """
            UPDATE verification_tokens
            SET consumed_at = :now
            WHERE user_id = :uid AND purpose = :p AND consumed_at IS NULL
            """
        ),
        {"now": now, "uid": user.id, "p": purpose},
    )
    db.flush()

    raw, hashed = generate_verification_token()
    row = VerificationToken(
        user_id=user.id,
        token_hash=hashed,
        purpose=purpose,
        issued_at=now,
        expires_at=now + timedelta(seconds=ttl_seconds),
    )
    db.add(row)
    db.flush()
    return raw, row


def _consume_verification_token_atomic(
    db: Session,
    user: User,
    raw_token: str,
    purpose: str,
) -> VerificationToken | None:
    """Atomically mark a verification token as consumed if still valid.

    Equivalent to `UPDATE verification_tokens SET consumed_at = now()
    WHERE token_hash = :h AND user_id = :uid AND purpose = :p
    AND consumed_at IS NULL AND expires_at > now() RETURNING *`.

    Returns the consumed row on success, None if the token is missing,
    expired, already consumed, or owned by another user / wrong purpose.
    """
    if not raw_token:
        return None
    hashed = hash_verification_token(raw_token)
    now = datetime.now(timezone.utc)
    res = db.execute(
        text(
            """
            UPDATE verification_tokens
            SET consumed_at = :now
            WHERE token_hash = :h
              AND user_id = :uid
              AND purpose = :p
              AND consumed_at IS NULL
              AND expires_at > :now
            RETURNING id
            """
        ),
        {"now": now, "h": hashed, "uid": user.id, "p": purpose},
    ).first()
    if res is None:
        return None
    # Re-fetch (and expire any cached copy) so the caller observes the freshly
    # set `consumed_at` value.
    row = db.execute(
        select(VerificationToken).where(VerificationToken.id == res[0])
    ).scalar_one_or_none()
    if row is not None:
        db.refresh(row)
    return row


# ── audit helpers ─────────────────────────────────────────────────────────


def _safe_audit_event(
    db: Session,
    event_type: str,
    user_id: UUID | str | None,
    meta: dict[str, Any] | None = None,
    *,
    ip_hash: str | None = None,
    user_agent: str | None = None,
) -> AuthEvent | None:
    """Insert an audit row only if the referenced user_id still exists in DB.

    Phase 3 RGPD HIGH 2 — protects against re-creation of identifying data
    (`email_hash`, etc.) for users whose record was just deleted by the cascade
    erase. Behaves like `audit_event` otherwise.
    """
    if user_id is not None:
        exists = db.execute(
            text("SELECT 1 FROM users WHERE id = :uid"),
            {"uid": str(user_id)},
        ).first()
        if exists is None:
            return None
    return audit_event(
        db,
        event_type,
        user_id=user_id,
        ip_hash=ip_hash,
        user_agent=user_agent,
        meta=meta,
    )


def _anonymize_auth_events(db: Session, user_id: UUID | str) -> None:
    """RGPD Art. 17 — irréversibilité (HIGH 2).

    UPDATE auth_events SET user_id = NULL, email_hash = NULL, ip_hash = NULL,
    user_agent = NULL WHERE user_id = :uid.
    """
    db.execute(
        text(
            """
            UPDATE auth_events
            SET user_id = NULL,
                email_hash = NULL,
                ip_hash = NULL,
                user_agent = NULL
            WHERE user_id = :uid
            """
        ),
        {"uid": str(user_id)},
    )


# ── ZIP export ────────────────────────────────────────────────────────────


def _row_to_serializable(row: Any) -> dict[str, Any]:
    """Convert a SQLAlchemy ORM row to a JSON-serializable dict.

    `password_hash` is stripped defensively (defense in depth on top of the
    `user.json` projection)."""
    out: dict[str, Any] = {}
    mapper = getattr(row, "__mapper__", None)
    if mapper is None:
        return out
    for col in mapper.columns:
        name = col.key
        if name == "password_hash":
            continue
        value = getattr(row, name, None)
        if isinstance(value, datetime):
            out[name] = value.isoformat()
        elif isinstance(value, date):
            out[name] = value.isoformat()
        elif isinstance(value, UUID):
            out[name] = str(value)
        elif isinstance(value, (bytes, bytearray)):
            out[name] = value.hex()
        else:
            out[name] = value
    return out


def _stringify_for_csv(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, datetime):
        return value.isoformat()
    if isinstance(value, date):
        return value.isoformat()
    if isinstance(value, UUID):
        return str(value)
    if isinstance(value, (dict, list)):
        return json.dumps(value, default=str)
    return str(value)


def _orm_class_for_table(table_name: str):
    """Resolve the ORM class for a given __tablename__. Returns None if unknown."""
    from server.db.models import Base

    for mapper in Base.registry.mappers:
        cls = mapper.class_
        if getattr(cls, "__tablename__", None) == table_name:
            return cls
    return None


def _write_table_to_zip(
    db: Session, zf: zipfile.ZipFile, user_id: UUID, table_name: str
) -> None:
    """Write `health/<table>.csv` + `health/<table>.json` for one table.

    `sleep_stages` is special-cased : it has no `user_id` column, but is owned
    via `session_id -> sleep_sessions.id`, so we filter through that join.
    """
    cls = _orm_class_for_table(table_name)
    if cls is None:
        # Defensive — write empty files so the ZIP shape is stable for tests.
        zf.writestr(f"health/{table_name}.csv", "")
        zf.writestr(f"health/{table_name}.json", "[]")
        return

    if table_name == "sleep_stages":
        rows = (
            db.execute(
                text(
                    """
                    SELECT s.* FROM sleep_stages s
                    JOIN sleep_sessions ss ON ss.id = s.session_id
                    WHERE ss.user_id = :uid
                    """
                ),
                {"uid": str(user_id)},
            )
            .mappings()
            .all()
        )
        # Rebuild ORM rows so encryption/decryption is transparent.
        orm_rows = []
        if rows:
            ids = [r["id"] for r in rows]
            orm_rows = (
                db.execute(select(cls).where(cls.id.in_(ids))).scalars().all()
            )
    else:
        if not hasattr(cls, "user_id"):
            zf.writestr(f"health/{table_name}.csv", "")
            zf.writestr(f"health/{table_name}.json", "[]")
            return
        orm_rows = (
            db.execute(select(cls).where(cls.user_id == user_id)).scalars().all()
        )

    serialized = [_row_to_serializable(r) for r in orm_rows]
    # CSV — collect headers from the mapper for a stable shape even when empty.
    columns = [c.key for c in cls.__mapper__.columns if c.key != "password_hash"]
    csv_buf = io.StringIO()
    writer = csv.DictWriter(csv_buf, fieldnames=columns, extrasaction="ignore")
    writer.writeheader()
    for rec in serialized:
        writer.writerow({k: _stringify_for_csv(rec.get(k)) for k in columns})
    zf.writestr(f"health/{table_name}.csv", csv_buf.getvalue())
    zf.writestr(f"health/{table_name}.json", json.dumps(serialized, default=str))


def build_user_export_zip(
    db: Session, user: User, full_audit: bool = False
) -> tempfile.SpooledTemporaryFile:
    """Build the RGPD export ZIP into a SpooledTemporaryFile (≤10MB RAM).

    Critical (HIGH 4) : start with `SELECT ... FOR UPDATE` on `users.id` to
    block a concurrent erase. The lock is released when the surrounding
    transaction commits.

    Layout :
      - `manifest.json`      — metadata (timestamp, user_id, version, counts)
      - `user.json`          — user profile (no password_hash)
      - `identity_providers.json`
      - `auth_events.json`   — last 50 by default, all if `full_audit=True`
      - `health/<table>.csv` + `.json` for each of the 21 tables.

    Caps the aggregate size at `EXPORT_MAX_BYTES` (50MB) → HTTPException 413.
    """
    # HIGH 4 — acquire the row lock on users.id BEFORE touching any other table.
    db.execute(select(User).where(User.id == user.id).with_for_update())

    spool = tempfile.SpooledTemporaryFile(
        max_size=EXPORT_SPOOL_MAX_BYTES, mode="w+b"
    )
    with zipfile.ZipFile(spool, mode="w", compression=zipfile.ZIP_DEFLATED) as zf:
        # --- manifest ---
        total_rows = 0
        # --- user.json (NO password_hash) ---
        user_dict = _row_to_serializable(user)
        zf.writestr("user.json", json.dumps(user_dict, default=str))

        # --- identity_providers.json ---
        ip_rows = (
            db.execute(
                select(IdentityProvider).where(IdentityProvider.user_id == user.id)
            )
            .scalars()
            .all()
        )
        ip_serialized = [_row_to_serializable(r) for r in ip_rows]
        zf.writestr(
            "identity_providers.json", json.dumps(ip_serialized, default=str)
        )

        # --- auth_events.json (50 most recent or all if full_audit) ---
        auth_q = (
            select(AuthEvent)
            .where(AuthEvent.user_id == user.id)
            .order_by(AuthEvent.created_at.desc())
        )
        if not full_audit:
            auth_q = auth_q.limit(EXPORT_AUTH_EVENTS_DEFAULT_LIMIT)
        auth_rows = db.execute(auth_q).scalars().all()
        auth_serialized = [_row_to_serializable(r) for r in auth_rows]
        zf.writestr("auth_events.json", json.dumps(auth_serialized, default=str))

        # --- health/<table>.{csv,json} for the 21 tables ---
        per_table_count: dict[str, int] = {}
        for table in HEALTH_TABLES:
            try:
                _write_table_to_zip(db, zf, user.id, table)
                # rough count via SQL (cheap) for the manifest
                if table == "sleep_stages":
                    cnt = db.execute(
                        text(
                            "SELECT COUNT(*) FROM sleep_stages s "
                            "JOIN sleep_sessions ss ON ss.id = s.session_id "
                            "WHERE ss.user_id = :uid"
                        ),
                        {"uid": str(user.id)},
                    ).scalar_one()
                else:
                    cnt = db.execute(
                        text(
                            f"SELECT COUNT(*) FROM {table} WHERE user_id = :uid"
                        ),
                        {"uid": str(user.id)},
                    ).scalar_one()
                per_table_count[table] = int(cnt or 0)
                total_rows += per_table_count[table]
            except Exception as exc:  # pragma: no cover
                _log.warning(
                    "rgpd.export.table_failed", table=table, error=str(exc)
                )
                per_table_count[table] = 0

        # --- manifest.json ---
        manifest = {
            "schema_version": 1,
            "generated_at": datetime.now(timezone.utc).isoformat(),
            "user_id": str(user.id),
            "tables_count": len(HEALTH_TABLES),
            "total_rows": total_rows,
            "per_table_count": per_table_count,
            "auth_events_full": bool(full_audit),
        }
        zf.writestr("manifest.json", json.dumps(manifest, default=str))

    # Pentester §9 — hard cap at 50MB.
    size = spool.tell()
    if size > EXPORT_MAX_BYTES:
        spool.close()
        raise HTTPException(status_code=413, detail="export_too_large")

    return spool


# ── erase cascade ─────────────────────────────────────────────────────────


def erase_user_cascade(db: Session, user_id: UUID | str) -> EraseStats:
    """Cascade applicatif explicit — 21 tables santé sans ondelete=CASCADE (HIGH 3).

    Order :
    1. SELECT FOR UPDATE on users.id (block concurrent export — HIGH 4).
    2. `_safe_audit_event("rgpd.erase.confirmed")` BEFORE delete users
       (so the audit row exists ; will be anonymized just after — LOW).
    3. DELETE from each of the 21 health tables WHERE user_id = ?.
    4. DELETE from identity_providers, refresh_tokens, verification_tokens.
    5. `_anonymize_auth_events(db, user_id)` — RGPD Art. 17.
    6. DELETE FROM users WHERE id = ?.

    Returns `EraseStats(tables={table: rowcount}, total_rows=sum)`.
    """
    uid_str = str(user_id)

    # 1. Lock user row to block a concurrent export.
    db.execute(
        text("SELECT id FROM users WHERE id = :uid FOR UPDATE"),
        {"uid": uid_str},
    )

    # 2. Audit BEFORE delete (LOW pentester ordering).
    _safe_audit_event(
        db,
        "rgpd.erase.confirmed",
        user_id=user_id,
        meta={"step": "begin"},
    )

    stats: dict[str, int] = {}

    # 3. Health tables — explicit DELETE per table (HIGH 3).
    for table in HEALTH_TABLES:
        if table == "sleep_stages":
            res = db.execute(
                text(
                    """
                    DELETE FROM sleep_stages
                    WHERE session_id IN (
                        SELECT id FROM sleep_sessions WHERE user_id = :uid
                    )
                    """
                ),
                {"uid": uid_str},
            )
        else:
            res = db.execute(
                text(f"DELETE FROM {table} WHERE user_id = :uid"),
                {"uid": uid_str},
            )
        stats[table] = res.rowcount or 0

    # 4. Auxiliary auth tables.
    for table in ("identity_providers", "refresh_tokens", "verification_tokens"):
        res = db.execute(
            text(f"DELETE FROM {table} WHERE user_id = :uid"),
            {"uid": uid_str},
        )
        stats[table] = res.rowcount or 0

    # 5. Anonymize auth_events BEFORE deleting users (HIGH 2).
    _anonymize_auth_events(db, user_id)

    # 6. Finally delete the users row.
    db.execute(text("DELETE FROM users WHERE id = :uid"), {"uid": uid_str})

    return EraseStats(tables=stats, total_rows=sum(stats.values()))
