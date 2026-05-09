---
title: "P5.2 Dashboard — Hypnogramme"
slug: p5-hypnogram
phase: P5
status: ready
created: 2026-05-09
branch: feat/p5-hypnogram
tags: [android, compose, sleep, hypnogram, canvas, ui, p5, native]
related_specs:
  - 2026-05-08-p5-dashboard-cards
  - 2026-04-26-nightfall-rebrand-data-saillance
  - 2026-04-23-plan-v2-refactor-master
implements:
  - android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavDestination.kt
  - android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt
  - android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/HypnogramViewModel.kt
  - android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/HypnogramScreen.kt
  - android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/HypnogramStatsSection.kt
tested_by:
  - android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/sleep/HypnogramScreenTest.kt
---

# Spec P5.2 — Hypnogramme

## Vision

`HypnogramScreen` est la visualisation centrale de Nightfall : elle affiche l'architecture d'une nuit de sommeil sous la forme d'une timeline horizontale colorée des stages (DEEP, LIGHT, REM, AWAKE). C'est la vue qui rend visibles les patterns circadiens — notamment le décalage Non-24 que 15 ans de consultations médicales n'avaient pas détecté. Elle est atteinte depuis un clic sur une `SleepNightCard` dans `SleepScreen` (P5.1), et constitue le coeur de valeur du produit.

---

## Contexte et dépendances

### Prérequis validés (P5.1 — mergé dans dev)

Tout le travail de base est déjà en place. Cette spec ne réutilise, ne modifie pas, et ne duplique rien de P5.1 — elle s'y branche.

| Élément P5.1 | Localisation | Rôle pour P5.2 |
|---|---|---|
| `SleepSessionResponse` | `data/sleep/SleepSessionResponse.kt` | Modèle de données consommé par `HypnogramViewModel` |
| `SleepStageResponse` | `data/sleep/SleepStageResponse.kt` | Chaque stage : `id`, `session_id`, `stage` (via `@SerialName("stage_type")`), `stage_start`, `stage_end` |
| `SleepRepository` / `SleepRepositoryImpl` | `data/sleep/` | Interface réutilisée telle quelle — `getSessions()` retourne déjà `stages` quand appelé avec `include_stages=true` |
| `SleepViewModel` | `viewmodel/sleep/SleepViewModel.kt` | Pas modifié — mais le `NoOpSleepRepository()` dans `NavGraph.kt` doit être remplacé par le vrai repo |
| `SleepScreen` + `SleepNightCard` | `src/native/.../sleep/` | `SleepNightCard.onClick` est déjà câblé — il faut le brancher sur `navController.navigate(NavDestination.Hypnogram.route(session.id))` |
| `NavDestination` | `ui/navigation/NavDestination.kt` | Modifier pour ajouter `Hypnogram` |
| `NavGraph` | `ui/navigation/NavGraph.kt` | Modifier pour : 1) câbler le vrai repo sur Sleep, 2) ajouter la route `hypnogram/{sessionId}`, 3) brancher l'onClick |

### Backend — pas de nouvel endpoint

`GET /api/sleep?include_stages=true` (déjà implémenté) retourne les stages dans `SleepSessionResponse.stages`. Aucune modification backend n'est nécessaire pour P5.2.

### Stratégie de passage de données : re-fetch par sessionId

La route de navigation passe uniquement le `sessionId` (String) : `hypnogram/{sessionId}`. `HypnogramViewModel` appelle `SleepRepository.getSessions()` et filtre par id. Cette stratégie est choisie pour :
- Éviter la sérialisation d'objets complexes dans les arguments de navigation (contrainte Architecture Navigation Compose)
- Garantir la fraîcheur des données (pas de désync possible avec le cache mémoire du `SleepViewModel`)
- Rester cohérent avec le pattern existant (même `SleepRepository` injecté)

Coût : un appel réseau supplémentaire à chaque ouverture de l'hypnogramme. Acceptable en V1 — pas de cache local (contrainte RGPD C2).

