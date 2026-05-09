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
    object Profile  : NavDestination("profile",  "Profil")
    object Import   : NavDestination("import",   "Importer")
    object Settings : NavDestination("settings", "Paramètres")
    object Hypnogram : NavDestination("hypnogram/{sessionId}", "Hypnogramme") {
        fun route(id: String): String = "hypnogram/$id"
    }

    companion object {
        fun bottomNavItems(): List<NavDestination> = listOf(Sleep, Timeline, Activity, Profile)
    }
}
