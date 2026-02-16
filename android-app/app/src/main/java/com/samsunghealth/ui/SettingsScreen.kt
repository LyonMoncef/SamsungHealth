package com.samsunghealth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samsunghealth.viewmodel.SyncViewModel

@Composable
fun SettingsScreen(
    viewModel: SyncViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var urlText by remember(state.backendUrl) { mutableStateOf(state.backendUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = urlText,
            onValueChange = { urlText = it },
            label = { Text("Backend URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            viewModel.setBackendUrl(urlText)
            onBack()
        }) {
            Text("Save")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(onClick = onBack) {
            Text("Back")
        }
    }
}
