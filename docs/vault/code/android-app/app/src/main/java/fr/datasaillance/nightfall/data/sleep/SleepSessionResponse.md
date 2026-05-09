---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepSessionResponse.kt
git_blob: d0fbe0e9c6f37a49ac5d75eb72cd3e7383172208
last_synced: '2026-05-09T04:03:35Z'
loc: 12
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepSessionResponse.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepSessionResponse.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepSessionResponse.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.sleep

import kotlinx.serialization.Serializable

@Serializable
data class SleepSessionResponse(
    val id: String,
    val sleep_start: String,
    val sleep_end: String,
    val created_at: String?,
    val stages: List<SleepStageResponse>?
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepSessionResponse` (class) — lines 5-12
