package fr.datasaillance.nightfall.domain.import_

/** États de l'écran d'import. Depuis la Phase 1.2, seul l'import Google Takeout (lieux) subsiste. */
sealed class ImportUiState {
    object Idle : ImportUiState()

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
