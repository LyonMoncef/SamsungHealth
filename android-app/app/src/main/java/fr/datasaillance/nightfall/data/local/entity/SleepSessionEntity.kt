package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Une session de sommeil locale. Miroir de `server.db.models.SleepSession`.
 *
 * Différences avec le serveur :
 * - pas de `user_id` (mono-utilisateur sur le device)
 * - timestamps en epoch millis UTC (vs DateTime tz-aware côté Postgres)
 * - score / efficiency / etc. en clair — chiffrement assuré au niveau du fichier DB par SQLCipher
 */
@Entity(
    tableName = "sleep_sessions",
    indices = [
        Index("sleep_start"),
        Index(value = ["sleep_start", "sleep_end"], unique = true),
    ],
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "sleep_start") val sleepStartMs: Long,
    @ColumnInfo(name = "sleep_end") val sleepEndMs: Long,
    @ColumnInfo(name = "sleep_score") val sleepScore: Int? = null,
    @ColumnInfo(name = "efficiency") val efficiency: Float? = null,
    @ColumnInfo(name = "sleep_duration_min") val sleepDurationMin: Int? = null,
    @ColumnInfo(name = "sleep_cycle") val sleepCycle: Int? = null,
    @ColumnInfo(name = "mental_recovery") val mentalRecovery: Float? = null,
    @ColumnInfo(name = "physical_recovery") val physicalRecovery: Float? = null,
    @ColumnInfo(name = "sleep_type") val sleepType: Int? = null,
    @ColumnInfo(name = "created_at") val createdAtMs: Long = System.currentTimeMillis(),
)
