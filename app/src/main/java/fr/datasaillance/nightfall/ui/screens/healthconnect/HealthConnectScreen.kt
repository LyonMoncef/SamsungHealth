package fr.datasaillance.nightfall.ui.screens.healthconnect

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.data.healthconnect.HealthConnectPermissions
import fr.datasaillance.nightfall.ui.components.DsCard
import fr.datasaillance.nightfall.ui.components.DsTopBar
import fr.datasaillance.nightfall.ui.components.StatusChip
import fr.datasaillance.nightfall.ui.components.StatusTone
import fr.datasaillance.nightfall.viewmodel.healthconnect.HealthConnectUiState
import fr.datasaillance.nightfall.viewmodel.healthconnect.HealthConnectViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Écran Health Connect : état de l'accès, demande d'autorisation, et résumé de ce qui a été lu.
 * Sert aussi de vérification terrain (spec TA-13) : le nombre de sessions et la plus ancienne
 * doivent être cohérents avec darkhour.
 */
@Composable
fun HealthConnectScreen(
    viewModel: HealthConnectViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.onPermissionsResult() }
    val askPermissions = { permissionLauncher.launch(HealthConnectPermissions.ALL) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            DsTopBar(
                title = "Health Connect",
                eyebrow = "Source des données",
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
                when (val state = uiState) {
                    HealthConnectUiState.Checking -> Progress("Vérification de Health Connect…")
                    HealthConnectUiState.Loading -> Progress("Lecture de tout l'historique…")
                    HealthConnectUiState.NotInstalled -> Message(
                        "Health Connect n'est pas disponible sur ce téléphone. Installez-le depuis le Play Store, puis revenez ici.",
                    )
                    HealthConnectUiState.UpdateRequired -> Message(
                        "Health Connect doit être mis à jour depuis le Play Store avant de pouvoir être utilisé.",
                    )
                    HealthConnectUiState.PermissionsMissing -> {
                        Message(
                            "Nightfall a besoin de lire votre sommeil, vos pas et leur historique complet. " +
                                "Ces données restent sur ce téléphone : rien n'est envoyé nulle part.",
                        )
                        Button(onClick = askPermissions, modifier = Modifier.fillMaxWidth()) {
                            Text("Autoriser l'accès")
                        }
                    }
                    is HealthConnectUiState.Loaded -> Loaded(state, onAskHistory = askPermissions, onReload = viewModel::refresh)
                    is HealthConnectUiState.Error -> {
                        Message(state.message)
                        Button(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) {
                            Text("Réessayer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Loaded(state: HealthConnectUiState.Loaded, onAskHistory: () -> Unit, onReload: () -> Unit) {
    DsCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${state.sleepSessionsCount} sessions de sommeil", style = MaterialTheme.typography.titleMedium)
            Text(
                text = state.oldestSleepStart?.let { "La plus ancienne : ${formatDate(it)}" } ?: "Aucune session de sommeil",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.stepsError == null) {
                Text("${state.hourlyStepsCount} heures avec des pas", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    "Pas illisibles : ${state.stepsError}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    when (state.historyAccess) {
        HistoryAccess.FULL -> StatusChip(text = "Historique complet", tone = StatusTone.SUCCESS)
        HistoryAccess.LIMITED_30_DAYS -> {
            StatusChip(text = "Historique limité à 30 jours", tone = StatusTone.WARNING)
            Message(
                "Seuls les 30 derniers jours sont accessibles : l'accès à l'historique n'est pas accordé, " +
                    "ou votre version de Health Connect ne le propose pas.",
            )
            Button(onClick = onAskHistory, modifier = Modifier.fillMaxWidth()) {
                Text("Autoriser l'historique complet")
            }
        }
    }
    OutlinedButton(onClick = onReload, modifier = Modifier.fillMaxWidth()) {
        Text("Relire Health Connect")
    }
}

@Composable
private fun Progress(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator()
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Message(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

private fun formatDate(instant: Instant): String = instant.atZone(ZoneId.systemDefault()).format(dateFormat)
