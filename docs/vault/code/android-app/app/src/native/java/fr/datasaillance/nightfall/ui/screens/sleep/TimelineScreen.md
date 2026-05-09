---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/TimelineScreen.kt
git_blob: 14cb4f8d5d2c6d528f8d812cc6247699644399c9
last_synced: '2026-05-09T14:31:05Z'
loc: 783
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
private const val MOUNTAIN_ROW_HEIGHT_DP = 64
private const val AXIS_BOTTOM_DP   = 24
private const val LABEL_WIDTH_DP   = 48
private const val LABEL_PADDING_DP = 4
// Fenêtre fixe 24h pour Non-24 — les heures de sommeil glissent partout.
private const val WINDOW_START_MIN = 0
private const val WINDOW_END_MIN   = 1440
// Grille verticale toutes les 3h pour rester lisible sur un écran portrait.
private const val GRID_STEP_HOURS  = 3
// Micro-réveil < ce seuil = trait vertical, pas de creux dans la mountain.
private const val MICRO_AWAKE_THRESHOLD_MIN = 5

enum class TimelineViewMode { BARS, MOUNTAIN }

// Mountain : 5 niveaux (0 = éveil session, 1 = AWAKE intra, 2 = DEEP, 3 = LIGHT, 4 = REM)
private fun mountainLevel(stage: String): Int = when (stage) {
    "REM"   -> 4
    "LIGHT" -> 3
    "DEEP"  -> 2
    "AWAKE" -> 1
    else    -> 0
}

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

// ── Night aggregation — 1 row = 1 calendar date (keyed by sleep_start.date) ──

private fun groupByNight(sessions: List<SleepSessionResponse>): List<Pair<LocalDate, List<SleepSessionResponse>>> =
    sessions
        .groupBy { runCatching { OffsetDateTime.parse(it.sleep_start).toLocalDate() }.getOrNull() }
        .filterKeys { it != null }
        .mapKeys { it.key!! }
        .entries
        .sortedBy { it.key }
        .map { (date, list) -> date to list }

// ── Selection model — un click ouvre le détail d'UNE nuit complète ─────────

private data class NightSelection(
    val date: LocalDate,
    val sessions: List<SleepSessionResponse>,
)

// ── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onOpenHypnogram: (sessionId: String, isoDate: String?) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNight by remember { mutableStateOf<NightSelection?>(null) }
    var viewMode by remember { mutableStateOf(TimelineViewMode.BARS) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Drift circadien", style = MaterialTheme.typography.headlineMedium) },
                    actions = {
                        TextButton(
                            onClick = {
                                viewMode = if (viewMode == TimelineViewMode.BARS) TimelineViewMode.MOUNTAIN else TimelineViewMode.BARS
                            },
                            modifier = Modifier.testTag("tl_view_toggle"),
                        ) {
                            Text(
                                text = if (viewMode == TimelineViewMode.BARS) "Mountain" else "Barres",
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
                        val listState = rememberLazyListState()
                        var initialScrollDone by remember { mutableStateOf(false) }

                        // 1er rendu : scroll au bas de la liste pour afficher la nuit la plus récente.
                        LaunchedEffect(nights.size, initialScrollDone) {
                            if (!initialScrollDone && nights.isNotEmpty()) {
                                listState.scrollToItem(nights.size) // +1 sentinel offset → last index
                                initialScrollDone = true
                            }
                        }

                        // Sentinel : quand l'utilisateur scrolle jusqu'en haut de la liste, on charge
                        // 30 jours plus anciens. distinctUntilChanged évite les triggers répétés
                        // pendant que la pagination est déjà en cours.
                        LaunchedEffect(listState, state.hasMore, state.loadingMore) {
                            snapshotFlow { listState.firstVisibleItemIndex }
                                .distinctUntilChanged()
                                .filter { it == 0 && state.hasMore && !state.loadingMore && initialScrollDone }
                                .collect { viewModel.loadOlder() }
                        }

                        Column(
                            modifier = Modifier.fillMaxSize().testTag("tl_screen")
                        ) {
                            TimelineAxisHeader()
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().testTag("tl_list")
                            ) {
                                item(key = "load_older_sentinel") {
                                    LoadOlderSentinel(
                                        loadingMore = state.loadingMore,
                                        hasMore = state.hasMore,
                                    )
                                }
                                items(nights, key = { (date, _) -> date.toEpochDay() }) { (date, sessions) ->
                                    when (viewMode) {
                                        TimelineViewMode.BARS -> NightRow(
                                            date = date,
                                            sessions = sessions,
                                            onClick = { selectedNight = NightSelection(date, sessions) },
                                        )
                                        TimelineViewMode.MOUNTAIN -> NightRowMountain(
                                            date = date,
                                            sessions = sessions,
                                            onClick = { selectedNight = NightSelection(date, sessions) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Bottom sheet — night detail (toutes sessions de la nuit) ───────────
    selectedNight?.let { night ->
        ModalBottomSheet(
            onDismissRequest = { selectedNight = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NightDetailSheet(
                night = night,
                onOpenHypnogram = { sessionId ->
                    val iso = night.date.toString()
                    selectedNight = null
                    onOpenHypnogram(sessionId, iso)
                },
            )
        }
    }
}

// ── Axis header — 24h fixe avec ticks toutes les GRID_STEP_HOURS heures ────

@Composable
private fun TimelineAxisHeader(modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val windowDuration = WINDOW_END_MIN - WINDOW_START_MIN

    Canvas(
        modifier = modifier.fillMaxWidth().height(AXIS_BOTTOM_DP.dp)
    ) {
        val canvasW = size.width
        val labelPx = LABEL_WIDTH_DP.dp.toPx()
        val drawableW = canvasW - labelPx
        val paint = android.graphics.Paint().apply {
            textSize = 9.sp.toPx()
            color = onSurface.copy(alpha = 0.7f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }
        var h = 0
        while (h <= 24) {
            val hMin = h * 60
            val x = labelPx + (hMin.toFloat() / windowDuration) * drawableW
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("%02dh".format(h % 24), x, size.height * 0.8f, paint)
            }
            h += GRID_STEP_HOURS
        }
    }
}

// ── Sentinel (top of LazyColumn) ────────────────────────────────────────────

@Composable
private fun LoadOlderSentinel(
    loadingMore: Boolean,
    hasMore: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("tl_sentinel"),
        contentAlignment = Alignment.Center
    ) {
        when {
            loadingMore -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            !hasMore -> Text(
                text = "Aucune donnée plus ancienne",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
            else -> Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ── Single night row (1 Canvas par nuit, lazy-loaded) ───────────────────────

@Composable
private fun NightRow(
    date: LocalDate,
    sessions: List<SleepSessionResponse>,
    onClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val windowDuration = WINDOW_END_MIN - WINDOW_START_MIN
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH) }
    val label = remember(date) { date.format(dateFmt) }
    val gridColor = onSurface.copy(alpha = 0.08f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT_DP.dp)
            .pointerInput(date) {
                detectTapGestures { onClick() }
            }
    ) {
        val canvasW = size.width
        val labelPx = LABEL_WIDTH_DP.dp.toPx()
        val drawableW = canvasW - labelPx
        val rowH = ROW_HEIGHT_DP.dp.toPx()
        val barH = BAR_HEIGHT_DP.dp.toPx()
        val barTop = (rowH - barH) / 2f

        val datePaint = android.graphics.Paint().apply {
            textSize = 10.sp.toPx()
            color = onSurface.copy(alpha = 0.6f).toArgb()
            textAlign = android.graphics.Paint.Align.LEFT
        }

        // Grid verticale en fond — toutes les GRID_STEP_HOURS heures.
        var h = 0
        while (h <= 24) {
            val x = labelPx + (h * 60f / windowDuration) * drawableW
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, rowH),
                strokeWidth = 1f,
            )
            h += GRID_STEP_HOURS
        }

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                label,
                LABEL_PADDING_DP.dp.toPx(),
                rowH / 2f + datePaint.textSize / 3f,
                datePaint,
            )
        }

        sessions.forEach { session ->
            val stages = session.stages?.sortedBy { it.stage_start } ?: emptyList()
            if (stages.isEmpty()) {
                drawFallbackBar(
                    session = session,
                    labelPx = labelPx,
                    drawableW = drawableW,
                    barTop = barTop,
                    barH = barH,
                )
            } else {
                stages.forEachIndexed { idx, stage ->
                    drawStageSegment(
                        stage = stage,
                        labelPx = labelPx,
                        drawableW = drawableW,
                        barTop = barTop,
                        barH = barH,
                        roundLeft = idx == 0,
                        roundRight = idx == stages.size - 1,
                    )
                }
            }
        }
    }
}

// ── Mountain view — graph "ligne montagne" 5 niveaux par nuit ───────────────

private data class MountainSegment(
    val startMin: Int,
    val endMin: Int,
    val level: Int,
    val color: Color,
)

private data class MicroAwakeMark(val minute: Int)

/**
 * Convertit les sessions d'une nuit en segments mountain :
 * - micro-AWAKE (<5min) → MicroAwakeMark, le segment courant continue à son niveau
 * - AWAKE >=5min → segment à L1 avec couleur AWAKE
 * - DEEP/LIGHT/REM → segment à leur niveau (2/3/4) avec leur couleur
 *
 * Entre 2 sessions consécutives, le path retombe à L0 puis remonte.
 */
private fun buildMountainData(
    sessions: List<SleepSessionResponse>,
): Pair<List<MountainSegment>, List<MicroAwakeMark>> {
    val segments = mutableListOf<MountainSegment>()
    val ticks = mutableListOf<MicroAwakeMark>()

    sessions.forEach { session ->
        val sortedStages = session.stages?.sortedBy { it.stage_start } ?: return@forEach
        sortedStages.forEach { stage ->
            val ss = runCatching { OffsetDateTime.parse(stage.stage_start) }.getOrNull() ?: return@forEach
            val se = runCatching { OffsetDateTime.parse(stage.stage_end) }.getOrNull() ?: return@forEach
            val durationMin = Duration.between(ss, se).toMinutes()
            val startMin = ss.hour * 60 + ss.minute
            var endMin = se.hour * 60 + se.minute
            if (endMin < startMin) endMin += 1440

            if (stage.stage == "AWAKE" && durationMin < MICRO_AWAKE_THRESHOLD_MIN) {
                ticks.add(MicroAwakeMark(startMin))
            } else {
                segments.add(
                    MountainSegment(
                        startMin = startMin,
                        endMin = endMin,
                        level = mountainLevel(stage.stage),
                        color = stageColor(stage.stage),
                    )
                )
            }
        }
    }
    return Pair(segments, ticks)
}

@Composable
private fun NightRowMountain(
    date: LocalDate,
    sessions: List<SleepSessionResponse>,
    onClick: () -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val windowDuration = WINDOW_END_MIN - WINDOW_START_MIN
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH) }
    val label = remember(date) { date.format(dateFmt) }
    val gridColor = onSurface.copy(alpha = 0.08f)
    val lineColor = onSurface.copy(alpha = 0.85f)
    val tickColor = ColorAwake.copy(alpha = 0.9f)

    val (segments, ticks) = remember(sessions) { buildMountainData(sessions) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(MOUNTAIN_ROW_HEIGHT_DP.dp)
            .pointerInput(date) { detectTapGestures { onClick() } }
    ) {
        val canvasW = size.width
        val labelPx = LABEL_WIDTH_DP.dp.toPx()
        val drawableW = canvasW - labelPx
        val rowH = MOUNTAIN_ROW_HEIGHT_DP.dp.toPx()
        val plotTop = 6.dp.toPx()
        val plotBot = rowH - 6.dp.toPx()
        val plotH = plotBot - plotTop
        // 5 paliers (L0..L4) → 4 intervalles
        fun yForLevel(level: Int): Float = plotBot - (level / 4f) * plotH

        val datePaint = android.graphics.Paint().apply {
            textSize = 10.sp.toPx()
            color = onSurface.copy(alpha = 0.6f).toArgb()
            textAlign = android.graphics.Paint.Align.LEFT
        }

        // Grid verticale (heures)
        var h = 0
        while (h <= 24) {
            val x = labelPx + (h * 60f / windowDuration) * drawableW
            drawLine(gridColor, Offset(x, 0f), Offset(x, rowH), strokeWidth = 1f)
            h += GRID_STEP_HOURS
        }

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                label,
                LABEL_PADDING_DP.dp.toPx(),
                rowH / 2f + datePaint.textSize / 3f,
                datePaint,
            )
        }

        fun xForMin(min: Int): Float =
            labelPx + (min.coerceIn(WINDOW_START_MIN, WINDOW_END_MIN).toFloat() / windowDuration) * drawableW

        // Aire colorée sous la ligne — un rect par segment, du niveau jusqu'à L0
        val l0y = yForLevel(0)
        segments.forEach { seg ->
            val x = xForMin(seg.startMin)
            val xEnd = xForMin(seg.endMin)
            val w = (xEnd - x).coerceAtLeast(1f)
            val y = yForLevel(seg.level)
            drawRect(
                color = seg.color.copy(alpha = 0.55f),
                topLeft = Offset(x, y),
                size = Size(w, l0y - y),
            )
        }

        // Polyline reliant les niveaux — démarre à L0, suit les segments, retombe à L0
        if (segments.isNotEmpty()) {
            val path = androidx.compose.ui.graphics.Path()
            // Démarre à L0 au début du 1er segment
            val first = segments.first()
            path.moveTo(xForMin(first.startMin), l0y)
            // Monte au niveau du 1er segment
            path.lineTo(xForMin(first.startMin), yForLevel(first.level))
            // Trace tous les segments
            segments.forEachIndexed { idx, seg ->
                val xStart = xForMin(seg.startMin)
                val xEnd = xForMin(seg.endMin)
                val y = yForLevel(seg.level)
                path.lineTo(xEnd, y)
                // Transition vers le segment suivant : on saute à son niveau au moment de son début
                if (idx < segments.size - 1) {
                    val next = segments[idx + 1]
                    val xNextStart = xForMin(next.startMin)
                    if (xNextStart > xEnd) {
                        // Gap entre segments → retombe à L0
                        path.lineTo(xEnd, l0y)
                        path.lineTo(xNextStart, l0y)
                        path.lineTo(xNextStart, yForLevel(next.level))
                    } else {
                        // Continue directement au niveau suivant
                        path.lineTo(xEnd, yForLevel(next.level))
                    }
                }
            }
            // Fin : retombe à L0
            val last = segments.last()
            path.lineTo(xForMin(last.endMin), l0y)
            drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx()))
        }

        // Traits verticaux pour micro-awakes
        ticks.forEach { tick ->
            val x = xForMin(tick.minute)
            drawLine(
                color = tickColor,
                start = Offset(x, plotTop),
                end = Offset(x, l0y),
                strokeWidth = 1.2.dp.toPx(),
            )
        }
    }
}

