---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/profile/ProfileScreen.kt
git_blob: 4488f7bfe72ccdb79038711701a52bb68466c9b0
last_synced: '2026-05-07T00:48:24Z'
loc: 38
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/profile/ProfileScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/profile/ProfileScreen.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/profile/ProfileScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ProfileScreen` (function) — lines 15-38
