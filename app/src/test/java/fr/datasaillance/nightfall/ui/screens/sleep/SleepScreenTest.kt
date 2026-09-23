package fr.datasaillance.nightfall.ui.screens.sleep

// spec: Tests d'acceptation TA-S-01, TA-S-02, TA-S-03, TA-S-04, TA-S-07, TA-S-07b, TA-S-08,
//       TA-S-09, TA-S-10, TA-S-11, TA-S-12
// spec: section "SleepScreen Night Cards" — layout, états, interactions, snapshots
// RED by construction: the following classes do not exist yet and imports will fail at compile time:
//   fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
//   fr.datasaillance.nightfall.data.sleep.SleepStageResponse
//   fr.datasaillance.nightfall.data.sleep.SleepRepository
//   fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState
//   fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel
//   fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen  (stub will be replaced)

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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

// ---------------------------------------------------------------------------
// Test fixtures — shared across all three test classes
// ---------------------------------------------------------------------------

// spec: TA-S-03/TA-S-04 — session avec stages, durée 7h23
private val session1 = fr.datasaillance.nightfall.data.sleep.SleepSessionResponse(
    id = "aaa-111",
    sleep_start = "2026-05-07T23:15:00+02:00",
    sleep_end = "2026-05-08T06:38:00+02:00",
    created_at = "2026-05-08T10:00:00Z",
    stages = listOf(
        fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
            id = "s1",
            session_id = "aaa-111",
            stage = "DEEP",
            stage_start = "2026-05-08T01:00:00+02:00",
            stage_end = "2026-05-08T02:30:00+02:00"
        ),
        fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
            id = "s2",
            session_id = "aaa-111",
            stage = "LIGHT",
            stage_start = "2026-05-08T02:30:00+02:00",
            stage_end = "2026-05-08T06:38:00+02:00"
        )
    )
)

// spec: TA-S-03/TA-S-04 — session sans stages, durée 6h00
private val session2 = fr.datasaillance.nightfall.data.sleep.SleepSessionResponse(
    id = "bbb-222",
    sleep_start = "2026-05-06T00:00:00Z",
    sleep_end = "2026-05-06T06:00:00Z",
    created_at = null,
    stages = null
)

// spec: TA-S-02 sort — session courte, durée 4h30 (oldest — must appear last after desc sort)
private val session3 = fr.datasaillance.nightfall.data.sleep.SleepSessionResponse(
    id = "ccc-333",
    sleep_start = "2026-05-05T02:00:00Z",
    sleep_end = "2026-05-05T06:30:00Z",
    created_at = null,
    stages = null
)

private val threeSessionsAscOrder = listOf(session3, session2, session1)

// ---------------------------------------------------------------------------
// Helper: inject state into SleepViewModel via reflection on _uiState
// ---------------------------------------------------------------------------

@Suppress("UNCHECKED_CAST")
private fun injectSleepState(
    viewModel: fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel,
    state: fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState
) {
    val field = fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel::class.java
        .getDeclaredField("_uiState")
    field.isAccessible = true
    (field.get(viewModel) as MutableStateFlow<fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState>)
        .value = state
}

// ---------------------------------------------------------------------------
// Class 1 — Paparazzi snapshot tests (no @RunWith — incompatible with Robolectric)
// spec: TA-S-09, TA-S-10, TA-S-11, TA-S-12
// ---------------------------------------------------------------------------

class SleepScreenSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    private fun buildViewModel(
        repository: fr.datasaillance.nightfall.data.sleep.SleepRepository =
            mock<fr.datasaillance.nightfall.data.sleep.SleepRepository>()
    ): fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel =
        fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel(repository)

    // spec: TA-S-09 — SleepScreen dark mode, ViewModel en état Success avec 3 sessions
    // spec: "Parité light / dark mode" — fond #191E22, surface #232E32, teal #0E9EB0
    // RED by construction: SleepScreen and SleepViewModel do not exist yet
    @Test
    fun sleepScreen_success_dark() {
        val viewModel = buildViewModel()
        injectSleepState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(
                listOf(session1, session2, session3)
            )
        )

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-S-09 — SleepScreen receives SleepViewModel and onSessionClick callback
                fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen(
                    viewModel = viewModel,
                    onSessionClick = {}
                )
            }
        }
        // spec: TA-S-09 — snapshot dark: fond #191E22, 3 night cards visibles
    }

    // spec: TA-S-10 — SleepScreen light mode, même état Success
    // spec: "Parité light / dark mode" — fond clair, même teal #0E9EB0 et amber #D37C04
    // RED by construction: SleepScreen and SleepViewModel do not exist yet
    @Test
    fun sleepScreen_success_light() {
        val viewModel = buildViewModel()
        injectSleepState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(
                listOf(session1, session2, session3)
            )
        )

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = false) {
                // spec: TA-S-10 — light mode snapshot: fond clair, night cards rendues
                fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen(
                    viewModel = viewModel,
                    onSessionClick = {}
                )
            }
        }
        // spec: TA-S-10 — snapshot light: fond #FAFAFA ou équivalent, 3 cards visibles
    }

    // spec: TA-S-11 — SleepScreen dark mode, ViewModel en état Loading
    // RED by construction: SleepUiState.Loading does not exist yet
    @Test
    fun sleepScreen_loading_dark() {
        val viewModel = buildViewModel()
        injectSleepState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Loading
        )

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-S-11 — Loading state: CircularProgressIndicator ou skeleton visible
                fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen(
                    viewModel = viewModel,
                    onSessionClick = {}
                )
            }
        }
        // spec: TA-S-11 — snapshot dark Loading: indicateur de chargement présent, liste absente
    }

    // spec: TA-S-12 — SleepScreen dark mode, ViewModel en état Error
    // RED by construction: SleepUiState.Error does not exist yet
    @Test
    fun sleepScreen_error_dark() {
        val viewModel = buildViewModel()
        injectSleepState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Error(
                "Vérifiez votre connexion réseau"
            )
        )

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-S-12 — Error state: message d'erreur + bouton retry visibles
                fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen(
                    viewModel = viewModel,
                    onSessionClick = {}
                )
            }
        }
        // spec: TA-S-12 — snapshot dark Error: "Vérifiez votre connexion réseau" visible, bouton retry présent
    }
}

// ---------------------------------------------------------------------------
// Class 2 — Robolectric behavioral/interaction tests
// spec: TA-S-01, TA-S-02, TA-S-07, TA-S-07b, TA-S-08
// Separate class from Paparazzi: incompatible Rule lifecycles
// ---------------------------------------------------------------------------

