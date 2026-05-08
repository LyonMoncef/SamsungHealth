---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportDataType.kt
git_blob: 75a78c149d7c279b5ed43b535ede149ab573ec93
last_synced: '2026-05-08T06:09:46Z'
loc: 35
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportDataType.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportDataType.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportDataType.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.domain.import_

import fr.datasaillance.nightfall.R

enum class ImportDataType(
    val samsungFilenamePrefixes: List<String>,
    val apiPath: String,
    val labelRes: Int,
    val iconRes: Int,
) {
    SLEEP(
        samsungFilenamePrefixes = listOf("com.samsung.shealth.sleep", "com.samsung.health.sleep"),
        apiPath = "api/sleep/import",
        labelRes = R.string.import_type_sleep,
        iconRes = R.drawable.ic_import_sleep,
    ),
    HEART_RATE(
        samsungFilenamePrefixes = listOf("com.samsung.shealth.tracker.heart_rate", "com.samsung.health.heart_rate"),
        apiPath = "api/heartrate/import",
        labelRes = R.string.import_type_heartrate,
        iconRes = R.drawable.ic_import_heartrate,
    ),
    STEPS(
        samsungFilenamePrefixes = listOf("com.samsung.shealth.step_daily_trend", "com.samsung.health.step_daily_trend"),
        apiPath = "api/steps/import",
        labelRes = R.string.import_type_steps,
        iconRes = R.drawable.ic_import_steps,
    ),
    EXERCISE(
        samsungFilenamePrefixes = listOf("com.samsung.shealth.exercise", "com.samsung.health.exercise"),
        apiPath = "api/exercise/import",
        labelRes = R.string.import_type_exercise,
        iconRes = R.drawable.ic_import_exercise,
    ),
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ImportDataType` (class) — lines 5-35
