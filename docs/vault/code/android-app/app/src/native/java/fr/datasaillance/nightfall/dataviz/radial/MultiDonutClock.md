---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/MultiDonutClock.kt
git_blob: baa4f971bfef642f64d41ecb58411430070cc783
last_synced: '2026-05-27T06:16:08Z'
loc: 825
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
 * Tap inside the donut → fires onSelectionChange with a two-level
 * RadialSelection (layer first, then precise segment on re-tap). See
 * hitTestRadial / DT-2.
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
data class RadialVisit(
    val startMs: Long,
    val endMs: Long,
    val placeName: String,
    /** true si PlaceResolver a trouvé un lieu labellisé dont le rayon contient la visite. */
    val anchored: Boolean = false,
    /** libellé du lieu labellisé matché, null si la visite n'est pas ancrée. */
    val placeLabel: String? = null,
)
data class RadialActivity(val startMs: Long, val endMs: Long, val activityType: String, val distanceMeters: Int)
data class RadialUsageRow(val packageName: String, val totalTimeForegroundMs: Long, val lastTimeUsedMs: Long)

/**
 * Session d'usage foreground aux horaires réels (DT-9). DTO minimaliste local à
 * la spec cadran-v2 — sera aligné/remplacé par la spec `usage-sessions`.
 */
data class UsageSession(
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
)

data class RadialDay(
    val date: LocalDate,
    val sleepStages: List<StageInterval> = emptyList(),
    val visits: List<RadialVisit> = emptyList(),
    val activities: List<RadialActivity> = emptyList(),
    val usageRows: List<RadialUsageRow> = emptyList(),      // conservé pour la liste apps
    val usageSessions: List<UsageSession> = emptyList(),    // vide → état dégradé anneau usage
)

enum class UsageVariant { Heat, Apps }

// ─────────────────────────────────────────────────────────────
// Sélection à deux niveaux (DT-1)
// ─────────────────────────────────────────────────────────────
enum class RadialLayer { SLEEP, USAGE, TIMELINE }

