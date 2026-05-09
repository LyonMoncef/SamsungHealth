---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/TakeoutTimelineParser.kt
git_blob: 0413da6e27b67125cdd57865933fd5f1f01961ba
last_synced: '2026-05-09T19:12:27Z'
loc: 125
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/TakeoutTimelineParser.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/TakeoutTimelineParser.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/TakeoutTimelineParser.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.location

import fr.datasaillance.nightfall.data.local.entity.location.ActivitySegmentEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * Parse les fichiers Google Takeout "Semantic Location History".
 *
 * Format racine (recent ~2024) :
 * ```json
 * { "timelineObjects": [
 *     { "placeVisit": {...} },
 *     { "activitySegment": {...} }
 * ] }
 * ```
 *
 * Lat/lng sont en E7 (degrés × 1e7) côté Google ; on convertit en degrés décimaux.
 * Timestamps en ISO 8601 avec offset (généralement Z) ou en epoch millis selon
 * la version d'export — on supporte les deux.
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
        val arr = root.optJSONArray("timelineObjects") ?: return ParseResult(emptyList(), emptyList())

        val visits = mutableListOf<LocationVisitEntity>()
        val segments = mutableListOf<ActivitySegmentEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            obj.optJSONObject("placeVisit")?.let { v ->
                parseVisit(v, importedAtMs)?.let { visits.add(it) }
            }
            obj.optJSONObject("activitySegment")?.let { s ->
                parseSegment(s, importedAtMs)?.let { segments.add(it) }
            }
        }
        return ParseResult(visits, segments)
    }

    private fun parseVisit(visit: JSONObject, importedAtMs: Long): LocationVisitEntity? {
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

    private fun parseSegment(segment: JSONObject, importedAtMs: Long): ActivitySegmentEntity? {
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

    /**
     * Accepte ISO 8601 (`2024-01-15T08:30:00.000Z` ou avec offset) ou epoch millis
     * en chaîne (vue dans certaines versions Takeout).
     */
    private fun parseTimestamp(value: String): Long? {
        if (value.isBlank()) return null
        // Cas epoch numérique
        value.toLongOrNull()?.let { return it }
        // Cas ISO 8601
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
    }

    private fun String?.ifBlankOrNullDefault(default: String?): String? =
        if (this.isNullOrBlank() || this == "null") default else this
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ParseResult` (class) — lines 28-31
- `parse` (function) — lines 33-49
- `parseVisit` (function) — lines 51-75
- `parseSegment` (function) — lines 77-109
- `parseTimestamp` (function) — lines 115-121
- `ifBlankOrNullDefault` (function) — lines 123-124
