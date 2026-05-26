---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/MultiDonutClock.kt
git_blob: 43806a75e7515f9bcbeea575500f72e3040185e6
last_synced: '2026-05-26T03:20:22Z'
loc: 546
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/MultiDonutClock.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/MultiDonutClock.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/MultiDonutClock.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.dataviz.radial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.ui.theme.DataSaillance
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/* ============================================================
 * MultiDonutClock — Compose port of dataviz/MultiDonutClock.jsx.
 *
 * Three concentric donuts on a 24-hour dial:
 *   • Outer   — sleep stages, with each wedge's inner radius set by
 *               stage depth (AWAKE at the rim, DEEP at the inner edge).
 *   • Middle  — phone usage, two interchangeable variants:
 *               UsageVariant.Heat — 24 hour-wedges colored by app-close
 *                                   density for the selected day (or by
 *                                   the supplied typical-day fallback).
 *               UsageVariant.Apps — top apps as concentric sub-rings,
 *                                   each showing today's foreground share
 *                                   and last-use time.
 *   • Inner   — timeline. Place visits as wedges, activity segments
 *               drawn within the inner half of the same band.
 *
 * Tap inside the donut → fires onQuadrantTap(0..3) where the quadrant
 * is the 6-hour block under the touch.
 *
 * The composable is "pure paint" — pass a `RadialDay` (immutable),
 * a variant, and (optionally) the typical-day usage distribution.
 *
 * Drop into:
 *   android-app/app/src/main/java/fr/datasaillance/nightfall/dataviz/radial/
 *
 * Depends on the existing DataSaillanceTheme. Extra brand tokens used:
 *   colorScheme.background  / secondary
 *   extras.highlight  / divider / textMuted / textFaint / borderStrong
 *   extras.stageAwake / stageRem / stageLight / stageDeep
 * ============================================================ */

// ─────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────
enum class SleepStage { AWAKE, REM, LIGHT, DEEP }

data class StageInterval(val type: SleepStage, val startMs: Long, val endMs: Long)
data class RadialVisit(val startMs: Long, val endMs: Long, val placeName: String)
data class RadialActivity(val startMs: Long, val endMs: Long, val activityType: String, val distanceMeters: Int)
data class RadialUsageRow(val packageName: String, val totalTimeForegroundMs: Long, val lastTimeUsedMs: Long)

data class RadialDay(
    val date: LocalDate,
    val sleepStages: List<StageInterval> = emptyList(),
    val visits: List<RadialVisit> = emptyList(),
    val activities: List<RadialActivity> = emptyList(),
    val usageRows: List<RadialUsageRow> = emptyList(),
)

enum class UsageVariant { Heat, Apps }

// ─────────────────────────────────────────────────────────────
// Brand-aware activity color resolver
// ─────────────────────────────────────────────────────────────
internal object Activity {
    val WALKING               = Color(0xFF6FB58A) to "marche"
    val RUNNING               = Color(0xFFA8C8A8) to "course"
    val CYCLING               = Color(0xFFE4C99A) to "vélo"
    val IN_PASSENGER_VEHICLE  = Color(0xFFE07260) to "voiture"
    val IN_BUS                = Color(0xFFC5B6D6) to "bus"
    val IN_SUBWAY             = Color(0xFFB7D4DE) to "métro"
    val FLYING                = Color(0xFF3BE5E7) to "avion"
    val FALLBACK              = Color(0xFF828587) to "autre"
    fun resolve(type: String): Pair<Color, String> = when (type) {
        "WALKING" -> WALKING; "RUNNING" -> RUNNING; "CYCLING" -> CYCLING
        "IN_PASSENGER_VEHICLE" -> IN_PASSENGER_VEHICLE
        "IN_BUS" -> IN_BUS; "IN_SUBWAY" -> IN_SUBWAY; "FLYING" -> FLYING
        else -> FALLBACK
    }
}

internal fun placeColor(name: String): Color = when {
    name.startsWith("Maison")  -> Color(0xFFD37C04)
    name.startsWith("Travail") -> Color(0xFF0E9EB0)
    else                       -> Color(0xFF7A9AAA)
}

// Sequential gamma-corrected color ramp (surface3 → cyan).
internal fun heatColor(t: Float): Color =
    lerp(Color(0xFF2A363B), Color(0xFF3BE5E7), t.coerceIn(0f, 1f).pow(0.55f))

