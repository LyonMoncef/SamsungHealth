---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/components/AuthErrorMessage.kt
git_blob: eb347a1579322297e5763a2317496892b1efce4f
last_synced: '2026-05-07T02:02:39Z'
loc: 23
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/components/AuthErrorMessage.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/components/AuthErrorMessage.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/components/AuthErrorMessage.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.auth.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

@Composable
fun AuthErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        }
    )
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `AuthErrorMessage` (function) — lines 11-23
