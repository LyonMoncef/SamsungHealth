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
