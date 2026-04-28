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
    symbols: [router, export_my_data, erase_my_data, audit_log_my_account]
  - file: server/security/rgpd.py
    symbols: [build_user_export_zip, erase_user_cascade, EraseConfirmation]
  - file: server/main.py
    symbols: [app]
tested_by:
  - file: tests/server/test_me_export.py
    classes: [TestExportZip, TestExportContent, TestExportRateLimit, TestUserIsolation]
  - file: tests/server/test_me_erase.py
    classes: [TestErasePreconditions, TestEraseCascade, TestEraseAudit, TestEraseAntiAccident]
  - file: tests/server/test_me_audit_log.py
    classes: [TestAuditLogScope, TestAuditLogPagination, TestAuditLogIpHmac]
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

## Pentester verdict — état du patch

**Statut au commit initial** : pentester reviewé 2026-04-28 → verdict `WARN`, 4 HIGH bloquants + 3 décisions design + 5 risques additionnels (3 HIGH + 4 MED + 4 LOW).

**Patch en cours** : section "Architecture révisée post-pentester" ci-dessous est la synthèse des 7 points principaux **intégrés en architecture**. Les sections **Livrables**, **Endpoints**, **Tests d'acceptation**, **frontmatter `tested_by`** ci-après peuvent encore référencer la version pre-patch — à reconciler dans la session TDD/impl prochaine.

**Risques additionnels pentester non encore intégrés explicitement** :
- HIGH H1 — race `erase_request` ↔ `audit-log`/`export` concurrent → `with db.begin():` + `SELECT FOR UPDATE` sur `users.id` dans `build_user_export_zip`
- HIGH H2 — `audit_event()` post-erase ne doit PAS logger `email_hash` si user_id n'existe plus en DB
- MED — token `verification_tokens.purpose` enum DB CHECK constraint (anti cross-purpose token reuse)
- MED — `/me/audit-log` `meta_size < 4KB` cap côté insertion
- MED — Compression bomb fallback (cap 50MB hit → export par-table chunks différé Phase 3+)
- LOW — Atomic `UPDATE consumed_at WHERE consumed_at IS NULL RETURNING *` (anti race single-use)
- LOW — `audit_event` AVANT `delete users` row : event créé puis immédiatement anonymisé par `_anonymize_auth_events` (cohérent, à tester explicit)

**Action prochaine session** : finir reconciliation Livrables/Tests/frontmatter avec ces 7 points + procéder TDD RED.

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

### 1. `GET /me/export` — Right to Portability (Art. 20)

**Format** : **ZIP** contenant :
- `manifest.json` — métadonnées export (timestamp, user_id, version schema, tables count, total rows)
- `user.json` — profil user (id, email, created_at, etc. — sans password_hash)
- `identity_providers.json` — Google linked accounts
- `auth_events.json` — toute l'historique audit du user (50 dernières par défaut, all si `?full=true`)
- `health/sleep_sessions.csv`, `health/sleep_stages.csv`, ... (1 CSV par table santé, 21 fichiers)
- `health/sleep_sessions.json`, ... (idem en JSON pour qui préfère)

**CSV format** : tous les champs déchiffrés (V2.2.1 chiffrement at-rest est transparent au niveau ORM). Les datetimes en ISO 8601 UTC. Headers exacts des colonnes DB.

**Streaming** : ZIP construit en mémoire (`io.BytesIO`) puis renvoyé via `StreamingResponse`. Acceptable jusqu'à ~50 MB (typical 5 ans de Samsung Health = ~10MB CSV).

**Endpoint** :
```python
@router.get("/me/export")
@limiter.limit("5/hour", key_func=_user_id_key)
async def export_my_data(
    request: Request,
    full: bool = Query(False, description="Include full audit_events history (default: 50 recent)"),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> StreamingResponse:
    """RGPD Art. 20 — droit à la portabilité. Export ZIP de toutes les données du user."""
    zip_bytes = build_user_export_zip(db, current_user, full_audit=full)
    audit_event(db, "rgpd.export", user_id=current_user.id, meta={"full": full, "size_bytes": len(zip_bytes)})
    db.commit()
    return StreamingResponse(
        iter([zip_bytes]),
        media_type="application/zip",
        headers={
            "Content-Disposition": f'attachment; filename="export_{current_user.id}_{date.today().isoformat()}.zip"',
            "Cache-Control": "no-store",
        },
    )
```

