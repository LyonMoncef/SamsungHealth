package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.datasaillance.nightfall.data.local.entity.ExerciseSessionEntity

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessions(rows: List<ExerciseSessionEntity>): List<Long>

    @Query("SELECT * FROM exercise_sessions WHERE start_time >= :fromMs AND start_time < :toMs ORDER BY start_time ASC")
    suspend fun getInRange(fromMs: Long, toMs: Long): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions ORDER BY start_time ASC")
    suspend fun getAll(): List<ExerciseSessionEntity>

    @Query("SELECT COUNT(*) FROM exercise_sessions")
    suspend fun count(): Int

    @Query("DELETE FROM exercise_sessions")
    suspend fun deleteAll()
}
