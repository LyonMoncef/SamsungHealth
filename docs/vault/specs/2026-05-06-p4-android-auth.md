---
title: "Phase 4 Android Auth Screens"
slug: 2026-05-06-p4-android-auth
status: draft
created: 2026-05-06
phase: P4
spec_type: ui
related_specs:
  - 2026-05-06-p4-android-shell
implements: []
tested_by: []
branch: feat/p4-android-auth
tags: [phase4, android, compose, auth, jwt, oauth, google, login]
---

# Spec — Phase 4 Android Auth Screens

## Vision

Les écrans auth sont la porte d'entrée sécurisée de l'application Nightfall : ils permettent à l'utilisateur de s'authentifier par email/mot de passe ou via Google OAuth, sans jamais exposer les données santé à un tiers. Le JWT d'accès est stocké exclusivement dans `TokenDataStore` (EncryptedSharedPreferences AES-256-GCM), conformément à C2 — aucune valeur sensible ne transite par SharedPreferences en clair ou par des logs. Ces écrans s'emboîtent dans le `NavGraph.kt` défini par la spec `p4-android-shell` et délèguent toute la logique réseau à `AuthViewModel` via Hilt.

---

## Contexte et dépendances

Cette spec est une couche applicative au-dessus du shell (`2026-05-06-p4-android-shell`). Elle consomme les éléments suivants déjà définis dans la spec shell :

| Composant shell | Rôle dans cette spec |
|-----------------|----------------------|
| `NavGraph.kt` | Ajouter les routes `"login"`, `"register"`, `"forgot-password"`, `"auth-callback"` |
| `TokenDataStore` | Seul point de stockage/lecture du JWT — `saveToken()`, `getToken()`, `clearToken()` |
| `NightfallApi` (interface Retrofit vide) | Peupler avec les endpoints auth : `/auth/login`, `/auth/register`, `/auth/password/reset/request`, `/auth/google/start` |
| `NightfallTheme` | Envelopper tous les composables auth |
| `AuthInterceptor` / `RetrofitClient` | Injecté automatiquement — pas de manipulation directe du token dans les ViewModels |
| `di/NetworkModule.kt` | Fournit le `Retrofit` singleton — `AuthViewModel` n'instancie pas de Retrofit |

### Endpoints backend utilisés

| Endpoint | Méthode | Contrat | Utilisé par |
|----------|---------|---------|-------------|
| `/auth/login` | POST | Body `{email, password}` → `{access_token, refresh_token, token_type, expires_in}` (HTTP 200) ou HTTP 401 `invalid_credentials` ou HTTP 403 `email_not_verified` | `LoginScreen` |
| `/auth/register` | POST | Body `{email, password}` + header `X-Registration-Token` → `{id, email}` (HTTP 201) ou HTTP 409 `email_already_exists` ou HTTP 400 `weak_password` | `RegisterScreen` |
| `/auth/password/reset/request` | POST | Body `{email}` → `{"status": "pending"}` (HTTP 202) — réponse toujours identique (anti-enum) | `ForgotPasswordScreen` |
| `/auth/google/start` | POST | Body `{return_to?}` → `{authorize_url}` (HTTP 200) | `LoginScreen` (bouton Google OAuth) |
| `/auth/google/callback` | GET | Géré côté backend — redirige vers `nightfall://auth/callback?token=<access_token>` | `AuthCallbackScreen` (deep link) |
| `/auth/logout` | POST | Header `Authorization: Bearer <token>` + body `{refresh_token}` → HTTP 204 | `ProfileScreen` (bouton "Se déconnecter") |

Note : le refresh token est géré en cookie httpOnly par le backend (`/auth/refresh`). Le client Android stocke uniquement l'`access_token` dans `TokenDataStore` ; le cookie de refresh est géré automatiquement par OkHttp `CookieJar` (voir décision D7 ci-dessous).

---

## Décisions techniques

