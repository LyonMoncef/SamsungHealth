package fr.datasaillance.nightfall.data.local.entity.usage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Session foreground reconstruite à partir des events bruts de
 * `UsageStatsManager.queryEvents()`. Une row = (package, startMs..endMs).
 *
 * Complète `usage_daily` : là où l'agrégat quotidien donne un total foreground,
 * la session donne la distribution horaire réelle (quand, combien de fois).
 *
 * `date` au format ISO `yyyy-MM-dd` (jour calendaire local, dérivé de `startMs`
 * dans le fuseau du device) pour s'aligner sur la convention de `usage_daily`.
 *
 * L'index unique sur `(package_name, start_ms)` garantit l'idempotence : un
 * re-run sur la même fenêtre utilise `OnConflictStrategy.REPLACE` et écrase la
 * row (la session peut être allongée si l'event de fermeture tarde).
 */
@Entity(
    tableName = "usage_session",
    indices = [
        Index(value = ["package_name", "start_ms"], unique = true),
        Index("date"),
        Index("start_ms"),
        Index("package_name"),
    ],
)
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "end_ms") val endMs: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,

    val date: String, // yyyy-MM-dd (zone device, dérivé de startMs)

    @ColumnInfo(name = "source") val source: String = "events",
    @ColumnInfo(name = "collected_at_ms") val collectedAtMs: Long,
)
