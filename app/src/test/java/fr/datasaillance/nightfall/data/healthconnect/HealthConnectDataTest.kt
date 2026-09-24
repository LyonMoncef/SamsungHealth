package fr.datasaillance.nightfall.data.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.core.model.RecordingMethod
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.SleepStage
import fr.datasaillance.nightfall.core.model.StageType
import fr.datasaillance.nightfall.core.model.StepsInterval
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

// Tests d'acceptation TA-7 à TA-12 de la spec 2026-09-23-phase1-data-foundation.
// JVM pur : on construit de vrais objets Health Connect et on remplace Health Connect par une fausse source.
class HealthConnectDataTest {

    private fun t(text: String): Instant = Instant.parse(text)

    private val plus2 = ZoneOffset.ofHours(2)

    // Health Connect interdit de fabriquer l'origine et la date de modification d'une donnée
    // (constructeur de Metadata interne) : les fabriques publiques "...WithId" fixent seulement
    // l'identifiant et la méthode, l'origine reste vide et la date vaut Instant.EPOCH.
    // L'origine et la date réelles sont vérifiées sur téléphone (TA-13).

    private fun sleep(id: String, start: String, end: String, lastModified: String = "2024-07-09T07:00:00Z") = SleepRecord(
        id = id, start = t(start), end = t(end), startOffset = plus2, endOffset = plus2,
        source = "com.sec.android.app.shealth", recordingMethod = RecordingMethod.AUTOMATICALLY_RECORDED,
        lastModified = t(lastModified), stages = emptyList(),
    )

    private fun steps(id: String, start: String, end: String, count: Long) = StepsInterval(
        id = id, start = t(start), end = t(end), startOffset = plus2, endOffset = plus2, count = count,
        source = "com.sec.android.app.shealth", recordingMethod = RecordingMethod.AUTOMATICALLY_RECORDED,
        lastModified = t("2024-07-09T08:00:00Z"),
    )

    /** Fausse source : renvoie des pages préparées à l'avance, dans l'ordre. */
    private class FakeSource(
        private val sleepPages: List<RecordPage<SleepRecord>> = listOf(RecordPage(emptyList(), null)),
        private val stepsPages: List<RecordPage<StepsInterval>> = listOf(RecordPage(emptyList(), null)),
    ) : HealthRecordsSource {
        private var sleepIndex = 0
        private var stepsIndex = 0

        override suspend fun sleepPage(pageToken: String?): RecordPage<SleepRecord> = sleepPages[sleepIndex++]

        override suspend fun stepsPage(pageToken: String?): RecordPage<StepsInterval> = stepsPages[stepsIndex++]
    }

    /** Source qui échoue si on la touche : pour vérifier qu'aucune lecture n'est tentée. */
    private object ForbiddenSource : HealthRecordsSource {
        override suspend fun sleepPage(pageToken: String?): RecordPage<SleepRecord> = fail("lecture interdite") as Nothing
        override suspend fun stepsPage(pageToken: String?): RecordPage<StepsInterval> = fail("lecture interdite") as Nothing
    }

    // ---------------------------------------------------------------- TA-7 conversion sommeil
    @Test
    fun `TA-7 chaque champ d'une session Health Connect est reporte dans le contrat`() {
        val hc = SleepSessionRecord(
            t("2024-07-08T22:31:00Z"), plus2, t("2024-07-09T06:45:30Z"), ZoneOffset.UTC,
            Metadata.manualEntryWithId("hc-1"),
            "titre ignoré", "notes ignorées",
            listOf(
                SleepSessionRecord.Stage(t("2024-07-08T22:31:00Z"), t("2024-07-08T23:00:00Z"), SleepSessionRecord.STAGE_TYPE_LIGHT),
                SleepSessionRecord.Stage(t("2024-07-08T23:00:00Z"), t("2024-07-09T00:00:00Z"), SleepSessionRecord.STAGE_TYPE_DEEP),
            ),
        )

        val expected = SleepRecord(
            id = "hc-1",
            start = t("2024-07-08T22:31:00Z"),
            end = t("2024-07-09T06:45:30Z"),
            startOffset = plus2,
            endOffset = ZoneOffset.UTC,
            source = "",
            recordingMethod = RecordingMethod.MANUAL_ENTRY,
            lastModified = Instant.EPOCH,
            stages = listOf(
                SleepStage(t("2024-07-08T22:31:00Z"), t("2024-07-08T23:00:00Z"), StageType.LIGHT),
                SleepStage(t("2024-07-08T23:00:00Z"), t("2024-07-09T00:00:00Z"), StageType.DEEP),
            ),
        )
        assertEquals(expected, hc.toSleepRecord())
    }

