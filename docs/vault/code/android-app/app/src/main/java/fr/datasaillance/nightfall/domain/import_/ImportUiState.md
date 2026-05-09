---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportUiState.kt
git_blob: 2b4245948793246660c4ba9702a109980ed4d7ee
last_synced: '2026-05-09T03:55:38Z'
loc: 20
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportUiState.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportUiState.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportUiState.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ImportUiState` (class) — lines 3-20
- `ConnectionFailed` (class) — lines 6-6
- `Uploading` (class) — lines 9-14
- `Success` (class) — lines 15-18
- `Error` (class) — lines 19-19
