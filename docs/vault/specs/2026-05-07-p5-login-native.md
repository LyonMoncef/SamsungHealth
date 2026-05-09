---
title: "P5 Login Native"
slug: 2026-05-07-p5-login-native
status: ready
created: 2026-05-07
branch: feat/p5-login-native
implements:
  - android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/LoginScreen.kt
  - android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt
tested_by:
  - android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/auth/LoginScreenTest.kt
related_specs:
  - 2026-05-06-p4-android-shell.md
  - 2026-04-23-plan-v2-refactor-master.md
tags: [android, compose, auth, p5, login, ui]
---

# Spec — P5 Login Native

## 1. Vision

Le `LoginScreen` est le premier écran visible au lancement de l'app quand aucun JWT n'est présent dans `TokenDataStore`. Phase 4 a livré un stub vide (`Text("Login — p4-android-auth")`) à `ui/screens/login/LoginScreen.kt` — un squelette de navigation fonctionnel mais sans UI réelle. Phase 5 commence par remplacer ce stub par un écran natif Compose complet (email + password + bouton CTA amber + feedback erreur inline) qui appelle `AuthViewModel.login()` et navigue vers `NavDestination.Sleep` après succès. Sans ce déblocage, aucun des six écrans de visualisation P5 ne peut être testé sur device.

Un `LoginScreen` fonctionnel existe déjà à `ui/screens/auth/LoginScreen.kt` — il est complet mais non câblé dans `NavGraph.kt`. Le livrable principal de cette spec est de connecter cet écran au graph de navigation et de supprimer le stub legacy.

---

## 2. État de l'existant (audit P4)

| Fichier | État | Action requise |
|---------|------|----------------|
| `ui/screens/login/LoginScreen.kt` | Stub — `Text("Login — p4-android-auth")` | Supprimer après câblage |
| `ui/screens/auth/LoginScreen.kt` | Implémentation complète (voir §4) | Câbler dans NavGraph |
| `ui/navigation/NavGraph.kt` | Importe le stub legacy | Remplacer l'import et l'instanciation |
| `viewmodel/auth/AuthViewModel.kt` | Complet — `login()`, `loginState: StateFlow<LoginUiState>` | Instancier dans NavGraph |
| `viewmodel/auth/AuthUiState.kt` | `LoginUiState` sealed class définie | Aucune |
| `data/http/AuthModels.kt` | `LoginRequest`, `LoginResponse` définis | Aucune |
| `data/auth/TokenDataStore.kt` | `saveToken()`, `getToken()`, `hasToken()` — EncryptedSharedPreferences | Aucune |
| `di/AppModule.kt` | Fournit `TokenDataStore` et `SettingsDataStore` — pas encore `AuthViewModel` | Ajouter factory `provideAuthViewModel` |
| `data/http/NightfallApi.kt` | `POST auth/login` défini | Aucune |
| `ui/screens/auth/components/` | `AuthTextField`, `AuthPrimaryButton`, `AuthErrorMessage` présents | Aucune |

---

## 3. Contrat de données

### 3.1 HTTP — POST /auth/login

```
Endpoint  : POST https://sh-dev.datasaillance.fr/auth/login
Auth      : aucune (endpoint public)
Body      : { "email": String, "password": String }
Success   : HTTP 200 — { "access_token": String, "refresh_token": String, "token_type": "bearer", "expires_in": Int }
Errors    : 401 (credentials invalides), 403 (email non vérifié), 422 (validation Pydantic)
```

Mapping erreurs HTTP → message utilisateur (défini dans `AuthViewModel.mapHttpError`) :

| Code | Message affiché |
|------|----------------|
| 401 | "Email ou mot de passe incorrect" |
| 403 | "Email non vérifié — consultez votre boîte mail" |
| 400 | "Mot de passe trop faible (12 caractères minimum, majuscule, chiffre, symbole)" |
| autre | "Erreur serveur (\$code)" |
| IOException | "Vérifiez votre connexion réseau" |

### 3.2 ViewModel — AuthViewModel

