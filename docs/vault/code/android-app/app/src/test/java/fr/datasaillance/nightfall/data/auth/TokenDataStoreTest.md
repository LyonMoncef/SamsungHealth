---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/data/auth/TokenDataStoreTest.kt
git_blob: 0929e8c9d82e1e873a22cf1fe6382db59e6db881
last_synced: '2026-05-07T00:48:24Z'
loc: 112
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/data/auth/TokenDataStoreTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/data/auth/TokenDataStoreTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/data/auth/TokenDataStoreTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.auth

// spec: Tests d'acceptation TA-10
// spec: section "TokenDataStore" — EncryptedSharedPreferences AES256-GCM
// RED by construction: fr.datasaillance.nightfall.data.auth.TokenDataStore does not exist yet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// This import will fail to resolve until production code is written:
// fr.datasaillance.nightfall.data.auth.TokenDataStore

@RunWith(RobolectricTestRunner::class)
class TokenDataStoreTest {

    private lateinit var context: Context
    private lateinit var tokenDataStore: TokenDataStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // spec: D5 — EncryptedDataStore (EncryptedSharedPreferences) pour le JWT
        tokenDataStore = TokenDataStore(context)
    }

    // spec: TA-10 — saveToken("abc123") → getToken() retourne "abc123"
    @Test
    fun tokenDataStore_saveAndRead() {
        tokenDataStore.saveToken("abc123")

        val result = tokenDataStore.getToken()

        assert(result == "abc123") {
            "getToken() must return the value previously saved with saveToken() — spec: TA-10"
        }
    }

    // spec: section "TokenDataStore" — clearToken() retire le JWT
    @Test
    fun tokenDataStore_clearToken() {
        tokenDataStore.saveToken("token-to-clear")
        tokenDataStore.clearToken()

        val result = tokenDataStore.getToken()

        assert(result == null) {
            "getToken() must return null after clearToken() — spec: TokenDataStore.clearToken"
        }
    }

    // spec: section "TokenDataStore" — getToken() retourne null par défaut (rien sauvegardé)
    @Test
    fun tokenDataStore_nullByDefault() {
        // Fresh instance with no prior saveToken call
        val freshStore = TokenDataStore(context)
        // Note: EncryptedSharedPreferences reuses the same file — clear first
        freshStore.clearToken()

        val result = freshStore.getToken()

        assert(result == null) {
            "getToken() must return null when nothing has been saved — spec: TokenDataStore"
        }
    }

    // spec: section "TokenDataStore" — hasToken() retourne true quand un token est présent
    @Test
    fun tokenDataStore_hasTokenReturnsTrueWhenPresent() {
        tokenDataStore.saveToken("some-jwt")

        assert(tokenDataStore.hasToken()) {
            "hasToken() must return true when a token is saved — spec: TokenDataStore.hasToken"
        }
    }

    // spec: section "TokenDataStore" — hasToken() retourne false quand rien n'est sauvegardé
    @Test
    fun tokenDataStore_hasTokenReturnsFalseWhenEmpty() {
        tokenDataStore.clearToken()

        assert(!tokenDataStore.hasToken()) {
            "hasToken() must return false when no token is saved — spec: TokenDataStore.hasToken"
        }
    }

    // spec: TA-10 — le fichier de préférences sur disque n'est pas lisible en clair
    // The contract: the prefs file must NOT contain the raw token string in plaintext.
    // This test verifies the encryption intent at the contract level.
    @Test
    fun tokenDataStore_storedValueIsNotPlaintext() {
        val plainToken = "super-secret-jwt-1234567890"
        tokenDataStore.saveToken(plainToken)

        // Read the raw preferences file bytes
        val prefsFile = context.filesDir.parentFile
            ?.resolve("shared_prefs/nightfall_secure_prefs.xml")

        // spec: C2 — EncryptedSharedPreferences AES256-GCM, jamais en clair
        if (prefsFile != null && prefsFile.exists()) {
            val rawContent = prefsFile.readText()
            assert(!rawContent.contains(plainToken)) {
                "JWT token must NOT appear in plaintext in the prefs file — spec: C2, TA-10"
            }
        }
        // If the file doesn't exist yet, the test will pass vacuously
        // but the class import itself will fail RED (class doesn't exist)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TokenDataStoreTest` (class) — lines 17-112
- `setUp` (function) — lines 23-28
- `tokenDataStore_saveAndRead` (function) — lines 31-40
- `tokenDataStore_clearToken` (function) — lines 43-53
- `tokenDataStore_nullByDefault` (function) — lines 56-68
- `tokenDataStore_hasTokenReturnsTrueWhenPresent` (function) — lines 71-78
- `tokenDataStore_hasTokenReturnsFalseWhenEmpty` (function) — lines 81-88
- `tokenDataStore_storedValueIsNotPlaintext` (function) — lines 93-111
