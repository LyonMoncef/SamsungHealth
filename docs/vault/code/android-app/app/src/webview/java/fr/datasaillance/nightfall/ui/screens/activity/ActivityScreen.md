---
type: code-source
language: kotlin
file_path: android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/activity/ActivityScreen.kt
git_blob: 58c35c31770952679f6cef7cb296c314821d0435
last_synced: '2026-05-07T03:51:34Z'
loc: 30
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/activity/ActivityScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/activity/ActivityScreen.kt`](../../../android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/activity/ActivityScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ActivityScreen` (function) — lines 11-30
