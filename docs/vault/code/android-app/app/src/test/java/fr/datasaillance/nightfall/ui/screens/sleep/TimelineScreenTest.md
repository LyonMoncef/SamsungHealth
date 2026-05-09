---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/sleep/TimelineScreenTest.kt
git_blob: ea89fd1558f524a2aa6fc4abf20fad29f818e990
last_synced: '2026-05-09T07:04:03Z'
loc: 539
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/sleep/TimelineScreenTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/sleep/TimelineScreenTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/sleep/TimelineScreenTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.sleep

// spec: Tests d'acceptation TA-TL-01 à TA-TL-07, TA-TL-VM-01 à TA-TL-VM-05
// spec: section "TimelineScreen" — états Loading/Success/Error/Empty, canvas multi-nuits, tri croissant, snapshots, ViewModel
// RED by construction: the following classes do not exist yet and imports will fail at compile time:
//   fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel
//   fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState
//   fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen
// Existing classes (will compile fine):
//   fr.datasaillance.nightfall.data.sleep.SleepRepository
//   fr.datasaillance.nightfall.data.sleep.SleepSessionResponse

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import fr.datasaillance.nightfall.data.sleep.SleepRepository
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import fr.datasaillance.nightfall.ui.theme.NightfallTheme

// ---------------------------------------------------------------------------
// Test fixtures — 3 sessions sur 3 nuits consécutives pour tester le tri et le drift
// spec: TA-TL-VM-01/TA-TL-VM-05 — sessions avec sleep_start dans l'ordre aléatoire
// ---------------------------------------------------------------------------

// spec: TA-TL-VM-01 — session la plus ancienne (2026-05-05)
private val session1 = SleepSessionResponse(
    id          = "tl-001",
    sleep_start = "2026-05-05T23:00:00+02:00",
    sleep_end   = "2026-05-06T07:00:00+02:00",
    created_at  = null,
    stages      = null
)

// spec: TA-TL-VM-01 — session intermédiaire (2026-05-06)
private val session2 = SleepSessionResponse(
    id          = "tl-002",
    sleep_start = "2026-05-06T23:45:00+02:00",
    sleep_end   = "2026-05-07T07:30:00+02:00",
    created_at  = null,
    stages      = null
)

// spec: TA-TL-VM-01 — session la plus récente (2026-05-08) avec drift +90 min
private val session3 = SleepSessionResponse(
    id          = "tl-003",
    sleep_start = "2026-05-08T00:30:00+02:00",
    sleep_end   = "2026-05-08T08:15:00+02:00",
    created_at  = null,
    stages      = null
)

// spec: TA-TL-VM-05 — session3 avant session1 en ordre inversé pour tester que le ViewModel trie correctement
private val sessionsUnsorted = listOf(session3, session1, session2)
private val sessionsSorted   = listOf(session1, session2, session3)

// ---------------------------------------------------------------------------
// Helper: inject state into TimelineViewModel via reflection on _uiState
// spec: pattern identique à injectHypnogramState dans HypnogramScreenTest
// ---------------------------------------------------------------------------

@Suppress("UNCHECKED_CAST")
private fun injectTimelineState(
    viewModel: fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel,
    state: fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState
) {
    val field = fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel::class.java
        .getDeclaredField("_uiState")
    field.isAccessible = true
    (field.get(viewModel) as MutableStateFlow<fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState>)
        .value = state
}

// ---------------------------------------------------------------------------
// Class 1 — Paparazzi snapshot tests (no @RunWith — incompatible avec Robolectric)
// spec: TA-TL-06, TA-TL-07 + états Loading/Error
// ---------------------------------------------------------------------------

class TimelineScreenSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    private fun buildViewModel(
        repository: SleepRepository = mock<SleepRepository>()
    ): fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel =
        fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel(
            repository = repository
        )

    // spec: TA-TL-06 — snapshot dark mode, état Success, ≥2 sessions avec drift visible
    // spec: "fond #191E22, barres teal visibles sur le canvas, labels de date lisibles"
    // RED by construction: TimelineScreen et TimelineViewModel n'existent pas encore
    @Test
    fun timelineScreen_success_dark() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Success(sessionsSorted)
        )

        paparazzi.snapshot {
            NightfallTheme(darkTheme = true) {
                // spec: TA-TL-06 — TimelineScreen en dark mode, état Success avec 3 sessions
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }
        // spec: TA-TL-06 — golden dark: fond #191E22, barres teal (0xFF0E9EB0), labels date lisibles
    }

    // spec: TA-TL-07 — snapshot light mode, même état Success
    // spec: "fond #FAFAFA, barres teal inchangées, labels sombres lisibles"
    // RED by construction: TimelineScreen et TimelineViewModel n'existent pas encore
    @Test
    fun timelineScreen_success_light() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Success(sessionsSorted)
        )

        paparazzi.snapshot {
            NightfallTheme(darkTheme = false) {
                // spec: TA-TL-07 — TimelineScreen en light mode, même état Success
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }
        // spec: TA-TL-07 — golden light: fond #FAFAFA, barres teal, labels sombres (onSurface = #1A1A1A)
    }

    // spec: état Loading — spinner visible, canvas absent
    // RED by construction: TimelineUiState.Loading n'existe pas encore
    @Test
    fun timelineScreen_loading_dark() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Loading
        )

        paparazzi.snapshot {
            NightfallTheme(darkTheme = true) {
                // spec: état Loading → CircularProgressIndicator avec testTag "tl_loading"
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }
        // spec: snapshot dark Loading: indicateur de chargement centré, pas de canvas
    }

    // spec: état Error — message d'erreur + bouton retry visibles
    // RED by construction: TimelineUiState.Error n'existe pas encore
    @Test
    fun timelineScreen_error_dark() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Error("Vérifiez votre connexion réseau")
        )

        paparazzi.snapshot {
            NightfallTheme(darkTheme = true) {
                // spec: état Error → message d'erreur + bouton "Réessayer" avec testTags tl_error/tl_retry
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }
        // spec: snapshot dark Error: "Vérifiez votre connexion réseau" visible, bouton retry présent
    }
}

// ---------------------------------------------------------------------------
// Class 2 — Robolectric behavioral tests
// spec: TA-TL-01, TA-TL-02, TA-TL-03, TA-TL-04, TA-TL-05
// Classe séparée de Paparazzi : lifecycles de Rule incompatibles
// ---------------------------------------------------------------------------

// spec: TA-TL-01 — état Success → tl_canvas assertExists
// spec: TA-TL-02 — état Loading → tl_loading assertExists, tl_canvas assertDoesNotExist
// spec: TA-TL-03 — état Error → tl_error assertExists, tl_retry assertExists
// spec: TA-TL-04 — état Empty → tl_empty assertExists
// spec: TA-TL-05 — état Success → tl_screen assertExists
// RED by construction: TimelineScreen, TimelineViewModel, TimelineUiState n'existent pas encore
@RunWith(RobolectricTestRunner::class)
class TimelineScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildViewModel(
        repository: SleepRepository = mock<SleepRepository>()
    ): fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel =
        fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel(
            repository = repository
        )

    // spec: TA-TL-01 — état Success → tl_canvas assertExists dans la hiérarchie Compose
    // RED: TimelineScreen n'a pas encore de testTag "tl_canvas"
    @Test
    fun timelineScreen_shows_canvas_in_success() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Success(sessionsSorted)
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-TL-01 — TimelineScreen avec ViewModel en état Success (3 sessions)
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }

        // spec: TA-TL-01 — canvas multi-nuits visible en état Success
        composeTestRule
            .onNode(hasTestTag("tl_canvas"))
            .assertExists()
    }

    // spec: TA-TL-02 — état Loading → tl_loading assertExists, tl_canvas assertDoesNotExist
    // RED: TimelineScreen n'a pas encore de testTag "tl_loading"
    @Test
    fun timelineScreen_shows_loading_and_hides_canvas() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Loading
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-TL-02 — TimelineScreen avec ViewModel en état Loading
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }

        // spec: TA-TL-02 — CircularProgressIndicator visible
        composeTestRule
            .onNode(hasTestTag("tl_loading"))
            .assertExists()

        // spec: TA-TL-02 — canvas absent en état Loading (pas de données encore)
        composeTestRule
            .onNode(hasTestTag("tl_canvas"))
            .assertDoesNotExist()
    }

    // spec: TA-TL-03 — état Error → tl_error assertExists, tl_retry assertExists
    // spec: "clic sur tl_retry déclenche TimelineViewModel.retry(), remet état à Loading"
    // RED: TimelineScreen n'a pas encore de testTags "tl_error" / "tl_retry"
    @Test
    fun timelineScreen_shows_error_with_retry() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Error("Vérifiez votre connexion réseau")
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-TL-03 — TimelineScreen avec ViewModel en état Error
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }

        // spec: TA-TL-03 — bloc message d'erreur visible
        composeTestRule
            .onNode(hasTestTag("tl_error"))
            .assertExists()

        // spec: TA-TL-03 — bouton "Réessayer" visible en état Error
        composeTestRule
            .onNode(hasTestTag("tl_retry"))
            .assertExists()
    }

    // spec: TA-TL-03 — clic sur tl_retry remet l'état à Loading
    // RED: TimelineScreen tl_retry + retry() n'existe pas encore
    @Test
    fun timelineScreen_retry_click_triggers_reload() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Error("Vérifiez votre connexion réseau")
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }

        // spec: TA-TL-03 — clic sur retry → retry() est appelé, état redevient Loading
        composeTestRule
            .onNode(hasTestTag("tl_retry"))
            .performClick()

        // spec: TA-TL-03 — après retry(), l'état est Loading (repository mock ne répond pas encore)
        composeTestRule
            .onNode(hasTestTag("tl_loading"))
            .assertExists()
    }

    // spec: TA-TL-04 — état Empty → tl_empty assertExists, tl_canvas assertDoesNotExist
    // spec: "tl_empty affiche le texte 'Aucune nuit enregistrée'"
    // RED: TimelineScreen n'a pas encore de testTag "tl_empty"
    @Test
    fun timelineScreen_shows_empty_state() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Empty
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-TL-04 — TimelineScreen avec ViewModel en état Empty
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }

        // spec: TA-TL-04 — message "Aucune nuit enregistrée" visible
        composeTestRule
            .onNode(hasTestTag("tl_empty"))
            .assertExists()

        // spec: TA-TL-04 — canvas absent en état Empty (pas de nuits à afficher)
        composeTestRule
            .onNode(hasTestTag("tl_canvas"))
            .assertDoesNotExist()
    }

    // spec: TA-TL-05 — état Success → tl_screen assertExists (conteneur racine de l'état Success)
    // RED: TimelineScreen n'a pas encore de testTag "tl_screen"
    @Test
    fun timelineScreen_root_exists_in_success() {
        val viewModel = buildViewModel()
        injectTimelineState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Success(sessionsSorted)
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-TL-05 — TimelineScreen avec ViewModel en état Success
                fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen(
                    viewModel = viewModel
                )
            }
        }

        // spec: TA-TL-05 — conteneur Column racine de l'état Success existe
        composeTestRule
            .onNode(hasTestTag("tl_screen"))
            .assertExists()
    }
}

