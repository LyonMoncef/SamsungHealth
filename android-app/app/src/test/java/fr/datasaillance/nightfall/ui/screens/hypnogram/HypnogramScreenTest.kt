package fr.datasaillance.nightfall.ui.screens.hypnogram

// spec: Tests d'acceptation TA-H-01 à TA-H-12
// spec: section "Hypnogramme" — navigation, canvas, légende, KPIs, buildSegments, snapshots
// RED by construction: the following symbols do not exist yet and imports will fail at compile time:
//   fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen
//   fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramCanvas
//   fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramSegment
//   fr.datasaillance.nightfall.ui.screens.hypnogram.buildSegments
//   fr.datasaillance.nightfall.ui.navigation.NavDestination.Hypnogram

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import java.time.OffsetDateTime

// ---------------------------------------------------------------------------
// Test fixtures — shared across all three test classes
// ---------------------------------------------------------------------------

// spec: TA-H-03, TA-H-04, TA-H-05, TA-H-06 — session complète avec 4 types de stages
// sleep_start 23:00 → sleep_end 06:23 = 7h23 total
// DEEP 90min, LIGHT 195min, REM 60min, AWAKE 38min — stages contiguous, couvrent la session entière
private val sessionFull = fr.datasaillance.nightfall.data.sleep.SleepSessionResponse(
    id = "hyp-001",
    sleep_start = "2026-05-07T23:00:00+02:00",
    sleep_end = "2026-05-08T06:23:00+02:00",
    created_at = "2026-05-08T10:00:00Z",
    stages = listOf(
        fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
            id = "h1",
            session_id = "hyp-001",
            stage = "DEEP",
            stage_start = "2026-05-08T00:00:00+02:00",
            stage_end = "2026-05-08T01:30:00+02:00"
        ),
        fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
            id = "h2",
            session_id = "hyp-001",
            stage = "LIGHT",
            stage_start = "2026-05-08T01:30:00+02:00",
            stage_end = "2026-05-08T04:45:00+02:00"
        ),
        fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
            id = "h3",
            session_id = "hyp-001",
            stage = "REM",
            stage_start = "2026-05-08T04:45:00+02:00",
            stage_end = "2026-05-08T05:45:00+02:00"
        ),
        fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
            id = "h4",
            session_id = "hyp-001",
            stage = "AWAKE",
            stage_start = "2026-05-08T05:45:00+02:00",
            stage_end = "2026-05-08T06:23:00+02:00"
        )
    )
)

// spec: TA-H-08 — session sans stages (stages = null)
private val sessionNoStages = fr.datasaillance.nightfall.data.sleep.SleepSessionResponse(
    id = "hyp-002",
    sleep_start = "2026-05-06T01:00:00+02:00",
    sleep_end = "2026-05-06T07:00:00+02:00",
    created_at = null,
    stages = null
)

// spec: TA-H-12 — session avec uniquement DEEP + REM, contiguous (pas de LIGHT, pas de gaps)
// stages couvrent exactement la durée totale — sleep_start à sleep_end sans trou
private val sessionDeepAndRemOnly = fr.datasaillance.nightfall.data.sleep.SleepSessionResponse(
    id = "hyp-003",
    sleep_start = "2026-05-05T22:00:00+02:00",
    sleep_end = "2026-05-06T06:00:00+02:00",
    created_at = null,
    stages = listOf(
        fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
            id = "h5",
            session_id = "hyp-003",
            stage = "DEEP",
            stage_start = "2026-05-05T22:00:00+02:00",
            stage_end = "2026-05-06T00:30:00+02:00"
        ),
        fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
            id = "h6",
            session_id = "hyp-003",
            stage = "REM",
            stage_start = "2026-05-06T00:30:00+02:00",
            stage_end = "2026-05-06T06:00:00+02:00"
        )
    )
)

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
// spec: TA-H-09, TA-H-10
// ---------------------------------------------------------------------------

class HypnogramScreenSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    private fun buildViewModel(): fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel =
        fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel(
            mock<fr.datasaillance.nightfall.data.sleep.SleepRepository>()
        )

    // spec: TA-H-09 — snapshot dark mode: fond #191E22, blocs Canvas colorés par stage
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_full_session_dark() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(
                listOf(sessionFull)
            )
        )

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-H-09 — HypnogramScreen dark, sessionFull (4 types de stages)
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "hyp-001",
                    sleepViewModel = vm,
                    onBack = {}
                )
            }
        }
        // spec: TA-H-09 — golden dark: fond #191E22, canvas teal/muted/cyan/amber visible
    }

    // spec: TA-H-10 — snapshot light mode: fond #FAFAFA, couleurs stages identiques (non-themées)
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_full_session_light() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(
                listOf(sessionFull)
            )
        )

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = false) {
                // spec: TA-H-10 — HypnogramScreen light, même session
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "hyp-001",
                    sleepViewModel = vm,
                    onBack = {}
                )
            }
        }
        // spec: TA-H-10 — golden light: fond #FAFAFA, couleurs stages identiques dark/light
    }
}

// ---------------------------------------------------------------------------
// Class 2 — Robolectric behavioral/interaction tests
// spec: TA-H-01, TA-H-02, TA-H-03, TA-H-04, TA-H-05, TA-H-06, TA-H-07, TA-H-08, TA-H-12
// ---------------------------------------------------------------------------