```kotlin
// Constructeur (DI manuelle — pas de Hilt, issue #52)
class AuthViewModel(
    private val api: NightfallApi,
    private val tokenDataStore: TokenDataStore
) : ViewModel()

// State exposé
val loginState: StateFlow<LoginUiState>  // émis depuis _loginState (MutableStateFlow)

// Méthode principale
fun login(email: String, password: String)
// 1. émet Loading
// 2. appelle api.login(LoginRequest(email, password))
// 3. en succès : tokenDataStore.saveToken(access_token) + émet Success
// 4. HttpException → émet Error(mapHttpError(code))
// 5. IOException → émet Error("Vérifiez votre connexion réseau")
```

### 3.3 Sealed class LoginUiState

```kotlin
sealed class LoginUiState {
    object Idle    : LoginUiState()   // état initial, bouton actif si champs non vides
    object Loading : LoginUiState()   // requête en cours — bouton + champs disabled
    object Success : LoginUiState()   // déclenche LaunchedEffect → onLoginSuccess()
    data class Error(val message: String) : LoginUiState()  // message inline sous les champs
}
```

### 3.4 TokenDataStore

Stockage du JWT via `EncryptedSharedPreferences` (AES-256-GCM, clé dans AndroidKeyStore). En environnement Robolectric uniquement : fallback `SharedPreferences` non chiffrées (Build.FINGERPRINT == "robolectric") — ce chemin ne doit jamais être actif sur un appareil réel (contrainte C2).

```kotlin
fun saveToken(token: String)   // appelé après HTTP 200
fun getToken(): String?
fun hasToken(): Boolean        // utilisé par NavGraph pour choisir startDestination
fun clearToken()               // utilisé par logout (ProfileScreen)
```

---

## 4. Layout et interactions du LoginScreen

### 4.1 Structure visuelle

```
Surface (fillMaxSize, couleur = colorScheme.background)
  Column (padding horizontal 24dp, centré verticalement)
    ├── Text "Connexion" (headlineLarge — Playfair Display Bold 32sp)
    ├── Spacer 32dp
    ├── AuthTextField — Email (keyboardType = Email, testTag = "field_email")
    ├── Spacer 16dp
    ├── AuthTextField — Mot de passe (isPassword = true, toggle œil, testTag = "field_password")
    ├── Spacer 8dp
    ├── [conditionnel] AuthErrorMessage (visible si LoginUiState.Error, liveRegion = Polite)
    ├── Spacer 8dp
    ├── Spacer 16dp
    ├── AuthPrimaryButton "Se connecter" (containerColor = secondary = Amber600, testTag = "btn_login")
    ├── Spacer 8dp
    ├── OutlinedButton "Continuer avec Google" (border + contenu = tertiary = Cyan500, testTag = "btn_google_oauth")
    ├── Spacer 16dp
    ├── TextButton "Mot de passe oublié ?" (color = tertiary, testTag = "link_forgot_password")
    └── TextButton "Créer un compte" (color = tertiary)
```

### 4.2 Signature du composable

```kotlin
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgotPassword: () -> Unit
)
```

### 4.3 États UX

| État `loginState` | Champs email/password | Bouton "Se connecter" | Bouton Google | AuthErrorMessage |
|-------------------|-----------------------|----------------------|---------------|-----------------|
| `Idle` | enabled | enabled (si au moins 1 char dans chaque champ — voir §4.4) | enabled | invisible |
| `Loading` | disabled | disabled + CircularProgressIndicator | disabled | invisible |
| `Success` | — | — | — | — (LaunchedEffect → onLoginSuccess) |
| `Error(msg)` | enabled | enabled | enabled | visible — msg au-dessus du CTA |

> Note : L'implémentation actuelle dans `ui/screens/auth/LoginScreen.kt` désactive le bouton uniquement via `isLoading` (pas via validation de champs vides). Le test d'acceptation TA-L-04 (bouton disabled si champs vides) exige un ajustement : `enabled = email.isNotBlank() && password.isNotBlank() && !isLoading`.

### 4.4 Logique locale (state holder)

```kotlin
var email    by remember { mutableStateOf("") }
var password by remember { mutableStateOf("") }
val isLoading = loginState is LoginUiState.Loading

// Validation d'activation du bouton (à ajouter à l'implémentation actuelle)
val isFormValid = email.isNotBlank() && password.isNotBlank()
```

### 4.5 Navigation après succès

```kotlin
LaunchedEffect(loginState) {
    if (loginState is LoginUiState.Success) {
        onLoginSuccess()
    }
}
```

Dans `NavGraph.kt`, `onLoginSuccess` est câblé comme :