---

## Architecture

### Vue d'ensemble des fichiers

```
android-app/app/src/main/java/fr/datasaillance/nightfall/
├── ui/navigation/
│   ├── NavDestination.kt        [MODIFIER — ajouter Hypnogram avec helper route()]
│   └── NavGraph.kt              [MODIFIER — câbler vrai SleepRepository + route hypnogram + onClick]
└── viewmodel/sleep/
    └── HypnogramViewModel.kt    [CRÉER]

android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/
├── HypnogramScreen.kt           [CRÉER]
└── HypnogramStatsSection.kt     [CRÉER]

android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/sleep/
└── HypnogramScreenTest.kt       [CRÉER — Paparazzi + Robolectric]
```

### Règle de placement des fichiers

- `HypnogramViewModel.kt` → `src/main/java/` : partagé entre les flavors (même pattern que `SleepViewModel`).
- `HypnogramScreen.kt`, `HypnogramStatsSection.kt` → `src/native/java/` : spécifiques à la flavor native (même pattern que `SleepScreen`, `SleepNightCard`).
- `NavDestination.kt`, `NavGraph.kt` → `src/main/java/` : inchangé, déjà dans le bon répertoire.

---

## Modifications des fichiers existants

### `NavDestination.kt` — ajout de `Hypnogram`

Ajouter l'object `Hypnogram` dans la sealed class. La route utilise un paramètre `{sessionId}` conforme à la syntaxe Navigation Compose. Un helper de type fonction (pas une propriété) est nécessaire car `object` ne peut pas porter de state.

```kotlin
object Hypnogram : NavDestination("hypnogram/{sessionId}", "Hypnogramme") {
    fun route(id: String): String = "hypnogram/$id"
}
```

`Hypnogram` n'est **pas** ajouté à `bottomNavItems()` — c'est un écran de détail, pas un onglet principal.

### `NavGraph.kt` — trois modifications

**1. Remplacer `NoOpSleepRepository()` par le vrai repository pour la route Sleep.**

La route `NavDestination.Sleep.route` instancie actuellement un stub :
```kotlin
// AVANT (à supprimer)
val sleepViewModel = remember { SleepViewModel(NoOpSleepRepository()) }
SleepScreen(viewModel = sleepViewModel, onSessionClick = {})
```

Remplacer par :
```kotlin
// APRÈS
val sleepRepository: SleepRepository = remember(api, tokenDataStore) {
    if (api != null && tokenDataStore != null) {
        SleepRepositoryImpl(api, tokenDataStore)
    } else {
        NoOpSleepRepository()
    }
}
val sleepViewModel = remember(sleepRepository) { SleepViewModel(sleepRepository) }
SleepScreen(
    viewModel = sleepViewModel,
    onSessionClick = { sessionId ->
        navController.navigate(NavDestination.Hypnogram.route(sessionId))
    }
)
```

**2. Ajouter les imports nécessaires** pour `SleepRepositoryImpl`, `HypnogramViewModel`, `HypnogramScreen`.

**3. Ajouter la route `hypnogram/{sessionId}`.**

```kotlin
composable(
    route = "hypnogram/{sessionId}",
    arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
) { backStackEntry ->
    val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
    val hypnogramRepository: SleepRepository = remember(api, tokenDataStore) {
        if (api != null && tokenDataStore != null) {
            SleepRepositoryImpl(api, tokenDataStore)
        } else {
            NoOpSleepRepository()
        }
    }
    val hypnogramViewModel = remember(sessionId, hypnogramRepository) {
        HypnogramViewModel(sessionId, hypnogramRepository)
    }
    HypnogramScreen(
        viewModel = hypnogramViewModel,
        onBack = { navController.popBackStack() }
    )
}
```

Import nécessaire : `androidx.navigation.NavType`, `androidx.navigation.navArgument`.

---

