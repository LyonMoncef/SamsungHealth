---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/ExerciseDao.kt
git_blob: 25f5857291d40aa05ceb14a523397fff8c429e65
last_synced: '2026-05-09T15:08:38Z'
loc: 26
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/ExerciseDao.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/ExerciseDao.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/dao/ExerciseDao.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.datasaillance.nightfall.data.local.entity.ExerciseSessionEntity

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessions(rows: List<ExerciseSessionEntity>): List<Long>

    @Query("SELECT * FROM exercise_sessions WHERE start_time >= :fromMs AND start_time < :toMs ORDER BY start_time ASC")
    suspend fun getInRange(fromMs: Long, toMs: Long): List<ExerciseSessionEntity>

    @Query("SELECT * FROM exercise_sessions ORDER BY start_time ASC")
    suspend fun getAll(): List<ExerciseSessionEntity>

    @Query("SELECT COUNT(*) FROM exercise_sessions")
    suspend fun count(): Int

    @Query("DELETE FROM exercise_sessions")
    suspend fun deleteAll()
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ExerciseDao` (class) — lines 9-26
- `insertSessions` (function) — lines 12-13
- `getInRange` (function) — lines 15-16
- `getAll` (function) — lines 18-19
- `count` (function) — lines 21-22
- `deleteAll` (function) — lines 24-25
