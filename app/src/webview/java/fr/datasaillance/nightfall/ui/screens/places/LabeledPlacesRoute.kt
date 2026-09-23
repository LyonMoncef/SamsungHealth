package fr.datasaillance.nightfall.ui.screens.places

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Stub flavor `webview` — l'écran de configuration des lieux labellisés utilise
 * du Compose Material3 natif, pas disponible dans le shell webview. Aligné avec
 * le pattern `RadialRoute` (cf. src/webview/.../ui/screens/radial/).
 */
@Composable
fun LabeledPlacesRoute(onBack: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Lieux connus — disponible uniquement en mode natif.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
