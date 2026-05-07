package fr.datasaillance.nightfall.ui.navigation

sealed class NavDestination(
    val route: String,
    val label: String
) {
    object Login          : NavDestination("login",           "Connexion")
    object Sleep          : NavDestination("sleep",           "Sommeil")
    object Trends         : NavDestination("trends",          "Tendances")
    object Activity       : NavDestination("activity",        "Activité")
    object Profile        : NavDestination("profile",         "Profil")
    object Import         : NavDestination("import",          "Importer")
    object Settings       : NavDestination("settings",        "Paramètres")
    object Register       : NavDestination("register",        "Créer un compte")
    object ForgotPassword : NavDestination("forgot_password", "Mot de passe oublié")

    companion object {
        fun bottomNavItems(): List<NavDestination> = listOf(Sleep, Trends, Activity, Profile)
    }
}
