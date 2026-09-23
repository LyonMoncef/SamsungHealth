---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LocationVisitEntity.kt
git_blob: 6676ff72778e9c636a950796b0ab7b0f52c26341
last_synced: '2026-05-09T19:12:26Z'
loc: 44
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LocationVisitEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LocationVisitEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LocationVisitEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Visite : Google Maps Timeline `placeVisit`. POI ou lieu où l'utilisateur est resté
 * un temps significatif.
 *
 * Coordonnées en degrés décimaux (Google exporte en E7 = degrés × 1e7, on convertit).
 * Timestamps en epoch millis UTC.
 */
@Entity(
    tableName = "location_visits",
    indices = [
        Index(value = ["start_ms", "end_ms", "lat", "lng"], unique = true),
        Index("start_ms"),
        Index("place_id"),
    ],
)
data class LocationVisitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "end_ms") val endMs: Long,

    val lat: Double,
    val lng: Double,

    @ColumnInfo(name = "place_id") val placeId: String? = null,
    @ColumnInfo(name = "place_name") val placeName: String? = null,
    val address: String? = null,

    /** "HIGH_CONFIDENCE" / "MEDIUM_CONFIDENCE" / "LOW_CONFIDENCE" / null. */
    val confidence: String? = null,

    /** Source : "takeout" en Phase A_gps, "live" en Phase B_gps. Permet futur cross-check. */
    val source: String = "takeout",

    @ColumnInfo(name = "imported_at_ms") val importedAtMs: Long = System.currentTimeMillis(),
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LocationVisitEntity` (class) — lines 15-44
