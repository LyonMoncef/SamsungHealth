---
type: code-source
language: kotlin
file_path: android-app/app/src/testNative/java/fr/datasaillance/nightfall/viewmodel/radial/RadialClockViewModelUsageSessionsTest.kt
git_blob: 4730a5ffb8b248e7f071e54b714465c446969854
last_synced: '2026-05-27T06:16:08Z'
loc: 113
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/testNative/java/fr/datasaillance/nightfall/viewmodel/radial/RadialClockViewModelUsageSessionsTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/testNative/java/fr/datasaillance/nightfall/viewmodel/radial/RadialClockViewModelUsageSessionsTest.kt`](../../../android-app/app/src/testNative/java/fr/datasaillance/nightfall/viewmodel/radial/RadialClockViewModelUsageSessionsTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
            sleepDao = db.sleepDao(),
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
            sleepDao = db.sleepDao(),
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `RadialClockViewModelUsageSessionsTest` (class) — lines 30-113
- `setUp` (function) — lines 39-50
- `tearDown` (function) — lines 52-56
- `buildDaysMap_populates_usage_sessions_when_dao_present` (function) — lines 58-93
- `buildDaysMap_no_usage_session_dao_yields_empty_degraded` (function) — lines 95-112
