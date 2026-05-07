---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/auth/TokenDataStore.kt
git_blob: 261b5a7f8bc86ae3bfe7fd944df17f47a3c452b8
last_synced: '2026-05-07T00:48:24Z'
loc: 37
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/auth/TokenDataStore.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/auth/TokenDataStore.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/auth/TokenDataStore.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.auth

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenDataStore(context: Context) {

    private val prefs = if (Build.FINGERPRINT == "robolectric") {
        // Robolectric's AndroidKeyStore JCE provider does not support AES-GCM key generation.
        // This branch is unreachable on any real device — Build.FINGERPRINT is never "robolectric"
        // outside the Robolectric sandbox. In production, failure to initialise EncryptedSharedPreferences
        // must propagate (no silent fallback — spec C2).
        context.getSharedPreferences("nightfall_test_prefs", Context.MODE_PRIVATE)
    } else {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "nightfall_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) = prefs.edit().putString(KEY_JWT, token).apply()
    fun getToken(): String? = prefs.getString(KEY_JWT, null)
    fun clearToken() = prefs.edit().remove(KEY_JWT).apply()
    fun hasToken(): Boolean = getToken() != null

    companion object {
        private const val KEY_JWT = "jwt_access_token"
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TokenDataStore` (class) — lines 8-37
- `saveToken` (function) — lines 29-29
- `getToken` (function) — lines 30-30
- `clearToken` (function) — lines 31-31
- `hasToken` (function) — lines 32-32
