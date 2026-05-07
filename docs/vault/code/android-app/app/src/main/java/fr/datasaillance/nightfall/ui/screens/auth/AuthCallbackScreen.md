---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/AuthCallbackScreen.kt
git_blob: 661a16c9ff6463a154bc65b0d96fedc1c0cc61cc
last_synced: '2026-05-07T02:02:39Z'
loc: 35
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/AuthCallbackScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/AuthCallbackScreen.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/AuthCallbackScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel

@Composable
fun AuthCallbackScreen(
    token: String?,
    viewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (!token.isNullOrBlank()) {
            viewModel.storeTokenFromCallback(token)
            onSuccess()
        } else {
            viewModel.setLoginError("Authentification Google échouée")
            onFailure()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `AuthCallbackScreen` (function) — lines 12-35
