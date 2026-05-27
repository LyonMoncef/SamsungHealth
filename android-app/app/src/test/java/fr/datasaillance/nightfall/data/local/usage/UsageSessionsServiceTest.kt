package fr.datasaillance.nightfall.data.local.usage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/** Fake events source — injecte des fixtures sans Context Android. */
private class FakeUsageEventsSource(private val events: List<UsageEventRecord>) : UsageEventsSource {
    override fun queryEvents(fromMs: Long, toMs: Long): List<UsageEventRecord> =
        events.filter { it.timeStampMs in fromMs until toMs }
}

@RunWith(AndroidJUnit4::class)
class UsageSessionsServiceTest {

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

    // TA-7 — Agrégation par date cohérente avec le fuseau device
    @Test
    fun ta7_date_derived_from_start_ms_in_device_zone() = runTest {
        // 2026-01-15T23:30:00Z → 00:30:00 le 16 janvier heure locale Paris (UTC+1 hiver)
        val startUtc = java.time.Instant.parse("2026-01-15T23:30:00Z").toEpochMilli()
        val endUtc = startUtc + 60_000
        val source = FakeUsageEventsSource(listOf(
            UsageEventRecord("com.app", 1, startUtc),
            UsageEventRecord("com.app", 2, endUtc),
        ))
        val service = UsageSessionsService(
            dao = db.usageSessionDao(),
            eventsSource = source,
            zone = ZoneId.of("Europe/Paris"),
            now = { 0L },
        )

        val n = service.collectSessions(LocalDate.of(2026, 1, 16))
        assertEquals(1, n)
        val rows = db.usageSessionDao().getByDate("2026-01-16")
        assertEquals(1, rows.size)
        assertEquals("2026-01-16", rows.first().date)
        // pas la date UTC
        assertEquals(0, db.usageSessionDao().getByDate("2026-01-15").size)
    }

    @Test
    fun collect_persists_reconstructed_sessions() = runTest {
        val day = LocalDate.of(2026, 4, 20)
        val zone = ZoneId.of("UTC")
        val base = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val source = FakeUsageEventsSource(listOf(
            UsageEventRecord("com.youtube", 1, base + 1_000),
            UsageEventRecord("com.youtube", 2, base + 61_000),
            UsageEventRecord("com.discord", 1, base + 120_000),
            UsageEventRecord("com.discord", 2, base + 180_000),
        ))
        val service = UsageSessionsService(db.usageSessionDao(), source, zone, now = { 0L })

        val n = service.collectSessions(day)
        assertEquals(2, n)
        val rows = db.usageSessionDao().getByDate("2026-04-20")
        assertEquals(2, rows.size)
        assertEquals(60_000L, rows[0].durationMs)
    }

    @Test
    fun collect_is_idempotent_on_rerun() = runTest {
        val day = LocalDate.of(2026, 4, 20)
        val zone = ZoneId.of("UTC")
        val base = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val source = FakeUsageEventsSource(listOf(
            UsageEventRecord("com.app", 1, base + 1_000),
            UsageEventRecord("com.app", 2, base + 61_000),
        ))
        val service = UsageSessionsService(db.usageSessionDao(), source, zone, now = { 0L })

        service.collectSessions(day)
        service.collectSessions(day)

        assertEquals(1, db.usageSessionDao().count())
    }

    @Test
    fun collect_returns_zero_when_no_events() = runTest {
        val service = UsageSessionsService(
            db.usageSessionDao(),
            FakeUsageEventsSource(emptyList()),
            ZoneId.of("UTC"),
            now = { 0L },
        )
        val n = service.collectSessions(LocalDate.of(2026, 4, 20))
        assertEquals(0, n)
        assertEquals(0, db.usageSessionDao().count())
    }

    @Test
    fun backfill_collects_multiple_days() = runTest {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.now(zone)
        val baseToday = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val baseYesterday = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val source = FakeUsageEventsSource(listOf(
            UsageEventRecord("com.app", 1, baseYesterday + 1_000),
            UsageEventRecord("com.app", 2, baseYesterday + 61_000),
            UsageEventRecord("com.app", 1, baseToday + 1_000),
            UsageEventRecord("com.app", 2, baseToday + 61_000),
        ))
        val service = UsageSessionsService(db.usageSessionDao(), source, zone, now = { 0L })

        val total = service.backfillSessions(days = 3)
        assertEquals(2, total)
        assertEquals(2, db.usageSessionDao().count())
    }
}