data class RadialSelection(
    val layer: RadialLayer,
    val segmentStartMs: Long? = null,   // null = couche entière sélectionnée (Niveau 1)
    val segmentEndMs: Long? = null,
)

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
    selection: RadialSelection? = null,
    onSelectionChange: (RadialSelection?) -> Unit = {},
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

    // Opacités focus mode (DT-3) : anneau non sélectionné dimé.
    fun layerAlpha(layer: RadialLayer): Float =
        if (selection == null || selection.layer == layer) 1.0f else 0.35f

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

        val geo = RadialGeometry(
            cx = cx, cy = cy,
            rSleepOuter = rSleepOuter + 25f, rSleepInner = rSleepDeepInner,
            rUsageOuter = rUsageOuter, rUsageInner = rUsageInner,
            rTimelineOuter = rTimelineOuter, rTimelineInner = rTimelineInner,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(day, selection, zone) {
                    detectTapGestures { offset ->
                        onSelectionChange(hitTestRadial(offset, geo, selection, day, zone))
                    }
                },
        ) {
            // 1. Sleep donut
            drawSleepDonut(day.sleepStages, cx, cy,
                rOuter = rSleepOuter, rInner = rSleepDeepInner, zone, extras,
                alpha = layerAlpha(RadialLayer.SLEEP),
                selection = selection?.takeIf { it.layer == RadialLayer.SLEEP },
                highlight = extras.highlight)

            // 2. Usage donut
            when (usageVariant) {
                UsageVariant.Heat -> drawUsageHeat(hourBuckets, day.usageSessions, cx, cy,
                    rOuter = rUsageOuter, rInner = rUsageInner,
                    bgColor = palette.background, emptyColor = palette.surface,
                    degradedColor = extras.divider, zone = zone,
                    alpha = layerAlpha(RadialLayer.USAGE))
                UsageVariant.Apps -> drawUsageApps(topApps, day.usageSessions, cx, cy,
                    rOuter = rUsageOuter, rInner = rUsageInner, zone,
                    emptyColor = extras.borderStrong,
                    faintColor = palette.onBackground,
                    degradedColor = extras.divider,
                    alpha = layerAlpha(RadialLayer.USAGE))
            }

            // 3. Timeline donut
            drawTimelineDonut(day.visits, day.activities,
                cx, cy, rTimelineOuter, rTimelineInner, zone, extras.divider,
                mutedColor = extras.textMuted,
                alpha = layerAlpha(RadialLayer.TIMELINE),
                selection = selection?.takeIf { it.layer == RadialLayer.TIMELINE },
                highlight = extras.highlight)

            // 4. Quadrant separator hairlines (repères horaires, pas une sélection)
            listOf(0f, 6f, 12f, 18f).forEach { h ->
                val a = hourToRad(h)
                val p1 = polar(cx, cy, rTimelineInner, a)
                val p2 = polar(cx, cy, rSleepOuter, a)
                drawLine(extras.divider, p1, p2, strokeWidth = 1f)
            }

            // 5. Hour ticks
            drawHourTicks(cx, cy, rHourTicks, extras.textMuted, extras.textFaint)

            // 6. Center hub
            drawCircle(palette.background, radius = rCenter, center = Offset(cx, cy))
            drawCircle(extras.divider, radius = rCenter, center = Offset(cx, cy),
                       style = Stroke(width = 1f))
        }

        // Overlay text labels (Compose Text for crisp typography)
        HourLabels(cx, cy, rHourTicks + 30f, density)
        CenterLabel(day, selection, cx, cy, density)
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
    alpha: Float = 1f,
    selection: RadialSelection? = null,
    highlight: Color = Color(0xFF0E9EB0),
) {
    if (stages.isEmpty()) {
        drawEmptyBand(cx, cy, rOuter, rInner, extras.divider.copy(alpha = alpha))
        return
    }

    val range = rOuter - rInner
    val hasSegment = selection?.segmentStartMs != null
    // Background hint band so the donut shape is always visible
    drawCircle(extras.divider.copy(alpha = 0.5f * alpha), radius = (rOuter + rInner) / 2f,
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
        val isSelected = hasSegment &&
            st.startMs < (selection!!.segmentEndMs ?: Long.MAX_VALUE) &&
            st.endMs > (selection.segmentStartMs ?: Long.MIN_VALUE)
        // Segment sélectionné = opacité nominale ; autres segments du même anneau dimés.
        val segAlpha = if (!hasSegment) alpha else if (isSelected) 1f else 0.45f * alpha
        drawDonutWedge(cx, cy, rOuter, rIn, h1, h2,
            stageColor(st.type, extras).copy(alpha = 0.92f * segAlpha))
        if (isSelected) {
            drawDonutWedgeStroke(cx, cy, rOuter, rIn, h1, h2, highlight, 2f)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Usage donut — heatmap variant (24 hour-wedges)
// ─────────────────────────────────────────────────────────────
private fun DrawScope.drawUsageHeat(
    buckets: FloatArray,
    sessions: List<UsageSession>,
    cx: Float, cy: Float,
    rOuter: Float, rInner: Float,
    bgColor: Color,
    emptyColor: Color,
    degradedColor: Color,
    zone: ZoneId,
    alpha: Float = 1f,
) {
    // État dégradé (DT-5/L6) : aucune session réelle → anneau grisé uniforme.
    if (sessions.isEmpty()) {
        drawEmptyBand(cx, cy, rOuter, rInner, degradedColor.copy(alpha = 0.4f * alpha))
        return
    }

    // Sessions réelles : densité d'usage par tranche horaire (minutes foreground).
    val realBuckets = FloatArray(24)
    sessions.forEach { s ->
        val h = localHour(s.startMs, zone).toInt().coerceIn(0, 23)
        realBuckets[h] += (s.endMs - s.startMs).toFloat() / 60_000f
    }
    val peak = (realBuckets.maxOrNull() ?: 0f).coerceAtLeast(1f)
    for (h in 0 until 24) {
        val v = realBuckets[h]
        val color = if (v == 0f) emptyColor.copy(alpha = alpha) else heatColor(v / peak).copy(alpha = alpha)
        drawDonutWedge(cx, cy, rOuter, rInner, h.toFloat(), h + 1f, color)
    }
    // Hairline separators between hour wedges
    for (h in 0 until 24) {
        val a = hourToRad(h.toFloat())
        val p1 = polar(cx, cy, rInner, a)
        val p2 = polar(cx, cy, rOuter, a)
        drawLine(bgColor.copy(alpha = 0.7f * alpha), p1, p2, strokeWidth = 1f)
    }
}

// ─────────────────────────────────────────────────────────────
// Usage donut — apps variant (concentric sub-rings per app)
// ─────────────────────────────────────────────────────────────
private fun DrawScope.drawUsageApps(
    apps: List<RadialUsageRow>,
    sessions: List<UsageSession>,
    cx: Float, cy: Float,
    rOuter: Float, rInner: Float,
    zone: ZoneId,
    emptyColor: Color,
    faintColor: Color,
    degradedColor: Color,
    alpha: Float = 1f,
) {
    if (apps.isEmpty()) {
        drawEmptyBand(cx, cy, rOuter, rInner, emptyColor.copy(alpha = alpha))
        return
    }
    val bandH = (rOuter - rInner) / apps.size

    apps.forEachIndexed { i, app ->
        val rOut = rOuter - i * bandH
        val rIn  = rOut - bandH * 0.85f
        // Faint background sub-ring
        drawCircle(
            color = faintColor.copy(alpha = 0.05f * alpha),
            radius = (rOut + rIn) / 2f,
            center = Offset(cx, cy),
            style = Stroke(width = rOut - rIn),
        )
        val color = heatColor(0.4f + (i.toFloat() / apps.size) * 0.5f).copy(alpha = alpha)
        // DT-5 : l'arc horaire faux est supprimé. On trace les VRAIS intervalles
        // de session pour cette app si disponibles ; sinon rien sur le cadran
        // (la liste apps reste visible dans la carte contextuelle).
        sessions.filter { it.packageName == app.packageName }.forEach { s ->
            drawDonutWedge(cx, cy, rOut, rIn,
                localHour(s.startMs, zone), localHour(s.endMs, zone), color)
        }
    }
    // Si aucune session réelle, signaler l'état dégradé par un voile gris discret.
    if (sessions.isEmpty()) {
        drawEmptyBand(cx, cy, rOuter, rInner, degradedColor.copy(alpha = 0.18f * alpha))
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
    mutedColor: Color,
    alpha: Float = 1f,
    selection: RadialSelection? = null,
    highlight: Color = Color(0xFF0E9EB0),
) {
    if (visits.isEmpty() && activities.isEmpty()) {
        drawEmptyBand(cx, cy, rOuter, rInner, dividerColor.copy(alpha = alpha))
        return
    }
    val hasSegment = selection?.segmentStartMs != null
    fun segAlpha(startMs: Long, endMs: Long): Float {
        if (!hasSegment) return alpha
        val sel = startMs < (selection!!.segmentEndMs ?: Long.MAX_VALUE) &&
            endMs > (selection.segmentStartMs ?: Long.MIN_VALUE)
        return if (sel) 1f else 0.45f * alpha
    }
    fun isSelected(startMs: Long, endMs: Long): Boolean = hasSegment &&
        startMs < (selection!!.segmentEndMs ?: Long.MAX_VALUE) &&
        endMs > (selection.segmentStartMs ?: Long.MIN_VALUE)

    // Faint background band
    drawCircle(dividerColor.copy(alpha = 0.5f * alpha),
        radius = (rOuter + rInner) / 2f, center = Offset(cx, cy),
        style = Stroke(width = rOuter - rInner))

    // DT-6 : binaire ancré (amber) / non labellisé (muted). Couleur d'activité
    // pour les segments de déplacement (palette Activity).
    visits.forEach { v ->
        val a = segAlpha(v.startMs, v.endMs)
        val baseAlpha = if (v.anchored) 0.85f else 0.55f
        val color = timelineVisitColor(v, mutedColor).copy(alpha = baseAlpha * a)
        drawDonutWedge(cx, cy, rOuter, rInner,
            localHour(v.startMs, zone), localHour(v.endMs, zone), color)
        if (isSelected(v.startMs, v.endMs)) {
            drawDonutWedgeStroke(cx, cy, rOuter, rInner,
                localHour(v.startMs, zone), localHour(v.endMs, zone), highlight, 1.5f)
        }
    }
    val rActOuter = rInner + (rOuter - rInner) * 0.65f
    activities.forEach { act ->
        val a = segAlpha(act.startMs, act.endMs)
        val color = Activity.resolve(act.activityType).first
        drawDonutWedge(cx, cy, rActOuter, rInner,
            localHour(act.startMs, zone), localHour(act.endMs, zone),
            color.copy(alpha = 0.95f * a))
        if (isSelected(act.startMs, act.endMs)) {
            drawDonutWedgeStroke(cx, cy, rActOuter, rInner,
                localHour(act.startMs, zone), localHour(act.endMs, zone), highlight, 1.5f)
        }
    }
}

/** Couleur d'une visite timeline (DT-6) : amber si ancré, muted sinon. */
internal fun timelineVisitColor(v: RadialVisit, mutedColor: Color): Color =
    if (v.anchored) Color(0xFFD37C04) else mutedColor

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
    val onBg = MaterialTheme.colorScheme.onBackground
    labels.forEach { (h, label) ->
        val a = hourToRad(h.toFloat())
        val px = cx + r * cos(a)
        val py = cy + r * sin(a)
        Text(
            text = label,
            color = if (h == 0 || h == 12) onBg else onBg.copy(alpha = 0.82f),
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
    day: RadialDay, selection: RadialSelection?,
    cx: Float, cy: Float, density: androidx.compose.ui.unit.Density,
) {
    val extras = DataSaillance.extras
    val metric = centerMetric(day, selection)
    Column(
        modifier = Modifier.absoluteOffsetPx(cx, cy, density),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = metric.label,
            color = extras.textMuted,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            text = metric.value,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
            ),
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

internal fun DrawScope.drawDonutWedgeStroke(
    cx: Float, cy: Float, rOuter: Float, rInner: Float,
    h1: Float, h2: Float, color: Color, strokeWidth: Float,
) {
    val h2Adj = if (h2 <= h1) h2 + 24f else h2
    val sweepDeg = ((h2Adj - h1) / 24f) * 360f
    val startDeg = (h1 / 24f) * 360f - 90f
    val outerRect = Rect(cx - rOuter, cy - rOuter, cx + rOuter, cy + rOuter)
    val innerRect = Rect(cx - rInner, cy - rInner, cx + rInner, cy + rInner)
    val path = Path().apply {
        arcTo(rect = outerRect, startAngleDegrees = startDeg,
              sweepAngleDegrees = sweepDeg, forceMoveTo = true)
        arcTo(rect = innerRect, startAngleDegrees = startDeg + sweepDeg,
              sweepAngleDegrees = -sweepDeg, forceMoveTo = false)
        close()
    }
    drawPath(path, color, style = Stroke(width = strokeWidth))
}

internal fun DrawScope.drawEmptyBand(
    cx: Float, cy: Float, rOuter: Float, rInner: Float, color: Color,
) {
    drawCircle(color.copy(alpha = 0.55f),
        radius = (rOuter + rInner) / 2f, center = Offset(cx, cy),
        style = Stroke(width = rOuter - rInner))
}

// ─────────────────────────────────────────────────────────────
// Métrique centre cadran (DT-4) — pure, testable
// ─────────────────────────────────────────────────────────────
/** Valeur + label affichés au centre selon la couche sélectionnée. */
data class CenterMetric(val label: String, val value: String)

/**
 * Calcule la métrique contextuelle du centre du cadran (DT-4).
 *
 * - Pas de sélection : durée sommeil prioritaire, sinon durée usage. Jamais de
 *   somme `visits + activities`.
 * - SLEEP : durée sommeil + nb de sessions.
 * - USAGE : temps écran total.
 * - TIMELINE : nb de sorties (`activities` uniquement, pas `visits`) + km totaux.
 */
internal fun centerMetric(day: RadialDay, selection: RadialSelection?): CenterMetric {
    val sleepMin = day.sleepStages.sumOf { (it.endMs - it.startMs) }.toFloat() / 60_000f
    val usageMin = day.usageRows.sumOf { it.totalTimeForegroundMs }.toFloat() / 60_000f

    fun dur(min: Float): String {
        val h = (min / 60).toInt(); val m = (min % 60).toInt()
        return "${h}h${"%02d".format(m)}"
    }

    return when (selection?.layer) {
        RadialLayer.SLEEP -> CenterMetric(
            label = "sommeil · ${countSleepSessions(day)} session" +
                if (countSleepSessions(day) > 1) "s" else "",
            value = if (sleepMin > 0f) dur(sleepMin) else "—",
        )
        RadialLayer.USAGE -> CenterMetric(
            label = "téléphone",
            value = if (usageMin > 0f) dur(usageMin) else "—",
        )
        RadialLayer.TIMELINE -> {
            val km = day.activities.sumOf { it.distanceMeters }.toFloat() / 1000f
            CenterMetric(
                label = "sorties" + if (km > 0f) " · %.1f km".format(km) else "",
                value = "${day.activities.size}",
            )
        }
        null -> when {
            sleepMin > 0f -> CenterMetric("sommeil", dur(sleepMin))
            usageMin > 0f -> CenterMetric("téléphone", dur(usageMin))
            else -> CenterMetric("—", "—")
        }
    }
}

/** Nb de sessions de sommeil = nb de groupes séparés par un trou > 30 min. */
internal fun countSleepSessions(day: RadialDay): Int {
    val sorted = day.sleepStages.sortedBy { it.startMs }
    if (sorted.isEmpty()) return 0
    var sessions = 1
    var prevEnd = sorted.first().endMs
    for (s in sorted.drop(1)) {
        if (s.startMs - prevEnd > 30 * 60_000L) sessions++
        prevEnd = maxOf(prevEnd, s.endMs)
    }
    return sessions
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

/**
 * Géométrie radiale des anneaux pour le hit-test (DT-2). Toutes les valeurs en px,
 * relatives au centre (cx, cy). Extrait du calcul de rendu pour rester testable pur.
 */
data class RadialGeometry(
    val cx: Float,
    val cy: Float,
    val rSleepOuter: Float,
    val rSleepInner: Float,
    val rUsageOuter: Float,
    val rUsageInner: Float,
    val rTimelineOuter: Float,
    val rTimelineInner: Float,
)

/**
 * Heure locale (0.0..24.0) sous le tap, dérivée de l'angle. Identique à la
 * convention de rendu : minuit au sommet, sens horaire.
 */
internal fun hourAtTap(offset: Offset, cx: Float, cy: Float): Float {
    val dx = offset.x - cx
    val dy = offset.y - cy
    val a = atan2(dy, dx)
    val raw = (((a + PI / 2) / (2 * PI)) * 24f).toFloat()
    return ((raw % 24f) + 24f) % 24f
}

/** Anneau touché par le tap, ou null si hors des bandes (hub central / hors cadran). */
internal fun layerAtRadius(offset: Offset, geo: RadialGeometry): RadialLayer? {
    val dx = offset.x - geo.cx
    val dy = offset.y - geo.cy
    val r = sqrt(dx * dx + dy * dy)
    return when {
        r in geo.rTimelineInner..geo.rTimelineOuter -> RadialLayer.TIMELINE
        r in geo.rUsageInner..geo.rUsageOuter -> RadialLayer.USAGE
        r in geo.rSleepInner..geo.rSleepOuter -> RadialLayer.SLEEP
        else -> null
    }
}

/**
 * Hit-test radial à deux étages (DT-2). Renvoie la sélection résultante :
 *  1. Hors zone / hub → null (désélection).
 *  2. Anneau différent de la sélection courante → Niveau 1 (couche entière).
 *  3. Même anneau que la sélection courante :
 *     - segment contenant l'heure tappée trouvé → Niveau 2 (start/end remplis).
 *     - sinon → null (re-tap couche déjà sélectionnée en Niveau 1 → désélection).
 */
internal fun hitTestRadial(
    offset: Offset,
    geo: RadialGeometry,
    current: RadialSelection?,
    day: RadialDay,
    zone: ZoneId,
): RadialSelection? {
    val layer = layerAtRadius(offset, geo) ?: return null
    val hour = hourAtTap(offset, geo.cx, geo.cy)

    // Anneau distinct (ou aucune sélection) → Niveau 1.
    if (current?.layer != layer) {
        return RadialSelection(layer)
    }

    // Même anneau déjà sélectionné → chercher le segment précis.
    val segment = segmentAt(layer, hour, day, zone)
    return if (segment != null) {
        RadialSelection(layer, segment.first, segment.second)
    } else {
        // Re-tap couche déjà en Niveau 1 sans segment → désélection.
        null
    }
}

/** Trouve le segment (startMs, endMs) de la couche contenant `hour`, ou null. */
internal fun segmentAt(
    layer: RadialLayer,
    hour: Float,
    day: RadialDay,
    zone: ZoneId,
): Pair<Long, Long>? {
    fun spans(startMs: Long, endMs: Long): Boolean {
        val h1 = localHour(startMs, zone)
        val h2Raw = localHour(endMs, zone)
        val h2 = if (h2Raw <= h1) h2Raw + 24f else h2Raw
        val h = if (hour < h1) hour + 24f else hour
        return h in h1..h2
    }
    return when (layer) {
        RadialLayer.SLEEP -> day.sleepStages
            .firstOrNull { spans(it.startMs, it.endMs) }
            ?.let { it.startMs to it.endMs }
        RadialLayer.USAGE -> day.usageSessions
            .firstOrNull { spans(it.startMs, it.endMs) }
            ?.let { it.startMs to it.endMs }
        RadialLayer.TIMELINE -> {
            day.visits.firstOrNull { spans(it.startMs, it.endMs) }
                ?.let { return it.startMs to it.endMs }
            day.activities.firstOrNull { spans(it.startMs, it.endMs) }
                ?.let { it.startMs to it.endMs }
        }
    }
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
- `SleepStage` (class) — lines 77-77
- `StageInterval` (class) — lines 79-79
- `RadialVisit` (class) — lines 80-88
- `RadialActivity` (class) — lines 89-89
- `RadialUsageRow` (class) — lines 90-90
- `UsageSession` (class) — lines 96-100
- `RadialDay` (class) — lines 102-109
- `UsageVariant` (class) — lines 111-111
- `RadialLayer` (class) — lines 116-116
- `RadialSelection` (class) — lines 118-122
- `resolve` (function) — lines 136-141
- `placeColor` (function) — lines 144-148
- `heatColor` (function) — lines 151-152
- `stageColor` (function) — lines 162-168
- `MultiDonutClock` (function) — lines 173-294
- `layerAlpha` (function) — lines 207-208
- `drawSleepDonut` (function) — lines 299-346
- `drawUsageHeat` (function) — lines 351-387
- `drawUsageApps` (function) — lines 392-432
- `drawTimelineDonut` (function) — lines 437-494
- `segAlpha` (function) — lines 454-459
- `isSelected` (function) — lines 460-462
- `timelineVisitColor` (function) — lines 497-498
- `drawHourTicks` (function) — lines 503-516
- `HourLabels` (function) — lines 521-544
- `CenterLabel` (function) — lines 549-578
- `drawDonutWedge` (function) — lines 583-602
- `drawDonutWedgeStroke` (function) — lines 604-621
- `drawEmptyBand` (function) — lines 623-629
- `CenterMetric` (class) — lines 635-635
- `centerMetric` (function) — lines 646-678
- `dur` (function) — lines 650-653
- `countSleepSessions` (function) — lines 681-691
- `hourToRad` (function) — lines 696-697
- `polar` (function) — lines 699-700
- `localHour` (function) — lines 702-705
- `RadialGeometry` (class) — lines 711-720
- `hourAtTap` (function) — lines 726-732
- `layerAtRadius` (function) — lines 735-745
- `hitTestRadial` (function) — lines 755-778
- `segmentAt` (function) — lines 781-808
- `spans` (function) — lines 787-793
- `absoluteOffsetPx` (function) — lines 813-825
