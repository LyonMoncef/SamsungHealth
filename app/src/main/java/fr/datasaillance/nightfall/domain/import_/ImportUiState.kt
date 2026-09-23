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
    data class Success(
        val results: List<ImportResult>,
        val missingTypes: List<ImportDataType> = emptyList(),
    ) : ImportUiState()
    data class Error(val message: String, val retryable: Boolean) : ImportUiState()

    // --- Google Timeline (local-only) ---
    object LocationImporting : ImportUiState()
    data class LocationSuccess(
        val visitsInserted: Int,
        val visitsSkipped: Int,
        val segmentsInserted: Int,
        val segmentsSkipped: Int,
        val filesProcessed: Int,
    ) : ImportUiState()
    data class LocationError(val message: String) : ImportUiState()
}