// ── Drawing helpers (pure DrawScope calls extracted for readability) ─────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallbackBar(
    session: SleepSessionResponse,
    labelPx: Float,
    drawableW: Float,
    barTop: Float,
    barH: Float,
) {
    val start = runCatching { OffsetDateTime.parse(session.sleep_start) }.getOrNull() ?: return
    val end = runCatching { OffsetDateTime.parse(session.sleep_end) }.getOrNull() ?: return
    val startMin = start.hour * 60 + start.minute
    var endMin = end.hour * 60 + end.minute
    if (endMin < startMin) endMin += 1440 // session traverse minuit
    drawBar(startMin, endMin, ColorDeep.copy(alpha = 0.6f), labelPx, drawableW, barTop, barH, roundLeft = true, roundRight = true)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStageSegment(
    stage: SleepStageResponse,
    labelPx: Float,
    drawableW: Float,
    barTop: Float,
    barH: Float,
    roundLeft: Boolean,
    roundRight: Boolean,
) {
    val ss = runCatching { OffsetDateTime.parse(stage.stage_start) }.getOrNull() ?: return
    val se = runCatching { OffsetDateTime.parse(stage.stage_end) }.getOrNull() ?: return
    val startMin = ss.hour * 60 + ss.minute
    var endMin = se.hour * 60 + se.minute
    if (endMin < startMin) endMin += 1440
    drawBar(startMin, endMin, stageColor(stage.stage), labelPx, drawableW, barTop, barH, roundLeft, roundRight)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBar(
    startMin: Int,
    endMin: Int,
    color: Color,
    labelPx: Float,
    drawableW: Float,
    barTop: Float,
    barH: Float,
    roundLeft: Boolean = false,
    roundRight: Boolean = false,
) {
    val windowDuration = WINDOW_END_MIN - WINDOW_START_MIN
    val s = startMin.coerceIn(WINDOW_START_MIN, WINDOW_END_MIN)
    val e = endMin.coerceIn(WINDOW_START_MIN, WINDOW_END_MIN)
    if (e <= s) return
    val x = labelPx + (s.toFloat() / windowDuration) * drawableW
    val xEnd = labelPx + (e.toFloat() / windowDuration) * drawableW
    val barW = (xEnd - x).coerceAtLeast(1.5f)
    if (!roundLeft && !roundRight) {
        drawRect(color = color, topLeft = Offset(x, barTop), size = Size(barW, barH))
        return
    }
    val cornerR = 3.dp.toPx().coerceAtMost(barW / 2f).coerceAtMost(barH / 2f)
    val rect = androidx.compose.ui.geometry.Rect(x, barTop, x + barW, barTop + barH)
    val path = androidx.compose.ui.graphics.Path().apply {
        if (roundLeft && roundRight) {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, androidx.compose.ui.geometry.CornerRadius(cornerR)))
        } else if (roundLeft) {
            moveTo(rect.left + cornerR, rect.top)
            lineTo(rect.right, rect.top)
            lineTo(rect.right, rect.bottom)
            lineTo(rect.left + cornerR, rect.bottom)
            arcTo(androidx.compose.ui.geometry.Rect(rect.left, rect.bottom - 2 * cornerR, rect.left + 2 * cornerR, rect.bottom), 90f, 90f, false)
            lineTo(rect.left, rect.top + cornerR)
            arcTo(androidx.compose.ui.geometry.Rect(rect.left, rect.top, rect.left + 2 * cornerR, rect.top + 2 * cornerR), 180f, 90f, false)
            close()
        } else {
            moveTo(rect.left, rect.top)
            lineTo(rect.right - cornerR, rect.top)
            arcTo(androidx.compose.ui.geometry.Rect(rect.right - 2 * cornerR, rect.top, rect.right, rect.top + 2 * cornerR), 270f, 90f, false)
            lineTo(rect.right, rect.bottom - cornerR)
            arcTo(androidx.compose.ui.geometry.Rect(rect.right - 2 * cornerR, rect.bottom - 2 * cornerR, rect.right, rect.bottom), 0f, 90f, false)
            lineTo(rect.left, rect.bottom)
            close()
        }
    }
    drawPath(path, color)
}

