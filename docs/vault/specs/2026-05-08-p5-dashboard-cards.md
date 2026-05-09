---
title: "P5.1 Dashboard — Sleep Night Cards"
slug: 2026-05-08-p5-dashboard-cards
phase: P5
status: ready
created: 2026-05-08
branch: feat/p5-dashboard-cards
tags: [android, compose, sleep, dashboard, ui, p5, native]
related_specs:
  - 2026-04-26-nightfall-rebrand-data-saillance
  - 2026-04-23-plan-v2-refactor-master
implements:
  - android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepScreen.kt
  - android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt
  - android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepository.kt
  - android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/SleepViewModel.kt
  - android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepNightCard.kt
tested_by:
  - android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepScreenTest.kt
---

# Spec P5.1 — Sleep Night Cards

## Vision

SleepScreen est la première vue post-login de Nightfall. Elle présente la liste chronologique des nuits importées, une carte ("Night Card") par session de sommeil. L'objectif est de donner à l'utilisateur une lecture d'un coup d'oeil de l'ensemble de son historique : durée, heure de coucher et réveil, et un indicateur coloré de qualité. C'est la porte d'entrée vers le détail hypnogramme (P5.2). La vue remplace intégralement le stub `SleepScreen.kt` livré en P4.

---

## Contexte et dépendances

### Prérequis validés

- **P5.0 LoginScreen natif** : terminé. Flow `POST /auth/login` → `TokenDataStore.saveToken()` → navigation vers `SleepScreen` est fonctionnel.
- **`NightfallApi`** : interface Retrofit existante dans `fr.datasaillance.nightfall.data.http`. À étendre (ne pas réécrire).
- **`TokenDataStore`** : classe existante dans `fr.datasaillance.nightfall.data.auth`. Fournit `getToken(): String?`. Lecture en constructeur `SleepRepository`.
- **Build flavor `native`** : `SleepScreen.kt` ciblé est dans `src/native/java/`. Le stub webview (`src/webview/java/`) n'est pas modifié par cette spec.
- **Pas de Hilt** : DI manuelle par constructeur, pattern `AuthViewModel(api, tokenDataStore)`.

### Backend — endpoint `GET /api/sleep`

Implémenté dans `server/routers/sleep.py`. Contrat :

```
GET /api/sleep
Headers: Authorization: Bearer <jwt>
Query params (optionnels):
  from=<ISO date string>       ex: 2026-01-01
  to=<ISO date string>         ex: 2026-05-08
  include_stages=true|false    default: false

Response 200: list[SleepSessionOut]
Response 401: non authentifié
Response 403: email non vérifié
```

`SleepSessionOut` (Pydantic côté serveur) :

| Champ | Type | Notes |
|-------|------|-------|
| `id` | `str` (UUID) | identifiant unique |
| `sleep_start` | `str` ISO 8601 | ex: `"2026-05-07T23:15:00+02:00"` |
| `sleep_end` | `str` ISO 8601 | ex: `"2026-05-08T06:38:00+02:00"` |
| `created_at` | `str` ISO 8601 ou `null` | date d'import |
| `stages` | `list[SleepStageOut]` ou `null` | présent seulement si `include_stages=true` |

`SleepStageOut` :

| Champ | Type | Notes |
|-------|------|-------|
| `id` | `str` (UUID) | |
| `session_id` | `str` (UUID) | |
| `stage_type` | `str` | valeurs Samsung Health : `"DEEP"`, `"LIGHT"`, `"REM"`, `"AWAKE"` |
| `stage_start` | `str` ISO 8601 | |
| `stage_end` | `str` ISO 8601 | |

**V1 : `include_stages=true` est requis** pour calculer le score de qualité (% sommeil profond). Pas de pagination — tout charger en une requête.

---

## Architecture

### Vue d'ensemble des fichiers à créer

```
android-app/app/src/main/java/fr/datasaillance/nightfall/
├── data/
│   ├── http/
│   │   └── NightfallApi.kt              [MODIFIER — ajouter getSleepSessions()]
│   └── sleep/
│       ├── SleepRepository.kt           [CRÉER — interface]
│       └── SleepRepositoryImpl.kt       [CRÉER — implémentation Retrofit]
└── viewmodel/
    └── sleep/
        ├── SleepViewModel.kt            [CRÉER]
        └── SleepUiState.kt              [CRÉER — sealed class]

android-app/app/src/native/java/fr/datasaillance/nightfall/
└── ui/screens/sleep/
    ├── SleepScreen.kt                   [REMPLACER le stub]
    └── SleepNightCard.kt                [CRÉER — composant carte]

android-app/app/src/test/java/fr/datasaillance/nightfall/
└── ui/screens/sleep/
    └── SleepScreenTest.kt               [CRÉER — tests TDD + Paparazzi]
```

