package fr.datasaillance.nightfall.viewmodel.import_

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.data.import_.ImportRepository
import fr.datasaillance.nightfall.domain.import_.ImportDataType
import fr.datasaillance.nightfall.domain.import_.ImportResult
import fr.datasaillance.nightfall.domain.import_.ImportUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImportViewModel(
    private val repository: ImportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun checkConnection() {
        _uiState.value = ImportUiState.Connecting
        viewModelScope.launch {
            val ok = repository.pingBackend()
            _uiState.value = if (ok) {
                ImportUiState.Connected
            } else {
                ImportUiState.ConnectionFailed("Backend inaccessible — vérifiez l'URL dans les paramètres")
            }
        }
    }

    fun startUpload(contentResolver: ContentResolver, treeUri: Uri) {
        viewModelScope.launch {
            val csvEntries = repository.extractCsvEntries(contentResolver, treeUri)
            if (csvEntries.isEmpty()) {
                _uiState.value = ImportUiState.Error(
                    message = "Aucun fichier Samsung Health reconnu dans l'archive.",
                    retryable = true,
                )
                return@launch
            }
            val results = mutableListOf<ImportResult>()
            val completed = mutableListOf<ImportDataType>()
            val skipped = mutableListOf<ImportDataType>()

            for (type in ImportDataType.entries) {
                val entry = csvEntries[type]
                if (entry == null) {
                    skipped.add(type)
                    continue
                }
                try {
                    _uiState.value = ImportUiState.Uploading(
                        currentType = type,
                        progress = 0f,
                        completedTypes = completed.toList(),
                        skippedTypes = skipped.toList(),
                    )
                    val result = repository.uploadCsv(
                        contentResolver = contentResolver,
                        uri = entry.uri,
                        type = type,
                        totalBytes = entry.size,
                        onProgress = { progress ->
                            _uiState.value = ImportUiState.Uploading(
                                currentType = type,
                                progress = progress,
                                completedTypes = completed.toList(),
                                skippedTypes = skipped.toList(),
                            )
                        },
                    )
                    results.add(result)
                    completed.add(type)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    results.add(
                        ImportResult(
                            type = type,
                            inserted = 0,
                            skipped = 0,
                            errorMessage = e.message,
                        )
                    )
                }
            }
            _uiState.value = ImportUiState.Success(results)
        }
    }

    fun reset() {
        _uiState.value = ImportUiState.Idle
    }
}
