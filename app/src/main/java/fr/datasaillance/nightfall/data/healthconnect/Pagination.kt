package fr.datasaillance.nightfall.data.healthconnect

/** Une page de résultats Health Connect : les enregistrements, et le token de la page suivante (null ou vide = fin). */
data class RecordPage<T>(val records: List<T>, val nextPageToken: String?)

/**
 * Lit toutes les pages, dans l'ordre, jusqu'à ce que le token suivant soit vide.
 * Si Health Connect renvoie un token déjà vu, on s'arrête avec une erreur plutôt que de boucler à l'infini.
 */
suspend fun <T> readAllPages(fetchPage: suspend (pageToken: String?) -> RecordPage<T>): List<T> {
    val all = mutableListOf<T>()
    val seenTokens = mutableSetOf<String>()
    var token: String? = null
    while (true) {
        val page = fetchPage(token)
        all.addAll(page.records)
        val next = page.nextPageToken
        if (next.isNullOrEmpty()) {
            break
        }
        check(seenTokens.add(next)) { "Health Connect a renvoyé deux fois le même token de page ($next) : lecture interrompue" }
        token = next
    }
    return all
}
