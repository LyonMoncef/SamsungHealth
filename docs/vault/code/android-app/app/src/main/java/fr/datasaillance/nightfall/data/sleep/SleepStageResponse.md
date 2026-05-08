---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepStageResponse.kt
git_blob: 5606b9f467a7cc9214655fe191da408a366fecae
last_synced: '2026-05-08T01:27:05Z'
loc: 13
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepStageResponse.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepStageResponse.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepStageResponse.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.sleep

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SleepStageResponse(
    val id: String,
    val session_id: String,
    @SerialName("stage_type") val stage: String,
    val stage_start: String,
    val stage_end: String
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepStageResponse` (class) — lines 6-13
