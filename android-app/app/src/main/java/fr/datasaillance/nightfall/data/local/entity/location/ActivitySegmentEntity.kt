package fr.datasaillance.nightfall.data.local.entity.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Segment d'activité : Google Maps Timeline `activitySegment`. Trajet entre 2 visites
 * (marche, voiture, vélo, transport en commun, etc.).
 */
@Entity(
    tableName = "activity_segments",
    indices = [
        Index(value = ["start_ms", "end_ms", "activity_type"], unique = true),
        Index("start_ms"),
        Index("activity_type"),
    ],
)
data class ActivitySegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "end_ms") val endMs: Long,

    @ColumnInfo(name = "start_lat") val startLat: Double,
    @ColumnInfo(name = "start_lng") val startLng: Double,
    @ColumnInfo(name = "end_lat") val endLat: Double,
    @ColumnInfo(name = "end_lng") val endLng: Double,

    /** Valeurs Google : WALKING, RUNNING, CYCLING, IN_PASSENGER_VEHICLE, IN_BUS, IN_SUBWAY, FLYING, etc. */
    @ColumnInfo(name = "activity_type") val activityType: String,

    @ColumnInfo(name = "distance_m") val distanceMeters: Int? = null,
    val confidence: String? = null,
    val source: String = "takeout",
    @ColumnInfo(name = "imported_at_ms") val importedAtMs: Long = System.currentTimeMillis(),
)
