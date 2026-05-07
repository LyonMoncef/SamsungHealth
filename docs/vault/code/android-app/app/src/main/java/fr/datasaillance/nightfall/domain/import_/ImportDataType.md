---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportDataType.kt
git_blob: 54597c9c952e5cb764bf75abafee457c0d844873
last_synced: '2026-05-07T03:10:49Z'
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
    val samsungFilenamePrefix: String,
    val apiPath: String,
    val labelRes: Int,
    val iconRes: Int,
) {
    SLEEP(
        samsungFilenamePrefix = "com.samsung.health.sleep",
        apiPath = "api/sleep/import",
        labelRes = R.string.import_type_sleep,
        iconRes = R.drawable.ic_import_sleep,
    ),
    HEART_RATE(
        samsungFilenamePrefix = "com.samsung.health.heart_rate",
        apiPath = "api/heartrate/import",
        labelRes = R.string.import_type_heartrate,
        iconRes = R.drawable.ic_import_heartrate,
    ),
    STEPS(
        samsungFilenamePrefix = "com.samsung.health.step_daily_trend",
        apiPath = "api/steps/import",
        labelRes = R.string.import_type_steps,
        iconRes = R.drawable.ic_import_steps,
    ),
    EXERCISE(
        samsungFilenamePrefix = "com.samsung.health.exercise",
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
