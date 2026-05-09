---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/TimelineScreen.kt
git_blob: a5bc9731bc6edb08faa180fa5c9cff38df8c62a3
last_synced: '2026-05-09T07:41:11Z'
loc: 500
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/TimelineScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/TimelineScreen.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/TimelineScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import fr.datasaillance.nightfall.data.sleep.SleepStageResponse
import fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState
import fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Constants ──────────────────────────────────────────────────────────────

private const val ROW_HEIGHT_DP    = 40
private const val BAR_HEIGHT_DP    = 24
private const val AXIS_BOTTOM_DP   = 24
private const val LABEL_WIDTH_DP   = 48
private const val LABEL_PADDING_DP = 4

// ── Stage colours ──────────────────────────────────────────────────────────

private val ColorDeep  = Color(0xFF0E9EB0)
private val ColorLight = Color(0xFF7A9AAA)
private val ColorRem   = Color(0xFF07BCD3)
private val ColorAwake = Color(0xFFD37C04)

private fun stageColor(type: String): Color = when (type) {
    "DEEP"  -> ColorDeep
    "LIGHT" -> ColorLight
    "REM"   -> ColorRem
    "AWAKE" -> ColorAwake
    else    -> ColorLight
}

private fun stageDisplayName(type: String): String = when (type) {
    "DEEP"  -> "Sommeil profond"
    "LIGHT" -> "Sommeil léger"
    "REM"   -> "Sommeil paradoxal"
    "AWAKE" -> "Éveil"
    else    -> type
}

// ── Window algorithm ──────────────────────────────────────────────────────

private fun computeTimelineWindow(sessions: List<SleepSessionResponse>): Pair<Int, Int> {
    val startMinutes = sessions.mapNotNull {
        runCatching { OffsetDateTime.parse(it.sleep_start) }.getOrNull()
    }.map { it.hour * 60 + it.minute }.sorted()
    val medianMinutes = if (startMinutes.isEmpty()) 22 * 60 else startMinutes[startMinutes.size / 2]
    val windowStart = (medianMinutes - 120).coerceAtLeast(0)
    val windowEnd   = (windowStart + 960).coerceAtMost(1440)
    return Pair(windowStart, windowEnd)
}

// ── Night aggregation — 1 row = 1 calendar date (keyed by sleep_start.date) ──

private fun groupByNight(sessions: List<SleepSessionResponse>): List<Pair<LocalDate, List<SleepSessionResponse>>> =
    sessions
        .groupBy { runCatching { OffsetDateTime.parse(it.sleep_start).toLocalDate() }.getOrNull() }
        .filterKeys { it != null }
        .mapKeys { it.key!! }
        .entries
        .sortedBy { it.key }
        .map { (date, list) -> date to list }

// ── Hit-box for tap detection on stage segments ────────────────────────────

private data class StageHitBox(
    val left: Float, val top: Float, val right: Float, val bottom: Float,
    val stageType: String,
    val stageStart: String,
    val stageEnd: String,
    val nightLabel: String
)

// ── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedStage by remember { mutableStateOf<StageHitBox?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Drift circadien", style = MaterialTheme.typography.headlineMedium) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (val state = uiState) {
                    is TimelineUiState.Idle -> {}

                    is TimelineUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize().testTag("tl_loading"),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    is TimelineUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize().testTag("tl_error"),
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
                                    modifier = Modifier.testTag("tl_retry")
                                ) {
                                    Text("Réessayer")
                                }
                            }
                        }
                    }

                    is TimelineUiState.Empty -> {
                        Box(
                            modifier = Modifier.fillMaxSize().testTag("tl_empty"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucune nuit enregistrée",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    is TimelineUiState.Success -> {
                        val nights = remember(state.sessions) { groupByNight(state.sessions) }
                        val (windowStart, windowEnd) = remember(state.sessions) {
                            computeTimelineWindow(state.sessions)
                        }
                        val canvasHeightDp = (nights.size * ROW_HEIGHT_DP + AXIS_BOTTOM_DP).dp

                        Column(
                            modifier = Modifier.fillMaxSize().testTag("tl_screen")
                        ) {
                            TimelineAxisHeader(windowStart = windowStart, windowEnd = windowEnd)
                            Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                TimelineCanvas(
                                    nights      = nights,
                                    windowStart = windowStart,
                                    windowEnd   = windowEnd,
                                    onStageTap  = { selectedStage = it },
                                    modifier    = Modifier
                                        .fillMaxWidth()
                                        .height(canvasHeightDp)
                                        .testTag("tl_canvas")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Bottom sheet — stage detail ────────────────────────────────────────
    selectedStage?.let { stage ->
        ModalBottomSheet(
            onDismissRequest = { selectedStage = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            StageDetailSheet(stage = stage)
        }
    }
}

// ── Axis header ─────────────────────────────────────────────────────────────

@Composable
private fun TimelineAxisHeader(
    windowStart: Int,
    windowEnd: Int,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val windowDuration = (windowEnd - windowStart).takeIf { it > 0 } ?: return

    Canvas(
        modifier = modifier.fillMaxWidth().height(AXIS_BOTTOM_DP.dp)
    ) {
        val canvasW   = size.width
        val labelPx   = LABEL_WIDTH_DP.dp.toPx()
        val drawableW = canvasW - labelPx
        val paint = android.graphics.Paint().apply {
            textSize  = 9.sp.toPx()
            color     = onSurface.copy(alpha = 0.6f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val step = if (windowDuration >= 720) 2 else 1
        var h = windowStart / 60
        val endHour = (windowEnd + 59) / 60
        while (h <= endHour) {
            val hMin = h * 60
            if (hMin in windowStart..windowEnd) {
                val x = labelPx + ((hMin - windowStart).toFloat() / windowDuration) * drawableW
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText("%02d:00".format(h % 24), x, size.height * 0.8f, paint)
                }
            }
            h += step
        }
    }
}

// ── Canvas ──────────────────────────────────────────────────────────────────

@Composable
private fun TimelineCanvas(
    nights: List<Pair<LocalDate, List<SleepSessionResponse>>>,
    windowStart: Int,
    windowEnd: Int,
    onStageTap: (StageHitBox) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val windowDuration = (windowEnd - windowStart).takeIf { it > 0 } ?: return
    val hitBoxes = remember { mutableListOf<StageHitBox>() }
    val dateFmt  = remember { DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH) }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                hitBoxes.firstOrNull { box ->
                    offset.x >= box.left && offset.x <= box.right &&
                    offset.y >= box.top  && offset.y <= box.bottom
                }?.let { onStageTap(it) }
            }
        }
    ) {
        hitBoxes.clear()

        val canvasW   = size.width
        val labelPx   = LABEL_WIDTH_DP.dp.toPx()
        val drawableW = canvasW - labelPx
        val rowH      = ROW_HEIGHT_DP.dp.toPx()
        val barH      = BAR_HEIGHT_DP.dp.toPx()
        val axisBot   = AXIS_BOTTOM_DP.dp.toPx()

        val datePaint = android.graphics.Paint().apply {
            textSize  = 10.sp.toPx()
            color     = onSurface.copy(alpha = 0.6f).toArgb()
            textAlign = android.graphics.Paint.Align.LEFT
        }
        val tickPaint = android.graphics.Paint().apply {
            textSize  = 9.sp.toPx()
            color     = onSurface.copy(alpha = 0.5f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }

        nights.forEachIndexed { rowIndex, (date, sessions) ->
            val rowTop = rowIndex * rowH
            val barTop = rowTop + (rowH - barH) / 2f
            val label  = date.format(dateFmt)

            // Date label
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    label,
                    LABEL_PADDING_DP.dp.toPx(),
                    rowTop + rowH / 2f + datePaint.textSize / 3f,
                    datePaint
                )
            }

            // Draw stages for all sessions of this night
            sessions.forEach { session ->
                val stages = session.stages
                if (stages.isNullOrEmpty()) {
                    // Fallback: single bar spanning sleep duration
                    drawFallbackBar(
                        session      = session,
                        windowStart  = windowStart,
                        windowDuration = windowDuration,
                        labelPx      = labelPx,
                        drawableW    = drawableW,
                        barTop       = barTop,
                        barH         = barH,
                        nightLabel   = label,
                        hitBoxes     = hitBoxes
                    )
                } else {
                    stages.sortedBy { it.stage_start }.forEach { stage ->
                        drawStageSegment(
                            stage        = stage,
                            windowStart  = windowStart,
                            windowDuration = windowDuration,
                            labelPx      = labelPx,
                            drawableW    = drawableW,
                            barTop       = barTop,
                            barH         = barH,
                            nightLabel   = label,
                            hitBoxes     = hitBoxes
                        )
                    }
                }
            }
        }

        // X-axis hour ticks at bottom
        val axisTopY = size.height - axisBot
        val step = if (windowDuration >= 720) 2 else 1
        var h = windowStart / 60
        val endHour = (windowEnd + 59) / 60
        while (h <= endHour) {
            val hMin = h * 60
            if (hMin in windowStart..windowEnd) {
                val x = labelPx + ((hMin - windowStart).toFloat() / windowDuration) * drawableW
                drawLine(
                    color       = onSurface.copy(alpha = 0.2f),
                    start       = Offset(x, axisTopY),
                    end         = Offset(x, axisTopY + 4.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "%02d:00".format(h % 24),
                        x,
                        size.height - 2.dp.toPx(),
                        tickPaint
                    )
                }
            }
            h += step
        }
    }
}

// ── Drawing helpers (pure DrawScope calls extracted for readability) ─────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallbackBar(
    session: SleepSessionResponse,
    windowStart: Int,
    windowDuration: Int,
    labelPx: Float,
    drawableW: Float,
    barTop: Float,
    barH: Float,
    nightLabel: String,
    hitBoxes: MutableList<StageHitBox>
) {
    val start = runCatching { OffsetDateTime.parse(session.sleep_start) }.getOrNull() ?: return
    val end   = runCatching { OffsetDateTime.parse(session.sleep_end) }.getOrNull() ?: return
    var startMin = start.hour * 60 + start.minute
    var endMin   = end.hour * 60 + end.minute
    if (endMin < startMin) endMin += 1440

    val startOff = (startMin - windowStart).coerceIn(0, windowDuration)
    val endOff   = (endMin   - windowStart).coerceIn(0, windowDuration)
    if (endOff <= startOff) return

    val x    = (labelPx + (startOff.toFloat() / windowDuration) * drawableW).coerceIn(labelPx, labelPx + drawableW)
    val xEnd = (labelPx + (endOff.toFloat()   / windowDuration) * drawableW).coerceIn(labelPx, labelPx + drawableW)
    val barW = xEnd - x
    if (barW <= 0f) return

    drawRect(color = ColorDeep.copy(alpha = 0.6f), topLeft = Offset(x, barTop), size = Size(barW, barH))
    hitBoxes.add(StageHitBox(x, barTop, xEnd, barTop + barH, "LIGHT",
        session.sleep_start, session.sleep_end, nightLabel))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStageSegment(
    stage: SleepStageResponse,
    windowStart: Int,
    windowDuration: Int,
    labelPx: Float,
    drawableW: Float,
    barTop: Float,
    barH: Float,
    nightLabel: String,
    hitBoxes: MutableList<StageHitBox>
) {
    val ss = runCatching { OffsetDateTime.parse(stage.stage_start) }.getOrNull() ?: return
    val se = runCatching { OffsetDateTime.parse(stage.stage_end) }.getOrNull() ?: return
    var startMin = ss.hour * 60 + ss.minute
    var endMin   = se.hour * 60 + se.minute
    if (endMin < startMin) endMin += 1440

    val startOff = (startMin - windowStart).coerceIn(0, windowDuration)
    val endOff   = (endMin   - windowStart).coerceIn(0, windowDuration)
    if (endOff <= startOff) return

    val x    = (labelPx + (startOff.toFloat() / windowDuration) * drawableW).coerceIn(labelPx, labelPx + drawableW)
    val xEnd = (labelPx + (endOff.toFloat()   / windowDuration) * drawableW).coerceIn(labelPx, labelPx + drawableW)
    val barW = xEnd - x
    if (barW <= 0f) return

    // AWAKE segments are shorter to distinguish them visually
    val segH  = if (stage.stage == "AWAKE") barH * 0.5f else barH
    val segTop = barTop + (barH - segH) / 2f

    drawRect(
        color   = stageColor(stage.stage),
        topLeft = Offset(x, segTop),
        size    = Size(barW, segH)
    )
    hitBoxes.add(StageHitBox(x, segTop, xEnd, segTop + segH,
        stage.stage, stage.stage_start, stage.stage_end, nightLabel))
}

// ── Bottom sheet detail ──────────────────────────────────────────────────────

@Composable
private fun StageDetailSheet(stage: StageHitBox) {
    val start = runCatching { OffsetDateTime.parse(stage.stageStart) }.getOrNull()
    val end   = runCatching { OffsetDateTime.parse(stage.stageEnd) }.getOrNull()
    val duration = if (start != null && end != null) Duration.between(start, end) else null
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stage.nightLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(stageColor(stage.stageType))
            )
            Text(
                text = stageDisplayName(stage.stageType),
                style = MaterialTheme.typography.titleMedium,
                color = stageColor(stage.stageType)
            )
        }
        if (duration != null) {
            val h  = duration.toHours()
            val mm = duration.toMinutes() % 60
            Text(
                text = if (h > 0) "${h}h ${mm.toString().padStart(2, '0')}min" else "${mm}min",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (start != null && end != null) {
            Text(
                text = "${start.format(timeFmt)} → ${end.format(timeFmt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `stageColor` (function) — lines 50-56
- `stageDisplayName` (function) — lines 58-64
- `computeTimelineWindow` (function) — lines 68-76
- `groupByNight` (function) — lines 80-87
- `StageHitBox` (class) — lines 91-97
- `TimelineScreen` (function) — lines 101-214
- `TimelineAxisHeader` (function) — lines 218-252
- `TimelineCanvas` (function) — lines 256-375
- `drawFallbackBar` (function) — lines 379-408
- `drawStageSegment` (function) — lines 410-447
- `StageDetailSheet` (function) — lines 451-500
