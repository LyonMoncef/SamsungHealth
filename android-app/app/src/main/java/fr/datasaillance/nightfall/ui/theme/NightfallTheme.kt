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
