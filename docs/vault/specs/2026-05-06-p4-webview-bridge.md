---
title: "Phase 4 WebView Bridge"
slug: 2026-05-06-p4-webview-bridge
status: ready
created: 2026-05-06
phase: P4
spec_type: feature
related_specs:
  - 2026-05-06-p4-android-shell
  - 2026-04-27-v2.3.3.2-frontend-nightfall
implements: []
tested_by:
  - android-app/app/src/test/java/fr/datasaillance/nightfall/webview/NightfallWebViewClientTest.kt
  - android-app/app/src/test/java/fr/datasaillance/nightfall/webview/NightfallJsInterfaceTest.kt
  - android-app/app/src/test/java/fr/datasaillance/nightfall/data/settings/SettingsDataStoreTest.kt
branch: feat/p4-webview-bridge
tags: [phase4, android, webview, bridge, javascript, security, theme]
---

# Spec — Phase 4 WebView Bridge

## Vision

Le WebView Bridge est le contrat d'intégration entre le runtime Android natif (flavor `webview`) et le frontend web (`static/`) servi par le backend FastAPI. Il garantit que chaque `WebViewScreen` charge exclusivement l'URL du backend self-hosted de l'utilisateur, que l'authentification JWT transite sans jamais toucher une URL, et que le thème système est reflété dans le frontend via l'injection d'un attribut `data-theme` au chargement de page. Ce pont est la couche de confiance entre le monde natif et le monde web — sa surface d'attaque est délibérément minimale et entièrement auditée.

---

## Contexte et état de l'existant

La spec `2026-05-06-p4-android-shell` établit la fondation :
- Package root : `fr.datasaillance.nightfall`
- Build flavor `webview` : `SleepScreen`, `TrendsScreen`, `ActivityScreen` sont des composables dans `app/src/webview/java/fr/datasaillance/nightfall/ui/screens/`
- `TokenDataStore` (EncryptedSharedPreferences AES256-GCM) : source unique du JWT
- `SettingsDataStore` (EncryptedSharedPreferences) : URL backend configurable, clé `backend_url`
- `RetrofitClient` cible `backendUrl` — la même URL est utilisée dans la WebView

La spec `2026-04-27-v2.3.3.2-frontend-nightfall` documente que le frontend web (`static/`) est du vanilla JS/HTML/CSS sans framework, sans build step. Les données sont chargées via `fetch("/api/sleep")` etc. Le frontend ne gère pas le flow d'authentification WebView — il s'attend à trouver un JWT déjà disponible dans `window.__AUTH_TOKEN__` ou dans le cookie de session.

L'ancien `network_security_config.xml` autorisait cleartext globalement — la Phase 4 le restreint à `10.0.2.2` uniquement (décision D6 de cette spec).

---

## Décisions techniques

