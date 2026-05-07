---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/settings/SettingsScreen.kt
git_blob: 9465166a0ee8d3828b285ca3b0c964127b31d101
last_synced: '2026-05-07T00:48:24Z'
loc: 49
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/settings/SettingsScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/settings/SettingsScreen.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/settings/SettingsScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    currentUrl: String = "",
    onSaveUrl: (String) -> Unit = {}
) {
    var urlInput by remember { mutableStateOf(currentUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Paramètres", style = MaterialTheme.typography.titleLarge)
        Text("URL du serveur Nightfall", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("URL backend") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onSaveUrl(urlInput) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enregistrer")
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SettingsScreen` (function) — lines 20-49
