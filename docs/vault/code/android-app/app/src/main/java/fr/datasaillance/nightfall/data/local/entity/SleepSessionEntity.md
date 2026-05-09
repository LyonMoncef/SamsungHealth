---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/SleepSessionEntity.kt
git_blob: 2eb6c8961cd647f6b5c676095de1a0d405fef53e
last_synced: '2026-05-09T15:08:38Z'
loc: 36
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/SleepSessionEntity.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/SleepSessionEntity.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/SleepSessionEntity.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Une session de sommeil locale. Miroir de `server.db.models.SleepSession`.
 *
 * Différences avec le serveur :
 * - pas de `user_id` (mono-utilisateur sur le device)
 * - timestamps en epoch millis UTC (vs DateTime tz-aware côté Postgres)
 * - score / efficiency / etc. en clair — chiffrement assuré au niveau du fichier DB par SQLCipher
 */
@Entity(
    tableName = "sleep_sessions",
    indices = [
        Index("sleep_start"),
        Index(value = ["sleep_start", "sleep_end"], unique = true),
    ],
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "sleep_start") val sleepStartMs: Long,
    @ColumnInfo(name = "sleep_end") val sleepEndMs: Long,
    @ColumnInfo(name = "sleep_score") val sleepScore: Int? = null,
    @ColumnInfo(name = "efficiency") val efficiency: Float? = null,
    @ColumnInfo(name = "sleep_duration_min") val sleepDurationMin: Int? = null,
    @ColumnInfo(name = "sleep_cycle") val sleepCycle: Int? = null,
    @ColumnInfo(name = "mental_recovery") val mentalRecovery: Float? = null,
    @ColumnInfo(name = "physical_recovery") val physicalRecovery: Float? = null,
    @ColumnInfo(name = "sleep_type") val sleepType: Int? = null,
    @ColumnInfo(name = "created_at") val createdAtMs: Long = System.currentTimeMillis(),
)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepSessionEntity` (class) — lines 16-36