| # | Décision | Justification |
|---|----------|---------------|
| D1 | Injection JWT via `evaluateJavascript("window.__AUTH_TOKEN__='$token'", null)` au `onPageFinished` | Interdit l'exposition du JWT dans l'URL (query param = présent dans les logs serveur, cache browser, historique) ; les cookies httpOnly ne sont pas accessibles depuis `evaluateJavascript` et nécessiteraient une synchronisation cookie jar complexe ; le header HTTP ne peut pas être injecté rétroactivement après chargement initial de page |
| D2 | `NightfallWebViewClient` : `shouldOverrideUrlLoading` bloque toute URL hors `backendUrl` | Empêche l'exfiltration du JWT si le frontend charge un lien externe — C1 + C3. Liens externes ouverts en Chrome Custom Tabs (pas dans la WebView) |
| D3 | `setAllowFileAccess(false)` + `setAllowContentAccess(false)` | Ferme les vecteurs `file://` et `content://` — la WebView n'a aucune raison de lire le filesystem ou le content provider de l'app |
| D4 | `mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW` | Interdit les ressources HTTP depuis une page HTTPS — pas de downgrade silencieux |
| D5 | `onReceivedSslError` : appel `handler.cancel()` sans bypass | Tout certificat invalide = connexion refusée. Pas de `handler.proceed()` en production — sinon MITM transparent |
| D6 | `network_security_config.xml` : cleartext autorisé uniquement pour `10.0.2.2` | Émulateur → localhost WSL2 ; tout autre domaine requiert TLS |
| D7 | `NightfallJsInterface` annoté `@JavascriptInterface` : surface exhaustivement listée | Chaque méthode exposée est une surface d'attaque JS → natif. La liste fermée (3 méthodes Phase 4 + 1 prévu Phase 5) est documentée dans cette spec et auditée par `pentester` |
| D8 | Thème injecté via `document.documentElement.setAttribute('data-theme', 'dark'|'light')` au `onPageFinished` | Compatible avec le mécanisme `prefers-color-scheme` déjà présent dans `static/css/ds-tokens.css` ; pas besoin de modifier le frontend |
| D9 | URL backend lue depuis `SettingsDataStore` (clé `backend_url`, EncryptedSharedPreferences) | Cohérence avec le shell (`NetworkModule.kt` utilise la même clé) ; pas de doublon de stockage |
| D10 | `SettingsDataStore` distinct de `TokenDataStore` | `TokenDataStore` = JWT (courte durée, rotation fréquente) ; `SettingsDataStore` = configuration stable. Séparation des responsabilités |
| D11 | `webView.addJavascriptInterface(NightfallJsInterface(...), "NightfallBridge")` | Nom du binding côté JS : `window.NightfallBridge` — préfixe `Nightfall` évite les collisions avec des libs tierces éventuelles |
| D12 | `onReceivedHttpError` sur 401/403 : `TokenDataStore.clearToken()` + navigation vers `LoginScreen` | JWT expiré ou révoqué → logout automatique plutôt que boucle de 401 silencieuse |
| D13 | `cacheMode = WebSettings.LOAD_DEFAULT` (réseau disponible) / `LOAD_CACHE_ELSE_NETWORK` (hors ligne) | Détection réseau via `ConnectivityManager` au `onPageStarted` ; dégradation gracieuse sans cache applicatif custom |

---

## Architecture

### Vue d'ensemble

```
Android Runtime (flavor webview)
│
├── MainActivity → NavGraph → SleepScreen / TrendsScreen / ActivityScreen
│       │
│       └── WebViewScreen(url = backendUrl + path, modifier)
│               │
│               ├── AndroidView { WebView }
│               │       ├── NightfallWebViewClient  (URL guard + SSL + 401/403)
│               │       └── NightfallJsInterface    (@JavascriptInterface bridge)
│               │
│               └── au onPageFinished :
│                       ├── evaluateJavascript("window.__AUTH_TOKEN__='$token'")
│                       └── evaluateJavascript("document.documentElement.setAttribute('data-theme','$theme')")
│
Backend FastAPI (static/ servi par le serveur)
│
├── GET /           → static/index.html
├── GET /api/sleep  → JSON (auth via Bearer ou window.__AUTH_TOKEN__ injecté côté JS)
└── ...
```

### Structure des fichiers

```
android-app/
  app/src/webview/java/fr/datasaillance/nightfall/
    ui/
      screens/
        sleep/
          SleepScreen.kt          ← WebViewScreen(url = backendUrl + "/", ...)
        trends/
          TrendsScreen.kt         ← WebViewScreen(url = backendUrl + "/trends", ...)
        activity/
          ActivityScreen.kt       ← WebViewScreen(url = backendUrl + "/activity", ...)
    webview/
      WebViewScreen.kt            ← composable générique réutilisé par les 3 screens
      NightfallWebViewClient.kt   ← WebViewClient sécurisé
      NightfallJsInterface.kt     ← @JavascriptInterface bridge JS → natif
    data/
      settings/
        SettingsDataStore.kt      ← EncryptedSharedPreferences, clés backend_url + theme_preference
```

### WebViewScreen composable

```kotlin
// android-app/app/src/webview/java/fr/datasaillance/nightfall/webview/WebViewScreen.kt

@Composable
fun WebViewScreen(
    url: String,
    modifier: Modifier = Modifier,
    tokenDataStore: TokenDataStore,
    settingsDataStore: SettingsDataStore,
    onOpenImport: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val themeValue = if (isDarkTheme) "dark" else "light"

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setAllowFileAccess(false)
                    setAllowContentAccess(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                val backendOrigin = settingsDataStore.getBackendUrl()
                    .trimEnd('/').let { URI(it).run { "$scheme://$host${if (port != -1) ":$port" else ""}" } }

                webViewClient = NightfallWebViewClient(
                    allowedOrigin  = backendOrigin,
                    tokenDataStore = tokenDataStore,
                    onLogout       = onLogout,
                )
                addJavascriptInterface(
                    NightfallJsInterface(
                        context        = ctx,
                        onOpenImport   = onOpenImport,
                    ),
                    "NightfallBridge"
                )
                loadUrl(url)
            }
        },
        update = { webView ->
            // Réinjection si le thème change sans rechargement de page
            val token = tokenDataStore.getToken() ?: ""
            webView.evaluateJavascript(
                "document.documentElement.setAttribute('data-theme','$themeValue')", null
            )
        },
        modifier = modifier.fillMaxSize()
    )
}
```

