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
