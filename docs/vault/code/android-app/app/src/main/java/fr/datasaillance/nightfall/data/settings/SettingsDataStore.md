---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/settings/SettingsDataStore.kt
git_blob: df60a9f0c0c5f27525716cbf093b425e6af03507
last_synced: '2026-05-09T03:55:38Z'
loc: 67
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/settings/SettingsDataStore.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/settings/SettingsDataStore.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/settings/SettingsDataStore.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.settings

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import fr.datasaillance.nightfall.BuildConfig

class SettingsDataStore(context: Context) {

    companion object {
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_THEME_PREF  = "theme_preference"
        private val DEFAULT_BACKEND = BuildConfig.DEFAULT_BACKEND_URL
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
        try {
            EncryptedSharedPreferences.create(
                context,
                "nightfall_settings_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.deleteSharedPreferences("nightfall_settings_prefs")
            val freshKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                context,
                "nightfall_settings_prefs",
                freshKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SettingsDataStore` (class) — lines 9-67
- `getBackendUrl` (function) — lines 48-49
- `setBackendUrl` (function) — lines 51-56
- `getThemePreference` (function) — lines 58-59
- `setThemePreference` (function) — lines 61-66