### Règle de placement des fichiers

- `SleepRepository`, `SleepRepositoryImpl`, `SleepViewModel`, `SleepUiState` → `src/main/java/` (partagé entre flavors webview et native).
- `SleepScreen.kt`, `SleepNightCard.kt` → `src/native/java/` (flavor native uniquement). Le stub webview reste intact.

---

## Modèles Kotlin

### `SleepSessionResponse` et `SleepStageResponse`

Placés dans `fr.datasaillance.nightfall.data.sleep` (package dédié).

```kotlin
@kotlinx.serialization.Serializable
data class SleepStageResponse(
    val id: String,
    val session_id: String,
    val stage_type: String,
    val stage_start: String,
    val stage_end: String
)

@kotlinx.serialization.Serializable
data class SleepSessionResponse(
    val id: String,
    val sleep_start: String,
    val sleep_end: String,
    val created_at: String?,
    val stages: List<SleepStageResponse>?
)
```

Contraintes :
- `@Serializable` de `kotlinx.serialization` — cohérent avec le reste du projet (pattern `ImportApiResponse` dans `NightfallApi.kt`).
- Noms de champs en `snake_case` pour correspondre directement au JSON backend (pas de `@SerialName` nécessaire si le serializer est configuré avec la stratégie snake_case, sinon utiliser `@SerialName`).
- Ces data classes ne contiennent pas de logique — la transformation métier se fait dans le ViewModel.

---

## Extension de `NightfallApi`

Ajouter dans l'interface existante `NightfallApi` :

```kotlin
@GET("api/sleep")
suspend fun getSleepSessions(
    @Header("Authorization") token: String,
    @Query("from") from: String? = null,
    @Query("to") to: String? = null,
    @Query("include_stages") includeStages: Boolean = true
): List<SleepSessionResponse>
```

Notes :
- Le token est passé en header `Authorization: Bearer <token>`. La valeur transmise par le ViewModel doit inclure le préfixe `"Bearer "`.
- `includeStages = true` par défaut en V1 pour permettre le calcul du score qualité.
- Pas de `Response<>` wrapper — les erreurs HTTP sont catchées via `retrofit2.HttpException` dans le Repository (pattern `AuthViewModel`).

---

## Repository

### Interface `SleepRepository`

```kotlin
interface SleepRepository {
    suspend fun getSleepSessions(): Result<List<SleepSessionResponse>>
}
```

### `SleepRepositoryImpl`

- Constructeur : `(private val api: NightfallApi, private val tokenDataStore: TokenDataStore)`
- Lit le token via `tokenDataStore.getToken()` avant chaque appel.
- Si le token est null : retourne `Result.failure(IllegalStateException("No token"))`.
- Formate le header : `"Bearer $token"`.
- Try/catch `retrofit2.HttpException` et `java.io.IOException` → `Result.failure(...)`.
- Succès : `Result.success(response)`.

Pas de cache en V1. Pas de pagination. L'appel charge toutes les sessions disponibles.

---

## ViewModel

### `SleepUiState`

```kotlin
sealed class SleepUiState {
    object Idle : SleepUiState()
    object Loading : SleepUiState()
    data class Success(val sessions: List<SleepSessionResponse>) : SleepUiState()
    data class Error(val message: String) : SleepUiState()
    object Empty : SleepUiState()
}
```

`Empty` est un état distinct de `Success(emptyList())` pour faciliter le rendu d'un message dédié.

### `SleepViewModel`

```kotlin
class SleepViewModel(
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SleepUiState>(SleepUiState.Idle)
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    fun loadSessions() { ... }
    fun retry() = loadSessions()
}
```

- `loadSessions()` émet `Loading`, appelle `repository.getSleepSessions()`, puis émet :
  - `Empty` si la liste résultante est vide.
  - `Success(sessions)` trié par `sleep_start` décroissant (nuit la plus récente en premier).
  - `Error(message)` si `Result.isFailure`.
