package fr.datasaillance.nightfall.data.local.location

import fr.datasaillance.nightfall.data.local.entity.location.ActivitySegmentEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity
import org.json.JSONObject
import java.time.Instant

/**
 * Parse les fichiers Google Takeout "Location History".
 *
 * Deux formats supportés :
 *
 * 1. **Ancien (Semantic Location History, ~pré-2024)** :
 *    ```json
 *    { "timelineObjects": [ { "placeVisit": {...} }, { "activitySegment": {...} } ] }
 *    ```
 *    Lat/lng en E7 (degrés × 1e7).
 *
 * 2. **Nouveau (Timeline 2024+)** :
 *    ```json
 *    { "semanticSegments": [
 *        { "startTime": "...", "endTime": "...", "visit": {...} },
 *        { "startTime": "...", "endTime": "...", "activity": {...} },
 *        { "startTime": "...", "endTime": "...", "timelinePath": [...] }   // ignoré (chemins GPS bruts)
 *    ], "rawSignals": [...] }                                              // ignoré (~22k points)
 *    ```
 *    Lat/lng en string `"45.81213°, 4.8888115°"`.
 *
 * Implémentation org.json (built-in Android) — tolérante, pas de dépendance ajoutée.
 */
object TakeoutTimelineParser {

    data class ParseResult(
        val visits: List<LocationVisitEntity>,
        val segments: List<ActivitySegmentEntity>,
    )

    fun parse(rawJson: String, importedAtMs: Long = System.currentTimeMillis()): ParseResult {
        val root = JSONObject(rawJson)
        val visits = mutableListOf<LocationVisitEntity>()
        val segments = mutableListOf<ActivitySegmentEntity>()

        // Format 1 — ancien : timelineObjects
        root.optJSONArray("timelineObjects")?.let { arr ->
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                obj.optJSONObject("placeVisit")?.let { v ->
                    parseLegacyVisit(v, importedAtMs)?.let { visits.add(it) }
                }
                obj.optJSONObject("activitySegment")?.let { s ->
                    parseLegacySegment(s, importedAtMs)?.let { segments.add(it) }
                }
            }
        }

        // Format 2 — nouveau : semanticSegments
        root.optJSONArray("semanticSegments")?.let { arr ->
            for (i in 0 until arr.length()) {
                val seg = arr.optJSONObject(i) ?: continue
                val startMs = parseTimestamp(seg.optString("startTime")) ?: continue
                val endMs = parseTimestamp(seg.optString("endTime")) ?: continue
                seg.optJSONObject("visit")?.let { v ->
                    parseNewVisit(v, startMs, endMs, importedAtMs)?.let { visits.add(it) }
                }
                seg.optJSONObject("activity")?.let { a ->
                    parseNewActivity(a, startMs, endMs, importedAtMs)?.let { segments.add(it) }
                }
                // timelinePath ignoré (sera réintroduit en Phase B_gps avec FusedLocation)
            }
        }