    @Test
    fun `TA-7 les 8 codes de stade correspondent 1 a 1 et un code inconnu donne UNKNOWN`() {
        val expected = mapOf(
            SleepSessionRecord.STAGE_TYPE_UNKNOWN to StageType.UNKNOWN,
            SleepSessionRecord.STAGE_TYPE_AWAKE to StageType.AWAKE,
            SleepSessionRecord.STAGE_TYPE_SLEEPING to StageType.SLEEPING,
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED to StageType.OUT_OF_BED,
            SleepSessionRecord.STAGE_TYPE_LIGHT to StageType.LIGHT,
            SleepSessionRecord.STAGE_TYPE_DEEP to StageType.DEEP,
            SleepSessionRecord.STAGE_TYPE_REM to StageType.REM,
            SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED to StageType.AWAKE_IN_BED,
        )
        for ((code, type) in expected) {
            assertEquals("code $code", type, stageTypeFrom(code))
        }
        assertEquals(StageType.UNKNOWN, stageTypeFrom(42))
    }

    @Test
    fun `TA-7 les 4 methodes d'enregistrement correspondent 1 a 1 et une valeur inconnue donne UNKNOWN`() {
        assertEquals(RecordingMethod.UNKNOWN, recordingMethodFrom(Metadata.RECORDING_METHOD_UNKNOWN))
        assertEquals(RecordingMethod.ACTIVELY_RECORDED, recordingMethodFrom(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED))
        assertEquals(RecordingMethod.AUTOMATICALLY_RECORDED, recordingMethodFrom(Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED))
        assertEquals(RecordingMethod.MANUAL_ENTRY, recordingMethodFrom(Metadata.RECORDING_METHOD_MANUAL_ENTRY))
        assertEquals(RecordingMethod.UNKNOWN, recordingMethodFrom(99))
    }

    // ---------------------------------------------------------------- TA-8 conversion pas
    @Test
    fun `TA-8 chaque champ d'un intervalle de pas Health Connect est reporte dans le contrat`() {
        val hc = StepsRecord(
            t("2024-07-09T07:00:00Z"), plus2, t("2024-07-09T07:15:00Z"), null, 1234,
            Metadata.manualEntryWithId("p-1"),
        )

        val expected = StepsInterval(
            id = "p-1",
            start = t("2024-07-09T07:00:00Z"),
            end = t("2024-07-09T07:15:00Z"),
            startOffset = plus2,
            endOffset = null,
            count = 1234,
            source = "",
            recordingMethod = RecordingMethod.MANUAL_ENTRY,
            lastModified = Instant.EPOCH,
        )
        assertEquals(expected, hc.toStepsInterval())
    }

    // ---------------------------------------------------------------- TA-9 pagination
    @Test
    fun `TA-9 trois pages sont toutes lues, dans l'ordre, jusqu'au token vide`() = runTest {
        val pages = listOf(
            RecordPage(listOf("a", "b"), "t1"),
            RecordPage(listOf("c"), "t2"),
            RecordPage(listOf("d"), null),
        )
        val tokensSeen = mutableListOf<String?>()
        var index = 0

        val all = readAllPages { token ->
            tokensSeen.add(token)
            pages[index++]
        }

        assertEquals(listOf("a", "b", "c", "d"), all)
        assertEquals(listOf(null, "t1", "t2"), tokensSeen)
    }

    @Test
    fun `TA-9 un token vide (chaine vide) termine aussi la lecture`() = runTest {
        val pages = listOf(RecordPage(listOf("a"), "t1"), RecordPage(listOf("b"), ""))
        var index = 0

        val all = readAllPages { pages[index++] }

        assertEquals(listOf("a", "b"), all)
    }

