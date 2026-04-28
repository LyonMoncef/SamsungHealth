---
type: spec
title: "Phase 3 — RGPD endpoints (/me/export, /me/erase, /me/audit-log)"
slug: 2026-04-28-phase3-rgpd-endpoints
status: ready
created: 2026-04-28
delivered: null
priority: high
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs:
  - 2026-04-26-v2-auth-foundation
  - 2026-04-26-v2.3.3.1-rate-limit-lockout
implements:
  - file: server/routers/me.py
    symbols: [router, export_request, export_confirm, erase_request, erase_confirm, audit_log_my_account]
  - file: server/security/rgpd.py
    symbols: [build_user_export_zip, erase_user_cascade, _anonymize_auth_events, _safe_audit_event, EraseStats]
  - file: server/main.py
    symbols: [app]
tested_by:
  - file: tests/server/test_me_export.py
    classes: [TestExportRequestReauth, TestExportConfirmZip, TestExportContent, TestExportRateLimit, TestUserIsolation, TestExportRaceWithErase]
  - file: tests/server/test_me_erase.py
    classes: [TestErasePreconditions, TestEraseCascadeAllTables, TestEraseAuthEventsAnonymized, TestEraseAntiAccident, TestEraseOAuthOnly, TestEraseTokenSingleUse]
  - file: tests/server/test_me_audit_log.py
    classes: [TestAuditLogScope, TestAuditLogAdminFilter, TestAuditLogPagination, TestAuditLogIpHmac, TestAuditLogMetaCap]
tags: [phase3, rgpd, gdpr, data-export, right-to-erasure, samsunghealth, spec]
---

# Phase 3 — RGPD endpoints

## Vision

Les Articles 15 (droit d'accès), 17 (droit à l'effacement) et 20 (droit à la portabilité) RGPD imposent à tout traitement de données perso de fournir au sujet **3 capacités** :
1. **Export** structuré et lisible des données détenues
2. **Effacement** complet sur demande (sauf cas légitimes de rétention)
3. **Accès au log d'audit** (qui a accédé à mes données ? quand ? pour quoi ?)

Phase 3 livre ces 3 endpoints sous `/me/*` (auth JWT bearer required, scope = current_user). Implementation pragmatique self-host single-user — le user EST son propre data subject.

V2.3 → V2.3.3.3 ont livré 21 tables santé multi-user + audit_events. Phase 3 = export + erase + audit-log queries qui touchent toutes ces tables avec filter `user_id == current_user.id`.

## Pentester verdict — patch complet

**Statut** : pentester reviewé 2026-04-28 → verdict `WARN`, 4 HIGH bloquants + 3 décisions design + 5 risques additionnels (3 HIGH + 4 MED + 4 LOW). **Patch reconcilié 2026-04-28** : tous les points intégrés en architecture, livrables, tests, frontmatter.

