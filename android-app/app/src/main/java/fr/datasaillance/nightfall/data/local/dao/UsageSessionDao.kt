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