Paramètres :
- `url: String` — URL complète incluant le path (ex : `https://sh-prod.datasaillance.fr/`)
- `tokenDataStore: TokenDataStore` — injecté via Hilt dans les screens parents
- `settingsDataStore: SettingsDataStore` — pour lire `backend_url` et calculer l'origine autorisée
- `onOpenImport: () -> Unit` — callback natif déclenché par `NightfallBridge.openImport()`
- `onLogout: () -> Unit` — callback déclenché sur 401/403

### NightfallWebViewClient

```kotlin
// android-app/app/src/webview/java/fr/datasaillance/nightfall/webview/NightfallWebViewClient.kt

class NightfallWebViewClient(
    private val allowedOrigin: String,       // ex : "https://sh-prod.datasaillance.fr"
    private val tokenDataStore: TokenDataStore,
    private val onLogout: () -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        return if (url.startsWith(allowedOrigin)) {
            false  // laisser la WebView charger
        } else {
            // Ouvrir Chrome Custom Tab pour les URL externes
            CustomTabsIntent.Builder().build().launchUrl(view.context, request.url)
            true   // bloquer la navigation dans la WebView
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        injectAuth(view)
        injectTheme(view)
    }

    private fun injectAuth(view: WebView) {
        val token = tokenDataStore.getToken() ?: return
        // Injection sécurisée : échappement JSON pour éviter injection de guillemets dans le token
        val safeToken = JSONObject.quote(token).removeSurrounding("\"")
        view.evaluateJavascript("window.__AUTH_TOKEN__='$safeToken';", null)
    }

    private fun injectTheme(view: WebView) {
        val isDark = (view.context.resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val themeVal = if (isDark) "dark" else "light"
        view.evaluateJavascript(
            "document.documentElement.setAttribute('data-theme','$themeVal');", null
        )
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()  // Refuse tout certificat invalide — pas de bypass
        Timber.e("WebView SSL error code=${error.primaryError} url=${view.url}")
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        if (request.isForMainFrame && errorResponse.statusCode in listOf(401, 403)) {
            Timber.w("WebView HTTP ${errorResponse.statusCode} on main frame — clearing token, logout")
            tokenDataStore.clearToken()
            onLogout()
        }
    }
}
```

Règles d'URL :
- Toute URL dont le préfixe correspond à `allowedOrigin` (scheme + host + port) est autorisée dans la WebView.
- Toute autre URL déclenche l'ouverture d'un Chrome Custom Tab et bloque la navigation WebView.
- L'`allowedOrigin` est calculé à partir de `SettingsDataStore.getBackendUrl()` en extrayant uniquement `scheme://host:port` — pas de path dans l'origine.

### NightfallJsInterface

```kotlin
// android-app/app/src/webview/java/fr/datasaillance/nightfall/webview/NightfallJsInterface.kt

class NightfallJsInterface(
    private val context: Context,
    private val onOpenImport: () -> Unit,
) {

    /**
     * Retourne la version de l'app Android.
     * Appelable depuis JS : window.NightfallBridge.getAppVersion()
     */
    @JavascriptInterface
    fun getAppVersion(): String = BuildConfig.VERSION_NAME

    /**
     * Déclenche l'ouverture de ImportScreen (SAF picker) depuis JS.
     * Appelable depuis JS : window.NightfallBridge.openImport()
     * Usage prévu Phase 4 : bouton "Importer" dans le dashboard web.
     */
    @JavascriptInterface
    fun openImport() {
        // Doit s'exécuter sur le main thread — Handler pour thread safety
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onOpenImport()
        }
    }

    /**
     * Réservé Phase 5 — hook pour événements analytiques JS → natif.
     * @param eventJson JSON string : {"type": "...", "payload": {...}}
     * Appelable depuis JS : window.NightfallBridge.onNightfallEvent(json)
     */
    @JavascriptInterface
    fun onNightfallEvent(eventJson: String) {
        // Phase 4 : no-op avec log Timber
        Timber.d("NightfallBridge.onNightfallEvent: $eventJson")
    }
}
```