| # | Décision | Justification |
|---|----------|---------------|
| D1 | `AuthViewModel` unique pour les 3 écrans form (Login, Register, ForgotPassword) | Les états sont disjoints ; un ViewModel partagé simplifie la DI sans couplage entre écrans |
| D2 | `LoginUiState` : sealed class `Idle / Loading / Success / Error(message: String)` | Pattern standard Compose ; évite les booleans multiples conflictuels |
| D3 | Erreur 401 affichée inline sous les champs, pas en dialog | Les dialogs bloquent l'UX mobile ; l'inline error est la pratique Material 3 recommandée |
| D4 | `AuthCallbackScreen` comme destination dédiée (pas un intercepteur Activity) | Le deep link `nightfall://auth/callback` est capturé par Navigation Compose via `deepLinks` — plus propre que `onNewIntent` dans `MainActivity` |
| D5 | Custom Tabs pour le flow OAuth Google | Custom Tabs partage la session navigateur (cookies Google) sans ouvrir une WebView interne non sécurisée ; requis pour le PKCE implicit flow |
| D6 | Appel `POST /auth/google/start` avant ouverture Custom Tabs | Le backend génère le state CSRF et l'URL Google avec nonce — le client ne forge pas l'URL lui-même |
| D7 | `OkHttp CookieJar` (in-memory) pour le refresh cookie httpOnly | Le backend pose le cookie `refresh_token` sur `/auth/refresh` avec `httpOnly + SameSite=Strict` ; OkHttp doit pouvoir le renvoyer sur `/auth/refresh` sans que le code Kotlin y touche |
| D8 | Pas de `refresh_token` dans `TokenDataStore` | Le refresh token est httpOnly et géré par le cookie jar OkHttp — le stocker en clair en SharedPreferences violerait C2 |
| D9 | Validation côté client minimale (email non-vide, password ≥ 1 char) avant envoi | Évite les 400 inutiles ; la vraie validation (force du mot de passe) reste serveur (`validate_password_strength`) |
| D10 | Timeout réseau hérité du shell : 30s connect + 30s read | Défini dans `NetworkModule.kt` — pas de surcharge dans `AuthViewModel` |
| D11 | `RegisterScreen` protégé par header `X-Registration-Token` fourni dans les Settings | L'enregistrement auto-register peut être désactivé côté backend (`SAMSUNGHEALTH_REGISTRATION_TOKEN`) ; le client envoie le token depuis les Settings de l'app |
| D12 | Aucun log Timber des valeurs JWT, email, password | C3 / C2 — `Timber.d` interdit sur toute valeur sensible ; seuls les codes d'erreur HTTP sont loggables |

---

## Architecture

### Nouveaux fichiers à créer

```
android-app/app/src/main/java/fr/datasaillance/nightfall/
  ui/
    screens/
      auth/
        LoginScreen.kt              ← composable principal auth
        RegisterScreen.kt           ← formulaire création de compte
        ForgotPasswordScreen.kt     ← formulaire demande de reset
        AuthCallbackScreen.kt       ← intercepte deep link OAuth, stocke JWT
      auth/components/
        AuthTextField.kt            ← TextField stylé NightfallTheme (email / password)
        AuthPrimaryButton.kt        ← bouton CTA Amber600 (Se connecter, S'inscrire)
        AuthErrorMessage.kt         ← texte d'erreur inline rouge/onError
  viewmodel/
    auth/
      AuthViewModel.kt              ← @HiltViewModel, états LoginUiState
      AuthUiState.kt                ← sealed class LoginUiState + RegisterUiState + ForgotUiState
data/
  http/
    NightfallApi.kt                 ← peupler avec AuthService (voir ci-dessous)
  auth/
    TokenDataStore.kt               ← existant shell — pas modifié
```

### Interface Retrofit — AuthService

```kotlin
// data/http/NightfallApi.kt  (ajout dans l'interface existante)
interface NightfallApi {

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequest,
        @Header("X-Registration-Token") registrationToken: String?
    ): RegisterResponse

    @POST("auth/password/reset/request")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequest): StatusResponse

    @POST("auth/google/start")
    suspend fun googleStart(@Body body: GoogleStartRequest): GoogleStartResponse
}
```

### Data classes Kotlin

```kotlin
// data/http/auth/AuthModels.kt
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
    val expires_in: Int
)

data class RegisterRequest(val email: String, val password: String)
data class RegisterResponse(val id: String, val email: String)

data class PasswordResetRequest(val email: String)
data class StatusResponse(val status: String)

data class GoogleStartRequest(val return_to: String? = null)
data class GoogleStartResponse(val authorize_url: String)
```

Toutes ces classes sont annotées `@Serializable` (kotlinx.serialization).

### States UI

