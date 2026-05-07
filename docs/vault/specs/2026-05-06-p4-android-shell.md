---
title: "Phase 4 Android Shell"
slug: 2026-05-06-p4-android-shell
status: draft
created: 2026-05-06
phase: P4
spec_type: ui
related_specs:
  - 2026-04-26-nightfall-rebrand-data-saillance
  - 2026-04-28-phase3-rgpd-endpoints
  - 2026-04-30-phase6-cicd-mvp
implements: []
tested_by: []
branch: feat/p4-android-shell
tags: [phase4, android, compose, jetpack, shell, navigation, theme, hilt, retrofit, samsunghealth, spec]
---

# Spec — Phase 4 Android Shell

## Vision

L'Android Shell est la fondation navigable de l'application Nightfall sur Android : il pose l'architecture de navigation (Jetpack Navigation Compose + bottom nav à 4 destinations), le système de thème (NightfallTheme Material 3 avec tokens DataSaillance, light + dark), et l'infrastructure réseau sécurisée (Retrofit + intercepteur JWT + TokenDataStore chiffré). C'est un rewrite complet de l'app existante (`com.samsunghealth`) — le code actuel (SyncScreen / HealthConnectManager / PreferencesManager) est remplacé entièrement sans migration. Les specs `p4-android-auth`, `p4-android-import` et `p4-webview-bridge` sont les prochaines couches qui s'emboîtent dans ce shell.

---

## Contexte et état de l'existant

L'app Android actuelle (`android-app/`) est un proto fonctionnel mais hors spec Phase 4 :
- Package : `com.samsunghealth` (à remplacer par `fr.datasaillance.nightfall`)
- Pas de navigation graph — navigation manuelle par `showSettings` booléen
- `PreferencesManager` utilise `datastore-preferences` en clair (non chiffré) — violation C2
- `ApiClient` : pas d'intercepteur JWT, pas de Hilt DI
- `MaterialTheme {}` sans tokens DataSaillance
- Pas de build flavors

La Phase 4 est un clean rewrite. Le répertoire `android-app/` est conservé comme root du module Gradle mais tout le code source est remplacé.

---

## Décisions techniques

