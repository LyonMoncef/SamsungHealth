---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/SleepUiState.kt
git_blob: fd1ae630e074b8039e6b7ed00e39cb85e2bbf18f
last_synced: '2026-05-09T04:03:35Z'
loc: 11
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/SleepUiState.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/SleepUiState.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/SleepUiState.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.viewmodel.sleep

import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse

sealed class SleepUiState {
    object Idle : SleepUiState()
    object Loading : SleepUiState()
    object Empty : SleepUiState()
    data class Success(val sessions: List<SleepSessionResponse>) : SleepUiState()
    data class Error(val message: String) : SleepUiState()
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepUiState` (class) — lines 5-11
- `Success` (class) — lines 9-9
- `Error` (class) — lines 10-10