```kotlin
navController.navigate(NavDestination.Sleep.route) {
    popUpTo(NavDestination.Login.route) { inclusive = true }
}
```

Le `popUpTo(inclusive = true)` est obligatoire pour empêcher le retour arrière vers Login après authentification.

---

## 5. Tokens design Compose

### 5.1 Color.kt — tokens DataSaillance

| Token Kotlin | Valeur hex | Role Material3 | Usage LoginScreen |
|---|---|---|---|
| `Teal700` | `#0E9EB0` | `primary` | Titre, bordures OutlinedTextField focus |
| `Amber600` | `#D37C04` | `secondary` | CTA "Se connecter" (`containerColor`) |
| `Cyan500` | `#07BCD3` | `tertiary` | Bouton Google, liens TextButton |
| `Background` | `#191E22` | `background` dark | Surface pleine page (dark mode) |
| `Surface` | `#232E32` | `surface` dark | Surfaces élevées (dark mode) |
| `BackgroundLight` | `#FAFAFA` | `background` light | Surface pleine page (light mode) |
| `SurfaceLight` | `#FFFFFF` | `surface` light | Surfaces élevées (light mode) |
| `NeutralGray` | `#828587` | — | Placeholder, hint |

Tokens interdits : `#6366f1` (indigo Tailwind), dégradés décoratifs, box-shadow glow.

### 5.2 Typographie

| Style Material3 | Famille | Poids | Taille | Usage |
|---|---|---|---|---|
| `headlineLarge` | Playfair Display | Bold | 32sp / 40sp | Titre "Connexion" |
| `bodyLarge` | Inter | Regular | 16sp / 24sp | Labels champs |
| `labelLarge` | Inter | Medium | 14sp / 20sp | Texte boutons |
| `bodySmall` | Inter | Regular | 12sp / 16sp | Erreurs inline |

> Divergence avec le brief : le brief mentionne la police Cairo. Le code existant dans `Type.kt` utilise Playfair Display (headings) + Inter (UI). Cette spec se conforme au code réel — Cairo est la police web (`static/`), non la police Android native. Si une migration vers Cairo est souhaitée pour Android, ouvrir une issue dédiée.

### 5.3 NightfallTheme (dark + light — obligatoires)

`NightfallTheme` expose les deux palettes via `darkColorScheme` et `lightColorScheme`. Chaque test Paparazzi doit capturer les deux modes (voir §7). Le switch se fait via `isSystemInDarkTheme()` en production — aucun override manuel dans `LoginScreen`.

---

## 6. Livrables

### 6.1 Fichiers à modifier

| Fichier | Modification |
|---------|-------------|
| `ui/navigation/NavGraph.kt` | Remplacer l'import `ui.screens.login.LoginScreen` par `ui.screens.auth.LoginScreen`. Instancier `AuthViewModel` (via `AppModule` ou factory locale). Passer `onNavigateRegister`, `onNavigateForgotPassword` à `LoginScreen`. |
| `di/AppModule.kt` | Ajouter `fun provideAuthViewModel(api: NightfallApi, tokenDataStore: TokenDataStore): AuthViewModel`. |
| `ui/screens/auth/LoginScreen.kt` | Ajouter la validation `isFormValid` pour désactiver le bouton si email ou password vides (TA-L-04). |

### 6.2 Fichiers à supprimer

| Fichier | Raison |
|---------|--------|
| `ui/screens/login/LoginScreen.kt` | Stub legacy — remplacé par `ui/screens/auth/LoginScreen.kt` câblé dans NavGraph |

### 6.3 Fichiers inchangés (conformes à la spec)

- `viewmodel/auth/AuthViewModel.kt` — complet, pas de modification
- `viewmodel/auth/AuthUiState.kt` — complet, pas de modification
- `data/http/AuthModels.kt` — complet, pas de modification
- `data/auth/TokenDataStore.kt` — complet, pas de modification
- `data/http/NightfallApi.kt` — `POST auth/login` présent
- `ui/screens/auth/components/AuthTextField.kt` — toggle password présent
- `ui/screens/auth/components/AuthPrimaryButton.kt` — CircularProgressIndicator present
- `ui/screens/auth/components/AuthErrorMessage.kt` — liveRegion Polite présent
- `ui/theme/Color.kt` — tokens DataSaillance complets
- `ui/theme/NightfallTheme.kt` — dark + light schemes définis