Surface `@JavascriptInterface` exhaustive (Phase 4) :

| Méthode | Signature Kotlin | Retour | Thread safe | Usage |
|---------|-----------------|--------|-------------|-------|
| `getAppVersion` | `(): String` | `String` | oui (lecture seule) | Version string pour affichage dans UI web |
| `openImport` | `(): Unit` | void | via `Handler(Looper.main)` | Ouvre `ImportScreen` natif depuis JS |
| `onNightfallEvent` | `(eventJson: String): Unit` | void | oui (no-op Phase 4) | Hook Phase 5 — log uniquement en Phase 4 |

Aucune méthode supplémentaire ne doit être ajoutée sans mise à jour de cette spec et re-audit `pentester`.

### SettingsDataStore

```kotlin
// android-app/app/src/main/java/fr/datasaillance/nightfall/data/settings/SettingsDataStore.kt

class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEY_BACKEND_URL   = "backend_url"
        private const val KEY_THEME_PREF    = "theme_preference"
        private const val DEFAULT_BACKEND   = "http://10.0.2.2:8001"  // émulateur → WSL2
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "nightfall_settings_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getBackendUrl(): String =
        prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND) ?: DEFAULT_BACKEND

    fun setBackendUrl(url: String) {
        require(url.startsWith("http://") || url.startsWith("https://")) {
            "backend_url must start with http:// or https://"
        }
        prefs.edit().putString(KEY_BACKEND_URL, url).apply()
    }

    fun getThemePreference(): String =
        prefs.getString(KEY_THEME_PREF, "system") ?: "system"

    fun setThemePreference(value: String) {
        require(value in listOf("system", "dark", "light")) {
            "theme_preference must be 'system', 'dark', or 'light'"
        }
        prefs.edit().putString(KEY_THEME_PREF, value).apply()
    }
}
```

Note : `SettingsDataStore` utilise le fichier de préférences `nightfall_settings_prefs` — distinct de `nightfall_secure_prefs` (utilisé par `TokenDataStore`). Cette séparation est intentionnelle (D10).

### Diagramme de séquence — chargement initial

```
MainActivity → NavGraph → SleepScreen (webview flavor)
    │
    └── WebViewScreen.AndroidView.factory()
            │
            ├── WebView.settings configured (D3, D4, D6)
            ├── webViewClient = NightfallWebViewClient(allowedOrigin, tokenDataStore, onLogout)
            ├── addJavascriptInterface(NightfallJsInterface, "NightfallBridge")
            └── webView.loadUrl("https://sh-prod.datasaillance.fr/")
                    │
                    ├── [réseau] GET https://sh-prod.datasaillance.fr/
                    │       │
                    │       └── FastAPI → static/index.html (+ JS, CSS)
                    │
                    └── NightfallWebViewClient.onPageFinished()
                            ├── injectAuth()
                            │       └── evaluateJavascript("window.__AUTH_TOKEN__='<jwt>'")
                            └── injectTheme()
                                    └── evaluateJavascript("document.documentElement.setAttribute('data-theme','dark')")

    [JS frontend]
        ├── fetch("/api/sleep", { headers: { Authorization: `Bearer ${window.__AUTH_TOKEN__}` } })
        └── render dashboard
```

### Diagramme de séquence — 401 (token expiré)

```
WebView GET /api/sleep
    │
    └── FastAPI → HTTP 401
            │
            └── NightfallWebViewClient.onReceivedHttpError(statusCode=401, isForMainFrame=true)
                    ├── tokenDataStore.clearToken()
                    └── onLogout()  ← callback → navController.navigate("login") { popUpTo(...) }
```

### Diagramme de séquence — lien externe

```
JS frontend → window.location = "https://example.com"
    │
    └── NightfallWebViewClient.shouldOverrideUrlLoading("https://example.com")
            ├── "https://example.com" ne commence pas par allowedOrigin
            ├── CustomTabsIntent.Builder().build().launchUrl(context, uri)
            └── return true  ← navigation WebView bloquée
```

