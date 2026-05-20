package fr.datasaillance.nightfall.ui.screens.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import fr.datasaillance.nightfall.data.local.usage.UsageStatsPermissionHelper
import fr.datasaillance.nightfall.viewmodel.wellbeing.DigitalWellbeingViewModel
import fr.datasaillance.nightfall.viewmodel.wellbeing.PeriodAppStat
import fr.datasaillance.nightfall.viewmodel.wellbeing.WellbeingPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalWellbeingScreen(
    viewModel: DigitalWellbeingViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Bien-être numérique", style = MaterialTheme.typography.headlineMedium) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (!state.permissionGranted) {
                    PermissionCard(
                        onGrant = {
                            val helper = UsageStatsPermissionHelper(context)
                            context.startActivity(helper.intentToGrantPermission())
                        },
                        onRecheck = { viewModel.refresh() },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                StatusCard(
                    permissionGranted = state.permissionGranted,
                    collectedDays = state.collectedDates.size,
                    totalRows = state.totalRows,
                    lastCollectionAtMs = state.lastCollectionAtMs,
                )
                state.lastCollectionEventLabel?.let { label ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (state.permissionGranted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.collectNow(context) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isRefreshing,
                        ) {
                            Text("Collecter maintenant")
                        }
                        OutlinedButton(
                            onClick = { viewModel.backfill(context, days = 7) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isRefreshing,
                        ) {
                            Text("Backfill 7j")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    PeriodChips(
                        selected = state.selectedPeriod,
                        onSelect = { viewModel.setPeriod(it) },
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val totalLabel = formatDuration(state.totalScreenTimeMs)
                    Text(
                        "Top apps · ${state.selectedPeriod.label} · total $totalLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (state.topApps.isEmpty()) {
                        Text(
                            "Pas encore de collecte. Lance 'Collecter maintenant' ou 'Backfill 7j'.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val maxMs = state.topApps.firstOrNull()?.totalForegroundMs ?: 1L
                        state.topApps.forEach { app ->
                            AppPeriodRow(app, maxMs, state.selectedPeriod.days)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit, onRecheck: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.padding(end = 8.dp))
            Text(
                "Accès aux statistiques d'usage requis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Pour collecter ton temps d'écran par application, active 'Usage Access' dans les paramètres Android. Aucune donnée ne quitte l'appareil.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onGrant, modifier = Modifier.weight(1f)) {
                Text("Ouvrir les paramètres")
            }
            OutlinedButton(onClick = onRecheck, modifier = Modifier.weight(1f)) {
                Text("Vérifier")
            }
        }
    }
}

@Composable
private fun StatusCard(
    permissionGranted: Boolean,
    collectedDays: Int,
    totalRows: Int,
    lastCollectionAtMs: Long?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (permissionGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (permissionGranted) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.padding(end = 12.dp))
        Column {
            Text(
                if (permissionGranted) "Collecte active" else "Permission absente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "$collectedDays jour(s) collecté(s) · $totalRows lignes en DB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            lastCollectionAtMs?.let { ms ->
                val time = java.time.Instant.ofEpochMilli(ms)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime()
                val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM HH:mm")
                Text(
                    "Dernière collecte : ${time.format(fmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PeriodChips(
    selected: WellbeingPeriod,
    onSelect: (WellbeingPeriod) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WellbeingPeriod.entries.forEach { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelect(period) },
                label = { Text(period.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun AppPeriodRow(stat: PeriodAppStat, maxMs: Long, periodDays: Int) {
    val ratio = if (maxMs <= 0) 0f else (stat.totalForegroundMs.toFloat() / maxMs.toFloat()).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.displayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (stat.displayLabel != stat.packageName) {
                    Text(
                        text = stat.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = formatDuration(stat.totalForegroundMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // Mini barre proportionnelle vs app la plus utilisée
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.shapes.extraSmall,
                ),
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.shapes.extraSmall,
                    ),
            )
        }
        if (periodDays > 1) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${stat.daysWithUsage}/${periodDays}j",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms < 60_000L) return "${ms / 1000}s"
    val minutes = ms / 60_000L
    if (minutes < 60L) return "${minutes}min"
    val hours = minutes / 60L
    val rem = minutes % 60L
    return if (rem == 0L) "${hours}h" else "${hours}h${rem.toString().padStart(2, '0')}"
}
