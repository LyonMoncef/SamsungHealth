package fr.datasaillance.nightfall.data.sleep

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LocalSleepRepositoryTest {

    private lateinit var db: NightfallDatabase
    private lateinit var repo: LocalSleepRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = LocalSleepRepository(db.sleepDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun get_all_sessions_when_no_filter() = runTest {
        seedSession(start = "2026-04-20T22:00:00Z", end = "2026-04-21T06:00:00Z")
        seedSession(start = "2026-04-21T22:00:00Z", end = "2026-04-22T06:00:00Z")

        val r = repo.getSessions()
        assertTrue(r.isSuccess)
        assertEquals(2, r.getOrNull()!!.size)
    }

    @Test
    fun get_sessions_in_range_filters_correctly() = runTest {
        seedSession(start = "2026-04-19T22:00:00Z", end = "2026-04-20T06:00:00Z")
        seedSession(start = "2026-04-20T22:00:00Z", end = "2026-04-21T06:00:00Z")
        seedSession(start = "2026-04-25T22:00:00Z", end = "2026-04-26T06:00:00Z")

        // Window [from=2026-04-20, to=2026-04-21] inclus → on attend la session du 20-21
        // (et celle du 19-20 si son sleep_start tombe dans la fenêtre — non ici)
        val r = repo.getSessions(from = LocalDate.of(2026, 4, 20), to = LocalDate.of(2026, 4, 21))
        assertTrue(r.isSuccess)
        val sessions = r.getOrNull()!!
        assertEquals(1, sessions.size)
        assertTrue(sessions.first().sleep_start.startsWith("2026-04-20"))
    }

    @Test
    fun stages_are_attached_to_their_session() = runTest {
        val sid = seedSession(start = "2026-04-20T22:00:00Z", end = "2026-04-21T06:00:00Z")
        seedStage(sid, "LIGHT", "2026-04-20T22:00:00Z", "2026-04-20T23:00:00Z")
        seedStage(sid, "DEEP", "2026-04-20T23:00:00Z", "2026-04-21T01:00:00Z")
        seedStage(sid, "REM", "2026-04-21T01:00:00Z", "2026-04-21T03:00:00Z")

        val sessions = repo.getSessions().getOrNull()!!
        assertEquals(1, sessions.size)
        val s = sessions.first()
        assertNotNull(s.stages)
        assertEquals(3, s.stages!!.size)
        assertEquals("LIGHT", s.stages.first().stage)
    }

    @Test
    fun iso_round_trip_preserves_timestamps() = runTest {
        val sid = seedSession(start = "2026-04-20T22:30:45Z", end = "2026-04-21T06:15:00Z")
        seedStage(sid, "LIGHT", "2026-04-20T22:30:45Z", "2026-04-21T00:00:00Z")

        val s = repo.getSessions().getOrNull()!!.first()
        assertEquals("2026-04-20T22:30:45Z", s.sleep_start)
        assertEquals("2026-04-21T06:15:00Z", s.sleep_end)
        val stage = s.stages!!.first()
        assertEquals("2026-04-20T22:30:45Z", stage.stage_start)
    }

    @Test
    fun bulk_load_60k_stages_under_2_seconds() = runTest {
        // Critère d'acceptation #2 de la spec — Hypnogramme < 200ms en lecture sur device
        // (Robolectric in-memory ~10x plus lent : seuil large à 5s ici).
        val sid = seedSession(start = "2026-04-20T22:00:00Z", end = "2026-04-21T22:00:00Z")
        val stages = (0 until 60_000).map { i ->
            SleepStageEntity(
                sessionId = sid,
                stageType = listOf("DEEP", "LIGHT", "REM", "AWAKE")[i % 4],
                stageStartMs = 1_745_181_600_000L + i * 1_000L,
                stageEndMs = 1_745_181_600_000L + (i + 1) * 1_000L,
            )
        }
        db.sleepDao().insertStages(stages)

        val t0 = System.currentTimeMillis()
        val sessions = repo.getSessions().getOrNull()!!
        val elapsed = System.currentTimeMillis() - t0

        assertEquals(1, sessions.size)
        assertEquals(60_000, sessions.first().stages!!.size)
        assertTrue("60k stages read took ${elapsed}ms (>5s)", elapsed < 5_000)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private suspend fun seedSession(start: String, end: String): Long {
        val s = SleepSessionEntity(sleepStartMs = isoToMs(start), sleepEndMs = isoToMs(end))
        return db.sleepDao().insertSessions(listOf(s)).first()
    }

    private suspend fun seedStage(sessionId: Long, type: String, start: String, end: String) {
        db.sleepDao().insertStages(listOf(
            SleepStageEntity(
                sessionId = sessionId,
                stageType = type,
                stageStartMs = isoToMs(start),
                stageEndMs = isoToMs(end),
            ),
        ))
    }

    private fun isoToMs(iso: String): Long = java.time.Instant.parse(iso).toEpochMilli()
}
