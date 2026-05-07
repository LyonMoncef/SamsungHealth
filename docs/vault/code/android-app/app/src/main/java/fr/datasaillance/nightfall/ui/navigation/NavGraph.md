---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavGraph.kt
git_blob: 46d256bd1d831cb160659cbd1f639a084851ca56
last_synced: '2026-05-07T22:01:38Z'
loc: 210
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
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.di.AppModule
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
import fr.datasaillance.nightfall.domain.import_.ImportDataType
import fr.datasaillance.nightfall.domain.import_.ImportResult
import fr.datasaillance.nightfall.ui.screens.activity.ActivityScreen
import fr.datasaillance.nightfall.ui.screens.auth.ForgotPasswordScreen
import fr.datasaillance.nightfall.ui.screens.auth.LoginScreen
import fr.datasaillance.nightfall.ui.screens.auth.RegisterScreen
import fr.datasaillance.nightfall.ui.screens.import_.ImportScreen
import fr.datasaillance.nightfall.ui.screens.profile.ProfileScreen
import fr.datasaillance.nightfall.ui.screens.settings.SettingsScreen
import fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen
import fr.datasaillance.nightfall.ui.screens.trends.TrendsScreen
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
import fr.datasaillance.nightfall.viewmodel.import_.ImportViewModel
import okhttp3.MultipartBody
import retrofit2.Response

@Composable
fun NavGraph(
    navController: NavHostController,
    hasToken: Boolean,
    backendUrl: String = "",
    onSaveUrl: (String) -> Unit = {},
    api: NightfallApi? = null,
    authViewModel: AuthViewModel? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val startDestination = if (hasToken) NavDestination.Sleep.route else NavDestination.Login.route
    val resolvedAuthViewModel = authViewModel ?: remember(api, context) {
        AppModule.provideAuthViewModel(
            api = api ?: NoOpNightfallApi(),
            tokenDataStore = AppModule.provideTokenDataStore(context)
        )
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
                LoginScreen(
                    viewModel = resolvedAuthViewModel,
                    onLoginSuccess = {
                        navController.navigate(NavDestination.Sleep.route) {
                            popUpTo(NavDestination.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateRegister = {
                        navController.navigate(NavDestination.Register.route)
                    },
                    onNavigateForgotPassword = {
                        navController.navigate(NavDestination.ForgotPassword.route)
                    }
                )
            }
            composable(NavDestination.Register.route) {
                RegisterScreen(
                    viewModel = resolvedAuthViewModel,
                    onRegisterSuccess = {
                        navController.navigate(NavDestination.Sleep.route) {
                            popUpTo(NavDestination.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavDestination.ForgotPassword.route) {
                ForgotPasswordScreen(
                    viewModel = resolvedAuthViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavDestination.Sleep.route)    { SleepScreen() }
            composable(NavDestination.Trends.route)   { TrendsScreen() }
            composable(NavDestination.Activity.route) { ActivityScreen() }
            composable(NavDestination.Profile.route) {
                ProfileScreen(
                    onImport   = { navController.navigate(NavDestination.Import.route) },
                    onSettings = { navController.navigate(NavDestination.Settings.route) },
                    onLogout   = {
                        navController.navigate(NavDestination.Login.route) {
                            popUpTo(NavDestination.Sleep.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(NavDestination.Import.route) {
                val repository: ImportRepository = remember {
                    if (api != null) {
                        ImportRepositoryImpl(api)
                    } else {
                        NoOpImportRepository()
                    }
                }
                val viewModel = remember { ImportViewModel(repository) }
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
- `NavGraph` (function) — lines 48-165
- `NoOpImportRepository` (class) — lines 167-182
- `pingBackend` (function) — lines 168-168
- `extractCsvEntries` (function) — lines 170-173
- `uploadCsv` (function) — lines 175-181
- `NoOpNightfallApi` (class) — lines 184-194
- `health` (function) — lines 185-185
- `login` (function) — lines 186-186
- `register` (function) — lines 187-187
- `requestPasswordReset` (function) — lines 188-188
- `googleStart` (function) — lines 189-189
- `importSleep` (function) — lines 190-190
- `importHeartRate` (function) — lines 191-191
- `importSteps` (function) — lines 192-192
- `importExercise` (function) — lines 193-193
- `ensureComposeNavigators` (function) — lines 202-210
