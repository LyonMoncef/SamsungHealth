package fr.datasaillance.nightfall.webview

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.CustomTabsIntent
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import org.json.JSONObject
import timber.log.Timber

class NightfallWebViewClient(
    private val allowedOrigin: String,
    private val tokenDataStore: TokenDataStore,
    private val onLogout: () -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val uri = request.url
        // CRITICAL: Use URI parsing (scheme+host+port) — NOT String.startsWith on the full URL.
        // String.startsWith("https://sh-prod.datasaillance.fr") would also match
        // "https://sh-prod.datasaillance.fr.attacker.com" — security test TA-WV-02 / pentester audit.
        val requestOrigin = buildString {
            append(uri.scheme ?: "")
            append("://")
            append(uri.host ?: "")
            if (uri.port != -1) append(":${uri.port}")
        }
        return if (requestOrigin == allowedOrigin) {
            false  // allowed — let WebView load
        } else {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(view.context, uri)
            true   // block WebView navigation
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        injectAuth(view)
        injectTheme(view)
    }

    private fun injectAuth(view: WebView) {
        val token = tokenDataStore.getToken() ?: return
        // JSONObject.quote escapes quotes/backslashes in the token before JS interpolation
        val safeToken = JSONObject.quote(token).removeSurrounding("\"")
        view.evaluateJavascript("window.__AUTH_TOKEN__='$safeToken';", null)
    }

    private fun injectTheme(view: WebView) {
        val isDark = (view.context.resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val themeVal = if (isDark) "dark" else "light"
        view.evaluateJavascript(
            "document.documentElement.setAttribute('data-theme','$themeVal');", null
        )
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()  // Never handler.proceed() — spec D5, pentester audit blocker
        Timber.e("WebView SSL error code=${error.primaryError} url=${view.url}")
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse,
    ) {
        if (request.isForMainFrame && errorResponse.statusCode in listOf(401, 403)) {
            Timber.w("WebView HTTP ${errorResponse.statusCode} on main frame — clearing token, logout")
            tokenDataStore.clearToken()
            onLogout()
        }
    }
}
