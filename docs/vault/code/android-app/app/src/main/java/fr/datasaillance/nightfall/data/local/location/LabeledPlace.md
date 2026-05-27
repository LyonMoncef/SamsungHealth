---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/LabeledPlace.kt
git_blob: a96688463c500d7a38adf57149c86a809894157c
last_synced: '2026-05-27T05:17:18Z'
loc: 27
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/LabeledPlace.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/LabeledPlace.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/location/LabeledPlace.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LabeledPlace` (class) — lines 10-17
- `toLabeledPlace` (function) — lines 20-27
