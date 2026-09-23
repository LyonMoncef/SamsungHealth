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
