package fr.datasaillance.nightfall.core.export

import fr.datasaillance.nightfall.core.model.RecordingMethod
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.SleepStage
import fr.datasaillance.nightfall.core.model.StageType
import fr.datasaillance.nightfall.core.model.StepsInterval
import java.time.Instant
import java.time.ZoneOffset

/**
 * Écriture et relecture du format d'export CSV v1 (spec 2026-09-23-phase1-data-foundation, DT-4).
 *
 * Règles du format :
 * - une ligne d'en-tête, puis une ligne par enregistrement, chaque ligne terminée par "\n" ;
 * - instants en ISO-8601 UTC ("2024-07-08T22:31:00Z"), offsets au format "+02:00" ("Z" pour UTC), vide si inconnu ;
 * - énumérations écrites par leur nom ("LIGHT") ;
 * - lignes triées (sessions et pas : par début puis id ; stades : par session puis début),
 *   pour que les mêmes données donnent toujours exactement le même fichier.
 */
object CsvExport {

    private const val SESSIONS_HEADER =
        "id,start_utc,end_utc,start_offset,end_offset,source,recording_method,last_modified_utc"
    private const val STAGES_HEADER =
        "session_id,start_utc,end_utc,stage"
    private const val STEPS_HEADER =
        "id,start_utc,end_utc,start_offset,end_offset,count,source,recording_method,last_modified_utc"

    // ------------------------------------------------------------------ écriture

    fun writeSleepSessions(records: List<SleepRecord>): String {
        val sorted = records.sortedWith(compareBy({ it.start }, { it.id }))
        val lines = mutableListOf(SESSIONS_HEADER)
        for (record in sorted) {
            lines.add(
                csvLine(
                    record.id,
                    record.start.toString(),
                    record.end.toString(),
                    offsetToText(record.startOffset),
                    offsetToText(record.endOffset),
                    record.source,
                    record.recordingMethod.name,
                    record.lastModified.toString(),
                ),
            )
        }
        return joinLines(lines)
    }

    fun writeSleepStages(records: List<SleepRecord>): String {
        // On aplatit tous les stades avec l'id de leur session, puis on trie.
        val rows = mutableListOf<Pair<String, SleepStage>>()
        for (record in records) {
            for (stage in record.stages) {
                rows.add(record.id to stage)
            }
        }
        val sorted = rows.sortedWith(
            compareBy(
                { it.first },
                { it.second.start },
                { it.second.end },
                { it.second.type.name },
            ),
        )
        val lines = mutableListOf(STAGES_HEADER)
        for ((sessionId, stage) in sorted) {
            lines.add(csvLine(sessionId, stage.start.toString(), stage.end.toString(), stage.type.name))
        }
        return joinLines(lines)
    }

    fun writeSteps(intervals: List<StepsInterval>): String {
        val sorted = intervals.sortedWith(compareBy({ it.start }, { it.id }))
        val lines = mutableListOf(STEPS_HEADER)
        for (interval in sorted) {
            lines.add(
                csvLine(
                    interval.id,
                    interval.start.toString(),
                    interval.end.toString(),
                    offsetToText(interval.startOffset),
                    offsetToText(interval.endOffset),
                    interval.count.toString(),
                    interval.source,
                    interval.recordingMethod.name,
                    interval.lastModified.toString(),
                ),
            )
        }
        return joinLines(lines)
    }

    // ------------------------------------------------------------------ relecture

