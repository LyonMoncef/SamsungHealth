package fr.datasaillance.nightfall.core.model

import java.time.Instant
import java.time.ZoneOffset

/**
 * Une session de sommeil telle que Health Connect la fournit (contrat v1).
 *
 * Faits bruts uniquement : aucune interprétation (date de la nuit, sieste,
 * dédup entre sources…). Voir spec 2026-09-23-phase1-data-foundation, DT-2 et DT-3.
 */
data class SleepRecord(
    val id: String,
    val start: Instant,
    val end: Instant,
    val startOffset: ZoneOffset?,
    val endOffset: ZoneOffset?,
    val source: String,
    val recordingMethod: RecordingMethod,
    val lastModified: Instant,
    val stages: List<SleepStage>,
)

data class SleepStage(
    val start: Instant,
    val end: Instant,
    val type: StageType,
)

/** Les 8 stades de Health Connect, en 1:1 (STAGE_TYPE_UNKNOWN = 0 … STAGE_TYPE_AWAKE_IN_BED = 7). */
enum class StageType {
    UNKNOWN,
    AWAKE,
    SLEEPING,
    OUT_OF_BED,
    LIGHT,
    DEEP,
    REM,
    AWAKE_IN_BED,
}
