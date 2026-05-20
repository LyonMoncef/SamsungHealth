---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/HypnogramScreen.kt
git_blob: a53d4063cfab9f32c2981e5542a498f18e529198
last_synced: '2026-05-20T16:30:46Z'
loc: 724
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/HypnogramScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/HypnogramScreen.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/HypnogramScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import fr.datasaillance.nightfall.data.sleep.SleepStageResponse
import fr.datasaillance.nightfall.viewmodel.sleep.HypnogramUiState
import fr.datasaillance.nightfall.viewmodel.sleep.HypnogramViewModel
import timber.log.Timber
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ColorDeep  = Color(0xFF0E9EB0)
private val ColorLight = Color(0xFF7A9AAA)
private val ColorRem   = Color(0xFF07BCD3)
private val ColorAwake = Color(0xFFD37C04)

private const val HYPNO_MICRO_AWAKE_THRESHOLD_MIN = 5

enum class HypnogramViewMode { BARS, MOUNTAIN }

private fun stageColor(stageType: String): Color = when (stageType) {
    "DEEP"  -> ColorDeep
    "REM"   -> ColorRem
    "AWAKE" -> ColorAwake
    else    -> ColorLight
}

private fun hypnoMountainLevel(stage: String): Int = when (stage) {
    "REM"   -> 4
    "LIGHT" -> 3
    "DEEP"  -> 2
    "AWAKE" -> 1
    else    -> 0
}

private fun hypnoStageDisplayName(type: String): String = when (type) {
    "DEEP"  -> "Sommeil profond"
    "LIGHT" -> "Sommeil léger"
    "REM"   -> "Sommeil paradoxal"
    "AWAKE" -> "Éveil"
    else    -> type
}

/**
 * Cherche le stage qui couvre un instant `t` (offset depuis le début).
 * Retourne null si aucun stage ne le couvre (gap entre 2 sessions).
 */
private fun stageAtTime(
    t: OffsetDateTime,
    sortedStages: List<SleepStageResponse>,
): SleepStageResponse? = sortedStages.firstOrNull { stage ->
    val ss = runCatching { OffsetDateTime.parse(stage.stage_start) }.getOrNull() ?: return@firstOrNull false
    val se = runCatching { OffsetDateTime.parse(stage.stage_end) }.getOrNull() ?: return@firstOrNull false
    !t.isBefore(ss) && t.isBefore(se)
}

