package fr.datasaillance.nightfall.viewmodel.sleep

import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse

sealed class SleepUiState {
    object Idle : SleepUiState()
    object Loading : SleepUiState()
    object Empty : SleepUiState()
    data class Success(val sessions: List<SleepSessionResponse>) : SleepUiState()
    data class Error(val message: String) : SleepUiState()
}
