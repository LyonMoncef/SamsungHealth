---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/HypnogramScreen.kt
git_blob: 01d0df6892316d2c9b37f02f5f406dd166ff1ffd
last_synced: '2026-05-09T06:05:32Z'
loc: 320
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

private fun stageColor(stageType: String): Color = when (stageType) {
    "DEEP"  -> ColorDeep
    "REM"   -> ColorRem
    "AWAKE" -> ColorAwake
    else    -> ColorLight
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = when (val s = uiState) {
                            is HypnogramUiState.Success -> nightTitle(s.session.sleep_start)
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
                            HypnogramSummarySection(state.session)
                            Spacer(modifier = Modifier.height(16.dp))
                            HypnogramCanvas(
                                session = state.session,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .testTag("hyp_canvas")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HypnogramLegend()
                            Spacer(modifier = Modifier.height(16.dp))
                            HypnogramStatsSection(session = state.session)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HypnogramSummarySection(
    session: SleepSessionResponse,
    modifier: Modifier = Modifier
) {
    val start = runCatching { OffsetDateTime.parse(session.sleep_start) }.getOrNull()
    val end   = runCatching { OffsetDateTime.parse(session.sleep_end) }.getOrNull()
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
    val deepMin  = session.stages
        ?.filter { it.stage == "DEEP" }
        ?.sumOf { s ->
            val ss = runCatching { OffsetDateTime.parse(s.stage_start) }.getOrNull()
            val se = runCatching { OffsetDateTime.parse(s.stage_end) }.getOrNull()
            if (ss != null && se != null) Duration.between(ss, se).toMinutes() else 0L
        } ?: 0L
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
    session: SleepSessionResponse,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface   = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val stageZoneH  = 100.dp.toPx()

        val start = runCatching { OffsetDateTime.parse(session.sleep_start) }.getOrNull()
        val end   = runCatching { OffsetDateTime.parse(session.sleep_end) }.getOrNull()
        val sessionDuration = if (start != null && end != null) Duration.between(start, end).toMillis() else 0L

        if (sessionDuration <= 0 || session.stages.isNullOrEmpty()) {
            drawRect(
                color   = surface.copy(alpha = 0.3f),
                topLeft = Offset(0f, 0f),
                size    = Size(canvasWidth, stageZoneH)
            )
            return@Canvas
        }

        val sorted = session.stages.sortedBy { it.stage_start }
        for (stage in sorted) {
            val ss = runCatching { OffsetDateTime.parse(stage.stage_start) }.getOrNull() ?: continue
            val se = runCatching { OffsetDateTime.parse(stage.stage_end) }.getOrNull() ?: continue
            val stageDurMs = Duration.between(ss, se).toMillis()
            val offsetMs   = Duration.between(start, ss).toMillis()

            val x     = (offsetMs.toFloat() / sessionDuration) * canvasWidth
            val width = (stageDurMs.toFloat() / sessionDuration) * canvasWidth

            val rectH: Float
            val rectTop: Float
            if (stage.stage == "AWAKE") {
                rectH   = 60.dp.toPx()
                rectTop = (stageZoneH - rectH) / 2f
            } else {
                rectH   = stageZoneH
                rectTop = 0f
            }

            drawRect(
                color   = stageColor(stage.stage),
                topLeft = Offset(x, rectTop),
                size    = Size(width, rectH)
            )
        }

        val tickStart = start!!.toLocalDateTime().let { ldt ->
            val h = ldt.hour
            val nextH = if (ldt.minute > 0 || ldt.second > 0) h + 1 else h
            start.withHour(nextH % 24).withMinute(0).withSecond(0).withNano(0)
                .let { if (nextH >= 24) it.plusDays(1) else it }
        }

        val paint = android.graphics.Paint().apply {
            textSize  = 10.sp.toPx()
            color     = onSurface.copy(alpha = 0.5f).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }

        var tick = tickStart
        while (!tick.isAfter(end!!)) {
            val tickOffsetMs = Duration.between(start, tick).toMillis()
            val xTick = (tickOffsetMs.toFloat() / sessionDuration) * canvasWidth
            drawLine(
                color       = onSurface.copy(alpha = 0.3f),
                start       = Offset(xTick, 95.dp.toPx()),
                end         = Offset(xTick, stageZoneH),
                strokeWidth = 1.dp.toPx()
            )
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    tick.format(DateTimeFormatter.ofPattern("HH:mm")),
                    xTick,
                    118.dp.toPx(),
                    paint
                )
            }
            tick = tick.plusHours(1)
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
- `stageColor` (function) — lines 39-44
- `nightTitle` (function) — lines 46-54
- `HypnogramScreen` (function) — lines 56-156
- `HypnogramSummarySection` (function) — lines 158-204
- `HypnogramCanvas` (function) — lines 206-292
- `HypnogramLegend` (function) — lines 294-307
- `LegendItem` (function) — lines 309-320
