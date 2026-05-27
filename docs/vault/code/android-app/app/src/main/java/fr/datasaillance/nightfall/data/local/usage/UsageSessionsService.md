---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageSessionsService.kt
git_blob: 89f088d4b64159050a37df4bb7f5b95601ced8c7
last_synced: '2026-05-27T00:40:51Z'
loc: 158
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageSessionsService.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageSessionsService.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageSessionsService.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.usage

import fr.datasaillance.nightfall.data.local.dao.UsageSessionDao
import fr.datasaillance.nightfall.data.local.entity.usage.UsageSessionEntity
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Session foreground reconstruite (non persistée directement). `durationMs` est
 * dérivé : `endMs - startMs`.
 */
data class RawSession(
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = endMs - startMs
}

/**
 * Reconstruit les sessions foreground à partir d'une séquence d'events triée
 * chronologiquement sur la fenêtre [fromMs, toMs] (algorithme DT-3).
 *
 * - ouverture (type 1) : ouvre une session ; un RESUMED orphelin (déjà ouvert)
 *   ferme implicitement la session précédente.
 * - fermeture par package (type 2 ou 23) : ferme la session du package ; un
 *   PAUSED orphelin (jamais ouvert) est ignoré silencieusement.
 * - fermeture globale (type 12 ou 14) : ferme toutes les sessions ouvertes
 *   (écran verrouillé / éteint).
 * - sessions ouvertes en fin de fenêtre : fermées à `toMs`.
 * - filtre : durée ≤ 0 ignorée.
 *
 * Fonction pure — aucune dépendance Android. Testable en JUnit pur.
 */
fun reconstructSessions(
    events: List<UsageEventRecord>,
    fromMs: Long,
    toMs: Long,
): List<RawSession> {
    val openSessions = LinkedHashMap<String, Long>() // packageName -> startMs
    val completed = ArrayList<RawSession>()

    val sorted = events.sortedBy { it.timeStampMs }
    for (e in sorted) {
        when (e.eventType) {
            EVENT_OPEN -> {
                val existing = openSessions[e.packageName]
                if (existing != null) {
                    // RESUMED orphelin sans PAUSED précédent — fermer l'ancienne session
                    completed += RawSession(e.packageName, existing, e.timeStampMs)
                }
                openSessions[e.packageName] = e.timeStampMs
            }

            EVENT_CLOSE_PAUSED, EVENT_CLOSE_STOPPED -> {
                val start = openSessions.remove(e.packageName)
                if (start != null) {
                    completed += RawSession(e.packageName, start, e.timeStampMs)
                }
                // PAUSED orphelin sans RESUMED précédent — ignoré silencieusement
            }

            EVENT_GLOBAL_KEYGUARD, EVENT_GLOBAL_SCREEN_OFF -> {
                for ((pkg, start) in openSessions) {
                    completed += RawSession(pkg, start, e.timeStampMs)
                }
                openSessions.clear()
            }
        }
    }

    // Sessions restantes ouvertes en fin de fenêtre → fermées à toMs
    for ((pkg, start) in openSessions) {
        completed += RawSession(pkg, start, toMs)
    }

    // Filtre : durée nulle ou négative ignorée
    return completed.filter { it.endMs > it.startMs }
}

/** Types d'events utilisés par la reconstruction (DT-2). */
private const val EVENT_OPEN = 1 // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
private const val EVENT_CLOSE_PAUSED = 2 // ACTIVITY_PAUSED / MOVE_TO_BACKGROUND
private const val EVENT_CLOSE_STOPPED = 23 // ACTIVITY_STOPPED
private const val EVENT_GLOBAL_KEYGUARD = 12 // KEYGUARD_SHOWN
private const val EVENT_GLOBAL_SCREEN_OFF = 14 // SCREEN_NON_INTERACTIVE

/**
 * Collecte et persiste les sessions foreground (table `usage_session`).
 *
 * - Idempotent : `OnConflictStrategy.REPLACE` sur `(package_name, start_ms)`.
 * - Aucune transmission réseau, tout en local (C1).
 * - Pas de gestion de permission ici — le caller (worker) vérifie
 *   `UsageStatsPermissionHelper.hasPermission()` avant.
 */
class UsageSessionsService(
    private val dao: UsageSessionDao,
    private val eventsSource: UsageEventsSource,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Long = System::currentTimeMillis,
) {

    /**
     * Collecte les sessions pour une journée calendaire `targetDate` (timezone
     * du device). Renvoie le nombre de sessions persistées.
     */
    suspend fun collectSessions(targetDate: LocalDate): Int {
        val fromMs = targetDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = targetDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val events = eventsSource.queryEvents(fromMs, toMs)
        val raw = reconstructSessions(events, fromMs, toMs)
        if (raw.isEmpty()) return 0

        val collectedAt = now()
        val rows = raw.map { s ->
            val date = Instant.ofEpochMilli(s.startMs).atZone(zone).toLocalDate().format(DATE_FMT)
            UsageSessionEntity(
                packageName = s.packageName,
                startMs = s.startMs,
                endMs = s.endMs,
                durationMs = s.endMs - s.startMs,
                date = date,
                source = "events",
                collectedAtMs = collectedAt,
            )
        }
        dao.upsert(rows)
        Timber.d("scope=usage_sessions date=$targetDate sessions=${rows.size}")
        return rows.size
    }

    /**
     * Backfill : tente de collecter les N jours précédents (+ aujourd'hui).
     * Best-effort — Android n'a que ~7-10 jours d'events en rétention. Les
     * erreurs par jour sont catchées et loggées sans interrompre les autres.
     * Renvoie le nombre total de sessions persistées.
     */
    suspend fun backfillSessions(days: Int): Int {
        val today = LocalDate.now(zone)
        var total = 0
        for (i in days downTo 0) {
            val day = today.minusDays(i.toLong())
            total += runCatching { collectSessions(day) }.getOrElse { e ->
                Timber.w("scope=usage_sessions_backfill day=$day error=${e::class.simpleName}")
                0
            }
        }
        return total
    }

    companion object {
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `RawSession` (class) — lines 15-21
- `reconstructSessions` (function) — lines 38-82
- `UsageSessionsService` (class) — lines 99-158
- `collectSessions` (function) — lines 110-134
- `backfillSessions` (function) — lines 142-153