| # | Décision | Justification |
|---|----------|---------------|
| D1 | Package root : `fr.datasaillance.nightfall` | Alignement écosystème DataSaillance ; `com.samsunghealth` est un nom proto |
| D2 | Build flavors : `webview` (défaut dev) + `native` (Phase 5, stubs) | Permet livraison progressive sans bloquer ; WebView charge `static/` du backend |
| D3 | Navigation Compose (`androidx.navigation:navigation-compose`) | Pas de Fragment, cohérent avec Compose-first ; deep links natifs |
| D4 | Hilt (DI) | Standard Jetpack DI ; suppression des singletons `object` (ex : `ApiClient`) |
| D5 | `EncryptedDataStore` (`androidx.security.crypto`) pour le JWT | Le JWT donne accès aux données Art.9 — C2 interdit SharedPreferences en clair |
| D6 | Retrofit + OkHttp + `AuthInterceptor` | Intercepteur unique qui injecte `Authorization: Bearer <token>` sur toutes les requêtes |
| D7 | `NightfallTheme` : MaterialTheme M3 wrappé, tokens DataSaillance | Cf. spec rebrand 2026-04-26 ; primary=teal, secondary=amber, tertiary=cyan |
| D8 | Polices **Playfair Display** (titres) + **Inter** (UI/body) bundlées via `res/font/` | Système canonique DataSaillance (cf. `Vectorizer/design-system/typography.md`) — variable fonts copiées depuis `static/assets/fonts/` ; Cairo n'est pas dans le système DS |
| D9 | Timber pour le logging | Pas de `Log.d` directs ; Timber.plant en `DebugTree` uniquement dans `webview` debug build |
| D10 | Paparazzi pour screenshot tests des composables | Tests offline (pas d'émulateur) ; couvre `NightfallTheme` light + dark, `BottomNavBar`, états écrans |
| D11 | `minSdk = 28`, `targetSdk = 35`, `compileSdk = 35` | Conservé par rapport au proto ; `EncryptedDataStore` requiert API 23+ — OK |
| D12 | `ProfileScreen` natif dans les 2 flavors | Paramètres app (URL backend, thème, déconnexion) ne dépendent pas du flavor WebView/native |

---

## Architecture

### Build flavors

```kotlin
// android-app/app/build.gradle.kts
android {
    namespace = "fr.datasaillance.nightfall"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.datasaillance.nightfall"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "4.0.0"
    }

    flavorDimensions += "rendering"
    productFlavors {
        create("webview") {
            dimension = "rendering"
            // Dashboard chargé via WebView → URL backend configurée
        }
        create("native") {
            dimension = "rendering"
            // Dashboard Compose Canvas — Phase 5 ; stubs vides en Phase 4
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

Les sources flavor-spécifiques se trouvent dans :
- `app/src/webview/java/fr/datasaillance/nightfall/` — surcharges WebView
- `app/src/native/java/fr/datasaillance/nightfall/` — stubs Phase 4 (corps vides), implémentations réelles Phase 5

### Structure des packages

```
android-app/
  app/
    src/
      main/
        java/fr/datasaillance/nightfall/
          NightfallApplication.kt          ← @HiltAndroidApp, Timber.plant
          MainActivity.kt                  ← @AndroidEntryPoint, NavHost root
          ui/
            theme/
              Color.kt                     ← constantes couleur DataSaillance
              Type.kt                      ← Cairo typography scale
              NightfallTheme.kt            ← MaterialTheme M3 wrappé light+dark
            navigation/
              NavGraph.kt                  ← NavHost + toutes les routes
              BottomNavBar.kt              ← composable bottom navigation 4 tabs
              NavDestination.kt            ← sealed class / enum des routes
            screens/
              sleep/
                SleepScreen.kt             ← WebView (webview flavor) / stub (native)
              trends/
                TrendsScreen.kt            ← WebView (webview flavor) / stub (native)
              activity/
                ActivityScreen.kt          ← WebView (webview flavor) / stub (native)
              profile/
                ProfileScreen.kt           ← natif les 2 flavors
              login/
                LoginScreen.kt             ← natif (redirigé si pas de token)
              import_/
                ImportScreen.kt            ← SAF picker — spec p4-android-import
              settings/
                SettingsScreen.kt          ← URL backend, thème, déconnexion
          data/
            auth/
              TokenDataStore.kt            ← EncryptedDataStore JWT
            http/
              RetrofitClient.kt            ← Retrofit + OkHttp factory (Hilt @Provides)
              AuthInterceptor.kt           ← OkHttp Interceptor injecte Bearer token
              NightfallApi.kt              ← interface Retrofit (vide en Phase 4 shell)
          di/
            AppModule.kt                   ← @Module @InstallIn(SingletonComponent)
            NetworkModule.kt               ← @Provides RetrofitClient, OkHttpClient
        res/
          values/strings.xml
          values/themes.xml                ← thème XML minimal requis par WindowManager
          xml/network_security_config.xml
        AndroidManifest.xml
      webview/
        java/fr/datasaillance/nightfall/
          ui/screens/sleep/SleepScreen.kt  ← WebView réelle
          ui/screens/trends/TrendsScreen.kt
          ui/screens/activity/ActivityScreen.kt
      native/
        java/fr/datasaillance/nightfall/
          ui/screens/sleep/SleepScreen.kt  ← stub @Composable { Text("Phase 5") }
          ui/screens/trends/TrendsScreen.kt
          ui/screens/activity/ActivityScreen.kt
    src/test/java/fr/datasaillance/nightfall/
      ui/theme/NightfallThemeTest.kt       ← Paparazzi screenshots
      ui/navigation/BottomNavBarTest.kt
```

### Navigation graph

```
NavHost(startDestination = NavDestination.Login si pas de token, sinon Sleep)
│
├── LoginScreen        (route: "login")            ← hors bottom nav
├── ImportScreen       (route: "import")           ← hors bottom nav, deep link nightfall://import
├── SettingsScreen     (route: "settings")         ← hors bottom nav
│
└── bottom nav scaffold
    ├── SleepScreen    (route: "sleep")     tab 0  ← icône : bed / lune
    ├── TrendsScreen   (route: "trends")    tab 1  ← icône : trending_up
    ├── ActivityScreen (route: "activity")  tab 2  ← icône : directions_run
    └── ProfileScreen  (route: "profile")   tab 3  ← icône : person
```

`ProfileScreen` contient deux points d'entrée vers les destinations hors-nav :
- Bouton "Importer données" → `navController.navigate("import")`
- Bouton "Paramètres" → `navController.navigate("settings")`

### Thème — NightfallTheme

```kotlin
// ui/theme/Color.kt
val Teal700   = Color(0xFF0E9EB0)   // primary dark + light
val Amber600  = Color(0xFFD37C04)   // secondary (accent chaud)
val Cyan500   = Color(0xFF07BCD3)   // tertiary
val Surface   = Color(0xFF232E32)   // surface dark
val Background = Color(0xFF191E22)  // background dark
val BackgroundLight = Color(0xFFFAFAFA)
val SurfaceLight    = Color(0xFFFFFFFF)
val NeutralGray     = Color(0xFF828587)
```

```kotlin
// ui/theme/NightfallTheme.kt
@Composable
fun NightfallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme(
        primary       = Teal700,
        secondary     = Amber600,
        tertiary      = Cyan500,
        background    = Background,
        surface       = Surface,
        onBackground  = Color(0xFFE8E4DC),
        onSurface     = Color(0xFFE8E4DC),
    ) else lightColorScheme(
        primary       = Teal700,
        secondary     = Amber600,
        tertiary      = Cyan500,
        background    = BackgroundLight,
        surface       = SurfaceLight,
        onBackground  = Color(0xFF1A1916),
        onSurface     = Color(0xFF1A1916),
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NightfallTypography,
        content     = content
    )
}
```

Les polices sont chargées via `FontFamily` depuis `res/font/` (variable fonts bundlées). Le fichier `ui/theme/Type.kt` définit `NightfallTypography: Typography` avec Playfair Display pour les titres (displayLarge → titleLarge) et Inter pour l'UI/body (titleMedium → labelSmall).

### TokenDataStore

```kotlin
// data/auth/TokenDataStore.kt
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "nightfall_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        encryptedPrefs.edit().putString(KEY_JWT, token).apply()
    }

    fun getToken(): String? = encryptedPrefs.getString(KEY_JWT, null)

    fun clearToken() {
        encryptedPrefs.edit().remove(KEY_JWT).apply()
    }

    fun hasToken(): Boolean = getToken() != null

    companion object {
        private const val KEY_JWT = "jwt_access_token"
    }
}
```

Note : `EncryptedSharedPreferences` (androidx.security.crypto) est le mécanisme retenu. L'API `EncryptedDataStore` (protobuf DataStore + tink) est une alternative documentée mais complexe — `EncryptedSharedPreferences` est suffisant et stable pour un token JWT (valeur scalaire). Si le modèle de données s'étend, migrer vers DataStore+tink via une migration explicite.

### RetrofitClient + AuthInterceptor

```kotlin
// data/http/AuthInterceptor.kt
class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenDataStore.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

