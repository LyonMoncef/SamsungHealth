package fr.datasaillance.nightfall.ui.screens.hypnogram

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import fr.datasaillance.nightfall.data.sleep.SleepStageResponse
import fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState
import fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel
import timber.log.Timber
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HypnogramScreen(
    sessionId: String,
    sleepViewModel: SleepViewModel,
    onBack: () -> Unit
) {
    Timber.d("hypnogram_screen session_id=$sessionId")

    val uiState by sleepViewModel.uiState.collectAsState()
    val session = when (val s = uiState) {
        is SleepUiState.Success -> {
            val found = s.sessions.find { it.id == sessionId }
            if (found == null) Timber.w("hypnogram_screen_not_found session_id=$sessionId")
            found
        }
        else -> null
    }

    val stages = session?.stages

    val nightLabel = session?.let {
        runCatching {
            val dt = OffsetDateTime.parse(it.sleep_start)
            val formatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)
            val raw = formatter.format(dt)
            raw.replaceFirstChar { c -> c.uppercaseChar() }
        }.getOrElse { "Nuit" }
    } ?: "Hypnogramme"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = nightLabel,
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            session == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("hypnogram_not_found"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nuit introuvable")
                }
            }
            stages.isNullOrEmpty() -> {
                Timber.d("hypnogram_screen_no_stages session_id=$sessionId")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .testTag("hypnogram_no_stages"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucune donnée de phase disponible")
                }
            }
            else -> {
                val sessionStart = OffsetDateTime.parse(session.sleep_start)
                val sessionEnd = OffsetDateTime.parse(session.sleep_end)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    val allSegments = buildSegments(stages, sessionStart, sessionEnd)
                    val hasImplicitAwake = allSegments.any { it.stageType == "AWAKE" }

                    HypnogramCanvas(
                        stages = stages,
                        sessionStart = sessionStart,
                        sessionEnd = sessionEnd,
                        modifier = Modifier.testTag("hypnogram_canvas")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HypnogramLegend(
                        stages = stages,
                        hasImplicitAwake = hasImplicitAwake,
                        modifier = Modifier.testTag("hypnogram_legend")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HypnogramKpis(
                        session = session,
                        stages = stages,
                        sessionStart = sessionStart,
                        sessionEnd = sessionEnd,
                        allSegments = allSegments,
                        modifier = Modifier.testTag("hypnogram_kpis")
                    )
                }
            }
        }
    }
}

@Composable
private fun HypnogramLegend(
    stages: List<SleepStageResponse>,
    hasImplicitAwake: Boolean = false,
    modifier: Modifier = Modifier
) {
    val presentTypes = stages.map { it.stage }.toSet()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val orderedTypes = listOf("DEEP", "LIGHT", "REM", "AWAKE")
        val labels = mapOf(
            "DEEP" to "Profond",
            "LIGHT" to "Léger",
            "REM" to "REM",
            "AWAKE" to "Éveil"
        )

        orderedTypes.filter { it in presentTypes || (it == "AWAKE" && hasImplicitAwake) }
            .forEachIndexed { index, stageType ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(16.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color = stageColor(stageType), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = labels[stageType] ?: stageType,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun HypnogramKpis(
    session: SleepSessionResponse,
    stages: List<SleepStageResponse>,
    sessionStart: OffsetDateTime,
    sessionEnd: OffsetDateTime,
    allSegments: List<HypnogramSegment>,
    modifier: Modifier = Modifier
) {
    val totalMs = sessionEnd.toInstant().toEpochMilli() - sessionStart.toInstant().toEpochMilli()
    val totalFormatted = formatDuration(totalMs)

    val awakeMs = allSegments.filter { it.stageType == "AWAKE" }.sumOf { it.endMs - it.startMs }

    val deepMs = stages.filter { it.stage == "DEEP" }.sumOf {
        OffsetDateTime.parse(it.stage_end).toInstant().toEpochMilli() -
            OffsetDateTime.parse(it.stage_start).toInstant().toEpochMilli()
    }
    val lightMs = stages.filter { it.stage == "LIGHT" }.sumOf {
        OffsetDateTime.parse(it.stage_end).toInstant().toEpochMilli() -
            OffsetDateTime.parse(it.stage_start).toInstant().toEpochMilli()
    }
    val remMs = stages.filter { it.stage == "REM" }.sumOf {
        OffsetDateTime.parse(it.stage_end).toInstant().toEpochMilli() -
            OffsetDateTime.parse(it.stage_start).toInstant().toEpochMilli()
    }

    val presentTypes = stages.map { it.stage }.toSet()
    val hasImplicitAwake = allSegments.any { it.stageType == "AWAKE" }

    Column(modifier = modifier.fillMaxWidth()) {
        KpiRow(stageType = null, value = totalFormatted)

        if ("DEEP" in presentTypes) {
            KpiRow(stageType = "DEEP", value = formatDuration(deepMs))
        }
        if ("LIGHT" in presentTypes) {
            KpiRow(stageType = "LIGHT", value = formatDuration(lightMs))
        }
        if ("REM" in presentTypes) {
            KpiRow(stageType = "REM", value = formatDuration(remMs))
        }
        if ("AWAKE" in presentTypes || hasImplicitAwake) {
            KpiRow(stageType = "AWAKE", value = formatDuration(awakeMs))
        }
    }
}

@Composable
private fun KpiRow(stageType: String?, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (stageType != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = stageColor(stageType), shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Text(
                text = "Total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
        }
        if (stageType != null) {
            Spacer(modifier = Modifier.weight(1f))
        }
        Text(
            text = value,
            style = if (stageType == null) MaterialTheme.typography.headlineLarge
                    else MaterialTheme.typography.headlineMedium
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}h ${minutes.toString().padStart(2, '0')}"
}
