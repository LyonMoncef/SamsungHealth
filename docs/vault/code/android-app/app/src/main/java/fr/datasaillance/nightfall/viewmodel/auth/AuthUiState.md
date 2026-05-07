---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/auth/AuthUiState.kt
git_blob: f15b74f2c8680c36ad2fbad88b14a3c0cdd5e87e
last_synced: '2026-05-07T02:02:39Z'
loc: 22
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/auth/AuthUiState.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/auth/AuthUiState.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/auth/AuthUiState.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.viewmodel.auth

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    object Sent : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LoginUiState` (class) — lines 3-8
- `Error` (class) — lines 7-7
- `RegisterUiState` (class) — lines 10-15
- `Error` (class) — lines 14-14
- `ForgotPasswordUiState` (class) — lines 17-22
- `Error` (class) — lines 21-21
