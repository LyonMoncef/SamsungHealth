package fr.datasaillance.nightfall.data.sleep

import fr.datasaillance.nightfall.data.local.dao.SleepDao
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Implémentation locale de `SleepRepository` qui lit les données depuis la base
 * Room SQLCipher au lieu d'appeler le VPS. Phase C de la migration local-first.
 *
 * Garde la même signature et les mêmes shapes (`SleepSessionResponse` /
 * `SleepStageResponse`) que `SleepRepositoryImpl`, donc ViewModels et UI sont
 * inchangés. Le parent du chargement saisit `from`/`to` comme avant.
 */
class LocalSleepRepository(
    private val sleepDao: SleepDao,
) : SleepRepository {

    override suspend fun getSessions(
        from: LocalDate?,
        to: LocalDate?,
    ): Result<List<SleepSessionResponse>> = runCatching {
        val sessions = if (from != null && to != null) {
            // [from 00:00 UTC, to+1 00:00 UTC) — `to` est inclusif côté API serveur,
            // on applique le même offset que server/routers/sleep.py:175 (`to + 1d`).
            val fromMs = from.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val toMs = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            sleepDao.getSessionsInRange(fromMs, toMs)
        } else {
            sleepDao.getAllSessions()
        }
        Timber.i("local_sleep_sessions count=${sessions.size} from=$from to=$to")
        if (sessions.isEmpty()) {
            return@runCatching emptyList<SleepSessionResponse>()
        }
        // Charge tous les stages des sessions retournées en 1 requête
        val ids = sessions.map { it.id }
        val allStages = sleepDao.getStagesForSessions(ids).groupBy { it.sessionId }
        sessions.map { session ->
            session.toResponse(stages = allStages[session.id].orEmpty())
        }
    }
}

internal fun SleepSessionEntity.toResponse(stages: List<SleepStageEntity>): SleepSessionResponse =
    SleepSessionResponse(
        id = this.id.toString(),
        sleep_start = msToIso(this.sleepStartMs),
        sleep_end = msToIso(this.sleepEndMs),
        created_at = msToIso(this.createdAtMs),
        stages = stages.map { it.toResponse() },
    )

internal fun SleepStageEntity.toResponse(): SleepStageResponse =
    SleepStageResponse(
        id = this.id.toString(),
        session_id = this.sessionId.toString(),
        stage = this.stageType,
        stage_start = msToIso(this.stageStartMs),
        stage_end = msToIso(this.stageEndMs),
    )

private val ISO_FMT: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

private fun msToIso(ms: Long): String =
    Instant.ofEpochMilli(ms).atOffset(ZoneOffset.UTC).format(ISO_FMT)
