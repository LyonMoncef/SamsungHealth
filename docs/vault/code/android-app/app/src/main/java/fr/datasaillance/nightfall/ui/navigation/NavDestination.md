---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavDestination.kt
git_blob: f3315e4424fd681e2b7bbb9f7efe5404d6416213
last_synced: '2026-05-09T14:31:04Z'
loc: 26
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
    object Activity : NavDestination("activity", "Activité")
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
        fun bottomNavItems(): List<NavDestination> = listOf(Sleep, Timeline, Activity, Profile)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NavDestination` (class) — lines 3-26
- `route` (function) — lines 19-20
- `bottomNavItems` (function) — lines 24-24
