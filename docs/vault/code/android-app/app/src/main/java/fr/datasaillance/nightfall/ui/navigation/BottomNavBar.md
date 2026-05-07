---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBar.kt
git_blob: d413b4dbf77afee2e48e34efe7c3252b585d1137
last_synced: '2026-05-07T00:48:24Z'
loc: 38
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBar.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBar.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBar.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private fun iconForDestination(destination: NavDestination): ImageVector = when (destination) {
    is NavDestination.Sleep    -> Icons.Default.Home
    is NavDestination.Trends   -> Icons.Default.ShowChart
    is NavDestination.Activity -> Icons.Default.FitnessCenter
    is NavDestination.Profile  -> Icons.Default.AccountCircle
    else                       -> Icons.Default.Home
}

@Composable
fun BottomNavBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavDestination.bottomNavItems().forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick  = { onNavigate(destination.route) },
                icon     = { Icon(iconForDestination(destination), contentDescription = destination.label) },
                label    = { Text(destination.label) }
            )
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `iconForDestination` (function) — lines 15-21
- `BottomNavBar` (function) — lines 23-38
