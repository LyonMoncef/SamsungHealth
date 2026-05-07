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
