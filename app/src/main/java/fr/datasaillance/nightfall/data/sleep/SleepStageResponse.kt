package fr.datasaillance.nightfall.data.sleep

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SleepStageResponse(
    val id: String,
    val session_id: String,
    @SerialName("stage_type") val stage: String,
    val stage_start: String,
    val stage_end: String
)
