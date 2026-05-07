---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/webview/WebViewScreen.kt
git_blob: 83c796d5235d2ae724673c9c00c9cd93726653be
last_synced: '2026-05-07T03:51:34Z'
loc: 65
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/webview/WebViewScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/webview/WebViewScreen.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/webview/WebViewScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.webview

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.settings.SettingsDataStore
import java.net.URI

@Composable
fun WebViewScreen(
    url: String,
    modifier: Modifier = Modifier,
    tokenDataStore: TokenDataStore,
    settingsDataStore: SettingsDataStore,
    onOpenImport: () -> Unit,
    onLogout: () -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val themeValue = if (isDarkTheme) "dark" else "light"

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    setAllowFileAccess(false)
                    setAllowContentAccess(false)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                }
                val backendUrl = settingsDataStore.getBackendUrl().trimEnd('/')
                val parsedUri = try { URI(backendUrl) } catch (e: Exception) { null }
                val backendOrigin = if (parsedUri != null) {
                    "${parsedUri.scheme}://${parsedUri.host}${if (parsedUri.port != -1) ":${parsedUri.port}" else ""}"
                } else backendUrl

                webViewClient = NightfallWebViewClient(
                    allowedOrigin  = backendOrigin,
                    tokenDataStore = tokenDataStore,
                    onLogout       = onLogout,
                )
                addJavascriptInterface(
                    NightfallJsInterface(
                        context      = ctx,
                        onOpenImport = onOpenImport,
                    ),
                    "NightfallBridge"
                )
                loadUrl(url)
            }
        },
        update = { webView ->
            webView.evaluateJavascript(
                "document.documentElement.setAttribute('data-theme','$themeValue')", null
            )
        },
        modifier = modifier.fillMaxSize(),
    )
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `WebViewScreen` (function) — lines 14-65
