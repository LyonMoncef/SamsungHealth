---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/SleepViewModel.kt
git_blob: 3ea5d0473bddc4577d3b72dcd16abad97a3a3ba2
last_synced: '2026-05-09T04:03:35Z'
loc: 54
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/SleepViewModel.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/SleepViewModel.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/SleepViewModel.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.viewmodel.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.data.sleep.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import timber.log.Timber
import java.time.OffsetDateTime

class SleepViewModel(private val repository: SleepRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SleepUiState>(SleepUiState.Idle)
    val uiState: StateFlow<SleepUiState> = _uiState.asStateFlow()

    fun loadSessions() {
        _uiState.value = SleepUiState.Loading
        viewModelScope.launch {
            try {
                yield()
                val result = repository.getSessions()
                _uiState.value = when {
                    result.isFailure -> SleepUiState.Error(mapError(result.exceptionOrNull()))
                    result.getOrNull().isNullOrEmpty() -> SleepUiState.Empty
                    else -> SleepUiState.Success(
                        result.getOrNull()!!.sortedByDescending { session ->
                            runCatching { OffsetDateTime.parse(session.sleep_start).toInstant() }
                                .getOrNull()
                        }
                    )
                }
                Timber.d("sleep_load_complete state=${_uiState.value::class.simpleName}")
            } catch (e: Exception) {
                Timber.w("sleep_load_error")
                _uiState.value = SleepUiState.Error(mapError(e))
            }
        }
    }

    fun retry() = loadSessions()

    private fun mapError(throwable: Throwable?): String = when (throwable) {
        is java.io.IOException -> "Vérifiez votre connexion réseau"
        is retrofit2.HttpException -> when (throwable.code()) {
            401 -> "Session expirée, reconnectez-vous"
            403 -> "Accès refusé"
            else -> "Erreur serveur (${throwable.code()})"
        }
        else -> "Une erreur inattendue est survenue"
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepViewModel` (class) — lines 14-54
- `loadSessions` (function) — lines 19-41
- `retry` (function) — lines 43-43
- `mapError` (function) — lines 45-53
