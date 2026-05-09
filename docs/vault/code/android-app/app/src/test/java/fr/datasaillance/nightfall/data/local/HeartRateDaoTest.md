---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/HeartRateDaoTest.kt
git_blob: 6475403a0ca0dd6a433854a4a6a22031de41fffc
last_synced: '2026-05-09T15:08:38Z'
loc: 57
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/HeartRateDaoTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/HeartRateDaoTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/HeartRateDaoTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `HeartRateDaoTest` (class) — lines 16-57
- `setUp` (function) — lines 22-29
- `tearDown` (function) — lines 31-34
- `insert_and_query_in_range` (function) — lines 36-48
- `duplicate_date_hour_is_ignored` (function) — lines 50-56