        return ParseResult(visits, segments)
    }

    // --- Format 1 (ancien) ---

    private fun parseLegacyVisit(visit: JSONObject, importedAtMs: Long): LocationVisitEntity? {
        val location = visit.optJSONObject("location") ?: return null
        val duration = visit.optJSONObject("duration") ?: return null

        val latE7 = location.optInt("latitudeE7", Int.MIN_VALUE)
        val lngE7 = location.optInt("longitudeE7", Int.MIN_VALUE)
        if (latE7 == Int.MIN_VALUE || lngE7 == Int.MIN_VALUE) return null

        val startMs = parseTimestamp(duration.optString("startTimestamp")) ?: return null
        val endMs = parseTimestamp(duration.optString("endTimestamp")) ?: return null

        return LocationVisitEntity(
            startMs = startMs,
            endMs = endMs,
            lat = latE7 / 1e7,
            lng = lngE7 / 1e7,
            placeId = location.optString("placeId").ifBlankOrNullDefault(null),
            placeName = location.optString("name").ifBlankOrNullDefault(null),
            address = location.optString("address").ifBlankOrNullDefault(null),
            confidence = visit.optString("visitConfidence").ifBlankOrNullDefault(null)
                ?: visit.optString("placeConfidence").ifBlankOrNullDefault(null),
            source = "takeout",
            importedAtMs = importedAtMs,
        )
    }

    private fun parseLegacySegment(segment: JSONObject, importedAtMs: Long): ActivitySegmentEntity? {
        val start = segment.optJSONObject("startLocation") ?: return null
        val end = segment.optJSONObject("endLocation") ?: return null
        val duration = segment.optJSONObject("duration") ?: return null

        val startLatE7 = start.optInt("latitudeE7", Int.MIN_VALUE)
        val startLngE7 = start.optInt("longitudeE7", Int.MIN_VALUE)
        val endLatE7 = end.optInt("latitudeE7", Int.MIN_VALUE)
        val endLngE7 = end.optInt("longitudeE7", Int.MIN_VALUE)
        if (startLatE7 == Int.MIN_VALUE || endLatE7 == Int.MIN_VALUE) return null

        val startMs = parseTimestamp(duration.optString("startTimestamp")) ?: return null
        val endMs = parseTimestamp(duration.optString("endTimestamp")) ?: return null

        val activityType = segment.optString("activityType").ifBlankOrNullDefault("UNKNOWN")
            ?: "UNKNOWN"
        val distance = segment.optInt("distance", -1).takeIf { it >= 0 }
        val confidence = segment.optString("confidence").ifBlankOrNullDefault(null)

        return ActivitySegmentEntity(
            startMs = startMs,
            endMs = endMs,
            startLat = startLatE7 / 1e7,
            startLng = startLngE7 / 1e7,
            endLat = endLatE7 / 1e7,
            endLng = endLngE7 / 1e7,
            activityType = activityType,
            distanceMeters = distance,
            confidence = confidence,
            source = "takeout",
            importedAtMs = importedAtMs,
        )
    }

    // --- Format 2 (nouveau) ---

    private fun parseNewVisit(
        visit: JSONObject,
        startMs: Long,
        endMs: Long,
        importedAtMs: Long,
    ): LocationVisitEntity? {
        val candidate = visit.optJSONObject("topCandidate") ?: return null
        val placeLocation = candidate.optJSONObject("placeLocation") ?: return null
        val (lat, lng) = parseLatLngString(placeLocation.optString("latLng")) ?: return null

        val placeId = candidate.optString("placeId").ifBlankOrNullDefault(null)
        // semanticType : INFERRED_HOME / INFERRED_WORK / UNKNOWN → on l'expose comme placeName
        // pour que l'utilisateur voie "Maison / Travail" au lieu d'un placeId opaque.
        val semanticType = candidate.optString("semanticType").ifBlankOrNullDefault(null)
        val placeName = semanticType?.removePrefix("INFERRED_")?.let { type ->
            when (type) {
                "HOME" -> "Maison"
                "WORK" -> "Travail"
                "UNKNOWN" -> null
                else -> type.lowercase().replaceFirstChar { it.uppercase() }
            }
        }
        val probability = visit.optDouble("probability", Double.NaN)
        val confidence = if (probability.isNaN()) null else "%.2f".format(probability)

        return LocationVisitEntity(
            startMs = startMs,
            endMs = endMs,
            lat = lat,
            lng = lng,
            placeId = placeId,
            placeName = placeName,
            address = null,
            confidence = confidence,
            source = "takeout",
            importedAtMs = importedAtMs,
        )
    }

    private fun parseNewActivity(
        activity: JSONObject,
        startMs: Long,
        endMs: Long,
        importedAtMs: Long,
    ): ActivitySegmentEntity? {
        val start = activity.optJSONObject("start") ?: return null
        val end = activity.optJSONObject("end") ?: return null
        val (startLat, startLng) = parseLatLngString(start.optString("latLng")) ?: return null
        val (endLat, endLng) = parseLatLngString(end.optString("latLng")) ?: return null

        val candidate = activity.optJSONObject("topCandidate") ?: return null
        val type = candidate.optString("type").ifBlankOrNullDefault("UNKNOWN") ?: "UNKNOWN"

        val distance = activity.optDouble("distanceMeters", Double.NaN)
            .takeIf { !it.isNaN() && it >= 0 }
            ?.toInt()

        val probability = activity.optDouble("probability", Double.NaN)
        val confidence = if (probability.isNaN()) null else "%.2f".format(probability)

        return ActivitySegmentEntity(
            startMs = startMs,
            endMs = endMs,
            startLat = startLat,
            startLng = startLng,
            endLat = endLat,
            endLng = endLng,
            activityType = type,
            distanceMeters = distance,
            confidence = confidence,
            source = "takeout",
            importedAtMs = importedAtMs,
        )
    }

    // --- Helpers ---

    /**
     * Parse `"45.81213°, 4.8888115°"` (nouveau format) ou `"45.81213, 4.8888115"` (sans degré).
     * Tolérant aux espaces et au symbole degré optionnel.
     */
    internal fun parseLatLngString(value: String): Pair<Double, Double>? {
        if (value.isBlank()) return null
        val parts = value.split(",").map { it.trim().trimEnd('°').trim() }
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        return lat to lng
    }

    /**
     * Accepte ISO 8601 (`2024-01-15T08:30:00.000Z` ou avec offset `+01:00`)
     * ou epoch millis en chaîne.
     */
    private fun parseTimestamp(value: String): Long? {
        if (value.isBlank()) return null
        value.toLongOrNull()?.let { return it }
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
    }

    private fun String?.ifBlankOrNullDefault(default: String?): String? =
        if (this.isNullOrBlank() || this == "null") default else this
}
