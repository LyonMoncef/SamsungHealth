package fr.datasaillance.nightfall.data.import_

import android.content.ContentResolver
import android.net.Uri
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.data.local.import_.LocalImportService
import fr.datasaillance.nightfall.domain.import_.ImportDataType
import fr.datasaillance.nightfall.domain.import_.ImportResult
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

private const val MAX_UNCOMPRESSED_BYTES = 200_000_000L
private const val MAX_ZIP_ENTRIES = 100

/**
 * Phase B local-first : les imports écrivent directement en Room locale via
 * `LocalImportService`. Plus aucun upload réseau santé. L'API n'est conservée
 * que pour le ping de connectivité (vérification VPS up).
 */
class ImportRepositoryImpl(
    private val api: NightfallApi,
    private val localImportService: LocalImportService,
) : ImportRepository {

    override suspend fun pingBackend(): Boolean {
        return try {
            val response = api.health()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private val zipEntryCache = mutableMapOf<ImportDataType, ByteArray>()

    override suspend fun extractCsvEntries(
        contentResolver: ContentResolver,
        treeUri: Uri,
    ): Map<ImportDataType, CsvEntry> {
        zipEntryCache.clear()
        return try {
            extractFromZip(contentResolver, treeUri)
        } catch (e: IOException) {
            emptyMap()
        }
    }

    private fun extractFromZip(
        contentResolver: ContentResolver,
        treeUri: Uri,
    ): Map<ImportDataType, CsvEntry> {
        val result = mutableMapOf<ImportDataType, CsvEntry>()
        val inputStream = contentResolver.openInputStream(treeUri)
            ?: throw IOException("Cannot open URI")

        var totalUncompressed = 0L
        var entryCount = 0

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entryCount >= MAX_ZIP_ENTRIES) {
                    throw IOException("Archive trop grande: dépasse $MAX_ZIP_ENTRIES entrées")
                }
                entryCount++

                val name = entry.name.substringAfterLast('/')
                val matchingType = ImportDataType.entries.firstOrNull { type ->
                    name.startsWith(type.samsungFilenamePrefix) && name.endsWith(".csv")
                }

                if (matchingType != null && matchingType !in result) {
                    val baos = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (zis.read(buffer).also { read = it } != -1) {
                        totalUncompressed += read
                        if (totalUncompressed > MAX_UNCOMPRESSED_BYTES) {
                            throw IOException("Archive trop grande: dépasse ${MAX_UNCOMPRESSED_BYTES / 1_000_000} Mo décompressés")
                        }
                        baos.write(buffer, 0, read)
                    }
                    val bytes = baos.toByteArray()
                    zipEntryCache[matchingType] = bytes
                    result[matchingType] = CsvEntry(uri = treeUri, size = bytes.size.toLong())
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return result
    }

    override suspend fun uploadCsv(
        contentResolver: ContentResolver,
        uri: Uri,
        type: ImportDataType,
        totalBytes: Long,
        onProgress: (Float) -> Unit,
    ): ImportResult {
        val bytes = zipEntryCache[type]
            ?: run {
                contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw IOException("Cannot read file for $type")
            }
        // Pas de progression réseau ici (parsing local quasi-instantané) — on émet 0
        // au début et 1 à la fin pour garder le contrat de l'UI.
        onProgress(0f)
        val r = when (type) {
            ImportDataType.SLEEP -> localImportService.importSleep(bytes)
            ImportDataType.SLEEP_STAGE -> localImportService.importSleepStages(bytes)
            ImportDataType.HEART_RATE -> localImportService.importHeartRate(bytes)
            ImportDataType.STEPS -> localImportService.importSteps(bytes)
            ImportDataType.EXERCISE -> localImportService.importExercise(bytes)
        }
        onProgress(1f)
        return ImportResult(type = type, inserted = r.inserted, skipped = r.skipped)
    }
}
