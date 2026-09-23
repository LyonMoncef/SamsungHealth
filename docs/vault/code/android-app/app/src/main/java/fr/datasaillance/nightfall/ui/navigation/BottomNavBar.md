---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBar.kt
git_blob: 202b57d183e71133738602723466190745b2fc89
last_synced: '2026-05-26T03:20:22Z'
loc: 40
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
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private fun iconForDestination(destination: NavDestination): ImageVector = when (destination) {
    is NavDestination.Sleep     -> Icons.Default.Home
    is NavDestination.Timeline  -> Icons.Default.ShowChart
    is NavDestination.Wellbeing -> Icons.Default.PhoneAndroid
    is NavDestination.Activity  -> Icons.Default.DonutLarge  // route 'activity' = Cadran radial
    is NavDestination.Profile   -> Icons.Default.AccountCircle
    else                        -> Icons.Default.Home
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
- `iconForDestination` (function) — lines 16-23
- `BottomNavBar` (function) — lines 25-40