**Mapping verdict → sections de la spec** :
| Pentester | Section spec |
|-----------|--------------|
| HIGH 1 — re-auth `/me/export` | §1 (2-step request/confirm + `_verify_reauth`) |
| HIGH 2 — anonymisation `auth_events` complète | §2 (`_anonymize_auth_events` UPDATE 4 cols NULL) + §8 (`_safe_audit_event`) |
| HIGH 3 — cascade applicatif explicit 21 tables | §2 (`erase_user_cascade` + `HEALTH_TABLES`) |
| HIGH 4 — race `export` ↔ `erase` | §1.bis (`SELECT FOR UPDATE` users.id dans `build_user_export_zip`) |
| Décision 5 — OAuth-only erase = Google nonce | §2 (pas d'email outbound) |
| Décision 6 — `/me/audit-log` filter `admin_*` | §3 (`include_admin=False` par défaut) |
| Décision 7 — filename générique | §1 (`export_my_data_{date}.zip`) |
| HIGH H1 — race serialization | §1.bis |
| HIGH H2 — `audit_event` post-erase no-op | §8 (`_safe_audit_event`) |
| MED — purpose enum DB CHECK | §11 (Alembic migration) |
| MED — meta cap 4KB | §3 (`AuthEvent.meta` truncate) |
| MED — compression bomb fallback | §9 (différé Phase 3+ documenté Out of scope) |
| LOW — atomic single-use UPDATE...RETURNING | §12 (`_consume_verification_token_atomic`) |
| LOW — audit AVANT delete users | §2 (`erase_user_cascade` ordering) |

**Action prochaine** : TDD RED via test-writer agent (3 fichiers tests ~34 cas).

## Décisions techniques

### Architecture révisée post-pentester (4 HIGH bloquants + 3 décisions design)

**HIGH bloquants** :
1. **Re-auth password sur `/me/export`** — JWT volé seul ne suffit plus pour exfiltrer. Aligne sur `/me/erase` 2-step (un endpoint `/me/export/request` qui retourne `export_token` TTL 5min, puis `/me/export/confirm?export_token=` qui stream le ZIP).
2. **Anonymisation `auth_events` complète** — `UPDATE auth_events SET user_id=NULL, email_hash=NULL, ip_hash=NULL, user_agent=NULL WHERE user_id=?`. RGPD Art. 17 : irréversibilité.
3. **Cascade applicatif explicite** — 21 tables santé sans `ondelete=CASCADE`, donc `DELETE FROM <each> WHERE user_id=?` AVANT `DELETE FROM users`. Test post-erase = `SELECT COUNT(*) WHERE user_id=?` == 0 sur les 21 tables.
4. **Race `erase` ↔ `export` serialized** — `with db.begin():` + `SELECT FOR UPDATE` sur `users.id` au début de `build_user_export_zip` ; bloque l'erase concurrent.

**Décisions design** :
5. **OAuth-only erase = Google re-auth nonce** (pas email outbound). Cohérent V2.3.2, cryptographiquement plus fort.
6. **`/me/audit-log` filtre `admin_*`** par défaut (`event_type.notlike('admin\_%')`). Events admin accessibles uniquement via export ZIP `?full=true`.
7. **Filename générique** : `export_my_data_{date}.zip` (sans `user_id` — pas de leak indirect).

**Bonus pentester intégré** :
- `tempfile.SpooledTemporaryFile(max_size=10*1024*1024)` au lieu de `io.BytesIO` (auto-spill disk, gain RAM gratuit)
- Atomic `UPDATE verification_tokens SET consumed_at=now() WHERE token_hash=? AND consumed_at IS NULL RETURNING *` (anti-race single-use)
- Audit `rgpd.export.downloaded` avec `meta.ip_hash` (forensique post-leak)
- Whitelist purpose enum DB-side (CHECK constraint) sur `verification_tokens.purpose` pour bloquer cross-purpose token reuse
- Validation `audit_event()` post-erase : skip si `user_id` n'existe plus en DB (anti re-création `email_hash` post-erase)

### 1. `/me/export` — Right to Portability (Art. 20) — **2-step re-auth (HIGH 1)**

**Why 2-step** : JWT volé seul ne doit PAS suffire à exfiltrer toute la base santé. Re-auth password (ou OAuth nonce) sur l'étape `request` aligne export sur erase et empêche un attaquant qui n'a que le token court d'aspirer les données.

**Format ZIP** :
- `manifest.json` — métadonnées export (timestamp, user_id, version schema, tables count, total rows)
- `user.json` — profil user (id, email, created_at — **jamais `password_hash`**)
- `identity_providers.json` — Google linked accounts
- `auth_events.json` — historique audit du user (50 dernières par défaut, all si `?full=true` — events `admin_*` exclus du défaut, voir §6)
- `health/sleep_sessions.csv` (+ `.json`), `health/sleep_stages.csv`, ... (21 tables santé, déchiffrement transparent ORM via V2.2.1)

**Endpoints** :
```python
@router.post("/me/export/request")
@limiter.limit("5/hour", key_func=_user_id_key)
async def export_request(
    request: Request,
    body: ExportRequestIn,  # {"password": str | None}
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> ExportRequestOut:
    """Re-auth + génère export_token TTL 5min single-use, purpose=account_export_confirm."""
    check_sec_fetch_site(request)
    _verify_reauth(db, current_user, body.password)  # password OU OAuth nonce ; soft backoff V2.3.3.1 si fail
    token = _create_verification_token(db, current_user, purpose="account_export_confirm", ttl_seconds=300)
    _safe_audit_event(db, "rgpd.export.requested", user_id=current_user.id, meta={"expires_at": token.expires_at.isoformat()})
    db.commit()
    return ExportRequestOut(export_token=token.raw, expires_at=token.expires_at)

@router.get("/me/export/confirm")
@limiter.limit("5/hour", key_func=_user_id_key)
async def export_confirm(
    request: Request,
    export_token: str = Query(..., min_length=32),
    full: bool = Query(False),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> StreamingResponse:
    """Consomme le token, stream le ZIP. SELECT FOR UPDATE sur users.id (HIGH 4)."""
    with db.begin():
        _consume_verification_token_atomic(db, current_user, export_token, purpose="account_export_confirm")
        db.execute(select(User).where(User.id == current_user.id).with_for_update())  # bloque erase concurrent
        spool = build_user_export_zip(db, current_user, full_audit=full)  # SpooledTemporaryFile(max_size=10MB)
    size_bytes = spool.tell()
    spool.seek(0)
    _safe_audit_event(db, "rgpd.export.downloaded", user_id=current_user.id,
                      meta={"full": full, "size_bytes": size_bytes, "ip_hash": _ip_hmac(request)})
    db.commit()
    return StreamingResponse(
        spool,  # auto-spill disk au-delà 10MB, pas de RAM blow-up
        media_type="application/zip",
        headers={
            "Content-Disposition": f'attachment; filename="export_my_data_{date.today().isoformat()}.zip"',  # filename générique (Décision 7)
            "Cache-Control": "no-store",
        },
    )
```

**Rate-limit `5/hour/user`** par endpoint : 5 export_request + 5 export_confirm/heure max, anti-DoS self.

### 1.bis Race `export` ↔ `erase` — serialization (HIGH 4)

`build_user_export_zip` ouvre `with db.begin():` + `SELECT ... FOR UPDATE` sur `users.id` au tout début. Si un `erase_confirm` concurrent tente d'acquérir le même verrou pendant un export en cours, il bloque jusqu'à fin de l'export (ou timeout court 10s → 503 retry-after). Cohérent : un export commencé doit finir avant l'erase, pas de fichier ZIP partiel orphelin.

### 2. `POST /me/erase` — Right to Erasure (Art. 17) — **cascade applicatif (HIGH 3) + anonymisation complète (HIGH 2)**

**Confirmation à 2 étapes** (anti-accident + re-auth) :

**Étape 1** : `POST /me/erase/request` body `{"password": str | None}` → 200 `{erase_token, expires_at}` (random 32 bytes URL-safe, TTL 5 min, single-use, `verification_tokens.purpose = account_erase_confirm`). Soft backoff V2.3.3.1 si password wrong. Audit `rgpd.erase.requested`.

**Étape 2** : `POST /me/erase/confirm` body `{"erase_token": "..."}` → 204 No Content.

```python
def erase_user_cascade(db: Session, user_id: int) -> EraseStats:
    """Cascade applicatif explicit — les 21 tables santé n'ont PAS ondelete=CASCADE (Décision pentester HIGH 3)."""
    with db.begin():
        db.execute(select(User).where(User.id == user_id).with_for_update())  # bloque export concurrent
        # 1. Audit AVANT delete users (pour que l'event existe ; sera anonymisé juste après)
        _safe_audit_event(db, "rgpd.erase.confirmed", user_id=user_id, meta={"step": "begin"})
        # 2. 21 tables santé — DELETE explicit
        stats = {}
        for table in HEALTH_TABLES:  # liste explicite des 21 tables (sleep_sessions, sleep_stages, mood, ...)
            r = db.execute(text(f"DELETE FROM {table} WHERE user_id = :uid"), {"uid": user_id})
            stats[table] = r.rowcount
        # 3. identity_providers, refresh_tokens, verification_tokens
        for table in ("identity_providers", "refresh_tokens", "verification_tokens"):
            r = db.execute(text(f"DELETE FROM {table} WHERE user_id = :uid"), {"uid": user_id})
            stats[table] = r.rowcount
        # 4. auth_events anonymisés (HIGH 2 — irréversibilité Art. 17 ; PAS de re-création email_hash post-erase via _safe_audit_event)
        _anonymize_auth_events(db, user_id)
        # 5. enfin DELETE users
        db.execute(text("DELETE FROM users WHERE id = :uid"), {"uid": user_id})
    return EraseStats(tables=stats, total_rows=sum(stats.values()))


def _anonymize_auth_events(db: Session, user_id: int) -> None:
    """RGPD Art. 17 — irréversibilité. Anonymise toutes les colonnes identifiantes (HIGH 2)."""
    db.execute(text("""
        UPDATE auth_events
        SET user_id = NULL,
            email_hash = NULL,
            ip_hash = NULL,
            user_agent = NULL
        WHERE user_id = :uid
    """), {"uid": user_id})
```

**`_safe_audit_event(db, event_type, user_id, meta)`** (helper pentester H2) :
- Vérifie que `users.id == user_id` existe en DB AVANT d'insérer la row `auth_events`. Si user déjà supprimé → skip silencieux (pas de re-création `email_hash` orphelin post-erase).
- Utilisé par TOUS les audit calls Phase 3 et plus larges (extension safe par défaut).

**Why hard delete + anonymisation `auth_events`** : RGPD Art. 17 = effacement réel des données identifiantes ; les rows `auth_events` survivent (anonymes) au titre de l'intérêt légitime sécu (preuve forensique post-breach), un attaquant qui a volé un token ne doit pas pouvoir effacer ses traces via le flow erase.

**Pour OAuth-only users** (`password_hash == OAUTH_SENTINEL`) : pas d'email outbound. `POST /me/erase/request` body `{"oauth_nonce": "..."}` ; le frontend ré-initialise un Google sign-in et passe le nonce signé Google → vérifié côté serveur (cohérent V2.3.2). Cryptographiquement plus fort que magic link email.

### 3. `GET /me/audit-log` — Right of Access (Art. 15) — **filter `admin_*` (Décision 6)**

Retourne les `auth_events` du current_user, paginated, **excluant par défaut les events `admin_*`** (lifecycle admin, sondages, etc. — non pertinents pour le user end et pollueraient le log).

```python
@router.get("/me/audit-log")
@limiter.limit("60/hour", key_func=_user_id_key)
async def audit_log_my_account(
    request: Request,
    limit: int = Query(50, gt=0, le=200),
    offset: int = Query(0, ge=0),
    include_admin: bool = Query(False),  # admin events visibles uniquement via export ZIP ?full=true par défaut
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> AuditLogPage:
    """RGPD Art. 15 — droit d'accès. Liste des auth_events liés au current_user."""
    q = select(AuthEvent).where(AuthEvent.user_id == current_user.id)
    if not include_admin:
        q = q.where(AuthEvent.event_type.notlike("admin\\_%", escape="\\"))
    rows = db.execute(q.order_by(AuthEvent.created_at.desc()).limit(limit).offset(offset)).scalars().all()
    total_q = select(func.count()).select_from(AuthEvent).where(AuthEvent.user_id == current_user.id)
    if not include_admin:
        total_q = total_q.where(AuthEvent.event_type.notlike("admin\\_%", escape="\\"))
    total = db.execute(total_q).scalar_one()
    _safe_audit_event(db, "rgpd.audit_log.read", user_id=current_user.id, meta={"limit": limit, "offset": offset})
    db.commit()
    return AuditLogPage(
        events=[AuditEventOut.model_validate(r) for r in rows],
        total=total, limit=limit, offset=offset,
    )
```

`AuditEventOut` Pydantic : `{id, event_type, ip_hash, user_agent, created_at, meta}` — **jamais** l'IP brute ni `email_hash` raw. Le user voit son propre log avec ip_hash HMAC (cohérent cross-events pour repérer IP suspecte).

**Cap meta size 4KB** (pentester MED) : à l'insertion `auth_events.meta`, vérifier `len(json.dumps(meta).encode()) < 4096` ; sinon truncate + flag `meta.truncated=true`. Évite qu'un endpoint mal codé écrive des MB de meta dans audit_events.

### 4. Auth scope — `current_user` injecté via Depends

Tous les endpoints `/me/*` exigent `Depends(get_current_user)` (V2.3 helper existant qui décode le JWT access token + lookup `users` table). Aucun `user_id` en path/query → pas d'IDOR possible. L'access token étant scopé au `sub=user_id`, le user ne peut accéder qu'à ses propres données by design.

Si access token invalide/expiré → 401 (JWT validation V2.3 raise HTTPException).

### 5. Anti-énumération via timing

**`POST /me/erase/request`** vérifie le password. Si wrong → 401 `invalid_credentials`. Soft backoff V2.3.3.1 s'applique (sleep exponentiel sur fail). Pas d'enum nouveau (le user est déjà loggué via JWT).

### 6. CSRF check `Sec-Fetch-Site`

Tous les POST `/me/erase/*` ajoutent `check_sec_fetch_site(request)` (pattern V2.3.3.2 + V2.3.3.3). Évite qu'un site malveillant force un user loggué à supprimer son compte via auto-submit form.

### 7. Rate-limit

| Endpoint | Limite | Justification |
|----------|--------|---------------|
| `GET /me/export` | 5/hour par user | export = expensive payload, anti-DoS self |
| `POST /me/erase/request` | 5/hour par user | anti-spam token verification_tokens |
| `POST /me/erase/confirm` | 5/hour par user | erase irréversible, pas de raison de spam |
| `GET /me/audit-log` | 60/hour par user | lecture, plus permissif |

Réutilise `_user_id_key` V2.3.3.1.

### 8. Audit events nouveaux

Extension `auth_events.event_type` :
- `rgpd.export.requested` (meta: expires_at)
- `rgpd.export.downloaded` (meta: full, size_bytes, ip_hash)
- `rgpd.erase.requested` (meta: oauth_only, expires_at)
- `rgpd.erase.confirmed` (meta: tables_deleted, total_rows, step)
- `rgpd.erase.failed` (meta: reason)
- `rgpd.audit_log.read` (meta: limit, offset)

Les rows `rgpd.erase.confirmed` survivent à l'erase (anonymisées via UPDATE user_id=NULL,email_hash=NULL,ip_hash=NULL,user_agent=NULL) — preuves légales.

**Tous insérés via `_safe_audit_event`** : skip silencieux si `users.id` n'existe plus (HIGH 2 — pas de re-création email_hash post-erase).

### 9. Streaming ZIP — `SpooledTemporaryFile` (bonus pentester)

`tempfile.SpooledTemporaryFile(max_size=10*1024*1024, mode="w+b")` au lieu de `io.BytesIO()` : reste en RAM jusqu'à 10MB puis spill disk transparent. Pas de RAM blow-up sur exports massifs.

**Cap 50 MB** : si `spool.tell() > 50*1024*1024`, retourner `413 Payload Too Large` + log `rgpd.export.too_large`. Pour V2.3 typical 1-5 MB, jamais hit. Compression bomb fallback (export par-table chunks) différé Phase 3+.

### 11. `verification_tokens.purpose` enum DB CHECK (pentester MED)

Migration Alembic ajoute `CHECK (purpose IN ('email_verify', 'password_reset', 'account_erase_confirm', 'account_export_confirm'))` sur la colonne `purpose`. Bloque cross-purpose token reuse au niveau DB (defense in depth, en plus du check applicatif).

### 12. Atomic single-use consumption (pentester LOW)

`_consume_verification_token_atomic(db, user, token, purpose)` :
```sql
UPDATE verification_tokens
SET consumed_at = now()
WHERE token_hash = :h AND user_id = :uid AND purpose = :p AND consumed_at IS NULL AND expires_at > now()
RETURNING id
```
Si `rowcount == 0` → 400 invalid_or_consumed_token. Anti-race natif côté DB, pas besoin de SELECT FOR UPDATE séparé.

### 10. Soft delete vs hard delete

**Décision : hard delete** (`DELETE FROM ...`).

Justification :
- RGPD Art. 17 = "effacement" implique suppression réelle, pas marquage soft.
- Self-host = le user trust son admin pour effacer réellement (vs SaaS où soft delete = courant pour raisons techniques).
- Backups : hors scope code, à documenter README "erase ne purge pas les backups antérieurs — l'admin doit purger backups manuellement post-erase si demandé".

Note : `auth_events` anonymisé par UPDATE user_id=NULL (pas DELETE). C'est cohérent RGPD (les rows sont conservées pour intérêt légitime sécu, mais ne contiennent plus de donnée perso identifiante après l'UPDATE — le `ip_hash` HMAC reste mais est dé-corrélable de l'identité user).

## Livrables

### Backend
- [ ] `server/routers/me.py` (NEW) : router avec 5 endpoints (`POST /me/export/request`, `GET /me/export/confirm`, `POST /me/erase/request`, `POST /me/erase/confirm`, `GET /me/audit-log`) + Pydantic models (`ExportRequestIn`, `ExportRequestOut`, `EraseRequestIn`, `EraseConfirmIn`, `AuditEventOut`, `AuditLogPage`)
- [ ] `server/security/rgpd.py` (NEW) :
  - `build_user_export_zip(db, user, full_audit) -> SpooledTemporaryFile` (avec `SELECT FOR UPDATE` au début, max_size=10MB)
  - `erase_user_cascade(db, user_id) -> EraseStats` (cascade applicatif explicit 21 tables + anonymisation)
  - `_anonymize_auth_events(db, user_id)` (UPDATE user_id=NULL, email_hash=NULL, ip_hash=NULL, user_agent=NULL)
  - `_safe_audit_event(db, event_type, user_id, meta)` (skip si user déjà supprimé — HIGH 2)
  - `_consume_verification_token_atomic(db, user, token, purpose)` (UPDATE...RETURNING anti-race)
  - `_verify_reauth(db, user, password_or_nonce)` (password OU OAuth nonce + soft backoff)
  - `_ip_hmac(request)` (réutilise V2.3.3.1 helper)
  - `HEALTH_TABLES: tuple[str, ...]` (liste explicite des 21 tables santé)
- [ ] `server/main.py` : `app.include_router(me.router)`
- [ ] **Alembic migration** : ajout `CHECK (purpose IN (...))` sur `verification_tokens.purpose` (extension purpose enum avec `account_export_confirm`)

### Tests
- [ ] `tests/server/test_me_export.py` (~13 tests) :
  - `TestExportRequestReauth` : 401 sans token, 401 wrong password (+ soft backoff), 200 OK + token TTL 5min, OAuth-only flow nonce
  - `TestExportConfirmZip` : token invalide → 400, token consumed → 400, ZIP structure (manifest + user.json + identity_providers + auth_events + 21×csv+json), filename générique `export_my_data_{date}.zip`
  - `TestExportContent` : no `password_hash` leak, CSV columns match DB, mood déchiffré
  - `TestUserIsolation` : 2 users distincts → ZIP user A ne contient AUCUNE data user B
  - `TestExportRateLimit` : 5 request/h OK, 6e → 429 ; idem confirm
  - `TestExportRaceWithErase` : export en cours bloque erase concurrent (test threadé ou simulé via `with_for_update` mock)
- [ ] `tests/server/test_me_erase.py` (~14 tests) :
  - `TestErasePreconditions` : 401 sans token, 401 wrong password + soft backoff, CSRF Sec-Fetch-Site cross-site → 403
  - `TestEraseCascadeAllTables` : post-erase, `SELECT COUNT WHERE user_id=?` == 0 sur **chacune** des 21 tables santé + identity_providers + refresh_tokens + verification_tokens
  - `TestEraseAuthEventsAnonymized` : `auth_events` rows du user existent toujours mais `user_id IS NULL AND email_hash IS NULL AND ip_hash IS NULL AND user_agent IS NULL`
  - `TestEraseAntiAccident` : token expiré → 400, token wrong purpose → 400, idempotent (2e call → 400 consumed)
  - `TestEraseOAuthOnly` : user `password_hash == OAUTH_SENTINEL` → password rejected, oauth_nonce accepted
  - `TestEraseTokenSingleUse` : 2 calls confirm en // → un seul réussit (atomic UPDATE...RETURNING)
  - Audit row `rgpd.erase.confirmed` créée AVANT delete users → existe en DB anonymisée post-erase
  - `_safe_audit_event` post-erase ne crée PAS de nouvelle row (user_id n'existe plus)
- [ ] `tests/server/test_me_audit_log.py` (~7 tests) :
  - `TestAuditLogScope` : retourne UNIQUEMENT events `user_id == current_user.id`
  - `TestAuditLogAdminFilter` : par défaut events `admin_*` exclus ; `?include_admin=true` les inclut
  - `TestAuditLogPagination` : limit/offset corrects, total count global
  - `TestAuditLogIpHmac` : `events[0].ip_hash` length == 16 hex, jamais d'IP brute
  - `TestAuditLogMetaCap` : insertion meta > 4KB → truncate + flag `meta.truncated=true`
  - Rate-limit 60/hour (60 OK, 61e 429)
  - Ordering DESC par `created_at`

### Docs
- [ ] `README.md` : section "RGPD endpoints (Phase 3)" + workflow user
- [ ] `NOTES.md` : entry "Backups RGPD — admin doit purger manuellement post-erase"
- [ ] `HISTORY.md` : entry changelog

## Tests d'acceptation

### Export — 2-step
1. **`POST /me/export/request` sans token** → 401.
2. **`POST /me/export/request` wrong password** → 401 + soft backoff V2.3.3.1.
3. **`POST /me/export/request` OK** → 200 `{export_token, expires_at}`. Token stocké en `verification_tokens` purpose=`account_export_confirm` TTL 5min.
4. **`GET /me/export/confirm?export_token=` invalide** → 400 invalid_or_consumed_token.
5. **`GET /me/export/confirm?export_token=` consumed (2e call)** → 400.
6. **`GET /me/export/confirm` OK** → 200 `application/zip` + `Content-Disposition: attachment; filename="export_my_data_{date}.zip"` (filename générique, pas de user_id leak).
7. **ZIP structure** : `manifest.json` + `user.json` + `identity_providers.json` + `auth_events.json` + `health/{table}.csv` + `health/{table}.json` pour les 21 tables.
8. **`user.json` no password_hash** : pas de key `password_hash` dans le JSON.
9. **CSV columns match DB** : `health/sleep_sessions.csv` headers = exact columns du table sleep_sessions.
10. **`health/mood.csv` content déchiffré** : fields chiffrés V2.2.1 (`mood_score`, `notes`) en clair (transparent ORM).
11. **Scope user isolation** : 2 users avec data, export user A → ZIP ne contient AUCUNE data user B.
12. **Rate-limit 5/h/user** : 5 request OK, 6e → 429 ; idem confirm.
13. **Audit `rgpd.export.requested` + `rgpd.export.downloaded`** : après flow complet, 2 rows en auth_events avec meta `size_bytes` + `ip_hash`.
14. **`?full=true`** : auth_events.json contient TOUTES les rows du user (pas juste 50).
15. **Race export ↔ erase** : un erase concurrent pendant un export en cours bloque jusqu'à fin export (test via threading ou mock `with_for_update`).
16. **OAuth-only export** : user OAuth-only → password rejected, oauth_nonce accepté.

### Erase
17. **`POST /me/erase/request` sans token** → 401.
18. **`POST /me/erase/request` avec token + bon password** → 200 `{erase_token, expires_at}`. Token stocké purpose=`account_erase_confirm`.
19. **`POST /me/erase/request` wrong password** → 401 + soft backoff.
20. **`POST /me/erase/request` OAuth-only user** → password rejected, oauth_nonce accepté (PAS d'email outbound).
21. **`POST /me/erase/confirm` token valide** → 204. Cascade delete :
    - `SELECT COUNT WHERE user_id=?` == 0 sur **chacune** des 21 tables santé (test param par table)
    - `identity_providers`, `refresh_tokens`, `verification_tokens` rows supprimées
    - `users` row supprimée
    - `auth_events` rows : `user_id IS NULL AND email_hash IS NULL AND ip_hash IS NULL AND user_agent IS NULL` (HIGH 2)
22. **`POST /me/erase/confirm` token invalide/expiré/wrong purpose** → 400.
23. **`POST /me/erase/confirm` token déjà consommé** → 400.
24. **Audit `rgpd.erase.confirmed` AVANT delete users** : row existe post-erase, anonymisée.
25. **`_safe_audit_event` post-erase no-op** : appel `_safe_audit_event(db, "test", user_id=deleted_uid)` → 0 rows insérées.
26. **Idempotent** : 2 erases en // sur même token → un seul réussit (atomic UPDATE...RETURNING).
27. **CSRF** : POST /me/erase/{request,confirm} avec `Sec-Fetch-Site: cross-site` → 403.

### Audit-log
28. **`GET /me/audit-log` sans token** → 401.
29. **`GET /me/audit-log` OK** → 200 `{events, total, limit, offset}`.
30. **Scope user** : UNIQUEMENT events `user_id == current_user.id`.
31. **Filter `admin_*` par défaut** : un event `admin_user_create` n'apparaît PAS dans la response sans `?include_admin=true`.
32. **`?include_admin=true`** : event `admin_*` inclus.
33. **`ip_hash` length == 16 hex** : pas d'IP brute.
34. **Pagination** : `?limit=10&offset=20` → max 10 events, total = count global.
35. **Ordering DESC** : events[0].created_at >= events[1].created_at.
36. **Rate-limit 60/h/user** : 60 OK, 61e → 429.
37. **`auth_events.meta` cap 4KB** : insertion d'un event avec meta > 4KB → row stockée avec `meta.truncated=true` + content tronqué.

### Migration & enum
38. **Alembic migration `verification_tokens.purpose` CHECK** : tentative INSERT avec purpose=`malicious` → DB rejette (`CHECK constraint failed`).

### Non-régression
39. **Pas de régression V2.3 → V2.3.3.3** : 373 + 1 skip restent verts.

## Out of scope Phase 3

- **Backups purge** : différé doc admin (pas du code)
- **Anonymisation des données santé soft delete** (au lieu de DELETE) : différé V3 si besoin légal spécifique
- **Bulk erase admin endpoint** (`POST /admin/users/{id}/erase`) : différé V3, l'admin peut faire DELETE direct DB pour l'instant
- **Export incremental** (depuis last export) : différé
- **Rate-limit IP global** sur /me/* : composite par user déjà couvre, pas besoin pur-IP

## Suite naturelle

- **Phase 6 master plan** : CI/CD multi-env (workflows GitHub Actions deploy dev/prod, secrets, smoke tests)
- **Phase 4 master plan** : Android Compose shell + WebView dashboard