## Nouveau ViewModel — `HypnogramViewModel`

### `HypnogramUiState`

Sealed class co-localisée dans `HypnogramViewModel.kt` (pas de fichier séparé — la complexité ne le justifie pas).

```kotlin
sealed class HypnogramUiState {
    object Idle    : HypnogramUiState()
    object Loading : HypnogramUiState()
    data class Success(val session: SleepSessionResponse) : HypnogramUiState()
    data class Error(val message: String) : HypnogramUiState()
}
```

Pas d'état `Empty` : si le `sessionId` ne correspond à aucune session, c'est un état `Error` avec le message `"Session introuvable"`.

### Signature et comportement

```kotlin
class HypnogramViewModel(
    private val sessionId: String,
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HypnogramUiState>(HypnogramUiState.Idle)
    val uiState: StateFlow<HypnogramUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    fun retry() = loadSession()

    private fun loadSession() { ... }

    private fun mapError(throwable: Throwable?): String { ... }
}
```

**`loadSession()` :**
1. Émet `Loading`.
2. Appelle `repository.getSessions()` (qui charge avec `include_stages=true`).
3. Filtre la liste par `id == sessionId`.
4. Si la liste est vide ou le filtre ne trouve rien → émet `Error("Session introuvable")`.
5. Si `Result.isFailure` → émet `Error(mapError(...))`.
6. Sinon → émet `Success(session)`.

**`mapError()` :** même logique que `SleepViewModel.mapError()` — 401 `"Session expirée, reconnectez-vous"`, 403 `"Accès refusé"`, `IOException` `"Vérifiez votre connexion réseau"`, autre `"Erreur serveur (code)"`.

**`init {}` :** `loadSession()` est appelé dans `init {}` pour que les tests unitaires puissent vérifier l'état sans composer de UI (cohérent avec la décision prise pour `SleepViewModel`).

---

## UI Compose — `HypnogramScreen`

### Signature

```kotlin
@Composable
fun HypnogramScreen(
    viewModel: HypnogramViewModel,
    onBack: () -> Unit
)
```

### Structure générale

```
Surface(color = MaterialTheme.colorScheme.background, modifier = fillMaxSize)
└── Scaffold
    ├── TopAppBar
    │   ├── navigationIcon: IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack) }
    │   ├── title: Text(nightTitle, style = headlineMedium)  // ex: "Nuit du Mer 7 mai"
    │   └── colors: TopAppBarDefaults.topAppBarColors(containerColor = background)
    └── content (padding innerPadding)
        ├── [Loading]  → Box(fillMaxSize, centerAlign) { CircularProgressIndicator }
        │               testTag = "hyp_loading"
        ├── [Error]    → Box(fillMaxSize, centerAlign) { Column { Text(message) + OutlinedButton("Réessayer") } }
        │               testTag = "hyp_error" / "hyp_retry"
        └── [Success]  → Column(fillMaxSize, verticalScroll(rememberScrollState()))
                         testTag = "hyp_screen"
                         ├── HypnogramSummarySection(session)    // résumé durée/horaires/score
                         ├── Spacer(16.dp)
                         ├── HypnogramCanvas(session)            // canvas Timeline
                         │   testTag = "hyp_canvas"
                         ├── Spacer(8.dp)
                         ├── HypnogramLegend()                   // 4 couleurs + labels
                         ├── Spacer(16.dp)
                         └── HypnogramStatsSection(session)      // durées et % par stage
                             testTag = "hyp_stats"
```

L'état `Idle` ne rend rien (même pattern que `SleepScreen`).

### Calcul du titre de la TopAppBar

