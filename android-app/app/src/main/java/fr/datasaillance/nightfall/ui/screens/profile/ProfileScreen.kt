package fr.datasaillance.nightfall.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    onImport: () -> Unit = {},
    onSettings: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profil", style = MaterialTheme.typography.titleLarge)
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Importer données")
        }
        Button(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Paramètres")
        }
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Se déconnecter")
        }
    }
}
