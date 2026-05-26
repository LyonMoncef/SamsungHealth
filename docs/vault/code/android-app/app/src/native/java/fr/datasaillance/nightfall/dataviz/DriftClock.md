---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/DriftClock.kt
git_blob: 7958cc2811e67cf1a3d3e0ef95612604b326add9
last_synced: '2026-05-26T03:08:52Z'
loc: 313
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/DriftClock.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/DriftClock.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/DriftClock.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.dataviz

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.ui.theme.DataSaillance
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/* ============================================================
 * DriftClock — a single arc that drifts smoothly around the 24h dial.
 *
 * The arc represents the *current* sleep session, with bedHour and
 * wakeHour interpolated in real time between consecutive sessions
 * along the shortest angular path. The dial stays clean: no persistent
 * trail, no past-night overlay.
 *
 *   DriftVariant.Pure   — only the arc + tip dots.
 *   DriftVariant.Comet  — same arc + a 5-night faded comet trail.
 *
 * Mirrors  ui_kits/nightfall/dataviz/DriftClock.jsx  1:1.
 *
 * Animation spec:
 *   - Full sweep:    30 000 ms at speed 1× (LinearEasing across the year).
 *                    Smooth angular interpolation between sessions makes the
 *                    drift readable even at 4×.
 *   - Speeds:        0.5× · 1× · 2× · 4×
 *   - Scrub:         drag the slider → pauses + snaps t.
 *   - Trail fade:    opacity = 0.55 * (1 − k/(L+1))^1.2  for k = 1..L.
 * ============================================================ */

enum class DriftVariant { Pure, Comet }

private const val SWEEP_MS_AT_1X = 30_000

