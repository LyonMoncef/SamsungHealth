---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/RadialClockScreen.kt
git_blob: 9b38ef005c42fed0e0eaf8ccc0e2e2c0d873aa68
last_synced: '2026-05-26T03:20:22Z'
loc: 482
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/RadialClockScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/RadialClockScreen.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/RadialClockScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.dataviz.radial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.ui.theme.DataSaillance
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
 *   • Multi-donut clock (square, 1:1)
 *   • Variant toggle (Heat / Apps)
 *   • Day summary card (sleep + stage strip + usage + visits/acts)
 *   • Quadrants card (4 tappable rows)
 *   • Top apps card (filtered by selected quadrant if any)
 *
 * The screen owns selectedDate / variant / quadrant. Sleep / timeline /
 * usage data come from a `days: Map<LocalDate, RadialDay>` map supplied by
 * the parent (typically a ViewModel that joins SleepDao + LocationDao +
 * UsageStatsDao queries).
 * ============================================================ */

@Composable
fun RadialClockScreen(
    days: Map<LocalDate, RadialDay>,
    initialDate: LocalDate,
    typicalUsageHourDist: FloatArray? = null,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val palette = MaterialTheme.colorScheme
    val extras  = DataSaillance.extras
    val sorted  = remember(days) { days.keys.sorted() }
    var dateIdx by remember(initialDate, sorted) {
        mutableStateOf(sorted.indexOf(initialDate).coerceAtLeast(0))
    }
    var variant   by remember { mutableStateOf(UsageVariant.Heat) }
    var quadrant  by remember { mutableStateOf<Int?>(null) }

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
                onPrev = { if (dateIdx > 0) { dateIdx--; quadrant = null } },
                onNext = { if (dateIdx < sorted.size - 1) { dateIdx++; quadrant = null } },
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
                selectedQuadrant = quadrant,
                onQuadrantTap = { q -> quadrant = if (q == quadrant) null else q },
                zone = zone,
            )
        }
        item {
            VariantToggle(value = variant, onChange = { variant = it })
        }
        item { DaySummaryCard(day, zone) }
        item { QuadrantsCard(day, quadrant, onTap = { q -> quadrant = if (q == quadrant) null else q }, zone) }
        item { TopAppsCard(day, quadrant, zone) }
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
// Variant toggle
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
// Day summary card
// ─────────────────────────────────────────────────────────────
@Composable
private fun DaySummaryCard(day: RadialDay, zone: ZoneId) {
    val extras = DataSaillance.extras
    val sleepMin = day.sleepStages.sumOf { (it.endMs - it.startMs) }.toFloat() / 60_000f
    val stageMin = mutableMapOf(
        SleepStage.AWAKE to 0f, SleepStage.REM to 0f,
        SleepStage.LIGHT to 0f, SleepStage.DEEP to 0f,
    )
    day.sleepStages.forEach { stageMin[it.type] = (stageMin[it.type] ?: 0f) +
        (it.endMs - it.startMs) / 60_000f }
    val usageMin = day.usageRows.sumOf { it.totalTimeForegroundMs }.toFloat() / 60_000f
    val totalKm  = day.activities.sumOf { it.distanceMeters }.toFloat() / 1000f

    SectionCard(title = "LA JOURNÉE", subtitle = "résumé") {
        MetricRow("sommeil",  formatDuration(sleepMin))
        if (sleepMin > 0f) StageStrip(stageMin, sleepMin)
        MetricRow("téléphone",     formatDuration(usageMin))
        MetricRow("lieux visités", "${day.visits.size}")
        MetricRow("trajets",
            "${day.activities.size}${if (totalKm > 0f) " · %.1f km".format(totalKm) else ""}")
    }
}

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

// ─────────────────────────────────────────────────────────────
// Quadrants card
// ─────────────────────────────────────────────────────────────
@Composable
private fun QuadrantsCard(day: RadialDay, selected: Int?, onTap: (Int) -> Unit, zone: ZoneId) {
    val labels = listOf("Nuit · 00-06", "Matin · 06-12", "Après-midi · 12-18", "Soir · 18-24")
    SectionCard(title = "QUADRANTS 6 H", subtitle = "tap un cadran sur le donut") {
        labels.forEachIndexed { q, label ->
            QuadrantRow(label, summary = quadrantSummary(day, q, zone),
                        active = selected == q, onClick = { onTap(q) })
        }
    }
}