### 6.4 Fichiers de tests (à compléter)

| Fichier | Statut |
|---------|--------|
| `src/test/…/ui/screens/auth/LoginScreenTest.kt` | RED existant — 4 cas Paparazzi. Passer GREEN après câblage NavGraph + ajout validation TA-L-04. |

---

## 7. Tests d'acceptation

### TA-L-01 — Affichage initial (idle state)

**Given** : l'app est lancée, `TokenDataStore.hasToken()` retourne `false`  
**When** : `NavGraph` calcule `startDestination = NavDestination.Login.route`  
**Then** : `LoginScreen` s'affiche avec champ email vide, champ password vide, bouton "Se connecter" disabled, aucun message d'erreur visible  
**Implémentation** : test Compose UI (`composeTestRule.onNodeWithTag("btn_login").assertIsNotEnabled()`)

### TA-L-02 — Login succès, navigation vers Sleep

**Given** : `LoginScreen` affiché, email et password remplis  
**When** : l'utilisateur appuie sur "Se connecter", `POST /auth/login` retourne HTTP 200 avec `access_token`  
**Then** : `TokenDataStore.saveToken()` est appelé, `loginState` passe à `Success`, `navController` navigue vers `NavDestination.Sleep.route`, la back-stack ne contient plus `Login` (popUpTo inclusive)  
**Implémentation** : test ViewModel (`AuthViewModelTest`) + test NavGraph avec `TestNavHostController`

### TA-L-03 — Erreur 401, message inline

**Given** : `LoginScreen` affiché, email et password remplis  
**When** : l'utilisateur appuie sur "Se connecter", `POST /auth/login` retourne HTTP 401  
**Then** : `loginState` passe à `Error("Email ou mot de passe incorrect")`, `AuthErrorMessage` devient visible sous les champs, aucune navigation ne se produit, les champs restent éditables  
**Implémentation** : test Compose UI (`composeTestRule.onNodeWithText("Email ou mot de passe incorrect").assertIsDisplayed()`)

### TA-L-04 — Champs vides, bouton disabled

**Given** : `LoginScreen` vient d'être ouvert, email = "", password = ""  
**When** : l'utilisateur n'a rien saisi  
**Then** : le bouton "Se connecter" est disabled (`enabled = false`)  
**Complément** : si l'un des deux champs est rempli mais pas l'autre, le bouton reste disabled  
**Implémentation** : test Compose UI + vérification dans `LoginScreen.kt` que `enabled = email.isNotBlank() && password.isNotBlank() && !isLoading`

> Note d'implémentation : l'ajout de `isFormValid` dans `LoginScreen.kt` est requis — l'implémentation actuelle ne valide pas les champs vides.

### TA-L-05 — Paparazzi golden idle state (dark + light)

**Given** : `LoginViewModel` dans l'état `Idle`, wrappé dans `NightfallTheme`  
**When** : Paparazzi snapshot sur `DeviceConfig.PIXEL_5`  
**Then** (dark) : fond `#191E22`, CTA amber `#D37C04`, liens cyan `#07BCD3` — snapshot valide vs golden  
**Then** (light) : fond `#FAFAFA`, même CTA amber — snapshot valide vs golden  
**Implémentation** : `LoginScreenTest.loginScreen_idle_dark()` et `loginScreen_idle_light()` dans le test existant  
**Note** : la police Playfair Display / Inter ne se résoud pas en JVM Paparazzi — `FontLoadingStrategy.OptionalLocal` déjà en place dans `Type.kt`, le fallback système est attendu dans les goldens.

### TA-L-06 — État Loading : champs et bouton désactivés

**Given** : `AuthViewModel.login()` appelé, réponse HTTP en attente  
**When** : `loginState == Loading`  
**Then** : les champs email et password sont `enabled = false`, le bouton affiche un `CircularProgressIndicator` à la place du texte, `contentDescription = "Chargement en cours"`  
**Implémentation** : `LoginScreenTest.loginScreen_loading_dark()` (snapshot existant) + test Compose UI sur `assertIsNotEnabled()`

---

## 8. Accessibilité