// spec: TA-S-01 — Loading: sleep_loading visible, sleep_list absent
// spec: TA-S-02 — Success(3): sleep_list visible, sleep_card_${id} pour chacune
// spec: TA-S-07 — Error: sleep_error visible, sleep_retry visible
// spec: TA-S-07b — click sleep_retry: state revient à Loading
// spec: TA-S-08 — Empty: sleep_empty visible, sleep_list absent
// RED by construction: SleepScreen test tags (sleep_loading, sleep_list, sleep_error,
//   sleep_retry, sleep_empty, sleep_card_${id}) n'existent pas dans le stub actuel
@RunWith(RobolectricTestRunner::class)
class SleepScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildViewModel(
        repository: fr.datasaillance.nightfall.data.sleep.SleepRepository =
            mock<fr.datasaillance.nightfall.data.sleep.SleepRepository>()
    ): fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel =
        fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel(repository)

    // spec: TA-S-01 — état Loading: sleep_loading assertExists, sleep_list assertDoesNotExist
    // RED: le stub SleepScreen n'a pas de testTag "sleep_loading" ni "sleep_list"
    @Test
    fun sleepScreen_shows_loading_indicator() {
        val viewModel = buildViewModel()
        injectSleepState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Loading
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-S-01 — SleepScreen avec ViewModel en Loading
                fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen(
                    viewModel = viewModel,
                    onSessionClick = {}
                )
            }
        }

        // spec: TA-S-01 — indicateur de chargement présent
        composeTestRule
            .onNode(hasTestTag("sleep_loading"))
            .assertExists()

        // spec: TA-S-01 — liste absente en état Loading
        composeTestRule
            .onNode(hasTestTag("sleep_list"))
            .assertDoesNotExist()
    }

    // spec: TA-S-02 — état Success(3 sessions): sleep_list assertExists,
    //   sleep_card_aaa-111, sleep_card_bbb-222, sleep_card_ccc-333 assertExists chacun
    // RED: le stub SleepScreen n'expose pas sleep_list ni sleep_card_${id}
    @Test
    fun sleepScreen_shows_list_when_success() {
        val viewModel = buildViewModel()
        injectSleepState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(
                listOf(session1, session2, session3)
            )
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-S-02 — SleepScreen avec 3 sessions
                fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen(
                    viewModel = viewModel,
                    onSessionClick = {}
                )
            }
        }

        // spec: TA-S-02 — conteneur de liste présent
        composeTestRule
            .onNode(hasTestTag("sleep_list"))
            .assertExists()

        // spec: TA-S-02 — une card par session, testTag = "sleep_card_${session.id}"
        composeTestRule
            .onNode(hasTestTag("sleep_card_aaa-111"))
            .assertExists()
        composeTestRule
            .onNode(hasTestTag("sleep_card_bbb-222"))
            .assertExists()
        composeTestRule
            .onNode(hasTestTag("sleep_card_ccc-333"))
            .assertExists()
    }

    // spec: TA-S-07 — état Error: sleep_error assertExists, sleep_retry assertExists
    // RED: le stub SleepScreen n'expose pas sleep_error ni sleep_retry
    @Test
    fun sleepScreen_shows_error_with_retry_button() {
        val viewModel = buildViewModel()
        injectSleepState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Error(
                "Vérifiez votre connexion réseau"
            )
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-S-07 — SleepScreen avec ViewModel en Error
                fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen(
                    viewModel = viewModel,
                    onSessionClick = {}
                )
            }
        }

        // spec: TA-S-07 — bloc d'erreur visible
        composeTestRule
            .onNode(hasTestTag("sleep_error"))
            .assertExists()

        // spec: TA-S-07 — bouton "Réessayer" visible en état Error
        composeTestRule
            .onNode(hasTestTag("sleep_retry"))
            .assertExists()
    }

    // spec: TA-S-08 — état Empty: sleep_empty assertExists, sleep_list assertDoesNotExist
    // RED: le stub SleepScreen n'expose pas sleep_empty
    @Test
    fun sleepScreen_shows_empty_state() {
        val viewModel = buildViewModel()
        injectSleepState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Empty
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-S-08 — SleepScreen avec ViewModel en Empty (aucune session importée)
                fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen(
                    viewModel = viewModel,
                    onSessionClick = {}
                )
            }
        }

        // spec: TA-S-08 — état vide visible (message "Aucune nuit enregistrée" ou équivalent)
        composeTestRule
            .onNode(hasTestTag("sleep_empty"))
            .assertExists()

        // spec: TA-S-08 — liste absente en état Empty
        composeTestRule
            .onNode(hasTestTag("sleep_list"))
            .assertDoesNotExist()
    }

    // spec: TA-S-07b — click sur sleep_retry: l'état repasse à Loading via SleepViewModel.loadSessions()
    // RED: le stub SleepScreen n'a pas de bouton sleep_retry fonctionnel
    @Suppress("UNCHECKED_CAST")
    @Test
    fun sleepScreen_retry_button_triggers_reload() {
        val repository = mock<fr.datasaillance.nightfall.data.sleep.SleepRepository>()
        val viewModel = buildViewModel(repository)

        // Pré-condition: state est Error pour que le bouton retry soit affiché
        injectSleepState(
            viewModel,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Error(
                "Vérifiez votre connexion réseau"
            )
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-S-07b — SleepScreen en Error avec bouton retry
                fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen(
                    viewModel = viewModel,
                    onSessionClick = {}
                )
            }
        }

        // spec: TA-S-07b — click sur le bouton retry
        composeTestRule
            .onNode(hasTestTag("sleep_retry"))
            .performClick()

        // spec: TA-S-07b — après le click, l'état doit repasser à Loading
        // (SleepViewModel.loadSessions() remet _uiState = Loading avant l'appel réseau)
        val uiStateField = fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel::class.java
            .getDeclaredField("_uiState")
        uiStateField.isAccessible = true
        val stateFlow = uiStateField.get(viewModel)
            as MutableStateFlow<fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState>
        val stateAfterRetry = stateFlow.value

        assert(stateAfterRetry is fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Loading) {
            "uiState must be SleepUiState.Loading after retry button click — spec: TA-S-07b, got: $stateAfterRetry"
        }
    }
}

// ---------------------------------------------------------------------------
// Class 3 — SleepViewModel unit tests (pure JVM, no Compose, no Robolectric)
// spec: TA-S-03, TA-S-04, TA-S-07 logic, TA-S-08 logic, TA-S-02 sort
// ---------------------------------------------------------------------------

