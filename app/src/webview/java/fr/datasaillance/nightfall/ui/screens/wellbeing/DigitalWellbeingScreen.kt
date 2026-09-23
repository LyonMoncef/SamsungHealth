package fr.datasaillance.nightfall.ui.screens.wellbeing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.viewmodel.wellbeing.DigitalWellbeingViewModel

/**
 * Stub flavor `webview` — l'écran Bien-être numérique est uniquement disponible
 * sur le flavor `native` (Phase B_us). Le flavor webview affichait jusqu'ici
 * l'app via WebView ; comme il n'y a pas d'équivalent serveur pour ces données
 * locales-only, on montre un placeholder.
 */
@Composable
fun DigitalWellbeingScreen(
    viewModel: DigitalWellbeingViewModel,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Bien-être numérique — disponible uniquement en mode natif.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
