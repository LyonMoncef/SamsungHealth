---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/UsageStatsDao.kt
git_blob: d0bcfae7e0cf96150ee866fed2e90eacd6bfd87e
last_synced: '2026-05-09T18:49:36Z'
loc: 36
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/UsageStatsDao.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/UsageStatsDao.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/UsageStatsDao.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.datasaillance.nightfall.data.local.entity.usage.UsageDailyEntity

@Dao
interface UsageStatsDao {

    /**
     * Upsert : Android peut affiner ses compteurs après-coup, on remplace donc
     * la ligne existante (REPLACE strategy via index unique `(date, package_name)`).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDaily(rows: List<UsageDailyEntity>): List<Long>

    @Query("SELECT * FROM usage_daily WHERE date = :date ORDER BY total_time_foreground_ms DESC")
    suspend fun getByDate(date: String): List<UsageDailyEntity>

    @Query("SELECT * FROM usage_daily WHERE date BETWEEN :fromDate AND :toDate ORDER BY date ASC, total_time_foreground_ms DESC")
    suspend fun getInRange(fromDate: String, toDate: String): List<UsageDailyEntity>

    @Query("SELECT * FROM usage_daily WHERE package_name = :packageName ORDER BY date DESC")
    suspend fun getByPackage(packageName: String): List<UsageDailyEntity>

    @Query("SELECT DISTINCT date FROM usage_daily ORDER BY date DESC")
    suspend fun getCollectedDates(): List<String>

    @Query("SELECT COUNT(*) FROM usage_daily")
    suspend fun count(): Int

    @Query("DELETE FROM usage_daily")
    suspend fun deleteAll()
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `UsageStatsDao` (class) — lines 9-36
- `upsertDaily` (function) — lines 16-17
- `getByDate` (function) — lines 19-20
- `getInRange` (function) — lines 22-23
- `getByPackage` (function) — lines 25-26
- `getCollectedDates` (function) — lines 28-29
- `count` (function) — lines 31-32
- `deleteAll` (function) — lines 34-35
