---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/Converters.kt
git_blob: b6637fbd61912029a4d4587597132b86b5868d8b
last_synced: '2026-05-27T05:17:18Z'
loc: 20
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/Converters.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/Converters.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/database/Converters.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `Converters` (class) — lines 12-20
- `fromPlaceCategory` (function) — lines 14-15
- `toPlaceCategory` (function) — lines 17-19
