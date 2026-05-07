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
