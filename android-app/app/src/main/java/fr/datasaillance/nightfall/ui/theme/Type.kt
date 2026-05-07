package fr.datasaillance.nightfall.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.R

// OptionalLocal: font loads from res/font/ on device; falls back to system default in Paparazzi
// (Paparazzi's JVM renderer cannot resolve R.font resources — OptionalLocal prevents the crash)
private val fontStrategy = FontLoadingStrategy.OptionalLocal

// Display / headings — Playfair Display (serif, canonical DataSaillance brand font)
val PlayfairDisplayFamily = FontFamily(
    Font(R.font.playfair_display_variable, FontWeight.Bold, loadingStrategy = fontStrategy),
)

// Body / UI — Inter (sans-serif, canonical DataSaillance UI font)
val InterFontFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal,   loadingStrategy = fontStrategy),
    Font(R.font.inter_variable, FontWeight.Medium,   loadingStrategy = fontStrategy),
    Font(R.font.inter_variable, FontWeight.SemiBold, loadingStrategy = fontStrategy),
)

val NightfallTypography = Typography(
    // Headings — Playfair Display Bold (H1/H2 equiv per typography.md)
    displayLarge   = TextStyle(fontFamily = PlayfairDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium  = TextStyle(fontFamily = PlayfairDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    headlineLarge  = TextStyle(fontFamily = PlayfairDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = PlayfairDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = PlayfairDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge     = TextStyle(fontFamily = PlayfairDisplayFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),

    // UI / body — Inter (H3+ equiv per typography.md)
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge   = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall   = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp),
)
