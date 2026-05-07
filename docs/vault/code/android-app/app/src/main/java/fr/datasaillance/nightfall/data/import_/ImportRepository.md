---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepository.kt
git_blob: e7cb7ff2c43c6ef7386e75ebcbbc913b86c4e4df
last_synced: '2026-05-07T03:10:49Z'
loc: 27
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepository.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepository.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepository.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `CsvEntry` (class) — lines 9-9
- `ImportRepository` (class) — lines 11-27
- `pingBackend` (function) — lines 12-12
- `extractCsvEntries` (function) — lines 14-17
- `uploadCsv` (function) — lines 19-26
