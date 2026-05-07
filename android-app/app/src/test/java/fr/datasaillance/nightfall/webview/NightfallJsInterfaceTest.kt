package fr.datasaillance.nightfall.webview

// spec: Tests d'acceptation TA-WV-07
// spec: section "NightfallJsInterface" — @JavascriptInterface bridge JS → natif
// RED by construction: fr.datasaillance.nightfall.webview.NightfallJsInterface does not exist yet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.webview.NightfallJsInterface

// Robolectric required: openImport() uses Handler(Looper.getMainLooper())
// which needs a working main Looper — Robolectric provides that.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NightfallJsInterfaceTest {

    private lateinit var context: Context
    private lateinit var onOpenImport: () -> Unit

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        onOpenImport = mock()
    }

    // spec: TA-WV-07 — getAppVersion() retourne une string non-vide correspondant à BuildConfig.VERSION_NAME
    // spec: section "NightfallJsInterface.getAppVersion()" — "@JavascriptInterface fun getAppVersion(): String"
    @Test
    fun getAppVersion_returnsNonEmptyVersionName() {
        val jsInterface = fr.datasaillance.nightfall.webview.NightfallJsInterface(
            context = context,
            onOpenImport = onOpenImport,
        )

        val version = jsInterface.getAppVersion()

        // spec: TA-WV-07 — must return a non-empty string
        assert(version.isNotEmpty()) {
            "getAppVersion() must return a non-empty string matching BuildConfig.VERSION_NAME — spec: TA-WV-07"
        }
    }

    // spec: TA-WV-07 — getAppVersion() retourne exactement BuildConfig.VERSION_NAME ("4.0.0" pour Phase 4)
    @Test
    fun getAppVersion_returnsBuildConfigVersionName() {
        val jsInterface = fr.datasaillance.nightfall.webview.NightfallJsInterface(
            context = context,
            onOpenImport = onOpenImport,
        )

        val version = jsInterface.getAppVersion()

        // spec: TA-WV-07 — VERSION_NAME is "4.0.0" per build.gradle.kts versionName = "4.0.0"
        assert(version == fr.datasaillance.nightfall.BuildConfig.VERSION_NAME) {
            "getAppVersion() must return BuildConfig.VERSION_NAME — spec: TA-WV-07, expected: " +
                "'${fr.datasaillance.nightfall.BuildConfig.VERSION_NAME}', got: '$version'"
        }
    }

    // spec: TA-WV-07 — openImport() déclenche le callback onOpenImport sur le main thread
    // spec: section "NightfallJsInterface.openImport()" — Handler(Looper.getMainLooper()).post { onOpenImport() }
    @Test
    fun openImport_triggersOnOpenImportCallback() {
        val jsInterface = fr.datasaillance.nightfall.webview.NightfallJsInterface(
            context = context,
            onOpenImport = onOpenImport,
        )

        jsInterface.openImport()

        // spec: TA-WV-07 — openImport posts to Looper.getMainLooper(); drain the looper to execute the post
        ShadowLooper.runMainLooperOneTask()

        // spec: TA-WV-07 — onOpenImport callback must have been invoked after looper idle
        verify(onOpenImport).invoke()
    }

    // spec: TA-WV-07 — openImport() posts to main looper: draining fully ensures callback fires
    @Test
    fun openImport_triggersCallback_afterLooperIdle() {
        val jsInterface = fr.datasaillance.nightfall.webview.NightfallJsInterface(
            context = context,
            onOpenImport = onOpenImport,
        )

        jsInterface.openImport()

        // Drain all pending tasks on the main looper
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        // spec: TA-WV-07 — onOpenImport must be called exactly once after looper drain
        verify(onOpenImport).invoke()
    }

    // spec: section "NightfallJsInterface.onNightfallEvent()" — ne crash pas avec un JSON valide
    @Test
    fun onNightfallEvent_validJson_doesNotThrow() {
        val jsInterface = fr.datasaillance.nightfall.webview.NightfallJsInterface(
            context = context,
            onOpenImport = onOpenImport,
        )

        // spec: onNightfallEvent is a Phase 4 no-op with Timber.d log only
        try {
            jsInterface.onNightfallEvent("""{"type":"test","payload":{}}""")
        } catch (e: Exception) {
            assert(false) {
                "onNightfallEvent must not throw with valid JSON — spec: NightfallJsInterface.onNightfallEvent, got: ${e.message}"
            }
        }
    }

    // spec: section "NightfallJsInterface.onNightfallEvent()" — ne crash pas avec une string vide
    @Test
    fun onNightfallEvent_emptyString_doesNotThrow() {
        val jsInterface = fr.datasaillance.nightfall.webview.NightfallJsInterface(
            context = context,
            onOpenImport = onOpenImport,
        )

        try {
            jsInterface.onNightfallEvent("")
        } catch (e: Exception) {
            assert(false) {
                "onNightfallEvent must not throw with empty string — spec: NightfallJsInterface.onNightfallEvent, got: ${e.message}"
            }
        }
    }

    // spec: section "NightfallJsInterface.onNightfallEvent()" — ne crash pas avec un JSON malformé
    @Test
    fun onNightfallEvent_malformedJson_doesNotThrow() {
        val jsInterface = fr.datasaillance.nightfall.webview.NightfallJsInterface(
            context = context,
            onOpenImport = onOpenImport,
        )

        try {
            jsInterface.onNightfallEvent("not-json")
        } catch (e: Exception) {
            assert(false) {
                "onNightfallEvent must not throw with malformed JSON — spec: NightfallJsInterface.onNightfallEvent, got: ${e.message}"
            }
        }
    }

    // spec: section "Surface @JavascriptInterface exhaustive" — 3 méthodes listées pour Phase 4
    // Verify that the class exposes exactly the expected @JavascriptInterface methods (contract surface audit).
    // Any undocumented addition is a security finding per pentester spec.
    @Test
    fun jsInterface_exposesExpectedPublicMethods() {
        val jsInterface = fr.datasaillance.nightfall.webview.NightfallJsInterface(
            context = context,
            onOpenImport = onOpenImport,
        )

        // Cast to Any to use the standard instance javaClass property without ambiguity
        val annotatedMethods = (jsInterface as Any).javaClass.declaredMethods
            .filter { method ->
                method.isAnnotationPresent(android.webkit.JavascriptInterface::class.java)
            }
            .map { it.name }
            .sorted()

        // spec: section "Surface @JavascriptInterface exhaustive" — exactly these 3 methods in Phase 4
        val expectedMethods = listOf("getAppVersion", "onNightfallEvent", "openImport").sorted()

        assert(annotatedMethods == expectedMethods) {
            "NightfallJsInterface must expose exactly 3 @JavascriptInterface methods in Phase 4 " +
                "— spec: section 'Surface @JavascriptInterface exhaustive'. " +
                "Expected: $expectedMethods, got: $annotatedMethods"
        }
    }
}
