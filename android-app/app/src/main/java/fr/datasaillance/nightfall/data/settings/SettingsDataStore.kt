package fr.datasaillance.nightfall.data.settings

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsDataStore(context: Context) {

    companion object {
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_THEME_PREF  = "theme_preference"
        private const val DEFAULT_BACKEND = "http://10.0.2.2:8001"
    }

    private val prefs = if (Build.FINGERPRINT == "robolectric") {
        // Robolectric's AndroidKeyStore JCE provider does not support AES-GCM key generation.
        // This branch is unreachable on any real device — Build.FINGERPRINT is never "robolectric"
        // outside the Robolectric sandbox. In production, failure to initialise EncryptedSharedPreferences
        // must propagate (no silent fallback — spec C2).
        context.getSharedPreferences("nightfall_test_settings_prefs", Context.MODE_PRIVATE)
    } else {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "nightfall_settings_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getBackendUrl(): String =
        prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND) ?: DEFAULT_BACKEND

    fun setBackendUrl(url: String) {
        require(url.startsWith("http://") || url.startsWith("https://")) {
            "backend_url must start with http:// or https://"
        }
        prefs.edit().putString(KEY_BACKEND_URL, url).apply()
    }

    fun getThemePreference(): String =
        prefs.getString(KEY_THEME_PREF, "system") ?: "system"

    fun setThemePreference(value: String) {
        require(value in listOf("system", "dark", "light")) {
            "theme_preference must be 'system', 'dark', or 'light'"
        }
        prefs.edit().putString(KEY_THEME_PREF, value).apply()
    }
}
