---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBarTest.kt
git_blob: 9bc8fc91b8cad478ce6406ada9e6de0c1fdc6a02
last_synced: '2026-05-07T00:48:24Z'
loc: 98
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBarTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBarTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/ui/navigation/BottomNavBarTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.navigation

// spec: Tests d'acceptation TA-02, TA-03, TA-04, TA-05
// spec: section "BottomNavBar" — NavigationBar M3 avec 4 tabs
// RED by construction: fr.datasaillance.nightfall.ui.navigation.BottomNavBar does not exist yet

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.ui.navigation.BottomNavBar
// fr.datasaillance.nightfall.ui.navigation.NavDestination
// fr.datasaillance.nightfall.ui.theme.NightfallTheme

class BottomNavBarTest {

    // spec: D10 — Paparazzi pour screenshot tests des composables (offline, pas d'émulateur)
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    // spec: TA-13 — dark mode snapshot de BottomNavBar avec tokens DataSaillance
    // spec: TA-04 — NavigationBar containerColor = Surface (#232E32) en dark mode
    @Test
    fun bottomNavBar_snapshot_dark() {
        paparazzi.snapshot {
            NightfallTheme(darkTheme = true) {
                BottomNavBar(
                    selectedRoute = NavDestination.Sleep.route,
                    onNavigate = {}
                )
            }
        }
        // Paparazzi compares against golden — RED until BottomNavBar and NightfallTheme exist
        // and golden recorded with `./gradlew recordPaparazziDebug`
    }

    // spec: TA-05 — light mode snapshot ; fond SurfaceLight (#FFFFFF), textes onSurface (#1A1916)
    @Test
    fun bottomNavBar_snapshot_light() {
        paparazzi.snapshot {
            NightfallTheme(darkTheme = false) {
                BottomNavBar(
                    selectedRoute = NavDestination.Sleep.route,
                    onNavigate = {}
                )
            }
        }
    }

    // spec: TA-02 — tab "Sommeil" est sélectionné par défaut quand l'app démarre avec token
    @Test
    fun bottomNavBar_sleepTabSelected() {
        paparazzi.snapshot {
            NightfallTheme(darkTheme = false) {
                BottomNavBar(
                    selectedRoute = NavDestination.Sleep.route,
                    onNavigate = {}
                )
            }
        }
        // The golden snapshot must show "Sommeil" tab as selected (highlighted)
        // RED until BottomNavBar exposes selectedRoute parameter + NightfallTheme exists
    }

    // spec: TA-03 — tab "Tendances" en état selected quand selectedRoute == trends
    @Test
    fun bottomNavBar_trendsTabSelected_snapshot() {
        paparazzi.snapshot {
            NightfallTheme(darkTheme = false) {
                BottomNavBar(
                    selectedRoute = NavDestination.Trends.route,
                    onNavigate = {}
                )
            }
        }
    }

    // spec: navigation graph section — 4 tabs présents (Sommeil, Tendances, Activité, Profil)
    // Validates BottomNavBar renders exactly 4 NavigationBarItems
    @Test
    fun bottomNavBar_fourTabsPresent_dark() {
        paparazzi.snapshot {
            NightfallTheme(darkTheme = true) {
                BottomNavBar(
                    selectedRoute = NavDestination.Activity.route,
                    onNavigate = {}
                )
            }
        }
        // Contract: 4 tabs — Sommeil, Tendances, Activité, Profil
        // spec: navigation graph section + BottomNavBar.kt livrable
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `BottomNavBarTest` (class) — lines 17-98
- `bottomNavBar_snapshot_dark` (function) — lines 28-40
- `bottomNavBar_snapshot_light` (function) — lines 43-53
- `bottomNavBar_sleepTabSelected` (function) — lines 56-68
- `bottomNavBar_trendsTabSelected_snapshot` (function) — lines 71-81
- `bottomNavBar_fourTabsPresent_dark` (function) — lines 85-97
