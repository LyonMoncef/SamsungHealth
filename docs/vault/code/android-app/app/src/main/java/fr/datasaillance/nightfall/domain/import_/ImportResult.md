---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportResult.kt
git_blob: 30bf359f638515070bf27f59abd34e67a1906c13
last_synced: '2026-05-07T03:10:49Z'
loc: 8
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportResult.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportResult.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportResult.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.domain.import_

data class ImportResult(
    val type: ImportDataType,
    val inserted: Int,
    val skipped: Int,
    val errorMessage: String? = null,
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ImportResult` (class) — lines 3-8
