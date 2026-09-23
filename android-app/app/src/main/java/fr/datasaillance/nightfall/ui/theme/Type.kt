package fr.datasaillance.nightfall.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ============================================================
 * DataSaillance — Type system (Android)
 *
 * Android natively uses Roboto (FontFamily.Default). Playfair
 * Display is WEB ONLY — it does not scale to mobile and is not
 * bundled in the APK. The display scale on Android therefore
 * relies on weight (Bold) and tracking to feel editorial without
 * a serif.
 *
 * Letter-spacing is in /em (sp via TextUnit) — Material 3
 * defaults are intentionally overridden to feel tighter at large
 * sizes and slightly looser at small sizes (per the identity).
 * ============================================================ */

private val Sans: FontFamily = FontFamily.Default  // Roboto

/* Sentinel tracking values, mirrored from the CSS tokens. */
private const val TrackingDisplay = -0.02
private const val TrackingTight   = -0.01
private const val TrackingBody    = 0.00
private const val TrackingLabel   = 0.02
private const val TrackingEyebrow = 0.12

val DataSaillanceTypography = Typography(
    /* ---- Display ----------------------------------------- */
    displayLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize   = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (TrackingDisplay * 56).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize   = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (TrackingDisplay * 45).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize   = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (TrackingDisplay * 36).sp,
    ),

    /* ---- Headline ---------------------------------------- */
    headlineLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize   = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (TrackingTight * 32).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (TrackingTight * 28).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),

    /* ---- Title (app bars, list rows, card titles) -------- */
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (0.15).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (0.1).sp,
    ),

    /* ---- Body -------------------------------------------- */
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (0.5).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (0.25).sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = (0.4).sp,
    ),

    /* ---- Label (buttons, chips, captions) ---------------- */
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (TrackingLabel * 14).sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = (TrackingLabel * 12).sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = (TrackingLabel * 11).sp,
    ),
)

/* Extra editorial / brand styles outside the Material 3 token slots.
   Add to MaterialTheme via a custom CompositionLocal if needed. */
object EditorialType {
    /** Eyebrow / tracked uppercase label. Min tracking 0.06em per identity. */
    val Eyebrow = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = (TrackingEyebrow * 11).sp,
    )

    /** Big numeric KPI in cards — tabular figures. */
    val KpiNumeric = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize   = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01 * 36).sp,
    )
}
