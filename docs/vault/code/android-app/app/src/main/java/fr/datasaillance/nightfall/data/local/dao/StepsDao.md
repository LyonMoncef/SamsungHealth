---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/StepsDao.kt
git_blob: dc61fc9772db305dfb1057d9ac167a700f403352
last_synced: '2026-05-09T15:08:38Z'
loc: 26
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/StepsDao.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/StepsDao.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/StepsDao.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.datasaillance.nightfall.data.local.entity.StepsHourlyEntity

@Dao
interface StepsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHourly(rows: List<StepsHourlyEntity>): List<Long>

    @Query("SELECT * FROM steps_hourly WHERE date BETWEEN :fromDate AND :toDate ORDER BY date, hour")
    suspend fun getInRange(fromDate: String, toDate: String): List<StepsHourlyEntity>

    @Query("SELECT * FROM steps_hourly ORDER BY date, hour")
    suspend fun getAll(): List<StepsHourlyEntity>

    @Query("SELECT COUNT(*) FROM steps_hourly")
    suspend fun count(): Int

    @Query("DELETE FROM steps_hourly")
    suspend fun deleteAll()
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `StepsDao` (class) — lines 9-26
- `insertHourly` (function) — lines 12-13
- `getInRange` (function) — lines 15-16
- `getAll` (function) — lines 18-19
- `count` (function) — lines 21-22
- `deleteAll` (function) — lines 24-25
