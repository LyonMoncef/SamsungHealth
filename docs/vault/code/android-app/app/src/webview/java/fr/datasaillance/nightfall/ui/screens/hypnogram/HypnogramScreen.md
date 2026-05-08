---
type: code-source
language: kotlin
file_path: android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/hypnogram/HypnogramScreen.kt
git_blob: 0bc97448024e105c2ae467bf4b289f15a5b1448e
last_synced: '2026-05-08T06:12:11Z'
loc: 14
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/hypnogram/HypnogramScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/hypnogram/HypnogramScreen.kt`](../../../android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/hypnogram/HypnogramScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.hypnogram

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel

@Composable
fun HypnogramScreen(
    sessionId: String,
    sleepViewModel: SleepViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onBack() }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `HypnogramScreen` (function) — lines 7-14
