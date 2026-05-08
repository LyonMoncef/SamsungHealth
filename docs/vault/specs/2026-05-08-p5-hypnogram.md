---
title: "P5.2 Hypnogramme"
slug: 2026-05-08-p5-hypnogram
phase: P5
status: ready
created: 2026-05-08
branch: feat/p5-hypnogram
tags: [android, compose, sleep, hypnogram, canvas, dashboard, ui, p5, native]
related_specs:
  - 2026-05-08-p5-dashboard-cards
  - 2026-04-23-plan-v2-refactor-master
implements:
  - android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavDestination.kt
  - android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt
  - android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepScreen.kt
  - android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/hypnogram/HypnogramScreen.kt
  - android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/hypnogram/HypnogramCanvas.kt
tested_by:
  - android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/hypnogram/HypnogramScreenTest.kt
---

# Spec P5.2 — Hypnogramme

## Vision

`HypnogramScreen` est la vue détail d'une nuit de sommeil, accessible par un tap sur une `SleepNightCard`. Elle affiche visuellement l'enchaînement des phases de sommeil sous forme d'un hypnogramme horizontal (Canvas Compose), accompagné des KPIs de durée par stage. C'est la vue qui donne à l'utilisateur la lecture des patterns circadiens que Nightfall a été conçu pour révéler — une nuit de Non-24 se lit immédiatement dans la répartition des blocs colorés. Aucun appel réseau supplémentaire n'est effectué : les stages sont déjà en mémoire depuis le chargement P5.1 avec `include_stages=true`.

---

## Contexte et dépendances

### Prérequis validés (P5.1)

- `SleepScreen` et `SleepNightCard` sont livrés et verts (spec `2026-05-08-p5-dashboard-cards`).
- `SleepViewModel` expose un `StateFlow<SleepUiState>`. L'état `SleepUiState.Success` contient `sessions: List<SleepSessionResponse>`, chaque session ayant `stages: List<SleepStageResponse>?` déjà chargé (`include_stages=true`).
- `SleepSessionResponse` et `SleepStageResponse` sont définis dans `fr.datasaillance.nightfall.data.sleep`. `SleepStageResponse` utilise `@SerialName("stage_type") val stage: String`.
- `NavDestination` est une `sealed class` dans `fr.datasaillance.nightfall.ui.navigation`. Pas de `Hypnogram` pour l'instant — à ajouter.
- `NavGraph` passe actuellement `onSessionClick = {}` (no-op) à `SleepScreen` — à corriger.
- Pas de Hilt : DI manuelle par constructeur. `SleepViewModel` est instancié via `remember` dans `NavGraph`.

### Pas de nouveau endpoint backend

Le backend `GET /api/sleep?include_stages=true` fournit déjà tous les stages en P5.1. P5.2 ne touche pas au backend.

---

## Architecture

### Fichiers à modifier

| Fichier | Action |
|---------|--------|
| `ui/navigation/NavDestination.kt` | MODIFIER — ajouter `object Hypnogram` |
| `ui/navigation/NavGraph.kt` | MODIFIER — ajouter la route Hypnogram, corriger `onSessionClick` |
| `ui/screens/sleep/SleepScreen.kt` | MODIFIER — `onSessionClick` est déjà câblé dans `SleepNightCard`; la logique de navigation est dans NavGraph |

### Fichiers à créer

```
android-app/app/src/native/java/fr/datasaillance/nightfall/
└── ui/screens/hypnogram/
    ├── HypnogramScreen.kt    [CRÉER — écran complet]
    └── HypnogramCanvas.kt    [CRÉER — composant Canvas pur]

android-app/app/src/test/java/fr/datasaillance/nightfall/
└── ui/screens/hypnogram/
    └── HypnogramScreenTest.kt  [CRÉER — tests TDD + Paparazzi]
```

### Règle de placement

- `HypnogramScreen.kt` et `HypnogramCanvas.kt` → `src/native/java/` (flavor native uniquement, cohérent avec `SleepScreen.kt`).
- Aucun nouveau fichier dans `src/main/java/` : le ViewModel et les modèles utilisés sont ceux de P5.1.

