package fr.datasaillance.nightfall.ui.screens.sleep

// spec: Tests d'acceptation TA-H-02, TA-H-03, TA-H-04, TA-H-05, TA-H-06, TA-H-07, TA-H-08,
//       TA-H-VM-01, TA-H-VM-02, TA-H-VM-03, TA-H-VM-04
// spec: section "HypnogramScreen" — états Loading/Success/Error, canvas, stats, snapshots, ViewModel
// RED by construction: the following classes do not exist yet and imports will fail at compile time:
//   fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel
//   fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState
//   fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen
// Existing classes (will compile fine):
//   fr.datasaillance.nightfall.data.sleep.SleepRepository
//   fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
//   fr.datasaillance.nightfall.data.sleep.SleepStageResponse

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import fr.datasaillance.nightfall.data.sleep.SleepStageResponse
import fr.datasaillance.nightfall.ui.theme.NightfallTheme

// ---------------------------------------------------------------------------
// Test fixtures — session complète avec 4 types de stages
// spec: TA-H-07/TA-H-08 — session avec DEEP, LIGHT, REM, AWAKE
// ---------------------------------------------------------------------------

// spec: TA-H-03/TA-H-04 — session complète : 4 stages couvrant la nuit du 7 mai
private val fullSession = SleepSessionResponse(
    id          = "hyp-001",
    sleep_start = "2026-05-07T23:15:00+02:00",
    sleep_end   = "2026-05-08T06:38:00+02:00",
    created_at  = "2026-05-08T10:00:00Z",
    stages = listOf(
        SleepStageResponse(
            id          = "h1",
            session_id  = "hyp-001",
            stage       = "DEEP",
            stage_start = "2026-05-08T00:00:00+02:00",
            stage_end   = "2026-05-08T01:30:00+02:00"
        ),
        SleepStageResponse(
            id          = "h2",
            session_id  = "hyp-001",
            stage       = "LIGHT",
            stage_start = "2026-05-08T01:30:00+02:00",
            stage_end   = "2026-05-08T04:00:00+02:00"
        ),
        SleepStageResponse(
            id          = "h3",
            session_id  = "hyp-001",
            stage       = "REM",
            stage_start = "2026-05-08T04:00:00+02:00",
            stage_end   = "2026-05-08T05:30:00+02:00"
        ),
        SleepStageResponse(
            id          = "h4",
            session_id  = "hyp-001",
            stage       = "AWAKE",
            stage_start = "2026-05-08T05:30:00+02:00",
            stage_end   = "2026-05-08T06:38:00+02:00"
        )
    )
)

// spec: TA-H-VM-02 — session avec un id différent (filtre ne trouve pas hyp-001)
private val otherSession = SleepSessionResponse(
    id          = "hyp-999",
    sleep_start = "2026-05-06T22:00:00+02:00",
    sleep_end   = "2026-05-07T06:00:00+02:00",
    created_at  = null,
    stages      = null
)

// ---------------------------------------------------------------------------
// Helper: inject state into HypnogramViewModel via reflection on _uiState
// spec: pattern identique à injectSleepState dans SleepScreenTest
// ---------------------------------------------------------------------------

@Suppress("UNCHECKED_CAST")
private fun injectHypnogramState(
    viewModel: fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel,
    state: fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState
) {
    val field = fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel::class.java
        .getDeclaredField("_uiState")
    field.isAccessible = true
    (field.get(viewModel) as MutableStateFlow<fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState>)
        .value = state
}

// ---------------------------------------------------------------------------
// Class 1 — Paparazzi snapshot tests (no @RunWith — incompatible avec Robolectric)
// spec: TA-H-07, TA-H-08
// ---------------------------------------------------------------------------

class HypnogramScreenSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    private fun buildViewModel(
        repository: SleepRepository = mock<SleepRepository>()
    ): fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel =
        fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel(
            sessionId  = "hyp-001",
            repository = repository
        )

    // spec: TA-H-07 — snapshot dark mode, état Success, fond #191E22, canvas avec 4 couleurs de stages
    // RED by construction: HypnogramScreen et HypnogramViewModel n'existent pas encore
    @Test
    fun hypnogramScreen_success_dark() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success(listOf(fullSession))
        )

        paparazzi.snapshot {
            NightfallTheme(darkTheme = true) {
                // spec: TA-H-07 — HypnogramScreen reçoit HypnogramViewModel et onBack
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }
        // spec: TA-H-07 — golden dark: fond #191E22, canvas visible avec DEEP/LIGHT/REM/AWAKE
    }

    // spec: TA-H-08 — snapshot light mode, même état Success, fond #FAFAFA
    // spec: "couleurs de stages hardcodées — identiques en dark et light mode"
    // RED by construction: HypnogramScreen et HypnogramViewModel n'existent pas encore
    @Test
    fun hypnogramScreen_success_light() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success(listOf(fullSession))
        )

        paparazzi.snapshot {
            NightfallTheme(darkTheme = false) {
                // spec: TA-H-08 — light mode snapshot: fond #FAFAFA, mêmes couleurs de stages
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }
        // spec: TA-H-08 — golden light: fond clair, canvas inchangé (couleurs physiologiques constantes)
    }

    // spec: état Loading — spinner visible, canvas absent
    // RED by construction: HypnogramUiState.Loading n'existe pas encore
    @Test
    fun hypnogramScreen_loading_dark() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Loading
        )

        paparazzi.snapshot {
            NightfallTheme(darkTheme = true) {
                // spec: état Loading → CircularProgressIndicator visible, hyp_canvas absent
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }
        // spec: snapshot dark Loading: indicateur de chargement, pas de canvas
    }

    // spec: état Error — message d'erreur + bouton retry visibles
    // RED by construction: HypnogramUiState.Error n'existe pas encore
    @Test
    fun hypnogramScreen_error_dark() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Error("Erreur réseau")
        )

        paparazzi.snapshot {
            NightfallTheme(darkTheme = true) {
                // spec: état Error → message d'erreur + bouton "Réessayer" visibles
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }
        // spec: snapshot dark Error: "Erreur réseau" visible, bouton retry présent
    }
}

// ---------------------------------------------------------------------------
// Class 2 — Robolectric behavioral tests
// spec: TA-H-02, TA-H-03, TA-H-04, TA-H-05, TA-H-06
// Classe séparée de Paparazzi : lifecycles de Rule incompatibles
// ---------------------------------------------------------------------------