```kotlin
private fun nightTitle(sleepStart: String): String {
    val dt = runCatching { OffsetDateTime.parse(sleepStart) }.getOrNull() ?: return ""
    // Convention : le jour de la semaine est celui de la veille (le jour où on s'est couché),
    // la date est celle de dt (le jour calendaire du début de la session).
    // Ex: session débutant le jeudi 7 mai à 23h15 → "Nuit du Mer 7 mai"
    // (on s'est couché le mercredi soir = nuit du mercredi).
    val prevDay = dt.minusDays(1)
    val dayFormatter  = DateTimeFormatter.ofPattern("EEE", Locale.FRENCH)
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
    val dayAbbr = prevDay.format(dayFormatter).replaceFirstChar { it.uppercase() }.trimEnd('.')
    val dateStr = dt.format(dateFormatter)
    return "Nuit du $dayAbbr $dateStr"  // ex: "Nuit du Mer 7 mai"
}
```

---

## Composant Canvas — `HypnogramCanvas`

### Décision : `Canvas { }` de Compose (pas Box/Row)

Un Canvas Compose (`androidx.compose.foundation.Canvas`) est utilisé pour dessiner la timeline des stages. Les raisons :
- Positionnement au pixel près selon les timestamps (impossible avec des `Box` proportionnels sans calcul flottant explicite)
- Séparation claire entre le rendu vectoriel et la hiérarchie de composants
- Contrôle total sur les graduations horaires en bas du canvas

### Signature

```kotlin
@Composable
fun HypnogramCanvas(
    session: SleepSessionResponse,
    modifier: Modifier = Modifier
)
```

Déclaré `private` dans `HypnogramScreen.kt` (composant interne non réutilisé ailleurs).

### Dimensions et layout

- `modifier = Modifier.fillMaxWidth().height(120.dp).testTag("hyp_canvas")`
- Les 120.dp se décomposent :
  - 100.dp : zone de dessin des rectangles de stages
  - 20.dp : zone des graduations horaires en bas

### Algorithme de dessin

**Préconditions :**
```kotlin
val start: OffsetDateTime = OffsetDateTime.parse(session.sleep_start)
val end: OffsetDateTime   = OffsetDateTime.parse(session.sleep_end)
val sessionDuration: Long = Duration.between(start, end).toMillis()
```

Si `sessionDuration <= 0` ou si `session.stages.isNullOrEmpty()`, le canvas dessine un rectangle plein de couleur `surface.copy(alpha = 0.3f)` comme placeholder.

**Pour chaque stage (trié par `stage_start` croissant) :**
```
stageStart = OffsetDateTime.parse(stage.stage_start)
stageEnd   = OffsetDateTime.parse(stage.stage_end)
stageDurationMs = Duration.between(stageStart, stageEnd).toMillis()

offsetMs = Duration.between(start, stageStart).toMillis()
x        = (offsetMs.toFloat() / sessionDuration) * canvasWidth
width    = (stageDurationMs.toFloat() / sessionDuration) * canvasWidth

// Hauteur et position Y selon le type de stage
if (stage.stage == "AWAKE") {
    rectHeight = 60.dp.toPx()
    rectTop    = (100.dp.toPx() - rectHeight) / 2f   // centré verticalement dans la zone de 100dp
} else {
    rectHeight = 100.dp.toPx()
    rectTop    = 0f
}

drawRect(
    color  = stageColor(stage.stage),
    topLeft = Offset(x, rectTop),
    size   = Size(width, rectHeight)
)
```

**Couleurs des stages (constantes locales dans `HypnogramScreen.kt`) :**
```kotlin
private val ColorDeep  = Color(0xFF0E9EB0)  // teal   — DEEP
private val ColorLight = Color(0xFF7A9AAA)  // muted blue-grey — LIGHT
private val ColorRem   = Color(0xFF07BCD3)  // cyan   — REM
private val ColorAwake = Color(0xFFD37C04)  // amber  — AWAKE

private fun stageColor(stageType: String): Color = when (stageType) {
    "DEEP"  -> ColorDeep
    "REM"   -> ColorRem
    "AWAKE" -> ColorAwake
    else    -> ColorLight  // "LIGHT" + tout inconnu
}
```

