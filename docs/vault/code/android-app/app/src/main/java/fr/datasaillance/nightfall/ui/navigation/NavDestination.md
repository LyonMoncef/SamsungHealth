---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NavDestination.kt
git_blob: 9b7fa89bddc2668e3b5c6b98aefce2dd7b2976ac
last_synced: '2026-05-09T02:10:36Z'
loc: 20
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
    object Trends   : NavDestination("trends",   "Tendances")
    object Activity : NavDestination("activity", "Activité")
    object Profile  : NavDestination("profile",  "Profil")
    object Import   : NavDestination("import",   "Importer")
    object Settings : NavDestination("settings", "Paramètres")

    companion object {
        fun bottomNavItems(): List<NavDestination> = listOf(Sleep, Trends, Activity, Profile)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NavDestination` (class) — lines 3-20
- `bottomNavItems` (function) — lines 18-18