```kotlin
// viewmodel/auth/AuthUiState.kt
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    object Sent : ForgotPasswordUiState()           // affiche confirmation, pas de distinction
    data class Error(val message: String) : ForgotPasswordUiState()
}
```

### AuthViewModel

```kotlin
// viewmodel/auth/AuthViewModel.kt
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: NightfallApi,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    private val _forgotState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val forgotState: StateFlow<ForgotPasswordUiState> = _forgotState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            try {
                val response = api.login(LoginRequest(email, password))
                tokenDataStore.saveToken(response.access_token)
                _loginState.value = LoginUiState.Success
            } catch (e: HttpException) {
                _loginState.value = LoginUiState.Error(mapHttpError(e.code()))
            } catch (e: IOException) {
                _loginState.value = LoginUiState.Error("Vérifiez votre connexion réseau")
            }
        }
    }

    fun register(email: String, password: String, registrationToken: String?) {
        viewModelScope.launch {
            _registerState.value = RegisterUiState.Loading
            try {
                api.register(RegisterRequest(email, password), registrationToken)
                _registerState.value = RegisterUiState.Success
            } catch (e: HttpException) {
                _registerState.value = RegisterUiState.Error(mapHttpError(e.code()))
            } catch (e: IOException) {
                _registerState.value = RegisterUiState.Error("Vérifiez votre connexion réseau")
            }
        }
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _forgotState.value = ForgotPasswordUiState.Loading
            try {
                api.requestPasswordReset(PasswordResetRequest(email))
                _forgotState.value = ForgotPasswordUiState.Sent
            } catch (e: Exception) {
                // Anti-enum: le backend renvoie toujours 202 — on affiche Sent quoi qu'il arrive
                _forgotState.value = ForgotPasswordUiState.Sent
            }
        }
    }

    suspend fun getGoogleStartUrl(): String? {
        return try {
            api.googleStart(GoogleStartRequest()).authorize_url
        } catch (e: Exception) {
            null
        }
    }

    fun storeTokenFromCallback(token: String) {
        tokenDataStore.saveToken(token)
        _loginState.value = LoginUiState.Success
    }

    private fun mapHttpError(code: Int): String = when (code) {
        401 -> "Email ou mot de passe incorrect"
        403 -> "Email non vérifié — consultez votre boîte mail"
        409 -> "Cette adresse email est déjà utilisée"
        400 -> "Mot de passe trop faible (12 caractères minimum, majuscule, chiffre, symbole)"
        else -> "Erreur serveur ($code)"
    }
}
```

Note : `mapHttpError` ne logue jamais les valeurs email/password — uniquement le code HTTP.

---

## Layout et design des écrans

### Tokens design (référence `NightfallTheme`)

| Rôle | Token Compose | Valeur hex |
|------|--------------|------------|
| Fond (dark) | `MaterialTheme.colorScheme.background` | `#191E22` |
| Surface carte | `MaterialTheme.colorScheme.surface` | `#232E32` |
| CTA principal | `MaterialTheme.colorScheme.secondary` (Amber600) | `#D37C04` |
| Lien / accent | `MaterialTheme.colorScheme.tertiary` (Cyan500) | `#07BCD3` |
| Texte principal | `MaterialTheme.colorScheme.onBackground` | `#E8E4DC` (dark) / `#1A1916` (light) |
| Erreur | `MaterialTheme.colorScheme.error` | Material 3 default |
| Police | Cairo (tous styles) | — |

Couleurs interdites dans ces écrans : `#6366f1`, tout `linear-gradient` décoratif, tout `box-shadow` type glow, toute police autre que Cairo.

### LoginScreen

Structure verticale centrée (`Column` + `Arrangement.Center`) :

1. Logo/titre "Nightfall" en `MaterialTheme.typography.headlineMedium` (Cairo SemiBold)
2. `AuthTextField` email (type `KeyboardType.Email`, `ImeAction.Next`)
3. `AuthTextField` password (type `KeyboardType.Password`, toggle visibilité, `ImeAction.Done`)
4. `AuthErrorMessage` — visible uniquement si `LoginUiState.Error` ; texte en `MaterialTheme.colorScheme.error`
5. `AuthPrimaryButton` "Se connecter" — `Amber600`, full width, désactivé si `Loading` + indicateur `CircularProgressIndicator`
6. `TextButton` "Mot de passe oublié ?" — couleur `Cyan500`, navigue vers `"forgot-password"`
7. `OutlinedButton` "Continuer avec Google" — icône Google + texte, couleur border `Cyan500`
8. `TextButton` "Créer un compte" — couleur `Cyan500`, navigue vers `"register"`

