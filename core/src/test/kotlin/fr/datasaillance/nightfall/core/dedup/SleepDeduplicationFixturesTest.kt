package fr.datasaillance.nightfall.core.dedup

import fr.datasaillance.nightfall.core.export.CsvExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.time.Instant

/**
 * Golden fixtures émises par la section 8 du notebook 01 : pour chaque cas, le port Kotlin
 * doit redonner exactement les sorties calculées (et vérifiées) dans le notebook.
 */
@RunWith(Parameterized::class)
class SleepDeduplicationFixturesTest(private val caseName: String) {

    companion object {
        private val fixturesDir = File(checkNotNull(SleepDeduplicationFixturesTest::class.java.getResource("/fixtures/dedup")).toURI())

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<String> = fixturesDir.listFiles()!!.filter { it.isDirectory }.map { it.name }.sorted()
    }

    private val caseDir = File(fixturesDir, caseName)

    private fun read(fileName: String): String = File(caseDir, fileName).readText()

    // Lignes de données d'un petit CSV attendu (sans guillemets : les fixtures n'en contiennent pas)
    private fun expectedRows(fileName: String): List<List<String>> =
        read(fileName).lines().drop(1).filter { it.isNotBlank() }.map { it.split(",") }

    private val records = CsvExport.readSleepRecords(read("sleep_sessions.csv"), read("sleep_stages.csv"))

    private fun expectedEpisodes(fileName: String): List<SleepEpisode> =
        expectedRows(fileName).map { (start, end, members) ->
            SleepEpisode(Instant.parse(start), Instant.parse(end), members.split(";"))
        }

    @Test
    fun `episodes d'analyse, regle darkhour`() {
        assertEquals(
            expectedEpisodes("expected_episodes_darkhour.csv"),
            SleepDeduplication.analysisEpisodes(records, EpisodeRule.OVERLAP_ONLY),
        )
    }

    @Test
    fun `episodes d'analyse, regle Nightfall`() {
        assertEquals(
            expectedEpisodes("expected_episodes_nightfall.csv"),
            SleepDeduplication.analysisEpisodes(records, EpisodeRule.OVERLAP_OR_TOUCH),
        )
    }

    @Test
    fun `sessions affichees`() {
        assertEquals(
            expectedRows("expected_displayed.csv").map { it.single() },
            SleepDeduplication.displayedSessions(records).map { it.id },
        )
    }

    @Test
    fun `le jeu de fixtures n'est pas vide`() {
        assertTrue("au moins 9 cas attendus, ${cases().size} trouvés", cases().size >= 9)
    }
}
