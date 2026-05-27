package fr.datasaillance.nightfall.dataviz.radial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.ui.theme.DataSaillance
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ============================================================
 * RadialClockScreen — full mobile screen wrapping MultiDonutClock.
 *
 * Layout (top → bottom, single column):
 *   • Page header (eyebrow + title + description)
 *   • Date navigator (prev / next + formatted date + index)
 *   • Multi-donut clock (square, 1:1) — sélection par couche à deux niveaux
 *   • Variant toggle (Heatmap horaire / Une ligne par app) — DT-5 / L13
 *   • LayerContextCard — vue d'ensemble OU détail couche/segment (DT-1 / L8)
 *
 * L'écran possède l'état selectedDate / variant / selection. La sélection est
 * réinitialisée au changement de date (L12 / TA-9).
 * ============================================================ */

@Composable
fun RadialClockScreen(
    days: Map<LocalDate, RadialDay>,
    initialDate: LocalDate,
    typicalUsageHourDist: FloatArray? = null,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    mapRenderer: TrajetMapRenderer = NullTrajetMapRenderer,
) {
    val palette = MaterialTheme.colorScheme
    val sorted  = remember(days) { days.keys.sorted() }
    var dateIdx by remember(initialDate, sorted) {
        mutableStateOf(sorted.indexOf(initialDate).coerceAtLeast(0))
    }
    var variant   by remember { mutableStateOf(UsageVariant.Heat) }
    var selection by remember { mutableStateOf<RadialSelection?>(null) }

    val date = sorted.getOrNull(dateIdx) ?: return
    val day = days[date] ?: RadialDay(date)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PageHeader()
        }
        item {
            DateNavigator(
                date = date,
                index = dateIdx,
                total = sorted.size,
                onPrev = { if (dateIdx > 0) { dateIdx--; selection = null } },
                onNext = { if (dateIdx < sorted.size - 1) { dateIdx++; selection = null } },
            )
        }
        item {
            MultiDonutClock(
                day = day,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(20.dp))
                    .padding(8.dp),
                usageVariant = variant,
                typicalUsageHourDist = typicalUsageHourDist,
                selection = selection,
                onSelectionChange = { selection = it },
                zone = zone,
            )
        }
        item {
            VariantToggle(value = variant, onChange = { variant = it })
        }
        item { LayerContextCard(day, selection, zone, mapRenderer) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────
// Page header
// ─────────────────────────────────────────────────────────────
@Composable
private fun PageHeader() {
    val extras = DataSaillance.extras
    Column {
        Text(
            text = "NIGHTFALL · MULTI-DONUT",
            color = extras.textMuted,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Une journée, trois couches concentriques.",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Date navigator (prev / next + formatted date + index)
// ─────────────────────────────────────────────────────────────
@Composable
private fun DateNavigator(
    date: LocalDate, index: Int, total: Int,
    onPrev: () -> Unit, onNext: () -> Unit,
) {
    val extras  = DataSaillance.extras
    val palette = MaterialTheme.colorScheme
    val fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ArrowButton(prev = true, onClick = onPrev)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.format(fmt).replaceFirstChar { it.uppercase() },
                color = palette.onBackground,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = "${index + 1} / $total",
                color = extras.textFaint,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            )
        }
        ArrowButton(prev = false, onClick = onNext)
    }
}

@Composable
private fun ArrowButton(prev: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.colorScheme
    val extras = DataSaillance.extras
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(extras.borderStrong.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (prev) "‹" else "›",
            color = palette.onBackground,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Variant toggle (DT-5 / L13 : Heatmap conditionnel à usageSessions, Apps reste)
// ─────────────────────────────────────────────────────────────
@Composable
private fun VariantToggle(value: UsageVariant, onChange: (UsageVariant) -> Unit) {
    val extras = DataSaillance.extras
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ANNEAU USAGE · 2 VERSIONS",
            color = extras.textFaint,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp,
                                                              fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, extras.borderStrong, RoundedCornerShape(999.dp))
                .padding(3.dp),
        ) {
            UsageVariant.entries.forEach { v ->
                ToggleChip(
                    text = if (v == UsageVariant.Heat) "Heatmap horaire" else "Une ligne / app",
                    active = value == v,
                    onClick = { onChange(v) },
                )
            }
        }
    }
}

