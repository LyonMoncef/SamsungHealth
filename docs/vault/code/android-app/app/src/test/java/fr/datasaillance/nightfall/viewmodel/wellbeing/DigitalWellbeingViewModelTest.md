---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/wellbeing/DigitalWellbeingViewModelTest.kt
git_blob: b6ed663de32239037a69a6484ee29a9d8c30abee
last_synced: '2026-05-20T18:53:28Z'
loc: 195
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/wellbeing/DigitalWellbeingViewModelTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/wellbeing/DigitalWellbeingViewModelTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/wellbeing/DigitalWellbeingViewModelTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.viewmodel.wellbeing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.data.local.entity.usage.UsageDailyEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DigitalWellbeingViewModelTest {

    private lateinit var db: NightfallDatabase

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        )
            .allowMainThreadQueries()
            // Force Room queries à tourner sur le thread courant pour que
            // les await suspendus restent synchrones avec UnconfinedTestDispatcher.
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private val fixedToday: LocalDate = LocalDate.parse("2026-05-20")

    private suspend fun seed(rows: List<UsageDailyEntity>) {
        db.usageStatsDao().upsertDaily(rows)
    }

    private fun row(
        date: String,
        pkg: String,
        fgMs: Long,
        collectedAtMs: Long = 1_000_000L,
    ) = UsageDailyEntity(
        date = date,
        packageName = pkg,
        totalTimeForegroundMs = fgMs,
        totalTimeVisibleMs = 0L,
        totalTimeForegroundServiceMs = 0L,
        lastTimeUsedMs = 0L,
        appLaunchCount = 0,
        collectedAtMs = collectedAtMs,
    )

    @Test
    fun refresh_with_permission_off_returns_empty_topApps_and_flag_false() = runTest(testDispatcher) {
        val vm = DigitalWellbeingViewModel(
            checkPermission = { false },
            dao = db.usageStatsDao(),
            clock = { fixedToday },
        )
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.permissionGranted)
        assertTrue(state.topApps.isEmpty())
        assertEquals(0L, state.totalScreenTimeMs)
    }

    @Test
    fun refresh_today_aggregates_only_today() = runTest(testDispatcher) {
        seed(listOf(
            row("2026-05-20", "com.chrome", 3_600_000L),  // today
            row("2026-05-20", "com.gmail", 1_200_000L),   // today
            row("2026-05-19", "com.chrome", 900_000L),    // yesterday — exclu en TODAY
        ))
        val vm = DigitalWellbeingViewModel(
            checkPermission = { true },
            dao = db.usageStatsDao(),
            clock = { fixedToday },
        )
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(WellbeingPeriod.TODAY, state.selectedPeriod)
        assertEquals(2, state.topApps.size)
        // Tri desc par totalForegroundMs
        assertEquals("com.chrome", state.topApps[0].packageName)
        assertEquals(3_600_000L, state.topApps[0].totalForegroundMs)
        assertEquals(1, state.topApps[0].daysWithUsage)
        assertEquals("com.gmail", state.topApps[1].packageName)
        // Total = somme du jour
        assertEquals(4_800_000L, state.totalScreenTimeMs)
    }

    @Test
    fun setPeriod_LAST_7_aggregates_across_days() = runTest(testDispatcher) {
        seed(listOf(
            row("2026-05-20", "com.chrome", 3_600_000L),
            row("2026-05-19", "com.chrome", 7_200_000L),
            row("2026-05-18", "com.chrome", 1_800_000L),
            row("2026-05-15", "com.gmail", 600_000L),
            row("2026-05-10", "com.spotify", 5_000_000L),  // hors fenêtre 7j
        ))
        val vm = DigitalWellbeingViewModel(
            checkPermission = { true },
            dao = db.usageStatsDao(),
            clock = { fixedToday },
        )
        advanceUntilIdle()
        vm.setPeriod(WellbeingPeriod.LAST_7)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(WellbeingPeriod.LAST_7, state.selectedPeriod)
        // Chrome agrégé sur 3 jours
        val chrome = state.topApps.first { it.packageName == "com.chrome" }
        assertEquals(12_600_000L, chrome.totalForegroundMs)
        assertEquals(3, chrome.daysWithUsage)
        // Gmail apparaît 1 jour
        val gmail = state.topApps.first { it.packageName == "com.gmail" }
        assertEquals(1, gmail.daysWithUsage)
        // Spotify (mai 10) hors fenêtre [14..20]
        assertTrue(state.topApps.none { it.packageName == "com.spotify" })
        // Total = chrome + gmail
        assertEquals(13_200_000L, state.totalScreenTimeMs)
    }

    @Test
    fun setPeriod_same_period_is_noop() = runTest(testDispatcher) {
        val vm = DigitalWellbeingViewModel(
            checkPermission = { true },
            dao = db.usageStatsDao(),
            clock = { fixedToday },
        )
        advanceUntilIdle()
        val before = vm.uiState.value
        vm.setPeriod(WellbeingPeriod.TODAY)  // same as default
        advanceUntilIdle()
        val after = vm.uiState.value
        // Pas de re-trigger refresh : on conserve l'instance d'état (égalité référentielle pas garantie,
        // mais le contenu est identique et selectedPeriod inchangé)
        assertEquals(before.selectedPeriod, after.selectedPeriod)
        assertEquals(before.topApps.size, after.topApps.size)
    }

    @Test
    fun aggregate_uses_packageResolver_when_provided() = runTest(testDispatcher) {
        seed(listOf(
            row("2026-05-20", "com.chrome", 1_000L),
            row("2026-05-20", "fr.datasaillance.nightfall.debug", 500L),
        ))
        // Resolver fictif qui mappe packageName → label custom
        val fakeResolver = object : fr.datasaillance.nightfall.data.local.usage.PackageInfoResolver(
            ApplicationProvider.getApplicationContext<android.content.Context>().packageManager
        ) {
            override fun labelFor(packageName: String): String = when (packageName) {
                "com.chrome" -> "Chrome"
                "fr.datasaillance.nightfall.debug" -> "Nightfall"
                else -> packageName
            }
        }
        val vm = DigitalWellbeingViewModel(
            checkPermission = { true },
            dao = db.usageStatsDao(),
            packageResolver = fakeResolver,
            clock = { fixedToday },
        )
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(2, state.topApps.size)
        // Chrome est en tête (1000 > 500), avec son label custom
        assertEquals("Chrome", state.topApps[0].displayLabel)
        assertEquals("com.chrome", state.topApps[0].packageName)
        assertEquals("Nightfall", state.topApps[1].displayLabel)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `DigitalWellbeingViewModelTest` (class) — lines 24-195
- `setUp` (function) — lines 32-45
- `tearDown` (function) — lines 47-51
- `seed` (function) — lines 55-57
- `row` (function) — lines 59-73
- `refresh_with_permission_off_returns_empty_topApps_and_flag_false` (function) — lines 75-87
- `refresh_today_aggregates_only_today` (function) — lines 89-112
- `setPeriod_LAST_7_aggregates_across_days` (function) — lines 114-145
- `setPeriod_same_period_is_noop` (function) — lines 147-163
- `aggregate_uses_packageResolver_when_provided` (function) — lines 165-194
- `labelFor` (function) — lines 175-179