---

## Navigation

### `NavDestination.Hypnogram`

Ajouter dans la `sealed class NavDestination` :

```kotlin
object Hypnogram : NavDestination("hypnogram/{sessionId}", "Hypnogramme") {
    fun route(sessionId: String) = "hypnogram/$sessionId"
}
```

La route paramétrée utilise un argument de chemin `{sessionId}` (pas un query param) pour être conforme au pattern Navigation Compose.

`bottomNavItems()` ne doit PAS inclure `Hypnogram` — c'est une destination de détail sans onglet bottom nav.

### Modification `NavGraph`

Deux changements dans `NavGraph.kt` :

**1. Corriger `onSessionClick` sur la route Sleep :**

```kotlin
composable(NavDestination.Sleep.route) {
    val sleepViewModel = remember { SleepViewModel(NoOpSleepRepository()) }
    SleepScreen(
        viewModel = sleepViewModel,
        onSessionClick = { sessionId ->
            navController.navigate(NavDestination.Hypnogram.route(sessionId))
        }
    )
}
```

Note : le `SleepViewModel` ici utilise un `NoOpSleepRepository` dans le contexte `NavGraph` (pattern existant). En pratique, dans `MainActivity`, un vrai `SleepRepositoryImpl` sera injecté. La correction de `onSessionClick` est la seule modification sémantique de cette route.

**2. Ajouter la route Hypnogram :**

```kotlin
composable(
    route = NavDestination.Hypnogram.route,
    arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
) { backStackEntry ->
    val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
    // Le sleepViewModel doit être le MÊME que celui de SleepScreen pour accéder aux sessions en mémoire.
    // Pattern : hoist via NavGraph parameter ou remember partagé au niveau du graphe.
    // Voir §Architecture ViewModel partagé ci-dessous.
    HypnogramScreen(
        sessionId = sessionId,
        sleepViewModel = /* sleepViewModel hoisted */,
        onBack = { navController.popBackStack() }
    )
}
```

### Architecture ViewModel partagé

`HypnogramScreen` doit accéder aux sessions déjà chargées par `SleepViewModel`. Deux approches acceptables :

**Approche retenue (simple, cohérente avec le pattern no-Hilt du projet) :** passer `sleepViewModel` comme paramètre de `NavGraph`. `NavGraph` reçoit un `sleepViewModel: SleepViewModel` optionnel (nullable, avec fallback `NoOpSleepRepository`). Les deux composables `SleepScreen` et `HypnogramScreen` partagent la même instance via ce paramètre.

```kotlin
@Composable
fun NavGraph(
    navController: NavHostController,
    hasToken: Boolean,
    sleepViewModel: SleepViewModel? = null,
    backendUrl: String = "",
    onSaveUrl: (String) -> Unit = {},
    api: NightfallApi? = null,
    authViewModel: AuthViewModel? = null,
)
```

Le `sleepViewModel` résolu (passé ou créé via `remember` avec `NoOpSleepRepository`) est réutilisé pour les deux routes.

`showBottomBar` — la route `"hypnogram/{sessionId}"` ne doit pas afficher la BottomNavBar. La condition existante `currentRoute in setOf("sleep", "trends", "activity", "profile")` est déjà correcte et exclut automatiquement la route Hypnogram.

---

## Composants UI

### `HypnogramScreen`

**Signature :**

```kotlin
@Composable
fun HypnogramScreen(
    sessionId: String,
    sleepViewModel: SleepViewModel,
    onBack: () -> Unit
)
```

**Localisation :** `src/native/java/fr/datasaillance/nightfall/ui/screens/hypnogram/HypnogramScreen.kt`

**Logique de résolution de la session :**

```kotlin
val uiState by sleepViewModel.uiState.collectAsState()
val session = when (val state = uiState) {
    is SleepUiState.Success -> state.sessions.find { it.id == sessionId }
    else -> null
}
```

**Arbre de rendu :**

