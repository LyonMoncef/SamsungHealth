package fr.datasaillance.nightfall.data.local.location

import fr.datasaillance.nightfall.data.local.entity.location.LocationVisitEntity

/**
 * Suggestion de lieu déduite d'un cluster de visites fréquentes. Sert à proposer à
 * l'utilisateur de labelliser un lieu qu'il fréquente, sans qu'il ait à connaître ses
 * coordonnées exactes (voie A de la saisie, cf. spec DT-7).
 */
data class PlaceSuggestion(
    val lat: Double,
    val lng: Double,
    val visitCount: Int,
    val lastVisitMs: Long,
    /** placeName/address le plus fréquent du cluster, null si aucun. */
    val approximateAddress: String?,
)

/**
 * Clustering naïf des visites par proximité géographique. Pur et déterministe :
 * aucune dépendance Android, aucun réseau (C1). Les suggestions sont calculées
 * uniquement à partir des `LocationVisitEntity` locales.
 */
object PlaceSuggestionService {

    /** Rayon de regroupement d'un cluster, en mètres. */
    const val CLUSTER_RADIUS_M = 200.0

    /** Nombre minimal de visites pour qu'un cluster devienne une suggestion. */
    const val MIN_VISITS = 3

    /**
     * Regroupe les visites par proximité (rayon [CLUSTER_RADIUS_M]) et ne retourne
     * que les clusters d'au moins [MIN_VISITS] visites. Chaque cluster expose le
     * barycentre, le nombre de visites, la date de la dernière visite et l'adresse
     * la plus fréquente. Trié par nombre de visites décroissant (tie-break : lat).
     *
     * Algorithme : agrégation gloutonne — chaque visite rejoint le premier cluster
     * dont le barycentre courant est dans le rayon, sinon ouvre un nouveau cluster.
     */
    fun computeSuggestions(visits: List<LocationVisitEntity>): List<PlaceSuggestion> {
        val clusters = mutableListOf<MutableCluster>()
        for (v in visits) {
            val match = clusters.firstOrNull {
                PlaceResolver.haversineMeters(v.lat, v.lng, it.centroidLat, it.centroidLng) <= CLUSTER_RADIUS_M
            }
            if (match != null) match.add(v) else clusters.add(MutableCluster().apply { add(v) })
        }
        return clusters
            .filter { it.count >= MIN_VISITS }
            .map { it.toSuggestion() }
            .sortedWith(compareByDescending<PlaceSuggestion> { it.visitCount }.thenBy { it.lat })
    }

    private class MutableCluster {
        private var sumLat = 0.0
        private var sumLng = 0.0
        var count = 0
            private set
        private var lastVisitMs = Long.MIN_VALUE
        private val labelFreq = HashMap<String, Int>()

        val centroidLat: Double get() = if (count == 0) 0.0 else sumLat / count
        val centroidLng: Double get() = if (count == 0) 0.0 else sumLng / count

        fun add(v: LocationVisitEntity) {
            sumLat += v.lat
            sumLng += v.lng
            count += 1
            if (v.endMs > lastVisitMs) lastVisitMs = v.endMs
            (v.placeName ?: v.address)?.let { labelFreq[it] = (labelFreq[it] ?: 0) + 1 }
        }

        fun toSuggestion() = PlaceSuggestion(
            lat = centroidLat,
            lng = centroidLng,
            visitCount = count,
            lastVisitMs = lastVisitMs,
            approximateAddress = labelFreq.maxByOrNull { it.value }?.key,
        )
    }
}
