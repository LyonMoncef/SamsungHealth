---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsCollectionWorker.kt
git_blob: 809731765c53e43c333bdc2e352e09ab27b081a5
last_synced: '2026-05-27T00:40:51Z'
loc: 92
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsCollectionWorker.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsCollectionWorker.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsCollectionWorker.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId

/**
 * Worker quotidien Phase B_us : collecte les UsageStats de la veille (J-1) et
 * les persiste dans Room. Aucun appel réseau. Skip silencieux si la permission
 * `PACKAGE_USAGE_STATS` n'est pas accordée (rien à faire — l'utilisateur la
 * réactivera depuis l'écran Bien-être numérique).
 *
 * Idempotent : `UsageStatsDao.upsertDaily` écrase les rows de la même journée.
 * Donc un re-run (manuel ou automatique) écrase les valeurs avec les dernières
 * mesures Android (qui peuvent s'affiner après quelques heures).
 */
class UsageStatsCollectionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val perm = UsageStatsPermissionHelper(ctx)
        if (!perm.hasPermission()) {
            Timber.i("scope=usage_worker permission=missing skip")
            return Result.success()
        }

        val mgr = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (mgr == null) {
            Timber.w("scope=usage_worker manager=null")
            return Result.success()
        }

        val database = NightfallDatabase.get(ctx)
        val service = LocalUsageStatsService(
            dao = database.usageStatsDao(),
            source = AndroidUsageStatsSource(mgr),
            zone = ZoneId.systemDefault(),
        )
        val sessionDao = database.usageSessionDao()
        val sessionsService = UsageSessionsService(
            dao = sessionDao,
            eventsSource = AndroidUsageEventsSource(mgr),
            zone = ZoneId.systemDefault(),
        )

        val target = inputData.getString(KEY_TARGET_DATE)?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: LocalDate.now().minusDays(1)

        // 1. Daily stats (inchangé) — un échec ici déclenche un retry.
        val dailyResult = runCatching {
            val rows = service.collectDailyStats(target)
            Timber.i("scope=usage_worker date=$target rows=$rows")
            Result.success()
        }.getOrElse { e ->
            Timber.w("scope=usage_worker error=${e::class.simpleName} msg=${e.message}")
            Result.retry()
        }

        // 2. Sessions (nouveau) — échec indépendant : ne doit pas faire échouer
        //    la collecte daily. On backfill si la table est encore vide.
        runCatching {
            if (sessionDao.count() == 0) {
                val backfilled = sessionsService.backfillSessions(days = BACKFILL_DAYS)
                Timber.i("scope=usage_sessions_worker backfill=$backfilled")
            } else {
                val sessions = sessionsService.collectSessions(target)
                Timber.i("scope=usage_sessions_worker date=$target sessions=$sessions")
            }
        }.onFailure { e ->
            Timber.w("scope=usage_sessions_worker error=${e::class.simpleName} msg=${e.message}")
        }

        return dailyResult
    }

    companion object {
        /** Optionnel — si présent, override la date cible (sinon = veille). */
        const val KEY_TARGET_DATE = "target_date"

        /** Nb de jours backfillés au premier lancement (rétention Android ~7-10j). */
        private const val BACKFILL_DAYS = 10
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `UsageStatsCollectionWorker` (class) — lines 22-92
- `doWork` (function) — lines 27-83
