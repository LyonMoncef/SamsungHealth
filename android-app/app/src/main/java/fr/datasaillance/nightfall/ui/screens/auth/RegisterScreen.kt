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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthErrorMessage
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthPrimaryButton
import fr.datasaillance.nightfall.ui.screens.auth.components.AuthTextField
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
import fr.datasaillance.nightfall.viewmodel.auth.RegisterUiState

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    registrationToken: String? = null
) {
    val registerState by viewModel.registerState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(registerState) {
        if (registerState is RegisterUiState.Success) {
            onRegisterSuccess()
        }
    }

    val isLoading = registerState is RegisterUiState.Loading
    val passwordsMatch = password == confirmPassword && password.isNotEmpty()

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
                text = "Créer un compte",
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

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Mot de passe",
                isPassword = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirmer le mot de passe",
                isPassword = true,
                isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                Spacer(modifier = Modifier.height(8.dp))
                AuthErrorMessage(
                    message = "Les mots de passe ne correspondent pas",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (registerState is RegisterUiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                AuthErrorMessage(
                    message = (registerState as RegisterUiState.Error).message,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AuthPrimaryButton(
                text = "S'inscrire",
                onClick = { viewModel.register(email, password, registrationToken = registrationToken) },
                isLoading = isLoading,
                enabled = passwordsMatch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
