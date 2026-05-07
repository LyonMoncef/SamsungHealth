---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/NightfallTheme.kt
git_blob: b6065162a6ea0325cdbfbf8343658b55094cb6cd
last_synced: '2026-05-07T00:48:24Z'
loc: 37
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/NightfallTheme.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/NightfallTheme.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/NightfallTheme.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun NightfallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme(
        primary      = Teal700,
        secondary    = Amber600,
        tertiary     = Cyan500,
        background   = Background,
        surface      = Surface,
        onBackground = Color(0xFFE8E4DC),
        onSurface    = Color(0xFFE8E4DC),
    ) else lightColorScheme(
        primary      = Teal700,
        secondary    = Amber600,
        tertiary     = Cyan500,
        background   = BackgroundLight,
        surface      = SurfaceLight,
        onBackground = Color(0xFF1A1916),
        onSurface    = Color(0xFF1A1916),
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NightfallTypography,
        content     = content
    )
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallTheme` (function) — lines 10-37
