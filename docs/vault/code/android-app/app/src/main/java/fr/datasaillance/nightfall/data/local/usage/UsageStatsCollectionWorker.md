---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsCollectionWorker.kt
git_blob: ff4aa1ded17d9ca3306842f8f1cb709cd6926f37
last_synced: '2026-05-20T18:28:21Z'
loc: 65
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

        val service = LocalUsageStatsService(
            dao = NightfallDatabase.get(ctx).usageStatsDao(),
            source = AndroidUsageStatsSource(mgr),
            zone = ZoneId.systemDefault(),
        )

        val target = inputData.getString(KEY_TARGET_DATE)?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: LocalDate.now().minusDays(1)

        return runCatching {
            val rows = service.collectDailyStats(target)
            Timber.i("scope=usage_worker date=$target rows=$rows")
            Result.success()
        }.getOrElse { e ->
            Timber.w("scope=usage_worker error=${e::class.simpleName} msg=${e.message}")
            Result.retry()
        }
    }

    companion object {
        /** Optionnel — si présent, override la date cible (sinon = veille). */
        const val KEY_TARGET_DATE = "target_date"
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `UsageStatsCollectionWorker` (class) — lines 22-65
- `doWork` (function) — lines 27-59
