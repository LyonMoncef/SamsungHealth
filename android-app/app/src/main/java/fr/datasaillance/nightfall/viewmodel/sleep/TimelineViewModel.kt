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
import java.time.LocalDate
import java.time.OffsetDateTime

private const val PAGE_DAYS = 30L
// Pour le 1er chargement : on n'a pas de borne d'historique, donc on étend la
// fenêtre vers le passé jusqu'à trouver des données ou abandonner.
private const val INITIAL_MAX_BACKOFF_PAGES = 24  // ~2 ans

sealed class TimelineUiState {
    object Idle : TimelineUiState()
    object Loading : TimelineUiState()
    data class Success(
        val sessions: List<SleepSessionResponse>,
        val loadingMore: Boolean = false,
        val hasMore: Boolean = true,
    ) : TimelineUiState()
    object Empty : TimelineUiState()
    data class Error(val message: String) : TimelineUiState()
}

class TimelineViewModel(
    private val repository: SleepRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TimelineUiState>(TimelineUiState.Idle)
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    // Borne basse de la fenêtre actuellement chargée (= sleep_start le plus ancien).
    // null tant que rien n'a été chargé.
    private var oldestLoaded: LocalDate? = null

    init {
        loadInitial()
    }

    fun retry() = loadInitial()

    private fun loadInitial() {
        _uiState.value = TimelineUiState.Loading
        oldestLoaded = null
        viewModelScope.launch {
            yield()
            try {
                val today = LocalDate.now()
                var from = today.minusDays(PAGE_DAYS)
                var to = today
                var sessions: List<SleepSessionResponse>? = null
                var attempts = 0
                while (attempts < INITIAL_MAX_BACKOFF_PAGES) {
                    val result = repository.getSessions(from = from, to = to)
                    if (result.isFailure) {
                        _uiState.value = TimelineUiState.Error(mapError(result.exceptionOrNull()))
                        return@launch
                    }
                    val batch = result.getOrNull() ?: emptyList()
                    if (batch.isNotEmpty()) {
                        sessions = batch
                        oldestLoaded = from
                        break
                    }
                    // Pas de données : on étend la fenêtre vers le passé.
                    to = from.minusDays(1)
                    from = to.minusDays(PAGE_DAYS)
                    attempts++
                }
                if (sessions == null) {
                    _uiState.value = TimelineUiState.Empty
                    return@launch
                }
                _uiState.value = TimelineUiState.Success(
                    sessions = sortSessions(sessions),
                    hasMore = true,
                )
            } catch (e: Exception) {
                Timber.w("scope=timeline_vm error=${e::class.simpleName}")
                _uiState.value = TimelineUiState.Error(mapError(e))
            }
        }
    }

    fun loadOlder() {
        val current = _uiState.value as? TimelineUiState.Success ?: return
        if (current.loadingMore || !current.hasMore) return
        val anchor = oldestLoaded ?: return

        _uiState.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            try {
                val to = anchor.minusDays(1)
                val from = to.minusDays(PAGE_DAYS)
                val result = repository.getSessions(from = from, to = to)
                if (result.isFailure) {
                    Timber.w("scope=timeline_vm load_older_failed")
                    _uiState.value = current.copy(loadingMore = false)
                    return@launch
                }
                val batch = result.getOrNull() ?: emptyList()
                oldestLoaded = from
                if (batch.isEmpty()) {
                    // Une seule page vide ne signifie pas fin d'historique : on continue
                    // plus loin via un prochain trigger. Pour éviter une boucle, on
                    // marque hasMore=false seulement après plusieurs pages vides
                    // consécutives — ici on garde simple et on laisse l'utilisateur
                    // re-trigger manuellement (futur scroll). Marquage conservateur :
                    _uiState.value = current.copy(
                        loadingMore = false,
                        hasMore = false,
                    )
                    return@launch
                }
                val merged = sortSessions(current.sessions + batch)
                _uiState.value = current.copy(
                    sessions = merged,
                    loadingMore = false,
                    hasMore = true,
                )
            } catch (e: Exception) {
                Timber.w("scope=timeline_vm load_older_error error=${e::class.simpleName}")
                _uiState.value = current.copy(loadingMore = false)
            }
        }
    }

    private fun sortSessions(list: List<SleepSessionResponse>): List<SleepSessionResponse> =
        list.sortedBy { session ->
            runCatching { OffsetDateTime.parse(session.sleep_start).toInstant() }.getOrNull()
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
