---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/LocationDao.kt
git_blob: cb4c67598757cc9b39e6e84892071dc8ab7763ca
last_synced: '2026-05-20T16:30:46Z'
loc: 69
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/LocationDao.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/LocationDao.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/LocationDao.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.datasaillance.nightfall.data.local.entity.location.ActivitySegmentEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationPathEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVisits(rows: List<LocationVisitEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSegments(rows: List<ActivitySegmentEntity>): List<Long>

    @Query("SELECT * FROM location_visits ORDER BY start_ms ASC")
    suspend fun getAllVisits(): List<LocationVisitEntity>

    @Query("SELECT * FROM location_visits WHERE start_ms >= :fromMs AND start_ms < :toMs ORDER BY start_ms ASC")
    suspend fun getVisitsInRange(fromMs: Long, toMs: Long): List<LocationVisitEntity>

    @Query("SELECT * FROM activity_segments ORDER BY start_ms ASC")
    suspend fun getAllSegments(): List<ActivitySegmentEntity>

    @Query("SELECT * FROM activity_segments WHERE start_ms >= :fromMs AND start_ms < :toMs ORDER BY start_ms ASC")
    suspend fun getSegmentsInRange(fromMs: Long, toMs: Long): List<ActivitySegmentEntity>

    @Query("SELECT activity_type, COUNT(*) as cnt FROM activity_segments GROUP BY activity_type ORDER BY cnt DESC")
    suspend fun getActivityTypeBreakdown(): List<ActivityTypeCount>

    /** Pour le badge "sorti du domicile" — retourne tous les start_ms d'activities dans la plage. */
    @Query("SELECT DISTINCT start_ms FROM activity_segments WHERE start_ms >= :fromMs AND start_ms < :toMs")
    suspend fun getActivityStartTimesInRange(fromMs: Long, toMs: Long): List<Long>

    @Query("SELECT COUNT(*) FROM location_visits")
    suspend fun countVisits(): Int

    @Query("SELECT COUNT(*) FROM activity_segments")
    suspend fun countSegments(): Int

    @Query("DELETE FROM location_visits")
    suspend fun deleteAllVisits()

    @Query("DELETE FROM activity_segments")
    suspend fun deleteAllSegments()

    // --- Paths GPS (timelinePath du nouveau format Google) ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPaths(rows: List<LocationPathEntity>): List<Long>

    @Query("SELECT * FROM location_paths WHERE start_ms >= :fromMs AND start_ms < :toMs ORDER BY start_ms ASC")
    suspend fun getPathsInRange(fromMs: Long, toMs: Long): List<LocationPathEntity>

    @Query("SELECT COUNT(*) FROM location_paths")
    suspend fun countPaths(): Int

    @Query("DELETE FROM location_paths")
    suspend fun deleteAllPaths()
}

data class ActivityTypeCount(
    @androidx.room.ColumnInfo(name = "activity_type") val activityType: String,
    val cnt: Int,
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LocationDao` (class) — lines 11-64
- `insertVisits` (function) — lines 14-15
- `insertSegments` (function) — lines 17-18
- `getAllVisits` (function) — lines 20-21
- `getVisitsInRange` (function) — lines 23-24
- `getAllSegments` (function) — lines 26-27
- `getSegmentsInRange` (function) — lines 29-30
- `getActivityTypeBreakdown` (function) — lines 32-33
- `getActivityStartTimesInRange` (function) — lines 36-37
- `countVisits` (function) — lines 39-40
- `countSegments` (function) — lines 42-43
- `deleteAllVisits` (function) — lines 45-46
- `deleteAllSegments` (function) — lines 48-49
- `insertPaths` (function) — lines 53-54
- `getPathsInRange` (function) — lines 56-57
- `countPaths` (function) — lines 59-60
- `deleteAllPaths` (function) — lines 62-63
- `ActivityTypeCount` (class) — lines 66-69
