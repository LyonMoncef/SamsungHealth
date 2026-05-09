---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt
git_blob: 78fa52b8e2383ef529db45eb2666d61a591c735d
last_synced: '2026-05-09T06:18:31Z'
loc: 257
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.navigation

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.GoogleStartRequest
import fr.datasaillance.nightfall.data.http.GoogleStartResponse
import fr.datasaillance.nightfall.data.http.ImportApiResponse
import fr.datasaillance.nightfall.data.http.LoginRequest
import fr.datasaillance.nightfall.data.http.LoginResponse
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.data.http.PasswordResetRequest
import fr.datasaillance.nightfall.data.http.RegisterRequest
import fr.datasaillance.nightfall.data.http.RegisterResponse
import fr.datasaillance.nightfall.data.http.StatusResponse
import fr.datasaillance.nightfall.data.import_.CsvEntry
import fr.datasaillance.nightfall.data.import_.ImportRepository
import fr.datasaillance.nightfall.data.import_.ImportRepositoryImpl
import fr.datasaillance.nightfall.data.sleep.SleepRepository
import fr.datasaillance.nightfall.data.sleep.SleepRepositoryImpl
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import fr.datasaillance.nightfall.domain.import_.ImportDataType
import fr.datasaillance.nightfall.domain.import_.ImportResult
import fr.datasaillance.nightfall.ui.screens.activity.ActivityScreen
import fr.datasaillance.nightfall.ui.screens.auth.ForgotPasswordScreen
import fr.datasaillance.nightfall.ui.screens.auth.LoginScreen
import fr.datasaillance.nightfall.ui.screens.auth.RegisterScreen
import fr.datasaillance.nightfall.ui.screens.import_.ImportScreen
import fr.datasaillance.nightfall.ui.screens.profile.ProfileScreen
import fr.datasaillance.nightfall.ui.screens.settings.SettingsScreen
import fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen
import fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen
import fr.datasaillance.nightfall.ui.screens.trends.TrendsScreen
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
import fr.datasaillance.nightfall.viewmodel.import_.ImportViewModel
import fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel
import fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel
import okhttp3.MultipartBody
import retrofit2.Response

