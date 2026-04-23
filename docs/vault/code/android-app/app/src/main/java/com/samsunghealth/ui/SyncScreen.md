---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/com/samsunghealth/ui/SyncScreen.kt
git_blob: 5e5c7c6b4533493715cd539280c79326589b8fb4
last_synced: '2026-04-23T09:43:48Z'
loc: 75
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/com/samsunghealth/ui/SyncScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/com/samsunghealth/ui/SyncScreen.kt`](../../../android-app/app/src/main/java/com/samsunghealth/ui/SyncScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package com.samsunghealth.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samsunghealth.viewmodel.SyncViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncScreen(
    viewModel: SyncViewModel,
    onRequestPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Sleep Sync", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(32.dp))

        if (!state.sdkAvailable) {
            Text("Health Connect is not available on this device.")
        } else if (!state.hasPermissions) {
            Button(onClick = onRequestPermissions) {
                Text("Grant Sleep Permission")
            }
        } else {
            Button(
                onClick = { viewModel.sync() },
                enabled = !state.syncing,
            ) {
                if (state.syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Sync Now")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.statusMessage.isNotEmpty()) {
            Text(state.statusMessage, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.lastSyncMillis > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            val formatted = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                .format(Date(state.lastSyncMillis))
            Text("Last sync: $formatted", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(onClick = onNavigateToSettings) {
            Text("Settings")
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SyncScreen` (function) — lines 15-75
