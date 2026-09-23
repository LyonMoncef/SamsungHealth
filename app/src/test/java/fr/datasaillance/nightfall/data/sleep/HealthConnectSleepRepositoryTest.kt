package fr.datasaillance.nightfall.data.sleep

import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.core.model.RecordingMethod
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.SleepStage
import fr.datasaillance.nightfall.core.model.StageType
import fr.datasaillance.nightfall.data.healthconnect.HealthData
import fr.datasaillance.nightfall.data.healthconnect.HealthDataCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

// Pont temporaire contrat → anciens écrans (spec 2026-09-23-phase1-data-foundation, DT-6).
class HealthConnectSleepRepositoryTest {

    private fun t(text: String): Instant = Instant.parse(text)

    private fun night(id: String, start: String, end: String, stages: List<SleepStage> = emptyList()) = SleepRecord(
        id = id, start = t(start), end = t(end), startOffset = null, endOffset = null,
        source = "com.sec.android.app.shealth", recordingMethod = RecordingMethod.AUTOMATICALLY_RECORDED,
        lastModified = t("2024-07-09T07:00:00Z"), stages = stages,
    )

    private fun data(vararg sleep: SleepRecord) = HealthData(sleep.toList(), emptyList(), HistoryAccess.FULL)

    @Test
    fun `une session est convertie au format attendu par les anciens ecrans`() {
        val record = night(
            "hc-1", "2024-07-08T22:31:00Z", "2024-07-09T06:45:30Z",
            stages = listOf(
                SleepStage(t("2024-07-08T22:31:00Z"), t("2024-07-08T23:00:00Z"), StageType.LIGHT),
                SleepStage(t("2024-07-08T23:00:00Z"), t("2024-07-09T00:00:00Z"), StageType.DEEP),
            ),
        )

        val expected = SleepSessionResponse(
            id = "hc-1",
            sleep_start = "2024-07-08T22:31:00Z",
            sleep_end = "2024-07-09T06:45:30Z",
            created_at = "2024-07-09T07:00:00Z",
            stages = listOf(
                SleepStageResponse("hc-1-0", "hc-1", "LIGHT", "2024-07-08T22:31:00Z", "2024-07-08T23:00:00Z"),
                SleepStageResponse("hc-1-1", "hc-1", "DEEP", "2024-07-08T23:00:00Z", "2024-07-09T00:00:00Z"),
            ),
        )
        assertEquals(expected, record.toSleepSessionResponse())
    }

    @Test
    fun `les stades sont traduits dans l'ancien vocabulaire, sans rien inventer`() {
        assertEquals("LIGHT", legacyStageName(StageType.LIGHT))
        assertEquals("DEEP", legacyStageName(StageType.DEEP))
        assertEquals("REM", legacyStageName(StageType.REM))
        // Les trois formes d'éveil : l'ancienne UI ne connaît que AWAKE.
        assertEquals("AWAKE", legacyStageName(StageType.AWAKE))
        assertEquals("AWAKE", legacyStageName(StageType.AWAKE_IN_BED))
        assertEquals("AWAKE", legacyStageName(StageType.OUT_OF_BED))
        // Pas d'équivalent : on garde le nom brut (l'UI l'affiche tel quel).
        assertEquals("SLEEPING", legacyStageName(StageType.SLEEPING))
        assertEquals("UNKNOWN", legacyStageName(StageType.UNKNOWN))
    }

    @Test
    fun `le filtrage par dates reprend celui de l'ancien depot (debut de nuit dans la fenetre, to inclus)`() = runTest {
        val cache = HealthDataCache()
        cache.update(
            data(
                night("avant", "2024-07-07T23:59:59Z", "2024-07-08T07:00:00Z"),
                night("dedans-1", "2024-07-08T00:00:00Z", "2024-07-08T07:00:00Z"),
                night("dedans-2", "2024-07-09T23:59:59Z", "2024-07-10T07:00:00Z"),
                night("apres", "2024-07-10T00:00:00Z", "2024-07-10T07:00:00Z"),
            ),
        )
        val repository = HealthConnectSleepRepository(cache) { error("ne doit pas relire Health Connect") }

        val inRange = repository.getSessions(LocalDate.parse("2024-07-08"), LocalDate.parse("2024-07-09")).getOrThrow()
        val all = repository.getSessions().getOrThrow()

        assertEquals(listOf("dedans-1", "dedans-2"), inRange.map { it.id })
        assertEquals(4, all.size)
    }

    @Test
    fun `sans lecture en cache, le depot lit Health Connect une fois et remplit le cache`() = runTest {
        val cache = HealthDataCache()
        var loads = 0
        val repository = HealthConnectSleepRepository(cache) {
            loads++
            data(night("s1", "2024-07-08T22:31:00Z", "2024-07-09T06:45:30Z"))
        }

        val first = repository.getSessions().getOrThrow()
        val second = repository.getSessions().getOrThrow()

        assertEquals(listOf("s1"), first.map { it.id })
        assertEquals(first, second)
        assertEquals(1, loads)
        assertEquals(listOf("s1"), cache.data.value?.sleep?.map { it.id })
    }

    @Test
    fun `le Cadran recoit les sessions du contrat dont le debut tombe dans sa fenetre`() = runTest {
        val cache = HealthDataCache()
        cache.update(
            data(
                night("avant", "2024-07-07T22:00:00Z", "2024-07-08T06:00:00Z"),
                night("dedans", "2024-07-08T22:00:00Z", "2024-07-09T06:00:00Z"),
                night("apres", "2024-07-10T22:00:00Z", "2024-07-11T06:00:00Z"),
            ),
        )
        val repository = HealthConnectSleepRepository(cache) { null }

        val records = repository.recordsStartingBetween(
            fromMs = t("2024-07-08T00:00:00Z").toEpochMilli(),
            toMs = t("2024-07-10T00:00:00Z").toEpochMilli(),
        )

        assertEquals(listOf("dedans"), records.map { it.id })
    }

    @Test
    fun `tant que Health Connect n'est pas pret, le depot renvoie une liste vide`() = runTest {
        val repository = HealthConnectSleepRepository(HealthDataCache()) { null }

        val sessions = repository.getSessions()

        assertTrue(sessions.isSuccess)
        assertEquals(emptyList<SleepSessionResponse>(), sessions.getOrThrow())
    }
}