### RegisterScreen

Structure verticale centrée :

1. Titre "Créer un compte" en `headlineMedium`
2. `AuthTextField` email
3. `AuthTextField` password (toggle visibilité)
4. `AuthTextField` confirmation mot de passe (type `KeyboardType.Password`)
5. Validation inline : les 2 mots de passe doivent être identiques avant envoi (vérification locale, pas de requête)
6. `AuthErrorMessage` si `RegisterUiState.Error`
7. `AuthPrimaryButton` "S'inscrire" — désactivé si mots de passe différents ou `Loading`
8. Après `RegisterUiState.Success` : message de confirmation "Compte créé — connectez-vous" + navigation automatique vers `"login"`

### ForgotPasswordScreen

Structure verticale centrée :

1. Titre "Réinitialiser le mot de passe"
2. Texte explicatif "Entrez votre email, vous recevrez un lien de réinitialisation."
3. `AuthTextField` email
4. `AuthPrimaryButton` "Envoyer le lien"
5. Si `ForgotPasswordUiState.Sent` : remplacer le formulaire par un message "Un email a été envoyé si ce compte existe." — pas de distinction compte connu/inconnu (anti-enum)
6. `TextButton` "Retour à la connexion" — navigue vers `"login"`

### AuthCallbackScreen

Écran de transition invisible à l'utilisateur (fond `background`, `CircularProgressIndicator` centré) :

1. Lancé automatiquement par le deep link `nightfall://auth/callback?token=<access_token>`
2. Lit le query param `token` depuis `NavBackStackEntry.arguments` ou `Uri`
3. Appelle `authViewModel.storeTokenFromCallback(token)` sur `LaunchedEffect(Unit)`
4. Si token présent et valide : navigue vers `"sleep"` en vidant le back stack
5. Si token absent ou vide : navigue vers `"login"` avec message d'erreur `LoginUiState.Error("Authentification Google échouée")`

---

## Navigation — intégration dans NavGraph.kt

Modifications à apporter dans `ui/navigation/NavGraph.kt` :

```kotlin
composable("login") {
    LoginScreen(
        viewModel = hiltViewModel(),
        onLoginSuccess = { navController.navigate("sleep") {
            popUpTo("login") { inclusive = true }
        }},
        onNavigateRegister = { navController.navigate("register") },
        onNavigateForgotPassword = { navController.navigate("forgot-password") }
    )
}

composable("register") {
    RegisterScreen(
        viewModel = hiltViewModel(),
        onRegisterSuccess = { navController.navigate("login") {
            popUpTo("register") { inclusive = true }
        }}
    )
}

composable("forgot-password") {
    ForgotPasswordScreen(
        viewModel = hiltViewModel(),
        onBack = { navController.popBackStack() }
    )
}

composable(
    route = "auth-callback?token={token}",
    arguments = listOf(navArgument("token") { nullable = true }),
    deepLinks = listOf(navDeepLink {
        uriPattern = "nightfall://auth/callback?token={token}"
    })
) { backStackEntry ->
    AuthCallbackScreen(
        token = backStackEntry.arguments?.getString("token"),
        viewModel = hiltViewModel(),
        onSuccess = { navController.navigate("sleep") {
            popUpTo(0) { inclusive = true }
        }},
        onFailure = { navController.navigate("login") {
            popUpTo(0) { inclusive = true }
        }}
    )
}
```

`LoginScreen` et les écrans auth sont **hors bottom nav scaffold** — la `BottomNavBar` n'est pas rendue quand la route courante est l'une de `{"login", "register", "forgot-password", "auth-callback"}`.

---

## Sécurité

### Google OAuth — flow Custom Tabs

```
LoginScreen
  → authViewModel.getGoogleStartUrl()  [POST /auth/google/start → {authorize_url}]
  → CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(authorize_url))
  → Backend Google → GET /auth/google/callback?code=&state=
  → Backend valide state+nonce, génère access_token, redirect vers nightfall://auth/callback?token=<access_token>
  → AuthCallbackScreen capte le deep link, appelle storeTokenFromCallback(token)
  → Navigation vers SleepScreen
```