// ── Bottom sheet detail ──────────────────────────────────────────────────────

@Composable
private fun NightDetailSheet(
    night: NightSelection,
    onOpenHypnogram: (sessionId: String) -> Unit,
) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH) }

    val totalSleep = remember(night) {
        night.sessions.sumOf { s ->
            val start = runCatching { OffsetDateTime.parse(s.sleep_start) }.getOrNull()
            val end = runCatching { OffsetDateTime.parse(s.sleep_end) }.getOrNull()
            if (start != null && end != null) Duration.between(start, end).toMinutes() else 0L
        }
    }
    val stageTotals = remember(night) {
        val acc = mutableMapOf<String, Long>()
        night.sessions.forEach { s ->
            s.stages?.forEach { st ->
                val start = runCatching { OffsetDateTime.parse(st.stage_start) }.getOrNull()
                val end = runCatching { OffsetDateTime.parse(st.stage_end) }.getOrNull()
                if (start != null && end != null) {
                    acc[st.stage] = (acc[st.stage] ?: 0L) + Duration.between(start, end).toMinutes()
                }
            }
        }
        acc
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = night.date.format(dateFmt).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = "Sommeil total : ${formatDuration(totalSleep)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (stageTotals.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("DEEP", "LIGHT", "REM", "AWAKE").forEach { stageType ->
                    val mins = stageTotals[stageType] ?: 0L
                    if (mins > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(stageColor(stageType)),
                            )
                            Text(
                                text = stageDisplayName(stageType),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = formatDuration(mins),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "${night.sessions.size} session${if (night.sessions.size > 1) "s" else ""} de sommeil",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        // Lien vers l'hypnogramme — une session = un bouton.
        night.sessions.forEachIndexed { idx, session ->
            val start = runCatching { OffsetDateTime.parse(session.sleep_start) }.getOrNull()
            val end = runCatching { OffsetDateTime.parse(session.sleep_end) }.getOrNull()
            val rangeLabel = if (start != null && end != null) {
                "${start.format(timeFmt)} → ${end.format(timeFmt)}"
            } else "Session ${idx + 1}"
            OutlinedButton(
                onClick = { onOpenHypnogram(session.id) },
                modifier = Modifier.fillMaxWidth().testTag("tl_open_hypnogram_$idx"),
            ) {
                Text("Hypnogramme · $rangeLabel")
            }
        }
    }
}

