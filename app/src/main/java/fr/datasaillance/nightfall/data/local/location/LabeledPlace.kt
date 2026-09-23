package fr.datasaillance.nightfall.data.local.location

import fr.datasaillance.nightfall.data.local.entity.location.LabeledPlaceEntity
import fr.datasaillance.nightfall.data.local.entity.location.PlaceCategory

/**
 * DTO immuable exposé au ViewModel et au canvas — découplé de l'entité Room.
 * Aucune dépendance Android : utilisable tel quel en test JUnit pur.
 */
data class LabeledPlace(
    val id: Long,
    val label: String,
    val category: PlaceCategory,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
)

/** Mappe une entité Room vers son DTO immuable. */
fun LabeledPlaceEntity.toLabeledPlace(): LabeledPlace = LabeledPlace(
    id = id,
    label = label,
    category = category,
    lat = lat,
    lng = lng,
    radiusMeters = radiusMeters,
)
