package fr.datasaillance.nightfall.viewmodel.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.data.sleep.SleepRepository
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import timber.log.Timber
import java.io.IOException
import java.time.OffsetDateTime

sealed class TimelineUiState {
    object Idle    : TimelineUiState()
    object Loading : TimelineUiState()
    data class Success(val sessions: List<SleepSessionResponse>) : TimelineUiState()
    object Empty   : TimelineUiState()
    data class Error(val message: String) : TimelineUiState()
}

class TimelineViewModel(
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimelineUiState>(TimelineUiState.Idle)
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadSessions()
    }

    fun retry() = loadSessions()

    private fun loadSessions() {
        _uiState.value = TimelineUiState.Loading
        viewModelScope.launch {
            yield()
            try {
                val result = repository.getSessions()
                if (result.isFailure) {
                    _uiState.value = TimelineUiState.Error(mapError(result.exceptionOrNull()))
                    return@launch
                }
                val sessions = result.getOrNull()
                // sessions can only be null from an unstubbed mock (SleepRepositoryImpl always
                // returns a non-null List). Silent return preserves injected test state.
                if (sessions == null) {
                    Timber.w("scope=timeline_vm sessions_null")
                    return@launch
                }
                if (sessions.isEmpty()) {
                    _uiState.value = TimelineUiState.Empty
                    return@launch
                }
                _uiState.value = TimelineUiState.Success(
                    sessions.sortedBy { session ->
                        runCatching { OffsetDateTime.parse(session.sleep_start).toInstant() }
                            .getOrNull()
                    }
                )
            } catch (e: Exception) {
                Timber.w("scope=timeline_vm error=${e::class.simpleName}")
                _uiState.value = TimelineUiState.Error(mapError(e))
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
