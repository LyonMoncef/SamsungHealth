---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LabeledPlaceEntity.kt
git_blob: caa091918ed295244b9fdab68de1740f32c497d8
last_synced: '2026-05-27T05:17:18Z'
loc: 43
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LabeledPlaceEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LabeledPlaceEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LabeledPlaceEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LabeledPlaceEntity` (class) — lines 18-43
