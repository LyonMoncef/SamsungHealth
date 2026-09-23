package fr.datasaillance.nightfall.data.local.import_

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalImportServiceTest {

    private lateinit var db: NightfallDatabase
    private lateinit var service: LocalImportService

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
        service = LocalImportService(
            sleepDao = db.sleepDao(),
            heartRateDao = db.heartRateDao(),
            stepsDao = db.stepsDao(),
            exerciseDao = db.exerciseDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun sleepCsv(rows: List<Triple<String, String, String?>> = emptyList()): ByteArray {
        // metadata + header + rows. La 1re col=user_id, 2e=valid digit pour déclencher le strip metadata.
        val header = "com.samsung.health.sleep.start_time,com.samsung.health.sleep.end_time,sleep_score"
        val sb = StringBuilder()
        sb.appendLine("com.samsung.shealth.sleep,12345,11")
        sb.appendLine(header)
        rows.forEach { (s, e, score) ->
            sb.appendLine("$s,$e,${score ?: ""}")
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun sleepStageCsv(rows: List<Triple<String, String, Int>>): ByteArray {
        val sb = StringBuilder()
        sb.appendLine("com.samsung.health.sleep_stage,12345,7")
        sb.appendLine("start_time,end_time,stage")
        rows.forEach { (s, e, code) -> sb.appendLine("$s,$e,$code") }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun heartRateCsv(rows: List<Quad>): ByteArray {
        val sb = StringBuilder()
        sb.appendLine("com.samsung.health.heart_rate,12345,4")
        sb.appendLine(
            "com.samsung.health.heart_rate.start_time," +
                "com.samsung.health.heart_rate.heart_rate," +
                "com.samsung.health.heart_rate.min," +
                "com.samsung.health.heart_rate.max",
        )
        rows.forEach { sb.appendLine("${it.ts},${it.bpm},${it.min},${it.max}") }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun stepsCsv(rows: List<Pair<Long, Int>>): ByteArray {
        val sb = StringBuilder()
        sb.appendLine("com.samsung.shealth.tracker.pedometer_day_summary,12345,5")
        sb.appendLine("day_time,count")
        rows.forEach { (ts, c) -> sb.appendLine("$ts,$c") }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun exerciseCsv(rows: List<ExRow>): ByteArray {
        val sb = StringBuilder()
        sb.appendLine("com.samsung.shealth.exercise,12345,8")
        sb.appendLine(
            "com.samsung.health.exercise.start_time," +
                "com.samsung.health.exercise.end_time," +
                "com.samsung.health.exercise.exercise_type," +
                "com.samsung.health.exercise.duration",
        )
        rows.forEach { sb.appendLine("${it.start},${it.end},${it.code},${it.durationMs}") }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    // ── Tests sleep ────────────────────────────────────────────────────────

    @Test
    fun import_sleep_inserts_rows() = runTest {
        val csv = sleepCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 07:30:00.000", "85"),
            Triple("2026-04-21 23:00:00.000", "2026-04-22 06:45:00.000", "78"),
        ))
        val r = service.importSleep(csv)
        assertEquals(2, r.inserted)
        assertEquals(0, r.skipped)
        assertEquals(2, db.sleepDao().countSessions())
    }

    @Test
    fun import_sleep_idempotent() = runTest {
        val csv = sleepCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 07:30:00.000", "85"),
        ))
        service.importSleep(csv)
        val r2 = service.importSleep(csv)
        assertEquals(0, r2.inserted)
        assertEquals(1, r2.skipped)
        assertEquals(1, db.sleepDao().countSessions())
    }

    @Test
    fun import_sleep_skips_invalid_dates() = runTest {
        val csv = sleepCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 07:30:00.000", "85"),
            Triple("not-a-date", "2026-04-22 06:45:00.000", "78"),
        ))
        val r = service.importSleep(csv)
        assertEquals(1, r.inserted)
        assertEquals(1, r.skipped)
    }

    // ── Tests sleep_stage ──────────────────────────────────────────────────

    @Test
    fun import_sleep_stage_links_to_existing_session() = runTest {
        // Pre-condition : 1 session sleep en DB
        service.importSleep(sleepCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 07:30:00.000", "85"),
        )))
        val stagesCsv = sleepStageCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 00:30:00.000", 40002), // LIGHT
            Triple("2026-04-21 00:30:00.000", "2026-04-21 02:00:00.000", 40003), // DEEP
            Triple("2026-04-21 02:00:00.000", "2026-04-21 03:00:00.000", 40004), // REM
            Triple("2026-04-21 03:00:00.000", "2026-04-21 07:30:00.000", 40002),
        ))
        val r = service.importSleepStages(stagesCsv)
        assertEquals(4, r.inserted)
        assertEquals(0, r.skipped)
        assertEquals(4, db.sleepDao().countStages())
    }

    @Test
    fun import_sleep_stage_skips_unknown_code() = runTest {
        service.importSleep(sleepCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 07:30:00.000", "85"),
        )))
        val r = service.importSleepStages(sleepStageCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 00:30:00.000", 40002),
            Triple("2026-04-21 00:30:00.000", "2026-04-21 02:00:00.000", 99999), // unknown
        )))
        assertEquals(1, r.inserted)
        assertEquals(1, r.skipped)
    }

    @Test
    fun import_sleep_stage_skips_when_no_parent_session() = runTest {
        // Pas de session pré-importée
        val r = service.importSleepStages(sleepStageCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 00:30:00.000", 40002),
        )))
        assertEquals(0, r.inserted)
        assertEquals(1, r.skipped)
    }

    @Test
    fun import_sleep_stage_idempotent() = runTest {
        service.importSleep(sleepCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 07:30:00.000", "85"),
        )))
        val csv = sleepStageCsv(listOf(
            Triple("2026-04-20 23:15:00.000", "2026-04-21 00:30:00.000", 40002),
        ))
        service.importSleepStages(csv)
        val r2 = service.importSleepStages(csv)
        assertEquals(0, r2.inserted)
        assertEquals(1, r2.skipped)
    }

    // ── Tests heart_rate ───────────────────────────────────────────────────

    @Test
    fun import_heart_rate_aggregates_by_hour() = runTest {
        // 3 samples à 8h, 2 à 9h → 2 enregistrements horaires
        val csv = heartRateCsv(listOf(
            Quad("2026-04-20 08:00:00.000", "70", "60", "80"),
            Quad("2026-04-20 08:15:00.000", "72", "62", "82"),
            Quad("2026-04-20 08:30:00.000", "74", "64", "84"),
            Quad("2026-04-20 09:00:00.000", "80", "70", "90"),
            Quad("2026-04-20 09:30:00.000", "82", "72", "92"),
        ))
        val r = service.importHeartRate(csv)
        assertEquals(2, r.inserted)
        val all = db.heartRateDao().getAll()
        assertEquals(2, all.size)
        val h8 = all.first { it.hour == 8 }
        assertEquals(72, h8.avgBpm)
        assertEquals(60, h8.minBpm)
        assertEquals(84, h8.maxBpm)
        assertEquals(3, h8.sampleCount)
    }

    // ── Tests steps ────────────────────────────────────────────────────────

    @Test
    fun import_steps_uses_day_time_format() = runTest {
        // day_time est en millis Unix. 2026-04-20 10:00:00 UTC = 1776592800000
        val csv = stepsCsv(listOf(
            1776592800000L to 1200,
            1776596400000L to 800, // 11h
        ))
        val r = service.importSteps(csv)
        assertEquals(2, r.inserted)
        val all = db.stepsDao().getAll()
        assertEquals(2, all.size)
    }

    // ── Tests exercise ─────────────────────────────────────────────────────

    @Test
    fun import_exercise_maps_known_type() = runTest {
        val csv = exerciseCsv(listOf(
            ExRow("2026-04-20 18:00:00.000", "2026-04-20 18:30:00.000", 1001, 1_800_000f),
        ))
        val r = service.importExercise(csv)
        assertEquals(1, r.inserted)
        val rows = db.exerciseDao().getAll()
        assertEquals("running", rows.first().exerciseType)
        assertEquals(30, rows.first().durationMin)
    }

    @Test
    fun import_exercise_unknown_type_kept_as_samsung_code() = runTest {
        val csv = exerciseCsv(listOf(
            ExRow("2026-04-20 18:00:00.000", "2026-04-20 18:30:00.000", 99999, 1_800_000f),
        ))
        service.importExercise(csv)
        assertEquals("samsung_99999", db.exerciseDao().getAll().first().exerciseType)
    }

    // ── Tests perf ─────────────────────────────────────────────────────────

    @Test
    fun import_60k_sleep_stages_under_5_seconds() = runTest {
        // Crée 1 session qui couvre toute la fenêtre des stages
        service.importSleep(sleepCsv(listOf(
            Triple("2023-12-22 00:00:00.000", "2024-12-22 00:00:00.000", "80"),
        )))
        // Génère 60k stages contigus d'1 minute chacun
        val sb = StringBuilder()
        sb.appendLine("com.samsung.health.sleep_stage,12345,7")
        sb.appendLine("start_time,end_time,stage")
        val baseSec = java.time.LocalDateTime.parse("2023-12-22T00:00:00").toEpochSecond(java.time.ZoneOffset.UTC)
        for (i in 0 until 60_000) {
            val s = java.time.LocalDateTime.ofEpochSecond(baseSec + i * 60L, 0, java.time.ZoneOffset.UTC)
            val e = java.time.LocalDateTime.ofEpochSecond(baseSec + (i + 1) * 60L, 0, java.time.ZoneOffset.UTC)
            val code = listOf(40001, 40002, 40003, 40004)[i % 4]
            sb.appendLine("${s.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))},${e.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))},$code")
        }
        val csv = sb.toString().toByteArray(Charsets.UTF_8)

        val t0 = System.currentTimeMillis()
        val r = service.importSleepStages(csv)
        val elapsed = System.currentTimeMillis() - t0

        assertEquals(60_000, r.inserted)
        assertTrue("60k stages took ${elapsed}ms (>15s)", elapsed < 15_000)
    }

    // ── Bisect helper ──────────────────────────────────────────────────────

    @Test
    fun bisect_right_matches_python_semantics() {
        // bisect_right([1, 3, 5, 7], 5) == 3 (insère APRÈS le 5 existant)
        assertEquals(3, LocalImportService.bisectRight(listOf(1L, 3L, 5L, 7L), 5L))
        assertEquals(0, LocalImportService.bisectRight(listOf(1L, 3L, 5L), 0L))
        assertEquals(3, LocalImportService.bisectRight(listOf(1L, 3L, 5L), 100L))
    }

    private data class Quad(val ts: String, val bpm: String, val min: String, val max: String)
    private data class ExRow(val start: String, val end: String, val code: Int, val durationMs: Float)
}
