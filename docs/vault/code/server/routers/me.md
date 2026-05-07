---
type: code-source
language: python
file_path: server/routers/me.py
git_blob: 597eae24fba619b71f4fa8a2dc33567a06799f8e
last_synced: '2026-05-06T08:02:34Z'
loc: 214
annotations: []
imports:
- datetime
- fastapi
- fastapi.responses
- sqlalchemy
- sqlalchemy.orm
- server.database
- server.db.models
- server.security.auth
- server.security.csrf
- server.security.rate_limit
- server.security.rgpd
exports: []
tags:
- code
- python
---

# server/routers/me.py

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`server/routers/me.py`](../../../server/routers/me.py).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```python
"""Phase 3 RGPD — `/me/*` endpoints (Art. 15 access, Art. 17 erasure, Art. 20 portability).

5 endpoints :
- POST /me/export/request    : re-auth (password OR oauth_nonce) → export_token TTL 5min
- GET  /me/export/confirm    : consume token + stream ZIP archive
- POST /me/erase/request     : re-auth → erase_token TTL 5min
- POST /me/erase/confirm     : consume token + cascade delete (21 health tables + auth bits) → 204
- GET  /me/audit-log         : paginated audit events scoped to current_user (admin_* filtered by default)

Helpers + Pydantic models live in `server/security/rgpd.py` (other agent).
"""
from __future__ import annotations

from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query, Request, Response
from fastapi.responses import StreamingResponse
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from server.database import get_session
from server.db.models import AuthEvent, User
from server.security.auth import get_current_user
from server.security.csrf import check_sec_fetch_site
from server.security.rate_limit import _user_id_key, limiter
from server.security.rgpd import (
    AuditEventOut,
    AuditLogPage,
    EraseConfirmIn,
    EraseRequestIn,
    EraseRequestOut,
    ExportRequestIn,
    ExportRequestOut,
    _consume_verification_token_atomic,
    _create_verification_token,
    _ip_hmac,
    _safe_audit_event,
    _verify_reauth,
    build_user_export_zip,
    erase_user_cascade,
)


router = APIRouter(tags=["rgpd"])


# ── 1. POST /me/export/request ────────────────────────────────────────────
@router.post("/me/export/request", response_model=ExportRequestOut)
@limiter.limit("5/hour", key_func=_user_id_key)
async def export_request(
    request: Request,
    response: Response,
    body: ExportRequestIn,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> ExportRequestOut:
    """Re-auth (password or OAuth nonce) + create export_token (TTL 5 min, single-use)."""
    check_sec_fetch_site(request)
    _verify_reauth(db, current_user, body.password, body.oauth_nonce)
    raw_token, row = _create_verification_token(
        db, current_user, "account_export_confirm", ttl_seconds=300
    )
    _safe_audit_event(
        db,
        "rgpd.export.requested",
        user_id=current_user.id,
        meta={"expires_at": row.expires_at.isoformat()},
    )
    db.commit()
    return ExportRequestOut(export_token=raw_token, expires_at=row.expires_at)


# ── 2. GET /me/export/confirm ─────────────────────────────────────────────
@router.get("/me/export/confirm")
@limiter.limit("5/hour", key_func=_user_id_key)
async def export_confirm(
    request: Request,
    response: Response,
    export_token: str = Query(..., min_length=32),
    full: bool = Query(False),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> StreamingResponse:
    """Consume token, stream ZIP archive (filename générique, no user_id leak)."""
    consumed = _consume_verification_token_atomic(
        db, current_user, export_token, "account_export_confirm"
    )
    if consumed is None:
        raise HTTPException(status_code=400, detail="invalid_or_consumed_token")
    db.commit()

    spool = build_user_export_zip(db, current_user, full_audit=full)
    size_bytes = spool.tell()
    spool.seek(0)
    _safe_audit_event(
        db,
        "rgpd.export.downloaded",
        user_id=current_user.id,
        meta={
            "full": full,
            "size_bytes": size_bytes,
            "ip_hash": _ip_hmac(request),
        },
    )
    db.commit()

    filename = f"export_my_data_{date.today().isoformat()}.zip"
    return StreamingResponse(
        spool,
        media_type="application/zip",
        headers={
            "Content-Disposition": f'attachment; filename="{filename}"',
            "Cache-Control": "no-store",
        },
    )


# ── 3. POST /me/erase/request ─────────────────────────────────────────────
@router.post("/me/erase/request", response_model=EraseRequestOut)
@limiter.limit("5/hour", key_func=_user_id_key)
async def erase_request(
    request: Request,
    response: Response,
    body: EraseRequestIn,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> EraseRequestOut:
    """Re-auth + create erase_token (TTL 5 min, single-use, purpose=account_erase_confirm)."""
    check_sec_fetch_site(request)
    _verify_reauth(db, current_user, body.password, body.oauth_nonce)
    raw_token, row = _create_verification_token(
        db, current_user, "account_erase_confirm", ttl_seconds=300
    )
    _safe_audit_event(
        db,
        "rgpd.erase.requested",
        user_id=current_user.id,
        meta={"expires_at": row.expires_at.isoformat()},
    )
    db.commit()
    return EraseRequestOut(erase_token=raw_token, expires_at=row.expires_at)


# ── 4. POST /me/erase/confirm ─────────────────────────────────────────────
@router.post("/me/erase/confirm", status_code=204)
@limiter.limit("5/hour", key_func=_user_id_key)
async def erase_confirm(
    request: Request,
    response: Response,
    body: EraseConfirmIn,
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> Response:
    """Consume token + cascade delete (21 health tables + identity_providers + auth bits) → 204."""
    check_sec_fetch_site(request)
    consumed = _consume_verification_token_atomic(
        db, current_user, body.erase_token, "account_erase_confirm"
    )
    if consumed is None:
        raise HTTPException(status_code=400, detail="invalid_or_consumed_token")
    db.commit()

    erase_user_cascade(db, current_user.id)
    db.commit()
    return Response(status_code=204)


# ── 5. GET /me/audit-log ──────────────────────────────────────────────────
@router.get("/me/audit-log", response_model=AuditLogPage)
@limiter.limit("60/hour", key_func=_user_id_key)
async def audit_log_my_account(
    request: Request,
    response: Response,
    limit: int = Query(50, gt=0, le=200),
    offset: int = Query(0, ge=0),
    include_admin: bool = Query(False),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> AuditLogPage:
    """RGPD Art. 15 — droit d'accès. Liste des auth_events liés au current_user.

    Par défaut filtre les `admin_*` events (Décision 6 spec) ; `?include_admin=true`
    pour les inclure. Pagination + total count global. Rate-limit 60/h/user.
    """
    q = select(AuthEvent).where(AuthEvent.user_id == current_user.id)
    if not include_admin:
        q = q.where(AuthEvent.event_type.notlike("admin\\_%", escape="\\"))
    rows = (
        db.execute(q.order_by(AuthEvent.created_at.desc()).limit(limit).offset(offset))
        .scalars()
        .all()
    )

    total_q = select(func.count()).select_from(AuthEvent).where(
        AuthEvent.user_id == current_user.id
    )
    if not include_admin:
        total_q = total_q.where(AuthEvent.event_type.notlike("admin\\_%", escape="\\"))
    total = db.execute(total_q).scalar_one()

    _safe_audit_event(
        db,
        "rgpd.audit_log.read",
        user_id=current_user.id,
        meta={"limit": limit, "offset": offset},
    )
    db.commit()

    return AuditLogPage(
        events=[AuditEventOut.model_validate(r) for r in rows],
        total=total,
        limit=limit,
        offset=offset,
    )
```

---

## Appendix — symbols & navigation *(auto)*

### Implements specs
- [[../../specs/2026-04-28-phase3-rgpd-endpoints]] — symbols: `router`, `export_request`, `export_confirm`, `erase_request`, `erase_confirm`, `audit_log_my_account`

### Imports
- `datetime`
- `fastapi`
- `fastapi.responses`
- `sqlalchemy`
- `sqlalchemy.orm`
- `server.database`
- `server.db.models`
- `server.security.auth`
- `server.security.csrf`
- `server.security.rate_limit`
- `server.security.rgpd`
