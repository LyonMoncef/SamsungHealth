---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/sleep/SleepRepository.kt
git_blob: 364919410d349ed4baeb81b851d72150ee1c7d95
last_synced: '2026-05-08T01:27:05Z'
loc: 5
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

interface SleepRepository {
    suspend fun getSessions(): Result<List<SleepSessionResponse>>
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepRepository` (class) — lines 3-5
- `getSessions` (function) — lines 4-4
