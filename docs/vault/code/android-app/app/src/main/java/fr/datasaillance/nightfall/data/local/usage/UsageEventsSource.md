---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageEventsSource.kt
git_blob: ab0f5878516379f8a356b593cf832e6f8d89a008
last_synced: '2026-05-27T00:40:51Z'
loc: 57
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageEventsSource.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageEventsSource.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageEventsSource.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager

/**
 * Source abstraite des events bruts de foreground — permet d'injecter un mock
 * en test sans avoir à instancier le `Context` Android complet.
 *
 * Symétrique de [UsageStatsSource], mais pour les events fins de
 * `UsageStatsManager.queryEvents()` (DT-1 / spec usage-sessions).
 */
interface UsageEventsSource {
    /**
     * Renvoie les events bruts sur la fenêtre [fromMs, toMs].
     * Les events sont retournés dans l'ordre chronologique ascendant.
     */
    fun queryEvents(fromMs: Long, toMs: Long): List<UsageEventRecord>
}

/**
 * DTO interne immutable — découple le service de `android.app.usage.UsageEvents.Event`
 * pour permettre l'injection de fixtures en test sans Context Android.
 *
 * `eventType` : constantes `UsageEvents.Event.*` (voir DT-2).
 */
data class UsageEventRecord(
    val packageName: String,
    val eventType: Int,
    val timeStampMs: Long,
)

/**
 * Default impl qui delegate au `UsageStatsManager`. En test, on injecte un
 * `UsageEventsSource` qui retourne des fixtures (pas de Context Android).
 *
 * Itère via `UsageEvents.hasNextEvent()` / `getNextEvent()` et mappe chaque
 * `UsageEvents.Event` vers le DTO [UsageEventRecord].
 */
class AndroidUsageEventsSource(private val mgr: UsageStatsManager) : UsageEventsSource {
    override fun queryEvents(fromMs: Long, toMs: Long): List<UsageEventRecord> {
        val events: UsageEvents = mgr.queryEvents(fromMs, toMs) ?: return emptyList()
        val out = ArrayList<UsageEventRecord>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: ""
            out += UsageEventRecord(
                packageName = pkg,
                eventType = event.eventType,
                timeStampMs = event.timeStamp,
            )
        }
        out.sortBy { it.timeStampMs }
        return out
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `UsageEventsSource` (class) — lines 13-19
- `queryEvents` (function) — lines 18-18
- `UsageEventRecord` (class) — lines 27-31
- `AndroidUsageEventsSource` (class) — lines 40-57
- `queryEvents` (function) — lines 41-56
