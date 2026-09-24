package fr.datasaillance.nightfall.core.export

import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.HourlySteps
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Archive d'export (spec 2026-09-23-phase1-data-foundation, DT-4 et DT-7) : un seul fichier ZIP
 * contenant les 3 CSV du contrat et `manifest.json`, pour la copie vers le notebook.
 *
 * La date de chaque entrée est celle de l'export (et non l'heure courante) : deux exports des mêmes
 * données avec le même manifeste donnent exactement les mêmes octets.
 */
object ExportArchive {

    fun write(output: OutputStream, sleep: List<SleepRecord>, steps: List<HourlySteps>, manifest: ExportManifest) {
        val entries = listOf(
            "sleep_sessions.csv" to CsvExport.writeSleepSessions(sleep),
            "sleep_stages.csv" to CsvExport.writeSleepStages(sleep),
            "steps.csv" to CsvExport.writeSteps(steps),
            "manifest.json" to manifest.toJson(),
        )
        val zip = ZipOutputStream(output)
        for ((name, text) in entries) {
            val entry = ZipEntry(name)
            entry.time = manifest.exportedAt.toEpochMilli()
            zip.putNextEntry(entry)
            zip.write(text.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        // finish() et non close() : c'est l'appelant qui a ouvert le flux, c'est à lui de le fermer.
        zip.finish()
    }
}
