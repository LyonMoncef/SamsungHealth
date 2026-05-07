---
type: code-source
language: kotlin
file_path: android-app/app/src/webview/java/fr/datasaillance/nightfall/ui/screens/activity/ActivityScreen.kt
git_blob: eb6ab8739c70539e531fc1f38de70db5dbae1eb7
last_synced: '2026-05-07T00:48:24Z'
loc: 18
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

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ActivityScreen() {
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            loadUrl("http://10.0.2.2:8001/activity")
        }
    })
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ActivityScreen` (function) — lines 8-18