private fun quadrantSummary(day: RadialDay, q: Int, zone: ZoneId): String {
    val h1 = q * 6f; val h2 = h1 + 6f
    fun overlaps(s: Float, e: Float) = s < h2 && e > h1
    val sleepMin = day.sleepStages
        .filter { overlaps(localHour(it.startMs, zone), localHour(it.endMs, zone)) }
        .sumOf { (it.endMs - it.startMs) }.toFloat() / 60_000f
    val visits = day.visits.count { overlaps(localHour(it.startMs, zone), localHour(it.endMs, zone)) }
    val acts   = day.activities.count { overlaps(localHour(it.startMs, zone), localHour(it.endMs, zone)) }
    val parts = mutableListOf<String>()
    if (sleepMin > 0f) parts += "${sleepMin.toInt()} min sommeil"
    if (visits > 0)    parts += "$visits lieu${if (visits > 1) "x" else ""}"
    if (acts > 0)      parts += "$acts trajet${if (acts > 1) "s" else ""}"
    return parts.joinToString(" · ").ifEmpty { "—" }
}

@Composable
private fun QuadrantRow(label: String, summary: String, active: Boolean, onClick: () -> Unit) {
    val palette = MaterialTheme.colorScheme
    val extras  = DataSaillance.extras
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (active) palette.secondary.copy(alpha = 0.12f) else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .border(
                1.dp,
                if (active) palette.secondary else extras.borderStrong.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = palette.onBackground,
                 style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(summary, color = extras.textMuted, style = MaterialTheme.typography.labelSmall)
        }
        if (active) {
            Text("SÉLECTIONNÉ", color = palette.secondary,
                 style = MaterialTheme.typography.labelSmall.copy(
                     letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Top apps card (filtered by selected quadrant if any)
// ─────────────────────────────────────────────────────────────
@Composable
private fun TopAppsCard(day: RadialDay, quadrant: Int?, zone: ZoneId) {
    if (day.usageRows.isEmpty()) return
    val apps = day.usageRows.sortedByDescending { it.totalTimeForegroundMs }.take(6)
    val peak = apps.maxOf { it.totalTimeForegroundMs }.toFloat()
    val quadrantLabel = quadrant?.let { " · ${listOf("00-06", "06-12", "12-18", "18-24")[it]}" } ?: ""

    SectionCard(
        title = "TOP APPS$quadrantLabel",
        subtitle = if (quadrant != null) "fermées dans le quadran" else "sur la journée",
    ) {
        val filtered = apps.filter { r ->
            if (quadrant == null) return@filter true
            val h = localHour(r.lastTimeUsedMs, zone)
            h >= quadrant * 6f && h < quadrant * 6f + 6f
        }
        if (filtered.isEmpty()) {
            Text(
                "Aucune fermeture d'app top-6 dans ce quadran.",
                color = DataSaillance.extras.textFaint,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        filtered.forEachIndexed { i, app ->
            val h = localHour(app.lastTimeUsedMs, zone)
            val w = app.totalTimeForegroundMs / peak
            val color = heatColor(0.4f + (i.toFloat() / apps.size) * 0.5f)
            AppBarRow(app, h, w, color)
        }
    }
}

@Composable
private fun AppBarRow(app: RadialUsageRow, h: Float, fillFrac: Float, color: Color) {
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
                text = "${(app.totalTimeForegroundMs / 60_000).toInt()}m · close %02d:%02d"
                    .format(h.toInt(), ((h - h.toInt()) * 60f).toInt()),
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

// ─────────────────────────────────────────────────────────────
// Shared card chrome
// ─────────────────────────────────────────────────────────────
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `RadialClockScreen` (function) — lines 46-107
- `PageHeader` (function) — lines 112-132
- `DateNavigator` (function) — lines 137-170
- `ArrowButton` (function) — lines 172-190
- `VariantToggle` (function) — lines 195-222
- `ToggleChip` (function) — lines 224-240
- `DaySummaryCard` (function) — lines 245-266
- `StageStrip` (function) — lines 268-299
- `QuadrantsCard` (function) — lines 304-313
- `quadrantSummary` (function) — lines 315-328
- `overlaps` (function) — lines 317-317
- `QuadrantRow` (function) — lines 330-363
- `TopAppsCard` (function) — lines 368-399
- `AppBarRow` (function) — lines 401-437
- `SectionCard` (function) — lines 442-460
- `MetricRow` (function) — lines 462-476
- `formatDuration` (function) — lines 478-482
