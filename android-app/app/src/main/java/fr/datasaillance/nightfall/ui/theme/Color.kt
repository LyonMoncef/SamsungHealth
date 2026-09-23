package fr.datasaillance.nightfall.ui.theme

import androidx.compose.ui.graphics.Color

/* ============================================================
 * DataSaillance — brand core
 * Five immutable values extracted from the official identity.
 * See: design-system/colors.md in datasaillance-identite-visuelle
 * ============================================================ */
object BrandColors {
    val Encre        = Color(0xFF191E22)   // dark canvas
    val EncrePanel   = Color(0xFF232E32)   // intra-formes / panel
    val Teal         = Color(0xFF0E9EB0)   // primary accent (saturated)
    val Amber        = Color(0xFFD37C04)   // secondary accent / CTA
    val Cyan         = Color(0xFF3BE5E7)   // tertiary accent / data
    val Gray         = Color(0xFF828587)   // light-mode neutral

    // Halo strokes for the dark-mode logo only — never used in UI.
    val HaloCyanOuter  = Color(0xFF3BE5E7)
    val HaloCyanInner  = Color(0xFF8DFFFF)
    val HaloAmberOuter = Color(0xFF854808)
    val HaloAmberInner = Color(0xFFFCBF0E)
}

/* ============================================================
 * Semantic palette — dark theme (default)
 * ============================================================ */
object DarkPalette {
    val Bg          = Color(0xFF191E22)
    val Surface1    = Color(0xFF1E262B)
    val Surface2    = Color(0xFF232E32)
    val Surface3    = Color(0xFF2A363B)
    val Surface4    = Color(0xFF324048)

    val Text        = Color(0xFFE8EFF2)
    val TextStrong  = Color(0xFFF2F6F8)
    val TextMuted   = Color(0xFF7A9AAA)
    val TextFaint   = Color(0xFF506872)
    val TextOnAccent = Color(0xFF0F1316)

    val Border        = Color(0xFF2E3D44)
    val BorderStrong  = Color(0xFF43555E)
    val Divider       = Color(0x14E8EFF2) // 8% white

    val Accent      = Color(0xFF0E9EB0)
    val AccentHover = Color(0xFF14B3C7)
    val AccentPress = Color(0xFF0A8090)

    val Cta         = Color(0xFFD37C04)
    val CtaHover    = Color(0xFFE48A0F)
    val CtaPress    = Color(0xFFB86A02)

    val Highlight     = Color(0xFF3BE5E7)
    val HighlightSoft = Color(0x293BE5E7) // 16%

    val Success = Color(0xFF6FB58A)
    val Warning = Color(0xFFE3B23C)
    val Danger  = Color(0xFFE07260)
    val Info    = Color(0xFF3BE5E7)

    // Sleep-stage colors used by the Nightfall hypnogram.
    val StageAwake  = Color(0xFFD37C04)
    val StageRem    = Color(0xFF3BE5E7)
    val StageLight  = Color(0xFF6FB5C8)
    val StageDeep   = Color(0xFF2D5C73)

    val Scrim   = Color(0x8C000000) // 55%
}

/* ============================================================
 * Semantic palette — light theme
 * ============================================================ */
object LightPalette {
    val Bg          = Color(0xFFFFFFFF)
    val Surface1    = Color(0xFFFAFBFC)
    val Surface2    = Color(0xFFF4F8FA)
    val Surface3    = Color(0xFFECF1F4)
    val Surface4    = Color(0xFFE2E9ED)

    val Text        = Color(0xFF191E22)
    val TextStrong  = Color(0xFF0F1316)
    val TextMuted   = Color(0xFF5A646C)
    val TextFaint   = Color(0xFF828587)
    val TextOnAccent = Color(0xFFFFFFFF)

    val Border        = Color(0xFFD0DDE3)
    val BorderStrong  = Color(0xFFB2C3CB)
    val Divider       = Color(0x1A191E22) // 10% ink

    val Accent      = Color(0xFF0E9EB0)
    val AccentHover = Color(0xFF0A8090)
    val AccentPress = Color(0xFF086574)

    val Cta         = Color(0xFFD37C04)
    val CtaHover    = Color(0xFFB86A02)
    val CtaPress    = Color(0xFF8E5202)

    val Highlight     = Color(0xFF07BCD3)
    val HighlightSoft = Color(0x2407BCD3) // 14%

    val Success = Color(0xFF2F8F5C)
    val Warning = Color(0xFFB47A0A)
    val Danger  = Color(0xFFB83B2A)
    val Info    = Color(0xFF07BCD3)

    val StageAwake  = Color(0xFFD37C04)
    val StageRem    = Color(0xFF07BCD3)
    val StageLight  = Color(0xFF5C8CA0)
    val StageDeep   = Color(0xFF1F4A60)

    val Scrim   = Color(0x6B0F1316) // 42%
}

/* ============================================================
 * Pastel data-viz field — categorical chart series.
 * Use index 0..5 for chart-1..chart-6. Never use for chrome.
 * ============================================================ */
object DataViz {
    val DarkSeries = listOf(
        Color(0xFFB7D4DE), // brume — misty cyan
        Color(0xFFE4C99A), // sable — warm sand
        Color(0xFFA8C8A8), // mousse — soft moss
        Color(0xFFE3A89A), // corail — faded coral
        Color(0xFFC5B6D6), // lilas — dusty lilac
        Color(0xFFD8D2C4), // perle — parchment neutral
    )
    val LightSeries = listOf(
        Color(0xFF6FA5B6),
        Color(0xFFB68B3A),
        Color(0xFF6FA070),
        Color(0xFFB86F60),
        Color(0xFF8B7AAB),
        Color(0xFFA8A29A),
    )
}
