---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/DayMapSection.kt
git_blob: fd9dac3259d13c1cbc23a005fe5b6f59b0770253
last_synced: '2026-05-26T03:04:09Z'
loc: 323
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/DayMapSection.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/DayMapSection.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/DayMapSection.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.sleep

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import fr.datasaillance.nightfall.data.local.entity.location.ActivitySegmentEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationPathEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity
import fr.datasaillance.nightfall.viewmodel.sleep.DayLocation
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun DayMapSection(
    dayLocation: DayLocation?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        fr.datasaillance.nightfall.ui.components.Eyebrow(text = "Déplacements")
        Spacer(modifier = Modifier.height(8.dp))

        when {
            dayLocation == null -> {
                LocationPlaceholder("Chargement…")
            }
            dayLocation.visits.isEmpty() && dayLocation.segments.isEmpty() && dayLocation.paths.isEmpty() -> {
                LocationPlaceholder("Aucun déplacement enregistré pour cette journée.")
            }
            else -> {
                DayStatsRow(dayLocation, onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            setHorizontalMapRepetitionEnabled(false)
                            setVerticalMapRepetitionEnabled(false)
                            minZoomLevel = 3.0
                            maxZoomLevel = 19.0
                        }
                    },
                    update = { map ->
                        populateMap(
                            map = map,
                            dayLocation = dayLocation,
                            visitColor = primary.toArgb(),
                            segmentColor = primary.toArgb(),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .testTag("hyp_day_map"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                ActivityLegend(dayLocation = dayLocation)
            }
        }
    }
}

/**
 * Légende dynamique : n'affiche que les types d'activité réellement présents
 * dans la journée. Évite la légende statique surchargée quand l'utilisateur n'a
 * fait que marcher (par ex.) — il ne verra que "Marche".
 */
@Composable
private fun ActivityLegend(dayLocation: DayLocation) {
    val typesPresent = dayLocation.segments.map { it.activityType }.distinct()
    if (typesPresent.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        typesPresent.forEach { type ->
            LegendChip(label = humanizeActivityType(type), colorInt = blendActivityColorInternal(type))
            Spacer(modifier = Modifier.padding(end = 8.dp))
        }
    }
}

@Composable
private fun LegendChip(label: String, colorInt: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .background(
                    androidx.compose.ui.graphics.Color(colorInt),
                    MaterialTheme.shapes.extraSmall,
                )
                .padding(6.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LocationPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayStatsRow(dayLocation: DayLocation, mutedColor: androidx.compose.ui.graphics.Color) {
    val totalDistance = dayLocation.segments.sumOf { it.distanceMeters ?: 0 }
    val typeBreakdown = dayLocation.segments
        .groupingBy { it.activityType }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
    val topType = typeBreakdown.firstOrNull()?.key?.let(::humanizeActivityType)

    Row(modifier = Modifier.fillMaxWidth()) {
        StatChip(label = "Visites", value = dayLocation.visits.size.toString(), muted = mutedColor)
        Spacer(modifier = Modifier.padding(end = 12.dp))
        StatChip(label = "Trajets", value = dayLocation.segments.size.toString(), muted = mutedColor)
        Spacer(modifier = Modifier.padding(end = 12.dp))
        StatChip(label = "Distance", value = formatDistance(totalDistance), muted = mutedColor)
        if (topType != null) {
            Spacer(modifier = Modifier.padding(end = 12.dp))
            StatChip(label = "Principal", value = topType, muted = mutedColor)
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, muted: androidx.compose.ui.graphics.Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = muted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun populateMap(
    map: MapView,
    dayLocation: DayLocation,
    visitColor: Int,
    segmentColor: Int,
) {
    map.overlays.clear()

    val points = mutableListOf<GeoPoint>()

    dayLocation.visits.forEach { visit ->
        val gp = GeoPoint(visit.lat, visit.lng)
        points.add(gp)
        val marker = Marker(map).apply {
            position = gp
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = buildVisitTitle(visit)
            snippet = formatVisitTimes(visit)
        }
        map.overlays.add(marker)
    }

    // Trajets réels (waypoints) — un Polyline par timelinePath.
    // On essaie d'inférer la couleur via l'activity qui chevauche temporellement.
    dayLocation.paths.forEach { path ->
        val pts = decodePathPoints(path)
        if (pts.isEmpty()) return@forEach
        val matchingActivity = dayLocation.segments.firstOrNull { seg ->
            path.startMs < seg.endMs && path.endMs > seg.startMs
        }
        val color = matchingActivity?.activityType?.let { blendActivityColor(it, segmentColor) }
            ?: segmentColor
        points.addAll(pts)
        val line = Polyline(map).apply {
            setPoints(pts)
            outlinePaint.color = color
            outlinePaint.strokeWidth = 8f
            title = matchingActivity?.let { humanizeActivityType(it.activityType) } ?: "Trajet"
            snippet = "${path.pointCount} points"
        }
        map.overlays.add(line)
    }

    // Fallback : activités sans path correspondant → segment vol d'oiseau (rare).
    dayLocation.segments.forEach { seg ->
        val hasPath = dayLocation.paths.any { path ->
            path.startMs < seg.endMs && path.endMs > seg.startMs
        }
        if (hasPath) return@forEach
        val start = GeoPoint(seg.startLat, seg.startLng)
        val end = GeoPoint(seg.endLat, seg.endLng)
        points.add(start)
        points.add(end)
        val line = Polyline(map).apply {
            setPoints(listOf(start, end))
            outlinePaint.color = blendActivityColor(seg.activityType, segmentColor)
            outlinePaint.strokeWidth = 6f
            // Marquer visuellement le fallback (pointillé via alpha réduit)
            outlinePaint.alpha = 140
            title = humanizeActivityType(seg.activityType) + " (estimé)"
            snippet = formatSegmentMeta(seg)
        }
        map.overlays.add(line)
    }

    if (points.isNotEmpty()) {
        val bbox = BoundingBox.fromGeoPointsSafe(points)
        // Note : zoomToBoundingBox attend que le layout soit fait. Si appelé trop tôt,
        // il rentre dans une boucle infinie sur certaines versions. Post via Handler.
        map.post { map.zoomToBoundingBox(bbox, true, 64) }
    }
    map.invalidate()
}

/** Décode `points_json` en List<GeoPoint>. Tolérant aux entrées malformées. */
private fun decodePathPoints(path: LocationPathEntity): List<GeoPoint> {
    return runCatching {
        val arr = JSONArray(path.pointsJson)
        val out = ArrayList<GeoPoint>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val lat = obj.optDouble("lat", Double.NaN)
            val lng = obj.optDouble("lng", Double.NaN)
            if (lat.isNaN() || lng.isNaN()) continue
            out.add(GeoPoint(lat, lng))
        }
        out
    }.getOrDefault(emptyList())
}

private fun buildVisitTitle(v: LocationVisitEntity): String =
    v.placeName ?: v.address ?: "Visite"

private fun formatVisitTimes(v: LocationVisitEntity): String {
    val zone = java.time.ZoneId.systemDefault()
    val start = java.time.Instant.ofEpochMilli(v.startMs).atZone(zone).toLocalTime()
    val end = java.time.Instant.ofEpochMilli(v.endMs).atZone(zone).toLocalTime()
    val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    return "${fmt.format(start)} → ${fmt.format(end)}"
}

private fun formatSegmentMeta(s: ActivitySegmentEntity): String {
    val dist = s.distanceMeters?.let { formatDistance(it) }
    return dist ?: "—"
}

private fun formatDistance(meters: Int): String =
    if (meters < 1000) "${meters} m" else "%.1f km".format(meters / 1000.0)

internal fun humanizeActivityType(type: String): String = when (type) {
    "WALKING" -> "Marche"
    "RUNNING" -> "Course"
    "CYCLING" -> "Vélo"
    "IN_PASSENGER_VEHICLE" -> "Voiture"
    "IN_BUS" -> "Bus"
    "IN_SUBWAY" -> "Métro"
    "IN_TRAIN" -> "Train"
    "FLYING" -> "Avion"
    "UNKNOWN" -> "Inconnu"
    else -> type.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * Couleur du polyline selon le type d'activité.
 * Palette DataSaillance (teal/amber/cyan) — pas de gradient décoratif.
 * Retourne `null` pour les types inconnus → le caller décide du fallback.
 */
private fun colorForActivityType(type: String): Int? = when (type) {
    "WALKING", "RUNNING" -> AndroidColor.parseColor("#3be5e7")  // cyan
    "CYCLING" -> AndroidColor.parseColor("#0e9eb0")             // teal
    "IN_PASSENGER_VEHICLE", "IN_BUS", "IN_SUBWAY", "IN_TRAIN" -> AndroidColor.parseColor("#d37c04")  // amber
    "FLYING" -> AndroidColor.parseColor("#8b5cf6")              // violet sobre
    else -> null
}

private fun blendActivityColor(type: String, fallback: Int): Int =
    colorForActivityType(type) ?: fallback

/** Variante sans fallback pour la légende — gris pour les types inconnus. */
private fun blendActivityColorInternal(type: String): Int =
    colorForActivityType(type) ?: AndroidColor.parseColor("#888888")
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `DayMapSection` (function) — lines 36-89
- `ActivityLegend` (function) — lines 96-106
- `LegendChip` (function) — lines 108-126
- `LocationPlaceholder` (function) — lines 128-142
- `DayStatsRow` (function) — lines 144-165
- `StatChip` (function) — lines 167-181
- `populateMap` (function) — lines 183-255
- `decodePathPoints` (function) — lines 258-271
- `buildVisitTitle` (function) — lines 273-274
- `formatVisitTimes` (function) — lines 276-282
- `formatSegmentMeta` (function) — lines 284-287
- `formatDistance` (function) — lines 289-290
- `humanizeActivityType` (function) — lines 292-303
- `colorForActivityType` (function) — lines 310-316
- `blendActivityColor` (function) — lines 318-319
- `blendActivityColorInternal` (function) — lines 322-323
