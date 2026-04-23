---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/com/samsunghealth/ui/SettingsScreen.kt
git_blob: d9891aee487eec418d4b3d8294a6a443fb21e578
last_synced: '2026-04-23T10:17:20Z'
loc: 52
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/com/samsunghealth/ui/SettingsScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/com/samsunghealth/ui/SettingsScreen.kt`](../../../android-app/app/src/main/java/com/samsunghealth/ui/SettingsScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SettingsScreen` (function) — lines 11-52
