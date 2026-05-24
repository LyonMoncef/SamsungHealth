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