// Stage → depth (0 = thinnest, 1 = deepest into the dial).
internal val stageDepth = mapOf(
    SleepStage.AWAKE to 0.0f,
    SleepStage.REM   to 0.32f,
    SleepStage.LIGHT to 0.66f,
    SleepStage.DEEP  to 1.0f,
)

internal fun stageColor(stage: SleepStage, extras: fr.datasaillance.nightfall.ui.theme.ExtraColors): Color =
    when (stage) {
        SleepStage.AWAKE -> extras.stageAwake
        SleepStage.REM   -> extras.stageRem
        SleepStage.LIGHT -> extras.stageLight
        SleepStage.DEEP  -> extras.stageDeep
    }

// ─────────────────────────────────────────────────────────────
// Public composable
// ─────────────────────────────────────────────────────────────
@Composable
fun MultiDonutClock(
    day: RadialDay,
    modifier: Modifier = Modifier,
    usageVariant: UsageVariant = UsageVariant.Heat,
    typicalUsageHourDist: FloatArray? = null,
    selectedQuadrant: Int? = null,
    onQuadrantTap: (Int) -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val palette = MaterialTheme.colorScheme
    val extras  = DataSaillance.extras
    val density = LocalDensity.current

    // Precompute per-hour usage minutes for the heat variant.
    val hourBuckets: FloatArray = remember(day, typicalUsageHourDist) {
        val out = FloatArray(24)
        if (day.usageRows.isNotEmpty()) {
            day.usageRows.forEach { r ->
                val h = Instant.ofEpochMilli(r.lastTimeUsedMs).atZone(zone).hour
                out[h] += r.totalTimeForegroundMs.toFloat()
            }
        } else if (typicalUsageHourDist != null) {
            typicalUsageHourDist.forEachIndexed { i, v -> out[i] = v }
        }
        out
    }

    // Top apps by foreground time (used by the Apps variant).
    val topApps: List<RadialUsageRow> = remember(day) {
        day.usageRows.sortedByDescending { it.totalTimeForegroundMs }.take(6)
    }

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        val sizePx = with(density) { minOf(maxWidth, maxHeight).toPx() }
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        // Radii (fractions of the canvas size)
        val rHourTicks      = sizePx * 0.470f
        val rSleepOuter     = sizePx * 0.460f
        val rSleepDeepInner = sizePx * 0.330f
        val rUsageOuter     = sizePx * 0.310f
        val rUsageInner     = sizePx * 0.225f
        val rTimelineOuter  = sizePx * 0.205f
        val rTimelineInner  = sizePx * 0.135f
        val rCenter         = sizePx * 0.130f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val q = hitTestQuadrant(offset, cx, cy,
                            rInner = rTimelineInner, rOuter = rSleepOuter + 25f)
                        if (q != null) onQuadrantTap(q)
                    }
                },
        ) {
            // 1. Selected-quadrant highlight (behind everything)
            selectedQuadrant?.let { q ->
                val color = palette.secondary.copy(alpha = 0.10f)
                drawDonutWedge(cx, cy, rSleepOuter + 25f, rTimelineInner,
                    h1 = q * 6f, h2 = q * 6f + 6f, color = color)
            }

            // 2. Sleep donut
            drawSleepDonut(day.sleepStages, cx, cy,
                rOuter = rSleepOuter, rInner = rSleepDeepInner, zone, extras)

            // 3. Usage donut
            when (usageVariant) {
                UsageVariant.Heat -> drawUsageHeat(hourBuckets, cx, cy,
                    rOuter = rUsageOuter, rInner = rUsageInner, palette.background)
                UsageVariant.Apps -> drawUsageApps(topApps, cx, cy,
                    rOuter = rUsageOuter, rInner = rUsageInner, zone)
            }

            // 4. Timeline donut
            drawTimelineDonut(day.visits, day.activities,
                cx, cy, rTimelineOuter, rTimelineInner, zone, extras.divider)

            // 5. Quadrant separator hairlines
            listOf(0f, 6f, 12f, 18f).forEach { h ->
                val a = hourToRad(h)
                val p1 = polar(cx, cy, rTimelineInner, a)
                val p2 = polar(cx, cy, rSleepOuter, a)
                drawLine(extras.divider, p1, p2, strokeWidth = 1f)
            }

            // 6. Hour ticks
            drawHourTicks(cx, cy, rHourTicks, extras.textMuted, extras.textFaint)

            // 7. Center hub
            drawCircle(palette.background, radius = rCenter, center = Offset(cx, cy))
            drawCircle(extras.divider, radius = rCenter, center = Offset(cx, cy),
                       style = Stroke(width = 1f))
        }

        // Overlay text labels (Compose Text for crisp typography)
        HourLabels(cx, cy, rHourTicks + 30f, density)
        CenterLabel(day, cx, cy, density)
    }
}