**Rate-limit `5/hour/user`** : empêche un user compromis (token volé) de scraper l'export en boucle. 5/heure suffit largement pour usage légitime.

### 2. `POST /me/erase` — Right to Erasure (Art. 17)

**Confirmation à 2 étapes** (anti-accident + anti-CSRF supplémentaire) :

**Étape 1** : `POST /me/erase/request` body `{"password": "..."}` → 200 `{erase_token, expires_at}` (token random 32 bytes URL-safe, TTL 5 min, single-use, stocké en `verification_tokens` purpose=`account_erase_confirm`). Audit `rgpd.erase.requested`.

**Étape 2** : `POST /me/erase/confirm` body `{"erase_token": "..."}` → 204 No Content. Cascade delete :
- `DELETE FROM sleep_sessions WHERE user_id = ?` (et 20 autres tables santé)
- `DELETE FROM identity_providers WHERE user_id = ?`
- `DELETE FROM refresh_tokens WHERE user_id = ?` (révoque toutes les sessions)
- `DELETE FROM verification_tokens WHERE user_id = ?`
- **`auth_events` rows** : on garde, mais on **anonymise** : `UPDATE auth_events SET user_id = NULL, email_hash = NULL WHERE user_id = ?`. Justification RGPD Art. 17(3)(e) : "obligations légales de conservation pour preuves" — un attaquant ne doit pas pouvoir effacer ses traces de breach via le flow erase.
- `DELETE FROM users WHERE id = ?` (supprime le compte lui-même)
- Audit `rgpd.erase.completed` (au moment de l'erase, AVANT que la row user disparaisse → l'event reste avec `user_id` set au moment du log puis NULL après UPDATE).

**Why 2-step + password** : un attaquant qui a volé un access token peut sinon supprimer le compte d'un coup. Ré-authentification password = preuve qu'il s'agit du vrai user.

**Pour OAuth-only users** : si `password_hash == OAUTH_SENTINEL`, on accepte un token verification émis sur l'email Google linked (purpose `account_erase_oauth_request` envoyé via email_outbound V2.3.1) au lieu du password.

### 3. `GET /me/audit-log` — Right of Access (Art. 15)

Retourne les `auth_events` rows du current_user, paginated.

```python
@router.get("/me/audit-log")
@limiter.limit("60/hour", key_func=_user_id_key)
async def audit_log_my_account(
    limit: int = Query(50, gt=0, le=200),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_session),
    current_user: User = Depends(get_current_user),
) -> dict:
    """RGPD Art. 15 — droit d'accès. Liste des auth_events liés au current_user."""
    rows = db.execute(
        select(AuthEvent)
        .where(AuthEvent.user_id == current_user.id)
        .order_by(AuthEvent.created_at.desc())
        .limit(limit)
        .offset(offset)
    ).scalars().all()
    total = db.execute(
        select(func.count()).select_from(AuthEvent).where(AuthEvent.user_id == current_user.id)
    ).scalar_one()
    audit_event(db, "rgpd.audit_log.read", user_id=current_user.id, meta={"limit": limit, "offset": offset})
    db.commit()
    return {
        "events": [AuditEventOut.from_orm(r).model_dump(mode="json") for r in rows],
        "total": total,
        "limit": limit,
        "offset": offset,
    }
```

`AuditEventOut` Pydantic : `{id, event_type, ip_hash, user_agent, created_at, meta}` — **jamais** l'IP brute ni `email_hash` raw (déjà HMAC). Le user voit son propre log avec ip_hash (anonymisé, mais cohérent cross-events pour qu'il puisse repérer une IP suspecte).

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
- `rgpd.export` (meta: full, size_bytes)
- `rgpd.erase.requested` (meta: oauth_only, expires_at)
- `rgpd.erase.confirmed` (meta: tables_deleted, total_rows)
- `rgpd.erase.failed` (meta: reason)
- `rgpd.audit_log.read` (meta: limit, offset)

