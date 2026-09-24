package fr.datasaillance.nightfall.core.dedup

import fr.datasaillance.nightfall.core.model.SleepRecord
import java.time.Duration
import java.time.Instant

/** Un épisode de sommeil pour l'analyse : une ou plusieurs sessions réunies en un seul intervalle. */
data class SleepEpisode(
    val start: Instant,
    val end: Instant,
    val memberIds: List<String>,
)

/** Quelles sessions réunir en un seul épisode d'analyse. */
enum class EpisodeRule {
    /** Règle darkhour : seulement les sessions qui se chevauchent. */
    OVERLAP_ONLY,

    /** Règle Nightfall : aussi les sessions bout à bout (corrections manuelles après une panne de montre). */
    OVERLAP_OR_TOUCH,
}

/**
 * Dédup des sessions de sommeil, port du notebook `notebooks/01_dedup_chevauchements.ipynb`.
 * Les deux vues sont validées par les golden fixtures de `core/src/test/resources/fixtures/dedup/`.
 */
object SleepDeduplication {

    const val DUPLICATE_RATIO = 0.8

    /** Épisodes d'analyse (notebook, sections 2.4 et 5) : un groupe de sessions liées devient un seul intervalle. */
    fun analysisEpisodes(records: List<SleepRecord>, rule: EpisodeRule = EpisodeRule.OVERLAP_OR_TOUCH): List<SleepEpisode> {
        val links = overlappingPairs(records).map { it.first to it.second }.toMutableList()
        if (rule == EpisodeRule.OVERLAP_OR_TOUCH) {
            links += touchingPairs(records)
        }

        return group(records, links).values
            .map { members ->
                SleepEpisode(
                    start = members.minOf { it.start },
                    end = members.maxOf { it.end },
                    memberIds = members.map { it.id }.sorted(),
                )
            }
            .sortedBy { it.start }
    }

    /**
     * Sessions affichées (notebook, section 2.4) : seuls les doublons (chevauchement ≥ [duplicateRatio]
     * de la plus courte) sont réunis, et on garde la session la plus riche de chaque groupe :
     * le plus de temps couvert par des stades, puis la plus longue, puis la plus récemment modifiée.
     */
    fun displayedSessions(records: List<SleepRecord>, duplicateRatio: Double = DUPLICATE_RATIO): List<SleepRecord> {
        val duplicates = overlappingPairs(records)
            .filter { it.shareOfShorter >= duplicateRatio }
            .map { it.first to it.second }

        val richestFirst = compareByDescending<SleepRecord> { stageCoverage(it) }
            .thenByDescending { Duration.between(it.start, it.end) }
            .thenByDescending { it.lastModified }

        return group(records, duplicates).values
            .map { members -> members.sortedWith(richestFirst).first() }
            .sortedBy { it.start }
    }

    // ------------------------------------------------------------------ briques du calcul

    /** Une paire de sessions qui se chevauchent, et la part du chevauchement dans la plus courte (entre 0 et 1). */
    private class OverlappingPair(val first: String, val second: String, val shareOfShorter: Double)

    /** Durée commune de deux intervalles, zéro s'ils sont disjoints ou bout à bout (notebook 2.1). */
    private fun overlap(a: SleepRecord, b: SleepRecord): Duration {
        val common = Duration.between(maxOf(a.start, b.start), minOf(a.end, b.end))
        return if (common.isNegative) Duration.ZERO else common
    }

    /**
     * Toutes les paires qui se chevauchent (notebook 2.2). Les sessions sont triées par début : pour chacune,
     * on ne regarde les suivantes que tant qu'elles commencent avant sa fin.
     */
    private fun overlappingPairs(records: List<SleepRecord>): List<OverlappingPair> {
        val sorted = records.sortedBy { it.start }
        val pairs = mutableListOf<OverlappingPair>()

        for (i in sorted.indices) {
            val a = sorted[i]
            for (j in i + 1 until sorted.size) {
                val b = sorted[j]
                if (b.start >= a.end) break

                val common = overlap(a, b)
                if (common.isZero) continue

                val shorter = minOf(Duration.between(a.start, a.end), Duration.between(b.start, b.end))
                pairs += OverlappingPair(a.id, b.id, common.toMillis().toDouble() / shorter.toMillis())
            }
        }
        return pairs
    }

    /** Les paires où la seconde session commence exactement à la fin de la première (notebook 5). */
    private fun touchingPairs(records: List<SleepRecord>): List<Pair<String, String>> {
        val sorted = records.sortedBy { it.start }
        val pairs = mutableListOf<Pair<String, String>>()

        for (i in sorted.indices) {
            val a = sorted[i]
            for (j in i + 1 until sorted.size) {
                val b = sorted[j]
                if (b.start > a.end) break
                if (b.start == a.end) pairs += a.id to b.id
            }
        }
        return pairs
    }

    /**
     * Regroupe les sessions liées (notebook 2.3) : chaque session démarre seule dans son groupe,
     * et à chaque paire on verse le groupe de la seconde dans celui de la première.
     * Retourne les groupes, chacun avec ses sessions.
     */
    private fun group(records: List<SleepRecord>, links: List<Pair<String, String>>): Map<String, List<SleepRecord>> {
        val groupOf = records.associate { it.id to it.id }.toMutableMap()

        for ((first, second) in links) {
            val groupFirst = groupOf.getValue(first)
            val groupSecond = groupOf.getValue(second)
            if (groupFirst == groupSecond) continue
            for ((id, g) in groupOf) {
                if (g == groupSecond) groupOf[id] = groupFirst
            }
        }
        return records.groupBy { groupOf.getValue(it.id) }
    }

    /** Temps couvert par les stades d'une session. */
    private fun stageCoverage(record: SleepRecord): Duration =
        record.stages.fold(Duration.ZERO) { total, stage -> total + Duration.between(stage.start, stage.end) }
}
