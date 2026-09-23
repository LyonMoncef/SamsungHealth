package fr.datasaillance.nightfall.domain.import_

data class ImportResult(
    val type: ImportDataType,
    val inserted: Int,
    val skipped: Int,
    val errorMessage: String? = null,
)