- `retry()` est un alias public de `loadSessions()` — exposé pour que le bouton "Réessayer" du composant `SleepScreen` l'appelle directement.
- Mappage des erreurs HTTP : 401 → `"Session expirée — reconnectez-vous"`, 403 → `"Accès refusé"`, toute autre `HttpException` → `"Erreur serveur (code)"`, `IOException` → `"Vérifiez votre connexion réseau"`.
- `loadSessions()` est appelé automatiquement depuis `init {}` (ou depuis `LaunchedEffect(Unit)` dans le composant — à décider à l'impl ; les deux sont acceptables mais `init {}` simplifie les tests unitaires).

---

## UI Compose

### `SleepScreen` (remplace le stub)

Signature :

```kotlin
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    onSessionClick: (sessionId: String) -> Unit
)
```

Structure générale :

```
Surface(color = MaterialTheme.colorScheme.background)
└── Scaffold
    ├── TopAppBar — titre "Mes nuits" (headlineLarge)
    └── content
        ├── [Loading]  → CircularProgressIndicator centré
        │               testTag = "sleep_loading"
        ├── [Empty]    → colonne centrée : icône lune + texte "Aucune nuit importée"
        │               testTag = "sleep_empty"
        ├── [Error]    → colonne : message erreur + OutlinedButton "Réessayer"
        │               testTag = "sleep_error" / "sleep_retry"
        └── [Success]  → LazyColumn
                        testTag = "sleep_list"
                        items = sessions
                        item → SleepNightCard(session, onClick)
                               testTag = "sleep_card_{session.id}"
```

### `SleepNightCard`

Signature :

```kotlin
@Composable
fun SleepNightCard(
    session: SleepSessionResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

#### Layout de la carte

```
Card(
  shape = RoundedCornerShape(12.dp),
  colors = CardDefaults.cardColors(containerColor = surface),  // #232E32 dark / #FFFFFF light
  modifier = Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=6.dp)
)
└── Row(modifier = Modifier.padding(16.dp))
    ├── Box(width=6.dp, fillMaxHeight) — barre de couleur indicateur durée (voir §Indicateur)
    ├── Spacer(8.dp)
    └── Column(modifier = Modifier.weight(1f))
        ├── Text — label nuit ex: "Lun 5 mai"     style=labelLarge (13sp 500, uppercase)
        ├── Text — durée ex: "7h 23"               style=headlineMedium (22sp 600)
        ├── Spacer(4.dp)
        └── Row
            ├── Text — "Coucher 23:15"             style=bodySmall (14sp 400)
            ├── Spacer(8.dp)
            ├── Text — "Réveil 06:38"              style=bodySmall (14sp 400)
            └── [si score disponible] Text — "% Profond 28%"  style=bodySmall, color=teal
```

#### Calcul des valeurs affichées

Toutes les transformations sont effectuées côté client à partir des ISO strings reçus du backend.

| Valeur affichée | Source | Calcul |
|-----------------|--------|--------|
| Label nuit | `sleep_start` | Date de `sleep_start` formatée `"EEE d MMM"` en français, ex: `"Lun 5 mai"`. Utiliser `java.time.format.DateTimeFormatter` avec `Locale.FRENCH`. |
| Durée | `sleep_start` + `sleep_end` | `Duration.between(parseIso(sleep_start), parseIso(sleep_end))`. Afficher `"7h 23"` (heures + minutes, pas de secondes). |
| Heure coucher | `sleep_start` | `HH:mm` de `sleep_start`. |
| Heure réveil | `sleep_end` | `HH:mm` de `sleep_end`. |
| Score qualité | `stages` | Si `stages != null && stages.isNotEmpty()` : `% = (durée cumulée stages DEEP / durée totale session) * 100`, arrondi à l'entier. Affiché seulement si le score est calculable. |

Parsing ISO : utiliser `java.time.OffsetDateTime.parse(isoString)` (gère les offsets `+02:00`, `Z`, etc.).

#### Indicateur coloré (barre gauche de la carte)

| Durée totale | Couleur | Token |
|--------------|---------|-------|
| >= 7h | Teal | `#0E9EB0` (`MaterialTheme.colorScheme.primary`) |
| >= 5h et < 7h | Amber | `#D37C04` (`MaterialTheme.colorScheme.secondary`) |
| < 5h | Rouge/muted | `Color(0xFFB00020)` (erreur Material — pas de token custom nécessaire) |

