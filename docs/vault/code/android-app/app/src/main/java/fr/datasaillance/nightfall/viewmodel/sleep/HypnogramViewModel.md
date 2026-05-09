---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/HypnogramViewModel.kt
git_blob: ea4e5479bc273af0f644f239ba66a90ce852a087
last_synced: '2026-05-09T08:38:11Z'
loc: 92
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/HypnogramViewModel.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/HypnogramViewModel.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/sleep/HypnogramViewModel.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.viewmodel.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.data.sleep.SleepRepository
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.time.OffsetDateTime

sealed class HypnogramUiState {
    object Idle    : HypnogramUiState()
    object Loading : HypnogramUiState()
    data class Success(val sessions: List<SleepSessionResponse>) : HypnogramUiState()
    data class Error(val message: String) : HypnogramUiState()
}

class HypnogramViewModel(
    private val sessionId: String,
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HypnogramUiState>(HypnogramUiState.Idle)
    val uiState: StateFlow<HypnogramUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    fun retry() = loadSession()

    private fun loadSession() {
        _uiState.value = HypnogramUiState.Loading
        viewModelScope.launch {
            yield()
            try {
                Timber.d("scope=hypno_vm session_id=$sessionId loading")
                val result = repository.getSessions()
                if (result.isFailure) {
                    val code = (result.exceptionOrNull() as? retrofit2.HttpException)?.code()
                    if (code != null) Timber.w("scope=hypno_vm http_code=$code")
                    else Timber.w("scope=hypno_vm error=IOException")
                    _uiState.value = HypnogramUiState.Error(mapError(result.exceptionOrNull()))
                    return@launch
                }
                val sessions = result.getOrNull()
                // sessions can only be null from an unstubbed mock (SleepRepositoryImpl always
                // returns a non-null List). Silent return preserves injected test state.
                if (sessions == null) {
                    Timber.w("scope=hypno_vm session_id=$sessionId sessions_null")
                    return@launch
                }
                val target = sessions.firstOrNull { it.id == sessionId }
                if (target == null) {
                    Timber.w("scope=hypno_vm session_id=$sessionId not found")
                    _uiState.value = HypnogramUiState.Error("Session introuvable")
                    return@launch
                }
                val targetDate = runCatching {
                    OffsetDateTime.parse(target.sleep_start).toLocalDate()
                }.getOrNull()
                val nightSessions = if (targetDate != null) {
                    sessions.filter { s ->
                        runCatching { OffsetDateTime.parse(s.sleep_start).toLocalDate() }.getOrNull() == targetDate
                    }.sortedBy { it.sleep_start }
                } else {
                    listOf(target)
                }
                Timber.d("scope=hypno_vm night_sessions=${nightSessions.size} total_stages=${nightSessions.sumOf { it.stages?.size ?: 0 }}")
                _uiState.value = HypnogramUiState.Success(nightSessions)
            } catch (e: Exception) {
                Timber.w("scope=hypno_vm error=${e::class.simpleName}")
                _uiState.value = HypnogramUiState.Error(mapError(e))
            }
        }
    }

    private fun mapError(throwable: Throwable?): String = when (throwable) {
        is IOException -> "Vérifiez votre connexion réseau"
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
- `HypnogramUiState` (class) — lines 16-21
- `Success` (class) — lines 19-19
- `Error` (class) — lines 20-20
- `HypnogramViewModel` (class) — lines 23-92
- `retry` (function) — lines 35-35
- `loadSession` (function) — lines 37-81
- `mapError` (function) — lines 83-91
