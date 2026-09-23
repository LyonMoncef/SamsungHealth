package fr.datasaillance.nightfall.ui.screens.sleep

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.ui.components.DsTopBar
import fr.datasaillance.nightfall.viewmodel.sleep.SleepUiState
import fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel

@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    onSessionClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DsTopBar(title = "Sommeil", eyebrow = "Vos nuits")

            when (val state = uiState) {
                is SleepUiState.Idle -> {}

                is SleepUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("sleep_loading"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                is SleepUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("sleep_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(state.sessions, key = { it.id }) { session ->
                            SleepNightCard(
                                session = session,
                                onClick = { onSessionClick(session.id) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                is SleepUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("sleep_empty"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pas encore de signaux. Connectez une source pour lever l'ancre.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }

                is SleepUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .testTag("sleep_error"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.retry() },
                                modifier = Modifier.testTag("sleep_retry"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Réessayer")
                            }
                        }
                    }
                }
            }
        }
    }
}
