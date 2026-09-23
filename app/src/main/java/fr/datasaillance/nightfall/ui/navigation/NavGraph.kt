package fr.datasaillance.nightfall.ui.navigation

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
import fr.datasaillance.nightfall.data.sleep.healthConnectSleepRepository
import fr.datasaillance.nightfall.data.sleep.SleepRepository
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import fr.datasaillance.nightfall.ui.screens.activity.ActivityScreen
import fr.datasaillance.nightfall.ui.screens.radial.RadialRoute
import fr.datasaillance.nightfall.ui.screens.wellbeing.DigitalWellbeingScreen
import fr.datasaillance.nightfall.viewmodel.wellbeing.DigitalWellbeingViewModel
import fr.datasaillance.nightfall.data.local.usage.UsageStatsPermissionHelper
import fr.datasaillance.nightfall.ui.screens.import_.ImportScreen
import fr.datasaillance.nightfall.ui.screens.profile.ProfileScreen
import fr.datasaillance.nightfall.ui.screens.settings.SettingsScreen
import fr.datasaillance.nightfall.ui.screens.sleep.HypnogramScreen
import fr.datasaillance.nightfall.ui.screens.sleep.SleepScreen
import fr.datasaillance.nightfall.ui.screens.sleep.TimelineScreen
import fr.datasaillance.nightfall.viewmodel.import_.ImportViewModel
import fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel
import fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel
import fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel
import androidx.health.connect.client.HealthConnectClient
import fr.datasaillance.nightfall.data.healthconnect.HealthConnectReader
import fr.datasaillance.nightfall.data.healthconnect.currentHealthConnectState
import fr.datasaillance.nightfall.data.healthconnect.sharedHealthDataCache
import fr.datasaillance.nightfall.ui.screens.healthconnect.HealthConnectScreen
import fr.datasaillance.nightfall.viewmodel.healthconnect.HealthConnectViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
) {
    val startDestination = NavDestination.Sleep.route
    val context = LocalContext.current

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
            composable(NavDestination.Sleep.route) {
                val sleepRepository: SleepRepository = remember(context) { healthConnectSleepRepository(context) }
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
                val hypnogramDb = remember(context) {
                    fr.datasaillance.nightfall.data.local.database.NightfallDatabase.get(context.applicationContext)
                }
                val hypnogramRepository: SleepRepository = remember(context) { healthConnectSleepRepository(context) }
                val hypnogramViewModel = remember(sessionId, dateArg, hypnogramRepository, hypnogramDb) {
                    HypnogramViewModel(
                        sessionId = sessionId,
                        repository = hypnogramRepository,
                        hintDate = dateArg,
                        locationDao = hypnogramDb.locationDao(),
                        usageStatsDao = hypnogramDb.usageStatsDao(),
                    )
                }
                HypnogramScreen(
                    viewModel = hypnogramViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(NavDestination.Timeline.route) {
                val db = remember(context) {
                    fr.datasaillance.nightfall.data.local.database.NightfallDatabase.get(context.applicationContext)
                }
                val timelineRepository: SleepRepository = remember(context) { healthConnectSleepRepository(context) }
                val timelineViewModel = remember(timelineRepository, db) {
                    TimelineViewModel(timelineRepository, db.locationDao())
                }
                TimelineScreen(
                    viewModel = timelineViewModel,
                    onOpenHypnogram = { sessionId, isoDate ->
                        navController.navigate(NavDestination.Hypnogram.route(sessionId, isoDate))
                    },
                )
            }
            // Route 'activity' rendered as the new MultiDonutClock radial view (phase 4a).
            composable(NavDestination.Activity.route) { RadialRoute() }
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
                    onHealthConnect = { navController.navigate(NavDestination.HealthConnect.route) },
                )
            }
            composable(NavDestination.Import.route) {
                val context = LocalContext.current
                val db = remember(context) {
                    fr.datasaillance.nightfall.data.local.database.NightfallDatabase.get(context.applicationContext)
                }
                val locationService = remember(db) {
                    fr.datasaillance.nightfall.data.local.location.LocalLocationImportService(db.locationDao())
                }
                val viewModel = remember(locationService) {
                    ImportViewModel(locationService)
                }
                ImportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(NavDestination.Settings.route) {
                SettingsScreen(
                    onOpenLabeledPlaces = { navController.navigate(NavDestination.LabeledPlaces.route) },
                )
            }
            composable(NavDestination.HealthConnect.route) {
                val appContext = context.applicationContext
                val viewModel = remember(appContext) {
                    HealthConnectViewModel(
                        checkState = { currentHealthConnectState(appContext) },
                        newSource = { HealthConnectReader(HealthConnectClient.getOrCreate(appContext)) },
                        cache = sharedHealthDataCache,
                    )
                }
                HealthConnectScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(NavDestination.LabeledPlaces.route) {
                fr.datasaillance.nightfall.ui.screens.places.LabeledPlacesRoute(
                    onBack = { navController.popBackStack() },
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
