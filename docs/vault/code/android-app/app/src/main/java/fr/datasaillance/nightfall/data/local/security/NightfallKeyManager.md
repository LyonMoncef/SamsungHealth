---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/security/NightfallKeyManager.kt
git_blob: f334a67d8d9e7bfa42d760143a1361cc89137d52
last_synced: '2026-05-09T15:08:38Z'
loc: 73
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/security/NightfallKeyManager.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/security/NightfallKeyManager.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/security/NightfallKeyManager.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Gestion de la clé/passphrase SQLCipher.
 *
 * Stratégie :
 * 1. À la première utilisation, on génère 32 octets aléatoires (passphrase).
 * 2. Cette passphrase est persistée dans `EncryptedSharedPreferences` (chiffrée
 *    par une clé maître Android Keystore — hardware-backed sur API 28+).
 * 3. Aux ouvertures suivantes, on lit la passphrase depuis EncryptedSharedPreferences.
 *
 * Pourquoi ne pas dériver directement la passphrase d'une clé Keystore ?
 * SQLCipher requiert un `byte[]` opaque côté SupportFactory ; les clés Keystore
 * AES ne peuvent pas être exportées en clair (par design). On utilise donc le
 * Keystore comme racine de confiance pour chiffrer un secret stocké côté disque,
 * pas pour dériver le secret en runtime.
 *
 * Implication : un effacement du Keystore (factory reset, migration ROM) rend la
 * DB illisible. Acceptable pour Phase A — la spec local-first prévoit un
 * mécanisme de backup utilisateur ultérieur.
 */
class NightfallKeyManager(private val context: Context) {

    fun getOrCreatePassphrase(): ByteArray {
        val prefs = encryptedPrefs()
        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            return android.util.Base64.decode(existing, android.util.Base64.NO_WRAP)
        }
        val passphrase = ByteArray(PASSPHRASE_SIZE_BYTES)
        SecureRandom().nextBytes(passphrase)
        val encoded = android.util.Base64.encodeToString(passphrase, android.util.Base64.NO_WRAP)
        prefs.edit().putString(KEY_PASSPHRASE, encoded).apply()
        return passphrase
    }

    private fun encryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyGenParameterSpec(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    companion object {
        private const val PREFS_NAME = "nightfall_db_secrets"
        private const val MASTER_KEY_ALIAS = "nightfall_db_master_key"
        private const val KEY_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_SIZE_BYTES = 32
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallKeyManager` (class) — lines 30-73
- `getOrCreatePassphrase` (function) — lines 32-43
- `encryptedPrefs` (function) — lines 45-65