---

## Gestion du thème

### Injection initiale

Au `onPageFinished`, `NightfallWebViewClient.injectTheme()` :
1. Lit `Configuration.UI_MODE_NIGHT_MASK` du contexte courant
2. Injecte `document.documentElement.setAttribute('data-theme', 'dark' | 'light')`

Le frontend web (`static/css/ds-tokens.css`) utilise `[data-theme="dark"]` et `[data-theme="light"]` comme sélecteurs — pas `@media (prefers-color-scheme)` seul. L'attribut injecté a donc la priorité.

### Observation des changements de thème

`MainActivity` observe `isSystemInDarkTheme()` via `collectAsState` depuis un `Flow<Boolean>` dérivé des préférences système. Quand le thème change :
1. `NightfallTheme` se recompose avec le nouveau `darkTheme`
2. Le bloc `update` de `AndroidView` est rappelé
3. `evaluateJavascript("document.documentElement.setAttribute('data-theme', '$themeValue')", null)` est réexécuté sans rechargement de page

Ce mécanisme évite le rechargement complet de la WebView pour un simple changement de thème.

### Priorité des préférences

| Source | Priorité | Mécanisme |
|--------|----------|-----------|
| Préférence utilisateur `"dark"` dans `SettingsDataStore` | 1 (haute) | Passe `darkTheme = true` à `NightfallTheme` + `WebViewScreen` |
| Préférence utilisateur `"light"` | 1 | Passe `darkTheme = false` |
| Préférence utilisateur `"system"` | 2 | Lit `isSystemInDarkTheme()` |
| Thème système Android | 3 (fallback) | `Configuration.UI_MODE_NIGHT_*` |

---

## Injection du JWT côté frontend

Le frontend web (`static/api.js` ou `static/js/api-client.js`) doit lire `window.__AUTH_TOKEN__` et l'injecter dans les requêtes `fetch`. Ce contrat est documenté ici — l'implémentation côté JS est dans la spec `2026-04-27-v2.3.3.2-frontend-nightfall`.

Pattern attendu côté JS (à implémenter dans `static/api.js`) :

```javascript
function getAuthHeaders() {
    const token = window.__AUTH_TOKEN__;
    if (!token) return {};
    return { 'Authorization': `Bearer ${token}` };
}

// Chaque fetch vers /api/* inclut ce header
const response = await fetch('/api/sleep', {
    headers: { ...getAuthHeaders(), 'Content-Type': 'application/json' }
});
```

Contrainte : `window.__AUTH_TOKEN__` est **toujours** une string ou `undefined` — jamais passé en query param, jamais loggué par `console.log` dans le code de production.

---

## Configuration réseau et sécurité Android

### network_security_config.xml (mis à jour Phase 4)

```xml
<!-- android-app/app/src/main/res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Cleartext uniquement pour l'émulateur Android (10.0.2.2 = host WSL2) -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
    <!-- Tout autre domaine : TLS requis -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

Ce fichier remplace la version proto qui autorisait cleartext globalement (`cleartextTrafficPermitted="true"` dans `base-config`).

### AndroidManifest.xml — ajouts nécessaires

```xml
<!-- Dans <application> -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Chrome Custom Tabs -->
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="https" />
    </intent>
</queries>
```

---

## Livrables

### Fichiers nouveaux (flavor `webview`)

- [ ] `app/src/webview/java/fr/datasaillance/nightfall/webview/WebViewScreen.kt` — composable générique avec `AndroidView`, configuration WebView sécurisée, injection JWT + thème au `onPageFinished`
- [ ] `app/src/webview/java/fr/datasaillance/nightfall/webview/NightfallWebViewClient.kt` — `WebViewClient` avec `shouldOverrideUrlLoading` (origine allowlist), `onReceivedSslError` (cancel), `onReceivedHttpError` (401/403 → logout), `onPageFinished` (inject auth + theme)
- [ ] `app/src/webview/java/fr/datasaillance/nightfall/webview/NightfallJsInterface.kt` — interface JS→natif : `getAppVersion()`, `openImport()`, `onNightfallEvent(String)` ; toutes annotées `@JavascriptInterface`

### Fichiers nouveaux (main source set)

- [ ] `app/src/main/java/fr/datasaillance/nightfall/data/settings/SettingsDataStore.kt` — `EncryptedSharedPreferences` pour `backend_url` (défaut `http://10.0.2.2:8001`) et `theme_preference` (défaut `"system"`) ; validations de format