```kotlin
// di/NetworkModule.kt  (fragment — complet dans le livrable)
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @BackendUrl baseUrl: String
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
```

L'URL backend (`baseUrl`) est lue depuis `TokenDataStore`/settings et injectée via qualifier `@BackendUrl`. La valeur par défaut pour le flavor `webview` debug est `http://10.0.2.2:8001` (émulateur → host machine).

### Gestion du thème utilisateur

`ProfileScreen` et `SettingsScreen` exposent un toggle dark/light. Le choix est persisté dans `EncryptedSharedPreferences` (clé `theme_preference`, valeurs `"system"` | `"dark"` | `"light"`). `MainActivity` lit cette préférence et passe `darkTheme` à `NightfallTheme`.

---

## Livrables

### Gradle + projet

- [ ] `android-app/app/build.gradle.kts` — `productFlavors` `webview` + `native`, namespace `fr.datasaillance.nightfall`, Hilt plugin, Paparazzi, dépendances complètes (Compose BOM, Navigation Compose, Hilt, Retrofit, OkHttp, androidx.security.crypto, Timber)
- [ ] `android-app/build.gradle.kts` — Hilt classpath + Kotlin kapt / KSP plugin
- [ ] `android-app/settings.gradle.kts` — `rootProject.name = "Nightfall"`, inclusion `:app`

