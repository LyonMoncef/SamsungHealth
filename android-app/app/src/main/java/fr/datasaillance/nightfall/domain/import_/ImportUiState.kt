package fr.datasaillance.nightfall.domain.import_

sealed class ImportUiState {
    object Idle : ImportUiState()
    object Connecting : ImportUiState()
    data class ConnectionFailed(val message: String) : ImportUiState()
    object Connected : ImportUiState()
    object Selecting : ImportUiState()
    data class Uploading(
        val currentType: ImportDataType,
        val progress: Float,
        val completedTypes: List<ImportDataType>,
        val skippedTypes: List<ImportDataType>,
    ) : ImportUiState()
    data class Success(val results: List<ImportResult>) : ImportUiState()
    data class Error(val message: String, val retryable: Boolean) : ImportUiState()
}
