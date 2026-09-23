package fr.datasaillance.nightfall.core.model

import java.time.Instant
import java.time.ZoneOffset

/** Un intervalle de pas tel que Health Connect le fournit (contrat v1). */
data class StepsInterval(
    val id: String,
    val start: Instant,
    val end: Instant,
    val startOffset: ZoneOffset?,
    val endOffset: ZoneOffset?,
    val count: Long,
    val source: String,
    val recordingMethod: RecordingMethod,
    val lastModified: Instant,
)