### Application + DI

- [ ] `NightfallApplication.kt` — `@HiltAndroidApp`, `Timber.plant(Timber.DebugTree())` conditionnel `BuildConfig.DEBUG`
- [ ] `di/AppModule.kt` — `@Provides @Singleton` `TokenDataStore`, qualifier `@BackendUrl`
- [ ] `di/NetworkModule.kt` — `@Provides @Singleton` `OkHttpClient`, `Retrofit`, `NightfallApi`

### Theme

- [ ] `ui/theme/Color.kt` — constantes couleur DataSaillance (dark + light)
- [ ] `ui/theme/Type.kt` — `NightfallTypography` avec Playfair Display (titres) + Inter (UI/body)
- [ ] `ui/theme/NightfallTheme.kt` — `MaterialTheme` M3 wrappé, dark/light `colorScheme`, `typography`

### Navigation

- [ ] `ui/navigation/NavDestination.kt` — `sealed class` ou `enum` avec routes string et labels/icons
- [ ] `ui/navigation/NavGraph.kt` — `NavHost` avec toutes les destinations (login, sleep, trends, activity, profile, import, settings) ; startDestination conditionnel sur `tokenDataStore.hasToken()`
- [ ] `ui/navigation/BottomNavBar.kt` — `NavigationBar` M3 avec 4 `NavigationBarItem` (Sommeil, Tendances, Activité, Profil)

### Screens

- [ ] `ui/screens/login/LoginScreen.kt` — stub `@Composable` avec `Text("Login — p4-android-auth")` + `onLoginSuccess: () -> Unit` callback
- [ ] `ui/screens/sleep/SleepScreen.kt` (main) — signature uniquement, implémentation dans flavor source sets
- [ ] `ui/screens/sleep/SleepScreen.kt` (webview) — `AndroidView` wrapping `WebView`, charge `${backendUrl}/` ; cookies HTTP-only, `settings.javaScriptEnabled = true`, `settings.domStorageEnabled = true`
- [ ] `ui/screens/sleep/SleepScreen.kt` (native) — `@Composable { Text("Phase 5 — Compose Canvas") }`
- [ ] `ui/screens/trends/TrendsScreen.kt` — même pattern webview/native
- [ ] `ui/screens/activity/ActivityScreen.kt` — même pattern webview/native
- [ ] `ui/screens/profile/ProfileScreen.kt` — natif, liste : URL backend (éditable), toggle thème (system/dark/light), bouton "Importer données" → `ImportScreen`, bouton "Se déconnecter" (clear token + navigate login)
- [ ] `ui/screens/import_/ImportScreen.kt` — stub `@Composable { Text("Import — p4-android-import") }` + deep link `nightfall://import`
- [ ] `ui/screens/settings/SettingsScreen.kt` — accès depuis ProfileScreen ; URL backend configurable

### Auth + Réseau

- [ ] `data/auth/TokenDataStore.kt` — `EncryptedSharedPreferences` AES256-GCM, `saveToken`, `getToken`, `clearToken`, `hasToken`
- [ ] `data/http/AuthInterceptor.kt` — `Interceptor` OkHttp qui lit `TokenDataStore.getToken()`
- [ ] `data/http/RetrofitClient.kt` — factory fonction (remplacée par Hilt en prod, conservée pour tests unitaires)
- [ ] `data/http/NightfallApi.kt` — interface Retrofit vide en Phase 4 shell (à peupler par p4-android-auth et p4-android-import)

### Entrée

- [ ] `MainActivity.kt` — `@AndroidEntryPoint`, `setContent { NightfallTheme { NavGraph() } }`, gestion `darkTheme` depuis préférence utilisateur

### Manifeste + Ressources

- [ ] `AndroidManifest.xml` — package `fr.datasaillance.nightfall`, `NightfallApplication`, permissions INTERNET, deep link intent-filter `nightfall://import`, `android:networkSecurityConfig`
- [ ] `res/xml/network_security_config.xml` — cleartext autorisé uniquement pour `10.0.2.2` (émulateur dev) ; tout le reste TLS
- [ ] `res/font/` — `inter_variable.ttf` + `playfair_display_variable.ttf` (copiés depuis `static/assets/fonts/`, licence OFL)

