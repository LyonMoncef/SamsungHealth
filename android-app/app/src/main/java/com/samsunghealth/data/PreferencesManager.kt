package com.samsunghealth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        private val KEY_LAST_SYNC = longPreferencesKey("last_sync_epoch_millis")
        const val DEFAULT_URL = "http://10.0.2.2:8000"
    }

    val backendUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BACKEND_URL] ?: DEFAULT_URL
    }

    val lastSyncMillis: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC] ?: 0L
    }

    suspend fun setBackendUrl(url: String) {
        context.dataStore.edit { it[KEY_BACKEND_URL] = url }
    }

    suspend fun setLastSync(epochMillis: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC] = epochMillis }
    }
}
