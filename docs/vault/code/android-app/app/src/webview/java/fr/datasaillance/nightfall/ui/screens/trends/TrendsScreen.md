---
type: code-source
language: kotlin
file_path: android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/trends/TrendsScreen.kt
git_blob: 5a1637781830dd92464191096499880557b7cef8
last_synced: '2026-05-07T03:51:34Z'
loc: 30
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/trends/TrendsScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/trends/TrendsScreen.kt`](../../../android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/trends/TrendsScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.trends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.settings.SettingsDataStore
import fr.datasaillance.nightfall.webview.WebViewScreen

@Composable
fun TrendsScreen(
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
        url = "$backendUrl/trends",
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
- `TrendsScreen` (function) — lines 11-30