**Graduations horaires (axe du temps) :**

Dans la zone 20.dp du bas du canvas :
- Calculer chaque heure pleine entre `start` (arrondi à l'heure supérieure) et `end` (arrondi à l'heure inférieure).
- Pour chaque heure pleine `h` :
  - `x = (Duration.between(start, h).toMillis().toFloat() / sessionDuration) * canvasWidth`
  - Trait vertical : `drawLine(color = onSurface.copy(alpha = 0.3f), start = Offset(x, 95.dp.toPx()), end = Offset(x, 100.dp.toPx()))`
  - Label : `drawContext.canvas.nativeCanvas.drawText("HH:mm", x, 118.dp.toPx(), paint)` avec `paint.textSize = 10.sp.toPx()`, `paint.color = onSurface.copy(alpha = 0.5f).toArgb()`

---

## Composant — `HypnogramSummarySection`

Déclaré `private` dans `HypnogramScreen.kt`.

```kotlin
@Composable
private fun HypnogramSummarySection(
    session: SleepSessionResponse,
    modifier: Modifier = Modifier
)
```

**Layout :**
```
Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    Text(durationText, style = headlineMedium, color = onBackground)   // ex: "7h 23"
    Spacer(4.dp)
    Row {
        Text("Coucher $bedTime", style = bodyMedium, color = onBackground.copy(0.7f))
        Spacer(12.dp)
        Text("Réveil $wakeTime", style = bodyMedium, color = onBackground.copy(0.7f))
    }
    Spacer(4.dp)
    if (deepPct != null) {
        Text("$deepPct% sommeil profond", style = bodyMedium, color = primary)  // teal
    }
}
```

**Calculs identiques à `SleepNightCard` (P5.1) :**
- `durationText` : `Duration.between(start, end)` → `"${h}h ${mm}"`
- `bedTime` / `wakeTime` : `OffsetDateTime.parse(...).format(DateTimeFormatter.ofPattern("HH:mm"))`
- `deepPct` : si `stages != null`, `(deepMinutes * 100 / totalMinutes).toInt()`, null sinon

---

## Composant — `HypnogramLegend`

Déclaré `private` dans `HypnogramScreen.kt`.

```kotlin
@Composable
private fun HypnogramLegend(modifier: Modifier = Modifier)
```

**Layout :**
```
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp)
) {
    LegendItem(color = ColorDeep,  label = "DEEP")
    LegendItem(color = ColorLight, label = "LIGHT")
    LegendItem(color = ColorRem,   label = "REM")
    LegendItem(color = ColorAwake, label = "AWAKE")
}
```

`LegendItem` : `Row { Box(12.dp × 12.dp, background = color, shape = CircleShape) ; Spacer(4.dp) ; Text(label, bodySmall) }`

---

## Composant — `HypnogramStatsSection`

Fichier propre : `HypnogramStatsSection.kt` dans `src/native/java/.../sleep/`.

```kotlin
@Composable
fun HypnogramStatsSection(
    session: SleepSessionResponse,
    modifier: Modifier = Modifier
)
```

**Logique :** Pour chaque type de stage dans l'ordre `[DEEP, LIGHT, REM, AWAKE]`, calculer la durée cumulée des stages de ce type et le pourcentage de la durée totale de la session. N'afficher une ligne que si la durée > 0.

**Layout :**
```
Column(modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("hyp_stats")) {
    Text("Détail des phases", style = labelLarge, color = onBackground.copy(0.7f))
    Spacer(8.dp)
    // Pour chaque stage présent (durée > 0) :
    StageStatRow(color, label, durationText, pctText)
}
```

`StageStatRow` :
```
Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Box(4.dp width, 20.dp height, background = stageColor)
    Spacer(8.dp)
    Text(label, style = bodyMedium, modifier = Modifier.width(60.dp))
    Spacer(8.dp)
    Text(durationText, style = bodyMedium, color = onSurface)        // ex: "1h 45"
    Spacer(8.dp)
    Text(pctText, style = bodySmall, color = onSurface.copy(0.6f))   // ex: "23%"
}
```

