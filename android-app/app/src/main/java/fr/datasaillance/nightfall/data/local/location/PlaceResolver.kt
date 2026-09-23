package fr.datasaillance.nightfall.data.local.location

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Résout une coordonnée (lat, lng) contre une liste de lieux labellisés par distance
 * haversine. Classe pure : pas de Context, pas de coroutines, pas de Room, pas de
 * `android.location.Location` — testable en JUnit unitaire sans instrumentation.
 *
 * Contrainte C1 : aucune requête réseau, aucun geocoding. Tout est calculé localement.
 */
class PlaceResolver(private val places: List<LabeledPlace>) {

    /**
     * Retourne le lieu labellisé le plus proche dont le centre est à distance
     * ≤ radiusMeters du point (lat, lng). En cas d'égalité de distance, le lieu
     * d'`id` le plus petit gagne (déterminisme garanti). `null` si aucun match.
     */
    fun resolve(lat: Double, lng: Double): LabeledPlace? {
        var best: LabeledPlace? = null
        var bestDist = Double.MAX_VALUE
        for (place in places) {
            val d = haversineMeters(lat, lng, place.lat, place.lng)
            if (d > place.radiusMeters) continue
            if (d < bestDist || (d == bestDist && (best == null || place.id < best.id))) {
                best = place
                bestDist = d
            }
        }
        return best
    }

    companion object {
        /** Rayon terrestre moyen en mètres. */
        private const val EARTH_RADIUS_M = 6_371_000.0

        /** Distance haversine en mètres entre deux points en degrés décimaux. */
        fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val phi1 = Math.toRadians(lat1)
            val phi2 = Math.toRadians(lat2)
            val dPhi = Math.toRadians(lat2 - lat1)
            val dLambda = Math.toRadians(lng2 - lng1)
            val a = sin(dPhi / 2).let { it * it } +
                cos(phi1) * cos(phi2) * sin(dLambda / 2).let { it * it }
            return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(a)))
        }
    }
}
