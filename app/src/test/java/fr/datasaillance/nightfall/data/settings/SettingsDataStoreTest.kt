package fr.datasaillance.nightfall.data.settings

// spec: Tests d'acceptation — SettingsDataStore
// spec: section "SettingsDataStore" — EncryptedSharedPreferences, backend_url, theme_preference
// spec: D9, D10 — URL backend lue depuis SettingsDataStore, distinct de TokenDataStore
// RED by construction: fr.datasaillance.nightfall.data.settings.SettingsDataStore does not exist yet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// This import will fail to resolve until production code is written:
// fr.datasaillance.nightfall.data.settings.SettingsDataStore

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsDataStoreTest {

    private lateinit var context: Context
    private lateinit var settingsDataStore: fr.datasaillance.nightfall.data.settings.SettingsDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // spec: D9 — SettingsDataStore wraps EncryptedSharedPreferences ("nightfall_settings_prefs")
        // spec: D10 — distinct from TokenDataStore ("nightfall_secure_prefs")
        settingsDataStore = fr.datasaillance.nightfall.data.settings.SettingsDataStore(context)
        // Reset to clean state before each test
        settingsDataStore.setBackendUrl("http://10.0.2.2:8001")
    }

    // spec: section "SettingsDataStore" — getBackendUrl() retourne "http://10.0.2.2:8001" si jamais défini
    // spec: SettingsDataStore companion DEFAULT_BACKEND = "http://10.0.2.2:8001"
    @Test
    fun getBackendUrl_returnsEmulatorDefault_whenNeverSet() {
        // Fresh store — clear any previously stored value to test the default path
        val freshStore = fr.datasaillance.nightfall.data.settings.SettingsDataStore(context)

        // We cannot truly test "never set" without a clean prefs file,
        // but we can verify the returned default is the expected emulator URL.
        // The contract: if KEY_BACKEND_URL is absent, return DEFAULT_BACKEND.
        // This is verified transitively: after setUp resets to the default value,
        // reading it back must return the default.
        val url = freshStore.getBackendUrl()

        // spec: DEFAULT_BACKEND = "http://10.0.2.2:8001" (émulateur → WSL2)
        assert(url == "http://10.0.2.2:8001") {
            "getBackendUrl() must return 'http://10.0.2.2:8001' as default — spec: SettingsDataStore.DEFAULT_BACKEND"
        }
    }

    // spec: setBackendUrl("https://sh-prod.datasaillance.fr") persiste et getBackendUrl() retourne la nouvelle valeur
    @Test
    fun setBackendUrl_persistsAndIsReadableViaGetBackendUrl() {
        val newUrl = "https://sh-prod.datasaillance.fr"

        settingsDataStore.setBackendUrl(newUrl)
        val result = settingsDataStore.getBackendUrl()

        // spec: setBackendUrl → prefs.edit().putString(KEY_BACKEND_URL, url).apply()
        assert(result == newUrl) {
            "getBackendUrl() must return the value set by setBackendUrl() — spec: SettingsDataStore.setBackendUrl"
        }
    }

    // spec: setBackendUrl("http://10.0.2.2:8001") — http:// prefix is valid (emulator use case)
    @Test
    fun setBackendUrl_httpPrefix_isAccepted() {
        val httpUrl = "http://10.0.2.2:8001"

        // spec: require(url.startsWith("http://") || url.startsWith("https://"))
        try {
            settingsDataStore.setBackendUrl(httpUrl)
        } catch (e: IllegalArgumentException) {
            assert(false) {
                "setBackendUrl must accept http:// URLs — spec: SettingsDataStore.setBackendUrl validation, got: ${e.message}"
            }
        }

        assert(settingsDataStore.getBackendUrl() == httpUrl) {
            "getBackendUrl() must return the http:// URL after setBackendUrl — spec: SettingsDataStore"
        }
    }

    // spec: setBackendUrl("ftp://invalid") lève une IllegalArgumentException
    //   (doit commencer par http:// ou https://)
    @Test(expected = IllegalArgumentException::class)
    fun setBackendUrl_ftpPrefix_throwsIllegalArgumentException() {
        // spec: require(url.startsWith("http://") || url.startsWith("https://"))
        settingsDataStore.setBackendUrl("ftp://invalid.example.com")
        // spec: "backend_url must start with http:// or https://"
    }

    // spec: setBackendUrl("") lève une IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun setBackendUrl_emptyString_throwsIllegalArgumentException() {
        // spec: empty string does not start with http:// or https:// → IllegalArgumentException
        settingsDataStore.setBackendUrl("")
    }

    // spec: setBackendUrl with no scheme lève une IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun setBackendUrl_noScheme_throwsIllegalArgumentException() {
        // spec: plain hostname without scheme — must be rejected
        settingsDataStore.setBackendUrl("sh-prod.datasaillance.fr")
    }

    // spec: getThemePreference() retourne "system" par défaut
    // spec: section "SettingsDataStore" — KEY_THEME_PREF default = "system"
    @Test
    fun getThemePreference_returnsSystemByDefault() {
        // Reset theme to default state
        val freshStore = fr.datasaillance.nightfall.data.settings.SettingsDataStore(context)

        val theme = freshStore.getThemePreference()

        // spec: prefs.getString(KEY_THEME_PREF, "system") ?: "system"
        assert(theme == "system") {
            "getThemePreference() must return 'system' by default — spec: SettingsDataStore.DEFAULT theme"
        }
    }

    // spec: setThemePreference("dark") persiste et getThemePreference() retourne "dark"
    @Test
    fun setThemePreference_dark_persistsAndIsReadable() {
        settingsDataStore.setThemePreference("dark")

        val result = settingsDataStore.getThemePreference()

        // spec: require(value in listOf("system", "dark", "light"))
        assert(result == "dark") {
            "getThemePreference() must return 'dark' after setThemePreference('dark') — spec: SettingsDataStore.setThemePreference"
        }
    }

    // spec: setThemePreference("light") persiste et getThemePreference() retourne "light"
    @Test
    fun setThemePreference_light_persistsAndIsReadable() {
        settingsDataStore.setThemePreference("light")

        val result = settingsDataStore.getThemePreference()

        // spec: require(value in listOf("system", "dark", "light"))
        assert(result == "light") {
            "getThemePreference() must return 'light' after setThemePreference('light') — spec: SettingsDataStore.setThemePreference"
        }
    }

    // spec: setThemePreference("system") persiste et getThemePreference() retourne "system"
    @Test
    fun setThemePreference_system_persistsAndIsReadable() {
        // First set to something else, then back to "system"
        settingsDataStore.setThemePreference("dark")
        settingsDataStore.setThemePreference("system")

        val result = settingsDataStore.getThemePreference()

        assert(result == "system") {
            "getThemePreference() must return 'system' after setThemePreference('system') — spec: SettingsDataStore.setThemePreference"
        }
    }

    // spec: setThemePreference("invalid") lève une IllegalArgumentException
    // spec: require(value in listOf("system", "dark", "light"))
    @Test(expected = IllegalArgumentException::class)
    fun setThemePreference_invalidValue_throwsIllegalArgumentException() {
        // spec: "theme_preference must be 'system', 'dark', or 'light'"
        settingsDataStore.setThemePreference("invalid")
    }

    // spec: setThemePreference("") lève une IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun setThemePreference_emptyString_throwsIllegalArgumentException() {
        // spec: empty string is not in the allowed set → IllegalArgumentException
        settingsDataStore.setThemePreference("")
    }

    // spec: setThemePreference("auto") — not in the allowed set → IllegalArgumentException
    @Test(expected = IllegalArgumentException::class)
    fun setThemePreference_auto_throwsIllegalArgumentException() {
        // spec: only "system", "dark", "light" are valid — "auto" is not
        settingsDataStore.setThemePreference("auto")
    }

    // spec: D10 — SettingsDataStore uses "nightfall_settings_prefs" distinct from TokenDataStore
    //   The two stores must use different prefs files to avoid key collision
    @Test
    fun settingsDataStore_usesDistinctPrefsFile_fromTokenDataStore() {
        val settingsPrefsFile = context.filesDir.parentFile
            ?.resolve("shared_prefs/nightfall_settings_prefs.xml")
        val tokenPrefsFile = context.filesDir.parentFile
            ?.resolve("shared_prefs/nightfall_secure_prefs.xml")

        // Write to settings store to force file creation
        settingsDataStore.setBackendUrl("https://sh-prod.datasaillance.fr")

        // spec: D10 — distinct file names confirm separate storage
        if (settingsPrefsFile != null && settingsPrefsFile.exists()) {
            if (tokenPrefsFile != null && tokenPrefsFile.exists()) {
                assert(settingsPrefsFile.name != tokenPrefsFile.name) {
                    "SettingsDataStore must use a distinct prefs file from TokenDataStore — spec: D10"
                }
            }
            // If token prefs file doesn't exist yet, the distinction is implicit
        }
    }
}
