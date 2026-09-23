package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Steps agrégés à l'heure. Miroir de `server.db.models.StepsHourly`.
 */
@Entity(
    tableName = "steps_hourly",
    indices = [Index(value = ["date", "hour"], unique = true)],
)
data class StepsHourlyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String, // ISO yyyy-MM-dd
    val hour: Int,
    @ColumnInfo(name = "step_count") val stepCount: Int,
)