// ─────────────────────────────────────────────────────────────
// Sleep donut — stage wedges with variable inner radius
// ─────────────────────────────────────────────────────────────
private fun DrawScope.drawSleepDonut(
    stages: List<StageInterval>,
    cx: Float, cy: Float,
    rOuter: Float, rInner: Float,
    zone: ZoneId,
    extras: fr.datasaillance.nightfall.ui.theme.ExtraColors,
) {
    if (stages.isEmpty()) {
        drawEmptyBand(cx, cy, rOuter, rInner, extras.divider)
        return
    }

    val range = rOuter - rInner
    // Background hint band so the donut shape is always visible
    drawCircle(extras.divider.copy(alpha = 0.5f), radius = (rOuter + rInner) / 2f,
               center = Offset(cx, cy), style = Stroke(width = range))

    // Merge consecutive identical-stage intervals for cleaner wedges
    val merged = mutableListOf<StageInterval>()
    for (st in stages) {
        val last = merged.lastOrNull()
        if (last != null && last.type == st.type && (st.startMs - last.endMs) < 1000L) {
            merged[merged.lastIndex] = last.copy(endMs = st.endMs)
        } else {
            merged += st
        }
    }
    for (st in merged) {
        val depth = stageDepth[st.type] ?: 0f
        val rIn = rOuter - depth * range
        val h1 = localHour(st.startMs, zone)
        val h2 = localHour(st.endMs, zone)
        drawDonutWedge(cx, cy, rOuter, rIn, h1, h2, stageColor(st.type, extras).copy(alpha = 0.92f))
    }
}

// ─────────────────────────────────────────────────────────────
// Usage donut — heatmap variant (24 hour-wedges)
// ─────────────────────────────────────────────────────────────
private fun DrawScope.drawUsageHeat(
    buckets: FloatArray,
    cx: Float, cy: Float,
    rOuter: Float, rInner: Float,
    bgColor: Color,
) {
    val peak = (buckets.maxOrNull() ?: 0f).coerceAtLeast(1f)
    for (h in 0 until 24) {
        val v = buckets[h]
        val color = if (v == 0f) Color(0xFF1E262B) else heatColor(v / peak)
        drawDonutWedge(cx, cy, rOuter, rInner, h.toFloat(), h + 1f, color)
    }
    // Hairline separators between hour wedges
    for (h in 0 until 24) {
        val a = hourToRad(h.toFloat())
        val p1 = polar(cx, cy, rInner, a)
        val p2 = polar(cx, cy, rOuter, a)
        drawLine(bgColor.copy(alpha = 0.7f), p1, p2, strokeWidth = 1f)
    }
}

// ─────────────────────────────────────────────────────────────
// Usage donut — apps variant (concentric sub-rings per app)
// ─────────────────────────────────────────────────────────────
private fun DrawScope.drawUsageApps(
    apps: List<RadialUsageRow>,
    cx: Float, cy: Float,
    rOuter: Float, rInner: Float,
    zone: ZoneId,
) {
    if (apps.isEmpty()) {
        drawEmptyBand(cx, cy, rOuter, rInner, Color(0xFF2E3D44))
        return
    }
    val bandH = (rOuter - rInner) / apps.size
    val peakMs = apps.maxOf { it.totalTimeForegroundMs }.toFloat()

    apps.forEachIndexed { i, app ->
        val rOut = rOuter - i * bandH
        val rIn  = rOut - bandH * 0.85f
        val h = localHour(app.lastTimeUsedMs, zone)
        val widthH = 0.4f + (app.totalTimeForegroundMs / peakMs) * 1.8f
        // Faint background sub-ring
        drawCircle(
            color = Color(0xFFE8EFF2).copy(alpha = 0.05f),
            radius = (rOut + rIn) / 2f,
            center = Offset(cx, cy),
            style = Stroke(width = rOut - rIn),
        )
        // Per-app marker arc
        val color = heatColor(0.4f + (i.toFloat() / apps.size) * 0.5f)
        drawDonutWedge(cx, cy, rOut, rIn, h - widthH, h, color)
    }
}

