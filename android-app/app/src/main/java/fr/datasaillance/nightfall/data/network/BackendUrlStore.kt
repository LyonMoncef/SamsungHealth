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
