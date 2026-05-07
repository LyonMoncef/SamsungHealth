---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NightfallTheme.kt
git_blob: 49b4e8dbb2292248f2d4a5ee472ee21594d06f6c
last_synced: '2026-05-07T00:48:24Z'
loc: 13
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NightfallTheme.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NightfallTheme.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/navigation/NightfallTheme.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import fr.datasaillance.nightfall.ui.theme.NightfallTheme as ThemeNightfallTheme

@Composable
fun NightfallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    ThemeNightfallTheme(darkTheme = darkTheme, content = content)
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallTheme` (function) — lines 7-13
