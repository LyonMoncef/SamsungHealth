---
type: code-source
language: kotlin
file_path: android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/trends/TrendsScreen.kt
git_blob: f6161ec66ef467e568a7aacde3dd89a1f76f7052
last_synced: '2026-05-07T00:48:24Z'
loc: 18
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

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun TrendsScreen() {
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            loadUrl("http://10.0.2.2:8001/trends")
        }
    })
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TrendsScreen` (function) — lines 8-18