**Calcul `durationText` par stage :** filtrer `session.stages` par type, sommer les durées, formater en `"Xh YY"` ou `"YY min"` si < 1h.

---

## Design tokens Compose

Réutiliser les tokens définis en P5.1. Aucune modification du thème `NightfallTheme` n'est requise.

| Rôle Material 3 | Dark mode | Light mode |
|---|---|---|
| `background` | `#191E22` | `#FAFAFA` |
| `surface` | `#232E32` | `#FFFFFF` |
| `primary` | `#0E9EB0` | `#0E9EB0` |
| `secondary` | `#D37C04` | `#D37C04` |
| `tertiary` | `#07BCD3` | `#07BCD3` |
| `onBackground` | `#FFFFFF` | `#1A1A1A` |
| `onSurface` | `#E0E0E0` | `#1A1A1A` |

### Couleurs des stages (hardcodées — pas des tokens thème)

| Stage | Couleur | HEX | Justification |
|---|---|---|---|
| DEEP | teal | `#0E9EB0` | = `primary` — stage le plus valorisé |
| LIGHT | bleu-gris muted | `#7A9AAA` | Neutre, pas dans la palette principale |
| REM | cyan | `#07BCD3` | = `tertiary` |
| AWAKE | amber | `#D37C04` | = `secondary` — signal d'alerte positif |

Les couleurs des stages sont **hardcodées** (constantes `Color(0xFF...)`) plutôt que via `MaterialTheme.colorScheme` car elles doivent être identiques en dark et light mode. Elles représentent des données physiologiques, pas des rôles UI.

### Typographie

| Style | Famille | Usage |
|---|---|---|
| `headlineMedium` | Système (Roboto) | Titre TopAppBar, durée totale |
| `bodyMedium` | Système (Roboto) | Horaires coucher/réveil, score, stats |
| `bodySmall` | Système (Roboto) | Graduations, pourcentages, légende |
| `labelLarge` | Système (Roboto) | En-têtes de sections (uppercase) |

---

## Test IDs (testTag Compose)

| Composant | testTag | Condition d'affichage |
|---|---|---|
| `Column` racine (état Success) | `hyp_screen` | état `Success` |
| `Canvas` timeline | `hyp_canvas` | état `Success` |
| `Column` stats | `hyp_stats` | état `Success` |
| Spinner / indicateur chargement | `hyp_loading` | état `Loading` |
| Bloc erreur | `hyp_error` | état `Error` |
| Bouton "Réessayer" | `hyp_retry` | état `Error` |

Application via `Modifier.testTag("...")` (pattern consistant avec `SleepScreen`).

---

## Tests d'acceptation

### TA-H-01 — Navigation depuis `SleepNightCard` vers `HypnogramScreen`

**Given** `SleepScreen` est composé avec un `SleepViewModel` en état `Success` contenant au moins une session (`id = "aaa-111"`),  
**when** l'utilisateur clique sur la `SleepNightCard` de cette session (testTag `sleep_card_aaa-111`),  
**then** `navController.navigate("hypnogram/aaa-111")` est appelé, et le composant `testTag = "hyp_screen"` devient visible.

### TA-H-02 — Titre de la TopAppBar

**Given** `HypnogramScreen` est composé avec un `HypnogramViewModel` en état `Success` pour une session avec `sleep_start = "2026-05-07T23:15:00+02:00"`,  
**when** le composant est rendu,  
**then** la `TopAppBar` affiche le texte `"Nuit du Mer 7 mai"` (calcul `OffsetDateTime` → `DateTimeFormatter` locale FRENCH).

### TA-H-03 — Canvas visible en état Success

**Given** `HypnogramScreen` est composé avec un `HypnogramViewModel` en état `Success` contenant une session avec `stages` non null et non vide,  
**when** le composant est rendu,  
**then** le composant `testTag = "hyp_canvas"` existe dans la hiérarchie de composants.

