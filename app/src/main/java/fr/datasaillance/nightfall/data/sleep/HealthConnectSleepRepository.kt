package fr.datasaillance.nightfall.data.sleep

import android.content.Context
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.StageType
import fr.datasaillance.nightfall.data.healthconnect.HealthData
import fr.datasaillance.nightfall.data.healthconnect.HealthDataCache
import fr.datasaillance.nightfall.data.healthconnect.loadFromDevice
import fr.datasaillance.nightfall.data.healthconnect.sharedHealthDataCache
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Pont TEMPORAIRE entre le contrat v1 et les écrans hérités (Sommeil, Timeline, Hypnogramme),
// qui attendent encore les anciennes formes `SleepSessionResponse` / `SleepStageResponse`.
// À supprimer avec la refonte des écrans (Phase 4). Spec 2026-09-23-phase1-data-foundation, DT-6.

/**
 * Traduit un stade du contrat dans l'ancien vocabulaire (DEEP / LIGHT / REM / AWAKE), le seul que ces écrans colorent.
 * Les trois formes d'éveil deviennent AWAKE ; SLEEPING et UNKNOWN gardent leur nom brut (affiché tel quel).
 */
fun legacyStageName(type: StageType): String = when (type) {
    StageType.LIGHT -> "LIGHT"
    StageType.DEEP -> "DEEP"
    StageType.REM -> "REM"
    StageType.AWAKE, StageType.AWAKE_IN_BED, StageType.OUT_OF_BED -> "AWAKE"
    StageType.SLEEPING -> "SLEEPING"
    StageType.UNKNOWN -> "UNKNOWN"
}

fun SleepRecord.toSleepSessionResponse(): SleepSessionResponse {
    val convertedStages = mutableListOf<SleepStageResponse>()
    stages.forEachIndexed { index, stage ->
        convertedStages.add(
            SleepStageResponse(
                id = "$id-$index",
                session_id = id,
                stage = legacyStageName(stage.type),
                stage_start = toIso(stage.start),
                stage_end = toIso(stage.end),
            ),
        )
    }
    return SleepSessionResponse(
        id = id,
        sleep_start = toIso(start),
        sleep_end = toIso(end),
        created_at = toIso(lastModified),
        stages = convertedStages,
    )
}

/**
 * `SleepRepository` alimenté par Health Connect, via le cache partagé :
 * - si une lecture est déjà en cache, on s'en sert ;
 * - sinon on lit Health Connect (si c'est possible) et on remplit le cache ;
 * - si Health Connect n'est pas prêt (permissions, installation), on renvoie une liste vide.
 */
class HealthConnectSleepRepository(
    private val cache: HealthDataCache,
    private val loadIfPossible: suspend () -> HealthData?,
) : SleepRepository {

    override suspend fun getSessions(from: LocalDate?, to: LocalDate?): Result<List<SleepSessionResponse>> = runCatching {
        val data = currentData()
        if (data == null) {
            return@runCatching emptyList()
        }
        // Même fenêtre que l'ancien dépôt local : début de nuit dans [from 00:00 UTC, to + 1 jour 00:00 UTC).
        val selected = if (from != null && to != null) {
            val windowStart = from.atStartOfDay(ZoneOffset.UTC).toInstant()
            val windowEnd = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            data.sleep.filter { it.start >= windowStart && it.start < windowEnd }
        } else {
            data.sleep
        }
        selected.map { it.toSleepSessionResponse() }
    }

    /** Pour le Cadran : les sessions du contrat dont le début tombe dans [fromMs, toMs). */
    suspend fun recordsStartingBetween(fromMs: Long, toMs: Long): List<SleepRecord> {
        val data = currentData() ?: return emptyList()
        return data.sleep.filter { it.start.toEpochMilli() >= fromMs && it.start.toEpochMilli() < toMs }
    }

    /** La lecture en cache, sinon une nouvelle lecture Health Connect (qui remplit le cache), sinon null. */
    private suspend fun currentData(): HealthData? = cache.data.value ?: loadIfPossible()?.also { cache.update(it) }
}

/** Le dépôt branché sur le vrai Health Connect du téléphone et sur le cache partagé de l'app. */
fun healthConnectSleepRepository(context: Context): HealthConnectSleepRepository {
    val appContext = context.applicationContext
    return HealthConnectSleepRepository(sharedHealthDataCache) { loadFromDevice(appContext) }
}

private val ISO_FMT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

/** Même format que l'ancien dépôt local : ISO-8601 avec offset UTC ("2024-07-08T22:31:00Z"). */
private fun toIso(instant: Instant): String = instant.atOffset(ZoneOffset.UTC).format(ISO_FMT)
