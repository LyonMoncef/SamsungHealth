---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/LoginScreen.kt
git_blob: 242aea91225a1a5c0a66d0f12bb9c4844a592bf1
last_synced: '2026-05-24T12:38:05Z'
loc: 278
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/LoginScreen.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/LoginScreen.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/auth/LoginScreen.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.datasaillance.nightfall.R
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthErrorMessage
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthTextField
import fr.datasaillance.nightfall.ui.theme.DataSaillance
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
import fr.datasaillance.nightfall.viewmodel.auth.LoginUiState
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateForgotPassword: () -> Unit
) {
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(loginState) {
        if (loginState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    val isLoading = loginState is LoginUiState.Loading
    val isFormValid = email.isNotBlank() && password.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
        ) {
            // Logo + wordmark + tagline
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_dark),
                    contentDescription = "DataSaillance",
                    modifier = Modifier.size(84.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                            append("DATA ")
                        }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                            append("SAILLANCE")
                        }
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Vos données de sommeil. Sur votre infrastructure. Hors du cloud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DataSaillance.extras.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.85f),
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Form
            FieldLabel("Email")
            Spacer(modifier = Modifier.height(6.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "",
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().semantics {
                    testTag = "field_email"
                    contentDescription = "Adresse email"
                },
            )

            Spacer(modifier = Modifier.height(14.dp))

            FieldLabel("Mot de passe", focused = true)
            Spacer(modifier = Modifier.height(6.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "",
                isPassword = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().semantics {
                    testTag = "field_password"
                    contentDescription = "Mot de passe"
                },
            )

            // Forgot password link (right-aligned)
            Box(modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onNavigateForgotPassword,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .semantics { testTag = "link_forgot_password" },
                ) {
                    Text(
                        text = "Mot de passe oublié ?",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (loginState is LoginUiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                AuthErrorMessage(
                    message = (loginState as LoginUiState.Error).message,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Amber — "Prendre la barre"
            Button(
                onClick = { viewModel.login(email, password) },
                enabled = isFormValid && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { testTag = "btn_login" },
            ) {
                Text(
                    text = if (isLoading) "Connexion…" else "Prendre la barre",
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Outline button — "Continuer avec Google"
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val url = viewModel.getGoogleStartUrl()
                        if (url != null) {
                            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                        }
                    }
                },
                enabled = !isLoading,
                border = BorderStroke(1.dp, DataSaillance.extras.borderStrong),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { testTag = "btn_google_oauth" },
            ) {
                Text("Continuer avec Google", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Register link (sub-CTA, kept for completeness)
            TextButton(
                onClick = onNavigateRegister,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = "Créer un compte",
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(36.dp))

            // Footer — privacy chips inline + version
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Local-first · TLS 1.3 · chiffrement AES-256-GCM",
                    style = MaterialTheme.typography.labelSmall,
                    color = DataSaillance.extras.textFaint,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "nightfall · v4.0.0 · sh-prod.datasaillance.fr",
                    style = MaterialTheme.typography.labelSmall,
                    color = DataSaillance.extras.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String, focused: Boolean = false) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = if (focused) MaterialTheme.colorScheme.primary
                else DataSaillance.extras.textMuted,
        letterSpacing = 0.24.sp,
    )
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LoginScreen` (function) — lines 57-266
- `FieldLabel` (function) — lines 268-278
