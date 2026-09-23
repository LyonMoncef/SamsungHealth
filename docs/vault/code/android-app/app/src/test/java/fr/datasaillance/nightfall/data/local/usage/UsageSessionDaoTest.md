---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/usage/UsageSessionDaoTest.kt
git_blob: fb0e75c0e5bff95c59941a16ee88ee1c77e4d8be
last_synced: '2026-05-27T00:40:51Z'
loc: 92
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/usage/UsageSessionDaoTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/usage/UsageSessionDaoTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/usage/UsageSessionDaoTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.usage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.dao.UsageSessionDao
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.usage.UsageSessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests DAO Room in-memory (pattern SleepDaoTest). Couvre TA-6, TA-11.
 */
@RunWith(AndroidJUnit4::class)
class UsageSessionDaoTest {

    private lateinit var db: NightfallDatabase
    private lateinit var dao: UsageSessionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.usageSessionDao()
    }

    @After
    fun tearDown() { db.close() }

    private fun session(pkg: String, start: Long, end: Long, date: String = "2026-01-15") =
        UsageSessionEntity(
            packageName = pkg,
            startMs = start,
            endMs = end,
            durationMs = end - start,
            date = date,
            collectedAtMs = 0L,
        )

    // TA-6 — Idempotence : re-run même (package, start) → pas de doublon, REPLACE
    @Test
    fun ta6_upsert_is_idempotent_via_replace() = runTest {
        dao.upsert(listOf(session("com.app", start = 1000, end = 2000)))
        dao.upsert(listOf(session("com.app", start = 1000, end = 3000)))

        assertEquals(1, dao.count())
        val rows = dao.getByDate("2026-01-15")
        assertEquals(1, rows.size)
        assertEquals(3000L, rows.first().endMs) // valeur du second upsert
    }

    // TA-11 — getSessionsInRange retourne uniquement les sessions dans la fenêtre
    @Test
    fun ta11_get_sessions_in_range() = runTest {
        dao.upsert(listOf(
            session("com.a", start = 1_000, end = 2_000),
            session("com.b", start = 5_000, end = 6_000),
            session("com.c", start = 10_000, end = 11_000),
        ))

        val inRange = dao.getSessionsInRange(fromMs = 4_000, toMs = 9_000)
        assertEquals(1, inRange.size)
        assertEquals(5_000L, inRange.first().startMs)
    }

    @Test
    fun get_by_date_sorted_by_start_ms() = runTest {
        dao.upsert(listOf(
            session("com.b", start = 5_000, end = 6_000),
            session("com.a", start = 1_000, end = 2_000),
        ))
        val rows = dao.getByDate("2026-01-15")
        assertEquals(2, rows.size)
        assertEquals(1_000L, rows[0].startMs)
        assertEquals(5_000L, rows[1].startMs)
    }

    @Test
    fun delete_all_clears_table() = runTest {
        dao.upsert(listOf(session("com.a", start = 1_000, end = 2_000)))
        assertEquals(1, dao.count())
        dao.deleteAll()
        assertEquals(0, dao.count())
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `UsageSessionDaoTest` (class) — lines 19-92
- `setUp` (function) — lines 25-32
- `tearDown` (function) — lines 34-35
- `session` (function) — lines 37-45
- `ta6_upsert_is_idempotent_via_replace` (function) — lines 48-57
- `ta11_get_sessions_in_range` (function) — lines 60-71
- `get_by_date_sorted_by_start_ms` (function) — lines 73-83
- `delete_all_clears_table` (function) — lines 85-91