Le deep link `nightfall://auth/callback` doit être déclaré dans `AndroidManifest.xml` :

```xml
<activity android:name=".MainActivity" ...>
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="nightfall" android:host="auth" android:pathPrefix="/callback" />
    </intent-filter>
</activity>
```

### Règles de sécurité strictes

- Aucun `Timber.d` / `Timber.v` / `Timber.i` contenant `email`, `password`, `token`, `access_token`, ou toute valeur de champ sensible — uniquement les codes HTTP sont loggables
- `TokenDataStore.saveToken()` ne loggue pas la valeur du token
- `network_security_config.xml` hérité du shell : cleartext autorisé uniquement pour `10.0.2.2`, TLS obligatoire en release
- `CustomTabsIntent` hérite de la politique TLS du navigateur système — pas de bypass possible
- Le refresh token n'est jamais stocké dans `TokenDataStore` — il est httpOnly et géré par `CookieJar` OkHttp (en mémoire, non persisté)

### OkHttp CookieJar

Ajouter dans `NetworkModule.kt` :

```kotlin
.cookieJar(JavaNetCookieJar(CookieManager().apply {
    setCookiePolicy(CookiePolicy.ACCEPT_ALL)
}))
```

Ceci permet au backend de poser et de relire le cookie `refresh_token` sur `/auth/refresh` sans intervention du code Kotlin.

---

## Accessibilité

| Élément | Exigence |
|---------|---------|
| `AuthTextField` email | `contentDescription = "Adresse email"`, `testTag = "field_email"` |
| `AuthTextField` password | `contentDescription = "Mot de passe"`, `testTag = "field_password"` ; bouton toggle visibilité doit avoir `contentDescription` |
| `AuthPrimaryButton` | `testTag = "btn_login"` / `"btn_register"` / `"btn_reset"` |
| Bouton Google OAuth | `contentDescription = "Se connecter avec Google"`, `testTag = "btn_google_oauth"` |
| `AuthErrorMessage` | `semantics { liveRegion = LiveRegionMode.Polite }` pour annonce TalkBack automatique |
| `CircularProgressIndicator` | `contentDescription = "Chargement en cours"` |
| `TextButton` "Mot de passe oublié" | `testTag = "link_forgot_password"` |

Touch target minimum : 48dp × 48dp pour tous les éléments interactifs (Material 3 guideline).

---

## États des composables

### AuthTextField

| Paramètre | Type | Valeur |
|-----------|------|--------|
| `value` | `String` | valeur courante |
| `onValueChange` | `(String) -> Unit` | callback |
| `label` | `String` | "Email" / "Mot de passe" / "Confirmer le mot de passe" |
| `isPassword` | `Boolean` | `false` par défaut |
| `isError` | `Boolean` | déclenche le style rouge Material 3 |
| `enabled` | `Boolean` | `false` si `Loading` |
| `keyboardOptions` | `KeyboardOptions` | voir layout |

### AuthPrimaryButton

| Paramètre | Type | Valeur |
|-----------|------|--------|
| `text` | `String` | libellé |
| `onClick` | `() -> Unit` | callback |
| `isLoading` | `Boolean` | remplace le texte par `CircularProgressIndicator` |
| `enabled` | `Boolean` | `true` par défaut |

---

## Livrables

### Fichiers à créer

- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/LoginScreen.kt` — composable `@Composable fun LoginScreen(viewModel: AuthViewModel, onLoginSuccess, onNavigateRegister, onNavigateForgotPassword)`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/RegisterScreen.kt` — composable `@Composable fun RegisterScreen(viewModel: AuthViewModel, onRegisterSuccess)`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreen.kt` — composable `@Composable fun ForgotPasswordScreen(viewModel: AuthViewModel, onBack)`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/AuthCallbackScreen.kt` — composable `@Composable fun AuthCallbackScreen(token: String?, viewModel: AuthViewModel, onSuccess, onFailure)`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/components/AuthTextField.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/components/AuthPrimaryButton.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/components/AuthErrorMessage.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/auth/AuthViewModel.kt` — `@HiltViewModel`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/auth/AuthUiState.kt` — sealed classes `LoginUiState`, `RegisterUiState`, `ForgotPasswordUiState`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/auth/AuthModels.kt` — data classes Kotlin `@Serializable`

### Fichiers à modifier

- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt` — ajouter les 4 endpoints auth (`login`, `register`, `requestPasswordReset`, `googleStart`)
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt` — ajouter les 4 routes auth + condition `hasToken()` pour `startDestination`
- [ ] `android-app/app/src/main/AndroidManifest.xml` — ajouter `intent-filter` deep link `nightfall://auth/callback`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/di/NetworkModule.kt` — ajouter `CookieJar` OkHttp pour gestion du cookie refresh httpOnly

