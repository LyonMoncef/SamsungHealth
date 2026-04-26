---
type: spec
title: "V2.3 — Auth foundation atomique (users + JWT + multi-user FK)"
slug: 2026-04-26-v2-auth-foundation
status: ready
created: 2026-04-26
delivered: null
priority: high
related_plans:
  - 2026-04-23-plan-v2-refactor-master
related_specs:
  - 2026-04-26-v2-structlog-observability
implements:
  - file: server/db/models.py
    symbols: [User, RefreshToken, AuthEvent]
  - file: server/security/auth.py
    symbols: [hash_password, verify_password, _DUMMY_HASH, create_access_token, create_refresh_token, decode_access_token, rotate_refresh_token, revoke_refresh_token, get_current_user, _validate_jwt_secret_at_boot, _validate_registration_token]
  - file: server/security/redaction.py
    symbols: [redact_sensitive_keys, _SENSITIVE_KEYS]
  - file: server/routers/auth.py
    symbols: [router, register, login, refresh, logout]
  - file: server/routers/sleep.py
    symbols: [router, list_sleep, get_sleep_session]
  - file: server/routers/heartrate.py
    symbols: [router]
  - file: server/routers/steps.py
    symbols: [router]
  - file: server/routers/exercise.py
    symbols: [router]
  - file: server/routers/mood.py
    symbols: [router]
  - file: server/main.py
    symbols: [app, lifespan]
  - file: server/logging_config.py
    symbols: [_build_processors]
  - file: alembic/versions/0004_auth_foundation.py
    symbols: [upgrade, downgrade]
  - file: requirements.txt
    symbols: [argon2-cffi, PyJWT]
tested_by:
  - file: tests/server/test_password_hashing.py
    classes: [TestArgon2Params, TestVerifyPassword, TestTimingEqualization]
    methods:
      - test_hash_uses_argon2id
      - test_hash_params_match_rfc9106_profile2
      - test_verify_correct_password
      - test_verify_wrong_password_returns_false
      - test_verify_nonexistent_user_runs_dummy_hash
      - test_verify_constant_time_within_tolerance
  - file: tests/server/test_jwt.py
    classes: [TestJwtSecretValidation, TestAccessToken, TestRefreshToken, TestJwtDecodeFootguns]
    methods:
      - test_jwt_secret_required_at_boot
      - test_jwt_secret_min_256_bits
      - test_jwt_secret_rejects_changeme
      - test_create_access_token_includes_kid
      - test_decode_rejects_alg_none
      - test_decode_requires_explicit_algorithms
      - test_decode_requires_exp_iat_sub_claims
      - test_decode_rejects_missing_iss_aud
      - test_payload_contains_only_sub_no_pii
      - test_previous_secret_decode_only
  - file: tests/server/test_auth_routes.py
    classes: [TestRegister, TestLogin, TestRefresh, TestLogout, TestUserEnumeration]
    methods:
      - test_register_requires_admin_token
      - test_register_creates_user_returns_201
      - test_register_duplicate_email_409
      - test_register_invalid_token_403
      - test_login_returns_access_and_refresh
      - test_login_wrong_password_401_identical_message
      - test_login_unknown_user_401_identical_message
      - test_login_response_time_constant_within_tolerance
      - test_refresh_rotates_token
      - test_refresh_revoked_token_401
      - test_logout_revokes_refresh
      - test_logout_idempotent
  - file: tests/server/test_health_routes_auth.py
    classes: [TestHealthRequiresAuth, TestUserDataIsolation]
    methods:
      - test_sleep_get_without_token_401
      - test_heartrate_get_without_token_401
      - test_steps_get_without_token_401
      - test_exercise_get_without_token_401
      - test_mood_get_without_token_401
      - test_sleep_post_without_token_401
      - test_user_a_cannot_read_user_b_sleep
      - test_user_a_cannot_read_user_b_mood
      - test_user_a_post_associates_with_user_a_id
  - file: tests/server/test_redaction.py
    classes: [TestRedactionProcessor]
    methods:
      - test_redacts_password_key
      - test_redacts_token_key
      - test_redacts_authorization_key
      - test_redacts_jwt_key
      - test_redacts_secret_key
      - test_redacts_cookie_key
      - test_redaction_case_insensitive
      - test_redaction_nested_dict
      - test_redaction_preserves_non_sensitive_keys
  - file: tests/server/test_auth_events.py
    classes: [TestAuthEventLogging]
    methods:
      - test_login_success_writes_event
      - test_login_failure_writes_event_with_email_hash_not_plain
      - test_register_writes_event
      - test_refresh_writes_event
