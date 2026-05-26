---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/Shape.kt
git_blob: d2d250a17e792a73d4a0f79828d2863c6b0f027a
last_synced: '2026-05-24T01:17:33Z'
loc: 31
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/Shape.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/Shape.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/theme/Shape.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* ============================================================
 * DataSaillance — Shapes (corner radii)
 *
 * Tight, never bubbly. Maximum 24 dp on UI surfaces.
 * Bottom sheets clip top-only corners.
 * ============================================================ */
val DataSaillanceShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),    // chips, small inputs
    small      = RoundedCornerShape(8.dp),    // buttons, text fields
    medium     = RoundedCornerShape(12.dp),   // cards, list items
    large      = RoundedCornerShape(16.dp),   // sheets, large panels
    extraLarge = RoundedCornerShape(24.dp),   // bottom-sheet handle
)

/** Bottom sheet — only top corners are rounded. */
val BottomSheetShape = RoundedCornerShape(
    topStart = CornerSize(24.dp),
    topEnd   = CornerSize(24.dp),
    bottomStart = CornerSize(0.dp),
    bottomEnd   = CornerSize(0.dp),
)

/** Pill — used for filter chips and badges that need to feel softer. */
val PillShape = RoundedCornerShape(999.dp)
```

---

## Appendix — symbols & navigation *(auto)*
