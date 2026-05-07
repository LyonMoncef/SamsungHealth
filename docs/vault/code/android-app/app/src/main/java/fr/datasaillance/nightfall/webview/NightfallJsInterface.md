---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/webview/NightfallJsInterface.kt
git_blob: f8daf2812cef0b4ad477e8933223ffc0d9d3ad8b
last_synced: '2026-05-07T03:51:34Z'
loc: 33
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/webview/NightfallJsInterface.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/webview/NightfallJsInterface.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/webview/NightfallJsInterface.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.webview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import fr.datasaillance.nightfall.BuildConfig
import timber.log.Timber

class NightfallJsInterface(
    private val context: Context,
    private val onOpenImport: () -> Unit,
) {

    @JavascriptInterface
    fun getAppVersion(): String = BuildConfig.VERSION_NAME

    @JavascriptInterface
    fun openImport() {
        Handler(Looper.getMainLooper()).post {
            onOpenImport()
        }
    }

    @JavascriptInterface
    fun onNightfallEvent(eventJson: String) {
        if (eventJson.length > 4096) {
            Timber.w("NightfallBridge.onNightfallEvent: payload too large (${eventJson.length} chars), ignoring")
            return
        }
        Timber.d("NightfallBridge.onNightfallEvent: $eventJson")
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallJsInterface` (class) — lines 10-33
- `getAppVersion` (function) — lines 15-16
- `openImport` (function) — lines 18-23
- `onNightfallEvent` (function) — lines 25-32
