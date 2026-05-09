---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepositoryImpl.kt
git_blob: 1d5121ead2f5371815083acc39d390e0ad0338e1
last_synced: '2026-05-09T04:03:35Z'
loc: 28
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepositoryImpl.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepositoryImpl.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepositoryImpl.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.sleep

import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.NightfallApi
import timber.log.Timber
import java.io.IOException

class SleepRepositoryImpl(
    private val api: NightfallApi,
    private val tokenDataStore: TokenDataStore
) : SleepRepository {

    override suspend fun getSessions(): Result<List<SleepSessionResponse>> {
        val token = tokenDataStore.getToken()
            ?: return Result.failure(IOException("No auth token"))
        return try {
            val sessions = api.getSleepSessions("Bearer $token")
            Timber.i("sleep_sessions_fetched count=${sessions.size}")
            Result.success(sessions)
        } catch (e: retrofit2.HttpException) {
            Timber.w("sleep_fetch_http_error code=${e.code()}")
            Result.failure(e)
        } catch (e: IOException) {
            Timber.w("sleep_fetch_network_error")
            Result.failure(e)
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepRepositoryImpl` (class) — lines 8-28
- `getSessions` (function) — lines 13-27
