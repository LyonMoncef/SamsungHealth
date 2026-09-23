package fr.datasaillance.nightfall.viewmodel.healthconnect

import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.core.model.RecordingMethod
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.StepsInterval
import fr.datasaillance.nightfall.data.healthconnect.HealthConnectState
import fr.datasaillance.nightfall.data.healthconnect.HealthDataCache
import fr.datasaillance.nightfall.data.healthconnect.HealthRecordsSource
import fr.datasaillance.nightfall.data.healthconnect.RecordPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

// Écran Health Connect (spec 2026-09-23-phase1-data-foundation, DT-5, TA-11 à TA-13).
@OptIn(ExperimentalCoroutinesApi::class)
class HealthConnectViewModelTest {

    private val scheduler = TestCoroutineScheduler()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sleep(id: String, start: String) = SleepRecord(
        id = id, start = Instant.parse(start), end = Instant.parse(start).plusSeconds(8 * 3600),
        startOffset = null, endOffset = null, source = "com.sec.android.app.shealth",
        recordingMethod = RecordingMethod.AUTOMATICALLY_RECORDED, lastModified = Instant.parse(start), stages = emptyList(),
    )

    private fun steps(id: String, start: String) = StepsInterval(
        id = id, start = Instant.parse(start), end = Instant.parse(start).plusSeconds(600), startOffset = null, endOffset = null,
        count = 100, source = "com.sec.android.app.shealth", recordingMethod = RecordingMethod.AUTOMATICALLY_RECORDED,
        lastModified = Instant.parse(start),
    )

    /** Source à une seule page ; compte combien de fois on la lit. */
    private class OnePageSource(
        private val sleep: List<SleepRecord>,
        private val steps: List<StepsInterval>,
        private val failure: Exception? = null,
    ) : HealthRecordsSource {
        var reads = 0

        override suspend fun sleepPage(pageToken: String?): RecordPage<SleepRecord> {
            reads++
            failure?.let { throw it }
            return RecordPage(sleep, null)
        }

        override suspend fun stepsPage(pageToken: String?): RecordPage<StepsInterval> = RecordPage(steps, null)
    }

    private fun viewModel(state: HealthConnectState, source: OnePageSource, cache: HealthDataCache = HealthDataCache()) =
        HealthConnectViewModel(checkState = { state }, newSource = { source }, cache = cache)

    @Test
    fun `historique complet lu, nombre de sessions, plus ancienne, pas, et cache partage mis a jour`() = runTest(scheduler) {
        val cache = HealthDataCache()
        val source = OnePageSource(
            sleep = listOf(sleep("s2", "2024-08-01T22:00:00Z"), sleep("s1", "2024-07-08T22:31:00Z")),
            steps = listOf(steps("p1", "2024-07-09T07:00:00Z")),
        )
        val vm = viewModel(HealthConnectState.Ready(HistoryAccess.FULL), source, cache)

        vm.refresh()
        scheduler.advanceUntilIdle()

        val expected = HealthConnectUiState.Loaded(
            sleepSessionsCount = 2,
            oldestSleepStart = Instant.parse("2024-07-08T22:31:00Z"),
            stepsIntervalsCount = 1,
            historyAccess = HistoryAccess.FULL,
        )
        assertEquals(expected, vm.uiState.value)
        assertEquals(listOf("s1", "s2"), cache.data.value?.sleep?.map { it.id })
    }

    @Test
    fun `TA-11 historique limite remonte explicitement a l'ecran`() = runTest(scheduler) {
        val vm = viewModel(HealthConnectState.Ready(HistoryAccess.LIMITED_30_DAYS), OnePageSource(emptyList(), emptyList()))

        vm.refresh()
        scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("$state", state is HealthConnectUiState.Loaded && state.historyAccess == HistoryAccess.LIMITED_30_DAYS)
        assertNull((state as HealthConnectUiState.Loaded).oldestSleepStart)
    }

    @Test
    fun `TA-12 permissions manquantes, ecran d'autorisation et aucune lecture`() = runTest(scheduler) {
        val source = OnePageSource(emptyList(), emptyList())
        val vm = viewModel(HealthConnectState.PermissionsMissing, source)

        vm.refresh()
        scheduler.advanceUntilIdle()

        assertEquals(HealthConnectUiState.PermissionsMissing, vm.uiState.value)
        assertEquals(0, source.reads)
    }

    @Test
    fun `Health Connect absent ou a mettre a jour donne l'etat correspondant, sans lecture`() = runTest(scheduler) {
        val source = OnePageSource(emptyList(), emptyList())
        val absent = viewModel(HealthConnectState.NotInstalled, source)
        val outdated = viewModel(HealthConnectState.UpdateRequired, source)

        absent.refresh()
        outdated.refresh()
        scheduler.advanceUntilIdle()

        assertEquals(HealthConnectUiState.NotInstalled, absent.uiState.value)
        assertEquals(HealthConnectUiState.UpdateRequired, outdated.uiState.value)
        assertEquals(0, source.reads)
    }

    @Test
    fun `une erreur de lecture donne un etat d'erreur au lieu de planter, et le cache reste vide`() = runTest(scheduler) {
        val cache = HealthDataCache()
        val vm = viewModel(HealthConnectState.Ready(HistoryAccess.FULL), OnePageSource(emptyList(), emptyList(), IOException("boom")), cache)

        vm.refresh()
        scheduler.advanceUntilIdle()

        assertTrue("${vm.uiState.value}", vm.uiState.value is HealthConnectUiState.Error)
        assertNull(cache.data.value)
    }

    @Test
    fun `apres la reponse aux permissions, l'etat est reverifie`() = runTest(scheduler) {
        var state: HealthConnectState = HealthConnectState.PermissionsMissing
        val vm = HealthConnectViewModel(
            checkState = { state },
            newSource = { OnePageSource(listOf(sleep("s1", "2024-07-08T22:31:00Z")), emptyList()) },
            cache = HealthDataCache(),
        )
        vm.refresh()
        scheduler.advanceUntilIdle()
        assertEquals(HealthConnectUiState.PermissionsMissing, vm.uiState.value)

        state = HealthConnectState.Ready(HistoryAccess.FULL)
        vm.onPermissionsResult()
        scheduler.advanceUntilIdle()

        val loaded = vm.uiState.value
        assertTrue("$loaded", loaded is HealthConnectUiState.Loaded && loaded.sleepSessionsCount == 1)
    }
}