    fun readSleepRecords(sessionsCsv: String, stagesCsv: String): List<SleepRecord> {
        // 1. Les stades, regroupés par id de session.
        val stagesBySession = mutableMapOf<String, MutableList<SleepStage>>()
        for (fields in dataRows(stagesCsv, STAGES_HEADER)) {
            val sessionId = fields[0]
            val stage = SleepStage(
                start = Instant.parse(fields[1]),
                end = Instant.parse(fields[2]),
                type = StageType.valueOf(fields[3]),
            )
            stagesBySession.getOrPut(sessionId) { mutableListOf() }.add(stage)
        }

        // 2. Les sessions, auxquelles on rattache leurs stades.
        val records = mutableListOf<SleepRecord>()
        for (fields in dataRows(sessionsCsv, SESSIONS_HEADER)) {
            val id = fields[0]
            val stages = stagesBySession.remove(id) ?: mutableListOf()
            records.add(
                SleepRecord(
                    id = id,
                    start = Instant.parse(fields[1]),
                    end = Instant.parse(fields[2]),
                    startOffset = textToOffset(fields[3]),
                    endOffset = textToOffset(fields[4]),
                    source = fields[5],
                    recordingMethod = RecordingMethod.valueOf(fields[6]),
                    lastModified = Instant.parse(fields[7]),
                    stages = stages.sortedWith(compareBy({ it.start }, { it.end }, { it.type.name })),
                ),
            )
        }

        // 3. Un stade dont la session n'existe pas signale un fichier corrompu.
        if (stagesBySession.isNotEmpty()) {
            error("Stades rattachés à des sessions absentes : ${stagesBySession.keys.sorted()}")
        }
        return records.sortedWith(compareBy({ it.start }, { it.id }))
    }

    fun readSteps(stepsCsv: String): List<StepsInterval> {
        val intervals = mutableListOf<StepsInterval>()
        for (fields in dataRows(stepsCsv, STEPS_HEADER)) {
            intervals.add(
                StepsInterval(
                    id = fields[0],
                    start = Instant.parse(fields[1]),
                    end = Instant.parse(fields[2]),
                    startOffset = textToOffset(fields[3]),
                    endOffset = textToOffset(fields[4]),
                    count = fields[5].toLong(),
                    source = fields[6],
                    recordingMethod = RecordingMethod.valueOf(fields[7]),
                    lastModified = Instant.parse(fields[8]),
                ),
            )
        }
        return intervals.sortedWith(compareBy({ it.start }, { it.id }))
    }

    // ------------------------------------------------------------------ petits outils

    private fun offsetToText(offset: ZoneOffset?): String = offset?.id ?: ""

    private fun textToOffset(text: String): ZoneOffset? = if (text.isEmpty()) null else ZoneOffset.of(text)

    private fun joinLines(lines: List<String>): String {
        val builder = StringBuilder()
        for (line in lines) {
            builder.append(line).append('\n')
        }
        return builder.toString()
    }

    private fun csvLine(vararg values: String): String = values.joinToString(",") { csvField(it) }

    /** Entoure le champ de guillemets (et double ses guillemets) seulement s'il en a besoin (RFC 4180). */
    private fun csvField(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }

    /** Lit un fichier CSV, vérifie son en-tête et renvoie les lignes de données (sans l'en-tête). */
    private fun dataRows(text: String, expectedHeader: String): List<List<String>> {
        val rows = parseCsv(text)
        val header = rows.firstOrNull()?.joinToString(",")
        require(header == expectedHeader) { "En-tête inattendu : '$header' (attendu : '$expectedHeader')" }
        return rows.drop(1)
    }

    /**
     * Découpe un texte CSV en lignes et en champs, caractère par caractère.
     * Entre guillemets, la virgule et le saut de ligne font partie du champ,
     * et un guillemet doublé ("") représente un guillemet.
     */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (insideQuotes) {
                if (c == '"') {
                    val nextIsQuote = i + 1 < text.length && text[i + 1] == '"'
                    if (nextIsQuote) {
                        field.append('"')
                        i++ // on saute le second guillemet
                    } else {
                        insideQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> insideQuotes = true
                    ',' -> {
                        fields.add(field.toString())
                        field.clear()
                    }
                    '\n' -> {
                        fields.add(field.toString())
                        field.clear()
                        rows.add(fields)
                        fields = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        // Dernière ligne si le fichier ne se termine pas par "\n".
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(field.toString())
            rows.add(fields)
        }
        return rows
    }
}