// ---------------------------------------------------------------------------
// Class 3 — TimelineViewModel unit tests (pure JVM, pas de Compose)
// spec: TA-TL-VM-01, TA-TL-VM-02, TA-TL-VM-03, TA-TL-VM-04, TA-TL-VM-05
// ---------------------------------------------------------------------------

// spec: TA-TL-VM-01 — repository retourne sessions → Success avec liste triée croissant
// spec: TA-TL-VM-02 — repository retourne liste vide → Empty
// spec: TA-TL-VM-03 — repository retourne failure(IOException) → Error
// spec: TA-TL-VM-04 — après init{} sans advanceUntilIdle() → Loading (synchrone)
// spec: TA-TL-VM-05 — Success.sessions triées par sleep_start croissant (pas décroissant)
// RED by construction: TimelineViewModel et TimelineUiState n'existent pas encore
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SleepRepository
    private lateinit var viewModel: fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel

    @Before
    fun setUp() {
        // spec: TA-TL-VM-01 — viewModelScope utilise Dispatchers.Main ; remplacé par testDispatcher
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        // spec: DI manuelle — TimelineViewModel(repository), init{} déclenche loadSessions()
        viewModel = fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel(
            repository = repository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // spec: TA-TL-VM-01 — repository retourne 3 sessions → uiState = Success avec liste triée croissant
    // spec: "trie par sleep_start croissant (nuit la plus ancienne en premier)"
    // RED: TimelineViewModel.loadSessions() n'existe pas encore
    @Test
    fun timelineViewModel_emits_success_with_sessions() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.success(sessionsUnsorted)
        )

        // spec: TA-TL-VM-01 — init{} appelle loadSessions() ; advanceUntilIdle() laisse la coroutine finir
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Success) {
            "uiState must be TimelineUiState.Success when repository returns sessions — spec: TA-TL-VM-01, got: $state"
        }

        val sessions = (state as fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Success).sessions
        assert(sessions.isNotEmpty()) {
            "Success.sessions must not be empty — spec: TA-TL-VM-01, got empty list"
        }
    }

    // spec: TA-TL-VM-02 — repository retourne Result.success(emptyList()) → uiState = Empty
    // spec: "Empty est un état distinct de Success(emptyList())"
    // RED: TimelineViewModel.loadSessions() n'existe pas encore
    @Test
    fun timelineViewModel_emits_empty_when_no_sessions() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.success(emptyList())
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Empty) {
            "uiState must be TimelineUiState.Empty when repository returns empty list — spec: TA-TL-VM-02, got: $state"
        }
    }

    // spec: TA-TL-VM-03 — repository retourne Result.failure(IOException) → uiState = Error
    // spec: mapError() — IOException → "Vérifiez votre connexion réseau"
    // RED: TimelineViewModel.loadSessions() n'existe pas encore
    @Test
    fun timelineViewModel_emits_error_when_repository_fails() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.failure(java.io.IOException("Connection refused"))
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Error) {
            "uiState must be TimelineUiState.Error when repository returns failure — spec: TA-TL-VM-03, got: $state"
        }

        // spec: TA-TL-VM-03 — IOException → message "Vérifiez votre connexion réseau"
        val message = (state as fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Error).message
        assert(message == "Vérifiez votre connexion réseau") {
            "Error.message must be 'Vérifiez votre connexion réseau' for IOException — spec: TA-TL-VM-03, got: '$message'"
        }
    }

    // spec: TA-TL-VM-04 — après init{} et AVANT advanceUntilIdle() → uiState = Loading
    // spec: "loadSessions() émet Loading en premier, avant tout appel réseau"
    // RED: TimelineViewModel.loadSessions() n'existe pas encore
    @Test
    fun timelineViewModel_emits_loading_synchronously_on_init() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.success(sessionsUnsorted)
        )

        // spec: TA-TL-VM-04 — init{} appelle loadSessions() ; sans advanceUntilIdle() la coroutine
        //   est suspendue à getSessions() et uiState doit déjà être Loading
        val stateWhileInFlight = viewModel.uiState.value
        assert(stateWhileInFlight is fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Loading) {
            "uiState must be TimelineUiState.Loading immediately after init{} — spec: TA-TL-VM-04, got: $stateWhileInFlight"
        }

        advanceUntilIdle()
    }

    // spec: TA-TL-VM-05 — Success.sessions sont triées par sleep_start croissant (pas décroissant comme SleepViewModel)
    // spec: "la nuit la plus ancienne est à gauche sur l'axe temporel du canvas"
    // RED: TimelineViewModel.loadSessions() n'existe pas encore
    @Test
    fun timelineViewModel_sessions_sorted_ascending_by_sleep_start() = runTest {
        whenever(repository.getSessions()).thenReturn(Result.success(sessionsUnsorted))

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Success) {
            "uiState must be TimelineUiState.Success — spec: TA-TL-VM-05, got: $state"
        }
        val sessions = (state as fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState.Success).sessions

        // spec: TA-TL-VM-05 — sleep_start: session1 (2026-05-05) < session2 (2026-05-06) < session3 (2026-05-08)
        assert(sessions[0].id == "tl-001") {
            "First must be tl-001 (oldest, 2026-05-05) — spec: TA-TL-VM-05, got ${sessions[0].id}"
        }
        assert(sessions[1].id == "tl-002") {
            "Second must be tl-002 (2026-05-06) — spec: TA-TL-VM-05, got ${sessions[1].id}"
        }
        assert(sessions[2].id == "tl-003") {
            "Third must be tl-003 (newest, 2026-05-08) — spec: TA-TL-VM-05, got ${sessions[2].id}"
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `injectTimelineState` (function) — lines 82-91
- `TimelineScreenSnapshotTest` (class) — lines 98-198
- `buildViewModel` (function) — lines 106-111
- `timelineScreen_success_dark` (function) — lines 116-133
- `timelineScreen_success_light` (function) — lines 138-155
- `timelineScreen_loading_dark` (function) — lines 159-176
- `timelineScreen_error_dark` (function) — lines 180-197
- `TimelineScreenInteractionTest` (class) — lines 212-395
- `buildViewModel` (function) — lines 218-223
- `timelineScreen_shows_canvas_in_success` (function) — lines 227-248
- `timelineScreen_shows_loading_and_hides_canvas` (function) — lines 252-278
- `timelineScreen_shows_error_with_retry` (function) — lines 283-309
- `timelineScreen_retry_click_triggers_reload` (function) — lines 313-338
- `timelineScreen_shows_empty_state` (function) — lines 343-369
- `timelineScreen_root_exists_in_success` (function) — lines 373-394
- `TimelineViewModelTest` (class) — lines 409-539
- `setUp` (function) — lines 415-424
- `tearDown` (function) — lines 426-429
- `timelineViewModel_emits_success_with_sessions` (function) — lines 434-452
- `timelineViewModel_emits_empty_when_no_sessions` (function) — lines 457-469
- `timelineViewModel_emits_error_when_repository_fails` (function) — lines 474-492
- `timelineViewModel_emits_loading_synchronously_on_init` (function) — lines 497-511
- `timelineViewModel_sessions_sorted_ascending_by_sleep_start` (function) — lines 516-538
