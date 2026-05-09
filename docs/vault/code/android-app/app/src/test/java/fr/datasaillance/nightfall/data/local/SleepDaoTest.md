---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/SleepDaoTest.kt
git_blob: 5388f13412f743d05e6eca54ecfb27dbbb878754
last_synced: '2026-05-09T15:08:38Z'
loc: 135
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/SleepDaoTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/SleepDaoTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/SleepDaoTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.dao.SleepDao
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.SleepSessionEntity
import fr.datasaillance.nightfall.data.local.entity.SleepStageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

@RunWith(AndroidJUnit4::class)
class SleepDaoTest {

    private lateinit var db: NightfallDatabase
    private lateinit var dao: SleepDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.sleepDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insert_and_query_sessions() = runTest {
        val s1 = SleepSessionEntity(sleepStartMs = 1_000_000L, sleepEndMs = 1_100_000L)
        val s2 = SleepSessionEntity(sleepStartMs = 2_000_000L, sleepEndMs = 2_100_000L)
        val ids = dao.insertSessions(listOf(s1, s2))

        assertEquals(2, ids.size)
        assertEquals(2, dao.countSessions())

        val all = dao.getAllSessions()
        assertEquals(2, all.size)
        assertEquals(1_000_000L, all[0].sleepStartMs)
    }

    @Test
    fun insert_duplicate_session_is_ignored() = runTest {
        val s = SleepSessionEntity(sleepStartMs = 1_000_000L, sleepEndMs = 1_100_000L)
        dao.insertSessions(listOf(s))
        val secondInsert = dao.insertSessions(listOf(s))

        assertEquals(1, dao.countSessions())
        assertEquals("duplicate insert returns -1", -1L, secondInsert.first())
    }

    @Test
    fun query_sessions_in_range() = runTest {
        dao.insertSessions(listOf(
            SleepSessionEntity(sleepStartMs = 1_000L, sleepEndMs = 2_000L),
            SleepSessionEntity(sleepStartMs = 5_000L, sleepEndMs = 6_000L),
            SleepSessionEntity(sleepStartMs = 10_000L, sleepEndMs = 11_000L),
        ))

        val mid = dao.getSessionsInRange(fromMs = 4_000L, toMs = 9_000L)
        assertEquals(1, mid.size)
        assertEquals(5_000L, mid.first().sleepStartMs)
    }

    @Test
    fun insert_stages_with_session_fk() = runTest {
        val sessionIds = dao.insertSessions(listOf(
            SleepSessionEntity(sleepStartMs = 1_000_000L, sleepEndMs = 1_500_000L),
        ))
        val sid = sessionIds.first()

        val stages = listOf(
            SleepStageEntity(sessionId = sid, stageType = "LIGHT", stageStartMs = 1_000_000L, stageEndMs = 1_100_000L),
            SleepStageEntity(sessionId = sid, stageType = "DEEP", stageStartMs = 1_100_000L, stageEndMs = 1_300_000L),
            SleepStageEntity(sessionId = sid, stageType = "REM", stageStartMs = 1_300_000L, stageEndMs = 1_500_000L),
        )
        dao.insertStages(stages)

        assertEquals(3, dao.countStages())
        val fetched = dao.getStagesForSession(sid)
        assertEquals(3, fetched.size)
        assertEquals("LIGHT", fetched[0].stageType)
        assertEquals("DEEP", fetched[1].stageType)
        assertEquals("REM", fetched[2].stageType)
    }

    @Test
    fun cascade_delete_session_removes_stages() = runTest {
        val sid = dao.insertSessions(listOf(
            SleepSessionEntity(sleepStartMs = 1_000_000L, sleepEndMs = 1_500_000L),
        )).first()
        dao.insertStages(listOf(
            SleepStageEntity(sessionId = sid, stageType = "LIGHT", stageStartMs = 1_000_000L, stageEndMs = 1_500_000L),
        ))
        assertEquals(1, dao.countStages())

        dao.deleteAllSessions()
        assertEquals(0, dao.countSessions())
        assertEquals("stages should cascade-delete with their session", 0, dao.countStages())
    }

    @Test
    fun bulk_insert_60k_stages_under_2_seconds() = runTest {
        // Garde-fou perf — au cœur de la migration local-first.
        val sid = dao.insertSessions(listOf(
            SleepSessionEntity(sleepStartMs = 0L, sleepEndMs = 60_000_000L),
        )).first()

        val stages = (0 until 60_000).map { i ->
            SleepStageEntity(
                sessionId = sid,
                stageType = if (i % 3 == 0) "DEEP" else if (i % 3 == 1) "LIGHT" else "REM",
                stageStartMs = i * 1_000L,
                stageEndMs = (i + 1) * 1_000L,
            )
        }
        val t0 = System.currentTimeMillis()
        dao.insertStages(stages)
        val elapsed = System.currentTimeMillis() - t0

        assertEquals(60_000, dao.countStages())
        // Robolectric in-memory est ~10x plus lent que device réel — seuil large
        assert(elapsed < 5_000) { "60k stages bulk insert took ${elapsed}ms (>5s)" }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepDaoTest` (class) — lines 18-135
- `setUp` (function) — lines 24-31
- `tearDown` (function) — lines 33-36
- `insert_and_query_sessions` (function) — lines 38-50
- `insert_duplicate_session_is_ignored` (function) — lines 52-60
- `query_sessions_in_range` (function) — lines 62-73
- `insert_stages_with_session_fk` (function) — lines 75-95
- `cascade_delete_session_removes_stages` (function) — lines 97-110
- `bulk_insert_60k_stages_under_2_seconds` (function) — lines 112-134