La barre a une largeur fixe de 6 dp et s'étend sur toute la hauteur de la carte (via `fillMaxHeight` dans un `Row`).

---

## Design tokens Compose

### Palette Material 3

Les tokens suivants doivent être mappés dans les thèmes `LightColorScheme` / `DarkColorScheme` de l'app (déjà partiellement défini depuis P4/P5.0) :

| Rôle Material 3 | Dark mode | Light mode |
|-----------------|-----------|------------|
| `background` | `#191E22` | `#FAFAFA` |
| `surface` | `#232E32` | `#FFFFFF` |
| `primary` | `#0E9EB0` | `#0E9EB0` |
| `secondary` | `#D37C04` | `#D37C04` |
| `tertiary` | `#07BCD3` | `#07BCD3` |
| `onBackground` | `#FFFFFF` | `#1A1A1A` |
| `onSurface` | `#E0E0E0` | `#1A1A1A` |

### Typographie Compose

| Style Material 3 | Famille | Taille | Poids |
|------------------|---------|--------|-------|
| `headlineLarge` | Système (Roboto) | 32sp | 700 |
| `headlineMedium` | Système (Roboto) | 22sp | 600 |
| `labelLarge` | Système (Roboto) | 13sp | 500 |
| `bodySmall` | Système (Roboto) | 14sp | 400 |

Les fichiers de polices doivent être copiés dans `android-app/app/src/main/assets/fonts/` (depuis `~/MyPersonalProjects/Vectorizer/IdentiteVisuelle/Polices/`) si ce n'est pas déjà fait par P5.0.

---

## Test IDs (testTag Compose)

| Composant | testTag | Condition d'affichage |
|-----------|---------|----------------------|
| `CircularProgressIndicator` | `sleep_loading` | état `Loading` |
| `LazyColumn` liste | `sleep_list` | état `Success` |
| Chaque `SleepNightCard` | `sleep_card_{session.id}` | état `Success`, une entrée par session |
| Message erreur (column) | `sleep_error` | état `Error` |
| Bouton "Réessayer" | `sleep_retry` | état `Error` |
| Message vide (column) | `sleep_empty` | état `Empty` |

Application via `Modifier.semantics { testTag = "..." }` (pattern existant dans `LoginScreen.kt`).

---

## Tests d'acceptation

### TA-S-01 — Chargement initial

**Given** l'utilisateur est authentifié et `SleepScreen` vient d'être composé,  
**when** `SleepViewModel.loadSessions()` est en cours d'exécution,  
**then** le composant avec `testTag = "sleep_loading"` est visible et `testTag = "sleep_list"` n'existe pas.

### TA-S-02 — Liste chargée avec succès

**Given** le repository retourne 3 sessions de sommeil,  
**when** `uiState` passe à `Success(sessions)`,  
**then** le composant `testTag = "sleep_list"` est visible et contient exactement 3 éléments avec les testTags `sleep_card_{id}` correspondants, triés par date décroissante (plus récent en premier).

### TA-S-03 — Contenu d'une carte

**Given** une session avec `sleep_start = "2026-05-07T23:15:00+02:00"` et `sleep_end = "2026-05-08T06:38:00+02:00"`,  
**when** `SleepNightCard` est rendu,  
**then** le texte `"Mer 7 mai"` est visible, la durée `"7h 23"` est visible, `"23:15"` est visible, `"06:38"` est visible.

### TA-S-04 — Calcul de durée correct

**Given** `sleep_start = "2026-05-07T22:00:00Z"` et `sleep_end = "2026-05-08T05:30:00Z"`,  
**when** la carte est rendue,  
**then** la durée affichée est `"7h 30"` (calcul côté client, pas de valeur pré-calculée du backend).

### TA-S-05 — Indicateur coloré selon durée

**Given** trois sessions avec durées respectives 7h30, 6h00, 4h45,  
**when** les cartes sont rendues,  
**then** la barre indicatrice de la première carte a la couleur teal `#0E9EB0`, la seconde amber `#D37C04`, la troisième rouge `#B00020`.

### TA-S-06 — Score qualité affiché si stages disponibles

**Given** une session avec `stages` non null contenant des stages dont 90 minutes de type `"DEEP"` sur une durée totale de 450 minutes,  
**when** la carte est rendue,  
**then** le texte `"Profond 20%"` (ou équivalent arrondi) est visible dans la carte.

### TA-S-07 — État erreur avec retry

