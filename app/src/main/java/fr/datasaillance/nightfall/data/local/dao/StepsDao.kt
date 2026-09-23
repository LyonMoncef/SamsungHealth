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
