---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LocationPathEntity.kt
git_blob: 7fa3c152761340c910cb7114f18a227de47198f5
last_synced: '2026-05-20T16:30:46Z'
loc: 47
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LocationPathEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LocationPathEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/LocationPathEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Chemin GPS détaillé (Google Takeout `timelinePath`). Contient une suite de waypoints
 * `{lat, lng, t}` qui permet de tracer la trajectoire réelle au lieu d'un trait
 * vol d'oiseau.
 *
 * Volume typique : ~10 waypoints par path, ~1000 paths pour 3 ans d'historique → 10k points
 * → JSON < 1 MB → OK pour stockage Room.
 *
 * `points_json` = `[{"lat":45.527,"lng":4.878,"t":1708347000000}, ...]` (epoch ms).
 */
@Entity(
    tableName = "location_paths",
    indices = [
        Index(value = ["start_ms", "end_ms"], unique = true),
        Index("start_ms"),
    ],
)
data class LocationPathEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "start_ms") val startMs: Long,
    @ColumnInfo(name = "end_ms") val endMs: Long,

    /** JSON sérialisé des waypoints. Voir [LocationPathPoint] pour le schéma. */
    @ColumnInfo(name = "points_json") val pointsJson: String,

    /** Nombre de waypoints (dénormalisé pour filtrage rapide sans parser le JSON). */
    @ColumnInfo(name = "point_count") val pointCount: Int,

    val source: String = "takeout",
    @ColumnInfo(name = "imported_at_ms") val importedAtMs: Long = System.currentTimeMillis(),
)

/** Schéma d'un waypoint sérialisé dans `points_json`. */
data class LocationPathPoint(
    val lat: Double,
    val lng: Double,
    val t: Long,
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LocationPathEntity` (class) — lines 18-40
- `LocationPathPoint` (class) — lines 43-47
