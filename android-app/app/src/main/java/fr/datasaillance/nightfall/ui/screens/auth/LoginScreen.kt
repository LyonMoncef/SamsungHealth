package fr.datasaillance.nightfall.ui.screens.auth

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthErrorMessage
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthPrimaryButton
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthTextField
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
            Text(
                text = "Connexion",
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
                modifier = Modifier.fillMaxWidth().semantics {
                    testTag = "field_email"
                    contentDescription = "Adresse email"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Mot de passe",
                isPassword = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().semantics {
                    testTag = "field_password"
                    contentDescription = "Mot de passe"
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (loginState is LoginUiState.Error) {
                AuthErrorMessage(
                    message = (loginState as LoginUiState.Error).message,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            AuthPrimaryButton(
                text = "Se connecter",
                onClick = { viewModel.login(email, password) },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth().semantics { testTag = "btn_login" }
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.fillMaxWidth().semantics { testTag = "btn_google_oauth" }
            ) {
                Text("Continuer avec Google")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateForgotPassword,
                modifier = Modifier.semantics { testTag = "link_forgot_password" }
            ) {
                Text(
                    text = "Mot de passe oublié ?",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            TextButton(onClick = onNavigateRegister) {
                Text(
                    text = "Créer un compte",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
