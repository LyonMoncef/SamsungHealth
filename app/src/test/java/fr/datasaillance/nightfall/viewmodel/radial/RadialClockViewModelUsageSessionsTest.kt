package fr.datasaillance.nightfall.viewmodel.radial

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.usage.UsageSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * Intégration cadran-v2 (DT-9) : le RadialClockViewModel peuple
 * `RadialDay.usageSessions` à partir du `UsageSessionDao`. Sans DAO (param null),
 * la liste reste vide (état dégradé) — rétrocompatible.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RadialClockViewModelUsageSessionsTest {

    private lateinit var db: NightfallDatabase
    private val testDispatcher = UnconfinedTestDispatcher()
    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.parse("2026-05-20")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        )
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun buildDaysMap_populates_usage_sessions_when_dao_present() = runTest {
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        // Session de 10h à 10h30 ce jour.
        db.usageSessionDao().upsert(
            listOf(
                UsageSessionEntity(
                    packageName = "com.example.app",
                    startMs = dayStart + 10 * 3_600_000L,
                    endMs = dayStart + 10 * 3_600_000L + 1_800_000L,
                    durationMs = 1_800_000L,
                    date = today.toString(),
                    collectedAtMs = dayStart,
                ),
            ),
        )

        val vm = RadialClockViewModel(
            sleepRecordsInRange = { _, _ -> emptyList() },
            locationDao = db.locationDao(),
            usageStatsDao = db.usageStatsDao(),
            labeledPlaceDao = db.labeledPlaceDao(),
            usageSessionDao = db.usageSessionDao(),
            windowDays = 30,
            zone = zone,
            clock = { today },
        )
        advanceUntilIdle()

        val day = vm.uiState.value.days[today]
        assertNotNull(day)
        assertEquals(1, day!!.usageSessions.size)
        val s = day.usageSessions.first()
        assertEquals("com.example.app", s.packageName)
        assertEquals(dayStart + 10 * 3_600_000L, s.startMs)
    }

    @Test
    fun buildDaysMap_no_usage_session_dao_yields_empty_degraded() = runTest {
        val vm = RadialClockViewModel(
            sleepRecordsInRange = { _, _ -> emptyList() },
            locationDao = db.locationDao(),
            usageStatsDao = db.usageStatsDao(),
            labeledPlaceDao = db.labeledPlaceDao(),
            usageSessionDao = null,
            windowDays = 30,
            zone = zone,
            clock = { today },
        )
        advanceUntilIdle()

        val day = vm.uiState.value.days[today]
        assertNotNull(day)
        assertTrue("DAO absent → liste vide (état dégradé)", day!!.usageSessions.isEmpty())
    }
}