### Dépendances Gradle supplémentaires

- [ ] `implementation("androidx.browser:browser:1.8.0")` — Custom Tabs pour OAuth Google

---

## Parité light / dark mode

Les deux modes sont obligatoires (C4). Vérifications à couvrir par Paparazzi :

| Composable | Dark | Light |
|-----------|------|-------|
| `LoginScreen` (état Idle) | Fond `#191E22`, champ sur `#232E32`, CTA `#D37C04` | Fond `#FAFAFA`, champ blanc, CTA `#D37C04` |
| `LoginScreen` (état Error) | Message erreur rouge inline visible | Idem |
| `RegisterScreen` (état Idle) | Idem `LoginScreen` | Idem |
| `ForgotPasswordScreen` (état Sent) | Message de confirmation visible, fond dark | Idem light |
| `AuthCallbackScreen` (état Loading) | `CircularProgressIndicator` sur fond dark | Idem light |

---

## Tests d'acceptation

**TA-AUTH-01 — Login succès : JWT stocké et navigation vers dashboard**
- Given : utilisateur non connecté sur `LoginScreen`, backend disponible
- When : l'utilisateur entre un email et mot de passe valides et appuie sur "Se connecter"
- Then : `POST /auth/login` est appelé ; `tokenDataStore.saveToken()` est appelé avec l'`access_token` reçu ; `NavController` navigue vers `"sleep"` ; `"login"` est retiré du back stack

**TA-AUTH-02 — Login échec 401 : erreur inline, pas de dialog**
- Given : utilisateur sur `LoginScreen`, backend retourne HTTP 401 `invalid_credentials`
- When : l'utilisateur soumet le formulaire
- Then : `LoginUiState.Error("Email ou mot de passe incorrect")` est affiché sous les champs de formulaire ; aucun Dialog n'est ouvert ; le champ password reste visible et éditable ; `tokenDataStore.saveToken()` n'est pas appelé

**TA-AUTH-03 — Login email non vérifié : message adapté**
- Given : backend retourne HTTP 403 `email_not_verified`
- When : l'utilisateur soumet le formulaire de login
- Then : `LoginUiState.Error("Email non vérifié — consultez votre boîte mail")` est affiché inline ; `tokenDataStore.saveToken()` n'est pas appelé

**TA-AUTH-04 — Loading state : bouton désactivé et indicateur visible**
- Given : `LoginUiState.Loading` est actif (requête en cours)
- When : le composable `LoginScreen` est rendu
- Then : `AuthPrimaryButton` a `enabled = false` ; un `CircularProgressIndicator` est affiché à la place du texte ; les champs `AuthTextField` ont `enabled = false`

**TA-AUTH-05 — Register succès : navigation vers login**
- Given : utilisateur sur `RegisterScreen`, backend retourne HTTP 201
- When : l'utilisateur entre email + mot de passe + confirmation identique et appuie sur "S'inscrire"
- Then : `RegisterUiState.Success` est émis ; un message de confirmation est affiché ; la navigation se fait vers `"login"` en retirant `"register"` du back stack

**TA-AUTH-06 — Register mots de passe différents : blocage côté client**
- Given : utilisateur sur `RegisterScreen`, champ password = "abc", champ confirmation = "xyz"
- When : le composable est rendu
- Then : `AuthPrimaryButton` a `enabled = false` ; aucun appel réseau n'est effectué

**TA-AUTH-07 — ForgotPassword : réponse identique quelle que soit l'existence du compte**
- Given : utilisateur sur `ForgotPasswordScreen`
- When : l'utilisateur entre n'importe quel email et appuie sur "Envoyer le lien"
- Then : le formulaire est remplacé par le message "Un email a été envoyé si ce compte existe." quelle que soit la réponse backend (200, 202, erreur réseau) — comportement anti-enum

