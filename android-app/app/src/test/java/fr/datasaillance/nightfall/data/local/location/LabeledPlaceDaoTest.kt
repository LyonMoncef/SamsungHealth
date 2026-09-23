package fr.datasaillance.nightfall.data.local.location

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.dao.LabeledPlaceDao
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.location.LabeledPlaceEntity
import fr.datasaillance.nightfall.data.local.entity.location.PlaceCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests DAO in-memory (Robolectric) — TA-6 (CRUD + résolution) et TA-7 (réactivité Flow).
 */
@RunWith(AndroidJUnit4::class)
class LabeledPlaceDaoTest {

    private lateinit var db: NightfallDatabase
    private lateinit var dao: LabeledPlaceDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.labeledPlaceDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // TA-6 — CRUD + réflexion dans la résolution
    @Test
    fun insert_then_getAll_then_resolve_then_delete() = runTest {
        val id = dao.insert(
            LabeledPlaceEntity(
                label = "Maison",
                category = PlaceCategory.DOMICILE,
                lat = 48.8566,
                lng = 2.3522,
                radiusMeters = 150,
            ),
        )
        assertTrue(id > 0L)
        assertEquals(1, dao.count())

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(PlaceCategory.DOMICILE, all.first().category)

        val resolver = PlaceResolver(all.map { it.toLabeledPlace() })
        assertNotNull(resolver.resolve(48.8567, 2.3524))

        dao.delete(all.first())
        assertEquals(0, dao.count())
        val resolverAfter = PlaceResolver(dao.getAll().map { it.toLabeledPlace() })
        assertNull(resolverAfter.resolve(48.8567, 2.3524))
    }

    @Test
    fun update_modifies_existing_row() = runTest {
        val id = dao.insert(
            LabeledPlaceEntity(label = "Bureau", category = PlaceCategory.TRAVAIL, lat = 1.0, lng = 2.0),
        )
        val stored = dao.getAll().first { it.id == id }
        dao.update(stored.copy(label = "Bureau Paris", radiusMeters = 300))
        val updated = dao.getAll().first { it.id == id }
        assertEquals("Bureau Paris", updated.label)
        assertEquals(300, updated.radiusMeters)
    }

    // TA-7 — réactivité getAllFlow
    @Test
    fun getAllFlow_emits_after_insert() = runTest {
        assertTrue(dao.getAllFlow().first().isEmpty())
        dao.insert(
            LabeledPlaceEntity(label = "Famille", category = PlaceCategory.FAMILLE, lat = 3.0, lng = 4.0),
        )
        val emitted = dao.getAllFlow().first()
        assertEquals(1, emitted.size)
        assertEquals("Famille", emitted.first().label)
    }
}
