---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/HeartRateHourlyEntity.kt
git_blob: de3556482abd4f234635470d4a097633f34149bb
last_synced: '2026-05-09T15:08:38Z'
loc: 25
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/HeartRateHourlyEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/HeartRateHourlyEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/HeartRateHourlyEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Mesure HR agrégée à l'heure. Miroir de `server.db.models.HeartRateHourly`.
 * Clé unique (date, hour) — un seul enregistrement par heure.
 */
@Entity(
    tableName = "heart_rate_hourly",
    indices = [Index(value = ["date", "hour"], unique = true)],
)
data class HeartRateHourlyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String, // ISO yyyy-MM-dd
    val hour: Int,
    @ColumnInfo(name = "min_bpm") val minBpm: Int,
    @ColumnInfo(name = "max_bpm") val maxBpm: Int,
    @ColumnInfo(name = "avg_bpm") val avgBpm: Int,
    @ColumnInfo(name = "sample_count") val sampleCount: Int,
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `HeartRateHourlyEntity` (class) — lines 12-25
