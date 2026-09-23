package fr.datasaillance.nightfall.data.local.usage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.os.Build
import fr.datasaillance.nightfall.data.local.dao.UsageStatsDao
import fr.datasaillance.nightfall.data.local.entity.usage.UsageDailyEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Source abstraite des UsageStats — permet d'injecter un mock en test
 * sans avoir à instancier le `Context` Android complet.
 */
interface UsageStatsSource {
    /** Renvoie la liste de stats agrégées sur la fenêtre [fromMs, toMs]. */
    fun queryUsageStats(intervalType: Int, fromMs: Long, toMs: Long): List<UsageStats>
}

/**
 * Default impl qui delegate au `UsageStatsManager` Android. Dans un test,
 * on injecte un `UsageStatsSource` qui retourne des fixtures.
 */
class AndroidUsageStatsSource(private val mgr: UsageStatsManager) : UsageStatsSource {
    override fun queryUsageStats(intervalType: Int, fromMs: Long, toMs: Long): List<UsageStats> =
        mgr.queryUsageStats(intervalType, fromMs, toMs) ?: emptyList()
}

/**
 * Collecte les UsageStats journalières et les persiste dans Room (table `usage_daily`).
 *
 * - Idempotent : `OnConflictStrategy.REPLACE` sur `(date, package_name)` — un
 *   re-run pour la même date écrase les valeurs (Android peut affiner après coup).
 * - Aucune transmission réseau, tout en local (cf. spec local-first).
 * - Pas de gestion de permission ici — le caller (UI / WorkManager) doit
 *   vérifier `UsageStatsPermissionHelper.hasPermission()` avant.
 */
class LocalUsageStatsService(
    private val dao: UsageStatsDao,
    private val source: UsageStatsSource,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Long = System::currentTimeMillis,
) {

    /**
     * Collecte les stats pour une journée calendaire `targetDate` (timezone du device).
     * Renvoie le nombre de packages écrits.
     */
    suspend fun collectDailyStats(targetDate: LocalDate): Int {
        val start = targetDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = targetDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val raw = source.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

        // L'API peut retourner plusieurs entrées par package (rotation des buckets) :
        // on agrège par package en sommant.
        val byPkg = HashMap<String, UsageDailyEntity>()
        val collectedAt = now()
        val dateStr = targetDate.format(DATE_FMT)
        for (us in raw) {
            val pkg = us.packageName ?: continue
            // appLaunchCount n'est PAS exposé par l'API publique UsageStats — c'est
            // un champ package-private (`mAppLaunchCount` dans AOSP). On laisse
            // 0 ici ; Phase B_us l'extraira en comptant les ACTIVITY_RESUMED via
            // UsageStatsManager.queryEvents().
            val launches = 0
            val visible = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { us.totalTimeVisible }.getOrDefault(0L)
            } else 0L
            val fgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching { us.totalTimeForegroundServiceUsed }.getOrDefault(0L)
            } else 0L

            val existing = byPkg[pkg]
            byPkg[pkg] = if (existing == null) {
                UsageDailyEntity(
                    date = dateStr,
                    packageName = pkg,
                    totalTimeForegroundMs = us.totalTimeInForeground,
                    totalTimeVisibleMs = visible,
                    totalTimeForegroundServiceMs = fgs,
                    lastTimeUsedMs = us.lastTimeUsed,
                    appLaunchCount = launches,
                    collectedAtMs = collectedAt,
                )
            } else {
                existing.copy(
                    totalTimeForegroundMs = existing.totalTimeForegroundMs + us.totalTimeInForeground,
                    totalTimeVisibleMs = existing.totalTimeVisibleMs + visible,
                    totalTimeForegroundServiceMs = existing.totalTimeForegroundServiceMs + fgs,
                    lastTimeUsedMs = maxOf(existing.lastTimeUsedMs, us.lastTimeUsed),
                    appLaunchCount = existing.appLaunchCount + launches,
                )
            }
        }
        // Filtre : on ignore les packages totalement inactifs (0ms, 0 launches)
        val rows = byPkg.values.filter { it.totalTimeForegroundMs > 0 || it.appLaunchCount > 0 }
        if (rows.isEmpty()) return 0
        dao.upsertDaily(rows)
        return rows.size
    }

    /**
     * Backfill : tente de collecter les N jours précédents. Android renvoie ce
     * qu'il a en rétention (typiquement 7-10 jours en daily). Idempotent.
     */
    suspend fun backfillDays(days: Int): Int {
        val today = LocalDate.now(zone)
        var totalRows = 0
        for (i in days downTo 1) {
            totalRows += collectDailyStats(today.minusDays(i.toLong()))
        }
        // Inclut aussi la journée en cours
        totalRows += collectDailyStats(today)
        return totalRows
    }

    companion object {
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