// spec: TA-H-03 — Success → hyp_canvas assertExists
// spec: TA-H-02 — Success → titre "Mer 7 mai" visible dans la TopAppBar
// spec: TA-H-04 — Success → hyp_stats assertExists
// spec: TA-H-05 — Loading → hyp_loading assertExists, hyp_canvas assertDoesNotExist
// spec: TA-H-06 — Error → hyp_error assertExists, hyp_retry assertExists
// RED by construction: HypnogramScreen, HypnogramViewModel, HypnogramUiState n'existent pas encore
@RunWith(RobolectricTestRunner::class)
class HypnogramScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildViewModel(
        repository: SleepRepository = mock<SleepRepository>()
    ): fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel =
        fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel(
            sessionId  = "hyp-001",
            repository = repository
        )

    // spec: TA-H-03 — état Success → hyp_canvas assertExists dans la hiérarchie
    // RED: HypnogramScreen n'a pas encore de testTag "hyp_canvas"
    @Test
    fun hypnogramScreen_shows_canvas() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success(listOf(fullSession))
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-H-03 — HypnogramScreen avec ViewModel en état Success
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }

        // spec: TA-H-03 — canvas de la timeline visible en état Success
        composeTestRule
            .onNode(hasTestTag("hyp_canvas"))
            .assertExists()
    }

    // spec: TA-H-02 — état Success, sleep_start "2026-05-07T23:15:00+02:00"
    //   → TopAppBar affiche "Nuit du Mer 7 mai" (DateTimeFormatter, Locale.FRENCH)
    // RED: HypnogramScreen n'existe pas encore, nightTitle() n'est pas implémentée
    @Test
    fun hypnogramScreen_shows_night_title() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success(listOf(fullSession))
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-H-02 — sleep_start = "2026-05-07T23:15:00+02:00" → titre "Nuit du Mer 7 mai"
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }

        // spec: TA-H-02 — texte "Mer 7 mai" doit apparaître dans la TopAppBar
        // Note: onNodeWithText match partiel suffisant — le titre complet est "Nuit du Mer 7 mai"
        composeTestRule
            .onNodeWithText("Mer 7 mai", substring = true)
            .assertExists()
    }

    // spec: TA-H-04 — état Success → hyp_stats assertExists (section "Détail des phases")
    // RED: HypnogramScreen n'a pas encore de testTag "hyp_stats"
    @Test
    fun hypnogramScreen_shows_stats_section() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success(listOf(fullSession))
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-H-04 — HypnogramScreen avec 4 stages : DEEP, LIGHT, REM, AWAKE
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }

        // spec: TA-H-04 — section stats visible avec les durées par stage
        composeTestRule
            .onNode(hasTestTag("hyp_stats"))
            .assertExists()
    }

    // spec: TA-H-05 — état Loading → hyp_loading assertExists, hyp_canvas assertDoesNotExist
    // RED: HypnogramScreen n'a pas encore de testTag "hyp_loading"
    @Test
    fun hypnogramScreen_shows_loading() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Loading
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-H-05 — HypnogramScreen avec ViewModel en état Loading
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }

        // spec: TA-H-05 — CircularProgressIndicator visible
        composeTestRule
            .onNode(hasTestTag("hyp_loading"))
            .assertExists()

        // spec: TA-H-05 — canvas absent en état Loading (pas de données encore)
        composeTestRule
            .onNode(hasTestTag("hyp_canvas"))
            .assertDoesNotExist()
    }

    // spec: TA-H-06 — état Error → hyp_error assertExists, hyp_retry assertExists
    // RED: HypnogramScreen n'a pas encore de testTags "hyp_error" / "hyp_retry"
    @Test
    fun hypnogramScreen_shows_error_with_retry() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Error("Erreur réseau")
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                // spec: TA-H-06 — HypnogramScreen avec ViewModel en état Error
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }

        // spec: TA-H-06 — bloc message d'erreur visible
        composeTestRule
            .onNode(hasTestTag("hyp_error"))
            .assertExists()

        // spec: TA-H-06 — bouton "Réessayer" visible en état Error
        composeTestRule
            .onNode(hasTestTag("hyp_retry"))
            .assertExists()
    }

    // spec: TA-H-09 — session sans stages → hyp_canvas visible (placeholder), hyp_stats sans lignes
    @Test
    fun hypnogramScreen_stages_null_shows_canvas_placeholder() {
        val sessionNoStages = SleepSessionResponse(
            id          = "hyp-001",
            sleep_start = "2026-05-07T23:15:00+02:00",
            sleep_end   = "2026-05-08T06:38:00+02:00",
            created_at  = null,
            stages      = null
        )
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success(listOf(sessionNoStages))
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }

        // spec: TA-H-09 — hyp_canvas visible même sans stages (placeholder affiché)
        composeTestRule.onNode(hasTestTag("hyp_canvas")).assertExists()
        // spec: TA-H-09 — hyp_stats visible mais sans lignes de stage
        composeTestRule.onNode(hasTestTag("hyp_stats")).assertExists()
    }

    // spec: TA-H-11 — session avec sleep_start == sleep_end → pas d'exception, canvas placeholder
    @Test
    fun hypnogramScreen_zero_duration_does_not_crash() {
        val sessionZeroDuration = SleepSessionResponse(
            id          = "hyp-001",
            sleep_start = "2026-05-07T23:15:00+02:00",
            sleep_end   = "2026-05-07T23:15:00+02:00",
            created_at  = null,
            stages      = listOf(
                SleepStageResponse(
                    id         = "h1",
                    session_id = "hyp-001",
                    stage      = "DEEP",
                    stage_start = "2026-05-07T23:15:00+02:00",
                    stage_end   = "2026-05-07T23:15:00+02:00"
                )
            )
        )
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success(listOf(sessionZeroDuration))
        )

        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = {}
                )
            }
        }

        // spec: TA-H-11 — aucune exception, canvas visible (placeholder)
        composeTestRule.onNode(hasTestTag("hyp_canvas")).assertExists()
    }

    // spec: TA-H-12 — clic sur bouton back → callback onBack appelé
    @Test
    fun hypnogramScreen_back_button_calls_onBack() {
        val viewModel = buildViewModel()
        injectHypnogramState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success(listOf(fullSession))
        )

        var backCalled = false
        composeTestRule.setContent {
            NightfallTheme(darkTheme = true) {
                fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen(
                    viewModel = viewModel,
                    onBack    = { backCalled = true }
                )
            }
        }

        // spec: TA-H-12 — clic sur l'icône ArrowBack → onBack() déclenché
        composeTestRule
            .onNode(androidx.compose.ui.test.hasContentDescription("Retour"))
            .performClick()

        assert(backCalled) { "onBack callback must be called when back arrow is clicked — spec: TA-H-12" }
    }
}

// ---------------------------------------------------------------------------
// Class 3 — HypnogramViewModel unit tests (pure JVM, pas de Compose)
// spec: TA-H-VM-01, TA-H-VM-02, TA-H-VM-03, TA-H-VM-04
// ---------------------------------------------------------------------------