tags: [v2.3, auth, jwt, argon2, multi-user, rgpd, security, atomic]
---

# Spec — V2.3 Auth foundation atomique

## Vision

Introduire l'authentification multi-utilisateur de manière **atomique et non-splittable** : users + JWT (access + refresh) + colonnes `user_id` FK sur les 22 tables santé + filtering systématique dans tous les routers + redaction structlog + audit trail. Une seule PR qui flippe l'app de single-tenant ouvert vers multi-tenant authentifié.

**Décision atomique imposée par audit pentester** : tout split partiel laisserait soit des routes santé ouvertes (brèche Art.9 RGPD notifiable Art.33), soit un UX dégradé qui pousse les users à étendre les TTL JWT en config. Les deux risques sont inacceptables, donc V2.3 ship tout en bloc.

Hors scope V2.3 (différés V2.3.1 → V2.3.3) : reset password flow, Google OAuth, rate limiting + frontend Nightfall login form.

## Décisions techniques

### Hashing

- **Lib** : `argon2-cffi>=23.1`
- **Algo** : argon2id
- **Params** : RFC 9106 profile #2 — `memory_cost=46_080` (≈45 MB), `time_cost=2`, `parallelism=1`. Choisi sur reco pentester (single-thread sur 2-core VM, cible wall-clock 250-350ms vs OWASP defaults qui saturent à 600ms+).
- **Bench au boot** : log `auth.argon2.bench` avec `wall_ms` mesuré à `configure_logging()` (info uniquement, ne bloque pas).
- **Dummy hash** : constante module-level `_DUMMY_HASH` pré-calculée → `verify_password(plain, _DUMMY_HASH)` exécuté quand user introuvable pour timing equalization (cf. risque A pentester).

### JWT

