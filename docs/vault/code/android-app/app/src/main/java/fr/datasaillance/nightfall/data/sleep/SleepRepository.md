---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepository.kt
git_blob: 8afdf07ec5313001294fbc9c250c660af2fadb10
last_synced: '2026-05-09T14:31:04Z'
loc: 10
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepository.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepository.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepository.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.sleep

import java.time.LocalDate

interface SleepRepository {
    suspend fun getSessions(
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): Result<List<SleepSessionResponse>>
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepRepository` (class) — lines 5-10
- `getSessions` (function) — lines 6-9
