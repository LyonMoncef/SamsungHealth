package fr.datasaillance.nightfall.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.dao.HeartRateDao
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.HeartRateHourlyEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class HeartRateDaoTest {

    private lateinit var db: NightfallDatabase
    private lateinit var dao: HeartRateDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.heartRateDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_query_in_range() = runTest {
        dao.insertHourly(listOf(
            HeartRateHourlyEntity(date = "2026-04-20", hour = 8, minBpm = 50, maxBpm = 90, avgBpm = 70, sampleCount = 12),
            HeartRateHourlyEntity(date = "2026-04-21", hour = 9, minBpm = 55, maxBpm = 95, avgBpm = 75, sampleCount = 10),
            HeartRateHourlyEntity(date = "2026-04-22", hour = 10, minBpm = 60, maxBpm = 100, avgBpm = 80, sampleCount = 11),
        ))
        assertEquals(3, dao.count())

        val mid = dao.getInRange("2026-04-21", "2026-04-21")
        assertEquals(1, mid.size)
        assertEquals(75, mid.first().avgBpm)
    }

    @Test
    fun duplicate_date_hour_is_ignored() = runTest {
        val row = HeartRateHourlyEntity(date = "2026-04-20", hour = 8, minBpm = 50, maxBpm = 90, avgBpm = 70, sampleCount = 12)
        dao.insertHourly(listOf(row))
        dao.insertHourly(listOf(row))
        assertEquals(1, dao.count())
    }
}
