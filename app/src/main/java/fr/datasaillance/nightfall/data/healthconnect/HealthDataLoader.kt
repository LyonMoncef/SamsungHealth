package fr.datasaillance.nightfall.data.healthconnect

import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.HourlySteps
import kotlinx.coroutines.CancellationException
import java.time.Instant

/** Ce qui fournit les données, déjà converties vers le contrat. En test : une fausse source. */
interface HealthRecordsSource {
    /** Une page de sessions de sommeil (enregistrements bruts). */
    suspend fun sleepPage(pageToken: String?): RecordPage<SleepRecord>

    /** Tous les totaux de pas horaires, calculés par Health Connect (contrat v2). */
    suspend fun hourlySteps(): List<HourlySteps>
}

/**
 * Tout ce qu'on a lu, plus la profondeur d'historique réellement accessible (à afficher si limitée).
 * Si la lecture des pas a échoué, [steps] est vide et [stepsError] dit pourquoi : le sommeil reste utilisable.
 */
data class HealthData(
    val sleep: List<SleepRecord>,
    val steps: List<HourlySteps>,
    val historyAccess: HistoryAccess,
    val stepsError: String? = null,
)

/**
 * Erreur de lecture Health Connect, avec l'étape en cause et le message d'origine,
 * pour qu'un échec sur le téléphone soit diagnosticable sans deviner.
 */
class HealthReadException(val step: String, cause: Throwable) :
    Exception("Échec pendant « $step » : ${cause::class.simpleName}${cause.message?.let { " — $it" } ?: ""}", cause)

/**
 * Lit toutes les pages de sommeil (dédup par identifiant, tri par début puis id) et les pas horaires (tri par début).
 * Le sommeil est indispensable : son échec remonte. Les pas ne le sont pas : leur échec est consigné dans
 * `stepsError` (jamais silencieux) et le sommeil reste disponible.
 */
suspend fun loadHealthData(source: HealthRecordsSource, historyAccess: HistoryAccess): HealthData {
    val sleep = readStep("lecture du sommeil") { readAllPages { token -> source.sleepPage(token) } }
    var steps: List<HourlySteps> = emptyList()
    var stepsError: String? = null
    try {
        steps = readStep("lecture des pas") { source.hourlySteps() }.sortedBy { it.start }
    } catch (error: HealthReadException) {
        stepsError = error.message
    }
    return HealthData(
        sleep = keepLatestById(sleep, { it.id }, { it.lastModified }).sortedWith(compareBy({ it.start }, { it.id })),
        steps = steps,
        historyAccess = historyAccess,
        stepsError = stepsError,
    )
}

/** Exécute une étape de lecture ; toute erreur remonte étiquetée avec le nom de l'étape (l'annulation, elle, passe telle quelle). */
private suspend fun <T> readStep(step: String, block: suspend () -> T): T {
    try {
        return block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        throw HealthReadException(step, error)
    }
}

/**
 * Ne lit rien tant que Health Connect n'est pas prêt (spec TA-12) : renvoie null dans ce cas.
 * La source n'est créée qu'une fois l'état vérifié (créer le client Health Connect échoue s'il est absent).
 */
suspend fun loadIfReady(state: HealthConnectState, newSource: () -> HealthRecordsSource): HealthData? {
    if (state !is HealthConnectState.Ready) {
        return null
    }
    return loadHealthData(newSource(), state.historyAccess)
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