**TA-AUTH-08 — AuthCallbackScreen deep link : JWT stocké et navigation dashboard**
- Given : le backend redirige vers `nightfall://auth/callback?token=valid_jwt_token`
- When : `AuthCallbackScreen` est affiché avec `token = "valid_jwt_token"`
- Then : `tokenDataStore.saveToken("valid_jwt_token")` est appelé ; le `NavController` navigue vers `"sleep"` en vidant le back stack complet

**TA-AUTH-09 — AuthCallbackScreen deep link invalide : retour login avec erreur**
- Given : deep link reçu est `nightfall://auth/callback?token=` (token vide)
- When : `AuthCallbackScreen` est affiché avec `token = null` ou `token = ""`
- Then : `tokenDataStore.saveToken()` n'est pas appelé ; `NavController` navigue vers `"login"` ; `LoginUiState.Error("Authentification Google échouée")` est émis

**TA-AUTH-10 — Pas de log du JWT**
- Given : `LoginUiState.Success` vient d'être émis après un login valide
- When : les logs Timber sont inspectés
- Then : aucune ligne de log ne contient la valeur de l'`access_token` ni la valeur du `password` soumis

**TA-AUTH-11 — Token persisté entre redémarrages**
- Given : `authViewModel.login()` a réussi, `tokenDataStore.saveToken("tok123")` a été appelé
- When : une nouvelle instance de `TokenDataStore` est créée (simule redémarrage de l'app)
- Then : `tokenDataStore.getToken()` retourne `"tok123"` ; `tokenDataStore.hasToken()` retourne `true` ; `NavGraph` démarre sur `"sleep"` sans repasser par `"login"`

**TA-AUTH-12 — Paparazzi LoginScreen dark et light**
- Given : `NightfallTheme(darkTheme = true)` et `NightfallTheme(darkTheme = false)` sur `LoginScreen(uiState = LoginUiState.Idle)`
- When : Paparazzi génère les screenshots
- Then : le screenshot dark correspond au snapshot de référence (fond `#191E22`, CTA `#D37C04`, lien `#07BCD3`) ; le screenshot light correspond au snapshot de référence (fond `#FAFAFA`, même CTA)

---

## Fichiers de test à créer

- [ ] `android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/auth/AuthViewModelTest.kt` — tests unitaires JUnit4 : login succès, login 401, login 403, register succès, register 409, forgotPassword (anti-enum), storeTokenFromCallback
- [ ] `android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/auth/LoginScreenTest.kt` — Paparazzi screenshots : `Idle` dark, `Idle` light, `Error` dark, `Loading` dark
- [ ] `android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/auth/RegisterScreenTest.kt` — Paparazzi : `Idle`, `Error` (mots de passe différents), dark + light
- [ ] `android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreenTest.kt` — Paparazzi : `Idle` + `Sent`

---

## Contraintes RGPD et sécurité (récapitulatif)

| Contrainte | Application dans ces écrans |
|------------|----------------------------|
| **C1** Local-first | `POST /auth/google/start` frappe uniquement le backend VPS personnel — jamais les serveurs Google directement depuis le client |
| **C2** Chiffrement | JWT stocké dans `EncryptedSharedPreferences` AES-256-GCM via `TokenDataStore` — pas de SharedPreferences en clair, pas de Room, pas de fichier |
| **C3** Sécurité | Pas de log de valeurs sensibles ; Custom Tabs pour OAuth ; TLS obligatoire en release ; header `X-Registration-Token` requis pour `/auth/register` |
| **C4** Design | Tokens DataSaillance uniquement — `Amber600` pour CTA, `Cyan500` pour liens, `Background #191E22` dark, police Cairo ; interdit : `#6366f1`, glow, gradient décoratif |
| **C5** No LLM | Aucun appel LLM — l'auth est purement réseau REST |

---

## Suite naturelle

| Spec | Slug | Dépendance sur cette spec |
|------|------|--------------------------|
| Import SAF Android | `p4-android-import` | `NightfallApi` peuplé, `TokenDataStore` opérationnel, `AuthViewModel` stable |
| WebView Bridge | `p4-webview-bridge` | `tokenDataStore.getToken()` injecté en cookie dans la WebView ; flux déconnexion partagé |