// ─────────────────────────────────────────────────────────────
// Timeline donut — visits (full band) + activities (inner half)
// ─────────────────────────────────────────────────────────────
private fun DrawScope.drawTimelineDonut(
    visits: List<RadialVisit>,
    activities: List<RadialActivity>,
    cx: Float, cy: Float,
    rOuter: Float, rInner: Float,
    zone: ZoneId,
    dividerColor: Color,
) {
    if (visits.isEmpty() && activities.isEmpty()) {
        drawEmptyBand(cx, cy, rOuter, rInner, dividerColor)
        return
    }
    // Faint background band
    drawCircle(dividerColor.copy(alpha = 0.5f),
        radius = (rOuter + rInner) / 2f, center = Offset(cx, cy),
        style = Stroke(width = rOuter - rInner))

    visits.forEach { v ->
        drawDonutWedge(cx, cy, rOuter, rInner,
            localHour(v.startMs, zone), localHour(v.endMs, zone),
            placeColor(v.placeName).copy(alpha = 0.85f))
    }
    val rActOuter = rInner + (rOuter - rInner) * 0.65f
    activities.forEach { a ->
        val color = Activity.resolve(a.activityType).first
        drawDonutWedge(cx, cy, rActOuter, rInner,
            localHour(a.startMs, zone), localHour(a.endMs, zone),
            color.copy(alpha = 0.95f))
    }
}

// ─────────────────────────────────────────────────────────────
// Hour ticks (24, with majors at 0/6/12/18)
// ─────────────────────────────────────────────────────────────
private fun DrawScope.drawHourTicks(
    cx: Float, cy: Float, r: Float, minorColor: Color, mutedColor: Color,
) {
    for (h in 0 until 24) {
        val a = hourToRad(h.toFloat())
        val major = h % 6 == 0
        val minor = h % 3 == 0
        val len = if (major) 14f else if (minor) 8f else 4f
        val p1 = polar(cx, cy, r, a)
        val p2 = polar(cx, cy, r + len, a)
        drawLine(if (minor) minorColor else mutedColor, p1, p2,
                 strokeWidth = if (major) 1.5f else 1f)
    }
}

