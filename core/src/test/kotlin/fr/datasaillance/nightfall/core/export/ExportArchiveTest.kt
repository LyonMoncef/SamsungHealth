package fr.datasaillance.nightfall.core.export

import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.core.model.RecordingMethod
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.SleepStage
import fr.datasaillance.nightfall.core.model.StageType
import fr.datasaillance.nightfall.core.model.HourlySteps
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipInputStream

// Archive d'export (spec 2026-09-23-phase1-data-foundation, DT-4 et DT-7, TA-14).
class ExportArchiveTest {

    private fun t(text: String): Instant = Instant.parse(text)

    private val night = SleepRecord(
        id = "s1", start = t("2024-07-08T22:31:00Z"), end = t("2024-07-09T06:45:30Z"), startOffset = null, endOffset = null,
        source = "com.sec.android.app.shealth", recordingMethod = RecordingMethod.AUTOMATICALLY_RECORDED,
        lastModified = t("2024-07-09T07:00:00Z"),
        stages = listOf(SleepStage(t("2024-07-08T22:31:00Z"), t("2024-07-08T23:00:00Z"), StageType.LIGHT)),
    )

    private val steps = HourlySteps(
        start = t("2024-07-09T07:00:00Z"), end = t("2024-07-09T08:00:00Z"), offset = null,
        count = 1234, sources = listOf("com.sec.android.app.shealth"),
    )

    private val manifest = buildExportManifest(listOf(night), listOf(steps), t("2026-09-24T10:00:00Z"), "4.0.0", HistoryAccess.FULL)

    private fun archiveBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        ExportArchive.write(output, listOf(night), listOf(steps), manifest)
        return output.toByteArray()
    }

    /** Relit l'archive : nom de chaque entrée → contenu texte, dans l'ordre. */
    private fun entries(bytes: ByteArray): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                result.add(entry.name to zip.readBytes().toString(Charsets.UTF_8))
            }
        }
        return result
    }

    @Test
    fun `l'archive contient les 3 CSV et le manifeste, dans cet ordre, avec le contenu attendu`() {
        val content = entries(archiveBytes())

        assertEquals(
            listOf("sleep_sessions.csv", "sleep_stages.csv", "steps.csv", "manifest.json"),
            content.map { it.first },
        )
        assertEquals(CsvExport.writeSleepSessions(listOf(night)), content[0].second)
        assertEquals(CsvExport.writeSleepStages(listOf(night)), content[1].second)
        assertEquals(CsvExport.writeSteps(listOf(steps)), content[2].second)
        assertEquals(manifest.toJson(), content[3].second)
    }

    @Test
    fun `TA-14 les donnees relues depuis l'archive sont identiques aux donnees exportees`() {
        val content = entries(archiveBytes()).toMap()

        val sleepBack = CsvExport.readSleepRecords(content.getValue("sleep_sessions.csv"), content.getValue("sleep_stages.csv"))
        val stepsBack = CsvExport.readSteps(content.getValue("steps.csv"))

        assertEquals(listOf(night), sleepBack)
        assertEquals(listOf(steps), stepsBack)
    }

    @Test
    fun `deux exports des memes donnees avec le meme manifeste produisent exactement les memes octets`() {
        assertArrayEquals(archiveBytes(), archiveBytes())
    }
}
