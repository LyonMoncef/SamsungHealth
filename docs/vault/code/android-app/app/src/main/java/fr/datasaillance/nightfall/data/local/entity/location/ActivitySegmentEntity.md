---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/ActivitySegmentEntity.kt
git_blob: d498b4f96a9f72694da688c9ab36aeea6b108465
last_synced: '2026-05-09T19:12:26Z'
loc: 39
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/ActivitySegmentEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/ActivitySegmentEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/ActivitySegmentEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Segment d'activité : Google Maps Timeline `activitySegment`. Trajet entre 2 visites
 * (marche, voiture, vélo, transport en commun, etc.).
 */
@Entity(
    tableName = "activity_segments",
    indices = [
        Index(value = ["start_ms", "end_ms", "activity_type"], unique = true),
        Index("start_ms"),
        Index("activity_type"),
    ],
)
data class ActivitySegmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "end_ms") val endMs: Long,

    @ColumnInfo(name = "start_lat") val startLat: Double,
    @ColumnInfo(name = "start_lng") val startLng: Double,
    @ColumnInfo(name = "end_lat") val endLat: Double,
    @ColumnInfo(name = "end_lng") val endLng: Double,

    /** Valeurs Google : WALKING, RUNNING, CYCLING, IN_PASSENGER_VEHICLE, IN_BUS, IN_SUBWAY, FLYING, etc. */
    @ColumnInfo(name = "activity_type") val activityType: String,

    @ColumnInfo(name = "distance_m") val distanceMeters: Int? = null,
    val confidence: String? = null,
    val source: String = "takeout",
    @ColumnInfo(name = "imported_at_ms") val importedAtMs: Long = System.currentTimeMillis(),
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ActivitySegmentEntity` (class) — lines 12-39
