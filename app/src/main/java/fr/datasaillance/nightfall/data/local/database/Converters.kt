package fr.datasaillance.nightfall.data.local.database

import androidx.room.TypeConverter
import fr.datasaillance.nightfall.data.local.entity.location.PlaceCategory

/**
 * Convertisseurs Room pour les types non primitifs persistés dans [NightfallDatabase].
 *
 * `PlaceCategory` ↔ String : stocké comme `TEXT` (nom de l'enum). Une valeur inconnue
 * (corruption / version antérieure) retombe sur [PlaceCategory.AUTRE] plutôt que de crasher.
 */
class Converters {

    @TypeConverter
    fun fromPlaceCategory(value: PlaceCategory): String = value.name

    @TypeConverter
    fun toPlaceCategory(value: String): PlaceCategory =
        runCatching { PlaceCategory.valueOf(value) }.getOrDefault(PlaceCategory.AUTRE)
}
