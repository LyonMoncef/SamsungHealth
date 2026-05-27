package fr.datasaillance.nightfall.data.local.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JUnit purs (aucune dépendance Android) de l'algorithme de reconstruction
 * DT-3. Couvre TA-1, TA-2, TA-3, TA-4, TA-5, TA-8.
 */
class ReconstructSessionsTest {

    private val T = 1_000_000_000_000L

    private fun ev(pkg: String, type: Int, ts: Long) = UsageEventRecord(pkg, type, ts)

    // TA-1 — Session simple : RESUMED puis PAUSED
    @Test
    fun ta1_simple_session_resumed_then_paused() {
        val events = listOf(
            ev("com.example.app", 1, T + 0),
            ev("com.example.app", 2, T + 60_000),
        )
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 120_000)

        assertEquals(1, sessions.size)
        val s = sessions.first()
        assertEquals("com.example.app", s.packageName)
        assertEquals(T + 0, s.startMs)
        assertEquals(T + 60_000, s.endMs)
        assertEquals(60_000L, s.durationMs)
    }

    // TA-2 — Session laissée ouverte en fin de fenêtre → fermée à toMs
    @Test
    fun ta2_open_session_closed_at_toMs() {
        val events = listOf(ev("com.browser", 1, T + 0))
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 300_000)

        assertEquals(1, sessions.size)
        assertEquals(T + 0, sessions.first().startMs)
        assertEquals(T + 300_000, sessions.first().endMs)
    }

    // TA-3 — KEYGUARD_SHOWN (12) clôture toutes les sessions ouvertes
    @Test
    fun ta3_keyguard_shown_closes_all_open_sessions() {
        val events = listOf(
            ev("com.app1", 1, T + 0),
            ev("com.app2", 1, T + 10_000),
            ev("", 12, T + 30_000),
        )
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 60_000)

        assertEquals(2, sessions.size)
        val byPkg = sessions.associateBy { it.packageName }
        assertEquals(T + 30_000, byPkg.getValue("com.app1").endMs)
        assertEquals(T + 30_000, byPkg.getValue("com.app2").endMs)
        // Aucune session fermée à toMs
        assertTrue(sessions.none { it.endMs == T + 60_000 })
    }

    // TA-4 — Event PAUSED orphelin ignoré
    @Test
    fun ta4_orphan_paused_ignored() {
        val events = listOf(ev("com.orphan", 2, T + 0))
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 60_000)
        assertEquals(0, sessions.size)
    }

    // TA-5 — RESUMED consécutifs sans PAUSED (orphelin de début)
    @Test
    fun ta5_consecutive_resumed_without_paused() {
        val events = listOf(
            ev("com.app1", 1, T + 0),
            ev("com.app1", 1, T + 20_000),
            ev("com.app1", 2, T + 50_000),
        )
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 60_000)

        assertEquals(2, sessions.size)
        val sorted = sessions.sortedBy { it.startMs }
        assertEquals(T + 0, sorted[0].startMs)
        assertEquals(T + 20_000, sorted[0].endMs)
        assertEquals(T + 20_000, sorted[1].startMs)
        assertEquals(T + 50_000, sorted[1].endMs)
    }

    // TA-8 — 3 usages distincts d'une même app → 3 sessions séparées
    @Test
    fun ta8_three_separate_sessions_no_continuous_arc() {
        val events = listOf(
            ev("com.app", 1, T + 0),
            ev("com.app", 2, T + 60_000),
            ev("com.app", 1, T + 120_000),
            ev("com.app", 2, T + 180_000),
            ev("com.app", 1, T + 300_000),
            ev("com.app", 2, T + 360_000),
        )
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 400_000)

        assertEquals(3, sessions.size)
        val sorted = sessions.sortedBy { it.startMs }
        assertEquals(T + 0, sorted[0].startMs); assertEquals(T + 60_000, sorted[0].endMs)
        assertEquals(T + 120_000, sorted[1].startMs); assertEquals(T + 180_000, sorted[1].endMs)
        assertEquals(T + 300_000, sorted[2].startMs); assertEquals(T + 360_000, sorted[2].endMs)
        // Pas d'arc continu T+0 → T+360_000
        assertTrue(sessions.none { it.startMs == T + 0 && it.endMs == T + 360_000 })
    }

    // Fermeture par ACTIVITY_STOPPED (23) — variante de fermeture par package
    @Test
    fun close_by_activity_stopped_type23() {
        val events = listOf(
            ev("com.app", 1, T + 0),
            ev("com.app", 23, T + 40_000),
        )
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 60_000)
        assertEquals(1, sessions.size)
        assertEquals(T + 40_000, sessions.first().endMs)
    }

    // SCREEN_NON_INTERACTIVE (14) ferme aussi globalement
    @Test
    fun screen_off_type14_closes_all() {
        val events = listOf(
            ev("com.app1", 1, T + 0),
            ev("com.app2", 1, T + 5_000),
            ev("", 14, T + 20_000),
        )
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 60_000)
        assertEquals(2, sessions.size)
        assertTrue(sessions.all { it.endMs == T + 20_000 })
    }

    // Filtre durée nulle : RESUMED puis PAUSED au même timestamp → ignoré
    @Test
    fun zero_duration_session_filtered() {
        val events = listOf(
            ev("com.app", 1, T + 0),
            ev("com.app", 2, T + 0),
        )
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 60_000)
        assertEquals(0, sessions.size)
    }

    // Events fournis désordonnés → triés avant reconstruction
    @Test
    fun unsorted_events_are_sorted_first() {
        val events = listOf(
            ev("com.app", 2, T + 60_000),
            ev("com.app", 1, T + 0),
        )
        val sessions = reconstructSessions(events, fromMs = T - 1, toMs = T + 120_000)
        assertEquals(1, sessions.size)
        assertEquals(T + 0, sessions.first().startMs)
        assertEquals(T + 60_000, sessions.first().endMs)
    }
}
