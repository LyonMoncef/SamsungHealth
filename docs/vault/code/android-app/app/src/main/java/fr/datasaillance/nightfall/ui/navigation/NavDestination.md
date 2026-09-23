---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavDestination.kt
git_blob: 26e31799980412209ba3248f2530d698e5492a93
last_synced: '2026-05-29T08:09:08Z'
loc: 30
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavDestination.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavDestination.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavDestination.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
    // 'activity' route conservée pour rétro-compat tests, mais le label affiché
    // est désormais 'Cadran' — radial clock = évolution moderne de l'écran Activité.
    object Activity : NavDestination("activity", "Cadran")
    object Wellbeing : NavDestination("wellbeing", "Bien-être")
    object Profile  : NavDestination("profile",  "Profil")
    object Import   : NavDestination("import",   "Importer")
    object Settings : NavDestination("settings", "Paramètres")
    object LabeledPlaces : NavDestination("labeled_places", "Lieux connus")
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NavDestination` (class) — lines 3-30
- `route` (function) — lines 23-24
- `bottomNavItems` (function) — lines 28-28
