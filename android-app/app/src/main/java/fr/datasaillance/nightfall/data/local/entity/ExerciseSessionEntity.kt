package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_sessions",
    indices = [Index(value = ["start_time", "end_time"], unique = true)],
)
data class ExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "start_time") val startTimeMs: Long,
    @ColumnInfo(name = "end_time") val endTimeMs: Long,
    @ColumnInfo(name = "exercise_type") val exerciseType: String,
    @ColumnInfo(name = "duration_min") val durationMin: Int? = null,
    @ColumnInfo(name = "calorie") val calorie: Float? = null,
    @ColumnInfo(name = "mean_heart_rate") val meanHeartRate: Int? = null,
)
