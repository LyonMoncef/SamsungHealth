---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/NavDestinationP5Test.kt
git_blob: 37b8c7a5d1554771c9c8315557a0d7fda6473299
last_synced: '2026-05-09T04:03:35Z'
loc: 60
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/NavDestinationP5Test.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/NavDestinationP5Test.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/NavDestinationP5Test.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.navigation

// spec: P5 §10 / §6.1 — NavDestination.Register et NavDestination.ForgotPassword manquants
// RED par exécution : ces tests compilent via reflection JVM mais échouent à l'exécution
// TANT QUE NavDestination.kt n'expose pas ces deux objets dans le sealed class.
// Une fois ajoutés, ces tests passeront GREEN et valideront les routes.

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavDestinationP5Test {

    // spec: §10 — NavDestination.Register requis par NavGraph.kt câblé (onNavigateRegister)
    // RED : NavDestination.kt ne contient pas Register dans le sealed class.
    // Lookup via reflection : échoue à l'exécution avec NoSuchFieldException tant que l'objet est absent.
    @Test
    fun navDestination_register_route_exists() {
        // spec: §10 / §6.1 — NavDestination.Register doit être un objet du sealed class
        // avec un champ `route` non vide (convention: "register")
        val nestedClass = try {
            Class.forName("fr.datasaillance.nightfall.ui.navigation.NavDestination\$Register")
        } catch (e: ClassNotFoundException) {
            throw AssertionError(
                "NavDestination.Register object not found — must be added to NavDestination sealed class — spec: §10. " +
                "ClassNotFoundException: ${e.message}"
            )
        }
        val instance = nestedClass.getField("INSTANCE").get(null)
        val routeField = nestedClass.getMethod("getRoute")
        val route = routeField.invoke(instance) as String
        assert(route.isNotBlank()) {
            "NavDestination.Register.route must be non-blank — spec: §10"
        }
    }

    // spec: §10 — NavDestination.ForgotPassword requis par NavGraph.kt câblé (onNavigateForgotPassword)
    // RED : NavDestination.kt ne contient pas ForgotPassword dans le sealed class.
    // Lookup via reflection : échoue à l'exécution avec AssertionError tant que l'objet est absent.
    @Test
    fun navDestination_forgotPassword_route_exists() {
        // spec: §10 / §6.1 — NavDestination.ForgotPassword doit être un objet du sealed class
        // avec un champ `route` non vide (convention: "forgot-password")
        val nestedClass = try {
            Class.forName("fr.datasaillance.nightfall.ui.navigation.NavDestination\$ForgotPassword")
        } catch (e: ClassNotFoundException) {
            throw AssertionError(
                "NavDestination.ForgotPassword object not found — must be added to NavDestination sealed class — spec: §10. " +
                "ClassNotFoundException: ${e.message}"
            )
        }
        val instance = nestedClass.getField("INSTANCE").get(null)
        val routeField = nestedClass.getMethod("getRoute")
        val route = routeField.invoke(instance) as String
        assert(route.isNotBlank()) {
            "NavDestination.ForgotPassword.route must be non-blank — spec: §10"
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NavDestinationP5Test` (class) — lines 12-60
- `navDestination_register_route_exists` (function) — lines 18-36
- `navDestination_forgotPassword_route_exists` (function) — lines 41-59
