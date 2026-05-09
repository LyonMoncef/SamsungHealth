---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/network/BackendUrlStore.kt
git_blob: f53334f2e5dd422d62f5b7e2a171891686da0170
last_synced: '2026-05-09T03:55:38Z'
loc: 41
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/network/BackendUrlStore.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/network/BackendUrlStore.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/network/BackendUrlStore.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import fr.datasaillance.nightfall.BuildConfig

class BackendUrlStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = try {
        EncryptedSharedPreferences.create(
            context,
            "nightfall_backend_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.deleteSharedPreferences("nightfall_backend_prefs")
        val freshKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            "nightfall_backend_prefs",
            freshKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveUrl(url: String) = prefs.edit().putString(KEY_URL, url).apply()

    fun getUrl(): String = prefs.getString(KEY_URL, null) ?: BuildConfig.DEFAULT_BACKEND_URL

    companion object {
        private const val KEY_URL = "backend_url"
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `BackendUrlStore` (class) — lines 8-41
- `saveUrl` (function) — lines 34-34
- `getUrl` (function) — lines 36-36