Les rows `rgpd.erase.confirmed` survivent à l'erase (anonymisées via UPDATE user_id=NULL) — preuves légales.

### 9. Streaming ZIP — taille max + chunks

**Cap 50 MB** : si `zip_bytes` > 50 MB, retourner `413 Payload Too Large` + log `rgpd.export.too_large`. Indice qu'un user a trop de données et qu'il faudra implémenter export chunked Phase 3+. Pour V2.3 typical 1-5 MB, jamais hit.

**Génération en RAM acceptable** : `io.BytesIO()` + `zipfile.ZipFile` standard library. Taille typique ~5 MB → OK. Future-proof : si on hit 50 MB → migration vers `tempfile.SpooledTemporaryFile` ou stream chunks via async generator.

### 10. Soft delete vs hard delete

**Décision : hard delete** (`DELETE FROM ...`).

Justification :
- RGPD Art. 17 = "effacement" implique suppression réelle, pas marquage soft.
- Self-host = le user trust son admin pour effacer réellement (vs SaaS où soft delete = courant pour raisons techniques).
- Backups : hors scope code, à documenter README "erase ne purge pas les backups antérieurs — l'admin doit purger backups manuellement post-erase si demandé".

Note : `auth_events` anonymisé par UPDATE user_id=NULL (pas DELETE). C'est cohérent RGPD (les rows sont conservées pour intérêt légitime sécu, mais ne contiennent plus de donnée perso identifiante après l'UPDATE — le `ip_hash` HMAC reste mais est dé-corrélable de l'identité user).

## Livrables

### Backend
- [ ] `server/routers/me.py` (NEW) : router avec 4 endpoints (`GET /me/export`, `POST /me/erase/request`, `POST /me/erase/confirm`, `GET /me/audit-log`) + Pydantic models (`AuditEventOut`, `EraseRequestIn`, `EraseConfirmIn`)
- [ ] `server/security/rgpd.py` (NEW) : `build_user_export_zip(db, user, full_audit) -> bytes`, `erase_user_cascade(db, user_id) -> EraseStats`, helper `_anonymize_auth_events(db, user_id)`
- [ ] `server/main.py` : `app.include_router(me.router)`
- [ ] (optionnel) `server/security/email_outbound.py` : nouveau template "account_erase_oauth_request" pour OAuth-only users (réutilise pattern V2.3.1)

### Tests
- [ ] `tests/server/test_me_export.py` (~10 tests : ZIP structure, CSV/JSON par table, manifest, no password_hash leak, scope user_id isolation, rate-limit 5/hour, payload size cap 50MB, audit row inserted)
- [ ] `tests/server/test_me_erase.py` (~12 tests : 2-step flow, password required, OAuth-only flow, cascade delete 21 tables, refresh_tokens revoked, identity_providers deleted, auth_events anonymized not deleted, idempotent if user already deleted, audit row created BEFORE user delete)
- [ ] `tests/server/test_me_audit_log.py` (~6 tests : scope user_id, pagination limit/offset, ip_hash never raw, total count correct, ordering DESC by created_at, rate-limit)

### Docs
- [ ] `README.md` : section "RGPD endpoints (Phase 3)" + workflow user
- [ ] `NOTES.md` : entry "Backups RGPD — admin doit purger manuellement post-erase"
- [ ] `HISTORY.md` : entry changelog

## Tests d'acceptation

