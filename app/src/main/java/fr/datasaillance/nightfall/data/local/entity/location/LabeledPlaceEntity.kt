package fr.datasaillance.nightfall.data.local.entity.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lieu labellisé par l'utilisateur (domicile, travail, famille, vacances, autre).
 *
 * Coordonnées en degrés décimaux WGS-84. Une visite ([LocationVisitEntity]) qui tombe
 * dans le rayon d'un lieu labellisé est dite « ancrée » — quelle que soit la catégorie.
 *
 * Sécurité : table chiffrée at-rest via SQLCipher full-DB (clé Android Keystore). Pas
 * d'index unique sur (label, category) — l'utilisateur peut déclarer deux lieux de même
 * nom à des adresses différentes ; pas de notion de domicile unique.
 */
@Entity(
    tableName = "labeled_place",
    indices = [Index("category")],
)
data class LabeledPlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Libellé libre saisi par l'utilisateur. Ex : "Maison de mamie", "Bureau Paris". */
    val label: String,

    /** Catégorie sémantique. Plusieurs lieux par catégorie autorisés. */
    val category: PlaceCategory,

    /** Latitude en degrés décimaux. */
    val lat: Double,

    /** Longitude en degrés décimaux. */
    val lng: Double,

    /** Rayon de match en mètres. Défaut : 150 m. */
    @ColumnInfo(name = "radius_meters") val radiusMeters: Int = 150,

    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis(),
)
