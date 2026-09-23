---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/PlaceResolver.kt
git_blob: 4df556bae68f465120235be6a1e1f72ce94e3038
last_synced: '2026-05-27T05:17:18Z'
loc: 52
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/PlaceResolver.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/PlaceResolver.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/PlaceResolver.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `PlaceResolver` (class) — lines 16-52
- `resolve` (function) — lines 23-35
- `haversineMeters` (function) — lines 42-50
