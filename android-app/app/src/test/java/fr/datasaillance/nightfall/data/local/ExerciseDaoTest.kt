package fr.datasaillance.nightfall.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.dao.ExerciseDao
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.ExerciseSessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals

@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var db: NightfallDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_query_in_range() = runTest {
        dao.insertSessions(listOf(
            ExerciseSessionEntity(startTimeMs = 1_000L, endTimeMs = 2_000L, exerciseType = "running"),
            ExerciseSessionEntity(startTimeMs = 5_000L, endTimeMs = 6_000L, exerciseType = "cycling"),
            ExerciseSessionEntity(startTimeMs = 10_000L, endTimeMs = 11_000L, exerciseType = "walking"),
        ))
        assertEquals(3, dao.count())
        val mid = dao.getInRange(fromMs = 4_000L, toMs = 9_000L)
        assertEquals(1, mid.size)
        assertEquals("cycling", mid.first().exerciseType)
    }

    @Test
    fun duplicate_start_end_ignored() = runTest {
        val s = ExerciseSessionEntity(startTimeMs = 1_000L, endTimeMs = 2_000L, exerciseType = "running")
        dao.insertSessions(listOf(s))
        dao.insertSessions(listOf(s.copy(exerciseType = "cycling")))
        assertEquals(1, dao.count())
        assertEquals("running", dao.getAll().first().exerciseType)
    }
}
