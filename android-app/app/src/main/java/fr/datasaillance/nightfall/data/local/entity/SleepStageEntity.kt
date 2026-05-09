package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_stages",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("session_id"),
        Index(value = ["stage_start", "stage_end"], unique = true),
    ],
)
data class SleepStageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "stage_type") val stageType: String, // DEEP / LIGHT / REM / AWAKE
    @ColumnInfo(name = "stage_start") val stageStartMs: Long,
    @ColumnInfo(name = "stage_end") val stageEndMs: Long,
)
