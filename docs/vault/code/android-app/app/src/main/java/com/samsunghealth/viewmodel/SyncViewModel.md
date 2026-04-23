---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/com/samsunghealth/viewmodel/SyncViewModel.kt
git_blob: 7ee86f32b874a881924c2ab354a70b5041cdcc3b
last_synced: '2026-04-23T10:40:53Z'
loc: 175
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/com/samsunghealth/viewmodel/SyncViewModel.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/com/samsunghealth/viewmodel/SyncViewModel.kt`](../../../android-app/app/src/main/java/com/samsunghealth/viewmodel/SyncViewModel.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package com.samsunghealth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.samsunghealth.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

data class SyncUiState(
    val backendUrl: String = PreferencesManager.DEFAULT_URL,
    val lastSyncMillis: Long = 0L,
    val syncing: Boolean = false,
    val statusMessage: String = "",
    val sdkAvailable: Boolean = true,
    val hasPermissions: Boolean = false,
)

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    val healthConnect = HealthConnectManager(application)

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.backendUrl.collect { url ->
                _state.update { it.copy(backendUrl = url) }
            }
        }
        viewModelScope.launch {
            prefs.lastSyncMillis.collect { millis ->
                _state.update { it.copy(lastSyncMillis = millis) }
            }
        }
    }

    fun checkSdkAndPermissions() {
        viewModelScope.launch {
            val status = healthConnect.getSdkStatus()
            val available = status == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE
            val hasPerms = if (available) healthConnect.hasAllPermissions() else false
            _state.update { it.copy(sdkAvailable = available, hasPermissions = hasPerms) }
        }
    }

    fun onPermissionsResult() {
        checkSdkAndPermissions()
    }

    fun sync() {
        viewModelScope.launch {
            _state.update { it.copy(syncing = true, statusMessage = "Reading from Health Connect...") }
            try {
                val lastSync = _state.value.lastSyncMillis
                val since = if (lastSync > 0) {
                    Instant.ofEpochMilli(lastSync)
                } else {
                    Instant.ofEpochMilli(0)
                }

                val api = ApiClient.create(_state.value.backendUrl)
                val parts = mutableListOf<String>()

                // Sleep
                _state.update { it.copy(statusMessage = "Reading sleep data...") }
                val sessions = healthConnect.readSleepSessions(since)
                if (sessions.isNotEmpty()) {
                    _state.update { it.copy(statusMessage = "Pushing ${sessions.size} sleep sessions...") }
                    val payload = BulkPayload(
                        sessions = sessions.map { s ->
                            SessionPayload(
                                sleep_start = s.sleepStart,
                                sleep_end = s.sleepEnd,
                                stages = s.stages.map { st ->
                                    StagePayload(
                                        stage_type = st.stageType,
                                        stage_start = st.stageStart,
                                        stage_end = st.stageEnd,
                                    )
                                },
                            )
                        }
                    )
                    val result = api.postSleep(payload)
                    parts.add("Sleep ${result.inserted}/${result.skipped}")
                }

                // Steps
                _state.update { it.copy(statusMessage = "Reading steps data...") }
                val steps = healthConnect.readSteps(since)
                if (steps.isNotEmpty()) {
                    _state.update { it.copy(statusMessage = "Pushing ${steps.size} step records...") }
                    val payload = StepsBulkPayload(
                        records = steps.map { s ->
                            StepsHourlyPayload(
                                date = s.date,
                                hour = s.hour,
                                step_count = s.stepCount,
                            )
                        }
                    )
                    val result = api.postSteps(payload)
                    parts.add("Steps ${result.inserted}/${result.skipped}")
                }

                // Heart Rate
                _state.update { it.copy(statusMessage = "Reading heart rate data...") }
                val heartRate = healthConnect.readHeartRate(since)
                if (heartRate.isNotEmpty()) {
                    _state.update { it.copy(statusMessage = "Pushing ${heartRate.size} HR records...") }
                    val payload = HeartRateBulkPayload(
                        records = heartRate.map { hr ->
                            HeartRateHourlyPayload(
                                date = hr.date,
                                hour = hr.hour,
                                min_bpm = hr.minBpm,
                                max_bpm = hr.maxBpm,
                                avg_bpm = hr.avgBpm,
                                sample_count = hr.sampleCount,
                            )
                        }
                    )
                    val result = api.postHeartRate(payload)
                    parts.add("HR ${result.inserted}/${result.skipped}")
                }

                // Exercise
                _state.update { it.copy(statusMessage = "Reading exercise data...") }
                val exercises = healthConnect.readExerciseSessions(since)
                if (exercises.isNotEmpty()) {
                    _state.update { it.copy(statusMessage = "Pushing ${exercises.size} exercise sessions...") }
                    val payload = ExerciseBulkPayload(
                        sessions = exercises.map { ex ->
                            ExercisePayload(
                                exercise_type = ex.exerciseType,
                                exercise_start = ex.exerciseStart,
                                exercise_end = ex.exerciseEnd,
                                duration_minutes = ex.durationMinutes,
                            )
                        }
                    )
                    val result = api.postExercise(payload)
                    parts.add("Exercise ${result.inserted}/${result.skipped}")
                }

                val now = System.currentTimeMillis()
                prefs.setLastSync(now)

                val message = if (parts.isEmpty()) {
                    "No new data found"
                } else {
                    "Synced: ${parts.joinToString(", ")}"
                }

                _state.update {
                    it.copy(
                        syncing = false,
                        statusMessage = message,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(syncing = false, statusMessage = "Error: ${e.message}") }
            }
        }
    }

    fun setBackendUrl(url: String) {
        viewModelScope.launch { prefs.setBackendUrl(url) }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SyncUiState` (class) — lines 12-19
- `SyncViewModel` (class) — lines 21-175
- `checkSdkAndPermissions` (function) — lines 42-49
- `onPermissionsResult` (function) — lines 51-53
- `sync` (function) — lines 55-170
- `setBackendUrl` (function) — lines 172-174
