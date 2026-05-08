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
