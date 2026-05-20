---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsScheduler.kt
git_blob: 63b38542323949ce25ea9d7dd058415ed70fd3a0
last_synced: '2026-05-20T18:28:21Z'
loc: 92
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsScheduler.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsScheduler.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsScheduler.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.usage

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Planification du worker UsageStats. Deux modes :
 * - **Périodique** : 1× par 24 h (intervalle min imposé par Android), `KEEP` pour
 *   ne pas écraser une instance déjà en file.
 * - **One-shot** : déclenché par l'UI ("Collecter maintenant", "Backfill N jours").
 *
 * Pas de constraint réseau (collecte locale pure). Pas de `setRequiresCharging`
 * (collecte légère, on accepte de tourner sur batterie).
 */
object UsageStatsScheduler {

    private const val PERIODIC_WORK_NAME = "usage_stats_periodic_collection"
    private const val ONESHOT_WORK_NAME = "usage_stats_oneshot_collection"

    /**
     * Enqueue un PeriodicWorkRequest 1×/jour. Idempotent : si une instance
     * existe déjà avec le même `uniqueWorkName`, garde l'ancienne (`KEEP`).
     */
    fun schedulePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<UsageStatsCollectionWorker>(
            repeatInterval = 24, repeatIntervalTimeUnit = TimeUnit.HOURS,
        ).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
        Timber.d("scope=usage_scheduler periodic=enqueued")
    }

    /** Déclenche une collecte immédiate pour la date donnée (défaut = veille). */
    fun runOnce(context: Context, target: LocalDate? = null) {
        val data = target?.let {
            Data.Builder().putString(UsageStatsCollectionWorker.KEY_TARGET_DATE, it.toString()).build()
        } ?: Data.EMPTY
        val request = OneTimeWorkRequestBuilder<UsageStatsCollectionWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONESHOT_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        Timber.d("scope=usage_scheduler oneshot=enqueued target=$target")
    }

    /**
     * Backfill N jours : enqueue 1 worker par jour pour les N derniers jours
     * inclus aujourd'hui. Tous chaînés sous le même `uniqueWorkName` côté one-shot.
     */
    fun backfill(context: Context, days: Int) {
        val today = LocalDate.now()
        val requests = (days downTo 0).map { offset ->
            val target = today.minusDays(offset.toLong())
            val data = Data.Builder()
                .putString(UsageStatsCollectionWorker.KEY_TARGET_DATE, target.toString())
                .build()
            OneTimeWorkRequestBuilder<UsageStatsCollectionWorker>()
                .setInputData(data)
                .build()
        }
        if (requests.isEmpty()) return
        WorkManager.getInstance(context)
            .beginUniqueWork(
                ONESHOT_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                requests.first(),
            )
            .let { chain ->
                requests.drop(1).fold(chain) { acc, req -> acc.then(req) }
            }
            .enqueue()
        Timber.d("scope=usage_scheduler backfill=${days}d enqueued")
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `schedulePeriodic` (function) — lines 33-46
- `runOnce` (function) — lines 49-62
- `backfill` (function) — lines 68-91
