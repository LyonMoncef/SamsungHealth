---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/DataSaillanceTheme.kt
git_blob: bf5098d243d5373dfa2e73c7a2ac7c9da96a4186
last_synced: '2026-05-26T03:04:09Z'
loc: 165
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/DataSaillanceTheme.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/DataSaillanceTheme.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/DataSaillanceTheme.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/* ============================================================
 * DataSaillance — Theme entry point
 *
 * Wraps Material 3 with the DataSaillance color scheme,
 * typography and shapes. Use `darkTheme` to force a specific
 * mode in previews; defaults to the system.
 *
 * The brief tokens (sleep stage colors, pastel data-viz field,
 * Saillance pulse duration) live outside the Material 3 color
 * scheme — expose them via a CompositionLocal so screens can
 * read them like MaterialTheme.colorScheme.
 * ============================================================ */

data class ExtraColors(
    val highlight: androidx.compose.ui.graphics.Color,
    val highlightSoft: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val borderStrong: androidx.compose.ui.graphics.Color,
    val divider: androidx.compose.ui.graphics.Color,
    val textStrong: androidx.compose.ui.graphics.Color,
    val textMuted: androidx.compose.ui.graphics.Color,
    val textFaint: androidx.compose.ui.graphics.Color,
    val success: androidx.compose.ui.graphics.Color,
    val warning: androidx.compose.ui.graphics.Color,
    val stageAwake: androidx.compose.ui.graphics.Color,
    val stageRem: androidx.compose.ui.graphics.Color,
    val stageLight: androidx.compose.ui.graphics.Color,
    val stageDeep: androidx.compose.ui.graphics.Color,
    val dataViz: List<androidx.compose.ui.graphics.Color>,
)

val LocalExtraColors = staticCompositionLocalOf<ExtraColors> {
    error("No ExtraColors provided — wrap your composable in DataSaillanceTheme.")
}

private val DarkColors = darkColorScheme(
    primary           = DarkPalette.Accent,
    onPrimary         = DarkPalette.TextStrong,
    primaryContainer  = DarkPalette.AccentPress,
    onPrimaryContainer = DarkPalette.TextStrong,
    secondary         = DarkPalette.Cta,
    onSecondary       = DarkPalette.TextOnAccent,
    secondaryContainer = DarkPalette.CtaPress,
    onSecondaryContainer = DarkPalette.TextStrong,
    tertiary          = DarkPalette.Highlight,
    onTertiary        = DarkPalette.TextOnAccent,
    background        = DarkPalette.Bg,
    onBackground      = DarkPalette.Text,
    surface           = DarkPalette.Surface2,
    onSurface         = DarkPalette.Text,
    surfaceVariant    = DarkPalette.Surface3,
    onSurfaceVariant  = DarkPalette.TextMuted,
    surfaceTint       = DarkPalette.Accent,
    error             = DarkPalette.Danger,
    onError           = DarkPalette.TextStrong,
    outline           = DarkPalette.Border,
    outlineVariant    = DarkPalette.BorderStrong,
    scrim             = DarkPalette.Scrim,
)

private val LightColors = lightColorScheme(
    primary           = LightPalette.Accent,
    onPrimary         = LightPalette.TextOnAccent,
    primaryContainer  = LightPalette.AccentHover,
    onPrimaryContainer = LightPalette.TextOnAccent,
    secondary         = LightPalette.Cta,
    onSecondary       = LightPalette.TextOnAccent,
    secondaryContainer = LightPalette.CtaHover,
    onSecondaryContainer = LightPalette.TextOnAccent,
    tertiary          = LightPalette.Highlight,
    onTertiary        = LightPalette.TextOnAccent,
    background        = LightPalette.Bg,
    onBackground      = LightPalette.Text,
    surface           = LightPalette.Bg,
    onSurface         = LightPalette.Text,
    surfaceVariant    = LightPalette.Surface2,
    onSurfaceVariant  = LightPalette.TextMuted,
    surfaceTint       = LightPalette.Accent,
    error             = LightPalette.Danger,
    onError           = LightPalette.TextOnAccent,
    outline           = LightPalette.Border,
    outlineVariant    = LightPalette.BorderStrong,
    scrim             = LightPalette.Scrim,
)

private val DarkExtras = ExtraColors(
    highlight       = DarkPalette.Highlight,
    highlightSoft   = DarkPalette.HighlightSoft,
    border          = DarkPalette.Border,
    borderStrong    = DarkPalette.BorderStrong,
    divider         = DarkPalette.Divider,
    textStrong      = DarkPalette.TextStrong,
    textMuted       = DarkPalette.TextMuted,
    textFaint       = DarkPalette.TextFaint,
    success         = DarkPalette.Success,
    warning         = DarkPalette.Warning,
    stageAwake      = DarkPalette.StageAwake,
    stageRem        = DarkPalette.StageRem,
    stageLight      = DarkPalette.StageLight,
    stageDeep       = DarkPalette.StageDeep,
    dataViz         = DataViz.DarkSeries,
)

private val LightExtras = ExtraColors(
    highlight       = LightPalette.Highlight,
    highlightSoft   = LightPalette.HighlightSoft,
    border          = LightPalette.Border,
    borderStrong    = LightPalette.BorderStrong,
    divider         = LightPalette.Divider,
    textStrong      = LightPalette.TextStrong,
    textMuted       = LightPalette.TextMuted,
    textFaint       = LightPalette.TextFaint,
    success         = LightPalette.Success,
    warning         = LightPalette.Warning,
    stageAwake      = LightPalette.StageAwake,
    stageRem        = LightPalette.StageRem,
    stageLight      = LightPalette.StageLight,
    stageDeep       = LightPalette.StageDeep,
    dataViz         = DataViz.LightSeries,
)

@Composable
fun DataSaillanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors  = if (darkTheme) DarkColors else LightColors
    val extras  = if (darkTheme) DarkExtras else LightExtras
    androidx.compose.runtime.CompositionLocalProvider(LocalExtraColors provides extras) {
        MaterialTheme(
            colorScheme = colors,
            typography  = DataSaillanceTypography,
            shapes      = DataSaillanceShapes,
            content     = content,
        )
    }
}

/** Convenience accessor mirroring `MaterialTheme.colorScheme`. */
object DataSaillance {
    val extras: ExtraColors
        @Composable get() = LocalExtraColors.current
}

/**
 * Rétro-compat : ancien call site `NightfallTheme { ... }` continue de marcher.
 * Toutes les nouvelles call sites devraient utiliser `DataSaillanceTheme` directement.
 */
@Composable
fun NightfallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    DataSaillanceTheme(darkTheme = darkTheme, content = content)
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ExtraColors` (class) — lines 24-40
- `DataSaillanceTheme` (function) — lines 132-147
- `NightfallTheme` (function) — lines 159-165