```
Scaffold(
    topBar = TopAppBar(
        title = nightLabel,       // "Mer 7 mai" — Playfair Display headlineLarge
        navigationIcon = IconButton(onClick = onBack)  // ← flèche retour
    )
)
└── content
    ├── [session == null]
    │   └── Box(testTag="hypnogram_not_found") — texte "Nuit introuvable"
    ├── [session != null && stages null/vides]
    │   └── Box(testTag="hypnogram_no_stages") — texte "Aucune donnée de phase disponible"
    └── [session != null && stages non vides]
        └── Column(fillMaxSize, padding 16.dp)
            ├── HypnogramCanvas(stages, modifier.testTag("hypnogram_canvas"))
            ├── Spacer(12.dp)
            ├── HypnogramLegend(stages, modifier.testTag("hypnogram_legend"))
            ├── Spacer(16.dp)
            └── HypnogramKpis(session, stages, modifier.testTag("hypnogram_kpis"))
```

**TopAppBar — titre :**

Le titre affiche la date de la nuit au format `"EEE d MMM"` (ex: `"Mer 7 mai"`) en Playfair Display, via `headlineLarge`. Même logique que `SleepNightCard` : parser `session.sleep_start` via `OffsetDateTime.parse()` + `DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)`, première lettre en majuscule.

**NavigationIcon :**

```kotlin
navigationIcon = {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
    }
}
```

### `HypnogramCanvas`

**Signature :**

```kotlin
@Composable
fun HypnogramCanvas(
    stages: List<SleepStageResponse>,
    sessionStart: OffsetDateTime,
    sessionEnd: OffsetDateTime,
    modifier: Modifier = Modifier
)
```

**Localisation :** `src/native/java/fr/datasaillance/nightfall/ui/screens/hypnogram/HypnogramCanvas.kt`

**Contraintes de rendu :**

