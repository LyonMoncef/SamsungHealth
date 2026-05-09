package fr.datasaillance.nightfall.ui.screens.sleep

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import fr.datasaillance.nightfall.viewmodel.sleep.TimelineUiState
import fr.datasaillance.nightfall.viewmodel.sleep.TimelineViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val ROW_HEIGHT_DP    = 40
private const val BAR_HEIGHT_DP    = 24
private const val AXIS_BOTTOM_DP   = 24
private const val LABEL_WIDTH_DP   = 48
private const val LABEL_PADDING_DP = 4

private val ColorSleepBar = Color(0xFF0E9EB0)

private fun computeTimelineWindow(sessions: List<SleepSessionResponse>): Pair<Int, Int> {
    val startMinutes = sessions.mapNotNull {
        runCatching { OffsetDateTime.parse(it.sleep_start) }.getOrNull()
    }.map { it.hour * 60 + it.minute }.sorted()

    val medianMinutes = if (startMinutes.isEmpty()) 22 * 60
    else startMinutes[startMinutes.size / 2]

    val windowStart = (medianMinutes - 120).coerceAtLeast(0)
    val windowEnd   = (windowStart + 960).coerceAtMost(1440)
    return Pair(windowStart, windowEnd)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
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
                        val (windowStart, windowEnd) = computeTimelineWindow(state.sessions)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("tl_screen")
                        ) {
                            TimelineAxisHeader(windowStart = windowStart, windowEnd = windowEnd)
                            Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                TimelineCanvas(
                                    sessions    = state.sessions,
                                    windowStart = windowStart,
                                    windowEnd   = windowEnd,
                                    modifier    = Modifier
                                        .fillMaxWidth()
                                        .height((state.sessions.size * ROW_HEIGHT_DP + AXIS_BOTTOM_DP).dp)
                                        .testTag("tl_canvas")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineAxisHeader(
    windowStart: Int,
    windowEnd: Int,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val windowDuration = (windowEnd - windowStart).takeIf { it > 0 } ?: return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(AXIS_BOTTOM_DP.dp)
    ) {
        val canvasW  = size.width
        val labelPx  = LABEL_WIDTH_DP.dp.toPx()
        val drawableW = canvasW - labelPx

        val paint = android.graphics.Paint().apply {
            textSize  = 9.sp.toPx()
            color     = onSurface.copy(alpha = 0.6f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val step = if (windowDuration >= 720) 2 else 1
        val startHour = windowStart / 60
        val endHour   = (windowEnd + 59) / 60

        var h = startHour
        while (h <= endHour) {
            val hMin = h * 60
            if (hMin in windowStart..windowEnd) {
                val x = labelPx + ((hMin - windowStart).toFloat() / windowDuration) * drawableW
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "%02d:00".format(h % 24),
                        x,
                        size.height * 0.8f,
                        paint
                    )
                }
            }
            h += step
        }
    }
}

@Composable
private fun TimelineCanvas(
    sessions: List<SleepSessionResponse>,
    windowStart: Int,
    windowEnd: Int,
    modifier: Modifier = Modifier
) {
    val onSurface     = MaterialTheme.colorScheme.onSurface
    val windowDuration = (windowEnd - windowStart).takeIf { it > 0 } ?: return

    Canvas(modifier = modifier) {
        val canvasW   = size.width
        val canvasH   = size.height
        val labelPx   = LABEL_WIDTH_DP.dp.toPx()
        val drawableW = canvasW - labelPx

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

        val rowH    = ROW_HEIGHT_DP.dp.toPx()
        val barH    = BAR_HEIGHT_DP.dp.toPx()
        val axisBot = AXIS_BOTTOM_DP.dp.toPx()
        val dateFmt = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

        // Draw bars and date labels
        sessions.forEachIndexed { index, session ->
            val start = runCatching { OffsetDateTime.parse(session.sleep_start) }.getOrNull()
                ?: return@forEachIndexed
            val end = runCatching { OffsetDateTime.parse(session.sleep_end) }.getOrNull()
                ?: return@forEachIndexed

            val startMin = start.hour * 60 + start.minute
            var endMin   = end.hour * 60 + end.minute
            if (endMin < startMin) endMin += 1440

            val startOff = (startMin - windowStart).coerceIn(0, windowDuration)
            val endOff   = (endMin   - windowStart).coerceIn(0, windowDuration)

            if (endOff > startOff) {
                val barLeft  = (labelPx + (startOff.toFloat() / windowDuration) * drawableW)
                    .coerceIn(labelPx, canvasW)
                val barRight = (labelPx + (endOff.toFloat() / windowDuration) * drawableW)
                    .coerceIn(labelPx, canvasW)
                val rowTop   = index * rowH
                val barTop   = rowTop + (rowH - barH) / 2f

                drawRect(
                    color   = ColorSleepBar,
                    topLeft = Offset(barLeft, barTop),
                    size    = Size(barRight - barLeft, barH)
                )
            }

            // Date label
            val label  = start.format(dateFmt)
            val textY  = index * rowH + rowH / 2f + datePaint.textSize / 3f
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(label, LABEL_PADDING_DP.dp.toPx(), textY, datePaint)
            }
        }

        // X-axis hour ticks at bottom
        val axisTopY  = canvasH - axisBot
        val tickBotY  = axisTopY + 4.dp.toPx()
        val step = if (windowDuration >= 720) 2 else 1
        val startHour = windowStart / 60
        val endHour   = (windowEnd + 59) / 60

        var h = startHour
        while (h <= endHour) {
            val hMin = h * 60
            if (hMin in windowStart..windowEnd) {
                val x = labelPx + ((hMin - windowStart).toFloat() / windowDuration) * drawableW
                drawLine(
                    color       = onSurface.copy(alpha = 0.2f),
                    start       = Offset(x, axisTopY),
                    end         = Offset(x, tickBotY),
                    strokeWidth = 1.dp.toPx()
                )
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "%02d:00".format(h % 24),
                        x,
                        canvasH - 2.dp.toPx(),
                        tickPaint
                    )
                }
            }
            h += step
        }
    }
}
