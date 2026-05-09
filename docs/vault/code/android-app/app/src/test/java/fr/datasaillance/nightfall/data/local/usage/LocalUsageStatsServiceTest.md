---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/usage/LocalUsageStatsServiceTest.kt
git_blob: 0ffe5bc972455df0150c569eeeea3e19e9c92ed8
last_synced: '2026-05-09T18:49:36Z'
loc: 133
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/usage/LocalUsageStatsServiceTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/usage/LocalUsageStatsServiceTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/usage/LocalUsageStatsServiceTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.usage

import android.app.usage.UsageStats
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/** Fake source qui renvoie des fixtures sans dépendre de UsageStatsManager Android. */
private class FakeUsageStatsSource(private val rows: List<UsageStats>) : UsageStatsSource {
    override fun queryUsageStats(intervalType: Int, fromMs: Long, toMs: Long): List<UsageStats> = rows
}

@RunWith(AndroidJUnit4::class)
class LocalUsageStatsServiceTest {

    private lateinit var db: NightfallDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() { db.close() }

    private fun stat(pkg: String, fgMs: Long, lastUsed: Long = 0L): UsageStats {
        // UsageStats n'a pas de constructeur public — on instancie via reflection.
        val ctor = UsageStats::class.java.getDeclaredConstructor()
        ctor.isAccessible = true
        val u = ctor.newInstance()
        // Champs accessibles via reflection (non publics)
        val cls = UsageStats::class.java
        cls.getField("mPackageName").set(u, pkg)
        cls.getField("mTotalTimeInForeground").setLong(u, fgMs)
        cls.getField("mLastTimeUsed").setLong(u, lastUsed)
        return u
    }

    @Test
    fun collect_writes_one_row_per_package() = runTest {
        val source = FakeUsageStatsSource(listOf(
            stat("com.youtube", 3_600_000L, lastUsed = 1_700_000_000_000L),
            stat("com.discord", 600_000L),
            stat("com.android.systemui", 0L), // doit être filtré (0ms, 0 launches)
        ))
        val service = LocalUsageStatsService(
            dao = db.usageStatsDao(),
            source = source,
            zone = ZoneId.of("UTC"),
            now = { 1_700_000_000_000L },
        )

        val n = service.collectDailyStats(LocalDate.of(2026, 4, 20))
        assertEquals(2, n)
        val rows = db.usageStatsDao().getByDate("2026-04-20")
        assertEquals(2, rows.size)
        assertEquals("com.youtube", rows[0].packageName) // tri par fg desc
        assertEquals(3_600_000L, rows[0].totalTimeForegroundMs)
    }

    @Test
    fun collect_aggregates_duplicate_packages() = runTest {
        // Android peut renvoyer 2 entrées pour le même pkg (rotation buckets) — on somme
        val source = FakeUsageStatsSource(listOf(
            stat("com.youtube", 1_000_000L),
            stat("com.youtube", 2_500_000L),
        ))
        val service = LocalUsageStatsService(
            dao = db.usageStatsDao(),
            source = source,
            zone = ZoneId.of("UTC"),
        )
        service.collectDailyStats(LocalDate.of(2026, 4, 20))
        val rows = db.usageStatsDao().getByDate("2026-04-20")
        assertEquals(1, rows.size)
        assertEquals(3_500_000L, rows[0].totalTimeForegroundMs)
    }

    @Test
    fun collect_is_idempotent_via_replace() = runTest {
        val source1 = FakeUsageStatsSource(listOf(stat("com.youtube", 1_000_000L)))
        val s1 = LocalUsageStatsService(db.usageStatsDao(), source1, ZoneId.of("UTC"))
        s1.collectDailyStats(LocalDate.of(2026, 4, 20))

        // 2e collecte sur la même date avec valeur affinée par Android — doit écraser
        val source2 = FakeUsageStatsSource(listOf(stat("com.youtube", 2_345_678L)))
        val s2 = LocalUsageStatsService(db.usageStatsDao(), source2, ZoneId.of("UTC"))
        s2.collectDailyStats(LocalDate.of(2026, 4, 20))

        val rows = db.usageStatsDao().getByDate("2026-04-20")
        assertEquals(1, rows.size)
        assertEquals(2_345_678L, rows[0].totalTimeForegroundMs)
        assertEquals(1, db.usageStatsDao().count())
    }

    @Test
    fun query_in_range_returns_sorted() = runTest {
        val s = LocalUsageStatsService(
            db.usageStatsDao(),
            FakeUsageStatsSource(listOf(stat("com.a", 100L), stat("com.b", 200L))),
            ZoneId.of("UTC"),
        )
        s.collectDailyStats(LocalDate.of(2026, 4, 20))
        s.collectDailyStats(LocalDate.of(2026, 4, 22))

        val rows = db.usageStatsDao().getInRange("2026-04-20", "2026-04-22")
        assertEquals(4, rows.size)
        // Trié par date ASC, fg DESC dans la même date
        assertEquals("2026-04-20", rows[0].date)
        assertTrue(rows[0].totalTimeForegroundMs >= rows[1].totalTimeForegroundMs)
    }

    @Test
    fun collect_skips_when_source_returns_empty() = runTest {
        val s = LocalUsageStatsService(db.usageStatsDao(), FakeUsageStatsSource(emptyList()), ZoneId.of("UTC"))
        val n = s.collectDailyStats(LocalDate.of(2026, 4, 20))
        assertEquals(0, n)
        assertEquals(0, db.usageStatsDao().count())
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `FakeUsageStatsSource` (class) — lines 19-21
- `queryUsageStats` (function) — lines 20-20
- `LocalUsageStatsServiceTest` (class) — lines 23-133
- `setUp` (function) — lines 28-34
- `tearDown` (function) — lines 36-37
- `stat` (function) — lines 39-50
- `collect_writes_one_row_per_package` (function) — lines 52-72
- `collect_aggregates_duplicate_packages` (function) — lines 74-90
- `collect_is_idempotent_via_replace` (function) — lines 92-107
- `query_in_range_returns_sorted` (function) — lines 109-124
- `collect_skips_when_source_returns_empty` (function) — lines 126-132
