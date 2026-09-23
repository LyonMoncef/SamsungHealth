package fr.datasaillance.nightfall.data.local.location

import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests purs du clustering de suggestions — DT-7 / L5.
 */
class PlaceSuggestionServiceTest {

    private fun visit(
        lat: Double,
        lng: Double,
        startMs: Long = 0L,
        endMs: Long = 1L,
        placeName: String? = null,
    ) = LocationVisitEntity(
        startMs = startMs,
        endMs = endMs,
        lat = lat,
        lng = lng,
        placeName = placeName,
    )

    @Test
    fun cluster_below_threshold_is_not_suggested() {
        val visits = listOf(
            visit(48.8566, 2.3522),
            visit(48.8567, 2.3523),
        ) // 2 visites < 3
        val suggestions = PlaceSuggestionService.computeSuggestions(visits)
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun cluster_at_threshold_becomes_suggestion_with_centroid_and_count() {
        val visits = listOf(
            visit(48.8566, 2.3522, startMs = 100, endMs = 200, placeName = "Bureau"),
            visit(48.8567, 2.3523, startMs = 300, endMs = 400, placeName = "Bureau"),
            visit(48.8565, 2.3521, startMs = 500, endMs = 900, placeName = "Bureau"),
        )
        val suggestions = PlaceSuggestionService.computeSuggestions(visits)
        assertEquals(1, suggestions.size)
        val s = suggestions.first()
        assertEquals(3, s.visitCount)
        assertEquals(900L, s.lastVisitMs)
        assertEquals("Bureau", s.approximateAddress)
        // barycentre proche du centre du cluster
        assertEquals(48.8566, s.lat, 0.001)
        assertEquals(2.3522, s.lng, 0.001)
    }

    @Test
    fun far_apart_visits_form_separate_clusters() {
        // Paris (3 visites) + Marseille (3 visites), > 200 m d'écart entre groupes.
        val paris = (0 until 3).map { visit(48.8566, 2.3522, startMs = it.toLong(), endMs = it + 1L) }
        val marseille = (0 until 3).map { visit(43.2965, 5.3698, startMs = it.toLong(), endMs = it + 1L) }
        val suggestions = PlaceSuggestionService.computeSuggestions(paris + marseille)
        assertEquals(2, suggestions.size)
    }

    @Test
    fun empty_visits_returns_empty() {
        assertTrue(PlaceSuggestionService.computeSuggestions(emptyList()).isEmpty())
    }
}
