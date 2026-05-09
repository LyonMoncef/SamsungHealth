---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/LocalImportService.kt
git_blob: 5f5f9bf679aaa4db47ff40dea3c689615f9cd23d
last_synced: '2026-05-09T15:30:15Z'
loc: 234
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/LocalImportService.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/LocalImportService.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/LocalImportService.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.import_

import fr.datasaillance.nightfall.data.local.dao.ExerciseDao
import fr.datasaillance.nightfall.data.local.dao.HeartRateDao
import fr.datasaillance.nightfall.data.local.dao.SleepDao
import fr.datasaillance.nightfall.data.local.dao.StepsDao
import fr.datasaillance.nightfall.data.local.entity.ExerciseSessionEntity
import fr.datasaillance.nightfall.data.local.entity.HeartRateHourlyEntity
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity
import fr.datasaillance.nightfall.data.local.entity.StepsHourlyEntity
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Résultat d'un import CSV pour un type donné.
 * Équivalent du `(inserted, skipped)` retourné par `parse_*_rows` côté serveur.
 */
data class LocalImportResult(val inserted: Int, val skipped: Int)

/**
 * Importe les CSV Samsung Health directement en base Room locale, sans VPS.
 *
 * Port Kotlin de `server/services/csv_import.py::parse_sleep_rows / parse_sleep_stage_rows /
 * parse_heartrate_rows / parse_steps_rows / parse_exercise_rows`.
 *
 * Les imports sont idempotents par construction : Room utilise `OnConflictStrategy.IGNORE`
 * sur les index uniques `(start, end)` ou `(date, hour)`, équivalent fonctionnel du
 * `ON CONFLICT DO NOTHING` Postgres.
 */
