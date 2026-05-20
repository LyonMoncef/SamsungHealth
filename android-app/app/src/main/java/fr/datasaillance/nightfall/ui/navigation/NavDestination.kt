package fr.datasaillance.nightfall.ui.navigation

sealed class NavDestination(
    val route: String,
    val label: String
) {
    object Login          : NavDestination("login",           "Connexion")
    object Register       : NavDestination("register",        "Créer un compte")
    object ForgotPassword : NavDestination("forgot_password", "Mot de passe oublié")
    object Sleep    : NavDestination("sleep",    "Sommeil")
    object Timeline : NavDestination("timeline", "Timeline")
    object Activity : NavDestination("activity", "Activité")
    object Wellbeing : NavDestination("wellbeing", "Bien-être")
    object Profile  : NavDestination("profile",  "Profil")
    object Import   : NavDestination("import",   "Importer")
    object Settings : NavDestination("settings", "Paramètres")
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