// ─────────────────────────────────────────────────────────────
// Hour labels (Compose Text overlay)
// ─────────────────────────────────────────────────────────────
@Composable
private fun BoxWithConstraintsScope.HourLabels(
    cx: Float, cy: Float, r: Float, density: androidx.compose.ui.unit.Density,
) {
    val labels = listOf(
        0 to "minuit", 6 to "06h", 12 to "midi", 18 to "18h",
    )
    labels.forEach { (h, label) ->
        val a = hourToRad(h.toFloat())
        val px = cx + r * cos(a)
        val py = cy + r * sin(a)
        Text(
            text = label,
            color = if (h == 0 || h == 12) Color(0xFFF2F6F8) else Color(0xFFE8EFF2),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = if (h == 0 || h == 12) 13.sp else 12.sp,
                fontWeight = if (h == 0 || h == 12) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.06f.sp,
            ),
            modifier = Modifier.absoluteOffsetPx(px, py, density),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Center label (date-aware metric)
// ─────────────────────────────────────────────────────────────
@Composable
private fun BoxWithConstraintsScope.CenterLabel(
    day: RadialDay, cx: Float, cy: Float, density: androidx.compose.ui.unit.Density,
) {
    val extras = DataSaillance.extras
    val sleepMin = day.sleepStages.sumOf { (it.endMs - it.startMs) }.toFloat() / 60_000f
    val usageMin = day.usageRows.sumOf { it.totalTimeForegroundMs }.toFloat() / 60_000f
    val main    = if (sleepMin > 0f) sleepMin else usageMin
    val label   = when {
        sleepMin > 0f -> "sommeil"
        usageMin > 0f -> "téléphone"
        else          -> "—"
    }
    val h = (main / 60).toInt()
    val m = (main % 60).toInt()
    Column(
        modifier = Modifier.absoluteOffsetPx(cx, cy, density),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = extras.textMuted,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            text = if (main > 0f) "${h}h${"%02d".format(m)}" else "—",
            color = Color(0xFFF2F6F8),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
            ),
        )
        Text(
            text = "${day.visits.size + day.activities.size} déplacements",
            color = extras.textFaint,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Donut wedge primitive (filled sector between two radii)
// ─────────────────────────────────────────────────────────────
internal fun DrawScope.drawDonutWedge(
    cx: Float, cy: Float, rOuter: Float, rInner: Float,
    h1: Float, h2: Float, color: Color,
) {
    val h2Adj = if (h2 <= h1) h2 + 24f else h2
    val sweepDeg = ((h2Adj - h1) / 24f) * 360f
    val startDeg = (h1 / 24f) * 360f - 90f
    val outerRect = Rect(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter)
    val innerRect = Rect(cx - rInner, cy - rInner, cx + rInner, cy + rInner)
    val path = Path().apply {
        // Outer arc forward
        arcTo(rect = outerRect, startAngleDegrees = startDeg,
              sweepAngleDegrees = sweepDeg, forceMoveTo = true)
        // Inner arc backward
        arcTo(rect = innerRect, startAngleDegrees = startDeg + sweepDeg,
              sweepAngleDegrees = -sweepDeg, forceMoveTo = false)
        close()
    }
    drawPath(path, color)
}

internal fun DrawScope.drawEmptyBand(
    cx: Float, cy: Float, rOuter: Float, rInner: Float, color: Color,
) {
    drawCircle(color.copy(alpha = 0.55f),
        radius = (rOuter + rInner) / 2f, center = Offset(cx, cy),
        style = Stroke(width = rOuter - rInner))
}

// ─────────────────────────────────────────────────────────────
// Math
// ─────────────────────────────────────────────────────────────
internal fun hourToRad(h: Float): Float =
    ((h / 24f) * 2f * PI - PI / 2).toFloat()

internal fun polar(cx: Float, cy: Float, r: Float, a: Float): Offset =
    Offset(cx + r * cos(a), cy + r * sin(a))

internal fun localHour(ms: Long, zone: ZoneId): Float {
    val z = Instant.ofEpochMilli(ms).atZone(zone)
    return z.hour + z.minute / 60f + z.second / 3600f
}

private fun hitTestQuadrant(
    offset: Offset, cx: Float, cy: Float, rInner: Float, rOuter: Float,
): Int? {
    val dx = offset.x - cx
    val dy = offset.y - cy
    val r = sqrt(dx * dx + dy * dy)
    if (r !in rInner..rOuter) return null
    val a = atan2(dy, dx)
    val raw = (((a + PI / 2) / (2 * PI)) * 24f).toFloat()
    val hour = ((raw % 24f) + 24f) % 24f
    return (hour / 6f).toInt().coerceIn(0, 3)
}

// ─────────────────────────────────────────────────────────────
// Modifier helper — position an overlay so its CENTER lands at (x, y) in px.
// ─────────────────────────────────────────────────────────────
private fun Modifier.absoluteOffsetPx(
    x: Float, y: Float, density: androidx.compose.ui.unit.Density,
): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(
                x = (x - placeable.width / 2f).toInt(),
                y = (y - placeable.height / 2f).toInt(),
            )
        }
    }
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepStage` (class) — lines 76-76
- `StageInterval` (class) — lines 78-78
- `RadialVisit` (class) — lines 79-79
- `RadialActivity` (class) — lines 80-80
- `RadialUsageRow` (class) — lines 81-81
- `RadialDay` (class) — lines 83-89
- `UsageVariant` (class) — lines 91-91
- `resolve` (function) — lines 105-110
- `placeColor` (function) — lines 113-117
- `heatColor` (function) — lines 120-121
- `stageColor` (function) — lines 131-137
- `MultiDonutClock` (function) — lines 142-247
- `drawSleepDonut` (function) — lines 252-286
- `drawUsageHeat` (function) — lines 291-310
- `drawUsageApps` (function) — lines 315-344
- `drawTimelineDonut` (function) — lines 349-378
- `drawHourTicks` (function) — lines 383-396
- `HourLabels` (function) — lines 401-423
- `CenterLabel` (function) — lines 428-470
- `drawDonutWedge` (function) — lines 475-494
- `drawEmptyBand` (function) — lines 496-502
- `hourToRad` (function) — lines 507-508
- `polar` (function) — lines 510-511
- `localHour` (function) — lines 513-516
- `hitTestQuadrant` (function) — lines 518-529
- `absoluteOffsetPx` (function) — lines 534-546
