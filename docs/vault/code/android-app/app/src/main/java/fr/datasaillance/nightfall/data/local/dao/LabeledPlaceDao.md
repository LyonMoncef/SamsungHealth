---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/LabeledPlaceDao.kt
git_blob: 789ea47f742f787d99c9e8ce08bc55b14afac790
last_synced: '2026-05-27T05:17:18Z'
loc: 34
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/LabeledPlaceDao.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/LabeledPlaceDao.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/LabeledPlaceDao.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import fr.datasaillance.nightfall.data.local.entity.location.LabeledPlaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabeledPlaceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(place: LabeledPlaceEntity): Long

    @Update
    suspend fun update(place: LabeledPlaceEntity)

    @Delete
    suspend fun delete(place: LabeledPlaceEntity)

    /** Flux réactif pour la liste de configuration — l'écran se met à jour en temps réel. */
    @Query("SELECT * FROM labeled_place ORDER BY category ASC, label ASC")
    fun getAllFlow(): Flow<List<LabeledPlaceEntity>>

    /** Snapshot synchrone utilisé par PlaceResolver (appelé hors UI thread). */
    @Query("SELECT * FROM labeled_place")
    suspend fun getAll(): List<LabeledPlaceEntity>

    @Query("SELECT COUNT(*) FROM labeled_place")
    suspend fun count(): Int
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LabeledPlaceDao` (class) — lines 12-34
- `insert` (function) — lines 15-16
- `update` (function) — lines 18-19
- `delete` (function) — lines 21-22
- `getAllFlow` (function) — lines 25-26
- `getAll` (function) — lines 29-30
- `count` (function) — lines 32-33