### Tests

- [ ] `src/test/ui/theme/NightfallThemeTest.kt` — Paparazzi screenshot `NightfallTheme(darkTheme = false)` + `(darkTheme = true)` sur un composable de référence ; les snapshots servent de régression visuelle
- [ ] `src/test/ui/navigation/BottomNavBarTest.kt` — Paparazzi : 4 états `selectedRoute` (un par tab), dark + light

---

## Contraintes RGPD et sécurité (C1–C5)

| Contrainte | Application dans ce shell |
|------------|--------------------------|
| **C1** Local-first | Aucun appel vers un service tiers — Retrofit cible uniquement `backendUrl` configurable par l'utilisateur (VPS personnel) |
| **C2** Chiffrement | JWT stocké dans `EncryptedSharedPreferences` AES256-GCM — pas de `SharedPreferences` en clair, pas de `datastore-preferences` non chiffré |
| **C3** Sécurité | `network_security_config.xml` : cleartext limité à `10.0.2.2` ; TLS requis partout ailleurs. Pentester agent passe avant merge |
| **C4** Design | Tokens DataSaillance uniquement — `NightfallTheme` sans `#6366f1`, sans neons, sans `linear-gradient` décoratif, polices Playfair Display + Inter |
| **C5** No LLM | Aucun appel LLM — le shell ne traite aucune donnée santé directement |

---

## Tests d'acceptation

Les tests d'acceptation suivants sont given/when/then et servent de critères de validation pour l'agent `tdd` puis l'agent `coder-android`.

**TA-01 — Démarrage sans token : redirection login**
- Given : application fraîchement installée, `TokenDataStore` vide
- When : l'utilisateur lance l'app
- Then : `NavHost` démarre sur `LoginScreen` ; la bottom navigation n'est pas visible

**TA-02 — Démarrage avec token valide : accès dashboard**
- Given : `TokenDataStore` contient un JWT valide (`hasToken() == true`)
- When : l'utilisateur lance l'app
- Then : `NavHost` démarre sur `SleepScreen` ; la bottom navigation est visible avec 4 tabs ; tab "Sommeil" est sélectionné

**TA-03 — Navigation bottom nav : switch entre tabs**
- Given : l'app est sur `SleepScreen`
- When : l'utilisateur tape sur l'onglet "Tendances"
- Then : `TrendsScreen` est affiché ; tab "Tendances" est en état `selected` ; les autres tabs ne sont pas `selected` ; aucun back stack n'est empilé (tap retour ne boucle pas entre tabs)

**TA-04 — Dark mode : tokens appliqués**
- Given : `NightfallTheme(darkTheme = true)` actif
- When : le composable `BottomNavBar` est rendu
- Then : `NavigationBar` a pour `containerColor` la valeur `Surface` (`#232E32`) ; les icônes des tabs non sélectionnés ont la couleur `onSurface` avec opacité 60% (M3 spec inactive)

**TA-05 — Light mode : tokens appliqués**
- Given : `NightfallTheme(darkTheme = false)` actif
- When : le composable `BottomNavBar` est rendu
- Then : le fond de la `NavigationBar` est `SurfaceLight` (`#FFFFFF`) ; les textes sont en `onSurface` (`#1A1916`)

**TA-06 — JWT injecté dans chaque requête Retrofit**
- Given : `TokenDataStore.getToken()` retourne `"test-jwt-token"`
- When : `NightfallApi` (via Retrofit) effectue n'importe quel appel HTTP
- Then : la requête OkHttp interceptée par `AuthInterceptor` contient le header `Authorization: Bearer test-jwt-token`

