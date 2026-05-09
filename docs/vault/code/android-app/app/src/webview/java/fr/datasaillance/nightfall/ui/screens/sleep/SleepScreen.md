---
type: code-source
language: kotlin
file_path: android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepScreen.kt
git_blob: aeac17cd1b6c51b56a5e4519bd27b1bdf81d3adf
last_synced: '2026-05-09T04:03:35Z'
loc: 33
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepScreen.kt`](../../../android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.sleep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.settings.SettingsDataStore
import fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel
import fr.datasaillance.nightfall.webview.WebViewScreen

@Composable
fun SleepScreen(
    viewModel: SleepViewModel? = null,
    onSessionClick: (String) -> Unit = {},
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
        url = "$backendUrl/",
        modifier = Modifier,
        tokenDataStore = token,
        settingsDataStore = settings,
        onOpenImport = onOpenImport,
        onLogout = onLogout,
    )
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepScreen` (function) — lines 12-33
