package fr.datasaillance.nightfall.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.dao.StepsDao
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.StepsHourlyEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class StepsDaoTest {

    private lateinit var db: NightfallDatabase
    private lateinit var dao: StepsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.stepsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_query() = runTest {
        dao.insertHourly(listOf(
            StepsHourlyEntity(date = "2026-04-20", hour = 10, stepCount = 1200),
            StepsHourlyEntity(date = "2026-04-20", hour = 11, stepCount = 800),
        ))
        assertEquals(2, dao.count())
        val rows = dao.getInRange("2026-04-20", "2026-04-20")
        assertEquals(2, rows.size)
        assertEquals(1200, rows[0].stepCount)
    }

    @Test
    fun duplicate_date_hour_ignored() = runTest {
        dao.insertHourly(listOf(StepsHourlyEntity(date = "2026-04-20", hour = 10, stepCount = 1200)))
        dao.insertHourly(listOf(StepsHourlyEntity(date = "2026-04-20", hour = 10, stepCount = 9999)))
        assertEquals(1, dao.count())
        assertEquals(1200, dao.getAll().first().stepCount)
    }
}
