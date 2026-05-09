package fr.datasaillance.nightfall.ui.screens.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import java.time.Duration
import java.time.OffsetDateTime

@Composable
fun HypnogramStatsSection(
    sessions: List<SleepSessionResponse>,
    modifier: Modifier = Modifier
) {
    val stages = sessions.flatMap { it.stages ?: emptyList() }
    val start = sessions.mapNotNull { runCatching { OffsetDateTime.parse(it.sleep_start) }.getOrNull() }.minOrNull()
    val end   = sessions.mapNotNull { runCatching { OffsetDateTime.parse(it.sleep_end) }.getOrNull() }.maxOrNull()
    val totalMin = if (start != null && end != null) Duration.between(start, end).toMinutes() else 0L

    data class StageInfo(val color: Color, val label: String, val type: String)
    val stageOrder = listOf(
        StageInfo(Color(0xFF0E9EB0), "DEEP",  "DEEP"),
        StageInfo(Color(0xFF7A9AAA), "LIGHT", "LIGHT"),
        StageInfo(Color(0xFF07BCD3), "REM",   "REM"),
        StageInfo(Color(0xFFD37C04), "AWAKE", "AWAKE")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("hyp_stats")
    ) {
        Text(
            "Détail des phases",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))

        for (info in stageOrder) {
            val durMin = stages
                .filter { it.stage == info.type }
                .sumOf { s ->
                    val ss = runCatching { OffsetDateTime.parse(s.stage_start) }.getOrNull()
                    val se = runCatching { OffsetDateTime.parse(s.stage_end) }.getOrNull()
                    if (ss != null && se != null) Duration.between(ss, se).toMinutes() else 0L
                }
            if (durMin <= 0L) continue

            val durationText = if (durMin >= 60) {
                val h  = durMin / 60
                val mm = durMin % 60
                "${h}h ${mm.toString().padStart(2, '0')}"
            } else {
                "$durMin min"
            }
            val pctText = if (totalMin > 0) "${(durMin * 100 / totalMin).toInt()}%" else ""

            StageStatRow(
                color        = info.color,
                label        = info.label,
                durationText = durationText,
                pctText      = pctText
            )
        }
    }
}

@Composable
private fun StageStatRow(
    color: Color,
    label: String,
    durationText: String,
    pctText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(durationText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(8.dp))
        Text(pctText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