### TA-H-04 — Section stats liste les stages présents

**Given** une session avec des stages DEEP (90 min), LIGHT (180 min), REM (60 min), AWAKE (30 min),  
**when** `HypnogramStatsSection` est rendu,  
**then** `testTag = "hyp_stats"` est visible et contient exactement 4 lignes (une par type de stage), chacune avec la durée correspondante en minutes ou heures.

### TA-H-05 — État Loading → spinner visible

**Given** `HypnogramViewModel` est en état `Loading` (appel réseau en cours),  
**when** `HypnogramScreen` est composé,  
**then** `testTag = "hyp_loading"` est visible, `testTag = "hyp_screen"` n'existe pas, `testTag = "hyp_canvas"` n'existe pas.

### TA-H-06 — État Error → message + retry

**Given** `HypnogramViewModel` est en état `Error("Vérifiez votre connexion réseau")`,  
**when** `HypnogramScreen` est composé,  
**then** `testTag = "hyp_error"` est visible avec le message d'erreur, `testTag = "hyp_retry"` est visible, `testTag = "hyp_screen"` n'existe pas. Un clic sur `hyp_retry` déclenche `HypnogramViewModel.retry()`, ce qui remet l'état à `Loading`.

### TA-H-07 — Paparazzi golden dark mode

**Given** `HypnogramScreen` composé sous `NightfallTheme(darkTheme = true)` avec un `HypnogramViewModel` en état `Success`, session contenant 4 types de stages (DEEP, LIGHT, REM, AWAKE),  
**when** Paparazzi génère un screenshot sur `DeviceConfig.PIXEL_5`,  
**then** le screenshot correspond au golden de référence (fond `#191E22`, canvas visible avec les 4 couleurs de stages).

### TA-H-08 — Paparazzi golden light mode

**Given** même état `Success` que TA-H-07,  
**when** Paparazzi génère un screenshot sous `NightfallTheme(darkTheme = false)`,  
**then** le screenshot correspond au golden de référence (fond `#FAFAFA`, canvas visible avec les mêmes 4 couleurs de stages — inchangées en light mode).

### TA-H-09 — Edge case : session sans stages

**Given** une session avec `stages = null`,  
**when** `HypnogramCanvas` et `HypnogramStatsSection` sont composés,  
**then** `hyp_canvas` est visible mais affiche un placeholder (rectangle muted), `hyp_stats` n'affiche aucune ligne de stage, la section summary n'affiche pas de score qualité.

### TA-H-10 — Edge case : sessionId introuvable

**Given** `HypnogramViewModel` reçoit un `sessionId` qui n'est présent dans aucune session retournée par le repository,  
**when** `loadSession()` termine,  
**then** `uiState` est `Error("Session introuvable")`.

### TA-H-11 — Edge case : durée de session nulle ou négative

**Given** une session avec `sleep_start == sleep_end` (ou `sleep_end` avant `sleep_start`),  
**when** `HypnogramCanvas` est composé,  
**then** aucune exception n'est levée et le composant affiche le placeholder sans planter.

### TA-H-12 — Bouton back de la TopAppBar

**Given** `HypnogramScreen` est composé avec `onBack = { capturedBack = true }`,  
**when** l'utilisateur clique sur l'icône flèche retour dans la TopAppBar,  
**then** `capturedBack` est `true` (le callback `onBack` a bien été appelé).

---

## Logging Android (Timber)

Aucune valeur de santé brute dans les logs (conformité C2 : pas de `sleep_start`, pas de `stage_start`, pas de durées en clair).