    @Test
    fun `TA-9 un token deja vu declenche une erreur au lieu d'une boucle infinie`() = runTest {
        val pages = listOf(RecordPage(listOf("a"), "t1"), RecordPage(listOf("b"), "t1"))
        var index = 0

        try {
            readAllPages { pages[index++] }
            fail("un token répété aurait dû lever une erreur")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message ?: "", (expected.message ?: "").contains("t1"))
        }
    }

    // ---------------------------------------------------------------- TA-10 dédup par identifiant
    @Test
    fun `TA-10 un meme identifiant lu sur deux pages n'est garde qu'une fois, dans sa version la plus recente`() = runTest {
        val source = FakeSource(
            sleepPages = listOf(
                RecordPage(listOf(sleep("s2", "2024-07-10T22:00:00Z", "2024-07-11T06:00:00Z"), sleep("s1", "2024-07-08T22:00:00Z", "2024-07-09T06:00:00Z", lastModified = "2024-07-09T07:00:00Z")), "t1"),
                RecordPage(listOf(sleep("s1", "2024-07-08T22:00:00Z", "2024-07-09T06:00:00Z", lastModified = "2024-07-09T09:00:00Z")), null),
            ),
            stepsPages = listOf(
                RecordPage(listOf(steps("p1", "2024-07-09T07:00:00Z", "2024-07-09T07:15:00Z", 10)), "t1"),
                RecordPage(listOf(steps("p1", "2024-07-09T07:00:00Z", "2024-07-09T07:15:00Z", 10)), null),
            ),
        )

        val data = loadHealthData(source, HistoryAccess.FULL)

        assertEquals(listOf("s1", "s2"), data.sleep.map { it.id })
        assertEquals(t("2024-07-09T09:00:00Z"), data.sleep.first { it.id == "s1" }.lastModified)
        assertEquals(listOf("p1"), data.steps.map { it.id })
        assertEquals(HistoryAccess.FULL, data.historyAccess)
    }

    // ---------------------------------------------------------------- diagnostic des erreurs de lecture
    private class FailingSource(private val failOnSleep: Boolean, private val error: Exception) : HealthRecordsSource {
        override suspend fun sleepPage(pageToken: String?): RecordPage<SleepRecord> =
            if (failOnSleep) throw error else RecordPage(emptyList(), null)
        override suspend fun stepsPage(pageToken: String?): RecordPage<StepsInterval> = throw error
    }

    @Test
    fun `une erreur pendant la lecture des pas indique l'etape et garde le message d'origine`() = runTest {
        try {
            loadHealthData(FailingSource(failOnSleep = false, IllegalArgumentException("startTime must be before endTime.")), HistoryAccess.FULL)
            fail("une erreur de lecture aurait dû remonter")
        } catch (error: HealthReadException) {
            assertEquals("lecture des pas", error.step)
            assertTrue(error.message ?: "", (error.message ?: "").contains("IllegalArgumentException"))
            assertTrue(error.message ?: "", (error.message ?: "").contains("startTime must be before endTime."))
            assertTrue(error.cause is IllegalArgumentException)
        }
    }

    @Test
    fun `une erreur pendant la lecture du sommeil indique cette etape`() = runTest {
        try {
            loadHealthData(FailingSource(failOnSleep = true, IllegalStateException("boom")), HistoryAccess.FULL)
            fail("une erreur de lecture aurait dû remonter")
        } catch (error: HealthReadException) {
            assertEquals("lecture du sommeil", error.step)
        }
    }

    // ---------------------------------------------------------------- TA-11 historique limité jamais silencieux
    private val allPermissions = setOf(
        "android.permission.health.READ_SLEEP",
        "android.permission.health.READ_STEPS",
        "android.permission.health.READ_HEALTH_DATA_HISTORY",
    )

    @Test
    fun `TA-11 fonctionnalite historique disponible et permission accordee donne un historique complet`() {
        val state = decideHealthConnectState(HealthConnectClient.SDK_AVAILABLE, allPermissions, historyFeatureAvailable = true)

        assertEquals(HealthConnectState.Ready(HistoryAccess.FULL), state)
    }

    @Test
    fun `TA-11 fonctionnalite historique absente donne un historique limite a 30 jours`() {
        val state = decideHealthConnectState(HealthConnectClient.SDK_AVAILABLE, allPermissions, historyFeatureAvailable = false)

        assertEquals(HealthConnectState.Ready(HistoryAccess.LIMITED_30_DAYS), state)
    }

    @Test
    fun `TA-11 permission historique refusee donne un historique limite a 30 jours`() {
        val withoutHistory = allPermissions - "android.permission.health.READ_HEALTH_DATA_HISTORY"

        val state = decideHealthConnectState(HealthConnectClient.SDK_AVAILABLE, withoutHistory, historyFeatureAvailable = true)

        assertEquals(HealthConnectState.Ready(HistoryAccess.LIMITED_30_DAYS), state)
    }

    // ---------------------------------------------------------------- TA-12 permissions manquantes
    @Test
    fun `TA-12 permission sommeil ou pas manquante donne un etat qui demande l'autorisation`() {
        val withoutSleep = allPermissions - "android.permission.health.READ_SLEEP"
        val withoutSteps = allPermissions - "android.permission.health.READ_STEPS"

        assertEquals(HealthConnectState.PermissionsMissing, decideHealthConnectState(HealthConnectClient.SDK_AVAILABLE, withoutSleep, true))
        assertEquals(HealthConnectState.PermissionsMissing, decideHealthConnectState(HealthConnectClient.SDK_AVAILABLE, withoutSteps, true))
    }

    @Test
    fun `TA-12 Health Connect absent ou a mettre a jour donne un etat explicite`() {
        assertEquals(HealthConnectState.NotInstalled, decideHealthConnectState(HealthConnectClient.SDK_UNAVAILABLE, emptySet(), false))
        assertEquals(HealthConnectState.UpdateRequired, decideHealthConnectState(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED, emptySet(), false))
    }

    @Test
    fun `TA-12 aucune lecture n'est tentee tant que Health Connect n'est pas pret`() = runTest {
        assertNull(loadIfReady(HealthConnectState.PermissionsMissing) { ForbiddenSource })
        assertNull(loadIfReady(HealthConnectState.NotInstalled) { ForbiddenSource })
        assertNull(loadIfReady(HealthConnectState.UpdateRequired) { error("la source ne doit même pas être créée") })
    }

    @Test
    fun `les permissions demandees sont exactement sommeil, pas et historique, en lecture seule`() {
        assertEquals(allPermissions, HealthConnectPermissions.ALL)
    }
}
