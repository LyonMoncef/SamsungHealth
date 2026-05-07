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