@RunWith(RobolectricTestRunner::class)
class HypnogramScreenInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildViewModel(): fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel =
        fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel(
            mock<fr.datasaillance.nightfall.data.sleep.SleepRepository>()
        )

    // spec: TA-H-01 — NavDestination.Hypnogram route helper retourne "hypnogram/{sessionId}"
    // spec: NavDestination.Hypnogram ne doit PAS être dans bottomNavItems()
    // RED by construction: NavDestination.Hypnogram does not exist yet
    @Test
    fun hypnogramScreen_nav_dest_route_is_correct() {
        // spec: TA-H-01 — route("abc-123") == "hypnogram/abc-123"
        assert(
            fr.datasaillance.nightfall.ui.navigation.NavDestination.Hypnogram.route("abc-123") == "hypnogram/abc-123"
        ) {
            "NavDestination.Hypnogram.route(\"abc-123\") must equal \"hypnogram/abc-123\" — spec: TA-H-01"
        }

        // spec: TA-H-01 — Hypnogram n'est pas un onglet bottom nav (destination de détail)
        val bottomRoutes = fr.datasaillance.nightfall.ui.navigation.NavDestination.bottomNavItems()
            .map { it.route }
        assert(
            fr.datasaillance.nightfall.ui.navigation.NavDestination.Hypnogram.route !in bottomRoutes
        ) {
            "NavDestination.Hypnogram.route must NOT be in bottomNavItems() — spec: TA-H-01, bottomRoutes=$bottomRoutes"
        }
    }

    // spec: TA-H-02 — bouton retour (contentDescription "Retour") invoque le callback onBack
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_back_button_invokes_callback() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(listOf(sessionFull))
        )

        var wasCalled = false

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-H-02 — HypnogramScreen avec onBack lambda
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "hyp-001",
                    sleepViewModel = vm,
                    onBack = { wasCalled = true }
                )
            }
        }

        // spec: TA-H-02 — clic sur la flèche retour de la TopAppBar (contentDescription "Retour")
        composeTestRule
            .onNode(
                androidx.compose.ui.test.hasContentDescription("Retour")
            )
            .performClick()

        // spec: TA-H-02 — après le clic, onBack doit avoir été invoqué
        assert(wasCalled) {
            "onBack lambda must be invoked when back button is clicked — spec: TA-H-02"
        }
    }

    // spec: TA-H-03 — canvas visible quand la session a des stages non vides
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_canvas_visible_when_stages_present() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(listOf(sessionFull))
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-H-03 — HypnogramScreen avec session ayant des stages
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "hyp-001",
                    sleepViewModel = vm,
                    onBack = {}
                )
            }
        }

        // spec: TA-H-03 — testTag "hypnogram_canvas" present when stages not empty
        composeTestRule
            .onNode(hasTestTag("hypnogram_canvas"))
            .assertExists()
    }

    // spec: TA-H-04 — légende contient les 4 labels quand les 4 types de stages sont présents
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_legend_shows_all_four_stage_types() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(listOf(sessionFull))
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-H-04 — session avec DEEP, LIGHT, REM, AWAKE
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "hyp-001",
                    sleepViewModel = vm,
                    onBack = {}
                )
            }
        }

        // spec: TA-H-04 — bloc légende présent
        composeTestRule
            .onNode(hasTestTag("hypnogram_legend"))
            .assertExists()

        // spec: TA-H-04 — DEEP → "Profond"
        composeTestRule
            .onNodeWithText("Profond")
            .assertExists()

        // spec: TA-H-04 — LIGHT → "Léger"
        composeTestRule
            .onNodeWithText("Léger")
            .assertExists()

        // spec: TA-H-04 — REM → "REM"
        composeTestRule
            .onNodeWithText("REM")
            .assertExists()

        // spec: TA-H-04 — AWAKE → "Éveil"
        composeTestRule
            .onNodeWithText("Éveil")
            .assertExists()
    }

    // spec: TA-H-05 — KPI durée totale: sleep_start 23:00 → sleep_end 06:23 = "7h 23"
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_kpis_show_correct_total_duration() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(listOf(sessionFull))
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-H-05 — session sleep_start 23:00 → sleep_end 06:23 = 7h23
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "hyp-001",
                    sleepViewModel = vm,
                    onBack = {}
                )
            }
        }

        // spec: TA-H-05 — bloc KPIs présent
        composeTestRule
            .onNode(hasTestTag("hypnogram_kpis"))
            .assertExists()

        // spec: TA-H-05 — durée totale formatée "7h 23" (heures + minutes)
        composeTestRule
            .onNodeWithText("7h 23")
            .assertExists()
    }

    // spec: TA-H-06 — KPI durée DEEP: 90min de DEEP dans sessionFull → "1h 30"
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_kpis_show_correct_deep_duration() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(listOf(sessionFull))
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-H-06 — sessionFull: DEEP 00:00→01:30 = 90min
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "hyp-001",
                    sleepViewModel = vm,
                    onBack = {}
                )
            }
        }

        // spec: TA-H-06 — durée DEEP = 90min → "1h 30" associé au label "Profond"
        composeTestRule
            .onNodeWithText("1h 30")
            .assertExists()
    }

    // spec: TA-H-07 — état not_found quand sessionId ne correspond à aucune session
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_not_found_when_session_id_invalid() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(listOf(sessionFull))
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-H-07 — sessionId "invalid-id" absent de la liste
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "invalid-id",
                    sleepViewModel = vm,
                    onBack = {}
                )
            }
        }

        // spec: TA-H-07 — testTag "hypnogram_not_found" visible quand session introuvable
        composeTestRule
            .onNode(hasTestTag("hypnogram_not_found"))
            .assertExists()

        // spec: TA-H-07 — canvas absent quand session introuvable
        composeTestRule
            .onNode(hasTestTag("hypnogram_canvas"))
            .assertDoesNotExist()
    }

    // spec: TA-H-08 — état no_stages quand la session existe mais stages == null
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_no_stages_state_when_stages_null() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(listOf(sessionNoStages))
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-H-08 — sessionNoStages: stages = null
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "hyp-002",
                    sleepViewModel = vm,
                    onBack = {}
                )
            }
        }

        // spec: TA-H-08 — testTag "hypnogram_no_stages" visible quand stages null ou vides
        composeTestRule
            .onNode(hasTestTag("hypnogram_no_stages"))
            .assertExists()

        // spec: TA-H-08 — canvas absent quand pas de stages
        composeTestRule
            .onNode(hasTestTag("hypnogram_canvas"))
            .assertDoesNotExist()
    }

    // spec: TA-H-12 — légende n'affiche que les types présents (DEEP + REM only, pas LIGHT ni AWAKE)
    // sessionDeepAndRemOnly: stages contiguous de sessionStart à sessionEnd — pas de gap AWAKE implicite
    // RED by construction: HypnogramScreen does not exist yet
    @Test
    fun hypnogramScreen_legend_omits_absent_stage_types() {
        val vm = buildViewModel()
        injectSleepState(
            vm,
            fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState.Success(listOf(sessionDeepAndRemOnly))
        )

        composeTestRule.setContent {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-H-12 — sessionDeepAndRemOnly: uniquement DEEP + REM, contiguous
                fr.datasaillance.nightfall.ui.screens.hypnogram.HypnogramScreen(
                    sessionId = "hyp-003",
                    sleepViewModel = vm,
                    onBack = {}
                )
            }
        }

        // spec: TA-H-12 — DEEP présent → "Profond" doit apparaître
        composeTestRule
            .onNodeWithText("Profond")
            .assertExists()

        // spec: TA-H-12 — REM présent → "REM" doit apparaître
        composeTestRule
            .onNodeWithText("REM")
            .assertExists()

        // spec: TA-H-12 — LIGHT absent → "Léger" ne doit pas apparaître
        composeTestRule
            .onNodeWithText("Léger")
            .assertDoesNotExist()

        // spec: TA-H-12 — AWAKE absent + pas de gap → "Éveil" ne doit pas apparaître
        composeTestRule
            .onNodeWithText("Éveil")
            .assertDoesNotExist()
    }
}

