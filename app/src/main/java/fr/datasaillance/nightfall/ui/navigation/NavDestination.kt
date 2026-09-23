package fr.datasaillance.nightfall.ui.navigation

sealed class NavDestination(
    val route: String,
    val label: String
) {
    object Sleep    : NavDestination("sleep",    "Sommeil")
    object Timeline : NavDestination("timeline", "Timeline")
    // 'activity' route conservée pour rétro-compat tests, mais le label affiché
    // est désormais 'Cadran' — radial clock = évolution moderne de l'écran Activité.
    object Activity : NavDestination("activity", "Cadran")
    object Wellbeing : NavDestination("wellbeing", "Bien-être")
    object Profile  : NavDestination("profile",  "Profil")
    object Import   : NavDestination("import",   "Importer")
    object Settings : NavDestination("settings", "Paramètres")
    object LabeledPlaces : NavDestination("labeled_places", "Lieux connus")
    object HealthConnect : NavDestination("health_connect", "Health Connect")
    object Hypnogram : NavDestination("hypnogram/{sessionId}?date={date}", "Hypnogramme") {
        // date facultative (ISO yyyy-MM-dd) — quand fournie, on fetch uniquement cette nuit
        // au lieu de télécharger tout l'historique sleep_sessions du user.
        fun route(id: String, date: String? = null): String =
            if (date != null) "hypnogram/$id?date=$date" else "hypnogram/$id"
    }

    companion object {
        fun bottomNavItems(): List<NavDestination> = listOf(Sleep, Timeline, Wellbeing, Activity, Profile)
    }
}
