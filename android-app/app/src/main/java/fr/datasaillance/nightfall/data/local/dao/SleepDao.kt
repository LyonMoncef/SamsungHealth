package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity

@Dao
interface SleepDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessions(sessions: List<SleepSessionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStages(stages: List<SleepStageEntity>): List<Long>

    @Query("SELECT * FROM sleep_sessions ORDER BY sleep_start ASC")
    suspend fun getAllSessions(): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_sessions WHERE sleep_start >= :fromMs AND sleep_start < :toMs ORDER BY sleep_start ASC")
    suspend fun getSessionsInRange(fromMs: Long, toMs: Long): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): SleepSessionEntity?

    @Query("SELECT * FROM sleep_stages WHERE session_id = :sessionId ORDER BY stage_start ASC")
    suspend fun getStagesForSession(sessionId: Long): List<SleepStageEntity>

    @Query("SELECT * FROM sleep_stages WHERE session_id IN (:sessionIds) ORDER BY stage_start ASC")
    suspend fun getStagesForSessions(sessionIds: List<Long>): List<SleepStageEntity>

    @Query("SELECT COUNT(*) FROM sleep_sessions")
    suspend fun countSessions(): Int

    @Query("SELECT COUNT(*) FROM sleep_stages")
    suspend fun countStages(): Int

    /**
     * Insert sessions + stages dans la même transaction. Le caller fournit pour
     * chaque stage un `sessionMatcher` qui retourne l'index de la session parente
     * dans la liste. Utilisé par l'import CSV qui résout les FK localement.
     */
    @Transaction
    suspend fun insertSessionsWithStages(
        sessions: List<SleepSessionEntity>,
        stagesBuilder: (insertedSessionIds: List<Long>) -> List<SleepStageEntity>,
    ): Pair<Int, Int> {
        val sessionResult = insertSessions(sessions)
        val stages = stagesBuilder(sessionResult)
        val stageResult = insertStages(stages)
        val insertedSessions = sessionResult.count { it != -1L }
        val insertedStages = stageResult.count { it != -1L }
        return insertedSessions to insertedStages
    }

    @Query("DELETE FROM sleep_sessions")
    suspend fun deleteAllSessions()
}