private fun nightTitle(sleepStart: String): String {
    val dt = runCatching { OffsetDateTime.parse(sleepStart) }.getOrNull() ?: return ""
    val prevDay = dt.minusDays(1)
    val dayFormatter  = DateTimeFormatter.ofPattern("EEE", Locale.FRENCH)
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
    val dayAbbr = prevDay.format(dayFormatter).replaceFirstChar { it.uppercase() }.trimEnd('.')
    val dateStr = dt.format(dateFormatter)
    return "Nuit du $dayAbbr $dateStr"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HypnogramScreen(
    viewModel: HypnogramViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var viewMode by remember { mutableStateOf(HypnogramViewMode.BARS) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = when (val s = uiState) {
                            is HypnogramUiState.Success -> nightTitle(s.sessions.first().sleep_start)
                            else -> ""
                        }
                        Text(title, style = MaterialTheme.typography.headlineMedium)
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            Timber.d("scope=hypno_screen back")
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                viewMode = if (viewMode == HypnogramViewMode.BARS) HypnogramViewMode.MOUNTAIN else HypnogramViewMode.BARS
                            },
                            modifier = Modifier.testTag("hyp_view_toggle"),
                        ) {
                            Text(
                                text = if (viewMode == HypnogramViewMode.BARS) "Mountain" else "Barres",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (val state = uiState) {
                    is HypnogramUiState.Idle -> {}

                    is HypnogramUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize().testTag("hyp_loading"),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is HypnogramUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().testTag("hyp_error"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { viewModel.retry() },
                                    modifier = Modifier.testTag("hyp_retry")
                                ) {
                                    Text("Réessayer")
                                }
                            }
                        }
                    }

                    is HypnogramUiState.Success -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .testTag("hyp_screen")
                        ) {
                            HypnogramSummarySection(state.sessions)
                            Spacer(modifier = Modifier.height(16.dp))
                            when (viewMode) {
                                HypnogramViewMode.BARS -> HypnogramCanvas(
                                    sessions = state.sessions,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(140.dp)
                                        .testTag("hyp_canvas")
                                )
                                HypnogramViewMode.MOUNTAIN -> HypnogramMountainCanvas(
                                    sessions = state.sessions,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .height(180.dp)
                                        .testTag("hyp_canvas_mountain")
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HypnogramLegend()
                            Spacer(modifier = Modifier.height(16.dp))
                            HypnogramStatsSection(sessions = state.sessions)
                            Spacer(modifier = Modifier.height(24.dp))
                            DayMapSection(dayLocation = state.dayLocation)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HypnogramSummarySection(
    sessions: List<SleepSessionResponse>,
    modifier: Modifier = Modifier
) {
    val start = sessions.mapNotNull { runCatching { OffsetDateTime.parse(it.sleep_start) }.getOrNull() }.minOrNull()
    val end   = sessions.mapNotNull { runCatching { OffsetDateTime.parse(it.sleep_end) }.getOrNull() }.maxOrNull()
    val duration = if (start != null && end != null) Duration.between(start, end) else null

    val durationText = duration?.let {
        val h = it.toHours()
        val mm = it.toMinutes() % 60
        "${h}h ${mm.toString().padStart(2, '0')}"
    } ?: ""

    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val bedTime  = start?.format(timeFmt) ?: ""
    val wakeTime = end?.format(timeFmt) ?: ""

    val totalMin = duration?.toMinutes() ?: 0L
    val deepMin  = sessions.flatMap { it.stages ?: emptyList() }
        .filter { it.stage == "DEEP" }
        .sumOf { s ->
            val ss = runCatching { OffsetDateTime.parse(s.stage_start) }.getOrNull()
            val se = runCatching { OffsetDateTime.parse(s.stage_end) }.getOrNull()
            if (ss != null && se != null) Duration.between(ss, se).toMinutes() else 0L
        }
    val deepPct = if (totalMin > 0 && deepMin > 0) (deepMin * 100 / totalMin).toInt() else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(durationText, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text("Coucher $bedTime", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Réveil $wakeTime", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
        if (deepPct != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("$deepPct% sommeil profond", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun HypnogramCanvas(
    sessions: List<SleepSessionResponse>,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface   = MaterialTheme.colorScheme.surface

    val sortedStarts = sessions.mapNotNull { runCatching { OffsetDateTime.parse(it.sleep_start) }.getOrNull() }
    val sortedEnds = sessions.mapNotNull { runCatching { OffsetDateTime.parse(it.sleep_end) }.getOrNull() }
    val start = sortedStarts.minOrNull()
    val end = sortedEnds.maxOrNull()
    val totalMs = if (start != null && end != null) Duration.between(start, end).toMillis() else 0L
    val allStages = sessions.flatMap { it.stages ?: emptyList() }.sortedBy { it.stage_start }

    var scrubX by remember { mutableStateOf<Float?>(null) }
    var canvasW by remember { mutableStateOf(0f) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(start, end, allStages.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset -> scrubX = offset.x.coerceIn(0f, canvasW) },
                        onDrag = { change, _ -> scrubX = change.position.x.coerceIn(0f, canvasW) },
                        onDragEnd = { scrubX = null },
                        onDragCancel = { scrubX = null },
                    )
                }
        ) {
            canvasW = size.width
            val canvasWidth = size.width
            val stageZoneH = 110.dp.toPx()
            val cornerR = 4.dp.toPx()

            if (totalMs <= 0 || allStages.isEmpty() || start == null || end == null) {
                drawRect(
                    color = surface.copy(alpha = 0.3f),
                    topLeft = Offset(0f, 0f),
                    size = Size(canvasWidth, stageZoneH),
                )
                return@Canvas
            }

            val firstStageStart = allStages.first().stage_start
            val lastStageEnd = allStages.last().stage_end
            for (stage in allStages) {
                val ss = runCatching { OffsetDateTime.parse(stage.stage_start) }.getOrNull() ?: continue
                val se = runCatching { OffsetDateTime.parse(stage.stage_end) }.getOrNull() ?: continue
                val stageDurMs = Duration.between(ss, se).toMillis()
                val offsetMs = Duration.between(start, ss).toMillis()
                val x = (offsetMs.toFloat() / totalMs) * canvasWidth
                val width = (stageDurMs.toFloat() / totalMs) * canvasWidth
                drawRoundedSegment(
                    color = stageColor(stage.stage),
                    left = x,
                    top = 0f,
                    width = width,
                    height = stageZoneH,
                    cornerRadius = cornerR,
                    roundLeft = stage.stage_start == firstStageStart,
                    roundRight = stage.stage_end == lastStageEnd,
                )
            }

            // Axe : 4 ticks fixes — coucher (0%), 33%, 66%, réveil (100%)
            drawAxisFourTicks(start, totalMs, canvasWidth, stageZoneH, onSurface)

            // Curseur de scrub
            scrubX?.let { x ->
                drawLine(
                    color = onSurface.copy(alpha = 0.7f),
                    start = Offset(x, 0f),
                    end = Offset(x, stageZoneH),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }

        // Tooltip flottant pendant le scrub
        scrubX?.let { x ->
            if (canvasW > 0 && totalMs > 0 && start != null) {
                val ratio = (x / canvasW).coerceIn(0f, 1f)
                val t = start.plus(Duration.ofMillis((totalMs * ratio).toLong()))
                val stage = stageAtTime(t, allStages)
                ScrubTooltip(
                    timeText = t.format(DateTimeFormatter.ofPattern("HH:mm")),
                    stageText = stage?.let { hypnoStageDisplayName(it.stage) } ?: "—",
                    stageDotColor = stage?.let { stageColor(it.stage) } ?: ColorLight,
                    xPx = x,
                    canvasWidthPx = canvasW,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAxisFourTicks(
    start: OffsetDateTime,
    totalMs: Long,
    canvasWidth: Float,
    stageZoneH: Float,
    onSurface: Color,
) {
    val paint = android.graphics.Paint().apply {
        textSize = 13.sp.toPx()
        color = onSurface.copy(alpha = 0.7f).toArgb()
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val ratios = listOf(0f, 1 / 3f, 2 / 3f, 1f)
    ratios.forEachIndexed { i, ratio ->
        val xTick = ratio * canvasWidth
        val t = start.plus(Duration.ofMillis((totalMs * ratio).toLong()))
        drawLine(
            color = onSurface.copy(alpha = 0.3f),
            start = Offset(xTick, stageZoneH - 5.dp.toPx()),
            end = Offset(xTick, stageZoneH),
            strokeWidth = 1.dp.toPx(),
        )
        // Aligne les labels aux extrémités pour éviter qu'ils dépassent l'écran
        paint.textAlign = when (i) {
            0 -> android.graphics.Paint.Align.LEFT
            ratios.lastIndex -> android.graphics.Paint.Align.RIGHT
            else -> android.graphics.Paint.Align.CENTER
        }
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                t.format(DateTimeFormatter.ofPattern("HH:mm")),
                xTick,
                stageZoneH + 22.dp.toPx(),
                paint,
            )
        }
    }
}

@Composable
private fun BoxScope.ScrubTooltip(
    timeText: String,
    stageText: String,
    stageDotColor: Color,
    xPx: Float,
    canvasWidthPx: Float,
) {
    val density = LocalDensity.current
    val approxTooltipWidthPx = with(density) { 160.dp.toPx() }
    val centeredX = (xPx - approxTooltipWidthPx / 2f).coerceIn(0f, canvasWidthPx - approxTooltipWidthPx)

    Surface(
        modifier = Modifier
            .offset { IntOffset(centeredX.toInt(), 0) }
            .align(Alignment.TopStart)
            .clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(stageDotColor),
            )
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stageText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundedSegment(
    color: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    cornerRadius: Float,
    roundLeft: Boolean,
    roundRight: Boolean,
) {
    if (width <= 0f) return
    val rect = androidx.compose.ui.geometry.Rect(left, top, left + width, top + height)
    if (!roundLeft && !roundRight) {
        drawRect(color, rect.topLeft, rect.size)
        return
    }
    val radius = cornerRadius.coerceAtMost(width / 2f).coerceAtMost(height / 2f)
    val path = androidx.compose.ui.graphics.Path().apply {
        if (roundLeft && roundRight) {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(rect, androidx.compose.ui.geometry.CornerRadius(radius))
            )
        } else if (roundLeft) {
            // Coins arrondis seulement à gauche, droits à droite
            moveTo(rect.left + radius, rect.top)
            lineTo(rect.right, rect.top)
            lineTo(rect.right, rect.bottom)
            lineTo(rect.left + radius, rect.bottom)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(rect.left, rect.bottom - 2 * radius, rect.left + 2 * radius, rect.bottom),
                startAngleDegrees = 90f, sweepAngleDegrees = 90f, forceMoveTo = false,
            )
            lineTo(rect.left, rect.top + radius)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(rect.left, rect.top, rect.left + 2 * radius, rect.top + 2 * radius),
                startAngleDegrees = 180f, sweepAngleDegrees = 90f, forceMoveTo = false,
            )
            close()
        } else {
            // Coins arrondis seulement à droite
            moveTo(rect.left, rect.top)
            lineTo(rect.right - radius, rect.top)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(rect.right - 2 * radius, rect.top, rect.right, rect.top + 2 * radius),
                startAngleDegrees = 270f, sweepAngleDegrees = 90f, forceMoveTo = false,
            )
            lineTo(rect.right, rect.bottom - radius)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(rect.right - 2 * radius, rect.bottom - 2 * radius, rect.right, rect.bottom),
                startAngleDegrees = 0f, sweepAngleDegrees = 90f, forceMoveTo = false,
            )
            lineTo(rect.left, rect.bottom)
            close()
        }
    }
    drawPath(path, color)
}

// ── Mountain canvas — graph 5 niveaux pour 1 hypnogramme zoomé ─────────────

private data class HypnoMountainSegment(
    val startMs: Long,
    val endMs: Long,
    val level: Int,
    val color: Color,
)

private data class HypnoMicroAwake(val tMs: Long)

private fun buildHypnoMountain(
    sessions: List<SleepSessionResponse>,
    sessionStart: OffsetDateTime,
): Pair<List<HypnoMountainSegment>, List<HypnoMicroAwake>> {
    val segments = mutableListOf<HypnoMountainSegment>()
    val ticks = mutableListOf<HypnoMicroAwake>()
    sessions.flatMap { it.stages ?: emptyList() }
        .sortedBy { it.stage_start }
        .forEach { stage ->
            val ss = runCatching { OffsetDateTime.parse(stage.stage_start) }.getOrNull() ?: return@forEach
            val se = runCatching { OffsetDateTime.parse(stage.stage_end) }.getOrNull() ?: return@forEach
            val durMin = Duration.between(ss, se).toMinutes()
            val startMs = Duration.between(sessionStart, ss).toMillis()
            val endMs = Duration.between(sessionStart, se).toMillis()
            if (stage.stage == "AWAKE" && durMin < HYPNO_MICRO_AWAKE_THRESHOLD_MIN) {
                ticks.add(HypnoMicroAwake(startMs))
            } else {
                segments.add(
                    HypnoMountainSegment(
                        startMs = startMs,
                        endMs = endMs,
                        level = hypnoMountainLevel(stage.stage),
                        color = stageColor(stage.stage),
                    )
                )
            }
        }
    return Pair(segments, ticks)
}

@Composable
private fun HypnogramMountainCanvas(
    sessions: List<SleepSessionResponse>,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val gridColor = onSurface.copy(alpha = 0.1f)
    val lineColor = onSurface.copy(alpha = 0.85f)
    val tickColor = ColorAwake.copy(alpha = 0.9f)

    val sortedStarts = sessions.mapNotNull { runCatching { OffsetDateTime.parse(it.sleep_start) }.getOrNull() }
    val sortedEnds = sessions.mapNotNull { runCatching { OffsetDateTime.parse(it.sleep_end) }.getOrNull() }
    val start = sortedStarts.minOrNull()
    val end = sortedEnds.maxOrNull()
    val totalMs = if (start != null && end != null) Duration.between(start, end).toMillis() else 0L
    val allStages = sessions.flatMap { it.stages ?: emptyList() }.sortedBy { it.stage_start }

    var scrubX by remember { mutableStateOf<Float?>(null) }
    var canvasW by remember { mutableStateOf(0f) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(start, end, allStages.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset -> scrubX = offset.x.coerceIn(0f, canvasW) },
                        onDrag = { change, _ -> scrubX = change.position.x.coerceIn(0f, canvasW) },
                        onDragEnd = { scrubX = null },
                        onDragCancel = { scrubX = null },
                    )
                }
        ) {
            canvasW = size.width
            val plotTop = 8.dp.toPx()
            val plotBot = size.height - 28.dp.toPx()
            val plotH = plotBot - plotTop

            if (start == null || end == null || totalMs <= 0L) return@Canvas

            fun yForLevel(level: Int): Float = plotBot - (level / 4f) * plotH
            fun xForMs(ms: Long): Float = (ms.toFloat() / totalMs) * canvasW

            val (segments, ticks) = buildHypnoMountain(sessions, start)
            val l0y = yForLevel(0)

            for (lvl in 0..4) {
                val y = yForLevel(lvl)
                drawLine(gridColor, Offset(0f, y), Offset(canvasW, y), strokeWidth = 1f)
            }

            segments.forEach { seg ->
                val x = xForMs(seg.startMs)
                val xEnd = xForMs(seg.endMs)
                val w = (xEnd - x).coerceAtLeast(1f)
                val y = yForLevel(seg.level)
                drawRect(seg.color.copy(alpha = 0.55f), Offset(x, y), Size(w, l0y - y))
            }

            if (segments.isNotEmpty()) {
                val path = androidx.compose.ui.graphics.Path()
                val first = segments.first()
                path.moveTo(xForMs(first.startMs), l0y)
                path.lineTo(xForMs(first.startMs), yForLevel(first.level))
                segments.forEachIndexed { idx, seg ->
                    val xEnd = xForMs(seg.endMs)
                    path.lineTo(xEnd, yForLevel(seg.level))
                    if (idx < segments.size - 1) {
                        val next = segments[idx + 1]
                        val xNextStart = xForMs(next.startMs)
                        if (xNextStart > xEnd) {
                            path.lineTo(xEnd, l0y)
                            path.lineTo(xNextStart, l0y)
                            path.lineTo(xNextStart, yForLevel(next.level))
                        } else {
                            path.lineTo(xEnd, yForLevel(next.level))
                        }
                    }
                }
                val last = segments.last()
                path.lineTo(xForMs(last.endMs), l0y)
                drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
            }

            ticks.forEach { tick ->
                val x = xForMs(tick.tMs)
                drawLine(tickColor, Offset(x, plotTop), Offset(x, l0y), strokeWidth = 1.4.dp.toPx())
            }

            // Axe X 4 ticks (coucher / 33% / 66% / réveil)
            val paint = android.graphics.Paint().apply {
                textSize = 13.sp.toPx()
                color = onSurface.copy(alpha = 0.7f).toArgb()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val ratios = listOf(0f, 1 / 3f, 2 / 3f, 1f)
            ratios.forEachIndexed { i, ratio ->
                val xTick = ratio * canvasW
                val t = start.plus(Duration.ofMillis((totalMs * ratio).toLong()))
                drawLine(onSurface.copy(alpha = 0.3f), Offset(xTick, plotBot - 4.dp.toPx()), Offset(xTick, plotBot), strokeWidth = 1.dp.toPx())
                paint.textAlign = when (i) {
                    0 -> android.graphics.Paint.Align.LEFT
                    ratios.lastIndex -> android.graphics.Paint.Align.RIGHT
                    else -> android.graphics.Paint.Align.CENTER
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        t.format(DateTimeFormatter.ofPattern("HH:mm")),
                        xTick,
                        plotBot + 18.dp.toPx(),
                        paint,
                    )
                }
            }

            // Curseur scrub
            scrubX?.let { x ->
                drawLine(
                    color = onSurface.copy(alpha = 0.7f),
                    start = Offset(x, plotTop),
                    end = Offset(x, plotBot),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
        }

        scrubX?.let { x ->
            if (canvasW > 0 && totalMs > 0 && start != null) {
                val ratio = (x / canvasW).coerceIn(0f, 1f)
                val t = start.plus(Duration.ofMillis((totalMs * ratio).toLong()))
                val stage = stageAtTime(t, allStages)
                ScrubTooltip(
                    timeText = t.format(DateTimeFormatter.ofPattern("HH:mm")),
                    stageText = stage?.let { hypnoStageDisplayName(it.stage) } ?: "—",
                    stageDotColor = stage?.let { stageColor(it.stage) } ?: ColorLight,
                    xPx = x,
                    canvasWidthPx = canvasW,
                )
            }
        }
    }
}

@Composable
private fun HypnogramLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendItem(color = ColorDeep,  label = "DEEP")
        LegendItem(color = ColorLight, label = "LIGHT")
        LegendItem(color = ColorRem,   label = "REM")
        LegendItem(color = ColorAwake, label = "AWAKE")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `HypnogramViewMode` (class) — lines 47-47
- `stageColor` (function) — lines 49-54
- `hypnoMountainLevel` (function) — lines 56-62
- `hypnoStageDisplayName` (function) — lines 64-70
- `stageAtTime` (function) — lines 76-83
- `nightTitle` (function) — lines 85-93
- `HypnogramScreen` (function) — lines 95-223
- `HypnogramSummarySection` (function) — lines 225-271
- `HypnogramCanvas` (function) — lines 273-369
- `drawAxisFourTicks` (function) — lines 371-408
- `ScrubTooltip` (function) — lines 410-454
- `drawRoundedSegment` (function) — lines 456-512
- `HypnoMountainSegment` (class) — lines 516-521
- `HypnoMicroAwake` (class) — lines 523-523
- `buildHypnoMountain` (function) — lines 525-553
- `HypnogramMountainCanvas` (function) — lines 555-696
- `yForLevel` (function) — lines 595-595
- `xForMs` (function) — lines 596-596
- `HypnogramLegend` (function) — lines 698-711
- `LegendItem` (function) — lines 713-724