### Fichiers modifiés

- [ ] `app/src/webview/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepScreen.kt` — `WebViewScreen(url = backendUrl + "/", ...)` avec injection `tokenDataStore`, `settingsDataStore`, `onOpenImport`, `onLogout`
- [ ] `app/src/webview/java/fr/datasaillance/nightfall/ui/screens/trends/TrendsScreen.kt` — `WebViewScreen(url = backendUrl + "/trends", ...)`
- [ ] `app/src/webview/java/fr/datasaillance/nightfall/ui/screens/activity/ActivityScreen.kt` — `WebViewScreen(url = backendUrl + "/activity", ...)`
- [ ] `app/src/main/res/xml/network_security_config.xml` — remplacer cleartext global par cleartext restreint à `10.0.2.2`
- [ ] `app/src/main/AndroidManifest.xml` — `<queries>` pour Chrome Custom Tabs
- [ ] `di/AppModule.kt` — `@Provides @Singleton SettingsDataStore`
- [ ] `static/api.js` — ajout de `getAuthHeaders()` qui lit `window.__AUTH_TOKEN__` ; injection dans chaque `fetch` vers `/api/*` (coordination avec spec `2026-04-27-v2.3.3.2-frontend-nightfall`)

### Dépendances Gradle (ajouts à `build.gradle.kts`)

- [ ] `implementation("androidx.browser:browser:1.8.0")` — Chrome Custom Tabs
- [ ] Vérifier que `androidx.security:security-crypto:1.1.0-alpha06` est déjà dans les dépendances shell (oui, per spec p4-android-shell)

### Tests

- [ ] `app/src/test/java/fr/datasaillance/nightfall/webview/NightfallWebViewClientTest.kt` — tests unitaires : shouldOverrideUrlLoading (URL autorisée, URL externe), onReceivedHttpError (401, 403, 200), onReceivedSslError (cancel vérifié)
- [ ] `app/src/test/java/fr/datasaillance/nightfall/webview/NightfallJsInterfaceTest.kt` — tests unitaires : `getAppVersion()` retourne `BuildConfig.VERSION_NAME` ; `openImport()` déclenche callback ; `onNightfallEvent()` ne crash pas
- [ ] `app/src/test/java/fr/datasaillance/nightfall/data/settings/SettingsDataStoreTest.kt` — `getBackendUrl()` retourne défaut si non défini ; `setBackendUrl()` valide le format ; `setThemePreference()` rejette valeurs invalides
- [ ] `app/src/androidTest/java/fr/datasaillance/nightfall/webview/WebViewScreenTest.kt` — test instrumenté : WebView charge l'URL attendue ; `window.__AUTH_TOKEN__` est injecté après `onPageFinished` ; `data-theme` est `dark` ou `light`

---

## Tests d'acceptation

**TA-WV-01 — JWT injecté au chargement de page**
- Given : `TokenDataStore.getToken()` retourne `"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"`
- When : la WebView déclenche `onPageFinished` pour une URL dont l'origine correspond à `allowedOrigin`
- Then : `NightfallWebViewClient.onPageFinished` appelle `evaluateJavascript` avec `window.__AUTH_TOKEN__='eyJ...'` ; le JWT n'apparaît jamais dans l'URL chargée ni dans les paramètres de requête

**TA-WV-02 — URL externe bloquée et ouverte en Custom Tab**
- Given : la WebView est chargée sur `https://sh-prod.datasaillance.fr/`
- When : le frontend JS tente de naviguer vers `https://external-site.example.com`
- Then : `shouldOverrideUrlLoading` retourne `true` (navigation bloquée dans la WebView) ; `CustomTabsIntent` est lancé avec l'URL externe ; aucun contenu de `external-site.example.com` n'est chargé dans la WebView

**TA-WV-03 — Erreur HTTP 401 déclenche logout**
- Given : la WebView charge `https://sh-prod.datasaillance.fr/` (main frame)
- When : le serveur répond `401 Unauthorized` sur la main frame request
- Then : `onReceivedHttpError` efface le token (`TokenDataStore.clearToken()`) ; le callback `onLogout` est invoqué ; la navigation revient à `LoginScreen` avec back stack vidé

