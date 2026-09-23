package fr.datasaillance.nightfall.data.local.entity.usage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stats d'usage agrégées par jour, par package. Une row = (date, package).
 * Source : `UsageStatsManager.queryUsageStats(INTERVAL_DAILY, ...)`.
 *
 * `date` au format ISO `yyyy-MM-dd` (jour calendaire local), pour s'aligner sur
 * la sémantique Android UsageStatsManager qui agrège par fuseau du device.
 */
@Entity(
    tableName = "usage_daily",
    indices = [
        Index(value = ["date", "package_name"], unique = true),
        Index("date"),
        Index("package_name"),
    ],
)
data class UsageDailyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val date: String,                                                  // yyyy-MM-dd
    @ColumnInfo(name = "package_name") val packageName: String,

    @ColumnInfo(name = "total_time_foreground_ms") val totalTimeForegroundMs: Long,
    @ColumnInfo(name = "total_time_visible_ms") val totalTimeVisibleMs: Long = 0L,
    @ColumnInfo(name = "total_time_fgs_ms") val totalTimeForegroundServiceMs: Long = 0L,

    @ColumnInfo(name = "last_time_used_ms") val lastTimeUsedMs: Long = 0L,
    @ColumnInfo(name = "app_launch_count") val appLaunchCount: Int = 0,

    @ColumnInfo(name = "collected_at_ms") val collectedAtMs: Long = System.currentTimeMillis(),
)
