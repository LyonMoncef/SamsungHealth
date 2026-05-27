---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/UsageSessionDao.kt
git_blob: aca746bc6dbba02ab6c8378f28d237622eed5bbb
last_synced: '2026-05-27T00:40:51Z'
loc: 30
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/UsageSessionDao.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/UsageSessionDao.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/UsageSessionDao.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.datasaillance.nightfall.data.local.entity.usage.UsageSessionEntity

@Dao
interface UsageSessionDao {

    /**
     * Upsert : un re-run sur la même fenêtre écrase la row existante (REPLACE
     * via index unique `(package_name, start_ms)`) — idempotence garantie.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sessions: List<UsageSessionEntity>): List<Long>

    @Query("SELECT * FROM usage_session WHERE start_ms >= :fromMs AND end_ms <= :toMs ORDER BY start_ms ASC")
    suspend fun getSessionsInRange(fromMs: Long, toMs: Long): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_session WHERE date = :date ORDER BY start_ms ASC")
    suspend fun getByDate(date: String): List<UsageSessionEntity>

    @Query("SELECT COUNT(*) FROM usage_session")
    suspend fun count(): Int

    @Query("DELETE FROM usage_session")
    suspend fun deleteAll()
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `UsageSessionDao` (class) — lines 9-30
- `upsert` (function) — lines 16-17
- `getSessionsInRange` (function) — lines 19-20
- `getByDate` (function) — lines 22-23
- `count` (function) — lines 25-26
- `deleteAll` (function) — lines 28-29
