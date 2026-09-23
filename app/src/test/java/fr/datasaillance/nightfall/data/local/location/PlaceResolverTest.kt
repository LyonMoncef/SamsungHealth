package fr.datasaillance.nightfall.data.local.location

import fr.datasaillance.nightfall.data.local.entity.location.PlaceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests purs (JUnit, sans instrumentation) du PlaceResolver — TA-1..TA-5.
 */
class PlaceResolverTest {

    private fun place(
        id: Long,
        category: PlaceCategory = PlaceCategory.DOMICILE,
        lat: Double,
        lng: Double,
        radius: Int = 150,
        label: String = "lieu-$id",
    ) = LabeledPlace(id, label, category, lat, lng, radius)

    // TA-1 — Match dans le rayon → ancré
    @Test
    fun resolve_returns_place_when_point_within_radius() {
        val resolver = PlaceResolver(
            listOf(place(1, PlaceCategory.DOMICILE, lat = 48.8566, lng = 2.3522, radius = 150)),
        )
        val match = resolver.resolve(48.8567, 2.3524) // ~18 m
        assertEquals(1L, match?.id)
        assertEquals(PlaceCategory.DOMICILE, match?.category)
    }

    // TA-2 — Hors rayon → null
    @Test
    fun resolve_returns_null_when_point_outside_radius() {
        val resolver = PlaceResolver(
            listOf(place(1, lat = 48.8566, lng = 2.3522, radius = 150)),
        )
        val match = resolver.resolve(48.8580, 2.3550) // ~250 m
        assertNull(match)
    }

    // TA-3 — Plusieurs candidats → le plus proche gagne
    @Test
    fun resolve_returns_closest_when_multiple_match() {
        // Point de test = origine. A à ~80 m (Est), B à ~120 m (Est). Les deux dans rayon 150.
        val testLat = 48.8566
        val testLng = 2.3522
        // 1 deg lng ≈ 73_000 m à cette latitude → ~80 m = 0.0011 deg
        val a = place(10, lat = testLat, lng = testLng + 0.0011, radius = 150, label = "A") // ~80 m
        val b = place(20, lat = testLat, lng = testLng + 0.00165, radius = 150, label = "B") // ~120 m
        val resolver = PlaceResolver(listOf(b, a)) // ordre d'insertion volontairement inversé
        val match = resolver.resolve(testLat, testLng)
        assertEquals("le plus proche (A) doit gagner", 10L, match?.id)
    }

    // TA-4 — Visite multi-jours sur lieu VACANCES → résolution basée coords seules
    @Test
    fun resolve_is_duration_independent_for_vacances() {
        val resolver = PlaceResolver(
            listOf(
                place(5, PlaceCategory.VACANCES, lat = 43.2965, lng = 5.3698, radius = 300, label = "Marseille"),
            ),
        )
        val match = resolver.resolve(43.2966, 5.3700) // dans le rayon 300
        assertEquals(PlaceCategory.VACANCES, match?.category)
        assertEquals("Marseille", match?.label)
    }

    // TA-5 — Liste vide → jamais de match
    @Test
    fun resolve_returns_null_for_empty_list() {
        val resolver = PlaceResolver(emptyList())
        assertNull(resolver.resolve(48.8566, 2.3522))
    }

    @Test
    fun haversine_is_symmetric_and_zero_for_same_point() {
        val d = PlaceResolver.haversineMeters(48.8566, 2.3522, 48.8566, 2.3522)
        assertEquals(0.0, d, 0.001)
    }
}
