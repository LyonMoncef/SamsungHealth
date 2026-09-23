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
