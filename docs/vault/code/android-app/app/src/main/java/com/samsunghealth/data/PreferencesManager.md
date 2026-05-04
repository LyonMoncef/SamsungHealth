---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/com/samsunghealth/data/PreferencesManager.kt
git_blob: 75107c4129eec5e939ab6c27813432ae1c6b6f8d
last_synced: '2026-04-23T10:49:30Z'
loc: 38
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/com/samsunghealth/data/PreferencesManager.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/com/samsunghealth/data/PreferencesManager.kt`](../../../android-app/app/src/main/java/com/samsunghealth/data/PreferencesManager.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
        const val DEFAULT_URL = "http://10.0.2.2:8001"
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `PreferencesManager` (class) — lines 15-38
- `setBackendUrl` (function) — lines 31-33
- `setLastSync` (function) — lines 35-37
