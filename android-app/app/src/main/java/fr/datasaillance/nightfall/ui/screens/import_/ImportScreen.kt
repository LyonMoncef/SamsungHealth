package fr.datasaillance.nightfall.ui.screens.import_

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.datasaillance.nightfall.R
import fr.datasaillance.nightfall.domain.import_.ImportUiState
import fr.datasaillance.nightfall.viewmodel.import_.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: ImportViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.startUpload(context.contentResolver, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_step_upload)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is ImportUiState.Idle -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                IdleContent(onCheckConnection = { viewModel.checkConnection() })
            }
            is ImportUiState.Connecting -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                ConnectingContent()
            }
            is ImportUiState.ConnectionFailed -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                ConnectionFailedContent(
                    message = state.message,
                    onRetry = { viewModel.checkConnection() },
                )
            }
            is ImportUiState.Connected -> ConnectedContent(
                onSelectFolder = { launcher.launch(null) },
                padding = padding,
            )
            is ImportUiState.Selecting -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                SelectingContent()
            }
            is ImportUiState.Uploading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                UploadingContent(
                    currentType = stringResource(state.currentType.labelRes),
                    progress = state.progress,
                    completedCount = state.completedTypes.size,
                    totalCount = state.completedTypes.size + state.skippedTypes.size + 1,
                )
            }
            is ImportUiState.Success -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                SuccessContent(
                    results = state.results,
                    onDone = {
                        viewModel.reset()
                        onNavigateBack()
                    },
                )
            }
            is ImportUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                ErrorContent(
                    message = state.message,
                    retryable = state.retryable,
                    onRetry = { viewModel.reset() },
                    onBack = onNavigateBack,
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onCheckConnection: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Importer des données Samsung Health",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCheckConnection) {
            Text("Vérifier la connexion")
        }
    }
}

@Composable
private fun ConnectingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Connexion au serveur…")
    }
}

@Composable
private fun ConnectionFailedContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Réessayer")
        }
    }
}

@Composable
private fun ConnectedContent(
    onSelectFolder: () -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Connexion établie",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        RgpdNoticeCard()
        Button(
            onClick = onSelectFolder,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sélectionner le dossier Samsung Health")
        }
    }
}

@Composable
private fun RgpdNoticeCard() {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
            )
            .drawBehind {
                drawRect(color = primary, size = size.copy(width = 4.dp.toPx()))
            }
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                append("Ces données sont envoyées uniquement vers ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = primary)) {
                    append("votre serveur")
                }
                append(". Elles ne transitent par aucun serveur tiers. Aucune copie n'est conservée sur cet appareil après l'import.")
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SelectingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sélectionnez le dossier Samsung Health…")
    }
}

@Composable
private fun UploadingContent(
    currentType: String,
    progress: Float,
    completedCount: Int,
    totalCount: Int,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Import en cours : $currentType",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$completedCount / $totalCount types traités",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SuccessContent(
    results: List<fr.datasaillance.nightfall.domain.import_.ImportResult>,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Import terminé",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        results.forEach { result ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(result.type.name)
                if (result.errorMessage != null) {
                    Text("Erreur", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("${result.inserted} insérés, ${result.skipped} ignorés")
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDone) {
            Text("Terminer")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (retryable) {
            Button(onClick = onRetry) {
                Text("Réessayer")
            }
        } else {
            Button(onClick = onBack) {
                Text("Retour")
            }
        }
    }
}
