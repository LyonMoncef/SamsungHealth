---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/StepsHourlyEntity.kt
git_blob: 609a0d7d5fc77a0c61d9158862fac3a1f0dbdaee
last_synced: '2026-05-09T15:08:38Z'
loc: 21
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/StepsHourlyEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/StepsHourlyEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/StepsHourlyEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Steps agrégés à l'heure. Miroir de `server.db.models.StepsHourly`.
 */
@Entity(
    tableName = "steps_hourly",
    indices = [Index(value = ["date", "hour"], unique = true)],
)
data class StepsHourlyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String, // ISO yyyy-MM-dd
    val hour: Int,
    @ColumnInfo(name = "step_count") val stepCount: Int,
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `StepsHourlyEntity` (class) — lines 11-21
