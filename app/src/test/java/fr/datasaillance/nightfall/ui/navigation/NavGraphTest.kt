package fr.datasaillance.nightfall.ui.navigation

// spec: Tests d'acceptation TA-01, TA-02, TA-03, TA-08, TA-11
// spec: section "Navigation graph"
// RED by construction: fr.datasaillance.nightfall.ui.navigation.* does not exist yet

import androidx.navigation.testing.TestNavHostController
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.ui.navigation.NavDestination
// fr.datasaillance.nightfall.ui.navigation.NavGraph
// fr.datasaillance.nightfall.ui.navigation.BottomNavBar
// fr.datasaillance.nightfall.ui.theme.NightfallTheme

@RunWith(RobolectricTestRunner::class)
class NavGraphTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    // Virage on-device : plus d'authentification, l'app démarre toujours sur SleepScreen
    @Test
    fun navGraph_startsOnSleep() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                NavGraph(navController = navController)
            }
        }

        assert(navController.currentDestination?.route == NavDestination.Sleep.route) {
            "Expected startDestination=sleep"
        }
    }

    // spec: TA-03 — l'utilisateur tape sur l'onglet "Timeline" → TimelineScreen affiché
    @Test
    fun navGraph_bottomNav_switchesToTimeline() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                NavGraph(navController = navController)
            }
        }

        // spec: TA-03 — bottom nav label "Timeline" must navigate to TimelineScreen
        composeTestRule.onNodeWithText("Timeline").performClick()

        assert(navController.currentDestination?.route == NavDestination.Timeline.route) {
            "Expected navigation to timeline after clicking Timeline tab — spec: TA-03"
        }
    }

    // spec: TA-03 (coverage extension) — onglet "Activité" → ActivityScreen
    @Test
    fun navGraph_bottomNav_switchesToActivity() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                NavGraph(navController = navController)
            }
        }

        // spec: navigation graph section — tab "Activité" (route: "activity")
        composeTestRule.onNodeWithText("Cadran").performClick()

        assert(navController.currentDestination?.route == NavDestination.Activity.route) {
            "Expected navigation to activity after clicking Activité tab — spec: navigation graph"
        }
    }

    // spec: navigation graph section — tab "Profil" (route: "profile")
    @Test
    fun navGraph_bottomNav_switchesToProfile() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                NavGraph(navController = navController)
            }
        }

        composeTestRule.onNodeWithText("Profil").performClick()

        assert(navController.currentDestination?.route == NavDestination.Profile.route) {
            "Expected navigation to profile after clicking Profil tab — spec: navigation graph"
        }
    }

    // spec: TA-08 — utilisateur sur ProfileScreen, appui "Importer données" → ImportScreen
    @Test
    fun navGraph_profileScreen_importButtonNavigatesToImport() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                NavGraph(navController = navController)
            }
        }

        // Navigate to ProfileScreen first
        composeTestRule.onNodeWithText("Profil").performClick()

        // spec: TA-08 — ligne "Sources de données" (import Takeout) du Profil, qui défile
        composeTestRule.onNodeWithText("Sources de données").performScrollTo().performClick()

        assert(navController.currentDestination?.route == NavDestination.Import.route) {
            "Expected navigation to import from ProfileScreen — spec: TA-08, got: ${navController.currentDestination?.route}"
        }
    }



}