### Export
1. **`GET /me/export` sans token** → 401.
2. **`GET /me/export` avec token** → 200 `application/zip` + `Content-Disposition: attachment; filename="export_{uuid}_{date}.zip"`.
3. **ZIP structure** : contient `manifest.json` + `user.json` + `identity_providers.json` + `auth_events.json` + `health/{table}.csv` + `health/{table}.json` pour les 21 tables.
4. **`user.json` no password_hash** : grep le JSON → no key `password_hash`.
5. **CSV columns match DB schema** : `health/sleep_sessions.csv` headers = exact columns of `sleep_sessions` table (sauf cols `_crypto_v` qui peuvent être omises).
6. **`health/mood.csv` content déchiffré** : test V2.2.1 + Phase 3 — les fields chiffrés (`mood_score`, `notes`) sont en clair dans le CSV (transparent ORM decryption).
7. **Scope user isolation** : créer 2 users avec data, GET /me/export comme user A → ZIP ne contient AUCUNE data de user B.
8. **Rate-limit 5/hour/user** : 5 GET → OK, 6e → 429.
9. **Audit row** : après export, query `auth_events` → 1 row `event_type=rgpd.export` + meta avec `size_bytes` + `full`.
10. **`?full=true`** : audit_events.json contient TOUTES les rows du user (pas juste 50). Avec un user qui a 200 events → length == 200.

### Erase
11. **`POST /me/erase/request` sans token** → 401.
12. **`POST /me/erase/request` avec token + bon password** → 200 `{erase_token, expires_at}`. Token stocké en `verification_tokens` purpose=`account_erase_confirm` TTL 5min.
13. **`POST /me/erase/request` wrong password** → 401 + soft backoff sleep V2.3.3.1.
14. **`POST /me/erase/request` OAuth-only user (no password)** → bypass password check, génère token + envoie email outbound.
15. **`POST /me/erase/confirm` token valide** → 204 No Content. Cascade delete :
    - `users` row supprimée
    - `sleep_sessions`, `mood`, etc. (21 tables) → supprimées toutes les rows du user
    - `identity_providers` row supprimée
    - `refresh_tokens` rows supprimées (toutes les sessions révoquées)
    - `verification_tokens` rows supprimées
    - `auth_events` rows : `user_id=NULL` + `email_hash=NULL` (anonymisées, PAS deleted)
16. **`POST /me/erase/confirm` token invalide/expiré** → 400.
17. **`POST /me/erase/confirm` token déjà consommé (single-use)** → 400.
18. **Audit `rgpd.erase.confirmed` créé AVANT delete users row** : query après erase → row `event_type=rgpd.erase.confirmed` exists avec user_id set au moment du log puis anonymisé.
19. **Cascade idempotent** : tenter erase 2× sur même token → 2e fail (token consumed).
20. **CSRF** : POST /me/erase/{request,confirm} avec `Sec-Fetch-Site: cross-site` → 403.

### Audit-log
21. **`GET /me/audit-log` sans token** → 401.
22. **`GET /me/audit-log` avec token** → 200 `{events: [...], total, limit, offset}`.
23. **Scope user** : retourne UNIQUEMENT les events où `user_id == current_user.id`.
24. **`ip_hash` jamais brut** : `events[0].ip_hash` length == 16 hex (HMAC truncated). Pas de field `ip` ou `last_login_ip` raw.
25. **Pagination** : `?limit=10&offset=20` → max 10 events, skip first 20. `total` = count global (pas paginated).
26. **Ordering DESC** : events[0].created_at >= events[1].created_at.
27. **Rate-limit 60/hour/user** : 60 GET → OK, 61e → 429.

### Non-régression
28. **Pas de régression V2.3 → V2.3.3.3** : 373 + 1 skip restent verts.

## Out of scope Phase 3

- **Backups purge** : différé doc admin (pas du code)
- **Anonymisation des données santé soft delete** (au lieu de DELETE) : différé V3 si besoin légal spécifique
- **Bulk erase admin endpoint** (`POST /admin/users/{id}/erase`) : différé V3, l'admin peut faire DELETE direct DB pour l'instant
- **Export incremental** (depuis last export) : différé
- **Rate-limit IP global** sur /me/* : composite par user déjà couvre, pas besoin pur-IP

## Suite naturelle

- **Phase 6 master plan** : CI/CD multi-env (workflows GitHub Actions deploy dev/prod, secrets, smoke tests)
- **Phase 4 master plan** : Android Compose shell + WebView dashboard