| Élément | Exigence |
|---------|----------|
| Champ email | `testTag = "field_email"`, `contentDescription = "Adresse email"` |
| Champ password | `testTag = "field_password"`, `contentDescription = "Mot de passe"` |
| Toggle visibilité password | `contentDescription` dynamique : "Afficher le mot de passe" / "Masquer le mot de passe" |
| Bouton CTA | `testTag = "btn_login"` |
| Message d'erreur | `liveRegion = LiveRegionMode.Polite` (annonce TalkBack sans interrompre) |
| Bouton Google | `testTag = "btn_google_oauth"` |
| Lien mot de passe oublié | `testTag = "link_forgot_password"` |
| Taille de cible minimale | 48dp × 48dp (Material3 guideline) — assuré par les composants Material par défaut |

---

## 9. Sécurité (C2 / C3)

| Point | Contrainte |
|-------|-----------|
| JWT stockage | `EncryptedSharedPreferences` AES-256-GCM uniquement sur device réel — pas de fallback non chiffré en production (C2) |
| Transport | TLS 1.3 via Caddy (sh-dev.datasaillance.fr) — `NightfallApi` utilise HTTPS, pas de `CLEARTEXT_TRAFFIC` |
| Logging password | Interdit — `AuthViewModel` ne logue jamais les champs `email` ni `password` en clair |
| Timeout réseau | OkHttp doit définir un `connectTimeout` et `readTimeout` (à vérifier dans `RetrofitClient`) — une erreur IOException est catchée et affichée inline |
| Rate limiting | Le backend (Phase 1) implémente le rate limiting sur `POST /auth/login` — l'app Android affiche le message d'erreur HTTP 429 comme `"Erreur serveur (429)"` (mapHttpError fallback) |
| Revue pentester | Toute PR touchant ce LoginScreen doit passer `/review` avec severity >= HIGH bloquante (C3) |

---

## 10. DI manuelle — pas de Hilt

Hilt est exclu (issue #52 : incompatibilité Kotlin 2.x kapt). L'instanciation de `AuthViewModel` dans `NavGraph.kt` suit le pattern `remember {}` avec factory explicite :

```kotlin
// Dans NavGraph.kt, composable Login :
composable(NavDestination.Login.route) {
    val context = LocalContext.current
    val viewModel = remember {
        val tokenDataStore = AppModule.provideTokenDataStore(context)
        val api = RetrofitClient.instance  // ou paramètre injecté dans NavGraph
        AuthViewModel(api, tokenDataStore)
    }
    LoginScreen(
        viewModel             = viewModel,
        onLoginSuccess        = {
            navController.navigate(NavDestination.Sleep.route) {
                popUpTo(NavDestination.Login.route) { inclusive = true }
            }
        },
        onNavigateRegister    = { navController.navigate(NavDestination.Register.route) },
        onNavigateForgotPassword = { navController.navigate(NavDestination.ForgotPassword.route) }
    )
}
```

> `NavDestination.Register` et `NavDestination.ForgotPassword` ne sont pas encore dans le sealed class — à ajouter lors du câblage (hors scope strict de cette spec mais requis pour compiler).

---

## 11. Parite dark / light mode

Les deux modes sont obligatoires (contrainte design). `NightfallTheme` expose déjà les deux color schemes. Aucune couleur hardcodée dans `LoginScreen.kt` — toutes les couleurs passent par `MaterialTheme.colorScheme.*`.

| Variable Material3 | Dark | Light |
|---|---|---|
| `background` | `#191E22` | `#FAFAFA` |
| `surface` | `#232E32` | `#FFFFFF` |
| `onBackground` | `#E8E4DC` | `#1A1916` |
| `secondary` (CTA) | `#D37C04` | `#D37C04` |
| `tertiary` (liens) | `#07BCD3` | `#07BCD3` |
| `error` | Material3 default red | Material3 default red |

---

## 12. Suite naturelle

Une fois `LoginScreen` câblé et les tests TA-L-01 à TA-L-06 GREEN :

1. **`spec-p5-dashboard-cards.md`** — premier écran de visualisation (Night Cards), désormais accessible après login
2. Compléter les destinations manquantes dans `NavDestination` : `Register`, `ForgotPassword` (nécessaires pour compiler le NavGraph complet)
3. `RegisterScreen` (`ui/screens/auth/RegisterScreen.kt`) et `ForgotPasswordScreen` existent déjà — les câbler dans le même NavGraph update

L'ordre du plan master reste : login natif → night cards → hypnogram → timeline → radial clock → metrics cards.