**Given** le repository retourne `Result.failure(IOException())`,  
**when** `uiState` passe à `Error("Vérifiez votre connexion réseau")`,  
**then** le composant `testTag = "sleep_error"` est visible avec le message d'erreur, le bouton `testTag = "sleep_retry"` est visible et un clic sur ce bouton déclenche un nouvel appel à `SleepViewModel.loadSessions()`.

### TA-S-08 — État vide (aucune session)

**Given** le repository retourne `Result.success(emptyList())`,  
**when** `uiState` passe à `Empty`,  
**then** le composant `testTag = "sleep_empty"` est visible avec un message indiquant qu'aucune nuit n'est importée, `testTag = "sleep_list"` n'existe pas.

### TA-S-09 — Snapshot Paparazzi dark mode / état Success

**Given** `SleepScreen` composé avec un `SleepViewModel` en état `Success` (3 sessions fictives),  
**when** Paparazzi génère un screenshot avec le thème dark (`NightfallDarkTheme`),  
**then** le screenshot correspond au golden de référence à ±0% (régression bloquante).

### TA-S-10 — Snapshot Paparazzi light mode / état Success

**Given** même state `Success` que TA-S-09,  
**when** Paparazzi génère un screenshot avec le thème light (`NightfallLightTheme`),  
**then** le screenshot correspond au golden de référence (fond `#FAFAFA`, surface blanche, texte sombre).

### TA-S-11 — Snapshot Paparazzi / état Loading

**Given** `SleepViewModel` en état `Loading`,  
**when** Paparazzi screenshot en dark mode,  
**then** seul `CircularProgressIndicator` (ou skeleton) est visible — pas de liste, pas d'erreur.

### TA-S-12 — Snapshot Paparazzi / état Error

**Given** `SleepViewModel` en état `Error("Vérifiez votre connexion réseau")`,  
**when** Paparazzi screenshot en dark mode,  
**then** le message d'erreur et le bouton "Réessayer" sont visibles, pas de liste.

---

## Logging Android

Utiliser `Timber` (déjà intégré en Phase 4). Événements à logger :

| Événement | Niveau | Champs |
|-----------|--------|--------|
| Début chargement sessions | `d` | `scope=sleep_vm` |
| Chargement réussi | `d` | `scope=sleep_vm`, `count=N` |
| Erreur HTTP | `w` | `scope=sleep_vm`, `http_code=N`, `message` |
| Erreur réseau | `w` | `scope=sleep_vm`, `error=IOException` |
| Clic sur une carte | `d` | `scope=sleep_screen`, `session_id` |

Aucun champ de valeur santé brut (pas de `sleep_start`, pas de `sleep_end` dans les logs) — conformité C2.

---

## RGPD

- `SleepScreen` affiche uniquement des données propres à l'utilisateur authentifié — le backend filtre par `user_id` via `Depends(get_current_user)`.
- Aucun cache local des données de liste (pas de Room, pas de SharedPreferences pour les sessions) en V1 — les données ne persistent pas côté Android au-delà de la session mémoire.
- Aucune donnée de santé n'apparaît dans les logs (voir §Logging).
- Le token JWT est lu depuis `EncryptedSharedPreferences` via `TokenDataStore` — chiffrement AES-256-GCM au repos conforme C2.

---

## Livrables

- [ ] `NightfallApi.kt` — ajout `getSleepSessions()`
- [ ] `SleepSessionResponse.kt` + `SleepStageResponse.kt` — data classes @Serializable
- [ ] `SleepRepository.kt` — interface
- [ ] `SleepRepositoryImpl.kt` — implémentation Retrofit
- [ ] `SleepUiState.kt` — sealed class
- [ ] `SleepViewModel.kt` — StateFlow + loadSessions() + retry()
- [ ] `SleepNightCard.kt` — composant carte (src/native/)
- [ ] `SleepScreen.kt` — remplacement du stub (src/native/) avec tous les états
- [ ] `SleepScreenTest.kt` — tests unitaires ViewModel + Paparazzi goldens

---

## Suite naturelle

**P5.2 — Hypnogramme** (`spec-p5-dashboard-hypnogram`) : clic sur une `SleepNightCard` navigue vers `HypnogramScreen(sessionId)`. L'endpoint `GET /api/sleep` avec `include_stages=true` fournit déjà les données nécessaires — elles pourront être passées directement via la navigation ou rechargées à la demande depuis `HypnogramViewModel`.
