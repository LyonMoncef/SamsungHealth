package fr.datasaillance.nightfall.data.healthconnect

import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import fr.datasaillance.nightfall.core.model.RecordingMethod
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.SleepStage
import fr.datasaillance.nightfall.core.model.StageType
import fr.datasaillance.nightfall.core.model.StepsInterval

// Conversion Health Connect → contrat v1 (spec 2026-09-23-phase1-data-foundation, DT-2 et DT-5).
// On recopie les faits tels quels : aucune interprétation ici.

fun SleepSessionRecord.toSleepRecord(): SleepRecord {
    val convertedStages = mutableListOf<SleepStage>()
    for (stage in stages) {
        convertedStages.add(SleepStage(stage.startTime, stage.endTime, stageTypeFrom(stage.stage)))
    }
    return SleepRecord(
        id = metadata.id,
        start = startTime,
        end = endTime,
        startOffset = startZoneOffset,
        endOffset = endZoneOffset,
        source = metadata.dataOrigin.packageName,
        recordingMethod = recordingMethodFrom(metadata.recordingMethod),
        lastModified = metadata.lastModifiedTime,
        stages = convertedStages,
    )
}

fun StepsRecord.toStepsInterval(): StepsInterval = StepsInterval(
    id = metadata.id,
    start = startTime,
    end = endTime,
    startOffset = startZoneOffset,
    endOffset = endZoneOffset,
    count = count,
    source = metadata.dataOrigin.packageName,
    recordingMethod = recordingMethodFrom(metadata.recordingMethod),
    lastModified = metadata.lastModifiedTime,
)

/** Code de stade Health Connect → StageType. Un code inconnu (version future de Health Connect) donne UNKNOWN. */
fun stageTypeFrom(code: Int): StageType = when (code) {
    SleepSessionRecord.STAGE_TYPE_UNKNOWN -> StageType.UNKNOWN
    SleepSessionRecord.STAGE_TYPE_AWAKE -> StageType.AWAKE
    SleepSessionRecord.STAGE_TYPE_SLEEPING -> StageType.SLEEPING
    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> StageType.OUT_OF_BED
    SleepSessionRecord.STAGE_TYPE_LIGHT -> StageType.LIGHT
    SleepSessionRecord.STAGE_TYPE_DEEP -> StageType.DEEP
    SleepSessionRecord.STAGE_TYPE_REM -> StageType.REM
    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> StageType.AWAKE_IN_BED
    else -> StageType.UNKNOWN
}

/** Méthode d'enregistrement Health Connect → RecordingMethod. Une valeur inconnue donne UNKNOWN. */
fun recordingMethodFrom(code: Int): RecordingMethod = when (code) {
    Metadata.RECORDING_METHOD_ACTIVELY_RECORDED -> RecordingMethod.ACTIVELY_RECORDED
    Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> RecordingMethod.AUTOMATICALLY_RECORDED
    Metadata.RECORDING_METHOD_MANUAL_ENTRY -> RecordingMethod.MANUAL_ENTRY
    else -> RecordingMethod.UNKNOWN
}
