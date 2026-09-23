package fr.datasaillance.nightfall.ui.screens.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.ui.components.DsCard
import fr.datasaillance.nightfall.ui.components.DsTopBar
import fr.datasaillance.nightfall.ui.components.StatusChip
import fr.datasaillance.nightfall.ui.components.StatusTone
import fr.datasaillance.nightfall.viewmodel.export.ExportUiState
import fr.datasaillance.nightfall.viewmodel.export.ExportViewModel
import java.time.LocalDate

/**
 * Export des données brutes Health Connect dans un fichier ZIP choisi par l'utilisateur
 * (spec DT-7). L'avertissement « non chiffré » est affiché avant tout export.
 */
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentResolver = LocalContext.current.contentResolver
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        // uri == null : l'utilisateur a annulé le choix du fichier, on ne fait rien.
        if (uri != null) {
            viewModel.export { contentResolver.openOutputStream(uri) }
        }
    }
    val chooseFileAndExport = { createFile.launch("nightfall-export-${LocalDate.now()}.zip") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            DsTopBar(
                title = "Exporter mes données",
                eyebrow = "Données brutes",
                leading = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Crée une archive ZIP avec toutes vos données de sommeil et de pas lues dans Health Connect, " +
                        "au format CSV (sessions, stades, pas) plus un fichier de description.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                DsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(text = "Non chiffré", tone = StatusTone.WARNING)
                        Text(
                            "Ce fichier contient vos données de santé en clair. Enregistrez-le dans un endroit " +
                                "que vous contrôlez, et ne le partagez pas.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                when (val state = uiState) {
                    ExportUiState.Idle -> Button(onClick = chooseFileAndExport, modifier = Modifier.fillMaxWidth()) {
                        Text("Choisir l'emplacement et exporter")
                    }
                    ExportUiState.Exporting -> {
                        CircularProgressIndicator()
                        Text("Lecture de Health Connect et écriture de l'archive…", style = MaterialTheme.typography.bodyMedium)
                    }
                    ExportUiState.NotReady -> {
                        Text(
                            "Health Connect n'est pas encore autorisé. Passez par Profil → Health Connect, puis revenez ici.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(onClick = chooseFileAndExport, modifier = Modifier.fillMaxWidth()) { Text("Réessayer") }
                    }
                    is ExportUiState.Done -> {
                        StatusChip(text = "Export terminé", tone = StatusTone.SUCCESS)
                        Text(
                            "${state.sleepSessionsCount} sessions de sommeil et ${state.stepsCount} mesures de pas exportées.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (state.historyAccess == HistoryAccess.LIMITED_30_DAYS) {
                            Text(
                                "Attention : seuls les 30 derniers jours étaient accessibles (historique Health Connect non autorisé).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Button(onClick = chooseFileAndExport, modifier = Modifier.fillMaxWidth()) { Text("Exporter à nouveau") }
                    }
                    is ExportUiState.Error -> {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Button(onClick = chooseFileAndExport, modifier = Modifier.fillMaxWidth()) { Text("Réessayer") }
                    }
                }
            }
        }
    }
}