// ---------------------------------------------------------------------------
// Class 3 — BuildSegmentsTest (pure JVM, no Compose, no Robolectric)
// spec: TA-H-11 — buildSegments() remplit les gaps avec AWAKE
// ---------------------------------------------------------------------------

class BuildSegmentsTest {

    // spec: TA-H-11 — gap entre deux stages → segment AWAKE inséré, somme durées == durée session
    // RED by construction: buildSegments() does not exist yet
    @Test
    fun buildSegments_fills_gaps_with_awake() {
        // Session 8h totale: 00:00 → 08:00
        val sessionStart = OffsetDateTime.parse("2026-05-01T00:00:00+02:00")
        val sessionEnd = OffsetDateTime.parse("2026-05-01T08:00:00+02:00")

        // stages couvrent 7h30: gap de 30min entre les deux stages
        val stages = listOf(
            fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
                id = "g1",
                session_id = "gap-test",
                stage = "DEEP",
                stage_start = "2026-05-01T00:00:00+02:00",
                stage_end = "2026-05-01T03:30:00+02:00"
            ),
            fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
                id = "g2",
                session_id = "gap-test",
                stage = "REM",
                stage_start = "2026-05-01T04:00:00+02:00", // gap de 30min après DEEP
                stage_end = "2026-05-01T08:00:00+02:00"
            )
        )

        // spec: TA-H-11 — buildSegments comble les gaps en AWAKE
        val segments = fr.datasaillance.nightfall.ui.screens.hypnogram.buildSegments(
            stages = stages,
            sessionStart = sessionStart,
            sessionEnd = sessionEnd
        )

        // spec: TA-H-11 — exactement un segment AWAKE correspondant au gap
        val awakeSegments = segments.filter { it.stageType == "AWAKE" }
        assert(awakeSegments.size == 1) {
            "buildSegments must insert exactly 1 AWAKE segment for the 30min gap — spec: TA-H-11, got ${awakeSegments.size} AWAKE segments"
        }

        // spec: TA-H-11 — somme des durées == durée totale de la session (8h en ms)
        val sessionDurationMs = 8L * 60 * 60 * 1000
        val totalSegmentMs = segments.sumOf { it.endMs - it.startMs }
        assert(totalSegmentMs == sessionDurationMs) {
            "Sum of segment durations must equal session duration (${sessionDurationMs}ms) — spec: TA-H-11, got ${totalSegmentMs}ms"
        }
    }

    // spec: TA-H-11 complement — stages parfaitement contiguous → aucun gap AWAKE inséré
    // RED by construction: buildSegments() does not exist yet
    @Test
    fun buildSegments_no_gap_if_stages_contiguous() {
        val sessionStart = OffsetDateTime.parse("2026-05-02T22:00:00+02:00")
        val sessionEnd = OffsetDateTime.parse("2026-05-03T06:00:00+02:00")

        // stages contiguous de sessionStart à sessionEnd sans trou
        val stages = listOf(
            fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
                id = "c1",
                session_id = "contiguous-test",
                stage = "DEEP",
                stage_start = "2026-05-02T22:00:00+02:00",
                stage_end = "2026-05-03T00:30:00+02:00"
            ),
            fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
                id = "c2",
                session_id = "contiguous-test",
                stage = "REM",
                stage_start = "2026-05-03T00:30:00+02:00",
                stage_end = "2026-05-03T06:00:00+02:00"
            )
        )

        val segments = fr.datasaillance.nightfall.ui.screens.hypnogram.buildSegments(
            stages = stages,
            sessionStart = sessionStart,
            sessionEnd = sessionEnd
        )

        // spec: pas de segment AWAKE implicite quand les stages couvrent toute la session
        val awakeSegments = segments.filter { it.stageType == "AWAKE" }
        assert(awakeSegments.isEmpty()) {
            "buildSegments must not insert AWAKE segments when stages are contiguous — spec: TA-H-11, got ${awakeSegments.size} AWAKE segments"
        }

        // spec: somme des durées == durée de la session (8h)
        val sessionDurationMs = 8L * 60 * 60 * 1000
        val totalSegmentMs = segments.sumOf { it.endMs - it.startMs }
        assert(totalSegmentMs == sessionDurationMs) {
            "Sum of segment durations must equal session duration — spec: TA-H-11, got ${totalSegmentMs}ms"
        }
    }

    // spec: TA-H-11 — premier stage démarre après sessionStart → segment AWAKE au début
    // RED by construction: buildSegments() does not exist yet
    @Test
    fun buildSegments_adds_awake_at_start_if_first_stage_late() {
        val sessionStart = OffsetDateTime.parse("2026-05-04T23:00:00+02:00")
        val sessionEnd = OffsetDateTime.parse("2026-05-05T07:00:00+02:00")

        // premier stage commence 10min après sessionStart
        val firstStageStart = OffsetDateTime.parse("2026-05-04T23:10:00+02:00")
        val stages = listOf(
            fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
                id = "late1",
                session_id = "late-start-test",
                stage = "DEEP",
                stage_start = "2026-05-04T23:10:00+02:00",
                stage_end = "2026-05-05T07:00:00+02:00"
            )
        )

        val segments = fr.datasaillance.nightfall.ui.screens.hypnogram.buildSegments(
            stages = stages,
            sessionStart = sessionStart,
            sessionEnd = sessionEnd
        )

        // spec: premier segment doit être AWAKE couvrant [sessionStart, firstStageStart]
        assert(segments.isNotEmpty()) {
            "buildSegments must return at least one segment — spec: TA-H-11"
        }

        val firstSegment = segments.first()
        assert(firstSegment.stageType == "AWAKE") {
            "First segment must be AWAKE when first stage starts after sessionStart — spec: TA-H-11, got: ${firstSegment.stageType}"
        }

        val expectedStartMs = sessionStart.toInstant().toEpochMilli()
        assert(firstSegment.startMs == expectedStartMs) {
            "First AWAKE segment startMs must equal sessionStart — spec: TA-H-11, got: ${firstSegment.startMs}, expected: $expectedStartMs"
        }

        val expectedEndMs = firstStageStart.toInstant().toEpochMilli()
        assert(firstSegment.endMs == expectedEndMs) {
            "First AWAKE segment endMs must equal firstStageStart — spec: TA-H-11, got: ${firstSegment.endMs}, expected: $expectedEndMs"
        }
    }

    // spec: TA-H-11 — dernier stage se termine avant sessionEnd → segment AWAKE en fin
    // RED by construction: buildSegments() does not exist yet
    @Test
    fun buildSegments_adds_awake_at_end_if_last_stage_early() {
        val sessionStart = OffsetDateTime.parse("2026-05-06T00:00:00+02:00")
        val sessionEnd = OffsetDateTime.parse("2026-05-06T08:00:00+02:00")

        // dernier stage se termine 10min avant sessionEnd
        val stages = listOf(
            fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
                id = "early1",
                session_id = "early-end-test",
                stage = "LIGHT",
                stage_start = "2026-05-06T00:00:00+02:00",
                stage_end = "2026-05-06T07:50:00+02:00"
            )
        )

        val segments = fr.datasaillance.nightfall.ui.screens.hypnogram.buildSegments(
            stages = stages,
            sessionStart = sessionStart,
            sessionEnd = sessionEnd
        )

        // spec: dernier segment doit être AWAKE couvrant [lastStageEnd, sessionEnd]
        val lastSegment = segments.last()
        assert(lastSegment.stageType == "AWAKE") {
            "Last segment must be AWAKE when last stage ends before sessionEnd — spec: TA-H-11, got: ${lastSegment.stageType}"
        }
    }

    // spec: TA-H-11 — résultat trié par startMs croissant même si les stages sont fournis dans le désordre
    // RED by construction: buildSegments() does not exist yet
    @Test
    fun buildSegments_returns_ordered_segments() {
        val sessionStart = OffsetDateTime.parse("2026-05-07T22:00:00+02:00")
        val sessionEnd = OffsetDateTime.parse("2026-05-08T06:00:00+02:00")

        // stages fournis en ordre inverse (REM avant DEEP chronologiquement)
        val stages = listOf(
            fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
                id = "o2",
                session_id = "order-test",
                stage = "REM",
                stage_start = "2026-05-08T02:00:00+02:00",
                stage_end = "2026-05-08T06:00:00+02:00"
            ),
            fr.datasaillance.nightfall.data.sleep.SleepStageResponse(
                id = "o1",
                session_id = "order-test",
                stage = "DEEP",
                stage_start = "2026-05-07T22:00:00+02:00",
                stage_end = "2026-05-08T02:00:00+02:00"
            )
        )

        val segments = fr.datasaillance.nightfall.ui.screens.hypnogram.buildSegments(
            stages = stages,
            sessionStart = sessionStart,
            sessionEnd = sessionEnd
        )

        // spec: segments triés par startMs croissant
        for (i in 0 until segments.size - 1) {
            assert(segments[i].startMs <= segments[i + 1].startMs) {
                "Segments must be ordered by startMs ascending — spec: TA-H-11, violation at index $i: ${segments[i].startMs} > ${segments[i + 1].startMs}"
            }
        }

        // spec: premier segment démarre à sessionStart
        assert(segments.first().startMs == sessionStart.toInstant().toEpochMilli()) {
            "First segment must start at sessionStart — spec: TA-H-11"
        }

        // spec: dernier segment se termine à sessionEnd
        assert(segments.last().endMs == sessionEnd.toInstant().toEpochMilli()) {
            "Last segment must end at sessionEnd — spec: TA-H-11"
        }
    }
}