- Utilise `androidx.compose.foundation.Canvas` (pas d'autre librairie graphique).
- Hauteur fixe : `80.dp` — définie via `Modifier.height(80.dp).fillMaxWidth()`.
- Axe X = temps linéaire de `sessionStart` à `sessionEnd`. Les instants sont normalisés en millisecondes.
- Chaque `SleepStageResponse` est converti en un rectangle horizontal :
  - `left = (stage_start_ms - session_start_ms) / session_duration_ms * canvasWidth`
  - `right = (stage_end_ms - session_start_ms) / session_duration_ms * canvasWidth`
  - `top = 0f`, `bottom = canvasHeight`
  - Couleur : voir §Couleurs des stages.
- Pas de gap entre stages consécutifs (rectangles jointifs, pas d'espace entre eux).
- Gestion des gaps temporels : si l'intervalle entre la fin d'un stage et le début du suivant est > 0 ms, remplir avec un rectangle de couleur `AWAKE` (`#D37C04`). Calculer les gaps avant le rendu Canvas.
- Corner radius : 0 (rectangles nets, pas d'arrondi — lisibilité des transitions).
- Pas d'axe de temps affiché dans le Canvas (les KPIs donnent les durées).

**Couleurs des stages :**

| Valeur `stage` | Couleur | Hex |
|----------------|---------|-----|
| `"DEEP"` | Teal | `#0E9EB0` |
| `"LIGHT"` | Muted blue-grey | `#7A9AAA` |
| `"REM"` | Cyan | `#07BCD3` |
| `"AWAKE"` | Amber | `#D37C04` |
| Valeur inconnue | Muted grey | `#4A4A4A` (fallback) |

Ces couleurs sont fixées (non Material-themées) pour garantir la lisibilité sémiologique identique en dark et light mode.

**Pré-calcul des gaps (logique pure, hors Canvas) :**

```
fun buildSegments(
    stages: List<SleepStageResponse>,
    sessionStart: OffsetDateTime,
    sessionEnd: OffsetDateTime
): List<HypnogramSegment>
```

`HypnogramSegment` : `data class HypnogramSegment(val stageType: String, val startMs: Long, val endMs: Long)`

1. Trier les stages par `stage_start` croissant.
2. Si le premier stage ne commence pas à `sessionStart` : insérer un segment `AWAKE` de `sessionStart` à `stages[0].stage_start`.
3. Pour chaque paire (stage[i], stage[i+1]) : si `stage[i].stage_end < stage[i+1].stage_start`, insérer un segment `AWAKE`.
4. Si le dernier stage ne finit pas à `sessionEnd` : insérer un segment `AWAKE` de `stages.last().stage_end` à `sessionEnd`.
5. Retourner la liste ordonnée.

Cette fonction est testable unitairement sans Compose.

### `HypnogramLegend`

Affiche uniquement les types de stages présents dans la liste `stages` (pas de légende pour les types absents).

Layout : `Row` horizontal avec `Spacer(12.dp)` entre chaque item. Chaque item = `Row` : cercle 10dp rempli de la couleur du stage + `Text(stageName, bodySmall)`.

Labels : `"DEEP"` → `"Profond"`, `"LIGHT"` → `"Léger"`, `"REM"` → `"REM"`, `"AWAKE"` → `"Éveil"`.

Les segments AWAKE implicites (gaps) comptent comme `"AWAKE"` présent si au moins un gap a été calculé.

### `HypnogramKpis`

Affiche les durées de la session et par stage. Pas de KPI pour les types de stage absents.

**KPIs obligatoires :**

| KPI | Source | Format |
|-----|--------|--------|
| Durée totale | `session.sleep_end - session.sleep_start` | `"7h 23"` (heures + minutes sans secondes) |
| Durée DEEP | Somme des stages `"DEEP"` | `"1h 30"` — affiché seulement si DEEP présent |
| Durée LIGHT | Somme des stages `"LIGHT"` | `"3h 45"` — affiché seulement si LIGHT présent |
| Durée REM | Somme des stages `"REM"` | `"1h 12"` — affiché seulement si REM présent |
| Durée AWAKE | Somme des stages `"AWAKE"` + gaps implicites | `"0h 16"` — affiché seulement si total > 0 |

**Layout :** grille 2 colonnes via `Row` imbriqués, ou liste verticale `Column` — au choix de l'implémenteur, mais la grille 2×2 est recommandée pour l'espace vertical.

Chaque item KPI :
```
Column {
    Text(label, bodySmall, color=onSurface.copy(alpha=0.6f))   // ex: "Profond"
    Text(value, headlineMedium, color=stageColor)               // ex: "1h 30" en teal
}
```

La durée totale est affichée en `headlineLarge` avec couleur `onBackground`, en tête de la section KPIs.

---

## Design tokens

### Palette (identique à P5.1)

| Rôle Material 3 | Dark mode | Light mode |
|-----------------|-----------|------------|
| `background` | `#191E22` | `#FAFAFA` |
| `surface` | `#232E32` | `#FFFFFF` |
| `primary` | `#0E9EB0` | `#0E9EB0` |
| `secondary` | `#D37C04` | `#D37C04` |
| `tertiary` | `#07BCD3` | `#07BCD3` |
| `onBackground` | `#FFFFFF` | `#1A1A1A` |
| `onSurface` | `#E0E0E0` | `#1A1A1A` |

### Couleurs des stages (non-themées, fixes)

| Stage | Couleur | Hex | Usage |
|-------|---------|-----|-------|
| DEEP | Teal | `#0E9EB0` | Canvas + KPI label |
| LIGHT | Muted | `#7A9AAA` | Canvas + KPI label |
| REM | Cyan | `#07BCD3` | Canvas + KPI label |
| AWAKE | Amber | `#D37C04` | Canvas + KPI label |

### Typographie

| Style Material 3 | Famille | Taille | Poids | Usage dans HypnogramScreen |
|------------------|---------|--------|-------|---------------------------|
| `headlineLarge` | Playfair Display | 32sp | 700 | TopAppBar titre, KPI durée totale |
| `headlineMedium` | Inter | 22sp | 600 | KPI valeur par stage |
| `bodySmall` | Inter | 14sp | 400 | KPI label, légende |
| `labelLarge` | Inter | 13sp | 500 | (réservé si header secondaire nécessaire) |

---

## Test IDs (testTag)

| Composant | testTag | Condition |
|-----------|---------|-----------|
| `Canvas` (HypnogramCanvas) | `hypnogram_canvas` | stages non vides |
| Bloc légende | `hypnogram_legend` | stages non vides |
| Bloc KPIs | `hypnogram_kpis` | stages non vides |
| Message session introuvable | `hypnogram_not_found` | `session == null` |
| Message pas de stages | `hypnogram_no_stages` | `session != null && stages null/vides` |

---

## Tests d'acceptation

### TA-H-01 — Navigation SleepScreen vers HypnogramScreen

**Given** `SleepScreen` affiché avec une liste de sessions (état `Success`),  
**when** l'utilisateur clique sur la `SleepNightCard` de la session `"abc-123"`,  
**then** `navController.currentDestination?.route` vaut `"hypnogram/abc-123"` et `HypnogramScreen` est composé avec `sessionId = "abc-123"`.

### TA-H-02 — Back button retour SleepScreen

**Given** `HypnogramScreen` affiché,  
**when** l'utilisateur appuie sur la flèche retour dans la TopAppBar,  
**then** `navController.popBackStack()` est appelé et le backstack revient à `NavDestination.Sleep.route`.

### TA-H-03 — Canvas visible

**Given** `HypnogramScreen` composé avec une session ayant des stages non vides,  
**when** le composant est rendu,  
**then** un composant avec `testTag = "hypnogram_canvas"` est visible dans la hiérarchie.

### TA-H-04 — Légende contient les stages présents

**Given** une session dont les stages contiennent `"DEEP"`, `"LIGHT"`, `"REM"` et `"AWAKE"`,  
**when** `HypnogramScreen` est rendu,  
**then** le composant `testTag = "hypnogram_legend"` est visible et contient les textes `"Profond"`, `"Léger"`, `"REM"`, `"Éveil"`.

### TA-H-05 — KPI durée totale correcte

**Given** une session avec `sleep_start = "2026-05-07T23:00:00+02:00"` et `sleep_end = "2026-05-08T06:23:00+02:00"`,  
**when** `HypnogramScreen` est rendu,  
**then** le composant `testTag = "hypnogram_kpis"` est visible et contient le texte `"7h 23"`.

### TA-H-06 — KPI durée DEEP correcte

**Given** une session dont les stages incluent deux segments `"DEEP"` de 45 minutes chacun (total 90 minutes),  
**when** `HypnogramScreen` est rendu,  
**then** le bloc KPIs contient le texte `"1h 30"` associé au label `"Profond"`.

### TA-H-07 — État not_found si sessionId invalide

**Given** `SleepViewModel` en état `Success` avec des sessions dont aucune n'a l'id `"invalid-id"`,  
**when** `HypnogramScreen` est composé avec `sessionId = "invalid-id"`,  
**then** le composant `testTag = "hypnogram_not_found"` est visible et `testTag = "hypnogram_canvas"` n'existe pas.

### TA-H-08 — État no_stages si stages null ou vides

**Given** `SleepViewModel` en état `Success` avec une session `"session-42"` dont `stages = null` (ou `stages = emptyList()`),  
**when** `HypnogramScreen` est composé avec `sessionId = "session-42"`,  
**then** le composant `testTag = "hypnogram_no_stages"` est visible et `testTag = "hypnogram_canvas"` n'existe pas.

### TA-H-09 — Snapshot Paparazzi dark mode

**Given** `HypnogramScreen` composé avec une session fictive contenant 4 types de stages (DEEP 90min, LIGHT 180min, REM 60min, AWAKE 15min) et le thème `NightfallDarkTheme`,  
**when** Paparazzi génère un screenshot,  
**then** le screenshot correspond au golden de référence à ±0% (bloquant CI). Le fond doit être `#191E22`, les blocs Canvas doivent être colorés par stage.

### TA-H-10 — Snapshot Paparazzi light mode

**Given** même state que TA-H-09 avec le thème `NightfallLightTheme`,  
**when** Paparazzi génère un screenshot,  
**then** le screenshot correspond au golden light de référence. Le fond doit être `#FAFAFA`, les couleurs de stages identiques (non-themées).

### TA-H-11 — Gaps implicites remplis en AWAKE

**Given** une session de 8h dont les stages couvrent seulement 7h30 (gap de 30min entre deux stages),  
**when** `buildSegments()` est appelée,  
**then** la liste retournée contient un segment de type `"AWAKE"` pour les 30min de gap et la somme des durées de tous les segments est égale à la durée totale de la session.

### TA-H-12 — Légende n'affiche pas les stages absents

**Given** une session dont les stages contiennent uniquement `"DEEP"` et `"REM"` (pas de `"LIGHT"`, pas de `"AWAKE"` et pas de gaps),  
**when** `HypnogramScreen` est rendu,  
**then** les textes `"Léger"` et `"Éveil"` sont absents du composant `testTag = "hypnogram_legend"`.

---

## Accessibilité

- `HypnogramCanvas` ajoute un `semantics { contentDescription = "Hypnogramme — $totalDuration de sommeil" }` sur son `Modifier` pour les lecteurs d'écran. Le Canvas lui-même n'est pas accessible pixel à pixel — la description textuelle compense.
- Les boutons et items interactifs ont `contentDescription` explicite.
- Contraste : les couleurs de stages sont fixes et respectent un ratio ≥ 3:1 sur fond `#191E22` (dark) et `#FAFAFA` (light) — à valider en implémentation pour `#7A9AAA` (LIGHT stage, le plus sombre).

---

## Logging

Utiliser `Timber` (déjà intégré Phase 4). Événements à logger dans `HypnogramScreen` :

| Événement | Niveau | Champs |
|-----------|--------|--------|
| Écran affiché | `d` | `scope=hypnogram_screen`, `session_id` |
| Session non trouvée | `w` | `scope=hypnogram_screen`, `session_id` |
| Stages absents | `d` | `scope=hypnogram_screen`, `session_id` |
| Nombre de segments calculés | `d` | `scope=hypnogram_canvas`, `segment_count` |

Aucun champ de valeur santé brut dans les logs (pas de `sleep_start`, pas de durées en secondes, pas de types de stage individuels) — conformité C2.

---

## RGPD

- Aucune nouvelle donnée n'est persistée côté Android : `HypnogramScreen` lit depuis `SleepUiState.Success` (mémoire volatile, non persistée — cohérent avec la décision P5.1 de ne pas utiliser Room en V1).
- Aucun appel réseau supplémentaire — les stages sont déjà en mémoire.
- Les screenshots Paparazzi utilisent des données fictives (fixtures) sans UUID ni dates réelles.
- Conformité aux contraintes C1 (local-first) et C2 (pas de logging de données santé) inchangée.

---

## Livrables

- [ ] `NavDestination.kt` — ajout de `object Hypnogram` avec route paramétrée et helper `route(sessionId: String)`
- [ ] `NavGraph.kt` — correction `onSessionClick` + ajout route `composable("hypnogram/{sessionId}")` + hoist `sleepViewModel`
- [ ] `HypnogramScreen.kt` — écran complet (src/native/) avec états `not_found`, `no_stages`, et vue principale
- [ ] `HypnogramCanvas.kt` — composant Canvas pur (src/native/) avec `buildSegments()` pur (testable sans Compose)
- [ ] `HypnogramScreenTest.kt` — tests unitaires `buildSegments()` + tests Compose (TA-H-01 à TA-H-12) + goldens Paparazzi

---

## Suite naturelle

**P5.3 Stacked timeline** (`spec-p5-timeline`) : vue calendaire de l'ensemble des nuits, chaque nuit représentée par une barre miniature colorée selon la répartition des stages. Réutilisera `HypnogramCanvas` en mode miniature (hauteur réduite, non interactive).
