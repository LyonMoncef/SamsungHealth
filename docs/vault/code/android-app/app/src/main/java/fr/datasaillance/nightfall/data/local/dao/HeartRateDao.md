---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/HeartRateDao.kt
git_blob: 42eeb1c8f671470b3d9396898b9fe7ec11666a2b
last_synced: '2026-05-09T15:08:38Z'
loc: 26
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/HeartRateDao.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/HeartRateDao.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/HeartRateDao.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.datasaillance.nightfall.data.local.entity.HeartRateHourlyEntity

@Dao
interface HeartRateDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHourly(rows: List<HeartRateHourlyEntity>): List<Long>

    @Query("SELECT * FROM heart_rate_hourly WHERE date BETWEEN :fromDate AND :toDate ORDER BY date, hour")
    suspend fun getInRange(fromDate: String, toDate: String): List<HeartRateHourlyEntity>

    @Query("SELECT * FROM heart_rate_hourly ORDER BY date, hour")
    suspend fun getAll(): List<HeartRateHourlyEntity>

    @Query("SELECT COUNT(*) FROM heart_rate_hourly")
    suspend fun count(): Int

    @Query("DELETE FROM heart_rate_hourly")
    suspend fun deleteAll()
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `HeartRateDao` (class) — lines 9-26
- `insertHourly` (function) — lines 12-13
- `getInRange` (function) — lines 15-16
- `getAll` (function) — lines 18-19
- `count` (function) — lines 21-22
- `deleteAll` (function) — lines 24-25
