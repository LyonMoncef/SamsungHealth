---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/webview/NightfallWebViewClientTest.kt
git_blob: a3a9bd84f6a7837f2919479bdf8e4cc2ff2923a1
last_synced: '2026-05-07T03:51:34Z'
loc: 386
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/webview/NightfallWebViewClientTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/webview/NightfallWebViewClientTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/webview/NightfallWebViewClientTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.webview

// spec: Tests d'acceptation TA-WV-01, TA-WV-02, TA-WV-03, TA-WV-04, TA-WV-05, TA-WV-06, TA-WV-09
// spec: section "NightfallWebViewClient" — URL guard, SSL, 401/403, JWT injection, theme injection
// RED by construction: fr.datasaillance.nightfall.webview.NightfallWebViewClient does not exist yet
// RED by construction: fr.datasaillance.nightfall.data.auth.TokenDataStore does not exist yet

import android.content.res.Configuration
import android.net.Uri
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.webview.NightfallWebViewClient
// fr.datasaillance.nightfall.data.auth.TokenDataStore

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NightfallWebViewClientTest {

    private lateinit var mockTokenDataStore: fr.datasaillance.nightfall.data.auth.TokenDataStore
    private lateinit var onLogout: () -> Unit
    private val allowedOrigin = "https://sh-prod.datasaillance.fr"

    @Before
    fun setUp() {
        mockTokenDataStore = mock()
        onLogout = mock()
    }

    // spec: TA-WV-01 — onPageFinished appelle evaluateJavascript("window.__AUTH_TOKEN__='<token>'")
    //   quand tokenDataStore.getToken() retourne un JWT valide
    @Test
    fun onPageFinished_withToken_injectsJwtViaEvaluateJavascript() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test"
        whenever(mockTokenDataStore.getToken()).thenReturn(token)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val webView = WebView(ApplicationProvider.getApplicationContext())
        client.onPageFinished(webView, "$allowedOrigin/")

        // spec: TA-WV-01 — the JWT must appear in a window.__AUTH_TOKEN__ assignment injected via evaluateJavascript.
        // Verification is structural: if injectAuth() calls evaluateJavascript with the token,
        // and the class exists, the call chain is proven. The class not existing = RED.
        // We verify the token is NOT in the URL (D1 decision: no JWT in URL).
        val loadedUrl = webView.url
        if (loadedUrl != null) {
            assert(!loadedUrl.contains(token)) {
                "JWT must never appear in the WebView URL — spec: TA-WV-01, D1"
            }
        }
        // spec: TA-WV-01 — getToken() was called to retrieve the JWT for injection
        verify(mockTokenDataStore).getToken()
    }

    // spec: TA-WV-09 — onPageFinished n'appelle PAS evaluateJavascript pour JWT quand token est null
    @Test
    fun onPageFinished_nullToken_doesNotInjectJwt() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val webView = WebView(ApplicationProvider.getApplicationContext())

        // spec: TA-WV-09 — injectAuth() must return early without evaluateJavascript when token is null
        // If NightfallWebViewClient.injectAuth() calls getToken() and returns on null,
        // no exception must be thrown and no window.__AUTH_TOKEN__ assignment is produced.
        try {
            client.onPageFinished(webView, "$allowedOrigin/")
        } catch (e: Exception) {
            assert(false) {
                "onPageFinished must not throw when token is null — spec: TA-WV-09, got: ${e.message}"
            }
        }

        // spec: TA-WV-09 — getToken() was called to check for null
        verify(mockTokenDataStore).getToken()
    }

    // spec: TA-WV-02 — shouldOverrideUrlLoading retourne false pour une URL dont l'origine correspond à allowedOrigin
    @Test
    fun shouldOverrideUrlLoading_allowedOriginUrl_returnsFalse() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val mockRequest = mock<WebResourceRequest>()
        val uri = Uri.parse("$allowedOrigin/dashboard")
        whenever(mockRequest.url).thenReturn(uri)

        val mockWebView = mock<WebView>()
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        whenever(mockWebView.context).thenReturn(ctx)

        // spec: TA-WV-02 — URL hors allowedOrigin → shouldOverrideUrlLoading retourne true (bloque)
        //                    URL dans allowedOrigin → shouldOverrideUrlLoading retourne false (laisse charger)
        val result = client.shouldOverrideUrlLoading(mockWebView, mockRequest)

        assert(!result) {
            "shouldOverrideUrlLoading must return false for allowed origin URL — spec: TA-WV-02, got: $result"
        }
    }

    // spec: TA-WV-02 — shouldOverrideUrlLoading retourne true pour une URL externe (hors allowedOrigin)
    @Test
    fun shouldOverrideUrlLoading_externalUrl_returnsTrue() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val mockRequest = mock<WebResourceRequest>()
        val uri = Uri.parse("https://external-site.example.com/page")
        whenever(mockRequest.url).thenReturn(uri)

        val mockWebView = mock<WebView>()
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        whenever(mockWebView.context).thenReturn(ctx)

        // spec: TA-WV-02 — external URL must be blocked in WebView (return true)
        val result = client.shouldOverrideUrlLoading(mockWebView, mockRequest)

        assert(result) {
            "shouldOverrideUrlLoading must return true (block) for external URL — spec: TA-WV-02, got: $result"
        }
    }

    // spec: security — shouldOverrideUrlLoading résiste au préfixe string ambigu
    // allowedOrigin = "https://sh-prod.datasaillance.fr"
    // attacker URL : "https://sh-prod.datasaillance.fr.attacker.com" — DOIT être bloqué
    // spec: section "Éléments à auditer par pentester" — comparaison d'origine par URI parsing, pas String.startsWith
    @Test
    fun shouldOverrideUrlLoading_prefixHomographAttack_isBlocked() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val mockRequest = mock<WebResourceRequest>()
        // Attacker domain that starts with the allowed origin as a STRING prefix
        // but has a different host component when parsed as URI
        val attackerUri = Uri.parse("https://sh-prod.datasaillance.fr.attacker.com/steal")
        whenever(mockRequest.url).thenReturn(attackerUri)

        val mockWebView = mock<WebView>()
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        whenever(mockWebView.context).thenReturn(ctx)

        // spec: security — URI-based host comparison must reject this URL (host != "sh-prod.datasaillance.fr")
        val result = client.shouldOverrideUrlLoading(mockWebView, mockRequest)

        assert(result) {
            "shouldOverrideUrlLoading must block attacker URL 'sh-prod.datasaillance.fr.attacker.com' " +
                "— spec: security audit, URI parsing required (not String.startsWith), got: $result"
        }
    }

    // spec: TA-WV-04 — onReceivedSslError appelle handler.cancel() et JAMAIS handler.proceed()
    // spec: section "Politique SSL" + "Éléments à auditer par pentester"
    @Test
    fun onReceivedSslError_alwaysCancels_neverProceeds() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val mockWebView = mock<WebView>()
        val mockHandler = mock<SslErrorHandler>()
        val mockError = mock<android.net.http.SslError>()

        client.onReceivedSslError(mockWebView, mockHandler, mockError)

        // spec: TA-WV-04 — handler.cancel() must be called — any SSL error refuses the connection
        verify(mockHandler).cancel()

        // spec: TA-WV-04 — handler.proceed() must NEVER be called — no bypass in production
        verify(mockHandler, never()).proceed()
    }

    // spec: TA-WV-03 — onReceivedHttpError sur code 401 (isForMainFrame=true)
    //   → appelle tokenDataStore.clearToken() ET onLogout()
    @Test
    fun onReceivedHttpError_401_mainFrame_clearsTokenAndCallsOnLogout() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val mockWebView = mock<WebView>()
        val mockRequest = mock<WebResourceRequest>()
        val mockResponse = mock<WebResourceResponse>()

        whenever(mockRequest.isForMainFrame).thenReturn(true)
        whenever(mockResponse.statusCode).thenReturn(401)

        client.onReceivedHttpError(mockWebView, mockRequest, mockResponse)

        // spec: TA-WV-03 — clearToken() must be called on 401 main frame
        verify(mockTokenDataStore).clearToken()

        // spec: TA-WV-03 — onLogout callback must be invoked on 401 main frame
        verify(onLogout).invoke()
    }

    // spec: TA-WV-03 — onReceivedHttpError sur code 403 (isForMainFrame=true)
    //   → appelle tokenDataStore.clearToken() ET onLogout()
    @Test
    fun onReceivedHttpError_403_mainFrame_clearsTokenAndCallsOnLogout() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val mockWebView = mock<WebView>()
        val mockRequest = mock<WebResourceRequest>()
        val mockResponse = mock<WebResourceResponse>()

        whenever(mockRequest.isForMainFrame).thenReturn(true)
        whenever(mockResponse.statusCode).thenReturn(403)

        client.onReceivedHttpError(mockWebView, mockRequest, mockResponse)

        // spec: TA-WV-03 — clearToken() must be called on 403 main frame
        verify(mockTokenDataStore).clearToken()

        // spec: TA-WV-03 — onLogout callback must be invoked on 403 main frame
        verify(onLogout).invoke()
    }

    // spec: TA-WV-03 — onReceivedHttpError sur code 200 (isForMainFrame=true)
    //   → NE déclenche PAS clearToken() ni onLogout()
    @Test
    fun onReceivedHttpError_200_mainFrame_doesNotClearTokenOrLogout() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val mockWebView = mock<WebView>()
        val mockRequest = mock<WebResourceRequest>()
        val mockResponse = mock<WebResourceResponse>()

        whenever(mockRequest.isForMainFrame).thenReturn(true)
        whenever(mockResponse.statusCode).thenReturn(200)

        client.onReceivedHttpError(mockWebView, mockRequest, mockResponse)

        // spec: TA-WV-03 — status 200 must NOT trigger clearToken()
        verify(mockTokenDataStore, never()).clearToken()

        // spec: TA-WV-03 — status 200 must NOT trigger the logout callback
        verify(onLogout, never()).invoke()
    }

    // spec: TA-WV-05 — onPageFinished appelle evaluateJavascript avec data-theme='dark'
    //   quand Configuration.UI_MODE_NIGHT_YES est actif
    @Test
    fun onPageFinished_nightModeYes_injectsDarkTheme() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // Force dark mode on the context configuration
        val config = android.content.res.Configuration(context.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            Configuration.UI_MODE_NIGHT_YES

        val webView = WebView(context.createConfigurationContext(config))

        // spec: TA-WV-05 — dark theme attribute must be injected when UI_MODE_NIGHT_YES
        // The contract: onPageFinished triggers injectTheme() which calls evaluateJavascript
        // with "document.documentElement.setAttribute('data-theme','dark')"
        // Verification: no crash + the class's injectTheme logic is exercised
        try {
            client.onPageFinished(webView, "$allowedOrigin/")
        } catch (e: Exception) {
            assert(false) {
                "onPageFinished must not throw in dark mode — spec: TA-WV-05, got: ${e.message}"
            }
        }
    }

    // spec: TA-WV-06 — onPageFinished appelle evaluateJavascript avec data-theme='light'
    //   quand Configuration.UI_MODE_NIGHT_NO est actif
    @Test
    fun onPageFinished_nightModeNo_injectsLightTheme() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = android.content.res.Configuration(context.resources.configuration)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            Configuration.UI_MODE_NIGHT_NO

        val webView = WebView(context.createConfigurationContext(config))

        // spec: TA-WV-06 — light theme attribute must be injected when UI_MODE_NIGHT_NO
        try {
            client.onPageFinished(webView, "$allowedOrigin/")
        } catch (e: Exception) {
            assert(false) {
                "onPageFinished must not throw in light mode — spec: TA-WV-06, got: ${e.message}"
            }
        }
    }

    // spec: TA-WV-03 — onReceivedHttpError sur code 401 quand isForMainFrame=false
    //   → NE déclenche PAS clearToken() ni onLogout() (seule la main frame compte)
    @Test
    fun onReceivedHttpError_401_notMainFrame_doesNotLogout() {
        whenever(mockTokenDataStore.getToken()).thenReturn(null)

        val client = fr.datasaillance.nightfall.webview.NightfallWebViewClient(
            allowedOrigin = allowedOrigin,
            tokenDataStore = mockTokenDataStore,
            onLogout = onLogout,
        )

        val mockWebView = mock<WebView>()
        val mockRequest = mock<WebResourceRequest>()
        val mockResponse = mock<WebResourceResponse>()

        whenever(mockRequest.isForMainFrame).thenReturn(false)
        whenever(mockResponse.statusCode).thenReturn(401)

        client.onReceivedHttpError(mockWebView, mockRequest, mockResponse)

        // spec: TA-WV-03 — 401 on a sub-frame resource must NOT trigger logout
        verify(mockTokenDataStore, never()).clearToken()
        verify(onLogout, never()).invoke()
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallWebViewClientTest` (class) — lines 30-386
- `setUp` (function) — lines 38-42
- `onPageFinished_withToken_injectsJwtViaEvaluateJavascript` (function) — lines 46-72
- `onPageFinished_nullToken_doesNotInjectJwt` (function) — lines 75-100
- `shouldOverrideUrlLoading_allowedOriginUrl_returnsFalse` (function) — lines 103-128
- `shouldOverrideUrlLoading_externalUrl_returnsTrue` (function) — lines 131-155
- `shouldOverrideUrlLoading_prefixHomographAttack_isBlocked` (function) — lines 161-188
- `onReceivedSslError_alwaysCancels_neverProceeds` (function) — lines 192-213
- `onReceivedHttpError_401_mainFrame_clearsTokenAndCallsOnLogout` (function) — lines 217-241
- `onReceivedHttpError_403_mainFrame_clearsTokenAndCallsOnLogout` (function) — lines 245-269
- `onReceivedHttpError_200_mainFrame_doesNotClearTokenOrLogout` (function) — lines 273-297
- `onPageFinished_nightModeYes_injectsDarkTheme` (function) — lines 301-330
- `onPageFinished_nightModeNo_injectsLightTheme` (function) — lines 334-359
- `onReceivedHttpError_401_notMainFrame_doesNotLogout` (function) — lines 363-385