// spec: TA-S-03/TA-S-04 — SleepViewModel.loadSessions() success → SleepUiState.Success
// spec: TA-S-07 logic — SleepViewModel.loadSessions() failure → SleepUiState.Error
// spec: TA-S-08 logic — SleepViewModel.loadSessions() returns empty → SleepUiState.Empty
// spec: TA-S-02 sort — sessions triées par sleep_start desc dans Success.sessions
// RED by construction: SleepViewModel, SleepRepository, SleepUiState n'existent pas encore
@OptIn(ExperimentalCoroutinesApi::class)
class SleepViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: fr.datasaillance.nightfall.data.sleep.SleepRepository
    private lateinit var viewModel: fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel

    @Before
    fun setUp() {
        // spec: TA-S-03 — viewModelScope utilise Dispatchers.Main; remplacé par testDispatcher
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        // spec: DI manuelle — pas de Hilt, SleepViewModel construit directement avec repository
        viewModel = fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // spec: TA-S-03 / TA-S-04 — loadSessions() success → SleepUiState.Success avec les sessions
    // RED: SleepViewModel.loadSessions() n'existe pas encore
    @Test
    fun sleepViewModel_emits_success_when_repository_returns_sessions() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.success(listOf(session1, session2))
        )

        viewModel.loadSessions()
        advanceUntilIdle()

        // spec: TA-S-03 — uiState doit être SleepUiState.Success après un appel réussi
        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success) {
            "uiState must be SleepUiState.Success when repository returns sessions — spec: TA-S-03, got: $state"
        }

        // spec: TA-S-04 — Success.sessions contient bien les sessions retournées
        val sessions = (state as fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success).sessions
        assert(sessions.size == 2) {
            "Success.sessions must contain 2 sessions — spec: TA-S-04, got: ${sessions.size}"
        }
    }

    // spec: TA-S-07 logic — loadSessions() failure (IOException) → SleepUiState.Error
    // RED: SleepViewModel.loadSessions() n'existe pas encore
    @Test
    fun sleepViewModel_emits_error_when_repository_fails() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.failure(java.io.IOException("Connection refused"))
        )

        viewModel.loadSessions()
        advanceUntilIdle()

        // spec: TA-S-07 — uiState doit être SleepUiState.Error si le repository échoue
        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Error) {
            "uiState must be SleepUiState.Error when repository returns failure — spec: TA-S-07, got: $state"
        }

        // spec: TA-S-07 — Error.message doit être non-null et non-vide
        val message = (state as fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Error).message
        assert(message.isNotBlank()) {
            "SleepUiState.Error.message must not be blank — spec: TA-S-07, got: '$message'"
        }
    }

    // spec: TA-S-08 logic — loadSessions() returns empty list → SleepUiState.Empty
    // RED: SleepViewModel.loadSessions() et SleepUiState.Empty n'existent pas encore
    @Test
    fun sleepViewModel_emits_empty_when_no_sessions() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.success(emptyList())
        )

        viewModel.loadSessions()
        advanceUntilIdle()

        // spec: TA-S-08 — uiState doit être SleepUiState.Empty si le repository retourne une liste vide
        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Empty) {
            "uiState must be SleepUiState.Empty when repository returns empty list — spec: TA-S-08, got: $state"
        }
    }

    // spec: TA-S-02 sort — sessions triées par sleep_start descending dans Success.sessions
    // 3 sessions données en ordre ascendant (session3=oldest → session1=newest)
    // après loadSessions(), Success.sessions doit être [session1, session2, session3] (desc)
    // RED: SleepViewModel.loadSessions() n'existe pas encore
    @Test
    fun sleepViewModel_sorts_sessions_by_start_desc() = runTest {
        // Données en ordre ascendant: oldest first (session3 < session2 < session1)
        whenever(repository.getSessions()).thenReturn(
            Result.success(threeSessionsAscOrder)
        )

        viewModel.loadSessions()
        advanceUntilIdle()

        // spec: TA-S-02 sort — Success.sessions trié par sleep_start desc (newest first)
        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success) {
            "uiState must be SleepUiState.Success — spec: TA-S-02 sort, got: $state"
        }
        val sessions = (state as fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success).sessions
        assert(sessions.size == 3) {
            "Success.sessions must contain 3 sessions — spec: TA-S-02 sort, got: ${sessions.size}"
        }
        // spec: TA-S-02 sort — premier élément = session la plus récente (session1: 2026-05-07T23:15)
        assert(sessions[0].id == "aaa-111") {
            "sessions[0] must be session1 (most recent) — spec: TA-S-02 sort, got id: ${sessions[0].id}"
        }
        // spec: TA-S-02 sort — deuxième élément = session2 (2026-05-06T00:00)
        assert(sessions[1].id == "bbb-222") {
            "sessions[1] must be session2 — spec: TA-S-02 sort, got id: ${sessions[1].id}"
        }
        // spec: TA-S-02 sort — troisième élément = session3 (oldest: 2026-05-05T02:00)
        assert(sessions[2].id == "ccc-333") {
            "sessions[2] must be session3 (oldest) — spec: TA-S-02 sort, got id: ${sessions[2].id}"
        }
    }

    // spec: TA-S-03 — état Loading émis synchroniquement avant que la coroutine suspende
    // RED: SleepViewModel.loadSessions() n'existe pas encore
    @Test
    fun sleepViewModel_emits_loading_while_in_flight() = runTest {
        whenever(repository.getSessions()).thenReturn(
            Result.success(listOf(session1))
        )

        // spec: TA-S-03 — _uiState = Loading doit être positionné AVANT que la coroutine atteigne getSessions()
        viewModel.loadSessions()

        // Avant advanceUntilIdle() — la coroutine est suspendue à getSessions()
        val stateWhileInFlight = viewModel.uiState.value
        assert(stateWhileInFlight is fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Loading) {
            "uiState must be SleepUiState.Loading immediately after loadSessions() — spec: TA-S-03, got: $stateWhileInFlight"
        }

        advanceUntilIdle()
    }
}