@Composable
fun NavGraph(
    navController: NavHostController,
    hasToken: Boolean,
    backendUrl: String = "",
    onSaveUrl: (String) -> Unit = {},
    api: NightfallApi? = null,
    tokenDataStore: TokenDataStore? = null,
) {
    val startDestination = if (hasToken) NavDestination.Sleep.route else NavDestination.Login.route
    val authViewModel = remember(api, tokenDataStore) {
        if (api != null && tokenDataStore != null) AuthViewModel(api, tokenDataStore) else null
    }

    // Adds ComposeNavigator/DialogNavigator to the navigator provider when absent.
    // TestNavHostController only registers TestNavigator by default; without this,
    // NavHost + composable{} throws ClassCastException under Robolectric.
    ensureComposeNavigators(navController)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in setOf("sleep", "trends", "activity", "profile")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    selectedRoute = currentRoute ?: NavDestination.Sleep.route,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(NavDestination.Login.route) {
                if (authViewModel != null) {
                    LoginScreen(
                        viewModel            = authViewModel,
                        onLoginSuccess       = {
                            navController.navigate(NavDestination.Sleep.route) {
                                popUpTo(NavDestination.Login.route) { inclusive = true }
                            }
                        },
                        onNavigateRegister   = { navController.navigate(NavDestination.Register.route) },
                        onNavigateForgotPassword = { navController.navigate(NavDestination.ForgotPassword.route) },
                    )
                }
            }
            composable(NavDestination.Register.route) {
                if (authViewModel != null) {
                    RegisterScreen(
                        viewModel        = authViewModel,
                        onRegisterSuccess = {
                            navController.navigate(NavDestination.Login.route) {
                                popUpTo(NavDestination.Register.route) { inclusive = true }
                            }
                        },
                    )
                }
            }
            composable(NavDestination.ForgotPassword.route) {
                if (authViewModel != null) {
                    ForgotPasswordScreen(
                        viewModel = authViewModel,
                        onBack    = { navController.popBackStack() },
                    )
                }
            }
            composable(NavDestination.Sleep.route) {
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
            }
            composable(
                route = NavDestination.Hypnogram.route,
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
            composable(NavDestination.Trends.route)   { TrendsScreen() }
            composable(NavDestination.Activity.route) { ActivityScreen() }
            composable(NavDestination.Profile.route) {
                ProfileScreen(
                    onImport   = { navController.navigate(NavDestination.Import.route) },
                    onSettings = { navController.navigate(NavDestination.Settings.route) },
                    onLogout   = {
                        authViewModel?.logout()
                        navController.navigate(NavDestination.Login.route) {
                            popUpTo(NavDestination.Sleep.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavDestination.Import.route) {
                val repository: ImportRepository = remember(api) {
                    if (api != null) {
                        ImportRepositoryImpl(api)
                    } else {
                        NoOpImportRepository()
                    }
                }
                val viewModel = remember(repository) { ImportViewModel(repository) }
                ImportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(NavDestination.Settings.route) {
                SettingsScreen(
                    currentUrl = backendUrl,
                    onSaveUrl  = onSaveUrl
                )
            }
        }
    }
}

private class NoOpSleepRepository : SleepRepository {
    override suspend fun getSessions(): Result<List<SleepSessionResponse>> =
        Result.success(emptyList())
}

private class NoOpImportRepository : ImportRepository {
    override suspend fun pingBackend(): Boolean = false

    override suspend fun extractCsvEntries(
        contentResolver: ContentResolver,
        treeUri: Uri,
    ): Map<ImportDataType, CsvEntry> = emptyMap()

    override suspend fun uploadCsv(
        contentResolver: ContentResolver,
        uri: Uri,
        type: ImportDataType,
        totalBytes: Long,
        onProgress: (Float) -> Unit,
    ): ImportResult = throw UnsupportedOperationException("No-op repository")
}

private class NoOpNightfallApi : NightfallApi {
    override suspend fun health(): Response<Unit> = throw UnsupportedOperationException("No-op api")
    override suspend fun login(body: LoginRequest): LoginResponse = throw UnsupportedOperationException("No-op api")
    override suspend fun register(body: RegisterRequest, registrationToken: String?): RegisterResponse = throw UnsupportedOperationException("No-op api")
    override suspend fun requestPasswordReset(body: PasswordResetRequest): StatusResponse = throw UnsupportedOperationException("No-op api")
    override suspend fun googleStart(body: GoogleStartRequest): GoogleStartResponse = throw UnsupportedOperationException("No-op api")
    override suspend fun getSleepSessions(token: String, from: String?, to: String?, includeStages: Boolean): List<SleepSessionResponse> = emptyList()
    override suspend fun importSleep(file: MultipartBody.Part): ImportApiResponse = throw UnsupportedOperationException("No-op api")
    override suspend fun importHeartRate(file: MultipartBody.Part): ImportApiResponse = throw UnsupportedOperationException("No-op api")
    override suspend fun importSteps(file: MultipartBody.Part): ImportApiResponse = throw UnsupportedOperationException("No-op api")
    override suspend fun importExercise(file: MultipartBody.Part): ImportApiResponse = throw UnsupportedOperationException("No-op api")
}

/**
 * Adds [ComposeNavigator] and [DialogNavigator] to the [NavHostController]'s navigator provider
 * if they are not already registered. [androidx.navigation.testing.TestNavHostController] only
 * registers a [TestNavigator] by default; without [ComposeNavigator], NavHost + composable{}
 * throws ClassCastException under Robolectric.
 */
private fun ensureComposeNavigators(navController: NavHostController) {
    val provider = navController.navigatorProvider
    if (!provider.navigators.containsKey("composable")) {
        provider.addNavigator(ComposeNavigator())
    }
    if (!provider.navigators.containsKey("dialog")) {
        provider.addNavigator(DialogNavigator())
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NavGraph` (function) — lines 55-206
- `NoOpSleepRepository` (class) — lines 208-211
- `getSessions` (function) — lines 209-210
- `NoOpImportRepository` (class) — lines 213-228
- `pingBackend` (function) — lines 214-214
- `extractCsvEntries` (function) — lines 216-219
- `uploadCsv` (function) — lines 221-227
- `NoOpNightfallApi` (class) — lines 230-241
- `health` (function) — lines 231-231
- `login` (function) — lines 232-232
- `register` (function) — lines 233-233
- `requestPasswordReset` (function) — lines 234-234
- `googleStart` (function) — lines 235-235
- `getSleepSessions` (function) — lines 236-236
- `importSleep` (function) — lines 237-237
- `importHeartRate` (function) — lines 238-238
- `importSteps` (function) — lines 239-239
- `importExercise` (function) — lines 240-240
- `ensureComposeNavigators` (function) — lines 249-257
