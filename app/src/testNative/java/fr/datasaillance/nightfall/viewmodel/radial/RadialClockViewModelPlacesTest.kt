package fr.datasaillance.nightfall.viewmodel.radial

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.location.LabeledPlaceEntity
import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity
import fr.datasaillance.nightfall.data.local.entity.location.PlaceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * TA-9 — le RadialClockViewModel enrichit RadialVisit via PlaceResolver :
 * une visite dans le rayon d'un lieu labellisé est `anchored`, l'autre non.
 *
 * Vit dans src/testNative car RadialVisit est défini dans le flavor `native`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class RadialClockViewModelPlacesTest {

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
    fun buildDaysMap_marks_visit_inside_radius_as_anchored() = runTest {
        // DOMICILE à Paris, rayon 150 m.
        db.labeledPlaceDao().insert(
            LabeledPlaceEntity(
                label = "Maison",
                category = PlaceCategory.DOMICILE,
                lat = 48.8566,
                lng = 2.3522,
                radiusMeters = 150,
            ),
        )

        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        // Visite 1 — dans le rayon (~18 m). Visite 2 — hors rayon (Marseille).
        db.locationDao().insertVisits(
            listOf(
                LocationVisitEntity(
                    startMs = dayStart + 3_600_000L,
                    endMs = dayStart + 7_200_000L,
                    lat = 48.8567,
                    lng = 2.3524,
                    placeName = "chez moi",
                ),
                LocationVisitEntity(
                    startMs = dayStart + 8_000_000L,
                    endMs = dayStart + 9_000_000L,
                    lat = 43.2965,
                    lng = 5.3698,
                    placeName = "ailleurs",
                ),
            ),
        )

        val vm = RadialClockViewModel(
            sleepDao = db.sleepDao(),
            locationDao = db.locationDao(),
            usageStatsDao = db.usageStatsDao(),
            labeledPlaceDao = db.labeledPlaceDao(),
            windowDays = 30,
            zone = zone,
            clock = { today },
        )
        advanceUntilIdle()

        val day = vm.uiState.value.days[today]
        assertNotNull("le jour courant doit être présent", day)
        val visits = day!!.visits.sortedBy { it.startMs }
        assertEquals(2, visits.size)

        val anchored = visits[0]
        assertTrue("visite dans le rayon → anchored", anchored.anchored)
        assertEquals("Maison", anchored.placeLabel)

        val notAnchored = visits[1]
        assertFalse("visite hors rayon → non anchored", notAnchored.anchored)
        assertNull(notAnchored.placeLabel)
    }

    @Test
    fun buildDaysMap_no_labeled_places_yields_all_unanchored() = runTest {
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        db.locationDao().insertVisits(
            listOf(
                LocationVisitEntity(
                    startMs = dayStart + 3_600_000L,
                    endMs = dayStart + 7_200_000L,
                    lat = 48.8567,
                    lng = 2.3524,
                    placeName = "chez moi",
                ),
            ),
        )

        val vm = RadialClockViewModel(
            sleepDao = db.sleepDao(),
            locationDao = db.locationDao(),
            usageStatsDao = db.usageStatsDao(),
            labeledPlaceDao = db.labeledPlaceDao(),
            windowDays = 30,
            zone = zone,
            clock = { today },
        )
        advanceUntilIdle()

        val day = vm.uiState.value.days[today]
        assertNotNull(day)
        assertTrue(day!!.visits.isNotEmpty())
        assertTrue("0 lieu configuré → tout non anchored", day.visits.none { it.anchored })
    }
}
