package fr.datasaillance.nightfall.viewmodel.healthconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.data.healthconnect.HealthConnectState
import fr.datasaillance.nightfall.data.healthconnect.HealthDataCache
import fr.datasaillance.nightfall.data.healthconnect.HealthRecordsSource
import fr.datasaillance.nightfall.data.healthconnect.loadHealthData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant

/** Ce que l'écran Health Connect affiche. */
sealed interface HealthConnectUiState {
    data object Checking : HealthConnectUiState
    data object NotInstalled : HealthConnectUiState
    data object UpdateRequired : HealthConnectUiState
    data object PermissionsMissing : HealthConnectUiState
    data object Loading : HealthConnectUiState
    data class Loaded(
        val sleepSessionsCount: Int,
        val oldestSleepStart: Instant?,
        val stepsIntervalsCount: Int,
        val historyAccess: HistoryAccess,
    ) : HealthConnectUiState
    data class Error(val message: String) : HealthConnectUiState
}

/**
 * Vérifie l'état de Health Connect, lit toutes les données quand c'est possible,
 * les dépose dans le cache partagé et résume la lecture pour l'écran.
 *
 * Les dépendances sont passées en paramètres (état, source, cache) pour pouvoir tester sans téléphone.
 */
class HealthConnectViewModel(
    private val checkState: suspend () -> HealthConnectState,
    private val newSource: () -> HealthRecordsSource,
    private val cache: HealthDataCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthConnectUiState>(HealthConnectUiState.Checking)
    val uiState: StateFlow<HealthConnectUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HealthConnectUiState.Checking
            try {
                when (val state = checkState()) {
                    HealthConnectState.NotInstalled -> _uiState.value = HealthConnectUiState.NotInstalled
                    HealthConnectState.UpdateRequired -> _uiState.value = HealthConnectUiState.UpdateRequired
                    HealthConnectState.PermissionsMissing -> _uiState.value = HealthConnectUiState.PermissionsMissing
                    is HealthConnectState.Ready -> {
                        _uiState.value = HealthConnectUiState.Loading
                        val data = loadHealthData(newSource(), state.historyAccess)
                        cache.update(data)
                        _uiState.value = HealthConnectUiState.Loaded(
                            sleepSessionsCount = data.sleep.size,
                            oldestSleepStart = data.sleep.minOfOrNull { it.start },
                            stepsIntervalsCount = data.steps.size,
                            historyAccess = data.historyAccess,
                        )
                        // Uniquement des comptes : aucune donnée de santé dans les logs.
                        Timber.i("scope=health_connect loaded sleep=${data.sleep.size} steps=${data.steps.size} history=${data.historyAccess}")
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w("scope=health_connect load_failed error=${error::class.simpleName}")
                _uiState.value = HealthConnectUiState.Error("Lecture Health Connect impossible (${error::class.simpleName}).")
            }
        }
    }

    /** Appelé au retour de l'écran de permissions Health Connect : on revérifie tout. */
    fun onPermissionsResult() = refresh()
}