- **Lib** : `PyJWT>=2.8.0` (CVE-2022-29217 fixed). Pas de `python-jose` (unmaintained).
- **Algo** : HS256. Pas de RS256 (single issuer/verifier auto-hébergé).
- **Claims access token** :
  - `sub: <user_uuid>` (string)
  - `iat: <int unix>`
  - `exp: <int unix>` (TTL 30 minutes — relevé de 15min suite reco pentester point #4 atomique)
  - `iss: "samsunghealth"`
  - `aud: "samsunghealth-api"`
  - `typ: "access"`
- **Claims refresh token (JWT)** : idem + `typ: "refresh"`, `exp` 30 jours, `jti: <uuid>` (référence ligne `refresh_tokens`).
- **Header** : `kid: "v1"` injecté dès maintenant pour rotation future sans refonte (reco pentester #3).
- **Secret** : env var `SAMSUNGHEALTH_JWT_SECRET` (≥ 32 octets après base64-decode OU ≥ 256 bits ASCII). Validation au boot via `lifespan` :
  - Présence (sinon `EncryptionConfigError`-style raise)
  - Longueur ≥ 256 bits
  - Reject explicite des strings `"changeme"`, `"secret"`, `"test"`, `"password"`, `"default"` (case-insensitive)
  - Shannon entropy ≥ 4.0 bits/char (sinon raise)
- **Rotation prête** : env var `SAMSUNGHEALTH_JWT_SECRET_PREVIOUS` (optionnelle) acceptée **uniquement en decode**, jamais en sign. Permet rotation sans logout global. Header `kid` distingue "v1" (current) vs "v0" (previous).
- **Decode strict** :
  - `algorithms=["HS256"]` toujours explicite (jamais `None`)
  - `options={"require": ["exp", "iat", "sub", "iss", "aud", "typ"]}`
  - `issuer="samsunghealth"`, `audience="samsunghealth-api"` validés par PyJWT
  - Assertion post-decode : `header["alg"] == "HS256"` (belt-and-braces vs alg confusion)
  - **Aucune PII dans le payload** — pas d'email, pas de password_hash, pas de role.

### Refresh tokens (DB-backed opaques au-dessus du JWT)

Le JWT refresh contient `jti: <uuid>` qui pointe sur une ligne `refresh_tokens` :
- Permet **révocation immédiate** côté serveur (logout, rotation, breach).
- Rotation : à chaque `/auth/refresh`, l'ancien JTI est révoqué et un nouveau JTI/JWT est émis.

Schéma `refresh_tokens` :
- `id UUID v7 PK`
- `user_id UUID FK users.id NOT NULL`
- `jti UUID UNIQUE NOT NULL` (référencé par claim `jti` du JWT)
- `issued_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- `expires_at TIMESTAMPTZ NOT NULL` (30 jours)
- `revoked_at TIMESTAMPTZ NULL` (NULL = actif, valeur = révoqué)
- `replaced_by UUID FK refresh_tokens.id NULL` (chain de rotation pour audit)
- `last_used_at TIMESTAMPTZ NULL`
- `user_agent TEXT NULL`, `ip INET NULL` (audit)

### Schéma `users`

```sql
id UUID v7 PK
email CITEXT UNIQUE NOT NULL          -- non chiffré (lookup login). Art. 4 PII, pas Art. 9.
password_hash TEXT NOT NULL            -- argon2id encoded string
created_at TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
email_verified_at TIMESTAMPTZ NULL     -- timestamptz au lieu de bool (audit) — sera set en V2.3.1 reset/verify
failed_login_count INT NOT NULL DEFAULT 0  -- pour V2.3.3 lockout, schéma stable maintenant
locked_until TIMESTAMPTZ NULL
last_login_at TIMESTAMPTZ NULL
last_login_ip INET NULL
password_changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
is_active BOOL NOT NULL DEFAULT TRUE
```

Extension Postgres requise : `CITEXT` (`CREATE EXTENSION IF NOT EXISTS citext` dans la migration alembic).

### Registration admin-gated (reco pentester #5)

- Endpoint `POST /auth/register` exige header `X-Registration-Token: <value>` matchant env var `SAMSUNGHEALTH_REGISTRATION_TOKEN`.
- Token = string aléatoire ≥ 32 chars, set out-of-band par l'admin (`.env` self-host, secret manager prod).
- Token absent → 403 `{"detail": "registration_disabled"}`. Token incorrect → 403 idem (pas de leak distinguant "absent" vs "incorrect").
- Pas de comptage d'usages dans cette spec (single-use vs multi-use = V2.3.3). Pour l'instant : un seul token statique réutilisable. Documenté dans README.
- Pas d'email verification flow (V2.3.1).
- Argon2 toujours exécuté côté `register` même si token rejeté → timing equalization (le test ne vérifie que le statut, pas le timing register).

### Endpoints

```
POST /auth/register     headers: X-Registration-Token, body: {email, password}
                        201 → {id, email}
                        403 → registration_disabled (token absent/incorrect)
                        409 → email_already_exists
                        422 → validation (email format, password ≥ 12 chars)

POST /auth/login        body: {email, password}
                        200 → {access_token, refresh_token, token_type: "bearer", expires_in: 1800}
                        401 → invalid_credentials  (identique sur user inconnu OU password faux)

POST /auth/refresh      body: {refresh_token}
                        200 → {access_token, refresh_token, ...}  (NOUVEAU refresh, ancien révoqué)
                        401 → invalid_refresh   (révoqué, expiré, malformé, replay)

POST /auth/logout       Authorization: Bearer <access>, body: {refresh_token}
                        204
                        401 → invalid_credentials  (access invalide)
                        Idempotent : refresh déjà révoqué → 204 quand même
```

Tous les endpoints vivent sous `/auth/*` (pas `/api/auth/*` car non-versionnés santé).

### `get_current_user` dependency

```python
async def get_current_user(
    authorization: str | None = Header(default=None),
    db: Session = Depends(get_session),
) -> User:
    # 1. Parse "Bearer <token>"
    # 2. decode_access_token() — raise 401 si invalide
    # 3. lookup users WHERE id = sub AND is_active = true — raise 401 si introuvable/inactif
    # 4. bind user_id_var (ContextVar V2.0.5) pour structlog
    # 5. return User
```

Appliqué sur **tous** les routers santé (sleep/heartrate/steps/exercise/mood) via `Depends(get_current_user)` au niveau APIRouter — chaque endpoint reçoit `current_user: User = Depends(get_current_user)` implicit.

### Multi-user FK sur tables santé (22 tables)

**Migration alembic 0004** :
- Pour chaque table santé existante (sleep_sessions, sleep_stages, heart_rate_records, heart_rate_hourly, heart_rate_daily, steps_records, steps_hourly, steps_daily, exercise_records, mood_records, et 12 autres tables Art.9 listées V2.2.1) :
  - Ajout colonne `user_id UUID NULL` (NULL pour tolérer données legacy)
  - Index `idx_<table>_user_id`
- **Stratégie données legacy** : 1 user "default" auto-créé à la première migration si table `users` vide ET tables santé non vides. Email = `legacy@samsunghealth.local`, password aléatoire (impossible à login — admin doit reset post-migration). Toutes les lignes legacy assignées à cet user. Documenté dans README "post-migration steps".
- Après backfill : migration **suivante (V2.3.1)** passera `user_id NOT NULL`. Pour V2.3 atomique on garde NULL-able pour ne pas bloquer si un script CSV tourne pendant la migration.

**Filtering routers** :
- Tous les `select(SleepSession).where(...)` deviennent `select(SleepSession).where(SleepSession.user_id == current_user.id, ...)`.
- Tous les `pg_insert(SleepSession).values(...)` reçoivent `user_id=current_user.id` injecté côté serveur (jamais accepté du body client).
- `on_conflict_do_nothing(index_elements=...)` : ajouter `user_id` à l'unique index si l'unique inclut le timestamp/end → un même `sleep_start` peut exister pour 2 users différents. Migration update les indexes en conséquence.

### Audit trail

Table `auth_events` (séparée de `users` pour rétention indépendante) :
- `id UUID v7 PK`
- `event_type TEXT NOT NULL` — enum-like : `register`, `login_success`, `login_failure`, `refresh`, `refresh_replay`, `logout`
- `user_id UUID FK users.id NULL` (NULL si user inconnu — login_failure sur email inexistant)
- `email_hash TEXT NULL` — sha256(email.lower()) — permet corrélation breach-investigation sans stocker l'email plain (reco pentester C)
- `ip INET NULL`, `user_agent TEXT NULL`
- `request_id TEXT NULL` (corrélation V2.0.5 structlog)
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`

Insertion async non-bloquante : `db.add(AuthEvent(...))` dans le même transaction que l'op auth, commit unique. Si insert audit fail → log structlog warning, ne crash pas l'auth.

### structlog redaction processor

`server/security/redaction.py` :
```python
_SENSITIVE_KEYS = frozenset({
    "password", "password_hash", "token", "refresh_token", "access_token",
    "authorization", "jwt", "secret", "cookie", "x-registration-token",
})

def redact_sensitive_keys(logger, method_name, event_dict):
    """structlog processor — replace sensitive values by '[REDACTED]'."""
    for key in list(event_dict.keys()):
        if key.lower() in _SENSITIVE_KEYS:
            event_dict[key] = "[REDACTED]"
        elif isinstance(event_dict[key], dict):
            event_dict[key] = _recurse_redact(event_dict[key])
    return event_dict
```

Plug-in dans `server/logging_config.py::_build_processors` **avant** le renderer final, après `merge_contextvars`. Append-only modification : V2.0.5 reste valide.

### Conventions logs auth

| Event structlog | Niveau | Champs supplémentaires |
|-----------------|--------|------------------------|
| `auth.register.success` | info | `user_id`, `email_hash` |
| `auth.register.rejected` | warning | `email_hash`, `reason: "invalid_token"\|"email_taken"\|"weak_password"` |
| `auth.login.success` | info | `user_id`, `email_hash` |
| `auth.login.failure` | warning | `email_hash`, `reason: "unknown_user"\|"wrong_password"\|"locked"` |
| `auth.refresh.success` | info | `user_id`, `old_jti`, `new_jti` |
| `auth.refresh.replay_attempt` | error | `email_hash`, `revoked_jti` (alarme breach potentielle) |
| `auth.logout.success` | info | `user_id`, `jti` |
| `auth.argon2.bench` | info | `wall_ms` (au boot) |

**Aucun de ces logs ne contient le password ni le token plain** — la redaction est belt-and-braces.

## Livrables

- [ ] `requirements.txt` : `argon2-cffi>=23.1`, `PyJWT>=2.8.0`
- [ ] `alembic/versions/0004_auth_foundation.py` :
  - `CREATE EXTENSION IF NOT EXISTS citext`
  - Tables `users`, `refresh_tokens`, `auth_events`
  - Add `user_id UUID NULL` + index sur 22 tables santé
  - Backfill legacy default user si applicable
- [ ] `server/db/models.py` : SQLAlchemy models `User`, `RefreshToken`, `AuthEvent` + `user_id` Mapped col sur 22 tables existantes
- [ ] `server/security/auth.py` :
  - `hash_password()`, `verify_password()`, `_DUMMY_HASH`
  - `create_access_token()`, `create_refresh_token()`, `decode_access_token()`, `decode_refresh_token()`
  - `rotate_refresh_token()`, `revoke_refresh_token()`
  - `_validate_jwt_secret_at_boot()`, `_validate_registration_token()`
  - `get_current_user()` dependency
- [ ] `server/security/redaction.py` : structlog processor `redact_sensitive_keys`
- [ ] `server/logging_config.py` : plug `redact_sensitive_keys` dans `_build_processors`
- [ ] `server/routers/auth.py` : `register`, `login`, `refresh`, `logout`
- [ ] `server/routers/{sleep,heartrate,steps,exercise,mood}.py` : ajout `Depends(get_current_user)` + filtering `user_id` sur SELECT/INSERT
- [ ] `server/main.py` : `app.include_router(auth_router)`, `_validate_jwt_secret_at_boot()` dans lifespan, `_validate_registration_token()` (warning si absent), bench argon2
- [ ] `server/middleware/request_context.py` : bind `user_id_var` dans `get_current_user` au lieu de None par défaut (modif minimale du V2.0.5 — append, pas rewrite)
- [ ] `.env.example` : `SAMSUNGHEALTH_JWT_SECRET=<32-bytes-base64>`, `SAMSUNGHEALTH_JWT_SECRET_PREVIOUS=`, `SAMSUNGHEALTH_REGISTRATION_TOKEN=<32-chars-random>`
- [ ] `tests/server/conftest.py` : fixtures `auth_user`, `auth_client` (TestClient avec header Authorization pré-rempli), `secondary_user` (pour tests isolation), env vars JWT/registration set par autouse
- [ ] Tests : 6 fichiers pour ~50 tests RED → GREEN
- [ ] `README.md` : section "Auth setup" (env vars requises, génération secret/token, post-migration legacy user)
- [ ] `NOTES.md` : checklist V2.3.1 (verification email, reset password, lockout enforcement, rate limit, CAPTCHA)
- [ ] `HISTORY.md` : entry changelog

## Tests d'acceptation

### Argon2

1. **argon2id** — `TestArgon2Params.test_hash_uses_argon2id` : `hash_password("x")` → string commence par `$argon2id$`
2. **RFC9106 profile #2** — `TestArgon2Params.test_hash_params_match_rfc9106_profile2` : params encodés dans le hash matchent `m=46080,t=2,p=1`
3. **Verify correct** — `TestVerifyPassword.test_verify_correct_password` : `verify_password("x", hash_password("x"))` → True
4. **Verify wrong** — `TestVerifyPassword.test_verify_wrong_password_returns_false` : `verify_password("y", hash_password("x"))` → False
5. **Dummy hash on missing user** — `TestTimingEqualization.test_verify_nonexistent_user_runs_dummy_hash` : login `unknown@x.com` → `_DUMMY_HASH` est invoqué (mock spy sur verify_password call count = 1)
6. **Constant-time login** — `TestTimingEqualization.test_verify_constant_time_within_tolerance` : login user inconnu vs login user connu mauvais password → ratio wall_ms < 1.5 (10 runs each, median)

### JWT

7. **Boot validation présence** — `TestJwtSecretValidation.test_jwt_secret_required_at_boot` : env var unset → boot raise
8. **Boot validation longueur** — `test_jwt_secret_min_256_bits` : secret 16 chars ASCII → boot raise
9. **Boot rejet trivial** — `test_jwt_secret_rejects_changeme` : secret `"changeme"` → boot raise
10. **kid header** — `TestAccessToken.test_create_access_token_includes_kid` : `jwt.get_unverified_header(token)["kid"] == "v1"`
11. **Reject alg=none** — `TestJwtDecodeFootguns.test_decode_rejects_alg_none` : token forgé avec alg=none → `decode_access_token` raise
12. **Algorithms explicit** — `test_decode_requires_explicit_algorithms` : code source `server/security/auth.py` grep `jwt.decode(.*algorithms=`. Match obligatoire.
13. **Required claims** — `test_decode_requires_exp_iat_sub_claims` : token sans `exp` → raise
14. **iss/aud validation** — `test_decode_rejects_missing_iss_aud` : token sans `iss` → raise
15. **No PII in payload** — `test_payload_contains_only_sub_no_pii` : decode → payload keys ⊆ {sub, iat, exp, iss, aud, typ}, pas d'email
16. **Previous secret decode-only** — `TestRefreshToken.test_previous_secret_decode_only` : token signé avec PREVIOUS → decode OK, sign avec PREVIOUS impossible (sign utilise toujours current)

### Auth routes

17. **Register exige token** — `TestRegister.test_register_requires_admin_token` : POST sans header → 403
18. **Register success** — `test_register_creates_user_returns_201` : POST avec token + email/password → 201 + row users + auth_events `register` row
19. **Register doublon** — `test_register_duplicate_email_409` : 2× POST même email → 2e = 409
20. **Register token invalide** — `test_register_invalid_token_403` : header `X-Registration-Token: wrong` → 403
21. **Login OK** — `TestLogin.test_login_returns_access_and_refresh` : POST credentials valides → 200 avec access_token + refresh_token
22. **Login wrong password** — `test_login_wrong_password_401_identical_message` : 401 + `{"detail": "invalid_credentials"}`
23. **Login unknown user** — `test_login_unknown_user_401_identical_message` : 401 + body **identique au point 22**
24. **Login timing** — `test_login_response_time_constant_within_tolerance` : 10 runs unknown vs 10 runs wrong-pwd → median ratio ∈ [0.7, 1.5]
25. **Refresh rotation** — `TestRefresh.test_refresh_rotates_token` : POST refresh → ancien JTI marqué revoked + nouveau JTI émis ; ancien token réutilisé → 401 + log `auth.refresh.replay_attempt`
26. **Refresh révoqué** — `test_refresh_revoked_token_401` : token déjà revoked → 401
27. **Logout révoque** — `TestLogout.test_logout_revokes_refresh` : POST logout → refresh non-utilisable
28. **Logout idempotent** — `test_logout_idempotent` : 2× logout même refresh → 204 les deux fois (pas d'erreur)

### Health routes auth

29-33. **Sleep/HR/steps/exercise/mood GET sans token** : tous → 401
34. **Sleep POST sans token** : 401
35. **Isolation users sleep** — `TestUserDataIsolation.test_user_a_cannot_read_user_b_sleep` : user A POST sleep, user B GET → liste vide (filtering `user_id`), pas 200 avec data de A
36. **Isolation users mood** : idem mood
37. **POST associates with caller** — `test_user_a_post_associates_with_user_a_id` : user A POST → row.user_id = A.id en DB

### Redaction

38-44. **Redaction processor** : password/token/authorization/jwt/secret/cookie remplacés `[REDACTED]`, case-insensitive, dict imbriqué OK, clés non-sensibles préservées

### Auth events

45. **login.success écrit auth_event** — `TestAuthEventLogging.test_login_success_writes_event` : login → row dans auth_events avec `event_type="login_success"` + `user_id` + `email_hash`
46. **login.failure : email_hash pas plain** — `test_login_failure_writes_event_with_email_hash_not_plain` : login échoué → row avec `email_hash = sha256(email)`, **pas** d'email plain dans aucune colonne
47. **register écrit event** : 1 row `event_type="register"`
48. **refresh écrit event** : 1 row `event_type="refresh"`

### Suite globale

49. `pytest tests/` ≥ 296 tests GREEN (248 baseline post-V2.0.5 + ~48 nouveaux), 0 régression

## Out of scope V2.3 (explicit)

- Email verification flow (différé V2.3.1)
- Reset password flow (V2.3.1)
- Email SMTP réel — tous les events email-like en V2.3.1 = log structlog uniquement
- Google OAuth (V2.3.2)
- Rate limiting login (V2.3.3)
- Frontend Nightfall login form (V2.3.3)
- Lockout enforcement automatique sur `failed_login_count > N` — la colonne existe en V2.3 mais pas le check (V2.3.3)
- CAPTCHA register
- Migration `user_id NOT NULL` sur tables santé (V2.3.0.1 immédiate post-V2.3 backfill validation)
- 2FA / TOTP

## Suite naturelle

- **V2.3.0.1** (clean-up immédiat post-merge V2.3, ~30min) : migration alembic 0005 passant `user_id NOT NULL` sur les 22 tables santé (après backfill validé)
- **V2.3.1** : reset password + email verification (table `verification_tokens`, email = log only en attendant SMTP)
- **V2.3.2** : Google OAuth (provider abstraction `AuthProvider`)
- **V2.3.3** : rate limiting login (slowapi + redis OR in-memory bucket) + lockout enforcement + frontend Nightfall login form
