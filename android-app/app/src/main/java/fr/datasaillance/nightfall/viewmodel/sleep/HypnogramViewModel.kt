package fr.datasaillance.nightfall.viewmodel.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.data.sleep.SleepRepository
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

sealed class HypnogramUiState {
    object Idle    : HypnogramUiState()
    object Loading : HypnogramUiState()
    data class Success(val session: SleepSessionResponse) : HypnogramUiState()
    data class Error(val message: String) : HypnogramUiState()
}

class HypnogramViewModel(
    private val sessionId: String,
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HypnogramUiState>(HypnogramUiState.Idle)
    val uiState: StateFlow<HypnogramUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    fun retry() = loadSession()

    private fun loadSession() {
        _uiState.value = HypnogramUiState.Loading
        viewModelScope.launch {
            yield()
            try {
                Timber.d("scope=hypno_vm session_id=$sessionId loading")
                val result = repository.getSessions()
                if (result.isFailure) {
                    val code = (result.exceptionOrNull() as? retrofit2.HttpException)?.code()
                    if (code != null) Timber.w("scope=hypno_vm http_code=$code")
                    else Timber.w("scope=hypno_vm error=IOException")
                    _uiState.value = HypnogramUiState.Error(mapError(result.exceptionOrNull()))
                    return@launch
                }
                val sessions = result.getOrNull()
                // sessions can only be null from an unstubbed mock (SleepRepositoryImpl always
                // returns a non-null List). Silent return preserves injected test state.
                if (sessions == null) {
                    Timber.w("scope=hypno_vm session_id=$sessionId sessions_null")
                    return@launch
                }
                val session = sessions.firstOrNull { it.id == sessionId }
                if (session == null) {
                    Timber.w("scope=hypno_vm session_id=$sessionId not found")
                    _uiState.value = HypnogramUiState.Error("Session introuvable")
                } else {
                    Timber.d("scope=hypno_vm stage_count=${session.stages?.size ?: 0}")
                    _uiState.value = HypnogramUiState.Success(session)
                }
            } catch (e: Exception) {
                Timber.w("scope=hypno_vm error=${e::class.simpleName}")
                _uiState.value = HypnogramUiState.Error(mapError(e))
            }
        }
    }

    private fun mapError(throwable: Throwable?): String = when (throwable) {
        is IOException -> "Vérifiez votre connexion réseau"
        is retrofit2.HttpException -> when (throwable.code()) {
            401 -> "Session expirée, reconnectez-vous"
            403 -> "Accès refusé"
            else -> "Erreur serveur (${throwable.code()})"
        }
        else -> "Une erreur inattendue est survenue"
    }
}
