package fr.datasaillance.nightfall.ui.screens.hypnogram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.data.sleep.SleepStageResponse
import timber.log.Timber
import java.time.OffsetDateTime

data class HypnogramSegment(val stageType: String, val startMs: Long, val endMs: Long)

fun buildSegments(
    stages: List<SleepStageResponse>,
    sessionStart: OffsetDateTime,
    sessionEnd: OffsetDateTime
): List<HypnogramSegment> {
    val sessionStartMs = sessionStart.toInstant().toEpochMilli()
    val sessionEndMs = sessionEnd.toInstant().toEpochMilli()

    val sorted = stages.sortedBy { OffsetDateTime.parse(it.stage_start).toInstant().toEpochMilli() }

    val result = mutableListOf<HypnogramSegment>()

    if (sorted.isEmpty()) {
        if (sessionStartMs < sessionEndMs) {
            result.add(HypnogramSegment("AWAKE", sessionStartMs, sessionEndMs))
        }
        return result
    }

    val firstStageStartMs = OffsetDateTime.parse(sorted.first().stage_start).toInstant().toEpochMilli()
    if (firstStageStartMs > sessionStartMs) {
        result.add(HypnogramSegment("AWAKE", sessionStartMs, firstStageStartMs))
    }

    for (i in sorted.indices) {
        val stage = sorted[i]
        val stageStartMs = OffsetDateTime.parse(stage.stage_start).toInstant().toEpochMilli()
        val stageEndMs = OffsetDateTime.parse(stage.stage_end).toInstant().toEpochMilli()
        result.add(HypnogramSegment(stage.stage, stageStartMs, stageEndMs))

        if (i < sorted.size - 1) {
            val nextStageStartMs = OffsetDateTime.parse(sorted[i + 1].stage_start).toInstant().toEpochMilli()
            if (stageEndMs < nextStageStartMs) {
                result.add(HypnogramSegment("AWAKE", stageEndMs, nextStageStartMs))
            }
        }
    }

    val lastStageEndMs = OffsetDateTime.parse(sorted.last().stage_end).toInstant().toEpochMilli()
    if (lastStageEndMs < sessionEndMs) {
        result.add(HypnogramSegment("AWAKE", lastStageEndMs, sessionEndMs))
    }

    return result.sortedBy { it.startMs }
}

private val ColorDeep = Color(0xFF0E9EB0)
private val ColorLight = Color(0xFF7A9AAA)
private val ColorRem = Color(0xFF07BCD3)
private val ColorAwake = Color(0xFFD37C04)
private val ColorUnknown = Color(0xFF4A4A4A)

fun stageColor(stageType: String): Color = when (stageType) {
    "DEEP" -> ColorDeep
    "LIGHT" -> ColorLight
    "REM" -> ColorRem
    "AWAKE" -> ColorAwake
    else -> ColorUnknown
}

@Composable
fun HypnogramCanvas(
    stages: List<SleepStageResponse>,
    sessionStart: OffsetDateTime,
    sessionEnd: OffsetDateTime,
    modifier: Modifier = Modifier
) {
    val segments = buildSegments(stages, sessionStart, sessionEnd)
    val segmentCount = segments.size
    Timber.d("hypnogram_canvas segments=$segmentCount")

    val sessionStartMs = sessionStart.toInstant().toEpochMilli()
    val sessionDurationMs = sessionEnd.toInstant().toEpochMilli() - sessionStartMs

    Canvas(
        modifier = modifier
            .height(80.dp)
            .fillMaxWidth()
            .semantics { contentDescription = "Hypnogramme" }
    ) {
        if (sessionDurationMs <= 0) return@Canvas
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (segment in segments) {
            val left = (segment.startMs - sessionStartMs).toFloat() / sessionDurationMs * canvasWidth
            val right = (segment.endMs - sessionStartMs).toFloat() / sessionDurationMs * canvasWidth
            drawRect(
                color = stageColor(segment.stageType),
                topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
                size = androidx.compose.ui.geometry.Size(right - left, canvasHeight)
            )
        }
    }
}
