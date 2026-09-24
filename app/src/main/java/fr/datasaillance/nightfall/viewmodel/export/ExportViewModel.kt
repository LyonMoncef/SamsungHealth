package fr.datasaillance.nightfall.viewmodel.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.core.export.ExportArchive
import fr.datasaillance.nightfall.core.export.buildExportManifest
import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.data.healthconnect.HealthData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.OutputStream
import java.time.Instant

/** Ce que l'écran d'export affiche. */
sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Exporting : ExportUiState
    data object NotReady : ExportUiState
    data class Done(val sleepSessionsCount: Int, val stepsCount: Int, val historyAccess: HistoryAccess) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

/**
 * Export des données brutes (spec DT-7) : relit Health Connect, puis écrit l'archive ZIP
 * (3 CSV + manifeste) dans le fichier que l'utilisateur a choisi.
 *
 * Dépendances passées en paramètres pour tester sans téléphone : la lecture, l'horloge,
 * et le dispatcher d'écriture (hors fil principal en temps normal).
 */
class ExportViewModel(
    private val loadData: suspend () -> HealthData?,
    private val appVersion: String,
    private val clock: () -> Instant = { Instant.now() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    /** [openOutput] ouvre le fichier choisi par l'utilisateur ; il n'est appelé que si les données sont prêtes. */
    fun export(openOutput: () -> OutputStream?) {
        viewModelScope.launch {
            _uiState.value = ExportUiState.Exporting
            try {
                val data = loadData()
                if (data == null) {
                    _uiState.value = ExportUiState.NotReady
                    return@launch
                }
                val manifest = buildExportManifest(data.sleep, data.steps, clock(), appVersion, data.historyAccess)
                withContext(ioDispatcher) {
                    val output = openOutput() ?: error("Impossible d'ouvrir le fichier choisi")
                    output.use { ExportArchive.write(it, data.sleep, data.steps, manifest) }
                }
                _uiState.value = ExportUiState.Done(data.sleep.size, data.steps.size, data.historyAccess)
                // Uniquement des comptes : aucune donnée de santé dans les logs.
                Timber.i("scope=export done sleep=${data.sleep.size} steps=${data.steps.size}")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "scope=export failed")
                _uiState.value = ExportUiState.Error("L'export a échoué. ${error::class.simpleName} — ${error.message}")
            }
        }
    }
}
