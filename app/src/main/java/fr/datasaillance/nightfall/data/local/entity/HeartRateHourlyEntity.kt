package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mesure HR agrégée à l'heure. Miroir de `server.db.models.HeartRateHourly`.
 * Clé unique (date, hour) — un seul enregistrement par heure.
 */
@Entity(
    tableName = "heart_rate_hourly",
    indices = [Index(value = ["date", "hour"], unique = true)],
)
data class HeartRateHourlyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String, // ISO yyyy-MM-dd
    val hour: Int,
    @ColumnInfo(name = "min_bpm") val minBpm: Int,
    @ColumnInfo(name = "max_bpm") val maxBpm: Int,
    @ColumnInfo(name = "avg_bpm") val avgBpm: Int,
    @ColumnInfo(name = "sample_count") val sampleCount: Int,
)