**TA-07 — Absence de token : aucun header Authorization**
- Given : `TokenDataStore.getToken()` retourne `null`
- When : `NightfallApi` effectue un appel HTTP
- Then : la requête ne contient pas le header `Authorization` (l'intercepteur ne forge pas de header vide)

**TA-08 — Navigation vers ImportScreen depuis ProfileScreen**
- Given : l'utilisateur est sur `ProfileScreen`
- When : il appuie sur "Importer données"
- Then : `navController` navigue vers `"import"` ; `ImportScreen` est affiché ; le bouton retour physique revient à `ProfileScreen`

**TA-09 — Deep link import**
- Given : l'app est installée et un token est présent
- When : le système Android résout `nightfall://import`
- Then : l'app s'ouvre directement sur `ImportScreen`

**TA-10 — TokenDataStore : persistance chiffrée**
- Given : `TokenDataStore.saveToken("abc123")` a été appelé
- When : une nouvelle instance de `TokenDataStore` est créée (simule redémarrage de l'app)
- Then : `getToken()` retourne `"abc123"` ; le fichier de préférences sur disque n'est pas lisible en clair

**TA-11 — Déconnexion : clear token + redirection login**
- Given : l'utilisateur est connecté, token présent
- When : il appuie sur "Se déconnecter" dans `ProfileScreen`
- Then : `TokenDataStore.clearToken()` est appelé ; `navController` navigue vers `"login"` en vidant le back stack (`popUpTo(NavDestination.Sleep) { inclusive = true }`)

**TA-12 — Paparazzi snapshot NightfallTheme light**
- Given : `NightfallTheme(darkTheme = false)` sur composable de référence
- When : Paparazzi génère le screenshot
- Then : le screenshot correspond au snapshot de référence (test de régression visuelle)

**TA-13 — Paparazzi snapshot NightfallTheme dark**
- Given : `NightfallTheme(darkTheme = true)` sur composable de référence
- When : Paparazzi génère le screenshot
- Then : fond `#191E22`, `NavigationBar` couleur `#232E32`, accent primary `#0E9EB0`

---

## Dépendances Gradle (versions cibles)

```kotlin
// Compose
implementation(platform("androidx.compose:compose-bom:2024.12.01"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.9.3")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.5")

// Hilt
implementation("com.google.dagger:hilt-android:2.52")
kapt("com.google.dagger:hilt-android-compiler:2.52")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Réseau
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

// Sécurité / stockage chiffré
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// Logging
implementation("com.jakewharton.timber:timber:5.0.1")

// Tests
testImplementation("app.cash.paparazzi:paparazzi:1.3.4")
testImplementation("junit:junit:4.13.2")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
```

Note : Hilt requiert `kapt`. Si le projet migre vers KSP, remplacer `kapt` par `ksp` et ajouter `ksp("com.google.dagger:hilt-android-compiler:2.52")`.

---

## Fichiers à supprimer (ancien proto)

Le clean rewrite implique la suppression des fichiers suivants du proto :
- `android-app/app/src/main/java/com/samsunghealth/ui/SyncScreen.kt`
- `android-app/app/src/main/java/com/samsunghealth/ui/SettingsScreen.kt`
- `android-app/app/src/main/java/com/samsunghealth/viewmodel/SyncViewModel.kt`
- `android-app/app/src/main/java/com/samsunghealth/data/HealthConnectManager.kt`
- `android-app/app/src/main/java/com/samsunghealth/data/ApiClient.kt`
- `android-app/app/src/main/java/com/samsunghealth/data/PreferencesManager.kt`
- `android-app/app/src/main/java/com/samsunghealth/MainActivity.kt` (remplacé)

Health Connect (permissions, sync) est remis en scope dans `p4-android-import`.

---

## Suite naturelle

Ce shell est la fondation des 3 specs Phase 4 suivantes, à développer dans cet ordre :

| Spec | Slug | Dépendance sur ce shell |
|------|------|------------------------|
| Auth Android | `p4-android-auth` | `LoginScreen`, `TokenDataStore`, `NightfallApi` (endpoint `/auth/login`) |
| Import SAF | `p4-android-import` | `ImportScreen`, deep link, `NightfallApi` (endpoints bulk upload), Health Connect |
| WebView Bridge | `p4-webview-bridge` | `SleepScreen` / `TrendsScreen` / `ActivityScreen` flavor `webview`, injection URL + cookie JWT |

La spec `p4-webview-bridge` précisera comment la WebView passe le JWT au backend (cookie `httpOnly` vs header JS) et les contraintes de `WebViewClient` (interception SSL, `shouldOverrideUrlLoading`, ClearText network policy).

Phase 5 (`p5-compose-canvas`) consommera les stubs `native` et remplacera les `Text("Phase 5")` par des composables Canvas réels (hypnogramme, radial clock, timeline).