**TA-WV-04 — Erreur SSL refusée sans bypass**
- Given : le backend utilise un certificat auto-signé ou expiré
- When : la WebView tente d'établir la connexion TLS
- Then : `onReceivedSslError` appelle `handler.cancel()` — jamais `handler.proceed()` ; la page ne se charge pas ; Timber.e log avec `primaryError` code

**TA-WV-05 — Thème dark injecté**
- Given : le système Android est en mode `UI_MODE_NIGHT_YES`
- When : la WebView déclenche `onPageFinished`
- Then : `evaluateJavascript` est appelé avec `document.documentElement.setAttribute('data-theme','dark')` ; l'attribut `data-theme` du `documentElement` vaut `"dark"`

**TA-WV-06 — Thème light injecté**
- Given : le système Android est en mode `UI_MODE_NIGHT_NO`
- When : la WebView déclenche `onPageFinished`
- Then : `evaluateJavascript` est appelé avec `document.documentElement.setAttribute('data-theme','light')` ; l'attribut `data-theme` du `documentElement` vaut `"light"`

**TA-WV-07 — `openImport()` ouvre ImportScreen**
- Given : `NightfallJsInterface` est attaché à la WebView avec binding `"NightfallBridge"`
- When : le frontend JS appelle `window.NightfallBridge.openImport()`
- Then : le callback `onOpenImport` est déclenché sur le main thread Android ; `NavController` navigue vers `"import"` ; `ImportScreen` est affiché

**TA-WV-08 — URL externe ne contient pas le JWT**
- Given : `TokenDataStore.getToken()` retourne un JWT valide
- When : `shouldOverrideUrlLoading` intercepte une URL externe
- Then : le JWT n'est pas inclus dans l'URL ouverte en Custom Tab (pas de paramètre `token=`, `auth=`, ou similaire)

**TA-WV-09 — Token absent : pas d'injection**
- Given : `TokenDataStore.getToken()` retourne `null`
- When : la WebView déclenche `onPageFinished`
- Then : `injectAuth()` retourne sans appel à `evaluateJavascript` ; aucun `window.__AUTH_TOKEN__` n'est défini dans le contexte JS de la page

**TA-WV-10 — URL backend configurable**
- Given : `SettingsDataStore.getBackendUrl()` retourne `"https://sh-dev.datasaillance.fr"`
- When : `SleepScreen` instancie `WebViewScreen`
- Then : `WebView.loadUrl` est appelé avec `"https://sh-dev.datasaillance.fr/"` ; `NightfallWebViewClient.allowedOrigin` vaut `"https://sh-dev.datasaillance.fr"` (sans trailing slash)

**TA-WV-11 — cleartext interdit hors émulateur**
- Given : l'app est en build release, URL backend = `"http://sh-prod.datasaillance.fr"` (HTTP, non TLS)
- When : la WebView tente de charger l'URL
- Then : Android OS bloque la connexion via `network_security_config.xml` (cleartext non autorisé hors `10.0.2.2`) ; la page ne se charge pas

