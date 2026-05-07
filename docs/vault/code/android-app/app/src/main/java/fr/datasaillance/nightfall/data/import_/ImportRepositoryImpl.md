---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepositoryImpl.kt
git_blob: 58de67aea204eea8b2f605bb8e206e90048a0718
last_synced: '2026-05-07T03:10:49Z'
loc: 124
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepositoryImpl.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepositoryImpl.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepositoryImpl.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.import_

import android.content.ContentResolver
import android.net.Uri
import fr.datasaillance.nightfall.data.http.CountingRequestBody
import fr.datasaillance.nightfall.data.http.ImportApiResponse
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.domain.import_.ImportDataType
import fr.datasaillance.nightfall.domain.import_.ImportResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

private const val MAX_UNCOMPRESSED_BYTES = 200_000_000L
private const val MAX_ZIP_ENTRIES = 100

class ImportRepositoryImpl(
    private val api: NightfallApi,
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

        val mediaType = "text/csv".toMediaType()
        val requestBody = bytes.toRequestBody(mediaType)
        val countingBody = CountingRequestBody(
            delegate = requestBody,
            totalBytes = totalBytes,
            onProgress = onProgress,
        )
        val part = MultipartBody.Part.createFormData("file", "${type.samsungFilenamePrefix}.csv", countingBody)

        val response: ImportApiResponse = when (type) {
            ImportDataType.SLEEP -> api.importSleep(part)
            ImportDataType.HEART_RATE -> api.importHeartRate(part)
            ImportDataType.STEPS -> api.importSteps(part)
            ImportDataType.EXERCISE -> api.importExercise(part)
        }
        return ImportResult(type = type, inserted = response.inserted, skipped = response.skipped)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ImportRepositoryImpl` (class) — lines 20-124
- `pingBackend` (function) — lines 24-31
- `extractCsvEntries` (function) — lines 35-45
- `extractFromZip` (function) — lines 47-92
- `uploadCsv` (function) — lines 94-123
