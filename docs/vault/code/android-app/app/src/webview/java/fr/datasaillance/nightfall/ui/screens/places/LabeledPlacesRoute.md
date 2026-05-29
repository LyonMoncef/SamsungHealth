---
type: code-source
language: kotlin
file_path: android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/places/LabeledPlacesRoute.kt
git_blob: d3f4725f32ac8087be2acae3fbb223b123daf7d8
last_synced: '2026-05-29T08:09:08Z'
loc: 30
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/places/LabeledPlacesRoute.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/places/LabeledPlacesRoute.kt`](../../../android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/places/LabeledPlacesRoute.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.places

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Stub flavor `webview` — l'écran de configuration des lieux labellisés utilise
 * du Compose Material3 natif, pas disponible dans le shell webview. Aligné avec
 * le pattern `RadialRoute` (cf. src/webview/.../ui/screens/radial/).
 */
@Composable
fun LabeledPlacesRoute(onBack: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Lieux connus — disponible uniquement en mode natif.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LabeledPlacesRoute` (function) — lines 18-30
