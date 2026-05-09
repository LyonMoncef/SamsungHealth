---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/auth/TokenDataStore.kt
git_blob: e0a2b76583c5f432a76592154d3fc826f401d7e6
last_synced: '2026-05-09T03:55:38Z'
loc: 49
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
        openEncryptedPrefs(context, "nightfall_secure_prefs")
    }

    fun saveToken(token: String) = prefs.edit().putString(KEY_JWT, token).apply()
    fun getToken(): String? = prefs.getString(KEY_JWT, null)
    fun clearToken() = prefs.edit().remove(KEY_JWT).apply()
    fun hasToken(): Boolean = getToken() != null

    companion object {
        private const val KEY_JWT = "jwt_access_token"
    }
}

private fun openEncryptedPrefs(
    context: Context,
    name: String,
): android.content.SharedPreferences {
    fun buildMasterKey() = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    fun create(key: MasterKey) = EncryptedSharedPreferences.create(
        context, name, key,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    return try {
        create(buildMasterKey())
    } catch (e: Exception) {
        // AEADBadTagException on reinstall: AndroidKeyStore key invalidated → wipe stale prefs and start fresh
        context.deleteSharedPreferences(name)
        create(buildMasterKey())
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TokenDataStore` (class) — lines 8-28
- `saveToken` (function) — lines 20-20
- `getToken` (function) — lines 21-21
- `clearToken` (function) — lines 22-22
- `hasToken` (function) — lines 23-23
- `openEncryptedPrefs` (function) — lines 30-49
- `buildMasterKey` (function) — lines 34-36
- `create` (function) — lines 37-41