@Composable
private fun ToggleChip(text: String, active: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.colorScheme
    val extras  = DataSaillance.extras
    Text(
        text = text,
        color = if (active) palette.onBackground else extras.textMuted,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) palette.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

// ─────────────────────────────────────────────────────────────
// LayerContextCard (DT-1 / L8) — pilotée par selection
// ─────────────────────────────────────────────────────────────
@Composable
private fun LayerContextCard(
    day: RadialDay,
    selection: RadialSelection?,
    zone: ZoneId,
    mapRenderer: TrajetMapRenderer,
) {
    when (selection?.layer) {
        null -> OverviewCard(day, zone)
        RadialLayer.SLEEP -> SleepContextCard(day, selection, zone)
        RadialLayer.USAGE -> UsageContextCard(day, selection, zone)
        RadialLayer.TIMELINE -> TimelineContextCard(day, selection, zone, mapRenderer)
    }
}

// ── Vue d'ensemble compacte (aucune sélection) ──────────────────
@Composable
private fun OverviewCard(day: RadialDay, zone: ZoneId) {
    val sleepMin = day.sleepStages.sumOf { (it.endMs - it.startMs) }.toFloat() / 60_000f
    val stageMin = stageMinutes(day)
    val usageMin = day.usageRows.sumOf { it.totalTimeForegroundMs }.toFloat() / 60_000f
    val totalKm  = day.activities.sumOf { it.distanceMeters }.toFloat() / 1000f

    SectionCard(title = "LA JOURNÉE", subtitle = "tap un anneau pour le détail") {
        MetricRow("sommeil", formatDuration(sleepMin))
        if (sleepMin > 0f) StageStrip(stageMin, sleepMin)
        MetricRow("téléphone", formatDuration(usageMin))
        MetricRow("lieux visités", "${day.visits.size}")
        MetricRow("sorties",
            "${day.activities.size}${if (totalKm > 0f) " · %.1f km".format(totalKm) else ""}")
    }
}

// ── Couche sommeil ──────────────────────────────────────────────
@Composable
private fun SleepContextCard(day: RadialDay, selection: RadialSelection, zone: ZoneId) {
    val extras = DataSaillance.extras
    if (selection.segmentStartMs != null) {
        // Niveau 2 — détail segment
        val stages = day.sleepStages.sortedBy { it.startMs }
        val idx = stages.indexOfFirst {
            it.startMs == selection.segmentStartMs && it.endMs == selection.segmentEndMs
        }
        val seg = stages.getOrNull(idx)
        SectionCard(title = "SOMMEIL · DÉTAIL", subtitle = "moment sélectionné") {
            if (seg == null) {
                Text("Segment introuvable.", color = extras.textFaint,
                    style = MaterialTheme.typography.bodySmall)
            } else {
                val durMin = (seg.endMs - seg.startMs).toFloat() / 60_000f
                val sameType = stages.filter { it.type == seg.type }
                val rank = sameType.indexOf(seg) + 1
                StageBadge(seg.type)
                MetricRow("durée", formatDuration(durMin))
                MetricRow("horaire",
                    "${hhmm(seg.startMs, zone)} → ${hhmm(seg.endMs, zone)}")
                MetricRow("position",
                    "${rank}e période ${seg.type.name.lowercase()}")
            }
        }
        return
    }
    // Niveau 1 — vue globale couche
    val sorted = day.sleepStages.sortedBy { it.startMs }
    val sleepMin = day.sleepStages.sumOf { (it.endMs - it.startMs) }.toFloat() / 60_000f
    val stageMin = stageMinutes(day)
    SectionCard(title = "SOMMEIL", subtitle = "vue globale de la nuit") {
        if (sorted.isEmpty()) {
            Text("Aucune donnée de sommeil ce jour.", color = extras.textFaint,
                style = MaterialTheme.typography.bodySmall)
        } else {
            MetricRow("coucher → lever",
                "${hhmm(sorted.first().startMs, zone)} → ${hhmm(sorted.last().endMs, zone)}")
            MetricRow("durée", formatDuration(sleepMin))
            MetricRow("sessions", "${countSleepSessions(day)}")
            if (sleepMin > 0f) StageStrip(stageMin, sleepMin)
        }
    }
}

@Composable
private fun StageBadge(stage: SleepStage) {
    val extras = DataSaillance.extras
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(stageColor(stage, extras)))
        Text(stage.name.lowercase(), color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

// ── Couche usage ────────────────────────────────────────────────
@Composable
private fun UsageContextCard(day: RadialDay, selection: RadialSelection, zone: ZoneId) {
    val extras = DataSaillance.extras
    val usageMin = day.usageRows.sumOf { it.totalTimeForegroundMs }.toFloat() / 60_000f
    val apps = day.usageRows.sortedByDescending { it.totalTimeForegroundMs }.take(5)

    if (selection.segmentStartMs != null && day.usageSessions.isNotEmpty()) {
        // Niveau 2 — disponible seulement quand usageSessions non vide
        val active = day.usageSessions.filter {
            it.startMs < (selection.segmentEndMs ?: Long.MAX_VALUE) &&
                it.endMs > selection.segmentStartMs
        }
        val cumMin = active.sumOf { it.endMs - it.startMs }.toFloat() / 60_000f
        SectionCard(title = "USAGE · DÉTAIL", subtitle = "tranche sélectionnée") {
            MetricRow("durée cumulée", formatDuration(cumMin))
            active.map { it.packageName }.distinct().take(5).forEach {
                Text(it, color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp))
            }
        }
        return
    }

    SectionCard(title = "USAGE", subtitle = "temps écran de la journée") {
        MetricRow("temps écran total", formatDuration(usageMin))
        if (day.usageSessions.isEmpty()) {
            DegradedBadge("Sessions horaires non disponibles — importer via Paramètres")
        }
        val peak = apps.maxOfOrNull { it.totalTimeForegroundMs }?.toFloat() ?: 1f
        apps.forEachIndexed { i, app ->
            val w = app.totalTimeForegroundMs / peak
            val color = heatColor(0.4f + (i.toFloat() / apps.size.coerceAtLeast(1)) * 0.5f)
            AppBarRow(app, w, color)
        }
        if (apps.isEmpty()) {
            Text("Aucune app enregistrée ce jour.", color = extras.textFaint,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DegradedBadge(text: String) {
    val extras = DataSaillance.extras
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(extras.divider.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(text, color = extras.textMuted,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp))
    }
}

@Composable
private fun AppBarRow(app: RadialUsageRow, fillFrac: Float, color: Color) {
    val palette = MaterialTheme.colorScheme
    val extras = DataSaillance.extras
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(app.packageName, color = palette.onBackground,
                 style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(
                text = "${(app.totalTimeForegroundMs / 60_000).toInt()}m",
                color = extras.textMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(palette.surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillFrac.coerceIn(0f, 1f))
                    .background(color),
            )
        }
    }
}

// ── Couche timeline ─────────────────────────────────────────────
@Composable
private fun TimelineContextCard(
    day: RadialDay,
    selection: RadialSelection,
    zone: ZoneId,
    mapRenderer: TrajetMapRenderer,
) {
    val extras = DataSaillance.extras
    if (selection.segmentStartMs != null) {
        // Niveau 2 — détail segment (visite ou activité)
        val visit = day.visits.firstOrNull {
            it.startMs == selection.segmentStartMs && it.endMs == selection.segmentEndMs
        }
        val act = day.activities.firstOrNull {
            it.startMs == selection.segmentStartMs && it.endMs == selection.segmentEndMs
        }
        SectionCard(title = "TIMELINE · DÉTAIL", subtitle = "moment sélectionné") {
            when {
                visit != null -> {
                    Text(visit.placeLabel ?: visit.placeName,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    MetricRow("horaire",
                        "${hhmm(visit.startMs, zone)} → ${hhmm(visit.endMs, zone)}")
                    MetricRow("durée",
                        formatDuration((visit.endMs - visit.startMs).toFloat() / 60_000f))
                    Text(
                        if (visit.anchored) "lieu ancré" else "lieu non labellisé",
                        color = if (visit.anchored) Color(0xFFD37C04) else extras.textMuted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
                act != null -> {
                    val (_, typeLabel) = Activity.resolve(act.activityType)
                    Text(typeLabel, color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                    MetricRow("horaire",
                        "${hhmm(act.startMs, zone)} → ${hhmm(act.endMs, zone)}")
                    MetricRow("durée",
                        formatDuration((act.endMs - act.startMs).toFloat() / 60_000f))
                    MetricRow("distance", "%.1f km".format(act.distanceMeters / 1000f))
                }
                else -> Text("Segment introuvable.", color = extras.textFaint,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    // Niveau 1 — vue globale couche
    val activeMin = day.activities.sumOf { it.endMs - it.startMs }.toFloat() / 60_000f
    SectionCard(title = "TIMELINE", subtitle = "lieux & déplacements") {
        day.visits.sortedBy { it.startMs }.forEach { v ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(v.placeLabel ?: v.placeName,
                    color = if (v.anchored) Color(0xFFD37C04) else MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                Text("${hhmm(v.startMs, zone)} → ${hhmm(v.endMs, zone)}",
                    color = extras.textMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        day.activities.sortedBy { it.startMs }.forEach { a ->
            val (_, label) = Activity.resolve(a.activityType)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$label · %.1f km".format(a.distanceMeters / 1000f),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium)
                Text("${hhmm(a.startMs, zone)} → ${hhmm(a.endMs, zone)}",
                    color = extras.textMuted, style = MaterialTheme.typography.labelSmall)
            }
        }
        MetricRow("temps de déplacement", formatDuration(activeMin))
        if (day.visits.isEmpty() && day.activities.isEmpty()) {
            Text("Aucun déplacement ce jour.", color = extras.textFaint,
                style = MaterialTheme.typography.bodySmall)
        }
        // Slot carte (DT-7) — no-op tant que le renderer par défaut est branché.
        if (mapRenderer !== NullTrajetMapRenderer) {
            mapRenderer.Render(day.visits, day.activities, Modifier.fillMaxWidth())
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Shared helpers & chrome
// ─────────────────────────────────────────────────────────────
@Composable
private fun StageStrip(stageMin: Map<SleepStage, Float>, totalMin: Float) {
    val extras = DataSaillance.extras
    val stages = listOf(SleepStage.AWAKE, SleepStage.REM, SleepStage.LIGHT, SleepStage.DEEP)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        ) {
            stages.forEach { s ->
                val frac = ((stageMin[s] ?: 0f) / totalMin).coerceIn(0f, 1f)
                if (frac > 0f) {
                    Box(
                        Modifier
                            .weight(frac)
                            .fillMaxHeight()
                            .background(stageColor(s, extras)),
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stages.forEach { s ->
                val pct = ((stageMin[s] ?: 0f) / totalMin * 100f).toInt()
                Text(
                    text = "$pct% ${s.name.lowercase().take(3)}",
                    color = extras.textMuted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                )
            }
        }
    }
}

private fun stageMinutes(day: RadialDay): Map<SleepStage, Float> {
    val stageMin = mutableMapOf(
        SleepStage.AWAKE to 0f, SleepStage.REM to 0f,
        SleepStage.LIGHT to 0f, SleepStage.DEEP to 0f,
    )
    day.sleepStages.forEach {
        stageMin[it.type] = (stageMin[it.type] ?: 0f) + (it.endMs - it.startMs) / 60_000f
    }
    return stageMin
}

@Composable
private fun SectionCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val extras  = DataSaillance.extras
    val palette = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(14.dp))
            .border(1.dp, extras.borderStrong.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = extras.textMuted,
             style = MaterialTheme.typography.labelSmall.copy(
                 letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold))
        Text(subtitle, color = extras.textFaint, style = MaterialTheme.typography.bodySmall)
        content()
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    val extras = DataSaillance.extras
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label.uppercase(Locale.FRENCH), color = extras.textMuted,
             style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp))
        Text(value, color = MaterialTheme.colorScheme.onBackground,
             style = MaterialTheme.typography.titleMedium.copy(
                 fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp))
    }
}

private fun formatDuration(min: Float): String {
    if (min <= 0f) return "—"
    val h = (min / 60).toInt(); val m = (min % 60).toInt()
    return "${h}h${"%02d".format(m)}"
}

private fun hhmm(ms: Long, zone: ZoneId): String {
    val z = Instant.ofEpochMilli(ms).atZone(zone)
    return "%02d:%02d".format(z.hour, z.minute)
}
