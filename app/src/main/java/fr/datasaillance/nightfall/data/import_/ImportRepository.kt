package fr.datasaillance.nightfall.data.import_

import android.content.ContentResolver
import android.net.Uri
import fr.datasaillance.nightfall.domain.import_.ImportDataType
import fr.datasaillance.nightfall.domain.import_.ImportResult
import java.io.IOException

data class CsvEntry(val uri: Uri, val size: Long)

interface ImportRepository {
    suspend fun pingBackend(): Boolean

    suspend fun extractCsvEntries(
        contentResolver: ContentResolver,
        treeUri: Uri,
    ): Map<ImportDataType, CsvEntry>

    @Throws(IOException::class)
    suspend fun uploadCsv(
        contentResolver: ContentResolver,
        uri: Uri,
        type: ImportDataType,
        totalBytes: Long,
        onProgress: (Float) -> Unit,
    ): ImportResult
}