**TA-WV-12 — Changement de thème sans rechargement de page**
- Given : la WebView a chargé le dashboard en thème `dark`
- When : l'utilisateur change la préférence thème vers `light` dans `SettingsScreen`
- Then : le bloc `update` de `AndroidView` est invoqué ; `evaluateJavascript("document.documentElement.setAttribute('data-theme','light')", null)` est appelé ; la WebView ne se rechargée pas (pas d'appel à `loadUrl` ou `reload`)

---

## Contraintes de sécurité

### Surface d'attaque `@JavascriptInterface`

Chaque méthode annotée `@JavascriptInterface` est appelable par n'importe quel JS exécuté dans la WebView. Le vecteur d'attaque classique est : XSS dans le frontend web → appel arbitraire à `NightfallBridge.*`.

Mesures de mitigation :
1. **Origine allowlist** (`NightfallWebViewClient.shouldOverrideUrlLoading`) : seul le contenu de `allowedOrigin` est chargé dans la WebView. Pas de contenu tiers injecté.
2. **`setAllowFileAccess(false)` + `setAllowContentAccess(false)`** : ferme les vecteurs `file://` et `content://` qui pourraient charger du JS local non contrôlé.
3. **Surface minimale** : 3 méthodes en Phase 4. `onNightfallEvent` est un no-op intentionnel — son seul effet est un log Timber. `openImport` est sans paramètre (pas d'injection de path). `getAppVersion` est en lecture seule.
4. **Thread safety** : `openImport` redirige vers `Looper.getMainLooper()` — évite les race conditions sur `NavController` depuis un thread WebView.
5. **Pas de méthode retournant des données santé** : aucune méthode `@JavascriptInterface` ne lit depuis `TokenDataStore`, `NightfallApi`, ou les données Art.9.

### Politique SSL

| Scénario | Comportement |
|----------|-------------|
| Certificat valide (CA système) | Connexion autorisée |
| Certificat auto-signé | `handler.cancel()` — connexion refusée |
| Certificat expiré | `handler.cancel()` — connexion refusée |
| Certificat révoqué | `handler.cancel()` — connexion refusée |
| HTTP (non TLS) hors `10.0.2.2` | Bloqué par `network_security_config.xml` avant même TLS |

Aucune exception en production. Le bypass `handler.proceed()` est explicitement interdit — toute PR qui l'introduit est bloquée par `pentester` (severity HIGH).

### Protection du JWT

| Vecteur | Protection |
|---------|-----------|
| JWT dans URL query param | Interdit par conception — jamais passé dans `loadUrl(url?token=...)` |
| JWT dans les logs système (`logcat`) | `injectAuth()` ne log pas le token. Timber uniquement sur erreurs |
| JWT exfiltré via URL externe | `shouldOverrideUrlLoading` bloque les navigations hors origine ; le JWT n'est pas inclus dans les Custom Tab URLs |
| JWT dans cache WebView | La WebView ne met pas en cache les réponses d'API (headers `Cache-Control: no-store` côté FastAPI) |
| JWT lisible via `content://` ou `file://` | `setAllowFileAccess(false)` + `setAllowContentAccess(false)` |

### Éléments à auditer par `pentester`

- Liste exhaustive des méthodes `@JavascriptInterface` (toute addition non documentée = finding HIGH)
- `shouldOverrideUrlLoading` : vérifier que la comparaison d'origine résiste à `https://allowedorigin.attacker.com` (startsWith doit matcher scheme+host+port, pas seulement un préfixe de string — implémenter via `URI` parsing, pas `String.startsWith`)
- `injectAuth` : vérifier l'échappement du token avant interpolation dans le JS string (JSONObject.quote ou équivalent)
- `onReceivedSslError` : vérifier l'absence de tout `handler.proceed()` dans les variantes de build
- `network_security_config.xml` : vérifier que `base-config cleartextTrafficPermitted="false"` est bien en place

---

## Dépendances inter-specs

| Spec | Nature de la dépendance |
|------|------------------------|
| `2026-05-06-p4-android-shell` | Fournit `TokenDataStore`, flavor `webview` source set, `NightfallTheme`, `isSystemInDarkTheme()` |
| `2026-04-27-v2.3.3.2-frontend-nightfall` | Le frontend JS doit lire `window.__AUTH_TOKEN__` et l'injecter dans les `fetch` — contrat côté web |
| `2026-05-06-p4-android-auth` | `LoginScreen` est la destination de `onLogout` callback ; le token est sauvegardé dans `TokenDataStore` après login |
| `2026-05-06-p4-android-import` | `ImportScreen` est la destination de `NightfallBridge.openImport()` via `onOpenImport` callback |

---

## Suite naturelle

Ce composant est la dernière pièce du flavor `webview` de Phase 4. Une fois livré et audité, la roadmap Phase 4 → 5 est :

| Spec | Slug | Lien avec WebView Bridge |
|------|------|--------------------------|
| Auth Android | `p4-android-auth` | `onLogout` callback reçoit le token depuis `TokenDataStore.saveToken()` |
| Import SAF | `p4-android-import` | `onOpenImport` callback de `NightfallJsInterface.openImport()` |
| Compose Canvas natif | `p5-compose-canvas` | Remplace les `WebViewScreen` du flavor `native` par des composables Canvas réels ; le WebView Bridge reste intact dans le flavor `webview` comme fallback |

Phase 5 (`p5-compose-canvas`) sera la spec qui retire la dépendance WebView pour le flavor `native` en reimplémantant hypnogramme, radial clock et timeline en Compose Canvas pur. Le WebView Bridge restera opérationnel comme fallback et pour les fonctionnalités qui n'ont pas encore d'équivalent natif.
