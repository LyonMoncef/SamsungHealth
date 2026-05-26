---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/radial/RadialRoute.kt
git_blob: 6d33d38e430b80556279c5d9b91b37fdb14f529c
last_synced: '2026-05-26T03:20:22Z'
loc: 57
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/radial/RadialRoute.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/radial/RadialRoute.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/radial/RadialRoute.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.radial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.dataviz.radial.RadialClockScreen
import fr.datasaillance.nightfall.viewmodel.radial.RadialClockViewModel

/**
 * Route Compose qui assemble `RadialClockViewModel` + `RadialClockScreen`.
 * Affiche un spinner pendant le 1er load, puis le multi-donut radial avec
 * navigation entre les jours de la fenêtre.
 */
@Composable
fun RadialRoute() {
    val context = LocalContext.current
    val db = remember(context) { NightfallDatabase.get(context.applicationContext) }
    val viewModel = remember(db) {
        RadialClockViewModel(
            sleepDao = db.sleepDao(),
            locationDao = db.locationDao(),
            usageStatsDao = db.usageStatsDao(),
            windowDays = 30,
        )
    }
    val state by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (state.isLoading || state.days.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            RadialClockScreen(
                days = state.days,
                initialDate = state.initialDate,
                typicalUsageHourDist = state.typicalUsageHourDist,
            )
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `RadialRoute` (function) — lines 24-57
