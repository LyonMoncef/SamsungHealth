package fr.datasaillance.nightfall.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.GoogleStartRequest
import fr.datasaillance.nightfall.data.http.LoginRequest
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.data.http.PasswordResetRequest
import fr.datasaillance.nightfall.data.http.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class AuthViewModel(
    private val api: NightfallApi,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    private val _forgotState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val forgotState: StateFlow<ForgotPasswordUiState> = _forgotState.asStateFlow()

    fun login(email: String, password: String) {
        _loginState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                val response = api.login(LoginRequest(email, password))
                tokenDataStore.saveToken(response.access_token)
                _loginState.value = LoginUiState.Success
            } catch (e: retrofit2.HttpException) {
                _loginState.value = LoginUiState.Error(mapHttpError(e.code()))
            } catch (e: IOException) {
                _loginState.value = LoginUiState.Error("Vérifiez votre connexion réseau")
            }
        }
    }

    fun register(email: String, password: String, registrationToken: String?) {
        _registerState.value = RegisterUiState.Loading
        viewModelScope.launch {
            try {
                api.register(RegisterRequest(email, password), registrationToken)
                _registerState.value = RegisterUiState.Success
            } catch (e: retrofit2.HttpException) {
                _registerState.value = RegisterUiState.Error(mapHttpError(e.code()))
            } catch (e: IOException) {
                _registerState.value = RegisterUiState.Error("Vérifiez votre connexion réseau")
            }
        }
    }

    fun requestPasswordReset(email: String) {
        _forgotState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                api.requestPasswordReset(PasswordResetRequest(email))
            } catch (e: Exception) {
                // intentional fall-through — anti-enum: always emit Sent
            }
            _forgotState.value = ForgotPasswordUiState.Sent
        }
    }

    suspend fun getGoogleStartUrl(): String? = try {
        api.googleStart(GoogleStartRequest()).authorize_url
    } catch (e: Exception) {
        null
    }

    fun storeTokenFromCallback(token: String) {
        tokenDataStore.saveToken(token)
        _loginState.value = LoginUiState.Success
    }

    fun setLoginError(message: String) {
        _loginState.value = LoginUiState.Error(message)
    }

    private fun mapHttpError(code: Int): String = when (code) {
        401 -> "Email ou mot de passe incorrect"
        403 -> "Email non vérifié — consultez votre boîte mail"
        409 -> "Cette adresse email est déjà utilisée"
        400 -> "Mot de passe trop faible (12 caractères minimum, majuscule, chiffre, symbole)"
        else -> "Erreur serveur ($code)"
    }
}
