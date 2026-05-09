---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavDestination.kt
git_blob: 8922e44fc45634f554cd2d4b07faa3593e990cc7
last_synced: '2026-05-09T07:04:02Z'
loc: 23
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
    object Hypnogram : NavDestination("hypnogram/{sessionId}", "Hypnogramme") {
        fun route(id: String): String = "hypnogram/$id"
    }

    companion object {
        fun bottomNavItems(): List<NavDestination> = listOf(Sleep, Timeline, Activity, Profile)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NavDestination` (class) — lines 3-23
- `route` (function) — lines 17-17
- `bottomNavItems` (function) — lines 21-21