class LocalImportService(
    private val sleepDao: SleepDao,
    private val heartRateDao: HeartRateDao,
    private val stepsDao: StepsDao,
    private val exerciseDao: ExerciseDao,
) {

    /** Import sleep CSV (`com.samsung.shealth.sleep.*.csv`). */
    suspend fun importSleep(csvBytes: ByteArray): LocalImportResult {
        val rows = SamsungCsvParser.parse(csvBytes)
        val entities = mutableListOf<SleepSessionEntity>()
        var skipped = 0
        for (row in rows) {
            val start = SamsungCsvParser.parseTimestampToMs(row["com.samsung.health.sleep.start_time"])
            val end = SamsungCsvParser.parseTimestampToMs(row["com.samsung.health.sleep.end_time"])
            if (start == null || end == null) {
                skipped++
                continue
            }
            entities.add(
                SleepSessionEntity(
                    sleepStartMs = start,
                    sleepEndMs = end,
                    sleepScore = row["sleep_score"]?.toIntOrNullSafe(),
                    efficiency = row["efficiency"]?.toFloatOrNullSafe(),
                    sleepDurationMin = row["sleep_duration"]?.toIntOrNullSafe(),
                    sleepCycle = row["sleep_cycle"]?.toIntOrNullSafe(),
                    mentalRecovery = row["mental_recovery"]?.toFloatOrNullSafe(),
                    physicalRecovery = row["physical_recovery"]?.toFloatOrNullSafe(),
                    sleepType = row["sleep_type"]?.toIntOrNullSafe(),
                ),
            )
        }
        val ids = sleepDao.insertSessions(entities)
        val inserted = ids.count { it != -1L }
        skipped += entities.size - inserted // duplicates ignorés
        return LocalImportResult(inserted, skipped)
    }

    /**
     * Import sleep_stage CSV. Exige que les sessions parentes soient déjà en base
     * (via `importSleep` précédemment) — chaque stage cherche la session qui le contient
     * par lookup binary-search sur les `sleep_start` triés.
     */
    suspend fun importSleepStages(csvBytes: ByteArray): LocalImportResult {
        val rows = SamsungCsvParser.parse(csvBytes)
        val sessions = sleepDao.getAllSessions()
        if (sessions.isEmpty()) {
            // Pas de session parente en DB → tous les stages sont orphelins
            return LocalImportResult(inserted = 0, skipped = rows.size)
        }
        val starts = sessions.map { it.sleepStartMs }

        val toInsert = mutableListOf<SleepStageEntity>()
        var skipped = 0
        for (row in rows) {
            val startMs = SamsungCsvParser.parseTimestampToMs(row["start_time"])
            val endMs = SamsungCsvParser.parseTimestampToMs(row["end_time"])
            val stageCode = row["stage"]?.toIntOrNullSafe()
            if (startMs == null || endMs == null || stageCode == null) {
                skipped++; continue
            }
            val stageType = SLEEP_STAGE_MAP[stageCode]
            if (stageType == null) {
                skipped++; continue
            }
            val sessionIdx = bisectRight(starts, startMs) - 1
            if (sessionIdx < 0) {
                skipped++; continue
            }
            val parent = sessions[sessionIdx]
            if (parent.sleepEndMs < endMs) {
                skipped++; continue
            }
            toInsert.add(
                SleepStageEntity(
                    sessionId = parent.id,
                    stageType = stageType,
                    stageStartMs = startMs,
                    stageEndMs = endMs,
                ),
            )
        }
        val ids = sleepDao.insertStages(toInsert)
        val inserted = ids.count { it != -1L }
        skipped += toInsert.size - inserted
        return LocalImportResult(inserted, skipped)
    }

    /** Import heart_rate CSV avec agrégation horaire (min / max / avg / count par (date, hour) UTC). */
    suspend fun importHeartRate(csvBytes: ByteArray): LocalImportResult {
        val rows = SamsungCsvParser.parse(csvBytes)
        // Slot par (date, hour) → liste de (bpm, min, max)
        val slots = HashMap<Pair<String, Int>, MutableList<Triple<Int, Int, Int>>>()
        for (row in rows) {
            val tsMs = SamsungCsvParser.parseTimestampToMs(row["com.samsung.health.heart_rate.start_time"]) ?: continue
            val bpm = row["com.samsung.health.heart_rate.heart_rate"]?.toFloatOrNullSafe()?.let { Math.round(it) } ?: continue
            val mn = row["com.samsung.health.heart_rate.min"]?.toFloatOrNullSafe()?.let { Math.round(it) } ?: continue
            val mx = row["com.samsung.health.heart_rate.max"]?.toFloatOrNullSafe()?.let { Math.round(it) } ?: continue
            val (date, hour) = msToDateHour(tsMs)
            slots.getOrPut(date to hour) { mutableListOf() }.add(Triple(bpm, mn, mx))
        }
        val entities = slots.map { (key, samples) ->
            val (date, hour) = key
            HeartRateHourlyEntity(
                date = date,
                hour = hour,
                avgBpm = (samples.sumOf { it.first } / samples.size.toDouble()).let { Math.round(it).toInt() },
                minBpm = samples.minOf { it.second },
                maxBpm = samples.maxOf { it.third },
                sampleCount = samples.size,
            )
        }
        val ids = heartRateDao.insertHourly(entities)
        val inserted = ids.count { it != -1L }
        return LocalImportResult(inserted, entities.size - inserted)
    }

    /**
     * Import steps CSV. Format actuel utilise `day_time` (millis) + `count`.
     * Fallback supporté : colonnes `com.samsung.health.step_daily_trend.start_time / .count`.
     */
    suspend fun importSteps(csvBytes: ByteArray): LocalImportResult {
        val rows = SamsungCsvParser.parse(csvBytes)
        val slots = HashMap<Pair<String, Int>, Int>()
        for (row in rows) {
            val dayTime = row["day_time"]?.toLongOrNullSafe()
            val tsMs: Long?
            val count: Int?
            if (dayTime != null) {
                tsMs = dayTime
                count = row["count"]?.toIntOrNullSafe()
            } else {
                tsMs = SamsungCsvParser.parseTimestampToMs(row["com.samsung.health.step_daily_trend.start_time"])
                count = row["com.samsung.health.step_daily_trend.count"]?.toIntOrNullSafe()
            }
            if (tsMs == null || count == null) continue
            val (date, hour) = msToDateHour(tsMs)
            slots[date to hour] = (slots[date to hour] ?: 0) + count
        }
        val entities = slots.map { (key, c) -> StepsHourlyEntity(date = key.first, hour = key.second, stepCount = c) }
        val ids = stepsDao.insertHourly(entities)
        val inserted = ids.count { it != -1L }
        return LocalImportResult(inserted, entities.size - inserted)
    }

    /** Import exercise CSV. */
    suspend fun importExercise(csvBytes: ByteArray): LocalImportResult {
        val rows = SamsungCsvParser.parse(csvBytes)
        val entities = mutableListOf<ExerciseSessionEntity>()
        var skipped = 0
        for (row in rows) {
            val start = SamsungCsvParser.parseTimestampToMs(row["com.samsung.health.exercise.start_time"])
            val end = SamsungCsvParser.parseTimestampToMs(row["com.samsung.health.exercise.end_time"])
            val typeCode = row["com.samsung.health.exercise.exercise_type"]?.toIntOrNullSafe()
            val durationMs = row["com.samsung.health.exercise.duration"]?.toFloatOrNullSafe()
            if (start == null || end == null || typeCode == null) {
                skipped++; continue
            }
            val type = EXERCISE_TYPE_MAP[typeCode] ?: "samsung_$typeCode"
            entities.add(
                ExerciseSessionEntity(
                    startTimeMs = start,
                    endTimeMs = end,
                    exerciseType = type,
                    durationMin = durationMs?.let { (it / 60_000f).toInt() },
                    calorie = row["com.samsung.health.exercise.calorie"]?.toFloatOrNullSafe(),
                    meanHeartRate = row["com.samsung.health.exercise.mean_heart_rate"]?.toFloatOrNullSafe()?.let { Math.round(it) },
                ),
            )
        }
        val ids = exerciseDao.insertSessions(entities)
        val inserted = ids.count { it != -1L }
        skipped += entities.size - inserted
        return LocalImportResult(inserted, skipped)
    }

    private fun msToDateHour(ms: Long): Pair<String, Int> {
        val zdt = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC)
        return zdt.toLocalDate().format(DATE_FMT) to zdt.hour
    }

    companion object {
        private val DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        // bisect_right équivalent Python — retourne l'index où insérer `target` pour
        // garder `list` triée, en plaçant à DROITE des entrées égales.
        internal fun bisectRight(list: List<Long>, target: Long): Int {
            var lo = 0
            var hi = list.size
            while (lo < hi) {
                val mid = (lo + hi) ushr 1
                if (target < list[mid]) hi = mid else lo = mid + 1
            }
            return lo
        }
    }
}

// Helpers de parsing tolérants — String?.toIntOrNull() est plus strict (refuse "12.0", " 12 ", etc.)
private fun String.toIntOrNullSafe(): Int? = trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()?.toInt()
private fun String.toFloatOrNullSafe(): Float? = trim().takeIf { it.isNotEmpty() }?.toFloatOrNull()
private fun String.toLongOrNullSafe(): Long? = trim().takeIf { it.isNotEmpty() }?.toLongOrNull()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LocalImportResult` (class) — lines 20-20
- `LocalImportService` (class) — lines 32-229
- `importSleep` (function) — lines 40-69
- `importSleepStages` (function) — lines 76-119
- `importHeartRate` (function) — lines 122-148
- `importSteps` (function) — lines 154-176
- `importExercise` (function) — lines 179-207
- `msToDateHour` (function) — lines 209-212
- `bisectRight` (function) — lines 219-227
- `toIntOrNullSafe` (function) — lines 232-232
- `toFloatOrNullSafe` (function) — lines 233-233
- `toLongOrNullSafe` (function) — lines 234-234
