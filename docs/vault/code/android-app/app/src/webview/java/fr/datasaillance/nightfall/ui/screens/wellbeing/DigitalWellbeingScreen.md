---
type: code-source
language: kotlin
file_path: android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/wellbeing/DigitalWellbeingScreen.kt
git_blob: 5aa97ad477433f83932c3c564442f46e83e5d08f
last_synced: '2026-05-20T18:28:21Z'
loc: 34
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/wellbeing/DigitalWellbeingScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/wellbeing/DigitalWellbeingScreen.kt`](../../../android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/wellbeing/DigitalWellbeingScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.wellbeing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.viewmodel.wellbeing.DigitalWellbeingViewModel

/**
 * Stub flavor `webview` — l'écran Bien-être numérique est uniquement disponible
 * sur le flavor `native` (Phase B_us). Le flavor webview affichait jusqu'ici
 * l'app via WebView ; comme il n'y a pas d'équivalent serveur pour ces données
 * locales-only, on montre un placeholder.
 */
@Composable
fun DigitalWellbeingScreen(
    viewModel: DigitalWellbeingViewModel,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Bien-être numérique — disponible uniquement en mode natif.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `DigitalWellbeingScreen` (function) — lines 20-34
