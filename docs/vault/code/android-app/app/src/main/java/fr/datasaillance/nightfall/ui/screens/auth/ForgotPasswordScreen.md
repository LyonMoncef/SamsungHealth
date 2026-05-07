---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreen.kt
git_blob: d6ad23662f7d735f469adbcd54e2c22413766d98
last_synced: '2026-05-07T02:02:39Z'
loc: 106
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreen.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthPrimaryButton
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthTextField
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
import fr.datasaillance.nightfall.viewmodel.auth.ForgotPasswordUiState

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val forgotState by viewModel.forgotState.collectAsState()

    var email by remember { mutableStateOf("") }

    val isLoading = forgotState is ForgotPasswordUiState.Loading
    val isSent = forgotState is ForgotPasswordUiState.Sent

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSent) {
                Text(
                    text = "Un email a été envoyé si ce compte existe.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onBack) {
                    Text(
                        text = "Retour à la connexion",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                Text(
                    text = "Réinitialiser le mot de passe",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(32.dp))

                AuthTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                AuthPrimaryButton(
                    text = "Envoyer le lien",
                    onClick = { viewModel.requestPasswordReset(email) },
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onBack) {
                    Text(
                        text = "Retour à la connexion",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ForgotPasswordScreen` (function) — lines 30-106
