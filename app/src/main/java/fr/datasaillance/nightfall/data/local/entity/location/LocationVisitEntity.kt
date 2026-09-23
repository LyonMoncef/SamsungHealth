package fr.datasaillance.nightfall.data.local.entity.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Visite : Google Maps Timeline `placeVisit`. POI ou lieu où l'utilisateur est resté
 * un temps significatif.
 *
 * Coordonnées en degrés décimaux (Google exporte en E7 = degrés × 1e7, on convertit).
 * Timestamps en epoch millis UTC.
 */
@Entity(
    tableName = "location_visits",
    indices = [
        Index(value = ["start_ms", "end_ms", "lat", "lng"], unique = true),
        Index("start_ms"),
        Index("place_id"),
    ],
)
data class LocationVisitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "end_ms") val endMs: Long,

    val lat: Double,
    val lng: Double,

    @ColumnInfo(name = "place_id") val placeId: String? = null,
    @ColumnInfo(name = "place_name") val placeName: String? = null,
    val address: String? = null,

    /** "HIGH_CONFIDENCE" / "MEDIUM_CONFIDENCE" / "LOW_CONFIDENCE" / null. */
    val confidence: String? = null,

    /** Source : "takeout" en Phase A_gps, "live" en Phase B_gps. Permet futur cross-check. */
    val source: String = "takeout",

    @ColumnInfo(name = "imported_at_ms") val importedAtMs: Long = System.currentTimeMillis(),
)
