package fr.datasaillance.nightfall.data.local.location

import fr.datasaillance.nightfall.data.local.dao.LocationDao
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

data class LocationImportResult(
    val visitsInserted: Int,
    val visitsSkipped: Int,
    val segmentsInserted: Int,
    val segmentsSkipped: Int,
    val pathsInserted: Int = 0,
    val pathsSkipped: Int = 0,
    val filesProcessed: Int,
)

/**
 * Importe les fichiers Google Takeout "Semantic Location History" en Room locale.
 *
 * Pipeline : `bytes (JSON ou ZIP) → parse → bulk insert Room (idempotent)`.
 *
 * - Idempotent par construction : indices uniques `(start_ms, end_ms, lat, lng)` pour
 *   visits et `(start_ms, end_ms, activity_type)` pour segments + OnConflictStrategy.IGNORE.
 * - Aucun appel réseau, aucune transmission externe.
 */
class LocalLocationImportService(
    private val dao: LocationDao,
) {

    /** Import d'un seul fichier JSON Semantic Location History. */
    suspend fun importJson(rawJson: String): LocationImportResult {
        val parsed = TakeoutTimelineParser.parse(rawJson)
        val visitIds = if (parsed.visits.isNotEmpty()) dao.insertVisits(parsed.visits) else emptyList()
        val segmentIds = if (parsed.segments.isNotEmpty()) dao.insertSegments(parsed.segments) else emptyList()
        val pathIds = if (parsed.paths.isNotEmpty()) dao.insertPaths(parsed.paths) else emptyList()
        val visitsInserted = visitIds.count { it != -1L }
        val segmentsInserted = segmentIds.count { it != -1L }
        val pathsInserted = pathIds.count { it != -1L }
        return LocationImportResult(
            visitsInserted = visitsInserted,
            visitsSkipped = parsed.visits.size - visitsInserted,
            segmentsInserted = segmentsInserted,
            segmentsSkipped = parsed.segments.size - segmentsInserted,
            pathsInserted = pathsInserted,
            pathsSkipped = parsed.paths.size - pathsInserted,
            filesProcessed = 1,
        )
    }

    /**
     * Import d'un ZIP Takeout. Cherche tous les `.json` dans le ZIP qui ressemblent
     * à du Semantic Location History (chemin contient "Semantic Location History"
     * ou nom de fichier en `<année>_<mois>.json`).
     */
    suspend fun importZip(input: InputStream, maxBytes: Long = 500_000_000L): LocationImportResult {
        var totalUncompressed = 0L
        var visitsInserted = 0
        var visitsSkipped = 0
        var segmentsInserted = 0
        var segmentsSkipped = 0
        var pathsInserted = 0
        var pathsSkipped = 0
        var filesProcessed = 0

        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && looksLikeSemanticHistory(name)) {
                    val baos = ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    var read: Int
                    while (zis.read(buf).also { read = it } != -1) {
                        totalUncompressed += read
                        if (totalUncompressed > maxBytes) {
                            throw IOException("Archive trop grande (>$maxBytes octets décompressés)")
                        }
                        baos.write(buf, 0, read)
                    }
                    val r = importJson(baos.toString(Charsets.UTF_8))
                    visitsInserted += r.visitsInserted
                    visitsSkipped += r.visitsSkipped
                    segmentsInserted += r.segmentsInserted
                    segmentsSkipped += r.segmentsSkipped
                    pathsInserted += r.pathsInserted
                    pathsSkipped += r.pathsSkipped
                    filesProcessed++
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return LocationImportResult(
            visitsInserted = visitsInserted,
            visitsSkipped = visitsSkipped,
            segmentsInserted = segmentsInserted,
            segmentsSkipped = segmentsSkipped,
            pathsInserted = pathsInserted,
            pathsSkipped = pathsSkipped,
            filesProcessed = filesProcessed,
        )
    }

    private fun looksLikeSemanticHistory(name: String): Boolean {
        if (!name.endsWith(".json", ignoreCase = true)) return false
        val lower = name.lowercase()
        // Patterns observés : "Takeout/Location History (Timeline)/Semantic Location History/2024/2024_JANUARY.json"
        // ou "Semantic Location History/.../2024_*.json"
        return "semantic location history" in lower ||
            Regex(""".*\d{4}_(january|february|march|april|may|june|july|august|september|october|november|december)\.json""").matches(lower) ||
            Regex(""".*\d{4}-\d{2}\.json""").matches(lower)
    }
}