@Composable
fun DriftClock(
    sessions: List<SleepSession>,
    modifier: Modifier = Modifier,
    variant: DriftVariant = DriftVariant.Pure,
    trailLength: Int = 5,
    autoPlay: Boolean = true,
) {
    if (sessions.isEmpty()) return
    val palette = MaterialTheme.colorScheme
    val extras  = DataSaillance.extras
    val scope   = rememberCoroutineScope()

    val t = remember { Animatable(0f) }
    var playing by remember { mutableStateOf(autoPlay) }
    var speed   by remember { mutableStateOf(1f) }

    LaunchedEffect(playing, speed) {
        if (!playing) return@LaunchedEffect
        val totalMs = (SWEEP_MS_AT_1X / speed).roundToInt()
        while (isActive) {
            val remainingMs = ((1f - t.value) * totalMs).roundToInt().coerceAtLeast(1)
            t.animateTo(1f, animationSpec = tween(remainingMs, easing = LinearEasing))
            t.snapTo(0f)
        }
    }

    val N = sessions.size
    val k     = t.value * (N - 1)
    val kLow  = k.toInt().coerceIn(0, N - 1)
    val frac  = k - kLow
    val kHigh = min(kLow + 1, N - 1)
    val s0    = sessions[kLow]
    val s1    = sessions[kHigh]
    val bedH  = lerpHour(s0.bedHour,  s1.bedHour,  frac)
    val wakeH = lerpHour(s0.wakeHour, s1.wakeHour, frac)
    val headDate = if (frac < 0.5f) s0.date else s1.date

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(360.dp), contentAlignment = Alignment.Center) {
            // Compute duration once per recomposition.
            val durHours: Float = run {
                var d = wakeH - bedH
                if (d <= 0f) d += 24f
                d
            }
            val durH = durHours.toInt()
            val durM = ((durHours - durH) * 60f).roundToInt()

            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val rOuter = size.minDimension * 0.42f       // hour ticks
                val ringR  = size.minDimension * 0.36f       // arc radius

                // Background ring — thin, faint
                drawCircle(extras.border.copy(alpha = 0.45f),
                           radius = ringR, center = Offset(cx, cy),
                           style = Stroke(width = 2f))

                // Trail comets (oldest first → freshest paints on top)
                if (variant == DriftVariant.Comet) {
                    for (i in trailLength downTo 1) {
                        val idx = kLow - i
                        if (idx < 0) continue
                        val s = sessions[idx]
                        val frac01 = (1f - (i / (trailLength + 1f))).coerceAtLeast(0f)
                        val alpha = 0.55f * frac01.pow(1.2f)
                        drawSessionArc(cx, cy, ringR,
                            s.bedHour, s.wakeHour,
                            color = extras.highlight,
                            alpha = alpha,
                            strokeWidth = 3f)
                    }
                }

                // Head arc — slightly thicker, on top
                drawSessionArc(cx, cy, ringR, bedH, wakeH,
                    color = palette.secondary, alpha = 1f, strokeWidth = 4.5f)

                // Bed marker — hollow ring at the start of the arc.
                val aBed  = hourToRad(bedH)
                val pBed  = Offset(cx + ringR * cos(aBed), cy + ringR * sin(aBed))
                drawCircle(Color(0xFF191E22), radius = 6f, center = pBed)
                drawCircle(palette.secondary, radius = 6f, center = pBed,
                           style = Stroke(width = 2.5f))

                // Wake marker — filled disc with small hole at the end.
                val aWake = hourToRad(wakeH)
                val pWake = Offset(cx + ringR * cos(aWake), cy + ringR * sin(aWake))
                drawCircle(palette.secondary, radius = 7f, center = pWake)
                drawCircle(Color(0xFF191E22), radius = 2.5f, center = pWake)

                drawHourDial(cx, cy, rOuter, extras.textFaint, extras.divider)
            }

            // Big readable duration overlay (Compose Text is more crisp than
            // canvas drawText for typography). Stacked at center.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "DURÉE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.4.sp,
                    ),
                    color = extras.textMuted,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$durH",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp),
                        color = palette.onBackground,
                    )
                    Text(
                        text = "h",
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                        color = palette.onBackground,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                    )
                    Text(
                        text = " %02d".format(durM),
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp),
                        color = palette.onBackground,
                    )
                }
                Text(
                    text = "%s → %s".format(fmtHourClock(bedH), fmtHourClock(wakeH)),
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.textFaint,
                )
            }
        }

        DriftScrubber(
            value = t.value,
            onScrub = { newT ->
                playing = false
                scope.launch { t.snapTo(newT) }
            },
            playing = playing,
            onPlayToggle = { playing = !playing },
            speed = speed,
            onSpeedChange = { speed = it },
            dateLabel = headDate,
            positionLabel = "${kLow + 1} / $N",
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Shortest-angular-path hour interpolation.
// ──────────────────────────────────────────────────────────────
private fun lerpHour(a: Float, b: Float, t: Float): Float {
    var d = b - a
    if (d >  12f) d -= 24f
    if (d < -12f) d += 24f
    return ((a + d * t) % 24f + 24f) % 24f
}

/** HH:MM 24-hour format for decimal hours. */
private fun fmtHourClock(h: Float): String {
    val hr = h.toInt() % 24
    val mn = ((h - h.toInt()) * 60f).roundToInt()
    return "%02d:%02d".format(hr, mn)
}

// `extras.bg` doesn't exist on the contract — fallback to the dark canvas.
// Kept here for any future code in this file that needs the encre background.
@Suppress("unused")
private val fr.datasaillance.nightfall.ui.theme.ExtraColors.bg: Color
    get() = Color(0xFF191E22)

// ──────────────────────────────────────────────────────────────
// Scrubber
// ──────────────────────────────────────────────────────────────
@Composable
private fun DriftScrubber(
    value: Float,
    onScrub: (Float) -> Unit,
    playing: Boolean,
    onPlayToggle: () -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    dateLabel: String,
    positionLabel: String,
) {
    val palette = MaterialTheme.colorScheme
    val extras  = DataSaillance.extras
    val interactionSource = remember { MutableInteractionSource() }
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(palette.secondary)
                .clickable(interactionSource = interactionSource, indication = null) { onPlayToggle() },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(12.dp)) {
                val w = size.width; val h = size.height
                if (playing) {
                    drawRect(palette.onSecondary, Offset(w * 0.16f, 0f), Size(w * 0.22f, h))
                    drawRect(palette.onSecondary, Offset(w * 0.62f, 0f), Size(w * 0.22f, h))
                } else {
                    val tri = Path().apply {
                        moveTo(w * 0.20f, 0f)
                        lineTo(w * 0.92f, h * 0.50f)
                        lineTo(w * 0.20f, h)
                        close()
                    }
                    drawPath(tri, palette.onSecondary)
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onScrub,
            modifier = Modifier.weight(1f),
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = palette.secondary,
                activeTrackColor = palette.secondary,
                inactiveTrackColor = extras.border,
            ),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(0.5f, 1f, 2f, 4f).forEach { sp ->
                val active = sp == speed
                TextButton(
                    onClick = { onSpeedChange(sp) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (sp == sp.toInt().toFloat()) "${sp.toInt()}×" else "${sp}×",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) palette.onSurface else extras.textMuted,
                    )
                }
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(dateLabel,     style = MaterialTheme.typography.labelSmall, color = extras.textFaint)
        Text(positionLabel, style = MaterialTheme.typography.labelSmall, color = extras.textFaint)
    }
}

private fun Float.pow(x: Float): Float =
    if (this <= 0f) 0f else kotlin.math.exp(kotlin.math.ln(this.toDouble()) * x.toDouble()).toFloat()

private val androidx.compose.ui.unit.IntSize.minDimension: Float
    get() = min(width, height).toFloat()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `DriftVariant` (class) — lines 54-54
- `DriftClock` (function) — lines 58-207
- `lerpHour` (function) — lines 212-217
- `fmtHourClock` (function) — lines 220-224
- `DriftScrubber` (function) — lines 235-307
- `pow` (function) — lines 309-310
