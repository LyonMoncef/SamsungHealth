---
type: code-source
language: kotlin
file_path: android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepScreen.kt
git_blob: df2455017ad7e239c5ddfb39570c4da38be079ac
last_synced: '2026-05-07T00:48:24Z'
loc: 18
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

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun SleepScreen() {
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            loadUrl("http://10.0.2.2:8001/")
        }
    })
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepScreen` (function) — lines 8-18
