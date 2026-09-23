package fr.datasaillance.nightfall.data.healthconnect

import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.StepsInterval
import java.time.Instant

/** Ce qui fournit les pages de données, déjà converties vers le contrat. En test : une fausse source. */
interface HealthRecordsSource {
    suspend fun sleepPage(pageToken: String?): RecordPage<SleepRecord>
    suspend fun stepsPage(pageToken: String?): RecordPage<StepsInterval>
}

/** Tout ce qu'on a lu, plus la profondeur d'historique réellement accessible (à afficher si limitée). */
data class HealthData(
    val sleep: List<SleepRecord>,
    val steps: List<StepsInterval>,
    val historyAccess: HistoryAccess,
)

/** Lit toutes les pages de sommeil et de pas, retire les doublons d'identifiant, trie par début puis id. */
suspend fun loadHealthData(source: HealthRecordsSource, historyAccess: HistoryAccess): HealthData {
    val sleep = readAllPages { token -> source.sleepPage(token) }
    val steps = readAllPages { token -> source.stepsPage(token) }
    return HealthData(
        sleep = keepLatestById(sleep, { it.id }, { it.lastModified }).sortedWith(compareBy({ it.start }, { it.id })),
        steps = keepLatestById(steps, { it.id }, { it.lastModified }).sortedWith(compareBy({ it.start }, { it.id })),
        historyAccess = historyAccess,
    )
}

/** Ne lit rien tant que Health Connect n'est pas prêt (spec TA-12) : renvoie null dans ce cas. */
suspend fun loadIfReady(state: HealthConnectState, source: HealthRecordsSource): HealthData? {
    if (state !is HealthConnectState.Ready) {
        return null
    }
    return loadHealthData(source, state.historyAccess)
}

/**
 * Dédup technique (spec DT-3) : un même identifiant vu plusieurs fois pendant la lecture
 * n'est gardé qu'une fois, dans sa version la plus récemment modifiée.
 */
private fun <T> keepLatestById(items: List<T>, idOf: (T) -> String, lastModifiedOf: (T) -> Instant): List<T> {
    val byId = LinkedHashMap<String, T>()
    for (item in items) {
        val id = idOf(item)
        val existing = byId[id]
        if (existing == null || lastModifiedOf(item) > lastModifiedOf(existing)) {
            byId[id] = item
        }
    }
    return byId.values.toList()
}
