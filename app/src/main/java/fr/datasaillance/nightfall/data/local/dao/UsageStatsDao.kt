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
