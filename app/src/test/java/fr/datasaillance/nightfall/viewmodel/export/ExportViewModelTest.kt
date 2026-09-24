package fr.datasaillance.nightfall.viewmodel.export

import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.core.model.RecordingMethod
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.HourlySteps
import fr.datasaillance.nightfall.data.healthconnect.HealthData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipInputStream

// Export ZIP depuis l'app (spec 2026-09-23-phase1-data-foundation, DT-7, TA-14).
@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(scheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val night = SleepRecord(
        id = "s1", start = Instant.parse("2024-07-08T22:31:00Z"), end = Instant.parse("2024-07-09T06:45:30Z"),
        startOffset = null, endOffset = null, source = "com.sec.android.app.shealth",
        recordingMethod = RecordingMethod.AUTOMATICALLY_RECORDED, lastModified = Instant.parse("2024-07-09T07:00:00Z"), stages = emptyList(),
    )

    private val steps = HourlySteps(
        start = Instant.parse("2024-07-09T07:00:00Z"), end = Instant.parse("2024-07-09T08:00:00Z"),
        offset = null, count = 10, sources = listOf("com.sec.android.app.shealth"),
    )

    private fun viewModel(loadData: suspend () -> HealthData?) = ExportViewModel(
        loadData = loadData,
        appVersion = "4.0.0",
        clock = { Instant.parse("2026-09-24T10:00:00Z") },
        ioDispatcher = testDispatcher,
    )

    private fun entryNames(bytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                names.add(entry.name)
            }
        }
        return names
    }

    @Test
    fun `export reussi, l'archive est ecrite dans le fichier choisi et le resume est affiche`() = runTest(scheduler) {
        val output = ByteArrayOutputStream()
        val vm = viewModel { HealthData(listOf(night), listOf(steps), HistoryAccess.FULL) }

        vm.export { output }
        scheduler.advanceUntilIdle()

        assertEquals(
            ExportUiState.Done(sleepSessionsCount = 1, hourlyStepsCount = 1, stepsError = null, historyAccess = HistoryAccess.FULL),
            vm.uiState.value,
        )
        assertEquals(listOf("sleep_sessions.csv", "sleep_stages.csv", "steps.csv", "manifest.json"), entryNames(output.toByteArray()))
    }

    @Test
    fun `Health Connect pas pret, rien n'est ecrit et l'ecran le dit`() = runTest(scheduler) {
        var opened = false
        val vm = viewModel { null }

        vm.export { opened = true; ByteArrayOutputStream() }
        scheduler.advanceUntilIdle()

        assertEquals(ExportUiState.NotReady, vm.uiState.value)
        assertTrue("le fichier ne doit pas être ouvert", !opened)
    }

    @Test
    fun `fichier impossible a ouvrir, etat d'erreur`() = runTest(scheduler) {
        val vm = viewModel { HealthData(listOf(night), listOf(steps), HistoryAccess.FULL) }

        vm.export { null }
        scheduler.advanceUntilIdle()

        assertTrue("${vm.uiState.value}", vm.uiState.value is ExportUiState.Error)
    }

    @Test
    fun `erreur de lecture Health Connect, etat d'erreur au lieu de planter`() = runTest(scheduler) {
        val vm = viewModel { error("lecture impossible") }

        vm.export { ByteArrayOutputStream() }
        scheduler.advanceUntilIdle()

        assertTrue("${vm.uiState.value}", vm.uiState.value is ExportUiState.Error)
    }
}
