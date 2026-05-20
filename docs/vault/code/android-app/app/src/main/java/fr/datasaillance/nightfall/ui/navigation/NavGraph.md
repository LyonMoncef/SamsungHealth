---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt
git_blob: ce4df44a5eb4f975ac69cab71ca2ca1d92e42c69
last_synced: '2026-05-20T18:53:28Z'
loc: 293
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
import androidx.compose.ui.platform.LocalContext
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
import fr.datasaillance.nightfall.data.sleep.LocalSleepRepository
import fr.datasaillance.nightfall.data.sleep.SleepRepository
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import fr.datasaillance.nightfall.domain.import_.ImportDataType
import fr.datasaillance.nightfall.domain.import_.ImportResult
import fr.datasaillance.nightfall.ui.screens.activity.ActivityScreen
import fr.datasaillance.nightfall.ui.screens.wellbeing.DigitalWellbeingScreen
import fr.datasaillance.nightfall.viewmodel.wellbeing.DigitalWellbeingViewModel
import fr.datasaillance.nightfall.data.local.usage.UsageStatsPermissionHelper
import fr.datasaillance.nightfall.ui.screens.auth.ForgotPasswordScreen
import fr.datasaillance.nightfall.ui.screens.auth.LoginScreen
import fr.datasaillance.nightfall.ui.screens.auth.RegisterScreen
import fr.datasaillance.nightfall.ui.screens.import_.ImportScreen
import fr.datasaillance.nightfall.ui.screens.profile.ProfileScreen
import fr.datasaillance.nightfall.ui.screens.settings.SettingsScreen
import fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen
import fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen
import fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
import fr.datasaillance.nightfall.viewmodel.import_.ImportViewModel
import fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel
import fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel
import fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel
import retrofit2.Response

@Composable
fun NavGraph(
    navController: NavHostController,
    hasToken: Boolean,
    backendUrl: String = "",
    onSaveUrl: (String) -> Unit = {},
    api: NightfallApi? = null,
    tokenDataStore: TokenDataStore? = null,
    authViewModel: AuthViewModel? = null,
) {
    val startDestination = if (hasToken) NavDestination.Sleep.route else NavDestination.Login.route
    val context = LocalContext.current
    val authViewModel = authViewModel ?: remember(api, tokenDataStore) {
        val resolvedApi = api ?: NoOpNightfallApi()
        val resolvedStore = tokenDataStore ?: TokenDataStore(context)
        AuthViewModel(resolvedApi, resolvedStore)
    }

    // Adds ComposeNavigator/DialogNavigator to the navigator provider when absent.
    // TestNavHostController only registers TestNavigator by default; without this,
    // NavHost + composable{} throws ClassCastException under Robolectric.
    ensureComposeNavigators(navController)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in setOf("sleep", "timeline", "activity", "profile")

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
            composable(NavDestination.Register.route) {
                RegisterScreen(
                    viewModel        = authViewModel,
                    onRegisterSuccess = {
                        navController.navigate(NavDestination.Login.route) {
                            popUpTo(NavDestination.Register.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(NavDestination.ForgotPassword.route) {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onBack    = { navController.popBackStack() },
                )
            }
            composable(NavDestination.Sleep.route) {
                val sleepRepository: SleepRepository = remember(context) {
                    val db = fr.datasaillance.nightfall.data.local.database.NightfallDatabase.get(context.applicationContext)
                    LocalSleepRepository(db.sleepDao())
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
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("date") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                )
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                val dateArg = backStackEntry.arguments?.getString("date")
                val hypnogramRepository: SleepRepository = remember(context) {
                    val db = fr.datasaillance.nightfall.data.local.database.NightfallDatabase.get(context.applicationContext)
                    LocalSleepRepository(db.sleepDao())
                }
                val hypnogramViewModel = remember(sessionId, dateArg, hypnogramRepository) {
                    HypnogramViewModel(sessionId, hypnogramRepository, hintDate = dateArg)
                }
                HypnogramScreen(
                    viewModel = hypnogramViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavDestination.Timeline.route) {
                val timelineRepository: SleepRepository = remember(context) {
                    val db = fr.datasaillance.nightfall.data.local.database.NightfallDatabase.get(context.applicationContext)
                    LocalSleepRepository(db.sleepDao())
                }
                val timelineViewModel = remember(timelineRepository) { TimelineViewModel(timelineRepository) }
                TimelineScreen(
                    viewModel = timelineViewModel,
                    onOpenHypnogram = { sessionId, isoDate ->
                        navController.navigate(NavDestination.Hypnogram.route(sessionId, isoDate))
                    },
                )
            }
            composable(NavDestination.Activity.route) { ActivityScreen() }
            composable(NavDestination.Wellbeing.route) {
                val db = remember(context) {
                    fr.datasaillance.nightfall.data.local.database.NightfallDatabase.get(context.applicationContext)
                }
                val viewModel = remember(context, db) {
                    val helper = UsageStatsPermissionHelper(context.applicationContext)
                    DigitalWellbeingViewModel(
                        checkPermission = { helper.hasPermission() },
                        dao = db.usageStatsDao(),
                        packageResolver = fr.datasaillance.nightfall.data.local.usage.PackageInfoResolver(
                            context.applicationContext.packageManager
                        ),
                    )
                }
                DigitalWellbeingScreen(viewModel = viewModel)
            }
            composable(NavDestination.Profile.route) {
                ProfileScreen(
                    onImport   = { navController.navigate(NavDestination.Import.route) },
                    onSettings = { navController.navigate(NavDestination.Settings.route) },
                    onLogout   = {
                        authViewModel.logout()
                        navController.navigate(NavDestination.Login.route) {
                            popUpTo(NavDestination.Sleep.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavDestination.Import.route) {
                val context = LocalContext.current
                val repository: ImportRepository = remember(api, context) {
                    if (api != null) {
                        val db = fr.datasaillance.nightfall.data.local.database.NightfallDatabase.get(context.applicationContext)
                        val localService = fr.datasaillance.nightfall.data.local.import_.LocalImportService(
                            sleepDao = db.sleepDao(),
                            heartRateDao = db.heartRateDao(),
                            stepsDao = db.stepsDao(),
                            exerciseDao = db.exerciseDao(),
                        )
                        ImportRepositoryImpl(api, localService)
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
    override suspend fun getSessions(
        from: java.time.LocalDate?,
        to: java.time.LocalDate?,
    ): Result<List<SleepSessionResponse>> = Result.success(emptyList())
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
- `NavGraph` (function) — lines 58-245
- `NoOpSleepRepository` (class) — lines 247-252
- `getSessions` (function) — lines 248-251
- `NoOpImportRepository` (class) — lines 254-269
- `pingBackend` (function) — lines 255-255
- `extractCsvEntries` (function) — lines 257-260
- `uploadCsv` (function) — lines 262-268
- `NoOpNightfallApi` (class) — lines 271-277
- `health` (function) — lines 272-272
- `login` (function) — lines 273-273
- `register` (function) — lines 274-274
- `requestPasswordReset` (function) — lines 275-275
- `googleStart` (function) — lines 276-276
- `ensureComposeNavigators` (function) — lines 285-293
