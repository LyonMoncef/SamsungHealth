---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/LocationDao.kt
git_blob: c2ec0aec659239b1d3ceeb156fdf4f1c69b64eeb
last_synced: '2026-05-09T19:12:26Z'
loc: 50
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

    @Query("SELECT COUNT(*) FROM location_visits")
    suspend fun countVisits(): Int

    @Query("SELECT COUNT(*) FROM activity_segments")
    suspend fun countSegments(): Int

    @Query("DELETE FROM location_visits")
    suspend fun deleteAllVisits()

    @Query("DELETE FROM activity_segments")
    suspend fun deleteAllSegments()
}

data class ActivityTypeCount(
    @androidx.room.ColumnInfo(name = "activity_type") val activityType: String,
    val cnt: Int,
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LocationDao` (class) — lines 10-45
- `insertVisits` (function) — lines 13-14
- `insertSegments` (function) — lines 16-17
- `getAllVisits` (function) — lines 19-20
- `getVisitsInRange` (function) — lines 22-23
- `getAllSegments` (function) — lines 25-26
- `getSegmentsInRange` (function) — lines 28-29
- `getActivityTypeBreakdown` (function) — lines 31-32
- `countVisits` (function) — lines 34-35
- `countSegments` (function) — lines 37-38
- `deleteAllVisits` (function) — lines 40-41
- `deleteAllSegments` (function) — lines 43-44
- `ActivityTypeCount` (class) — lines 47-50
