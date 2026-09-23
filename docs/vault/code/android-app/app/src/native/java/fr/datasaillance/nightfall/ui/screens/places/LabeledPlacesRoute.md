---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/places/LabeledPlacesRoute.kt
git_blob: 27b8a4b1c86c2921d63ec90c8257426c879668c6
last_synced: '2026-05-29T08:09:08Z'
loc: 40
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/places/LabeledPlacesRoute.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/places/LabeledPlacesRoute.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/places/LabeledPlacesRoute.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.places

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.viewmodel.places.LabeledPlacesViewModel

/**
 * Route Compose qui assemble `LabeledPlacesViewModel` + `LabeledPlacesScreen` côté
 * flavor `native`. La séparation route/screen suit le pattern `RadialRoute` :
 *   - le screen reste pur (state-in / events-out),
 *   - la route fait l'instanciation DAO + ViewModel + collectAsState,
 *   - les flavors qui ne savent pas afficher l'écran (ex. webview) fournissent un
 *     stub `LabeledPlacesRoute()` qui ne dépend pas de ce screen.
 */
@Composable
fun LabeledPlacesRoute(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val db = remember(context) { NightfallDatabase.get(context.applicationContext) }
    val viewModel = remember(db) {
        LabeledPlacesViewModel(
            labeledPlaceDao = db.labeledPlaceDao(),
            locationDao = db.locationDao(),
        )
    }
    val state by viewModel.uiState.collectAsState()

    LabeledPlacesScreen(
        state = state,
        onAddPlace = { label, category, lat, lng, radius ->
            viewModel.addPlace(label, category, lat, lng, radius)
        },
        onUpdatePlace = { viewModel.updatePlace(it) },
        onDeletePlace = { viewModel.deletePlace(it) },
        onBack = onBack,
    )
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LabeledPlacesRoute` (function) — lines 19-40
