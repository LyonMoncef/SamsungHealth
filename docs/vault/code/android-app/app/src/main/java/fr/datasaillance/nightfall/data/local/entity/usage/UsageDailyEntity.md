---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/usage/UsageDailyEntity.kt
git_blob: 4362df297b9e24dc15f7b98a87e0e0e48d00a7e6
last_synced: '2026-05-09T18:49:36Z'
loc: 38
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/usage/UsageDailyEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/usage/UsageDailyEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/usage/UsageDailyEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity.usage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stats d'usage agrégées par jour, par package. Une row = (date, package).
 * Source : `UsageStatsManager.queryUsageStats(INTERVAL_DAILY, ...)`.
 *
 * `date` au format ISO `yyyy-MM-dd` (jour calendaire local), pour s'aligner sur
 * la sémantique Android UsageStatsManager qui agrège par fuseau du device.
 */
@Entity(
    tableName = "usage_daily",
    indices = [
        Index(value = ["date", "package_name"], unique = true),
        Index("date"),
        Index("package_name"),
    ],
)
data class UsageDailyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val date: String,                                                  // yyyy-MM-dd
    @ColumnInfo(name = "package_name") val packageName: String,

    @ColumnInfo(name = "total_time_foreground_ms") val totalTimeForegroundMs: Long,
    @ColumnInfo(name = "total_time_visible_ms") val totalTimeVisibleMs: Long = 0L,
    @ColumnInfo(name = "total_time_fgs_ms") val totalTimeForegroundServiceMs: Long = 0L,

    @ColumnInfo(name = "last_time_used_ms") val lastTimeUsedMs: Long = 0L,
    @ColumnInfo(name = "app_launch_count") val appLaunchCount: Int = 0,

    @ColumnInfo(name = "collected_at_ms") val collectedAtMs: Long = System.currentTimeMillis(),
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `UsageDailyEntity` (class) — lines 15-38
