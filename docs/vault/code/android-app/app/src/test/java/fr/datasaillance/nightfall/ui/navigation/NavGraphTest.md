---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/NavGraphTest.kt
git_blob: b9038b488b5c441a602a3bc442051f46a5a05193
last_synced: '2026-05-09T07:04:02Z'
loc: 246
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/NavGraphTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/NavGraphTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/NavGraphTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.navigation

// spec: Tests d'acceptation TA-01, TA-02, TA-03, TA-08, TA-11
// spec: section "Navigation graph"
// RED by construction: fr.datasaillance.nightfall.ui.navigation.* does not exist yet

import androidx.navigation.testing.TestNavHostController
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.ui.navigation.NavDestination
// fr.datasaillance.nightfall.ui.navigation.NavGraph
// fr.datasaillance.nightfall.ui.navigation.BottomNavBar
// fr.datasaillance.nightfall.ui.theme.NightfallTheme
// fr.datasaillance.nightfall.data.auth.TokenDataStore (fakes needed)

@RunWith(RobolectricTestRunner::class)
class NavGraphTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // spec: TA-01 — application fraîchement installée, TokenDataStore vide → NavHost démarre sur LoginScreen
    @Test
    fun navGraph_noToken_navigatesToLogin() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                // NavGraph must receive a tokenDataStore that returns hasToken() == false
                NavGraph(
                    navController = navController,
                    hasToken = false
                )
            }
        }

        // spec: TA-01 — startDestination = login quand hasToken() == false
        assert(navController.currentDestination?.route == NavDestination.Login.route) {
            "Expected startDestination=login when no token — spec: TA-01"
        }
    }

    // spec: TA-02 — TokenDataStore contient un JWT valide → NavHost démarre sur SleepScreen
    @Test
    fun navGraph_withToken_navigatesToSleep() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                NavGraph(
                    navController = navController,
                    hasToken = true
                )
            }
        }

        // spec: TA-02 — startDestination = sleep quand hasToken() == true
        assert(navController.currentDestination?.route == NavDestination.Sleep.route) {
            "Expected startDestination=sleep when token present — spec: TA-02"
        }
    }

    // spec: TA-03 — l'utilisateur tape sur l'onglet "Timeline" → TimelineScreen affiché
    @Test
    fun navGraph_bottomNav_switchesToTimeline() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                NavGraph(
                    navController = navController,
                    hasToken = true
                )
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
                NavGraph(
                    navController = navController,
                    hasToken = true
                )
            }
        }

        // spec: navigation graph section — tab "Activité" (route: "activity")
        composeTestRule.onNodeWithText("Activité").performClick()

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
                NavGraph(
                    navController = navController,
                    hasToken = true
                )
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
                NavGraph(
                    navController = navController,
                    hasToken = true
                )
            }
        }

        // Navigate to ProfileScreen first
        composeTestRule.onNodeWithText("Profil").performClick()

        // spec: TA-08 — bouton "Importer données" dans ProfileScreen
        composeTestRule.onNodeWithText("Importer données").performClick()

        assert(navController.currentDestination?.route == NavDestination.Import.route) {
            "Expected navigation to import from ProfileScreen — spec: TA-08"
        }
    }

    // spec: TA-11 — bouton "Se déconnecter" → clearToken() + navigate login avec popUpTo
    @Test
    fun navGraph_profileScreen_logoutClearsTokenAndNavigatesToLogin() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                NavGraph(
                    navController = navController,
                    hasToken = true
                )
            }
        }

        composeTestRule.onNodeWithText("Profil").performClick()

        // spec: TA-11 — bouton "Se déconnecter" dans ProfileScreen
        composeTestRule.onNodeWithText("Se déconnecter").performClick()

        // spec: TA-11 — navController navigue vers "login" en vidant le back stack
        assert(navController.currentDestination?.route == NavDestination.Login.route) {
            "Expected navigation to login after logout — spec: TA-11"
        }
    }

    // spec: TA-L-02 — login success → navController navigue vers Sleep, Login popped de la back-stack
    @Test
    fun navGraph_login_success_navigates_to_sleep() {
        val api = mock<NightfallApi>()
        val tokenStore = mock<TokenDataStore>()
        val viewModel = AuthViewModel(api, tokenStore)
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                NavGraph(
                    navController = navController,
                    hasToken = false,
                    authViewModel = viewModel
                )
            }
        }

        // storeTokenFromCallback sets loginState = Success synchronously
        composeTestRule.runOnUiThread { viewModel.storeTokenFromCallback("fake-token") }
        composeTestRule.waitForIdle()

        assert(navController.currentDestination?.route == NavDestination.Sleep.route) {
            "Expected navigation to sleep after login success — spec: TA-L-02"
        }
    }

    // ---------------------------------------------------------------------------
    // spec: P5 §6.1 — NavGraph doit importer ui.screens.auth.LoginScreen (et non le stub
    // ui.screens.login.LoginScreen) — vérification via le titre "Connexion" visible.
    // ---------------------------------------------------------------------------
    @Test
    fun navGraph_login_screen_shows_real_loginscreen() {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())

        composeTestRule.setContent {
            NightfallTheme {
                // spec: §6.1 — NavGraph câble ui.screens.auth.LoginScreen (vrai LoginScreen)
                NavGraph(
                    navController = navController,
                    hasToken = false
                )
            }
        }

        // spec: §4.1 — le vrai LoginScreen affiche un Text "Connexion" (headlineLarge)
        // Le stub legacy affiche "Login — p4-android-auth" — ce noeud ne doit plus exister
        composeTestRule
            .onNodeWithText("Connexion")
            .assertExists("NavGraph must render the real LoginScreen with title 'Connexion' — spec: §6.1 / §4.1")

        // spec: §8 — field_email doit être visible (testTag présent dans le vrai LoginScreen uniquement)
        composeTestRule
            .onNode(androidx.compose.ui.test.hasTestTag("field_email"))
            .assertExists("field_email testTag must be present — only exists in ui.screens.auth.LoginScreen — spec: §8")
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NavGraphTest` (class) — lines 28-246
- `navGraph_noToken_navigatesToLogin` (function) — lines 35-53
- `navGraph_withToken_navigatesToSleep` (function) — lines 56-73
- `navGraph_bottomNav_switchesToTimeline` (function) — lines 76-95
- `navGraph_bottomNav_switchesToActivity` (function) — lines 98-117
- `navGraph_bottomNav_switchesToProfile` (function) — lines 120-138
- `navGraph_profileScreen_importButtonNavigatesToImport` (function) — lines 141-163
- `navGraph_profileScreen_logoutClearsTokenAndNavigatesToLogin` (function) — lines 166-188
- `navGraph_login_success_navigates_to_sleep` (function) — lines 191-215
- `navGraph_login_screen_shows_real_loginscreen` (function) — lines 221-245
