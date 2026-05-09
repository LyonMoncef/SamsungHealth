---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/SleepStageEntity.kt
git_blob: 34941724126ebd8cf65ea76f5a9783ed07d434d6
last_synced: '2026-05-09T15:08:38Z'
loc: 31
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/SleepStageEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/SleepStageEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/SleepStageEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_stages",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("session_id"),
        Index(value = ["stage_start", "stage_end"], unique = true),
    ],
)
data class SleepStageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "stage_type") val stageType: String, // DEEP / LIGHT / REM / AWAKE
    @ColumnInfo(name = "stage_start") val stageStartMs: Long,
    @ColumnInfo(name = "stage_end") val stageEndMs: Long,
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepStageEntity` (class) — lines 9-31
