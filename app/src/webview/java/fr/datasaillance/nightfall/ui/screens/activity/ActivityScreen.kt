package fr.datasaillance.nightfall.ui.screens.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.settings.SettingsDataStore
import fr.datasaillance.nightfall.webview.WebViewScreen

@Composable
fun ActivityScreen(
    tokenDataStore: TokenDataStore? = null,
    settingsDataStore: SettingsDataStore? = null,
    onOpenImport: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val context = LocalContext.current
    val token = remember(context) { tokenDataStore ?: TokenDataStore(context) }
    val settings = remember(context) { settingsDataStore ?: SettingsDataStore(context) }
    val backendUrl = settings.getBackendUrl().trimEnd('/')
    WebViewScreen(
        url = "$backendUrl/activity",
        modifier = Modifier,
        tokenDataStore = token,
        settingsDataStore = settings,
        onOpenImport = onOpenImport,
        onLogout = onLogout,
    )
}