private fun formatDuration(minutes: Long): String {
    if (minutes <= 0) return "—"
    val h = minutes / 60
    val mm = minutes % 60
    return if (h > 0) "${h}h ${mm.toString().padStart(2, '0')}min" else "${mm}min"
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TimelineViewMode` (class) — lines 54-54
- `mountainLevel` (function) — lines 57-63
- `stageColor` (function) — lines 72-78
- `stageDisplayName` (function) — lines 80-86
- `groupByNight` (function) — lines 90-97
- `NightSelection` (class) — lines 101-104
- `TimelineScreen` (function) — lines 108-272
- `TimelineAxisHeader` (function) — lines 276-302
- `LoadOlderSentinel` (function) — lines 306-332
- `NightRow` (function) — lines 336-416
- `MountainSegment` (class) — lines 420-425
- `MicroAwakeMark` (class) — lines 427-427
- `buildMountainData` (function) — lines 437-468
- `NightRowMountain` (function) — lines 470-588
- `yForLevel` (function) — lines 500-500
- `xForMin` (function) — lines 525-526
- `drawFallbackBar` (function) — lines 592-605
- `drawStageSegment` (function) — lines 607-622
- `drawBar` (function) — lines 624-671
- `NightDetailSheet` (function) — lines 675-776
- `formatDuration` (function) — lines 778-783
