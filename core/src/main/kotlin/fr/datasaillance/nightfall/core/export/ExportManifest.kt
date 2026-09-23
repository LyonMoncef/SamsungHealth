package fr.datasaillance.nightfall.core.export

import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.StepsInterval
import java.time.Instant

const val CONTRACT_VERSION = "1"

/** Contenu de `manifest.json` (spec DT-4). */
data class ExportManifest(
    val contractVersion: String,
    val exportedAt: Instant,
    val appVersion: String,
    val sleepSessionsCount: Int,
    val sleepStagesCount: Int,
    val stepsCount: Int,
    val historyAccess: HistoryAccess,
    val oldestSleepStart: Instant?,
) {
    /** JSON écrit à la main (format fixe, clés toujours dans le même ordre) : pas de bibliothèque dans `core/`. */
    fun toJson(): String {
        val oldest = if (oldestSleepStart == null) "null" else jsonString(oldestSleepStart.toString())
        val lines = listOf(
            "{",
            "  \"contract_version\": ${jsonString(contractVersion)},",
            "  \"exported_at\": ${jsonString(exportedAt.toString())},",
            "  \"app_version\": ${jsonString(appVersion)},",
            "  \"sleep_sessions_count\": $sleepSessionsCount,",
            "  \"sleep_stages_count\": $sleepStagesCount,",
            "  \"steps_count\": $stepsCount,",
            "  \"history_access\": ${jsonString(historyAccess.name)},",
            "  \"oldest_sleep_start\": $oldest",
            "}",
        )
        return lines.joinToString("\n") + "\n"
    }
}

fun buildExportManifest(
    sleepRecords: List<SleepRecord>,
    steps: List<StepsInterval>,
    exportedAt: Instant,
    appVersion: String,
    historyAccess: HistoryAccess,
): ExportManifest {
    var stagesCount = 0
    for (record in sleepRecords) {
        stagesCount += record.stages.size
    }
    return ExportManifest(
        contractVersion = CONTRACT_VERSION,
        exportedAt = exportedAt,
        appVersion = appVersion,
        sleepSessionsCount = sleepRecords.size,
        sleepStagesCount = stagesCount,
        stepsCount = steps.size,
        historyAccess = historyAccess,
        oldestSleepStart = sleepRecords.minOfOrNull { it.start },
    )
}

/** Chaîne JSON entre guillemets, avec échappement des guillemets, antislashs et caractères de contrôle. */
private fun jsonString(value: String): String {
    val builder = StringBuilder("\"")
    for (c in value) {
        when {
            c == '"' -> builder.append("\\\"")
            c == '\\' -> builder.append("\\\\")
            c == '\n' -> builder.append("\\n")
            c < ' ' -> builder.append(String.format("\\u%04x", c.code))
            else -> builder.append(c)
        }
    }
    return builder.append('"').toString()
}