// spec: TA-H-VM-01 — loadSession() success → HypnogramUiState.Success(session)
// spec: TA-H-VM-02 — sessionId introuvable dans la liste → HypnogramUiState.Error("Session introuvable")
// spec: TA-H-VM-03 — repository.getSessions() failure → HypnogramUiState.Error
// spec: TA-H-VM-04 — état Loading émis synchroniquement avant que la coroutine suspende
// RED by construction: HypnogramViewModel et HypnogramUiState n'existent pas encore
@OptIn(ExperimentalCoroutinesApi::class)
class HypnogramViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SleepRepository
    private lateinit var viewModel: fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel

    @Before
    fun setUp() {
        // spec: TA-H-VM-01 — viewModelScope utilise Dispatchers.Main ; remplacé par testDispatcher
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        // spec: DI manuelle — HypnogramViewModel(sessionId, repository), init{} déclenche loadSession()
        viewModel = fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel(
            sessionId  = "hyp-001",
            repository = repository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // spec: TA-H-VM-01 — repository retourne [fullSession], loadSession() filtre par "hyp-001"
    //   → uiState = HypnogramUiState.Success(listOf(fullSession))
    // RED: HypnogramViewModel.loadSession() n'existe pas encore
    @Test
    fun hypnogramViewModel_emits_success_when_session_found() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.success(listOf(fullSession))
        )

        // spec: TA-H-VM-01 — init{} appelle loadSession() ; advanceUntilIdle() laisse la coroutine finir
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success) {
            "uiState must be HypnogramUiState.Success when session hyp-001 is found — spec: TA-H-VM-01, got: $state"
        }

        // spec: TA-H-VM-01 — Success.sessions doit contenir la session correspondant au sessionId
        val sessions = (state as fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Success).sessions
        assert(sessions.any { it.id == "hyp-001" }) {
            "Success.sessions must contain session 'hyp-001' — spec: TA-H-VM-01, got: ${sessions.map { it.id }}"
        }
    }

    // spec: TA-H-VM-02 — repository retourne une session avec un id différent ("hyp-999")
    //   → filtre ne trouve pas "hyp-001" → uiState = Error("Session introuvable")
    // spec: "si le filtre ne trouve rien → émet Error('Session introuvable')"
    // RED: HypnogramViewModel.loadSession() n'existe pas encore
    @Test
    fun hypnogramViewModel_emits_error_when_session_not_found() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.success(listOf(otherSession))
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Error) {
            "uiState must be HypnogramUiState.Error when sessionId 'hyp-001' is not in repository results — spec: TA-H-VM-02, got: $state"
        }

        // spec: TA-H-VM-02 — message d'erreur spécifique "Session introuvable"
        val message = (state as fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Error).message
        assert(message == "Session introuvable") {
            "Error.message must be 'Session introuvable' when session not found — spec: TA-H-VM-02, got: '$message'"
        }
    }

    // spec: TA-H-VM-03 — repository.getSessions() retourne Result.failure(IOException)
    //   → uiState = HypnogramUiState.Error avec message non-vide
    // spec: mapError() — IOException → "Vérifiez votre connexion réseau"
    // RED: HypnogramViewModel.loadSession() n'existe pas encore
    @Test
    fun hypnogramViewModel_emits_error_when_repository_fails() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.failure(java.io.IOException("Connection refused"))
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Error) {
            "uiState must be HypnogramUiState.Error when repository returns failure — spec: TA-H-VM-03, got: $state"
        }

        // spec: TA-H-VM-03 — Error.message non-vide (mapError renvoie un message lisible)
        val message = (state as fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Error).message
        assert(message.isNotBlank()) {
            "HypnogramUiState.Error.message must not be blank — spec: TA-H-VM-03, got: '$message'"
        }
    }

    // spec: TA-H-VM-04 — après loadSession() et AVANT advanceUntilIdle() → uiState = Loading
    // spec: "loadSession() émet Loading en premier, avant tout appel réseau"
    // RED: HypnogramViewModel.loadSession() n'existe pas encore
    @Test
    fun hypnogramViewModel_emits_loading_while_in_flight() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.success(listOf(fullSession))
        )

        // spec: TA-H-VM-04 — init{} appelle loadSession() ; sans advanceUntilIdle() la coroutine
        //   est suspendue à getSessions() et uiState doit déjà être Loading
        val stateWhileInFlight = viewModel.uiState.value
        assert(stateWhileInFlight is fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState.Loading) {
            "uiState must be HypnogramUiState.Loading immediately after init{} — spec: TA-H-VM-04, got: $stateWhileInFlight"
        }

        advanceUntilIdle()
    }
}
