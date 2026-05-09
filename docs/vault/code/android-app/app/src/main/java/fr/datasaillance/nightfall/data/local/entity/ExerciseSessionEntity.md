---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/ExerciseSessionEntity.kt
git_blob: 730d9028b759bad62c196bf79654742311fb5a8b
last_synced: '2026-05-09T15:08:38Z'
loc: 21
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/ExerciseSessionEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/ExerciseSessionEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/ExerciseSessionEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_sessions",
    indices = [Index(value = ["start_time", "end_time"], unique = true)],
)
data class ExerciseSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "start_time") val startTimeMs: Long,
    @ColumnInfo(name = "end_time") val endTimeMs: Long,
    @ColumnInfo(name = "exercise_type") val exerciseType: String,
    @ColumnInfo(name = "duration_min") val durationMin: Int? = null,
    @ColumnInfo(name = "calorie") val calorie: Float? = null,
    @ColumnInfo(name = "mean_heart_rate") val meanHeartRate: Int? = null,
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ExerciseSessionEntity` (class) — lines 8-21
