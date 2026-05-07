# History

## Features

| Feature | Files | Commit |
|---------|-------|--------|
| Foundation docs — VISION.md + CLAUDE.md | `VISION.md`, `CLAUDE.md` | [`523a981`](#2026-05-06-523a981) |
| Agent migration Phase 2+3 — suppressions locaux obsolètes + vision-keeper projet | `.claude/agents/vision-keeper.md`, `.claude/skills/vision/SKILL.md`, `agents/contracts/vision_keeper.py` | [`pending`](#pending) |
| Phase 6 — MVP CI/CD VPS perso — Dockerfile multi-stage + GHCR + deploy-dev + deploy-prod (auto-rollback) + /healthz /readyz + requirements.lock + ci.yml security gates (pip-audit, gitleaks, docker-build smoke). Pentester ACCEPT_WITH_CAVEATS — 5 HIGH levés + 5 D décisions + 4 issues résiduelles (#issues). | `Dockerfile`, `docker-compose.prod.yml`, `.github/workflows/{ci,deploy-dev,deploy-prod}.yml`, `server/routers/health.py`, `server/main.py`, `tests/server/test_healthz.py`, `requirements.{in,lock}`, `.env.prod.example` | [`9b825b1`](#2026-04-30-9b825b1) |
| Phase 3 — RGPD endpoints `/me/{export,erase,audit-log}` (Art. 15/17/20) — 2-step re-auth, cascade applicatif explicit 21 tables santé, anonymisation `auth_events` (HIGH 2), race lock `SELECT FOR UPDATE` (HIGH 4), OAuth-only nonce, filter `admin_*`, filename générique, atomic UPDATE...RETURNING, purpose CHECK enum, audit_event helper meta cap 4KB. Pentester verdict WARN tracé issue #22 (closed by PR). | `server/routers/me.py`, `server/security/{rgpd,audit}.py`, `server/db/models.py`, `server/main.py`, `alembic/versions/0009_phase3_rgpd_audit_meta_purpose_check.py`, `tests/server/test_me_{export,erase,audit_log}.py`, `.github/ISSUE_TEMPLATE/pentester-review.yml`, `NOTES.md` | [`94bdfca`](#2026-04-30-94bdfca) |
| V2.3.3.3 — Auth finitions (Inter font + 4 pages admin UI + dashboard rebrand --ds-* + content-negotiation + last_login_ip HMAC + CSRF admin + trusted-types) | `static/admin/`, `static/assets/fonts/Inter-VariableFont_wght.ttf`, `static/css/admin.css`, `static/js/{admin,admin-auth,ds-colors}.js`, `static/dashboard.css`, `static/index.html`, `server/routers/admin.py`, `server/middleware/security_headers.py`, `server/security/rate_limit.py` | [`cab3ecb`](#2026-04-28-cab3ecb) |
| V2.3.3.2 — Frontend Nightfall (9 pages auth + theme switcher + rebrand Data Saillance) + security headers globaux + cookies httpOnly refresh + CSRF Sec-Fetch-Site | `static/auth/`, `static/css/`, `static/js/`, `static/assets/`, `server/middleware/security_headers.py`, `server/security/csrf.py`, `server/routers/static_pages.py`, `server/routers/{auth,auth_oauth}.py`, `server/main.py` | [`0b098a7`](#2026-04-27-0b098a7) |
| V2.3.3.1 — Rate-limit slowapi (multi-decorator IP composite + cap pur-IP) + soft backoff exponentiel (anti-DoS lockout) + admin lock/unlock + IP right-most-untrusted + email global cap | `server/security/rate_limit.py`, `server/security/rate_limit_storage.py`, `server/security/lockout.py`, `server/middleware/rate_limit_context.py`, `server/middleware/slowapi_pre_auth.py`, `alembic/versions/0008_users_last_failed_login.py`, `server/routers/{auth,auth_oauth,admin,sleep,heartrate,steps,exercise,mood}.py` | [`c119976`](#2026-04-27-c119976) |
| V2.3.2 — Google OAuth via AuthProvider abstraction (state CSRF + nonce, JWKS hardcoded, raw_claims whitelist 8 keys, return_to validator strict, deferred linking via oauth_link_confirm) | `alembic/versions/0007_identity_providers.py`, `server/security/auth_providers/`, `server/routers/auth_oauth.py`, `server/db/models.py`, `server/security/auth.py`, `server/security/redaction.py`, `server/routers/auth.py`, `server/main.py` | [`10c682c`](#2026-04-26-10c682c) |
| V2.3.1 — Password reset + email verification (dual-sink admin endpoint, 1h/24h TTL split, blocklist top-100, atomic audit) | `alembic/versions/0006_verification_tokens.py`, `server/security/passwords.py`, `server/security/email_outbound.py`, `server/security/auth.py`, `server/routers/auth.py`, `server/routers/admin.py` | [`83f77fd`](#2026-04-26-83f77fd) |
| V2.3.0.1 — `user_id NOT NULL` cleanup + scripts CSV multi-user | `alembic/versions/0005_user_id_not_null.py`, `server/db/models.py`, `scripts/import_samsung_csv.py`, `scripts/generate_sample.py` | [`08101d1`](#2026-04-26-08101d1) |
| V2.3 — Auth foundation atomique (users + JWT access+refresh + multi-user FK + redaction + audit) | `server/security/auth.py`, `server/security/redaction.py`, `server/routers/auth.py`, `server/db/models.py`, `alembic/versions/0004_auth_foundation.py` | [`e32801a`](#2026-04-26-e32801a) |
| V2.0.5 — structlog observability foundation (JSONL + request_id middleware) | `server/logging_config.py`, `server/middleware/request_context.py`, `server/main.py`, `requirements.txt` | [`f2c8cb2`](#2026-04-26-f2c8cb2) |
| Samsung Health CSV import — full DB schema (21 tables) | `server/database.py`, `scripts/import_samsung_csv.py`, `scripts/explore_samsung_export.py` | [`d032741`](#2026-04-21-d032741) |
| Dev mobile — WSL2 port forwarding + Android cleartext | `scripts/dev-mobile.ps1`, `Makefile`, `android-app/` | [`646aeaa`](#2026-04-21-646aeaa) |
| Nightfall sleep dashboard | `static/index.html`, `static/dashboard.css`, `static/dashboard.js`, `static/api.js` | [`b5cacc7`](#2026-04-21-b5cacc7) |
| Workflow bootstrap — CI, labels, hooks, tests | `.github/`, `.githooks/`, `Makefile`, `tests/` | [`939f5ef`](#2026-04-21-939f5ef) |
| Phase 3: Steps, heart rate, exercise + tabbed dashboard | `server/`, `static/`, `scripts/`, `android-app/` | [`242040a`](#2026-02-16-242040a) |
| Phase 2: Sleep stages + color-coded calendar + Android app | `server/`, `static/`, `scripts/`, `android-app/` | [`8d5cfb0`](#2026-02-16-8d5cfb0) |
| Phase 1: Backend + DB + UI + Scripts | `server/`, `static/`, `scripts/`, `requirements.txt` | [`6200a93`](#2026-02-16-6200a93) |
| Project scaffolding | `.gitignore`, `README.md`, `NOTES.md`, `HISTORY.md`, `ROADMAP.md` | [`6cc83dc`](#2026-02-16-6cc83dc) |
| Phase 4 Android Shell | `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/NightfallTheme.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBar.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/data/auth/TokenDataStore.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/data/network/BackendUrlStore.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/di/NetworkModule.kt`, `android-app/app/build.gradle.kts` | [`7a71b2b`](#2026-05-07-7a71b2b) |
| Phase 4 Android Auth Screens | `android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/auth/AuthViewModel.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/LoginScreen.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/RegisterScreen.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreen.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/AuthCallbackScreen.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/AuthModels.kt` | [`e89d409`](#2026-05-07-e89d409) |
| Phase 4 Android Import SAF | `android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/import_/ImportViewModel.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepository.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepositoryImpl.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/CountingRequestBody.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportUiState.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/import_/ImportScreen.kt` | [`4dcf071`](#2026-05-07-4dcf071) |
| Phase 4 Android WebView Bridge | `android-app/app/src/main/java/fr/datasaillance/nightfall/webview/NightfallWebViewClient.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/webview/NightfallJsInterface.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/webview/WebViewScreen.kt`, `android-app/app/src/main/java/fr/datasaillance/nightfall/data/settings/SettingsDataStore.kt` | [`pending`](#pending-webview-bridge) |

---

## Changelog

### 2026-05-07 `pending-webview-bridge`
feat(android): Phase 4 WebView Bridge — NightfallWebViewClient, NightfallJsInterface, WebViewScreen, SettingsDataStore
- NightfallWebViewClient.kt : WebViewClient avec shouldOverrideUrlLoading par URI parsing scheme+host+port (anti-homograph attack TA-WV-02) — CustomTabsIntent + FLAG_ACTIVITY_NEW_TASK pour externe ; onReceivedSslError handler.cancel() jamais proceed() (TA-WV-04) ; onReceivedHttpError 401/403 main frame → clearToken() + onLogout() (TA-WV-03) ; onPageFinished → injectAuth() (JSONObject.quote escaping) + injectTheme() (TA-WV-05/06)
- NightfallJsInterface.kt : 3 @JavascriptInterface exactement — getAppVersion() → BuildConfig.VERSION_NAME, openImport() → Handler(mainLooper).post, onNightfallEvent(String) log-only avec guard 4096 chars (anti-DoS)
- WebViewScreen.kt : AndroidView WebView — javaScriptEnabled, domStorageEnabled, setAllowFileAccess(false), setAllowContentAccess(false), MIXED_CONTENT_NEVER_ALLOW ; modifier.fillMaxSize() pour occuper tout l'espace parent ; backendOrigin calculé depuis SettingsDataStore via URI parsing
- SettingsDataStore.kt : EncryptedSharedPreferences "nightfall_settings_prefs" (distinct de TokenDataStore D10) ; Robolectric guard "nightfall_test_settings_prefs" (plain prefs en JVM) ; getBackendUrl/setBackendUrl avec validation http://|https:// ; getThemePreference/setThemePreference avec validation enum {system,dark,light}
- AppModule.kt : provideSettingsDataStore(context) ajouté
- AndroidManifest.xml : <queries> block https+http pour Chrome Custom Tabs API 30+
- SleepScreen/TrendsScreen/ActivityScreen (webview flavor) : wiring WebViewScreen avec tokenDataStore + settingsDataStore + onLogout
- 34 tests RED → GREEN (NightfallWebViewClientTest 12, NightfallJsInterfaceTest 8, SettingsDataStoreTest 14) ; 97/97 total GREEN

### 2026-05-07 `4dcf071`
feat: android: Phase 4 import SAF — ImportViewModel, CountingRequestBody, ImportRepository, ImportScreen stepper
- ImportUiState.kt : sealed class Idle/Connecting/ConnectionFailed/Connected/Selecting/Uploading/Success/Error
- ImportDataType.kt : enum SLEEP/HEART_RATE/STEPS/EXERCISE avec samsungFilenamePrefix, apiPath, labelRes, iconRes
- ImportResult.kt : data class par type (inserted, skipped, errorMessage?)
- CountingRequestBody.kt : OkHttp RequestBody avec CountingSink — onProgress(0f) appelé en début de writeTo (≥2 appels garantis pour petits payloads < segment OkIO)
- ImportRepository.kt : interface mockable ; CsvEntry(uri, size) ; uploadCsv @Throws(IOException)
- ImportRepositoryImpl.kt : extractCsvEntries via ZipInputStream avec ZIP bomb protection (200 Mo / 100 entrées max, IOException Archive trop grande) ; uploadCsv streamé via CountingRequestBody + dispatch Retrofit par type
- ImportViewModel.kt : plain ViewModel (no Hilt — issue #52) ; checkConnection() set Connecting AVANT viewModelScope.launch (StandardTestDispatcher) ; startUpload() catch CancellationException re-throw + catch Exception par type pour succès partiel
- ImportScreen.kt : SAF launcher rememberLauncherForActivityResult(OpenDocumentTree) → viewModel.startUpload ; ConnectedContent avec RgpdNoticeCard (surfaceVariant + border primary 4dp + AnnotatedString SemiBold URL) + bouton Sélectionner dossier ; collectAsStateWithLifecycle
- ImportStep.kt : sealed class Connection/Selection/Upload pour stepper visuel
- NightfallApi.kt : 4 endpoints @Multipart @POST ajoutés (importSleep/importHeartRate/importSteps/importExercise) + ImportApiResponse @Serializable
- NavGraph.kt : route import construisant ImportViewModel(ImportRepositoryImpl(api))
- build.gradle.kts : androidx.lifecycle:lifecycle-runtime-compose:2.8.7
- 16 tests RED → GREEN (ImportViewModelTest 9 + CountingRequestBodyTest 7) ; 63/63 total GREEN
- Spec p4-android-import.md : status ready, tested_by peuplé, backend endpoints hors scope Android PR

### 2026-05-07 `e89d409`
feat: android: Phase 4 auth — AuthViewModel, LoginScreen, RegisterScreen, ForgotPasswordScreen, OAuth Custom Tabs, accessibility
- AuthViewModel (plain ViewModel, no Hilt — issue #52) : login/register/requestPasswordReset/storeTokenFromCallback/setLoginError ; Loading state synchrone avant viewModelScope.launch ; mapHttpError 401/403/409/400
- AuthUiState.kt : sealed classes LoginUiState / RegisterUiState / ForgotPasswordUiState (Idle/Loading/Success/Error + Sent pour forgot) ; ForgotPasswordUiState.Error intentionnellement mort (anti-enum)
- LoginScreen : AuthTextField email+password avec testTag field_email/field_password + contentDescription ; AuthPrimaryButton Se connecter testTag btn_login ; OutlinedButton Google OAuth (border Cyan500) testTag btn_google_oauth ; TextButton Mot de passe oublié testTag link_forgot_password
- RegisterScreen : paramètre registrationToken: String? = null (injectable depuis Settings) ; validation inline mots de passe
- ForgotPasswordScreen : formulaire remplacé par confirmation anti-enum sur ForgotPasswordUiState.Sent
- AuthCallbackScreen : deep link nightfall://auth/callback ; setLoginError(Authentification Google échouée) + onFailure() si token absent
- AuthTextField : toggle visibilité password (Icons.Filled.Visibility/VisibilityOff, contentDescription Afficher/Masquer)
- AuthPrimaryButton : CircularProgressIndicator avec contentDescription Chargement en cours
- NightfallApi : 4 endpoints auth ajoutés (login, register, requestPasswordReset, googleStart)
- NetworkModule : CookieJar inline in-memory (JavaNetCookieJar non dispo sans okhttp-urlconnection)
- AndroidManifest : intent-filter deep link nightfall://auth/callback autoVerify=true
- build.gradle.kts : androidx.browser:browser:1.8.0 (Custom Tabs)
- 20 tests RED → GREEN (AuthViewModelTest 11 + LoginScreenTest 4 + RegisterScreenTest 3 + ForgotPasswordScreenTest 2) ; 47/47 total GREEN
- Spec p4-android-auth.md : status ready, tested_by peuplé, D7 CookieJar note, AuthModels package note, ForgotPasswordUiState.Error note, Cairo → Inter/Playfair Display

### 2026-05-07 `021ab8e`
fix: android: Inter + Playfair Display — remplace Cairo (non DS), fonts bundlées depuis static/assets/fonts
- Cairo n'est pas dans le système typographique DataSaillance — remplacé par Inter (body/UI) + Playfair Display (titres) conformément à Vectorizer/design-system/typography.md
- Variable fonts copiées depuis static/assets/fonts/ vers android-app/app/src/main/res/font/ (inter_variable.ttf + playfair_display_variable.ttf, licence OFL)
- FontLoadingStrategy.OptionalLocal : fallback système si R.font ne peut pas être résolu (Paparazzi JVM renderer) — évite le crash, production non affectée
- Spec p4-android-shell.md corrigée : D8 Cairo → Playfair Display + Inter, livrables et contrainte C4 mis à jour

### 2026-05-07 `7a71b2b`
feat: android: Phase 4 shell — fr.datasaillance.nightfall, navigation, theme, EncryptedSharedPreferences, configurable backend URL
- Clean rewrite of android-app/ from com.samsunghealth → fr.datasaillance.nightfall (7 legacy files deleted)
- NightfallTheme: Material3 dark/light color schemes with DataSaillance tokens (teal #0E9EB0, amber #D37C04, cyan #07BCD3)
- NavGraph: Scaffold + NavHost with 7 destinations (login, sleep, trends, activity, profile, import, settings); startDestination conditional on hasToken
- BottomNavBar: 4-tab NavigationBar (Sommeil, Tendances, Activité, Profil) + ProfileScreen with Paramètres nav entry
- TokenDataStore: EncryptedSharedPreferences AES256-GCM; Robolectric-only plaintext path guarded by Build.FINGERPRINT check (unreachable in production — spec C2)
- BackendUrlStore: EncryptedSharedPreferences-backed configurable backend URL, defaults to BuildConfig.DEFAULT_BACKEND_URL (http://10.0.2.2:8001 for debug)
- NetworkModule.provideRetrofit() reads base URL from BackendUrlStore; SettingsScreen exposes URL editing UI
- Build: namespace fr.datasaillance.nightfall, versionName 4.0.0, flavors webview+native (rendering dimension), security-crypto + timber + okhttp-logging deps added
- 27/27 unit tests GREEN (Paparazzi, Robolectric, MockWebServer); Hilt deferred → issue #52 (Kotlin 2.x kapt incompatibility)

### 2026-05-06 `523a981`
chore(project): CLAUDE.md — stack, contraintes C1/C2/C3, design DataSaillance, skill chain
- Crée CLAUDE.md projet : stack FastAPI/SQLAlchemy/pytest, key files, dev commands
- Contraintes non-négociables (local-first, RGPD Art.9, sécurité pentester bloquant, design DataSaillance, no LLM)
- Skill invocation chain `/spec → /vision → /tdd → /impl → /review → /commit`
- 4 questions ouvertes (fenêtre circadienne, multi-user, Phase B, webapp unifiée)

### 2026-05-06 `88c1d5e`
docs(vision): VISION.md — principe fondateur, design DataSaillance, contraintes
- Crée VISION.md depuis 99 messages utilisateurs extraits de 4 sessions JSONL
- Principe fondateur : découverte Non-24 par la dataviz (15 ans non-diagnostiqué)
- Contraintes hard C1/C2/C3, vocabulaire (drift/dette/régularité/radial clock/stacked timeline)
- Design system DataSaillance : tokens warm paper, accent rust, Cairo, light+dark obligatoires
- Phases de dev (0–3 ✅, 6 ✅, 4–5 en attente), questions ouvertes

---

## Checkpoint

### 2026-04-30 `checkpoint-pre-merge-dev-2026-05-01`
chore(checkpoint): safety tag before merge origin/dev into feat/phase6-cicd-mvp
- Reason: merge 10-file conflict — HISTORY.md, NOTES.md, Makefile, .githooks/pre-push, .gitignore, static/{dashboard.css,index.html}, .github/workflows/ci.yml, scripts/generate_sample.py, server/routers/sleep.py
- Scope: feat/phase6-cicd-mvp HEAD (commit 9340872) — stratégie B validée : --ours pour fichiers trivial + difficile, fusion manuelle pour Makefile/NOTES.md/HISTORY.md
- [CHECKPOINT]

### `checkpoint-before-v2-refactor-2026-04-23` — 2026-04-23 → `5343c9b`
chore(checkpoint): safety tag before V2 full security/RGPD refactor
- Reason: Major refactor — migration SQLite → Postgres, multi-user auth (email+pwd + Google OAuth), chiffrement AES-256-GCM, endpoints RGPD, Android Compose shell + dual-track UI (WebView dev / Compose natif prod), logging structuré sharded, docker-compose dev/prod
- Scope: tout le repo — `server/`, `android-app/`, `static/`, `tests/`, `alembic/` (nouveau), `docker-compose*.yml` (nouveau), CI workflows
- Target branch : `refactor/v2` long-lived, target des PRs de la refonte
- Anchor: `5343c9b feat(data): Samsung Health CSV import pipeline (#5)` — origin/main HEAD au moment de la bifurcation
- Plan de référence : `vault/02_Projects/SamsungHealth/specs/2026-04-23-plan-v2-refactor-master.md`
- Plan multi-agents : `vault/02_Projects/SamsungHealth/specs/2026-04-23-plan-v2-multi-agents-architecture.md`
- Estimation : ~6-8 semaines, livraison incrémentale (V1 = WebView, V2 = Compose natif)
- [CHECKPOINT]

### `nightfall-loom-v1` — 2026-04-23 → `ee09025`
chore(release-archive): tag état de l'app au moment de l'enregistrement loom
- Scope: `static/dashboard.js`, `static/dashboard.css`, `static/index.html`, `static/api.js`
- Branche source : `feat/nightfall-history` (PR #6 closed sans merge le 2026-04-23 — refonte V2 priorisée)
- Differs from `nightfall-dataviz-v1` par les commits de polish loom :
  - Period filter bar (7j/30j/full) + radial sous l'hypnogram (cc50998, d146174)
  - Stage tooltips riches sur timeline et radial clock (c776c96)
  - Bed/wake times prominents au-dessus des KPIs hypnogram (551a17a)
  - Mobile spacing épuré (4a71955, a2f22d1)
  - Perf partial-render donut + labels donut + period 6m default (ee09025)
- Codex : `vault/02_Projects/SamsungHealth/codex/release/nightfall-loom-v1.md`
- Pour ressortir l'état : `git checkout nightfall-loom-v1 && make dev`
- [CHECKPOINT]

### `nightfall-dataviz-v1` — 2026-04-22 → `1631c80`
[CHECKPOINT] Reference tag for all Nightfall dataviz chapters before refactor.
- Branche source : `feat/nightfall-history` (archivée, PR #6 closed)
- Scope: `static/dashboard.js`, `static/dashboard.css`
- Chapters included: heatmap (01), stacked timeline (02), hypnogram (03), radial (04), small multiples (05), ridgeline (06), top nights cards (07), agenda (08), metrics + month selector + 4 full-history views (09), elasticity / duration regularity (10), drift clock with animated playback (11)
- Key features: gap-based episode grouping, personal median sleep target, lag-1 autocorrelation, cycle count, bedtime scatter 24h, drift clock demo mode + smooth interpolation

---

## Changelog

### 2026-05-04 `ad6a08a`
chore(ci): add .gitleaks.toml allowlist (tests + vault doc mirror)
- Ajoute `.gitleaks.toml` avec `useDefault = true` — exclut `tests/` et `docs/vault/code/` (fixtures fake, jamais utilisées en production)
- Supprime les faux positifs gitleaks sur `_TEST_JWT_SECRET` dans les tests

### 2026-04-30 `edcfad2`
fix(ci): set fetch-depth: 0 on security job checkout for gitleaks diff scan
- Résout l'erreur gitleaks `ambiguous argument` causée par un shallow clone (depth=1) empêchant le diff de commits sur le job `Security gates`.

### 2026-04-30 `1c35bf7`
chore(lint): remove unused imports flagged by ruff (F401)
- `server/middleware/rate_limit_context.py` — supprime `ASGIApp`
- `server/middleware/security_headers.py` — supprime `ASGIApp`
- `server/middleware/slowapi_pre_auth.py` — supprime `Response`
- `server/routers/admin.py` — supprime `JSONResponse` (consolide from-import)
- `server/routers/auth.py` — supprime `jwt`, `_outbound_link_cache`
- `server/routers/auth_oauth.py` — supprime `Any`, `Header`, `status`, `hash_verification_token`, `google_mod`, `_GOOGLE_ERROR_MAP`, `_outbound_link_cache` (consolide from-imports)
- `server/routers/mood.py` — supprime `Any`, `BaseModel` (consolide from-import)

### 2026-04-30 `23ec1c1`
docs(phase6): README section Déploiement + NOTES ADR-3 VPS vs PaaS + spec symbols sync
- **README** : nouvelle section `## Déploiement` (~190 lignes) — vue d'ensemble CI/CD, architecture réseau VPS (diagram ASCII Caddy → Docker), snippet Caddy block `/readyz` IP externe, prérequis VPS (SSH keys, known_hosts, env secrets GH), variables d'env prod documentées, procédures deploy-dev / deploy-prod (workflow_dispatch), healthz + readyz exemples curl, guide backup PG manuel, smoke test local Docker.
- **NOTES.md** : ADR-3 "VPS perso vs PaaS" — justification coût/maîtrise/learning + non-choix PaaS + conséquences ops. Checklist hardening VPS avant 1er déploiement dev + prod.
- **Spec** : sync `implements` symbols (Dockerfile `[builder, runner]`, deploy workflows `[build, deploy]` / `[deploy]`, `.env.prod.example`).

### 2026-04-30 `9b825b1`
feat(phase6): MVP CI/CD VPS — Dockerfile + GHCR + 2 deploy workflows + /healthz /readyz + requirements.lock
- **Spec** : `docs/vault/specs/2026-04-30-phase6-cicd-mvp.md` (post-pentester reconcile, 5 HIGH + 5 D + risques significatifs intégrés). Baseline pytest : 22 passed + 1 skipped.
- **`Dockerfile`** (NEW) — multi-stage `builder` (pip + uv compile) + `runner` non-root `appuser:1001`, HEALTHCHECK urllib `/healthz`, `COPY --chown`, entrypoint direct `uvicorn` port 8001.
- **`docker-compose.prod.yml`** (NEW) — services `web` + `postgres` (réseau interne), `env_file: .env.prod`, bind port `8001:8001`, volumes PG nommé, restart `unless-stopped`.
- **`.github/workflows/deploy-dev.yml`** (NEW) — trigger `push:main`, build + push GHCR `dev-<sha>`, SSH VPS dev (`VPS_SSH_KEY_DEV` scoped env), `docker compose pull && up -d`, smoke `/healthz`, auto-rollback `PREV` si smoke fail.
- **`.github/workflows/deploy-prod.yml`** (NEW) — trigger `workflow_dispatch` + approval GitHub environment `production`, build + push GHCR `prod-<sha>`, SSH VPS prod (`VPS_SSH_KEY_PROD`), même pattern smoke + rollback.
- **`ci.yml`** : +2 jobs — `security` (pip-audit SCA + gitleaks secrets scan) ; `docker-build` (build no-push + smoke run container avec secrets éphémères générés runtime). `paths-ignore` étendu à `docs/vault/**`.
- **`server/routers/health.py`** (NEW) — `/healthz` (200 `{"status":"ok"}`) + `/readyz` (DB ping `SELECT 1` + alembic head check, 200 ou 503 `{"status":"degraded","checks":{"db":"…","migrations":"…"}}`).
- **`server/main.py`** — `app.include_router(health_router.router)`.
- **`tests/server/test_healthz.py`** (NEW) — 3 tests : healthz OK, readyz OK (mock alembic), readyz degraded DB down.
- **`tests/server/conftest.py`** — `test_healthz.py` ajouté à `_NO_AUTO_AUTH_FILES` (probes publics sans Bearer).
- **`requirements.in`** (NEW) — deps runtime déclarées (uv compile source).
- **`requirements.lock`** (NEW) — lockfile complet généré par `uv pip compile requirements.in`.
- **`.env.prod.example`** (NEW) — template variables prod avec instructions génération (clé AES-256, JWT secret, salt HMAC).
- **`.dockerignore`** (NEW) — exclut `.git`, `__pycache__`, `*.pyc`, `.env*`, `tests/`, `docs/`, `android-app/`.
- **`.github/known_hosts`** (NEW) — empreintes SSH VPS pré-vérifiées (anti TOFU, HIGH-1 pentester).

### 2026-04-30 `94bdfca`
feat(Phase 3): RGPD endpoints `/me/{export,erase,audit-log}` — Art. 15 (accès) / 17 (effacement) / 20 (portabilité) (#23)
- **Spec** : `docs/vault/specs/2026-04-28-phase3-rgpd-endpoints.md` (status: ready, 39 acceptance tests, 3 fichiers tests). Pentester reviewé pré-TDD → verdict `WARN` 4 HIGH + 3 décisions design + 5 risques additionnels — tous adressés. Trace audit publique : issue [#22](https://github.com/LyonMoncef/SamsungHealth/issues/22) (closed by PR).
- **5 endpoints** sur `/me/*` (auth Bearer required, scope = current_user) :
   - `POST /me/export/request` — re-auth password OU oauth_nonce → export_token TTL 5min single-use (purpose `account_export_confirm`). Rate-limit 5/h/user.
   - `GET /me/export/confirm?export_token=&full=` — consume + stream ZIP via `SpooledTemporaryFile(max_size=10MB)`. Filename générique `export_my_data_<date>.zip` (pas de user_id leak). Cache-Control no-store.
   - `POST /me/erase/request` — re-auth → erase_token TTL 5min purpose `account_erase_confirm`.
   - `POST /me/erase/confirm` → 204. Cascade applicatif explicit sur les 21 tables santé + identity_providers + refresh_tokens + verification_tokens, anonymisation auth_events (4 cols NULL), DELETE users.
   - `GET /me/audit-log` — paginated (limit/offset, total global). Filter `admin_*` par défaut (`include_admin=False`). 60/h/user.
- **`server/security/rgpd.py`** (NEW) — 21 `HEALTH_TABLES` + 7 Pydantic models + 9 helpers :
   - `build_user_export_zip(db, user, full_audit)` — **HIGH 4** : `SELECT FOR UPDATE` users.id AVANT toute access tables santé (bloque erase concurrent). ZIP : manifest.json + user.json (sans `password_hash`) + identity_providers.json + auth_events.json + 21 × csv+json (déchiffrement V2.2.1 transparent ORM).
   - `erase_user_cascade(db, user_id) -> EraseStats` — **HIGH 3** : ordre strict pentester (lock users → audit BEFORE delete → 21 health tables → 3 auth tables → anonymize auth_events → DELETE users). `sleep_stages` special-case (FK `session_id` cascade).
   - `_anonymize_auth_events(db, user_id)` — **HIGH 2** : UPDATE 4 cols NULL (`user_id`, `email_hash`, `ip_hash`, `user_agent`).
   - `_safe_audit_event(db, event_type, user_id, meta)` — **HIGH 2** : skip silencieux si user.id n'existe plus (anti re-création `email_hash` post-erase). Utilisé par TOUS les audit calls Phase 3.
   - `_consume_verification_token_atomic(db, user, raw, purpose)` — **LOW** : UPDATE...RETURNING anti-race single-use (consumed_at IS NULL AND expires_at > now() AND purpose match).
   - `_verify_reauth(db, user, password, oauth_nonce)` — argon2.verify (rejette OAUTH_SENTINEL) ; OAuth-only path via `_verify_oauth_nonce` (stub mockable test). Soft backoff V2.3.3.1 sur fail.
   - `_create_verification_token` — révoque tokens actifs précédents (`uq_verification_tokens_active_per_purpose`) avant insert.
   - `_ip_hmac(request)` — réutilise V2.3.3.1 helper.
- **`server/security/audit.py`** (NEW) — `audit_event(db, event_type, user_id, ip_hash, user_agent, meta)` helper centralisé (V2.3 patterns inlined `AuthEvent(...)` direct, ce helper consolide). Cap meta 4KB côté insertion : si `len(json.dumps(meta).encode("utf-8")) > 4096` → tronque + `meta["truncated"] = True`. **MED**.
- **`server/routers/me.py`** (NEW) — 5 routes async avec `@limiter.limit` (slowapi requiert `request: Request, response: Response` — pattern V2.3 réutilisé). CSRF `Sec-Fetch-Site` sur tous les POST. Audit events : `rgpd.{export.requested,export.downloaded,erase.requested,erase.confirmed,audit_log.read}`.
- **`alembic/versions/0009_phase3_rgpd_audit_meta_purpose_check.py`** (NEW) — `auth_events.{ip_hash TEXT NULL, meta JSONB NULL}` cols + `verification_tokens.purpose` CHECK enum whitelist (5 valeurs : `email_verification`, `password_reset`, `oauth_link_confirm`, `account_export_confirm`, `account_erase_confirm`). **MED** anti cross-purpose token reuse au DB level.
- **`server/db/models.py`** — +2 cols sur `AuthEvent` (`ip_hash`, `meta`) pour mirror la migration.
- **`server/main.py`** — `app.include_router(me.router)`.
- **Tests Phase 3** : 57/57 GREEN.
   - `test_me_export.py` (13 tests) — re-auth 401/wrong-pwd/OK/oauth-only ; confirm invalid/replay/zip ; ZIP structure 21 tables ; user.json no password_hash ; mood déchiffré ; user isolation ; rate-limit 5/h ; race export↔erase white-box (mock `db.execute` spy sur `SELECT FOR UPDATE`).
   - `test_me_erase.py` (34 tests, parametrize 21 cascade) — preconditions ; cascade param par table ; identity_providers/refresh_tokens/verification_tokens ; users row ; auth_events anonymized 4 cols NULL ; audit `rgpd.erase.confirmed` survives anonymized ; expired/wrong-purpose tokens 400 ; replay ; OAuth-only ; `_safe_audit_event` no-op post-erase ; atomic single-use UPDATE...RETURNING.
   - `test_me_audit_log.py` (10 tests) — 401, payload shape, scope user, filter `admin_*` par défaut + include_admin=true, pagination, ordering DESC, ip_hash 16-hex, meta cap 4KB, rate-limit 60/h.
- **Conftest patch** — `test_me_{export,erase,audit_log}.py` ajoutés à `_NO_AUTO_AUTH_FILES` (gestion Bearer manuelle pour 401-sans-token + multi-user isolation).
- **Test_alembic_0008.py patch** — pin `downgrade base + upgrade 1b4c5d6e7f83` au lieu de `upgrade head` (pattern projet 0006/0007). Évite régression à chaque nouvelle migration.
- **Workflow** : `.github/ISSUE_TEMPLATE/pentester-review.yml` (NEW) + ADR-2 dans `NOTES.md` formalisent le pattern : à chaque pentester review d'une spec → issue GitHub label `pentester-review` → PR ferme via `Closes #N`. Trace native indexable (`gh issue list --label pentester-review --state all`). Pattern à appliquer Phase 4 / Phase 6 / futures.
- **Différé hors scope Phase 3** : compression bomb fallback (cap 50MB hit → export par-table chunks différé Phase 3+) ; bulk admin erase ; export incremental.

### 2026-04-28 `cab3ecb`
feat(V2.3.3.3): auth finitions — Inter font + page admin UI + dashboard rebrand + 3 fixes pentester (CSRF admin, last_login_ip HMAC, cache-control)
- `docs/vault/specs/2026-04-28-v2.3.3.3-auth-finitions.md` créé (status: ready, 28+ acceptance tests, 6 fichiers tests backend / 53 tests). Patch v2 post-pentester avec 3 HIGH bloquants : (1) CSRF check `Sec-Fetch-Site` étendu admin (lock/unlock/probe), (2) `last_login_ip` HMAC dans `UserSummary` Pydantic (jamais brut), (3) Cache-Control no-store étendu `/admin/*`.
- Inter font bundle local (~340KB Variable TTF + OFL.txt) — fallback `-apple-system` n'est plus utilisé. `@font-face` dans `ds-tokens.css` avec `font-display: swap` (pas de FOUT bloquant).
- `server/routers/admin.py` — Pydantic `UserSummary` (avec `last_login_ip_hash` HMAC, jamais brut) + `UserDetail` (sanitize `request_id` field abuse pour event_type ∈ {admin_user_locked, admin_user_unlocked} → expose `meta.reason` + `meta.duration_minutes`). 3 nouveaux endpoints : `GET /admin/users` (list, audit `admin_users_list`), `GET /admin/users/{user_id}` (detail, audit `admin_user_detail_read`), `POST /admin/probe` (sentinel admin token validation). Helper `_check_admin_token_async` avec `asyncio.sleep(0.5)` constant + audit fail à chaque 401. `check_sec_fetch_site` ajouté à `lock_user`/`unlock_user`/`admin_probe`. Content-negotiation `_wants_html` : Accept text/html explicit → page HTML, sinon JSON.
- `server/middleware/security_headers.py` — `CSP_ADMIN_PAGES = CSP_AUTH_PAGES + 'require-trusted-types-for script; trusted-types default;'` pour `/admin/*` + `/static/admin/*`. `_cache_control_for` étendu `/admin/*` → no-store.
- `server/security/rate_limit.py` — `_ip_hash` exposé public pour réutilisation par `UserSummary`.
- Frontend admin :
   - `static/admin/login.html` — form simple + autocomplete=off, label "Token admin" (jamais "X-Registration-Token" pour ne pas leak nom env var)
   - `static/admin/users.html` — table users avec actions lock/unlock par row + filter client-side
   - `static/admin/user-detail.html` — card user info + identity_providers + recent_events timeline
   - `static/admin/pending-verifications.html` — cards verify_links copiables + auto-refresh 30s avec Page Visibility API pause (pas de polling si onglet caché)
   - `static/css/admin.css` (NEW) — layout admin (table, cards, status badges)
   - `static/js/admin.js` (NEW) — fetch wrapper avec X-Registration-Token, render avec `.textContent` only (CSP trusted-types compatible)
   - `static/js/admin-auth.js` (NEW) — login flow + sessionStorage admin_token + logout clear
   - `static/js/ds-colors.js` (NEW) — `dsColor(token)` helper avec sanitizer regex (fallback `#000000` si pas color value)
- Dashboard rebrand :
   - `static/dashboard.css` — migration `#hex` accent literals → `var(--ds-*)` tokens (--ds-accent-warm, --ds-accent-cool, etc.)
   - `static/index.html` — ajout `<link>` ds-tokens.css avant dashboard.css + theme toggle button + `<script>` theme.js
- 6 fichiers tests V2.3.3.3 (53 tests RED → GREEN) : `test_admin_pages.py` (22), `test_admin_users_endpoint.py` (12), `test_admin_csrf.py` (4), `test_admin_probe.py` (3), `test_inter_font_bundle.py` (3), `test_dashboard_rebrand.py` (5+9 paramétrés).
- Patch test mineur : `test_admin_pages.py` envoie `Accept: text/html` explicit (cohérent content-negotiation backend).
- `.gitattributes` (NEW) : `*.ttf binary` (anti-corruption Git CRLF).
- **373 passed + 1 skipped** suite complète V2.3 → V2.3.3.3.

### 2026-04-27 `0b098a7`
feat(V2.3.3.2): frontend Nightfall — 9 pages auth + theme switcher Data Saillance + security headers globaux + refresh cookie httpOnly + CSRF Sec-Fetch-Site
- `docs/vault/specs/2026-04-27-v2.3.3.2-frontend-nightfall.md` créé (status: ready, 45 acceptance tests dont 23 backend exécutables, 22 Playwright frontend reportés à manuel/V2.3.3.3). Patch v2 post-pentester avec 3 HIGH bloquants traités : (1) `oauth-callback.html` page intermédiaire ambigüe → backend redirige 302 vers `/auth/oauth-success#fragment` ou `/auth/oauth-error?code=` ou `/auth/oauth-link-pending`, (2) CSP `connect-src` Google endpoints inutile → restreint à `'self'`, (3) Security headers globaux (X-Frame-Options DENY, nosniff, Referrer-Policy, Permissions-Policy, HSTS conditionnel HTTPS) au lieu de juste pages auth.
- `server/middleware/security_headers.py` (NEW) — middleware ASGI GLOBAL : security headers + strip server/via headers + CSP différencié par path (`/auth/*` strict no unsafe-inline, `/api/*` minimal `default-src 'none'`, dashboard `'unsafe-inline'` toléré pour D3).
- `server/security/csrf.py` (NEW) — `check_sec_fetch_site(request)` raise 403 si `Sec-Fetch-Site in (cross-site, same-site)`. Appliqué sur 6 endpoints POST auth (login/register/refresh/reset/verify/oauth-link/google-start).
- `server/routers/static_pages.py` (NEW) — 9 endpoints GET pour pages auth (login, register, reset-request, reset-confirm, verify-email, oauth-link-confirm, oauth-success, oauth-error, oauth-link-pending) avec `Cache-Control: no-store`.
- `server/routers/auth.py` — `login` set Set-Cookie `refresh_token` httpOnly+Secure(prod)+SameSite=Strict+Path=/auth/refresh+30j. `refresh` lit body **explicit > implicit cookie** (back-compat tests V2.3 et clients legacy Android). `logout` delete_cookie. `register` + reset/verify request : check_sec_fetch_site.
- `server/routers/auth_oauth.py` — `google_callback` retourne `RedirectResponse(302)` selon scénario : success → `/auth/oauth-success#access_token=X&return_to=Y` + cookie httpOnly refresh, link pending → `/auth/oauth-link-pending`, error → `/auth/oauth-error?code=<mapped>` (whitelist stricte, **jamais** error_description). `oauth_link_confirm` symétrique.
- `server/main.py` — `app.include_router(static_pages.router)` AVANT auth router (priorité GET pages), `app.mount("/static", StaticFiles)`, `app.add_middleware(SecurityHeadersMiddleware)` global.
- **Frontend bundle Data Saillance** :
   - 9 pages HTML : structure minimale, no inline style/script, `<meta name="referrer" content="no-referrer">` sur reset/verify/oauth-link-confirm/oauth-success, autocomplete corrects, type=password sur admin token field, logo via `<picture><img>` pas inline SVG, `<a target="_blank" rel="noopener noreferrer">`.
   - 2 CSS : `ds-tokens.css` (--ds-* light + dark via `[data-theme]`, type scale base 16 ratio 1.25, spacing scale 4px) + `auth.css` (layout pages auth).
   - 3 JS : `theme.js` (FOUC prevention sync set data-theme avant body, toggle persist localStorage, listen prefers-color-scheme), `api-client.js` (fetch wrapper credentials:include, handle 429/423/401), `auth-form.js` (validation client-side, FLASH_PARAMS whitelist stricte `{registered, expired, verified, reset}`, history.replaceState après lecture token URL, `.textContent` only jamais `.innerHTML =`).
   - Bundle assets : Playfair Display TTF variable + OFL.txt (depuis `~/MyPersonalProjects/Vectorizer/`), 3 logos SVG (dark, white-fond-blanc, favicon).
- 3 fichiers tests V2.3.3.2 (52 tests RED → GREEN) : `test_static_auth_pages.py` (38), `test_csrf_check.py` (9), `test_refresh_cookie.py` (5/6).
- **Tests Playwright frontend** : reportés (Playwright pas installé). Tests visuels manuels pour V2.3.3.2.
- **320 passed + 1 skipped** (V2.3 timing soft backoff trade-off préservé). Suite complète V2.3 → V2.3.3.2.

### 2026-04-27 `c119976`
feat(V2.3.3.1): rate-limit slowapi + soft backoff lockout + admin lock/unlock + right-most-untrusted IP — 4 HIGH pentester intégrés
- `docs/vault/specs/2026-04-26-v2.3.3.1-rate-limit-lockout.md` créé (status: ready, 44 acceptance tests, 7 fichiers tests / 47 tests). Patch v2 post-pentester avec 4 HIGH bloquants traités : (1) hard lockout DoS-able remplacé par soft backoff exponentiel (1s..60s, pas de hard lock auto), (2) IP spoofing via XFF first-IP remplacé par right-most-untrusted, (3) lost-update failed_login_count remplacé par UPDATE atomic RETURNING, (4) TRUSTED_PROXIES obligatoire en prod (`SAMSUNGHEALTH_ENV=production` + vide → boot fail).
- `alembic/versions/0008_users_last_failed_login.py` — revision `1b4c5d6e7f83`, parent `0a3b4c5d6e72`. Ajout colonne `users.last_failed_login_at TIMESTAMPTZ NULL` (pour cleanup_stale_failed_login_counts cron).
- `server/security/rate_limit.py` (NEW) — `_resolve_client_ip` right-most-untrusted, key_funcs (`_pure_ip_key`, `_login_composite_key`, `_refresh_composite_key`, `_email_composite_key`, `_user_id_key`), `limiter` slowapi avec `LruMemoryStorage`, `_rate_limit_exceeded_handler` (jitter +0-5s, headers conditionnels prod/dev), `_validate_trusted_proxies_at_boot`, `audit_rate_limit_exceeded` (sample 1/10 + HMAC IP).
- `server/security/rate_limit_storage.py` (NEW) — `LruMemoryStorage` subclasse `MemoryStorage` slowapi avec cap LRU 100_000 entries (anti-OOM via key cardinality, pentester H2).
- `server/security/lockout.py` (NEW) — `register_failed_login` (UPDATE atomic + soft delay exponentiel `min(2^count, 60)s`), `register_successful_login` (race-guard sur admin lock), `is_user_locked` (admin manuel only), `cleanup_stale_failed_login_counts` (cron 24h reset).
- `server/middleware/rate_limit_context.py` + `server/middleware/slowapi_pre_auth.py` (NEW) — middleware leur permettant au rate-limit de catch entrant AVANT auth deps (pentester HIGH H1 : middleware order critical).
- `server/routers/auth.py` — décorateurs multi-key sur `login` (5/min composite + 30/min pur-IP) + `register/refresh/logout/verify/reset` selon table V2.3.3.1 limites par endpoint. Soft backoff intégré dans login wrong_password. Cap pur-email global 60/jour sur reset/verify request (anti mail-bombing distribué). Jitter 80-120ms préservé sur reset/verify (anti-énum V2.3.1). Auto-unlock user sur reset password confirm (audit `password_reset_unlocked_user`).
- `server/routers/auth_oauth.py` — décorateurs sur `start/callback/oauth-link/confirm` + cap fail séparé sur callback (5/min). OAuth callback failures n'incrémentent PAS `failed_login_count`. OAuth callback success sur dual-auth user → reset compteur.
- `server/routers/admin.py` — endpoints `POST /admin/users/{user_id}/lock` (body duration_minutes + reason, audit `admin_user_locked`) + `POST /admin/users/{user_id}/unlock` (audit `admin_user_unlocked`). X-Registration-Token gated.
- `server/routers/sleep|heartrate|steps|exercise|mood.py` — décorateur `@limiter.limit("1000/hour", key_func=_user_id_key)` sur POST endpoints uniquement (data poisoning self-DoS cap, pentester #1).
- `server/main.py` — wiring `SlowAPIMiddleware` + exception_handler + lifespan invoque `_validate_trusted_proxies_at_boot`. Middleware order : `SlowAPIPreAuthMiddleware` AVANT `RequestContextMiddleware` (FastAPI LIFO).
- `server/db/models.py` — ajout `last_failed_login_at: Mapped[datetime | None]` sur `User`.
- `requirements.txt` — `slowapi>=0.1.9,<1.0` (pinned).
- `tests/server/conftest.py` — env defaults `SAMSUNGHEALTH_ENV=test`, `SAMSUNGHEALTH_TRUSTED_PROXIES=127.0.0.1,::1`, fixture autouse `_reset_rate_limit_state` (clear bucket entre tests), 7 nouveaux fichiers test ajoutés à `_NO_AUTO_AUTH_FILES`.
- 7 fichiers tests V2.3.3.1 (47 tests).
- Patch mécanique : `tests/server/test_alembic_0007.py` — pre-condition pinned à rev `0a3b4c5d6e72` (avant : "head" qui pointe maintenant 0008, cassé mécaniquement). Aucune logique modifiée.
- **Skip explicite (trade-off documenté)** : `test_login_response_time_constant_within_tolerance` (V2.3 #24) skipped — le soft backoff brise volontairement l'égalité de timing wrong_password vs unknown_email. Anti-énum dégradé documenté dans la spec section "Anti-énumération".
- **268 passed + 1 skipped + 47 nouveaux V2.3.3.1 = 269/269 effectifs**, exit 0.

### 2026-04-26 `10c682c`
feat(V2.3.2): Google OAuth via AuthProvider abstraction — state CSRF + nonce, deferred linking, JWKS hardcoded, raw_claims whitelist
- `docs/vault/specs/2026-04-26-v2.3.2-google-oauth.md` créé (status: ready, 45 acceptance tests). Patch v2 post-pentester (4 HIGH bloquants + 4 MED durcissements + 3 ajouts intégrés).
- `alembic/versions/0007_identity_providers.py` — revision `0a3b4c5d6e72`, parent `9d2e3f5a6b71`. Crée table `identity_providers` (id UUID7 PK, user_id FK CASCADE, provider TEXT, provider_sub TEXT, provider_email TEXT lowercase, email_verified BOOL, linked_at/last_used_at TIMESTAMPTZ, raw_claims JSONB) + 2 unique constraints `(provider, provider_sub)` et `(user_id, provider)`. Ajoute aussi colonne `payload jsonb NULL` à `verification_tokens` (pour le purpose `oauth_link_confirm`).
- `server/security/auth_providers/__init__.py` (NEW) — `AuthProvider` ABC + `ProviderProfile` Pydantic + `AuthProviderError`.
- `server/security/auth_providers/state.py` (NEW) — state JWT HS256 self-contained + cache mémoire single-use TTL 10 min, **LRU cap 10_000** (anti-DoS), boot warning si `SAMSUNGHEALTH_DEPLOYMENT_INSTANCES > 1`.
- `server/security/auth_providers/google.py` (NEW) — `GoogleAuthProvider`, JWKS endpoint **hardcoded** (anti-SSRF, JAMAIS dérivé du iss claim), TTL `min(Cache-Control, 4h)` (cap pentester #5), `_RAW_CLAIMS_WHITELIST = {sub, email, email_verified, iss, aud, iat, exp, jti}` (RGPD minimisation), `_GOOGLE_ERROR_MAP` table 11 codes, validation env Google au boot.
- `server/security/auth_providers/return_to.py` (NEW) — `validate_return_to()` algo strict 7 règles (urlsplit, scheme `https|http`, refus userinfo `@`, IDN punycode, exact origin match, fragment stripped). 8 bypass classiques bloqués.
- `server/security/auth.py` — `OAUTH_SENTINEL = "OAUTH_ONLY_NO_PASSWORD_LOGIN"` constante, `verify_password` court-circuit si stored_hash == sentinel (jitter 80-120ms, return False sans appeler argon2 qui raise sur format invalide).
- `server/security/redaction.py` — `_SENSITIVE_KEYS` étendu avec `code`, `state`, `id_token`, `nonce`, `error_description`.
- `server/routers/auth_oauth.py` (NEW) — `POST /auth/google/start` (CSRF protection via `Sec-Fetch-Site` check, JSON response avec `authorize_url` jamais 302), `GET /auth/google/callback` (réponse JSON avec tokens, jamais URL fragment — anti-pattern OAuth implicit éliminé), `POST /auth/oauth-link/confirm`. Account linking matrix révisée : **JAMAIS d'auto-link sur email match** → flow deferred via `verification_tokens` purpose=`oauth_link_confirm` (TTL 1h). Auto-register gated par `SAMSUNGHEALTH_OAUTH_AUTO_REGISTER` (default false). Race condition fix : transaction unique `INSERT users ON CONFLICT (LOWER(email)) DO NOTHING RETURNING id` + fallback SELECT.
- `server/routers/auth.py` — `request_password_reset` étendu : si `user.password_hash == OAUTH_SENTINEL` → 202 silencieux, aucun token créé (les OAuth-only n'ont pas de password à reset).
- `server/db/models.py` — `class IdentityProvider(Uuid7PkMixin, Base)` + colonne `payload: Mapped[dict | None]` ajoutée à `VerificationToken`.
- `server/main.py` — wiring router `auth_oauth`, lifespan invoque `_validate_google_oauth_env_at_boot()` + warning `oauth.state_cache.multi_instance_unsafe` si `SAMSUNGHEALTH_DEPLOYMENT_INSTANCES > 1`.
- `tests/server/conftest.py` — env defaults Google OAuth + `_NO_AUTO_AUTH_FILES` étendu, fixture `google_keypair_and_jwks` (RSA + JWKS + sign helper pour mock Google), autouse `_reset_oauth_caches` fixture (isolation des caches module-level state/JWKS entre tests).
- `requirements.txt` — `httpx`, `PyJWT[crypto]` (déjà présent V2.3, juste extra crypto).
- 8 fichiers tests V2.3.2 (53 tests). **222/222 PASS** (V2.3 + V2.3.0.1 + V2.3.1 + V2.3.2).
- Patch mécanique : `tests/server/test_alembic_0006.py` — pre-condition pinned à rev `9d2e3f5a6b71` (avant : "head" qui pointe maintenant 0007, cassé mécaniquement par l'ajout 0007). Aucune logique modifiée.

### 2026-04-26 `83f77fd`
feat(V2.3.1): password reset + email verification — dual-sink admin endpoint, TTL 1h/24h split, blocklist top-100, atomic audit
- `docs/vault/specs/2026-04-26-v2.3.1-reset-password-email-verify.md` créé (status: ready, 25 acceptance tests, 9 test files / 46 tests). Patch post-pentester : 2 HIGH (verify_link out of logs + dual-sink admin endpoint, host header injection fix), 5 MED (TTL split, race condition + flip-back triggers, blocklist passwords, atomic audit transaction, HMAC email salt).
- `alembic/versions/0006_verification_tokens.py` — revision `9d2e3f5a6b71`, parent `8c1d2e4f5a90`. Crée table `verification_tokens` (id UUID7 PK, user_id FK CASCADE, token_hash UNIQUE TEXT, purpose TEXT, issued_at/expires_at/consumed_at TIMESTAMPTZ, ip INET, user_agent TEXT) + check constraint `consumed_after_issued` + index unique partiel `uq_verification_tokens_active_per_purpose WHERE consumed_at IS NULL` (race condition fix) + fonction PL/pgSQL `verification_tokens_no_unconsume()` + trigger `BEFORE UPDATE` anti-flip-back.
- `server/security/passwords.py` (NEW) — `_PASSWORD_BLOCKLIST` (104 entrées top-leaked ≥12 chars), `WeakPasswordError`, `validate_password_strength()` partagée register V2.3 + reset V2.3.1.
- `server/security/email_outbound.py` (NEW) — `_outbound_link_cache` thread-safe TTL 60s (eviction lazy), `hmac_email_hash(email)` HMAC-SHA256 avec `SAMSUNGHEALTH_EMAIL_HASH_SALT` (64 chars hex, infaisable dictionary), `send_verification_email`/`send_password_reset_email` log structlog `event="email.outbound"` SANS verify_link.
- `server/security/auth.py` — `TTL_PASSWORD_RESET=1h`, `TTL_EMAIL_VERIFICATION=24h`, `generate_verification_token()` (32 bytes secrets.token_urlsafe → 43 chars + sha256 hex), `hash_verification_token`, `verify_verification_token`, `_validate_public_base_url_at_boot` (env doit start `https://` ou `http://localhost`, jamais Host header), `_validate_email_hash_salt_at_boot` (≥ 32 bytes).
- `server/routers/auth.py` — 4 endpoints `/auth/verify-email/{request,confirm}` + `/auth/password/reset/{request,confirm}`. Anti-énum : jitter `random.uniform(0.080, 0.120)` AVANT le retour 202, `_DUMMY_TOKEN_OPS` si email inconnu. Re-request révoque l'ancien token (consumed_at + retry-with-revoke sur IntegrityError race). `/auth/password/reset/confirm` = transaction atomique unique (validate_password_strength → 400 si KO + token NON consommé, hash_password, UPDATE users + revoke ALL refresh_tokens user, UPDATE consumed_at, INSERT auth_events — fail audit = rollback complet). Login étendu pour gate dynamique `SAMSUNGHEALTH_REQUIRE_EMAIL_VERIFICATION` (default false → warning structlog `auth.login.unverified_email`).
- `server/routers/admin.py` (NEW) — `GET /admin/pending-verifications` exigeant `X-Registration-Token` (réutilise `check_registration_token` V2.3, stop-gap admin role). Retourne tokens actifs avec verify_link reconstruit (cache 60s) + INSERT auth_events `admin_pending_verifications_read`.
- `server/main.py` — `app.include_router(admin.router)`, lifespan invoque les 2 nouveaux validateurs au boot.
- `server/db/models.py` — ajout `class VerificationToken(Uuid7PkMixin, Base)`.
- `tests/server/conftest.py` — defaults env vars test (`SAMSUNGHEALTH_PUBLIC_BASE_URL`, `SAMSUNGHEALTH_EMAIL_HASH_SALT`), `_NO_AUTO_AUTH_FILES` étendu aux 9 nouveaux test files, autouse `_expire_sibling_sessions_on_commit` (cross-session identity-map coherence pour les tests qui ont 2 SA Session bind même engine sans expire_on_commit).
- 9 fichiers de tests (46 tests V2.3.1) — tous GREEN. Suite complète : **169/169 PASS** (V2.3 + V2.3.1).

### 2026-04-26 `08101d1`
chore(V2.3.0.1): user_id NOT NULL sur 21 tables santé + scripts CSV multi-user
- `alembic/versions/0005_user_id_not_null.py` créé (revision `8c1d2e4f5a90`, parent `7a3b9c0e1d24`) — safety check raise si rows orphelines, ALTER COLUMN SET NOT NULL sur 21 tables, drop des index partiels `WHERE user_id IS NULL`. Downgrade restaure l'état nullable + index partiel.
- `server/db/models.py` — 21 occurrences de `user_id: Mapped[UUID | None]` + `nullable=True` → `Mapped[UUID]` + `nullable=False`. RefreshToken déjà NOT NULL, AuthEvent reste nullable (login_failure sur email inexistant).
- `scripts/import_samsung_csv.py` — argparse `--user-email` (default `legacy@samsunghealth.local`), helper `_resolve_target_user_id`, `_upsert` injecte `user_id=TARGET_USER_ID` côté serveur, drop `index_where=text("user_id IS NULL")`, l'unique constraint matchée est désormais `(user_id, ...cols)`.
- `scripts/generate_sample.py` — argparse `--user-email`, propagation `user_id` aux 5 call sites (sleep + stages + steps + heart_rate + exercise), drop des `index_where`.
- `alembic/versions/0004_auth_foundation.py` — fix bug downgrade : remplace `op.drop_constraint(uq_name, ...)` par `op.execute("DROP INDEX IF EXISTS ...")` (l'upgrade créait un INDEX partiel, pas un constraint, donc drop_constraint échouait). Drop aussi `uq_<table>_user_window` créé par l'upgrade.
- `tests/server/conftest.py` — fixture `default_user_db` + helper `_ensure_orm_default_user` (insertion idempotente raw SQL, évite argon2 200ms par test) + autouse hook `_auto_inject_user_id_for_health_inserts` (event `before_insert` SQLAlchemy qui injecte un user_id par défaut si absent — évite de patcher 30+ inserts ORM dans les tests existants).
- `tests/server/test_crypto_foundation.py` + `tests/server/test_scripts_csv_import.py` — adaptation aux nouvelles signatures CLI argparse + injection `TARGET_USER_ID` via le fixture `csv_export_dir`.
- 123 tests verts (`pytest tests/server/`).

### 2026-04-26 `2beb3c7`
chore(design): référence rebrand Data Saillance (palette + typo + logos, dark + light) pour le produit Nightfall
- `docs/vault/specs/2026-04-26-nightfall-rebrand-data-saillance.md` créé (type=reference) — palette canonique HEX, type scale Playfair Display + Inter, 5 variantes logo, double-stroke "halo plat" dark, tokens CSS vars `--ds-*`, convention `MaterialTheme` Compose, règles d'usage. Source of truth externe : `~/MyPersonalProjects/Vectorizer/`. Light mode obligatoire (Nightfall v1 était dark-only)
- `docs/vault/specs/nightfall-fullspectrum-design-brief.md` : `status: ready → superseded`, `superseded_by: 2026-04-26-nightfall-rebrand-data-saillance`, `superseded_on: 2026-04-26`
- `docs/vault/specs/2026-04-23-plan-v2-refactor-master.md` : Phase 4 + Phase 5 patchées avec note "voir spec rebrand Data Saillance, référencement obligatoire dans frontmatter `related_specs:` des futures specs frontend"
- Convention : `code-cartographer` rendra le link automatique ; `plan-keeper` flaggera comme déviation toute spec frontend qui ne référence pas cette ref
- Aucun changement code : pure documentation/référence design

### 2026-04-26 `e32801a`
feat(V2.3): auth foundation atomique — users + JWT access+refresh + multi-user FK + redaction + audit
- **Atomique non-splittable** (imposé audit pentester) : 50 tests RED → GREEN dans une seule PR pour éviter brèche Art.9 RGPD entre split partiel
- `server/security/auth.py` : argon2id RFC 9106 profile #2 (m=46MB, t=2, p=1), PyJWT HS256 (`kid: "v1"`, `algorithms=["HS256"]` strict, `require=[exp,iat,sub,iss,aud,typ]`, validation iss/aud, `_DUMMY_HASH` pour timing equalization, `SAMSUNGHEALTH_JWT_SECRET_PREVIOUS` decode-only pour rotation), `get_current_user` dependency
- `server/security/redaction.py` : structlog processor `redact_sensitive_keys` (password, token, authorization, jwt, secret, cookie + nested + case-insensitive)
- `server/routers/auth.py` : `POST /auth/{register,login,refresh,logout}` — register admin-gated via `X-Registration-Token` (constant-time compare), refresh rotation avec détection replay (log `auth.refresh.replay_attempt` ERROR), logout idempotent, audit table `auth_events` (sha256 email_hash, jamais email plain)
- `server/db/models.py` : nouveaux models `User` (CITEXT email, failed_login_count, locked_until, last_login_at/ip, password_changed_at, is_active, email_verified_at TIMESTAMPTZ), `RefreshToken` (jti UNIQUE, replaced_by FK, revoked_at), `AuthEvent` ; `user_id UUID NULL FK` ajouté sur 21 tables santé
- `alembic/versions/0004_auth_foundation.py` : `CREATE EXTENSION citext`, schémas auth, partial unique index `WHERE user_id IS NULL` pour back-compat scripts CSV legacy + nouveau unique sur `(user_id, ...)` pour multi-user, backfill conditionnel d'un user `legacy@samsunghealth.local` (is_active=false) si données legacy détectées
- `server/routers/{sleep,heartrate,steps,exercise,mood}.py` : `Depends(get_current_user)` partout, filtering `where(<Model>.user_id == current_user.id)` sur SELECT, injection serveur `user_id=current_user.id` sur INSERT (jamais accepté du body)
- `server/main.py` : validation JWT secret au boot (présence + 256 bits + reject "changeme/secret/test/password/default" + Shannon entropy ≥ 4.0), bench argon2 au boot (`auth.argon2.bench` info)
- `server/logging_config.py` : plug `redact_sensitive_keys` après `merge_contextvars`
- `tests/server/conftest.py` : `_set_auth_env_defaults` autouse pour env vars JWT/registration ; `client_pg_ready` enrichi pour auto-auth des tests existants (avec opt-out par fichier pour les tests V2.3 auth nus)
- `scripts/{import_samsung_csv,generate_sample}.py` : `index_where=text("user_id IS NULL")` ajouté aux ON CONFLICT pour préserver idempotence avec partial index
- 50 tests RED → GREEN dans 6 fichiers (`test_password_hashing`, `test_jwt`, `test_auth_routes`, `test_health_routes_auth`, `test_redaction`, `test_auth_events`) — total **298/298 GREEN**, 0 régression
- Dépendances : `argon2-cffi>=23.1`, `PyJWT>=2.8.0`
- Spec : `docs/vault/specs/2026-04-26-v2-auth-foundation.md` (status delivered)
- Hors scope V2.3 (différés) : V2.3.0.1 `user_id NOT NULL`, V2.3.1 reset password + email verification, V2.3.2 Google OAuth, V2.3.3 rate limiting + lockout enforcement + frontend Nightfall login

### 2026-04-26 `f2c8cb2`
feat(V2.0.5): structlog observability foundation — JSONL logs + request_id middleware
- `server/logging_config.py` créé : `configure_logging()` + `get_logger()` + chaîne de processors structlog (timestamp ISO8601 UTC, level, logger scope, contextvars merger), JSONRenderer en prod / ConsoleRenderer(colors) en dev
- `server/middleware/request_context.py` créé : ASGI pure middleware, `request_id_var` + `user_id_var` ContextVars, sanitisation X-Request-ID (alnum+tirets, max 64), header in/out, log `request.complete` avec `latency_ms` (perf_counter) + `route` template + niveau INFO/WARNING/ERROR selon status
- `server/main.py` : `configure_logging()` dans lifespan, `RequestContextMiddleware` mounté avant routers
- 7 fichiers `server/{database,security/crypto,routers/*}.py` : `_log = get_logger(__name__)` ajouté pour V2.3+ auth events
- Env vars : `APP_ENV` (prod|dev) + `LOG_LEVEL` (DEBUG/INFO/WARNING/ERROR/CRITICAL avec fallback INFO si invalide)
- 13 tests RED → GREEN (`tests/server/test_logging_config.py` 6 tests, `tests/server/test_request_context_middleware.py` 7 tests) — total 248/248 GREEN, 0 régression
- Dépendance ajoutée : `structlog>=24.1`
- README section "Logs" + NOTES.md tech debt "PII scrubber automatique (V2.3+)"
- Spec : `docs/vault/specs/2026-04-26-v2-structlog-observability.md` (status delivered)
- Hors scope V2.0.5 : Caddy reverse proxy, CLI logq, OpenTelemetry, PII scrubber auto, migration scripts/* (différés V2.0.6+)

### 2026-04-26 `7135a5f`
chore(audit): snapshot V2 progress 2026-04-26 + fix V2.1.1 status delivered
- `docs/vault/audits/2026-04-26-v2-progress-audit.md` créé (nouveau dossier `audits/` pour les snapshots roadmap, distincts des `specs/`)
- Bilan : 11 PRs mergées, 235/235 tests GREEN, **3 phases V2 entamées** (P0 ~70%, P2 ~50%, Phase A 100%), 4 phases à 0% (P1 auth, P3 RGPD, P4 Android, P5 Compose Canvas)
- Anomalie corrigée : spec `2026-04-24-v2-postgres-routers-cutover.md` `status: ready → delivered`, `delivered: null → 2026-04-24` (PR #8 mergée le 2026-04-24, statut frontmatter pas mis à jour à l'époque)

### 2026-04-24 V2.2.1 — étendre chiffrement aux 9 tables Art.9 restantes

`729c3b1` spec V2.2.1 (12 tests d'acceptation, 9 sentinelles BYTEA + 2 round-trip critiques + 1 sentinelle _crypto_v default).

`28e2c32` test(v2.2.1): 12 tests RED — `TestSentinelleBytea` (1 par table : sleep_sessions, weight, blood_pressure, stress, spo2, heart_rate_hourly, respiratory_rate, skin_temperature, ecg) + `TestRoundTripCritique` (sleep_score Int + weight_kg Float + crypto_v default).

`d7e47d8` feat(v2.2.1): chiffrement étendu → 12/12 V2.2.1 GREEN, 235 suite globale.
- `server/db/encrypted.py` : ajout `EncryptedFloat` `TypeDecorator` (sérialisation `repr(float).encode('ascii')` pour préserver IEEE 754, `float(decoded)` au déchiffrement, accepte int en input)
- `server/db/models.py` : 9 models patchés en série mécanique — pour chaque colonne Art.9 : `Mapped[type | None] = mapped_column(EncryptedInt|EncryptedFloat)` + colonne `<col>_crypto_v: Mapped[int] = mapped_column(Integer, NOT NULL, server_default="1")`. Total : 33 colonnes chiffrées + 33 `_crypto_v`
  - `sleep_sessions` 7 cols : sleep_score/efficiency/sleep_duration_min/sleep_cycle/mental_recovery/physical_recovery/sleep_type
  - `weight` 7 cols : weight_kg/body_fat_pct/skeletal_muscle_pct/skeletal_muscle_mass_kg/fat_free_mass_kg/basal_metabolic_rate/total_body_water_kg
  - `blood_pressure` 4 cols : systolic/diastolic/pulse/mean_bp
  - `stress` 1 col : score
  - `spo2` 4 cols : spo2/min_spo2/max_spo2/low_duration_s
  - `heart_rate_hourly` 3 cols : min_bpm/max_bpm/avg_bpm (NOT NULL conservé)
  - `respiratory_rate` 3 cols : average/lower_limit/upper_limit
  - `skin_temperature` 3 cols : temperature/min_temp/max_temp
  - `ecg` 2 cols : mean_heart_rate/classification (sample_frequency/sample_count restent en clair — métadonnées non Art.9)
- `alembic/versions/0003_encrypt_remaining_art9.py` : migration unique pour les 9 tables (~370 lignes, autogenerated puis sed `postgresql_using='NULL'` sur tous les `alter_column`). Fresh DB requise (heart_rate_hourly.min_bpm NOT NULL → USING NULL fail si rows existantes)
- Pattern transparent confirmé : aucun changement aux routers, aux scripts d'import, aux tests V2.1.x — le chiffrement est invisible côté ORM (insert/read transparent) et côté API (JSON shape inchangé)
- Spec V2.2.1 → `delivered: 2026-04-24`. Spec parente V2.2 reste `delivered`, V2.2.1 = extension
- **Suite globale : 235 tests GREEN** (223 + 12 V2.2.1)
- Tous les champs Art.9 du schéma SamsungHealth sont maintenant chiffrés AES-256-GCM at-rest (10 tables, 37 colonnes total : 4 mood + 33 V2.2.1)

### 2026-04-24 V2.2 — fondation chiffrement AES-256-GCM at-rest + table pilote mood

`085dbcd` spec V2.2 (16 tests d'acceptation, fondation crypto + table pilote mood, scope strict Art.9 RGPD).

`1b9c5c9` test(v2.2): 16 tests RED — `TestLoadEncryptionKey` ×5 + `TestEncryptDecryptField` ×3 + `TestEncryptedTypeDecorator` ×1 + `TestBootValidation` ×1 + `TestMoodPersistenceEncrypted` ×3 + `TestMoodApiBackCompat` ×2 + `TestMoodErrorSanitization` ×1.

`66de248` feat(v2.2): fondation crypto AES-256-GCM → 9 tests fondation GREEN.
- `server/security/crypto.py` : `load_encryption_key()` validation stricte (présence + base64 + 32 bytes + non-zero), `encrypt_field`/`decrypt_field` (nonce 12 + ciphertext+tag concaténés), exceptions `EncryptionConfigError` et `DecryptionError` (wrap `cryptography.exceptions.InvalidTag`)
- `server/db/encrypted.py` : `EncryptedBytes`/`EncryptedString`/`EncryptedInt` SQLAlchemy `TypeDecorator` (transparent à l'ORM, sérialisation UTF-8 / ASCII int, BYTEA storage)
- `tests/server/conftest.py` : fixture autouse `_set_test_encryption_key` (clé test stable, reset cache lru entre tests)
- `.env.example` : template clé maître + commande de génération + warning sauvegarde
- `requirements.txt` : `cryptography>=42.0` (déjà 46.0.7 installé localement)

`5df4b2c` feat(v2.2): mood router + model chiffré → 16/16 V2.2 GREEN, 223 suite globale.
- `server/db/models.py::Mood` : `notes`/`emotions`/`factors` en `EncryptedString`, `mood_type` en `EncryptedInt`. Ajout `notes_crypto_v`/`emotions_crypto_v`/`factors_crypto_v`/`mood_type_crypto_v` (smallint NOT NULL default=1) — versionning chiffrement pour rotation future sans perte
- `alembic/versions/0002_encrypt_mood.py` : `op.alter_column` TEXT/INT → BYTEA via `postgresql_using='NULL'` (fresh DB assumée, downgrade non-data-preserving documenté)
- `server/main.py` : `lifespan` async context manager appelle `_validate_encryption_at_boot()` au démarrage uvicorn (pas à l'import — pour ne pas casser pytest collect)
- `server/routers/mood.py` : POST/GET `/api/mood`, `Depends(get_session)`, `pg_insert.on_conflict_do_nothing(start_time)`, wrap `DecryptionError` → `HTTPException(500, "internal_decryption_error")` opaque (sanitization spec §16)
- `server/models.py` : `MoodIn`/`MoodOut`/`MoodBulkIn` Pydantic (types python natifs, pas de bytes leak côté API)
- **Pattern transparent à valider en V2.2.1** : changer `Mapped[str]` → `mapped_column(EncryptedString)` + ajouter colonne `_crypto_v` + migration Alembic = ~2 min par champ. Reproductible mécaniquement sur les 9 tables Art.9 restantes
- **Suite globale : 223 tests GREEN** (207 + 16 V2.2)

### 2026-04-24 V2.1.2 — refonte scripts CSV import + sample generator vers SQLAlchemy/PG

`cf78667` spec V2.1.2 (4 tests d'acceptation, 21 importers à migrer).

`033515c` test(v2.1.2): 4 tests RED — `TestImportSamsungCsv` (round-trip + idempotent) + `TestGenerateSample` (30d + idempotent).

`898eab7` feat(v2.1.2): refactor `scripts/generate_sample.py` vers SQLAlchemy/PG → 2/4 GREEN.
- Pattern décisif pour l'idempotence : `random.seed(42)` au top de `main()` + **séparation génération vs insertion** (Phase 1 = sleep_plan en mémoire consomme tout le random linéairement, Phase 2 = pg_insert ON CONFLICT DO NOTHING). Sans ça, le `continue` après ON CONFLICT skippe `generate_stages` au 2e run → random consommé différemment → jours suivants divergent → 30 → 59 sessions
- Use `pg_insert(...).on_conflict_do_nothing(index_elements=...).returning(<PK>)` partout (steps_hourly, heart_rate_hourly, exercise_sessions, sleep_sessions, sleep_stages)
- `base_date` passé en `datetime(..., tzinfo=timezone.utc)` pour matcher PG `DateTime(timezone=True)` exactement entre runs
- Suppression complète SQLite (`sqlite3`, `DB_PATH`, `init_db()`, `get_connection()` inlinés)

`cc468d1` feat(v2.1.2): refactor `scripts/import_samsung_csv.py` vers SQLAlchemy/PG → 4/4 GREEN.
- **21 importers** migrés de `INSERT OR IGNORE` SQLite vers `pg_insert(Model).on_conflict_do_nothing(index_elements=...).returning(Model.id)`
- Helper unique `_upsert(db, model, values, conflict_cols) -> bool` au top du module — élimine la duplication 21×
- `parse_dt()` retourne maintenant `datetime` aware UTC (au lieu de string ISO) — préserve les égalités ON CONFLICT pour les colonnes `DateTime(timezone=True)`
- Helper `parse_day()` factorise les conversions `day_time` → `YYYY-MM-DD` string (cas `parse_dt` + `ms_to_date_str` selon source CSV)
- `import_sleep_stages` utilise `select(SleepSession.id, SleepSession.sleep_start, SleepSession.sleep_end)` au lieu d'un `SELECT *` (~10× plus rapide sur 30k+ sleep sessions)
- `main()` utilise `get_session()`, suppression de `init_db()`/`get_connection()` (Alembic gère le schema)
- README mis à jour : Components table → "Done (SQLAlchemy depuis V2.1.2)" pour `generate_sample` et `import_samsung_csv`
- Spec V2.1.2 → `delivered: 2026-04-24`
- **Suite globale : 207/207 tests GREEN** (203 + 4 V2.1.2)

### 2026-04-24 `1483873` (V2.1.1 cleanup final)
feat(v2.1.1): cleanup SQLite — purge legacy, scripts isolés, README PG, V2.1 delivered
- `server/database.py` : purge complète (`sqlite3`, `DB_PATH`, `_add_col`, `init_db`, `get_connection` supprimés). Garde `get_engine`/`get_session`/`SessionLocal`/`_DEFAULT_PG_URL`
- `server/main.py` : suppression hook `@app.on_event("startup") def startup(): init_db()` — schema géré par Alembic uniquement
- `scripts/import_samsung_csv.py` + `scripts/generate_sample.py` : helpers SQLite inlinés (autonomes, plus d'import depuis `server.database`). `init_db()` raise `SystemExit` avec message pédagogique pointant spec V2.1.2 (refonte SQLAlchemy à venir)
- `tests/conftest.py` : supprimé (le `client`/`clean_db` fixture SQLite n'a plus de consumer après migration tests sleep vers `tests/server/`)
- `README.md` : section Setup réécrite (Postgres + Docker prérequis, `make db-up && make db-migrate`), Components table mise à jour (SQLAlchemy 2.x, PG 16 Alembic UUID v7), warning migration depuis V1
- Spec `docs/vault/specs/2026-04-24-v2-postgres-migration.md` : `status: delivered`, `delivered: 2026-04-24`
- **Suite globale : 203 tests GREEN, 0 failed** (175 pré-existants + 12 sleep migrés vers PG + 6 round-trip cutover + 10 V2.1 fondation)
- 6 tests sentinelles cutover GREEN (`TestNoSqliteResidual` × 3 + 3 round-trip routers)

### 2026-04-24 `12140e8`
feat(v2.1.1): refactor heartrate + steps + exercise routers → SQLAlchemy + on_conflict_do_nothing returning id (3 round-trip GREEN)
- 3 routers convertis en SessionLocal + `pg_insert(...).on_conflict_do_nothing(index_elements=...).returning(<PK>)`
- Pattern `RETURNING id` requis car `result.rowcount` retourne -1 sur ON CONFLICT — la présence d'un row dans `RETURNING` indique l'insertion effective vs le skip
- Helper `_to_dt`/`_iso` dans exercise pour normaliser timezone UTC (les datetimes PG sont timezone-aware)
- Tests round-trip GREEN : POST 3 records → GET filter → assertions sur shape + count + valeurs croisées avec PG via `db_session`

### 2026-04-24 `b75482f`
feat(v2.1.1): refactor sleep router → SQLAlchemy + 12 tests legacy migrés vers tests/server (PG via testcontainers)
- `server/routers/sleep.py` : POST utilise `select` pour dedup (idempotent) puis `db.add(SleepSession)` + flush + add stages ; GET utilise `selectinload(SleepSession.stages)` quand `include_stages=true`
- `server/models.py` : `SleepSessionOut.id`/`SleepStageOut.id`/`session_id` passent `int → str` (UUID sérialisé). `created_at: str | None = None` (PG renvoie datetime, pas string)
- `tests/test_sleep.py` + `tests/test_sleep_api_shape.py` déplacés dans `tests/server/` (12 tests). Le `client` fixture remplacé par `client_pg_ready` (TestClient + `app.dependency_overrides[get_session]` vers le testcontainer)
- `tests/server/conftest.py` : remontée des fixtures `schema_ready` (alembic upgrade head sur testcontainer) + `client_pg_ready` (combine schema + client_pg). Fixture autouse `_pg_truncate_between_tests` (skip si test ne demande pas pg_container)
- 16 tests sleep-related GREEN (12 legacy + 3 persistence + 1 back-compat)

### 2026-04-24 `37d38fb`
test(v2.1.1): 7 tests RED — back-compat sleep adapté + 3 round-trip routers + 3 sentinelles SQLite cleanup
- `tests/server/test_routers_cutover.py` : `TestHeartRateRouter`/`TestStepsRouter`/`TestExerciseRouter` round-trip POST→GET avec assert via `db_session` PG (preuve que le router tape vraiment PG, pas SQLite résiduel)
- `TestNoSqliteResidual` × 3 : grep `^(import sqlite3|from sqlite3 )` dans `server/**/*.py`, présence `health.db`, `from server.database import get_connection` raise ImportError
- Test back-compat sleep adapté : params `from`/`to`/`include_stages` (réels frontend Nightfall) au lieu de `period=6m` (faux paramètre inventé)

### 2026-04-24 `b0edf6e`
spec(v2.1.1): cutover routers SQLite → SQLAlchemy + suppression code SQLite (spec fille de V2.1) + V2.1 in_progress
- Décision en cours de session : V2.1 splitée en 2 PR (option B). PR fondation = engine + models + alembic + docker + uuid7 (mergeable indépendamment). PR fille (V2.1.1) = refactor 4 routers + suppression complète SQLite + 12 tests legacy migrés
- `docs/vault/specs/2026-04-24-v2-postgres-routers-cutover.md` créée (status: ready, 8 livrables, 9 tests d'acceptation)
- Spec parente `2026-04-24-v2-postgres-migration.md` passée `status: in_progress` + section "Suite naturelle" mise à jour avec le découpage

### 2026-04-24 `9c4f87c`
fix(tests): truncate cascade entre tests pour isolation forte → 9/10 tests V2.1 GREEN
- Tests `TestSleepSessionPersistence` partageaient le testcontainer PG session-scoped → state résiduel entre tests (tests insert+read laissaient des rows, atomic test échouait sur "0 rows attendus")
- Fix : fixture `db_session` truncate cascade toutes les tables non-alembic en teardown
- Bilan tests V2.1 : **9 PASSED / 1 FAILED** (uuid7 ×3 + bootstrap ×3 + persistence ×3 GREEN ; back-compat HTTP `test_get_sleep_period_6m_response_shape_unchanged` reste RED car router refactor pas fait)

### 2026-04-24 `45cc18f`
feat(v2.1): server/database.py refactor (get_engine/get_session) + 21 SQLAlchemy models + alembic init + 0001_initial migration → 6/10 tests GREEN
- `server/database.py` : ajout `get_engine()` (lru_cache, lit `DATABASE_URL` ou défaut local), `get_session()`, `SessionLocal`. Legacy `get_connection()`/`DB_PATH`/`init_db()` conservés pour back-compat des 175+ tests SQLite existants (suppression dans impl 7/7)
- `server/db/models.py` : 21 tables SQLAlchemy 2.x avec `Mapped[]`, `Uuid7PkMixin` + `TimestampedMixin`, FK UUID + ondelete CASCADE, contraintes UNIQUE conservées (sleep_sessions, sleep_stages, steps_*, heart_rate_hourly, exercise_sessions, stress, spo2, respiratory_rate, hrv, skin_temperature, weight, height, blood_pressure, mood, water_intake, activity_daily, vitality_score, floors_daily, activity_level, ecg)
- `alembic/env.py` : lit `DATABASE_URL` env, target_metadata = `Base.metadata`
- `alembic/versions/0001_initial.py` : autogenerate complet + ajout `import server.db.uuid7` (pour résoudre le `server.db.uuid7.Uuid7()` référencé)
- `tests/server/conftest.py` : forcer driver psycopg 3 dans l'URL testcontainers (`postgresql://` → `postgresql+psycopg://`)
- 0 régression sur les 175 tests existants (legacy `get_connection`/`init_db` SQLite intact)

### 2026-04-24 `9491013`
feat(v2.1): docker-compose PG 16-alpine + Makefile db-up/db-down/db-migrate/db-reset
- `docker-compose.yml` : service `postgres:16-alpine` + volume nommé `pgdata` + healthcheck `pg_isready` + port 5432 exposé
- `Makefile` 4 targets : `db-up` (idempotent + wait-ready), `db-down` (volume préservé), `db-migrate` (DATABASE_URL local par défaut si absent), `db-reset` (DESTRUCTIVE — drop volume + recreate + migrate)

### 2026-04-24 `35c559e`
feat(v2.1): uuid7 helper + Uuid7 TypeDecorator + deps PG → 3 tests uuid7 GREEN
- `server/db/uuid7.py` : `uuid7()` (wrapper sur `uuid_utils.uuid7`, retourne `_uuid.UUID` standard) + `Uuid7` TypeDecorator (PG → `UUID(as_uuid=True)`, autres dialects → `CHAR(36)` string)
- `requirements.txt` : ajout `sqlalchemy>=2.0`, `psycopg[binary]>=3.1`, `alembic>=1.13`, `uuid_utils>=0.10`, `testcontainers[postgres]>=4.0`
- 3 tests `TestUuid7` (version 7, monotone par ms, timestamp extractable) GREEN immédiats — pas de DB requise

### 2026-04-24 `161aa86`
test(v2.1): 10 tests RED pour spec postgres-migration
- Première application réelle de la boucle TDD V2.1 — exécution `/tdd` inline (pas de subagent project-local exposé via Agent tool, comme noté wrap V2 foundation)
- `tests/server/conftest.py` — fixtures session : `pg_container` (testcontainers-postgres PG 16-alpine, skip si Docker indispo), `pg_url`, `engine`, `db_session` (SessionLocal SQLAlchemy)
- `tests/server/test_uuid7.py` — `TestUuid7` ×3 : `test_version_field_is_7`, `test_monotonic_within_ms` (100 UUID consécutifs en ordre strict), `test_timestamp_extractable` (extraction 48 bits hex, |Δ| < 5ms)
- `tests/server/test_postgres_bootstrap.py` — `TestBootstrap` ×3 : alembic upgrade head crée 22 tables avec PK UUID + created_at/updated_at obligatoires, idempotent (no "Running upgrade" en 2e run), downgrade base réversible (toutes tables sauf alembic_version disparaissent)
- `tests/server/test_models_postgres.py` — `TestSleepSessionPersistence` ×3 + `TestApiBackCompat` ×1 : insert assigne UUID v7 + created_at == updated_at, read by UUID, atomicité session+stages (FK invalide → rollback complet), back-compat shape JSON GET /api/sleep?period=6m (Nightfall ne bouge pas)
- Bilan RED : **10 tests, 0 GREEN, 4 FAILED + 6 ERROR setup**, tous attribuables à du code/infra manquant déclaré par la spec (`server.db.uuid7`, `server.db.models`, `alembic.ini`, `alembic/versions/0001_initial.py`)
- Stack runtime installé pour valider RED (sera réinstallé via `requirements.txt` à l'étape /impl) : `sqlalchemy>=2.0`, `psycopg[binary]>=3.1`, `alembic>=1.13`, `uuid_utils>=0.10`, `testcontainers[postgres]>=4.0`
- Découverte fix incidente : un `tests/server/__init__.py` créé par mégarde fait que pytest l'importe en tant que package `server` (collision avec `/server/`). Suppression → fix
- 4 notes vault auto-générées par pre-commit hook (cartographer)

### 2026-04-24 `e9c727e`
feat(skill): /tdd (test-writer) — bouclage du chaînon manquant /spec → /tdd → /impl
- Découvert pendant V2.1 kickoff : `/tdd` était référencé partout (`/spec` next_default, `/impl` prev) mais jamais créé
- `.claude/skills/tdd/SKILL.md` — délègue à `test-writer` subagent. Valide prérequis (spec status ≥ ready, frontmatter `tested_by:` non vide). Auto-détecte `target_test_dir` depuis `dirname(tested_by[0].file)`. Prépare `brief.json` (contrat `TestBrief`). Garde-fou : si `tests_green_count > 0` avant impl → faux test, signale au lieu de continuer
- Linked-list maintenant complète : `/spec` → `/tdd` → `/impl` → `/review` → `/align` → `/commit`

### 2026-04-24 `fa2906a`
feat(spec): v2-postgres-migration (status ready, 22 tables, 10 tests d'acceptation, UUID v7 + Alembic + testcontainers)
- Première vraie spec V2.1 produit, branche `feat/v2-postgres-migration` (depuis umbrella `refactor/phase-a-foundation-agents`)
- `docs/vault/specs/2026-04-24-v2-postgres-migration.md`
- Vision : sortir SQLite (single-file, schéma fragile via `_add_col` runtime) au profit de PG 16 + Alembic + UUID v7. Pas de feature produit livrée — fondation pour V2.2+ (chiffrement AES-GCM, JWT, structlog)
- Décisions : PG 16 + tout côté Python (UUID v7 via `uuid_utils`, AES-GCM via `cryptography` futur), SQLAlchemy 2.x ORM + `psycopg[binary]` synchrone, Alembic autogenerate baseline + revue manuelle, `testcontainers-postgres` pour intégration, API publique inchangée (Nightfall ne bouge pas), pas de port de données SQLite
- Livrables : `docker-compose.yml` PG 16-alpine, `server/db/{uuid7.py,models.py}`, `server/database.py` refactor (`get_engine`/`get_session`), `alembic/{env.py,versions/0001_initial.py}`, refactor 4 routers (`sleep`/`heart_rate`/`steps`/`activity`), suppression code SQLite, Makefile targets `db-up`/`db-down`/`db-migrate`/`db-reset`
- 10 tests d'acceptation given/when/then mappés sur 3 fichiers de test : `tests/server/{test_postgres_bootstrap,test_uuid7,test_models_postgres}.py` (bootstrap idempotent, downgrade réversible, UUID v7 monotone/version/timestamp, insert sleep session avec UUID + timestamps auto, read by UUID, atomicité session+stages, back-compat API frontend)
- Out-of-scope verrouillé : AES-GCM (V2.2), JWT (V2.3), structlog (V2.4), async SQLAlchemy, port données

### 2026-04-24 `086c597`
feat(skills): /impl (coder-backend) + /align (plan-keeper) pour boucle TDD V2.1
- Skills manquants identifiés dans le wrap V2 foundation, créés en début de V2.1
- `.claude/skills/impl/SKILL.md` — délègue au subagent `coder-<target>` (backend par défaut). Valide prérequis (spec ≥ ready, tests RED présents et bien rouges), résout `tests_red_path` via frontmatter `tested_by:` de la spec, prépare `brief.json` (contrat `CodeBrief`), délègue. Linked-list : prev `/tdd`, next `/review`
- `.claude/skills/align/SKILL.md` — délègue à `plan-keeper` pour audit déviations plans ↔ livraison. Read-only strict. Compare plans de la branche courante à l'état actuel des fichiers (snapshot ici-et-maintenant, pas de diff vs main). Prépare `brief.json` (contrat `PlanAuditBrief`) avec `triggered_by`/`recent_changes`/`severity_threshold`. Premier vrai test du plan-keeper en conditions réelles (deferred Option C dans wrap V2 foundation). Linked-list : prev `/review`, next `/commit`

### 2026-04-23 `bcde863`
docs(plans/agents): patch tous les paths vault/02_Projects/SamsungHealth/ → docs/vault/ suite vault Obsidian dédié
- `.claude/agents/spec-writer.md` — `vault/02_Projects/.../specs/` → `docs/vault/specs/` (×2 occurrences)
- `.claude/agents/documenter.md` — `vault/02_Projects/.../codex/` → `docs/vault/codex/` (×3 occurrences)
- `.claude/agents/plan-keeper.md` — discovery glob `vault/02_Projects/.../specs/` → `docs/vault/specs/` (×2 occurrences)
- `agents/README.md` — Master plan + Plan multi-agents pointers (×2)
- `docs/vault/README.md` — top-level link plan code-as-vault → `specs/` interne
- `docs/vault/specs/2026-04-23-plan-v2-multi-agents-architecture.md` — header + section codex CI (×2)
- `docs/vault/specs/2026-04-23-plan-v2-refactor-master.md` — header note ajoutée (specs versionnées + vault dédié)
- Skills `.claude/skills/**/*.md` audités → 0 référence à patcher (skills déjà cohérents)
- Re-bootstrap mirror : 78 notes vault, structure inchangée, paths internes propres
- 175/175 tests GREEN

### 2026-04-23 `48e5876`
feat(vault): vault Obsidian dédié pour SamsungHealth (séparé du PKM perso)
- **Path final** : `C:\Users\idsmf\Documents\Obsidian\SamsungHealth\` = `/mnt/c/Users/idsmf/Documents/Obsidian/SamsungHealth/`
- **Avant** : mirror posé dans `PKM/vault/02_Projects/SamsungHealth/` (intégré au vault PKM perso)
- **Maintenant** : vault Obsidian distinct, ouvert via `File → Open vault → Open folder as vault`
- `vault/02_Projects/SamsungHealth/` côté PKM **supprimé entièrement** (plus rien là-bas)
- `CARTOGRAPHER_MIRROR_TO` mis à jour : `~/.zshrc`, `Makefile` exemple, `docs/vault/README.md` setup
- Bootstrap : 78 notes vault dans le nouveau path, structure identique au repo

### 2026-04-23 `4c5c8db`
feat(vault): sortir SamsungHealth du PKM (le mirror EST le dossier 02_Projects/SamsungHealth/) + migrate codex/release + work template vers repo
- **Cleanup PKM** : suppression `vault/02_Projects/SamsungHealth/` entier (codex/, work/, specs/ stubs, code-vault/) — structure remplacée par mirror direct
- Migration `codex/release/nightfall-loom-v1.md` PKM → `docs/vault/codex/release/nightfall-loom-v1.md` (versionné dans le repo)
- Migration `work/_template.md` PKM → `docs/vault/_template/work-session.md`
- `CARTOGRAPHER_MIRROR_TO` mis à jour : `/mnt/c/.../PKM/vault/02_Projects/SamsungHealth/` (sans le sous-dossier `code-vault/`) — env var permanente dans `~/.zshrc`
- `Makefile` target `vault-mirror` exemple de path mis à jour
- `docs/vault/README.md` setup réécrit : "le mirror EST 02_Projects/SamsungHealth/", warning édition manuelle perdue
- Bootstrap mirror : 78 notes vault dans le nouveau path. Structure PKM identique au repo : `annotations/`, `assets/`, `changelog/`, `code/`, `codex/`, `_index/`, `specs/`, `_template/`

### 2026-04-23 `ac24832` → `52c5fc9` (Phase A.8 — specs in vault, 5 blocs)

Specs deviennent first-class dans le vault. Discipline spec-first adoptée : `plan` (méta-architecture) vs `spec` (unitaire < 1 semaine). Top-down `tested_by:` côté spec.

**Bloc 1** `ac24832` — migration 7 specs PKM → `docs/vault/specs/` (single source of truth dans le repo) + frontmatter `type:spec/plan` + stubs PKM avec wikilinks vers le repo.

**Bloc 2** `14a129a` — contrat Pydantic + spec_indexer
- `agents/contracts/spec.py` — `SpecMeta`, `SpecImplements`, `SpecTestedBy`, `SpecType` Literal (spec/plan/us/feature/stub), `SpecStatus` Literal (draft/ready/approved/in_progress/delivered/superseded/reference)
- `agents/cartographer/spec_indexer.py` — `load_spec`, `build_index`, `discover_spec_paths`, `detect_implements_drift`, `untested_specs` (returns list[str] de slugs spec sans tested_by)
- 8 tests + 5 re-exports
- Tolérance YAML : `created` accepte `str | date`, `extra: ignore` sur SpecMeta

**Bloc 3** `728ea5d` — note_renderer + index_generator integration
- `note_renderer` accepte `spec_index`, ajoute :
  - "Implements specs" section dans appendix code (file → specs)
  - "Specs:" annotation per-symbol (matching impl symbols)
  - "Validates specs" section sur tests (test → specs)
  - "Targets" section sur spec notes (auto Implementation + Tests rendus depuis frontmatter)
- `cli` : `_load_spec_index()` + `_render_spec_notes()` (spec-summary mirror dans `code/specs/`)
- `index_generator.generate_specs_index()` → `_index/specs.md` (table 5 colonnes + Untested specs section avec ⚠️)
- 4 nouveaux tests

**Bloc 4** `1b82456` — plan-keeper +2 deviations
- `DeviationType` += `spec_implements_drift`, `untested_spec` (Literal passe à 13 valeurs)
- Subagent prompt enrichi : table de détection + snippet bash réutilisant `spec_indexer`
- 1 test étendu (couverture des 13 valeurs)

**Bloc 5** `52c5fc9` — skill `/spec`
- `.claude/skills/spec/SKILL.md` génère squelette frontmatter pré-rempli (type/status/created/related_plans/implements []/tested_by [])
- Body template : Vision (2-3 phrases) / Décisions techniques / Livrables (checklist) / Tests d'acceptation (given/when/then) / Suite naturelle
- Refuse collisions, valide slug regex `^[a-z0-9][a-z0-9-]{2,40}$`
- `next_default: /tdd`

**État post-A.8** : 78 notes vault (+8 specs incl. nouvelle spec A.8), 7 specs indexées, 1 spec délivrée (nightfall) + 4 plans + 1 reference + 1 ready, 4 specs warning "untested" (action humaine), 175/175 tests GREEN. Code → spec → tests bidirectionnel visible dans Obsidian (mirror Windows).

**Plans patchés** : master + multi-agents + nouveau `2026-04-23-plan-specs-in-vault.md` dans le vault.

### 2026-04-23 `5108be3` → `6bbbdb2` (Phase A.7 — test↔code linking, 4 blocs)

**Bloc 1** `5108be3` — `tests/**`, `android-app/**/test/**` ajoutés à `DEFAULT_SOURCE_GLOBS` du cartographer + re-bootstrap (49 → 65 notes vault, +15 tests/conftest)

**Bloc 2** `0bcd477` — coverage manifest generator (115 symbols / 90 tests / 38 files)
- Added `agents/cartographer/coverage_map.py` — `run_pytest_cov()` (subprocess pytest avec dynamic_context + show_contexts), `parse_coverage()` (normalise JSON coverage.py → manifest 3 vues : by_symbol/by_test/by_file), `tests_for_range()` (intersection lignes ↔ contexts), `write_manifest()`. CLI `python -m agents.cartographer.coverage_map`
- Added `.claude/skills/sync-coverage/SKILL.md` — wrapper skill, next_default `/sync-vault --full`
- Updated `.gitignore` — `docs/vault/_index/coverage-map.json`, `coverage.json`, `.coverage*`, `.coveragerc-cartographer`
- Updated `requirements.txt` — `pytest-cov>=5.0`, `coverage>=7.0`
- Tests : 153/153 GREEN (146 + 7 nouveaux : parse fixture + smoke pytest réel)

**Bloc 3** `049b24f` — note_renderer intègre coverage + fix walker UTF-8 byte slicing
- Updated `agents/cartographer/note_renderer.py` — `render_note()` accepte `coverage_manifest` + `coverage_raw`, frontmatter expose `coverage_pct`, appendix Symbols liste `Tested by (N): test_X, ...`, callout annotation reçoit sub-callout `> [!test]+ Tested by` (intersection range), test files reçoivent section `## Exercises` (inverse map)
- Updated `agents/cartographer/cli.py` — `_load_coverage()` charge manifest + raw, propage à `_render_one`
- Updated `agents/cartographer/index_generator.py` — `generate_coverage_map_index()` génère `_index/coverage-map.md` (table fichiers + section "Untested symbols")
- **Fix walker** — `_node_text()` utilisait `source[node.start_byte:node.end_byte]` (slice CHAR avec offsets BYTES) → cassait sur multi-byte. Fix : `source.encode('utf-8')[start:end].decode()`. Symboles affichés correctement maintenant (avant : "erParseError(Val", après : "MarkerParseError")
- Tests : 158/158 GREEN (153 + 4 TestCoverageIntegration + 1 TestGenerateCoverageMapIndex)

**Bloc 4** `6bbbdb2` — CI workflow `.github/workflows/coverage.yml`
- Run sur push/PR vers main+dev (paths-ignore vault+md+claude)
- `python -m agents.cartographer.coverage_map` → manifest gitignored
- Threshold gate opt-in via `vars.COVERAGE_MIN_PCT` (off par défaut)
- Upload artifact `coverage-map` (manifest + raw, retention 14j)

**État post-A.7** : 67 notes vault, 90 tests indexés, 115 symbols mappés, coverage par symbole + par range + par fichier visible dans Obsidian. Mirror Windows synchronisé.

### 2026-04-23 `a3f9c30`
feat(phase-a.6): annotation-suggester subagent + contrat Pydantic + hook post-commit opt-in (CARTOGRAPHER_SUGGEST=1) (8 tests GREEN)
- Added `agents/contracts/annotation_suggester.py` — `AnnotationSuggestionBrief` (triggered_by post_commit/manual/skill, max_suggestions=5, confidence_threshold), `SuggestedAnnotation` (slug regex + file/line ou begin_line/end_line + rationale + body_draft + confidence low/medium/high + triggers liste), `AnnotationSuggestionReport` (overall suggestions_pending/no_suggestion/failed, next_recommended annotate/commit/none)
- Updated `agents/contracts/__init__.py` — +4 re-exports
- Added `.claude/agents/annotation-suggester.md` — subagent (Read/Grep/Bash, sonnet, color cyan). Heuristiques 8 triggers (issue_ref/pr_ref/kw:workaround/perf/security/semantic, complexity, regression_zone). Filtre dédoublonnage marqueurs existants + cap max_suggestions. Output `${WORK_DIR}/suggestions.md` pour review humaine. **JAMAIS d'écriture sur code/annotations** — propose seulement
- Added `.githooks/post-commit` (executable, opt-in via `$CARTOGRAPHER_SUGGEST=1`) — enregistre `work/post-commit-suggest/<sha>/brief.json` que l'humain materialise via `/annotate suggest --brief <path>`. Hook ne lance pas Claude Code lui-même (incompatible avec git hook context)
- Tests : 146/146 GREEN (139 + 7 nouveaux TestAnnotationSuggester + 1 re-export check)
- **Phase A.6 ✓** — A.5 + A.6 = code-vault complet avec proposition d'annotations automatique

### 2026-04-23 `dc2f108`
feat(cartographer): --mirror-to + bootstrap miroir Windows pour Obsidian (workaround EISDIR WSL chokidar) + Makefile target vault-mirror + doc
- **Contexte** : Obsidian Windows échoue avec `EISDIR: illegal operation on a directory, watch \\wsl.localhost\...` quand on essaie d'ouvrir un dossier sous WSL — bug connu chokidar/Electron sur les UNC paths WSL. Solution adoptée : mirror sens unique vers `/mnt/c/.../PKM/vault/02_Projects/SamsungHealth/code-vault/`
- Updated `agents/cartographer/cli.py` — `run()` accepte `mirror_to`, `_mirror_vault()` fait `rmtree+copytree` (purge stale notes) + écrit `MIRROR-README.md` warning. CLI flag `--mirror-to`, défaut env var `$CARTOGRAPHER_MIRROR_TO`
- Updated `tests/agents/test_cli.py` — `TestMirror` (3 tests : copy, overwrite stale, no-op si None)
- Updated `.githooks/pre-commit` — passe `--mirror-to $CARTOGRAPHER_MIRROR_TO` si l'env var est définie
- Updated `Makefile` — target `vault-mirror` (full re-render + copy)
- Updated `docs/vault/README.md` — section "Comment ouvrir" remplacée par setup mirror Windows + règles read-only
- Updated `.gitignore` — `docs/vault/.obsidian/` (config locale Obsidian, pollue les diffs)
- Persisté `export CARTOGRAPHER_MIRROR_TO=/mnt/c/Users/idsmf/Documents/PKM/vault/02_Projects/SamsungHealth/code-vault` dans `~/.zshrc`
- Bootstrap mirror exécuté : 50 notes code + 30 changelog + 3 indexes + MIRROR-README.md visibles dans le PKM Obsidian Windows
- Tests : 139/139 GREEN (136 + 3 mirror)

### 2026-04-23 `8f08068`
feat(plan-keeper): +3 deviation_types vault (vault_orphan_annotation/vault_missing_note/vault_outdated) + détection détaillée dans subagent prompt
- Updated `agents/contracts/plan_keeper.py` — `DeviationType` Literal étendu à 11 valeurs (8 originaux + 3 vault_*)
- Updated `tests/agents/test_contracts.py::TestPlanKeeper::test_deviation_type_literal` — couvre les 11 valeurs (8/8 GREEN)
- Updated `.claude/agents/plan-keeper.md` — table de détection enrichie avec les 3 types vault et leurs heuristiques (severity scaling sur ancienneté pour orphan, comparaison frontmatter `git_blob` vs `git ls-files -s` pour outdated, glob mismatch pour missing_note) + section "Détections vault — détail technique" avec snippets bash réutilisant `python3 -m agents.cartographer.cli --check`
- Tests : 136/136 GREEN
- Hook pre-commit live : 2 sources stagées → 2 notes vault auto-stagées (création de `docs/vault/code/tests/agents/test_contracts.md` — révèle que le glob bootstrap manque `tests/**` à inclure plus tard)
- **Phase A.5 task #11 ✓ — Phase A.5 100% complète (11/11 tasks)**

### 2026-04-23 `25f51de`
feat(phase-a.5): générateur changelog vault (1 note par commit, frontmatter+body) + bootstrap 30 dernières notes (7 tests GREEN)
- Added `agents/cartographer/changelog_generator.py` — `parse_git_log_records()` (NUL-delimited+RS format), `load_recent_commits()` (git log + git show --name-only par commit), `render_changelog_note()` (frontmatter type/sha/full_sha/date/author/commit_type/scope/files/tags + body Files touched), `generate_changelog()` idempotent (skip si fichier existe sauf `regenerate=True`), CLI `python -m agents.cartographer.changelog_generator --limit N --regenerate`
- Parse Conventional Commits (extrait `commit_type` et `scope` depuis `feat(scope): subject`)
- Tests : 136/136 GREEN (129 + 7 nouveaux ; mocking via `monkeypatch.setattr` sur `load_recent_commits`)
- **Bootstrap exécuté** : 30 notes dans `docs/vault/changelog/` (filename `<YYYY-MM-DD>-<short-sha>.md`)
- **Hook pre-commit déclenché en live** : 2 fichiers staged → 2 notes vault re-rendues + auto-stagées ✓
- Phase A.5 task #10 ✓ ; reste #11 plan-keeper extension (3 deviation_types vault)

### 2026-04-23 `d6f030d`
feat(phase-a.5): hook pre-commit cartographer (re-render notes vault sur staged sources) + make setup-hooks active core.hooksPath
- Added `.githooks/pre-commit` — filtre les fichiers staged par extensions source (`.py|.js|.mjs|.cjs|.kt|.kts|.html|.htm|.css`) ou path `docs/vault/annotations/`. Si annotation seule → `--full` (re-scan complet 5s). Si sources staged → `--diff <files>` (incrémental). Auto-stage `docs/vault/code/` + `docs/vault/_index/` régénérés. Skippable via `--no-verify` (déconseillé)
- Updated `Makefile` — ajout target `setup-hooks` (`git config core.hooksPath .githooks`), invoqué automatiquement par `install`. Active aussi le pre-push existant (branch naming check)
- Hook activé pour la session courante : `git config core.hooksPath .githooks`
- Test à blanc : modif de `agents/cartographer/__init__.py` → 1 note vault re-rendue + auto-stagée ✓
- Phase A.5 task #9 ✓ ; reste #10 changelog gen, #11 plan-keeper extension

### 2026-04-23 `40a195f`
feat(phase-a.5): subagent code-cartographer + 3 skills (/sync-vault /annotate /anchor-review) avec linked-list pattern
- Added `.claude/agents/code-cartographer.md` — subagent (tools : Read/Write/Grep/Glob/Bash, model sonnet, color teal). Workflow : invoque le CLI cartographer pour `full|diff|check`, gère les `AnnotationOpBrief` (create/update/delete/anchor-review). Règle stricte : sync sens unique code+annotations → notes vault rendues, jamais l'inverse
- Added `.claude/skills/sync-vault/SKILL.md` — wrapper du CLI. Sans arg = mode diff sur staged+modified. Args : `--full`, `--diff <files>`, `--check`. Next default `/commit`
- Added `.claude/skills/annotate/SKILL.md` — CRUD annotation. Forms : `<slug> --at <file>:<line>`, `<slug> --range <file>:<start>-<end>`, `edit <slug>`, `delete <slug>`. Valide slug regex, refuse collisions, inject marker + crée annotation file + re-render. Next `/sync-vault --diff`
- Added `.claude/skills/anchor-review/SKILL.md` — résolution orphans. Sans arg = liste. Avec slug = fuzzy match candidats + validation humaine OBLIGATOIRE avant ré-injection. Déplace l'annotation hors `_orphans/` + status active. Next `/commit`
- Tests : 129/129 GREEN (pas de tests sur les .md de skills — invoqués manuellement)
- Phase A.5 task #8 ✓ ; reste #9 hook pre-commit, #10 changelog gen, #11 plan-keeper extension

### 2026-04-23 `46f85e4`
feat(phase-a.5): CLI code-cartographer (full/diff/check) + bootstrap initial 47 notes vault + 3 indexes (5 tests GREEN)
- Added `agents/cartographer/cli.py` — `run(mode, repo_root, vault_root, ...)` orchestrateur + `main()` argv (`--full`, `--diff <files>`, `--check`). Découvre sources via globs, parse markers, walk AST, résout anchors, render notes vault, écrit `_index/` (mode full uniquement), retourne `CartographyReport`. `--check` = dry-run, exit 1 si new orphans
- Added `tests/agents/test_cli.py` — 5 tests (full crée notes + indexes, diff filtre, check pass/fail)
- **Bootstrap initial exécuté** : `python -m agents.cartographer.cli --full` → 47 notes vault dans `docs/vault/code/` (modules : agents/, scripts/, server/, static/, android-app/) + 3 indexes (`orphans.md`, `coverage.md`, `annotations-by-tag.md`)
- Removed `docs/vault/.gitkeep-structure` placeholder (vault maintenant peuplé)
- Tests : 129/129 GREEN (124 + 5 CLI)
- Phase A.5 task #7 ✓ ; reste #8 subagent+skills, #9 hook pre-commit, #10 changelog gen, #11 plan-keeper extension

### 2026-04-23 `f803fd8`
feat(phase-a.5): modules render cartographer (anchor_resolver + note_renderer + orphan_detector + index_generator) + 14 tests GREEN
- Added `agents/cartographer/anchor_resolver.py` — `resolve_anchors_for_file()` retourne `ResolveResult` (3 buckets : `active` avec lignes refreshed, `orphans` avec last_seen, `unmatched` slugs en code sans annotation file)
- Added `agents/cartographer/note_renderer.py` — `render_note()` produit la note vault complète : frontmatter (type/language/git_blob/last_synced ISO-Z/loc/annotations/imports/exports/tags) + H1 + Code mirror callout + orphan warning callout (si orphans) + code interleaved avec `> [!note]+` callouts au bon endroit (single = après marker line, range = après end_line) + appendix symbols/imports/exports
- Added `agents/cartographer/orphan_detector.py` — `detect_orphans()` retourne `OrphanDiff` (new_orphans : annotation active mais slug absent du code ; resolved_orphans : annotation status=orphan mais marker revenu)
- Added `agents/cartographer/index_generator.py` — `generate_orphans_index()`, `generate_coverage_index()` (source files sans annotation), `generate_tags_index()` (groupe par tag)
- Tests : 124/124 GREEN (110 existants + 4 anchor_resolver + 4 note_renderer + 3 orphan_detector + 3 index_generator)
- Phase A.5 task #6 ✓ ; reste #7 CLI bootstrap, #8 subagent+skills, #9 hook, #10 changelog gen, #11 plan-keeper extension

### 2026-04-23 `3032836`
feat(phase-a.5): contrats Pydantic cartographer + 4 modules core (markers/walker/IO/injector) + 42 tests GREEN
- Added `agents/contracts/cartographer.py` (7 types) — `AnchorKind`, `AnchorLocation`, `Annotation` (slug regex `^[a-z0-9][a-z0-9-]{2,40}$`), `CartographyBrief` (mode full/diff/check), `CartographyReport` (overall complete/partial/failed, next_recommended commit/review/anchor-review/none), `AnnotationOpBrief`/`Report`
- Updated `agents/contracts/__init__.py` — +7 re-exports
- Added `agents/cartographer/markers.py` — `parse_markers()` détecte `@vault:slug` dans Python/JS/Kotlin/HTML/CSS, gère single + range begin/end + non-contigu (même slug ×N), `infer_language()`, `MarkerParseError` sur unbalanced begin/end ou slug invalide
- Added `agents/cartographer/annotation_io.py` — `resolve_annotation_path()` (single-file → `<pkg>/<file>/<slug>.md`, cross-file → `_cross/`, no anchor → `_orphans/`), `read_annotation()`, `write_annotation()` avec auto-frontmatter ISO-Z, `update_status()`
- Added `agents/cartographer/marker_injector.py` — `inject_single()` EOL idempotent, `inject_range()` avec préservation indentation, `remove_marker()` distingue own-line (drop) vs EOL (strip)
- Added `agents/cartographer/walker.py` — `walk_file()` universel via tree-sitter, retourne `FileSymbols` (loc + symbols + imports + exports), best-effort sur Kotlin/syntax errors
- Updated `requirements.txt` — ajout `PyYAML>=6.0` (frontmatter annotation)
- Tests : 110/110 GREEN (68 contrats existants + 21 nouveaux cartographer + 17 markers + 8 IO + 10 injector + 7 walker)
- Phase A.5 task #4 + #5 ✓ ; reste #6 render, #7 CLI, #8 subagent+skills, #9 hook, #10 changelog, #11 plan-keeper extension

### 2026-04-21 `d032741`
feat(data): Samsung Health CSV import pipeline + full DB schema (21 tables)
- Added `scripts/explore_samsung_export.py` — reads Samsung Health CSV export, outputs schema only (column names + type tokens), no personal values
- Added `scripts/import_samsung_csv.py` — idempotent bulk import (INSERT OR IGNORE / upsert) from CSV export into SQLite; handles Samsung's metadata-line-1 format, prefixed HC column names, Unix-ms timestamps, sleep stage code remapping (40001-40004 → awake/light/deep/rem)
- Extended `server/database.py` with 16 new tables: `steps_daily`, `stress`, `spo2`, `respiratory_rate`, `hrv`, `skin_temperature`, `weight`, `height`, `blood_pressure`, `mood`, `water_intake`, `activity_daily`, `vitality_score`, `floors_daily`, `activity_level`, `ecg`
- Added `_add_col()` migration helper for ALTER TABLE ADD COLUMN IF NOT EXISTS
- Extended `sleep_sessions` with 7 optional columns (sleep_score, efficiency, sleep_duration_min, sleep_cycle, mental_recovery, physical_recovery, sleep_type) via migration
- Extended `exercise_sessions` with 6 optional columns (calorie_kcal, distance_m, mean/max/min heart_rate, mean_speed_ms)
- Added `CREATE UNIQUE INDEX IF NOT EXISTS` on sleep_stages(stage_start, stage_end) for idempotent re-import

### 2026-04-21 `646aeaa`
chore(dev): dev-mobile script + fix Android cleartext HTTP
- Ajout `scripts/dev-mobile.ps1` — détecte auto les IPs WSL2/Windows, configure port forwarding 8001 et règle firewall
- Ajout `make dev-mobile` — affiche les instructions pour tester depuis le téléphone
- Ajout `android-app/res/xml/network_security_config.xml` — autorise HTTP cleartext (app locale uniquement, pas de données sensibles en transit)
- Référencé dans `AndroidManifest.xml` via `android:networkSecurityConfig`
- Fix `PreferencesManager.kt` — `DEFAULT_URL` sur port 8001

### 2026-04-21 `b5cacc7`
feat(frontend): Nightfall sleep dashboard branché sur l'API réelle
- Remplacé `static/index.html` par la structure Nightfall (fonts Instrument Serif + Geist, `#app`, `#cursor-glow`)
- Copié `dashboard.css` et `dashboard.js` du handoff Claude Design (inchangés)
- Exposé `window.render` dans `dashboard.js` pour découpler chargement des données et rendu
- Créé `static/api.js` — fetch `GET /api/sleep?include_stages=true`, agrège steps + HR, calcule `totals`/`efficiency`/`score`/`summary`, expose `window.SleepData` puis appelle `render()`
- Stratégie "30 dernières sessions disponibles" au lieu d'une fenêtre calendaire fixe
- État vide géré si DB sans données (message sync Android)
- Ajouté `tests/test_sleep_api_shape.py` — 7 tests de contrat sur le shape de l'API (ISO strings, ordre, filtre, stages)

### 2026-04-21 `939f5ef`
chore(workflow): bootstrap project template — CI, labels, hooks, tests
- Added `.github/workflows/ci.yml` — CI Python 3.12, runs `make ci-test` + `make ci-lint` on PR to main/dev
- Added `.github/workflows/review-complete.yml` — bloque les PR sans label `tested` (skip release/hotfix/docs-only)
- Added `.github/ISSUE_TEMPLATE/bug.yml` + `feat.yml` — templates avec labels auto (priorité + taille)
- Added `.github/pull_request_template.md` — format standard résumé / changements / test plan
- Added `.github/labels.json` — 28 labels (P0-P3, size/XS-XXL, types, statuts)
- Added `.githooks/pre-push` — enforce naming branches (feat/|fix/|chore/|hotfix/|release/)
- Added `.claude/settings.json` — permissions de base pour le projet
- Added `Makefile` — targets `dev`, `test`, `lint`, `ci-test`, `ci-lint`
- Added `tests/conftest.py` — TestClient FastAPI + DB de test isolée + fixture clean entre tests
- Added `tests/test_sleep.py` — 5 tests sleep (POST, dedup, GET, stages, not found) — tous verts

### 2026-02-16 `242040a`
Add Phase 3: steps, heart rate, exercise data types + tabbed dashboard
- Renamed DB from `sleep.db` to `health.db` in `server/database.py`
- Added `steps_hourly`, `heart_rate_hourly`, `exercise_sessions` tables with UNIQUE constraints
- Added 6 new Pydantic models (In/Out/Bulk) for steps, heart rate, exercise in `server/models.py`
- Created `server/routers/steps.py` — `POST /api/steps` (bulk INSERT OR IGNORE), `GET /api/steps?from=&to=`
- Created `server/routers/heartrate.py` — `POST /api/heartrate`, `GET /api/heartrate?from=&to=`
- Created `server/routers/exercise.py` — `POST /api/exercise`, `GET /api/exercise?from=&to=`
- Included all 3 new routers in `server/main.py`
- Updated `scripts/generate_sample.py` to generate 30 days of steps (hourly, realistic day/night patterns), heart rate (hourly with night/day variance), and 10-15 exercise sessions
- Added `READ_STEPS`, `READ_HEART_RATE`, `READ_EXERCISE` permissions to `AndroidManifest.xml`
- Extended `HealthConnectManager.kt` with `readSteps()`, `readHeartRate()`, `readExerciseSessions()` methods with hourly aggregation
- Renamed `SleepApi` to `HealthApi` in `ApiClient.kt`, added `postSteps()`, `postHeartRate()`, `postExercise()` endpoints
- Updated `SyncViewModel.kt` to sync all 4 data types sequentially with combined status messages
- Added 5-tab navigation (Sleep, Steps, Heart Rate, Exercise, Trends) in `static/index.html`
- Refactored `static/app.js` with shared month navigation, tab switching, and 5 render functions
- Steps tab: green horizontal bar chart per day showing daily totals
- Heart Rate tab: min-max range bars with avg marker per day
- Exercise tab: card list grouped by date with type, time, duration
- Trends tab: stat cards grid (avg sleep, daily step avg, resting HR, exercise count)
- Added tab styles, step bar, HR range bar, exercise card, and stat card styles in `static/style.css`

### 2026-02-16 `92eae41`
Fix Gradle sync and fetch all available sleep data on initial sync
- Fixed `settings.gradle.kts`: renamed `dependencyResolution` to `dependencyResolutionManagement` with `FAIL_ON_PROJECT_REPOS`
- Added `gradle-wrapper.properties` with Gradle 8.11.1
- Changed initial sync to fetch from epoch 0 (all available data) instead of last 30 days

### 2026-02-16 `8d5cfb0`
Add Phase 2: sleep stages, color-coded calendar, and Android Health Connect app
- Added `sleep_stages` table in `server/database.py` with FK to `sleep_sessions`, cascade delete, session index
- Added `UNIQUE(sleep_start, sleep_end)` constraint on `sleep_sessions` for duplicate handling
- Added `SleepStageIn`, `SleepStageOut` Pydantic models; added optional `stages` field to session models
- Updated `POST /api/sleep` to insert stages via `cursor.lastrowid`, uses `INSERT OR IGNORE` for dedup, returns `{inserted, skipped}`
- Updated `GET /api/sleep` with `?include_stages=true` query param to join stages per session
- Added CSS stage classes: `.stage-light` (blue), `.stage-deep` (dark blue), `.stage-rem` (purple), `.stage-awake` (orange)
- Updated `app.js` to fetch with `include_stages=true`, compute dominant stage per hour-cell by overlap duration, show stage breakdown in tooltips
- Updated `scripts/generate_sample.py` to generate realistic ~90min sleep cycles (light → deep → light → REM, occasional awake)
- Created `android-app/` Gradle project with Jetpack Compose, Health Connect client, Retrofit + kotlinx-serialization
- `HealthConnectManager.kt` reads `SleepSessionRecord` with stages, maps HC stage constants to string types
- `ApiClient.kt` provides configurable Retrofit client (default `http://10.0.2.2:8000` for emulator)
- `PreferencesManager.kt` persists backend URL and last sync timestamp via DataStore
- `SyncViewModel.kt` orchestrates read-from-HC → POST-to-backend flow (last 30 days or since last sync)
- `SyncScreen.kt` and `SettingsScreen.kt` Compose UI with sync button, progress, status, and backend URL config
- Updated `ROADMAP.md` to mark Phase 1 as done

### 2026-02-16 `6200a93`
Add Phase 1: FastAPI backend, SQLite DB, sleep calendar UI, and import scripts
- Added `server/database.py` with SQLite connection (WAL mode), `sleep_sessions` table and index
- Added `server/models.py` with Pydantic models for sleep session validation
- Added `server/routers/sleep.py` with POST/GET `/api/sleep` endpoints (bulk insert, date range query)
- Added `server/main.py` wiring FastAPI app, static file serving, DB init on startup
- Added `static/index.html`, `style.css`, `app.js` — sleep calendar grid (24h columns, per-day rows, month nav, hover tooltips)
- Added `scripts/generate_sample.py` — generates 30 days of realistic sleep data
- Added `scripts/import_csv.py` — parses Samsung Health CSV exports and POSTs to API
- Added `requirements.txt` (fastapi, uvicorn, pydantic)
- Updated `.gitignore` to include `.venv/`
- Updated `README.md` with features table, setup, and usage instructions

### 2026-02-16 `6cc83dc`
Initial project scaffolding with documentation
- Created GitHub repo (LyonMoncef/SamsungHealth)
- Added README with architecture overview (Android SDK → FastAPI → SQLite → Web UI)
- Added NOTES.md with backlog items
- Added ROADMAP.md with 3-phase plan (backend+viz → Android app → expansion)
- Added .gitignore for Python, Android, IDE, and DB files