| Événement | Niveau | Champs |
|---|---|---|
| Début chargement session hypnogramme | `d` | `scope=hypno_vm`, `session_id` (UUID — pas de donnée de santé) |
| Chargement réussi | `d` | `scope=hypno_vm`, `stage_count=N` |
| Session introuvable après filtre | `w` | `scope=hypno_vm`, `session_id` |
| Erreur HTTP | `w` | `scope=hypno_vm`, `http_code=N` |
| Erreur réseau | `w` | `scope=hypno_vm`, `error=IOException` |
| Clic bouton back | `d` | `scope=hypno_screen` |

---

## RGPD

- **Aucun cache local** des données de session côté Android en V1 — pas de Room, pas de SharedPreferences pour les stages. Chaque ouverture de l'hypnogramme effectue un appel réseau frais (contrainte C2).
- Le `sessionId` transmis via la route de navigation est un UUID opaque — pas une donnée de santé en soi.
- L'UUID `session_id` apparaissant dans les logs est considéré comme un identifiant technique (pas une donnée de santé directe). Il ne révèle pas la date, la durée ni le contenu du sommeil.
- Les données sont filtrées côté backend par `user_id` via `Depends(get_current_user)` — l'utilisateur ne peut accéder qu'à ses propres sessions.
- Le token JWT est stocké dans `EncryptedSharedPreferences` via `TokenDataStore` — chiffrement AES-256-GCM au repos, conforme C2.

---

## Décisions techniques

| Décision | Choix | Raison |
|---|---|---|
| Passage de données nav | `sessionId` via route arg + re-fetch | Évite sérialisation d'objets complexes dans nav args ; conformité avec Architecture Navigation Compose |
| Rendu timeline | `Canvas { }` Compose | Positionnement au pixel près selon timestamps ; séparation rendu vectoriel / hiérarchie composants |
| AWAKE plus fin | hauteur 60.dp centrée vs 100.dp | Distinction visuelle : l'éveil n'est pas un stage de sommeil — le réduire visuellement ancre cette différence sémantique |
| Couleurs hardcodées | `Color(0xFF...)` vs `MaterialTheme.colorScheme` | Les couleurs représentent des données physiologiques — elles doivent être stables en dark ET light mode |
| `HypnogramUiState` co-localisé | Dans `HypnogramViewModel.kt` | Pas de complexité justifiant un fichier séparé ; cohérence avec la taille du domaine |
| `init {}` pour `loadSession()` | `init {}` vs `LaunchedEffect` | Simplifie les tests unitaires (pas besoin de Compose pour tester le chargement) |
| `HypnogramStatsSection` fichier propre | `HypnogramStatsSection.kt` | Composant testable isolément (Paparazzi partiel) ; lisibilité du fichier `HypnogramScreen.kt` |

---

## Livrables

- [ ] `NavDestination.kt` — ajout `object Hypnogram` avec `fun route(id: String)`
- [ ] `NavGraph.kt` — remplacement `NoOpSleepRepository` sur route Sleep + ajout route `hypnogram/{sessionId}` + câblage `onSessionClick → navigate`
- [ ] `HypnogramViewModel.kt` — `HypnogramUiState` (sealed) + `HypnogramViewModel` (init, loadSession, retry, mapError)
- [ ] `HypnogramScreen.kt` — `HypnogramScreen` + `HypnogramCanvas` + `HypnogramSummarySection` + `HypnogramLegend` (composants privés internes)
- [ ] `HypnogramStatsSection.kt` — composant public `HypnogramStatsSection` + `StageStatRow` interne
- [ ] `HypnogramScreenTest.kt` — classes `HypnogramSnapshotTest` (Paparazzi TA-H-07/08), `HypnogramInteractionTest` (Robolectric TA-H-01/02/03/04/05/06/09/10/11/12)

---

## Suite naturelle

**P5.3 — Timeline circadienne** : vue multi-nuits sur un axe du temps commun, permettant de visualiser la dérive circadienne caractéristique du Non-24. Les données et l'architecture de `SleepRepository` sont déjà compatibles — il s'agira d'agréger plusieurs `SleepSessionResponse` sur un même canvas.
