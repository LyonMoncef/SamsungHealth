---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/network/BackendUrlStore.kt
git_blob: 69d003d8f9856c6ccabb02dcf9f6061bdc6d42b9
last_synced: '2026-05-07T00:48:24Z'
loc: 29
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

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "nightfall_backend_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

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
- `BackendUrlStore` (class) — lines 8-29
- `saveUrl` (function) — lines 22-22
- `getUrl` (function) — lines 24-24
