---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/Heatmap.kt
git_blob: 6b84f8b68bbd25808b0899c6be924df7d5b688a2
last_synced: '2026-05-26T03:08:52Z'
loc: 232
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/Heatmap.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/Heatmap.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/Heatmap.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.dataviz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.ui.theme.DataSaillance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/* ============================================================
 * Heatmap — polar heatmap of sleep on the 24h dial.
 *
 *   HeatmapVariant.Ring  — 144 wedges (10-min bins), colored by total minutes.
 *   HeatmapVariant.Rose  — same bins, encoded as radial bar lengths.
 *
 * Mirrors  ui_kits/nightfall/dataviz/Heatmap.jsx  1:1.
 * Sequential color ramp from surface-3 to highlight cyan, gamma-corrected.
 * ============================================================ */

data class SleepSession(
    val date: String,       // YYYY-MM-DD
    val bedHour: Float,     // 0..24 decimal, local
    val wakeHour: Float,    // 0..24 decimal, local
    val durationMin: Int,
    val isMain: Boolean,
    val unixMs: Long,
)

enum class HeatmapVariant { Ring, Rose }

private const val BIN_COUNT = 144                  // 10-min bins
private const val BIN_WIDTH_H = 24f / BIN_COUNT    // 0.1666… h

@Composable
fun Heatmap(
    sessions: List<SleepSession>,
    modifier: Modifier = Modifier,
    variant: HeatmapVariant = HeatmapVariant.Ring,
) {
    val palette = MaterialTheme.colorScheme
    val extras  = DataSaillance.extras

    val bins = remember(sessions) { computeBins(sessions) }
    val peak = remember(bins)     { bins.maxOrNull() ?: 0f }

    // Color ramp anchors. `surface3` for empty, `highlight` for peak.
    val emptyColor = extras.borderStrong.copy(alpha = 0.0f)     // overlay only on filled bins
    val baseColor  = Color(0xFF1E262B)
    val peakColor  = extras.highlight

    Box(modifier = modifier.size(360.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rOuter = size.minDimension * 0.42f
            val rInner = size.minDimension * 0.16f

            when (variant) {
                HeatmapVariant.Ring -> drawRing(cx, cy, rOuter, rInner, bins, peak, baseColor, peakColor)
                HeatmapVariant.Rose -> drawRose(cx, cy, rOuter, rInner, bins, peak, peakColor, extras.divider)
            }
            drawHourDial(cx, cy, rOuter, extras.textFaint, extras.divider)
            // Outer and inner outlines
            drawCircle(palette.outline, radius = rOuter + 2f, center = Offset(cx, cy),
                       style = Stroke(width = 1f))
            drawCircle(palette.outline, radius = rInner - 2f, center = Offset(cx, cy),
                       style = Stroke(width = 1f))
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Variation A — Ring of 144 wedges
// ──────────────────────────────────────────────────────────────
private fun DrawScope.drawRing(
    cx: Float, cy: Float, rOuter: Float, rInner: Float,
    bins: FloatArray, peak: Float,
    baseColor: Color, peakColor: Color,
) {
    for (i in 0 until BIN_COUNT) {
        val h1 = i * BIN_WIDTH_H
        val h2 = (i + 1) * BIN_WIDTH_H
        val v  = if (peak > 0f) bins[i] / peak else 0f
        val color = if (bins[i] == 0f) baseColor else heatColor(v, baseColor, peakColor)
        drawWedge(cx, cy, rOuter, rInner, h1, h2, color)
    }
}

// ──────────────────────────────────────────────────────────────
// Variation B — Rose plot (radial bar lengths)
// ──────────────────────────────────────────────────────────────
private fun DrawScope.drawRose(
    cx: Float, cy: Float, rOuter: Float, rInner: Float,
    bins: FloatArray, peak: Float,
    peakColor: Color, divider: Color,
) {
    val maxLen = rOuter - rInner
    // 4 reference rings (25/50/75/100 %)
    val dash = PathEffect.dashPathEffect(floatArrayOf(2f, 3f), 0f)
    listOf(0.25f, 0.5f, 0.75f, 1f).forEach { f ->
        drawCircle(divider, radius = rInner + f * maxLen, center = Offset(cx, cy),
                   style = Stroke(width = 0.6f, pathEffect = dash))
    }
    for (i in 0 until BIN_COUNT) {
        if (bins[i] == 0f) continue
        val v   = if (peak > 0f) bins[i] / peak else 0f
        val len = v.pow(0.65f) * maxLen
        val h1  = i * BIN_WIDTH_H
        val h2  = (i + 1) * BIN_WIDTH_H
        val color = heatColor(v, Color(0xFF2A363B), peakColor)
        drawWedge(cx, cy, rInner + len, rInner, h1, h2, color)
    }
}

// ──────────────────────────────────────────────────────────────
// Color ramp — gamma-corrected lerp from base to peak.
// ──────────────────────────────────────────────────────────────
private fun heatColor(t: Float, base: Color, peak: Color): Color {
    val tt = t.coerceIn(0f, 1f).pow(0.55f)
    return lerp(base, peak, tt)
}

// ──────────────────────────────────────────────────────────────
// Bin computation — minutes slept per 10-min slot, with edge overlap.
// ──────────────────────────────────────────────────────────────
private fun computeBins(sessions: List<SleepSession>): FloatArray {
    val bins = FloatArray(BIN_COUNT)
    for (s in sessions) {
        var bed = s.bedHour
        var wake = s.wakeHour
        if (wake <= bed) wake += 24f
        val startBin = (bed / BIN_WIDTH_H).toInt()
        val endBin   = ((wake - 1e-6f) / BIN_WIDTH_H).toInt()
        for (i in startBin..endBin) {
            val a = i * BIN_WIDTH_H
            val b = a + BIN_WIDTH_H
            val ovStart = max(bed, a)
            val ovEnd   = kotlin.math.min(wake, b)
            if (ovEnd > ovStart) bins[((i % BIN_COUNT) + BIN_COUNT) % BIN_COUNT] += (ovEnd - ovStart) * 60f
        }
    }
    return bins
}

// ──────────────────────────────────────────────────────────────
// Polar drawing primitives — also used by DriftClock.kt
// ──────────────────────────────────────────────────────────────
internal fun DrawScope.drawWedge(
    cx: Float, cy: Float, rOut: Float, rIn: Float,
    h1: Float, h2: Float, color: Color,
) {
    val a1 = hourToRad(h1)
    var a2 = hourToRad(h2)
    if (a2 <= a1) a2 += (2 * PI).toFloat()
    val path = Path().apply {
        moveTo(cx + rOut * cos(a1), cy + rOut * sin(a1))
        arcToRad(cx, cy, rOut, a1, a2)
        lineTo(cx + rIn * cos(a2), cy + rIn * sin(a2))
        arcToRad(cx, cy, rIn, a2, a1, reverse = true)
        close()
    }
    drawPath(path, color)
}

private fun Path.arcToRad(cx: Float, cy: Float, r: Float, a1: Float, a2: Float, reverse: Boolean = false) {
    // Approximate the arc with quadratic Bezier segments — Path.arcTo would also work,
    // but with Compose 1.6+ we use a small chord-step polyline to stay portable.
    val steps = 16
    val from = if (reverse) a2 else a1
    val to   = if (reverse) a1 else a2
    for (s in 1..steps) {
        val a = from + (to - from) * (s.toFloat() / steps)
        lineTo(cx + r * cos(a), cy + r * sin(a))
    }
}

internal fun DrawScope.drawHourDial(
    cx: Float, cy: Float, rOuter: Float, color: Color, mutedColor: Color,
) {
    for (h in 0 until 24) {
        val a = hourToRad(h.toFloat())
        val major = h % 6 == 0
        val minor = h % 3 == 0
        val len = if (major) 8f else if (minor) 5f else 3f
        val p1 = Offset(cx + rOuter * cos(a), cy + rOuter * sin(a))
        val p2 = Offset(cx + (rOuter + len) * cos(a), cy + (rOuter + len) * sin(a))
        drawLine(if (minor) color else mutedColor, p1, p2,
                 strokeWidth = if (major) 1.5f else 1f)
    }
}

internal fun DrawScope.drawSessionArc(
    cx: Float, cy: Float, r: Float,
    bedH: Float, wakeH: Float,
    color: Color, alpha: Float, strokeWidth: Float,
) {
    val a1 = hourToRad(bedH)
    var a2 = hourToRad(wakeH)
    if (a2 <= a1) a2 += (2 * PI).toFloat()
    val sweep = a2 - a1
    drawArc(
        color = color,
        startAngle = Math.toDegrees(a1.toDouble()).toFloat(),
        sweepAngle = Math.toDegrees(sweep.toDouble()).toFloat(),
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        alpha = alpha.coerceIn(0f, 1f),
    )
}

internal fun hourToRad(h: Float): Float =
    ((h / 24f) * 2f * PI - PI / 2).toFloat()

private val androidx.compose.ui.unit.IntSize.minDimension: Float
    get() = kotlin.math.min(width, height).toFloat()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepSession` (class) — lines 35-42
- `HeatmapVariant` (class) — lines 44-44
- `Heatmap` (function) — lines 49-85
- `drawRing` (function) — lines 90-102
- `drawRose` (function) — lines 107-128
- `heatColor` (function) — lines 133-136
- `computeBins` (function) — lines 141-158
- `drawWedge` (function) — lines 163-178
- `arcToRad` (function) — lines 180-190
- `drawHourDial` (function) — lines 192-205
- `drawSessionArc` (function) — lines 207-226
- `hourToRad` (function) — lines 228-229
