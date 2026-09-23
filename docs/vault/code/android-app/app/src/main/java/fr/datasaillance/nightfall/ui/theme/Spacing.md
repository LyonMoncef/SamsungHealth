---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/Spacing.kt
git_blob: 2e0e5863b42b706afb432fc7e905fada4601da0d
last_synced: '2026-05-24T01:17:33Z'
loc: 28
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/Spacing.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/Spacing.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/Spacing.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* ============================================================
 * DataSaillance — Spacing tokens (4 dp base grid)
 *
 * Mirrors the CSS --space-* tokens in colors_and_type.css.
 * Use these instead of hard-coded dp values.
 * ============================================================ */
object Spacing {
    val none: Dp     = 0.dp
    val xxs: Dp      = 4.dp    // chip inset
    val xs: Dp       = 8.dp    // tight inline gap
    val sm: Dp       = 12.dp   // list row inner
    val md: Dp       = 16.dp   // default screen padding
    val lg: Dp       = 20.dp
    val xl: Dp       = 24.dp   // card inner padding
    val xxl: Dp      = 32.dp   // section gap
    val xxxl: Dp     = 40.dp

    /** Minimum touch-target — never use a smaller hit area. */
    val touchTarget: Dp = 48.dp

    /** Spacing reserved for the bottom safe area on edge-to-edge layouts. */
    val systemBar: Dp = 24.dp
}
```

---

## Appendix — symbols & navigation *(auto)*
