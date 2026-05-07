---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/settings/SettingsDataStore.kt
git_blob: 34cced71e1d6351ff848ba7082b6abdce8dbf41b
last_synced: '2026-05-07T03:51:34Z'
loc: 54
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SettingsDataStore` (class) — lines 8-54
- `getBackendUrl` (function) — lines 35-36
- `setBackendUrl` (function) — lines 38-43
- `getThemePreference` (function) — lines 45-46
- `setThemePreference` (function) — lines 48-53
