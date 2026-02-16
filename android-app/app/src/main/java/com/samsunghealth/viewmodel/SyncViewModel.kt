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
                    Instant.now().minus(30, ChronoUnit.DAYS)
                }

                val sessions = healthConnect.readSleepSessions(since)
                if (sessions.isEmpty()) {
                    _state.update { it.copy(syncing = false, statusMessage = "No new sleep sessions found") }
                    return@launch
                }

                _state.update { it.copy(statusMessage = "Pushing ${sessions.size} sessions to backend...") }

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

                val api = ApiClient.create(_state.value.backendUrl)
                val result = api.postSessions(payload)

                val now = System.currentTimeMillis()
                prefs.setLastSync(now)

                _state.update {
                    it.copy(
                        syncing = false,
                        statusMessage = "Synced: ${result.inserted} inserted, ${result.skipped} skipped",
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
