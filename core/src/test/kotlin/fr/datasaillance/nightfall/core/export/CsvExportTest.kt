package fr.datasaillance.nightfall.core.export

import fr.datasaillance.nightfall.core.model.HistoryAccess
import fr.datasaillance.nightfall.core.model.RecordingMethod
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.SleepStage
import fr.datasaillance.nightfall.core.model.StageType
import fr.datasaillance.nightfall.core.model.StepsInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

// Tests d'acceptation TA-1 à TA-6 de la spec 2026-09-23-phase1-data-foundation.
class CsvExportTest {

    private fun t(text: String): Instant = Instant.parse(text)

    private val plus2 = ZoneOffset.ofHours(2)

    // Une nuit complète qui contient les 8 types de stades, avec fuseau connu.
    private val night = SleepRecord(
        id = "s1",
        start = t("2024-07-08T22:31:00Z"),
        end = t("2024-07-09T06:45:30Z"),
        startOffset = plus2,
        endOffset = plus2,
        source = "com.sec.android.app.shealth",
        recordingMethod = RecordingMethod.AUTOMATICALLY_RECORDED,
        lastModified = t("2024-07-09T07:00:00.123Z"),
        stages = listOf(
            SleepStage(t("2024-07-08T22:31:00Z"), t("2024-07-08T22:40:00Z"), StageType.AWAKE_IN_BED),
            SleepStage(t("2024-07-08T22:40:00Z"), t("2024-07-08T23:30:00Z"), StageType.LIGHT),
            SleepStage(t("2024-07-08T23:30:00Z"), t("2024-07-09T00:30:00Z"), StageType.DEEP),
            SleepStage(t("2024-07-09T00:30:00Z"), t("2024-07-09T01:30:00Z"), StageType.REM),
            SleepStage(t("2024-07-09T01:30:00Z"), t("2024-07-09T01:35:00Z"), StageType.AWAKE),
            SleepStage(t("2024-07-09T01:35:00Z"), t("2024-07-09T01:40:00Z"), StageType.OUT_OF_BED),
            SleepStage(t("2024-07-09T01:40:00Z"), t("2024-07-09T05:00:00Z"), StageType.SLEEPING),
            SleepStage(t("2024-07-09T05:00:00Z"), t("2024-07-09T06:45:30Z"), StageType.UNKNOWN),
        ),
    )

    // Une sieste sans stades et sans fuseau connu.
    private val nap = SleepRecord(
        id = "s2",
        start = t("2024-07-10T13:00:00Z"),
        end = t("2024-07-10T13:40:00Z"),
        startOffset = null,
        endOffset = null,
        source = "com.example.nap",
        recordingMethod = RecordingMethod.MANUAL_ENTRY,
        lastModified = t("2024-07-10T13:41:00Z"),
        stages = emptyList(),
    )

    private val stepsMorning = StepsInterval(
        id = "p1",
        start = t("2024-07-09T07:00:00Z"),
        end = t("2024-07-09T07:15:00Z"),
        startOffset = plus2,
        endOffset = plus2,
        count = 1234,
        source = "com.sec.android.app.shealth",
        recordingMethod = RecordingMethod.AUTOMATICALLY_RECORDED,
        lastModified = t("2024-07-09T07:16:00Z"),
    )

    private val stepsUnknownZone = StepsInterval(
        id = "p2",
        start = t("2024-07-09T12:00:00Z"),
        end = t("2024-07-09T12:01:00Z"),
        startOffset = null,
        endOffset = null,
        count = 0,
        source = "com.example.pedometer",
        recordingMethod = RecordingMethod.UNKNOWN,
        lastModified = t("2024-07-09T12:02:00Z"),
    )

    // ---------------------------------------------------------------- TA-1
    @Test
    fun `TA-1 ecrire puis relire le sommeil redonne exactement les memes sessions`() {
        val records = listOf(night, nap)

        val sessionsCsv = CsvExport.writeSleepSessions(records)
        val stagesCsv = CsvExport.writeSleepStages(records)
        val readBack = CsvExport.readSleepRecords(sessionsCsv, stagesCsv)

        assertEquals(records, readBack)
    }

    // ---------------------------------------------------------------- TA-2
    @Test
    fun `TA-2 ecrire puis relire les pas redonne exactement les memes intervalles`() {
        val intervals = listOf(stepsMorning, stepsUnknownZone)

        val readBack = CsvExport.readSteps(CsvExport.writeSteps(intervals))

        assertEquals(intervals, readBack)
    }

    // ---------------------------------------------------------------- TA-3
    @Test
    fun `TA-3 le meme contenu dans un autre ordre produit exactement le meme fichier`() {
        val nightWithShuffledStages = night.copy(stages = night.stages.reversed())

        assertEquals(
            CsvExport.writeSleepSessions(listOf(night, nap)),
            CsvExport.writeSleepSessions(listOf(nap, night)),
        )
        assertEquals(
            CsvExport.writeSleepStages(listOf(night, nap)),
            CsvExport.writeSleepStages(listOf(nap, nightWithShuffledStages)),
        )
        assertEquals(
            CsvExport.writeSteps(listOf(stepsMorning, stepsUnknownZone)),
            CsvExport.writeSteps(listOf(stepsUnknownZone, stepsMorning)),
        )
    }

    // ---------------------------------------------------------------- TA-4
    @Test
    fun `TA-4 format exact du fichier des sessions`() {
        val utcNight = night.copy(id = "s3", startOffset = ZoneOffset.UTC, endOffset = ZoneOffset.UTC)

        val expected =
            "id,start_utc,end_utc,start_offset,end_offset,source,recording_method,last_modified_utc\n" +
                "s1,2024-07-08T22:31:00Z,2024-07-09T06:45:30Z,+02:00,+02:00,com.sec.android.app.shealth,AUTOMATICALLY_RECORDED,2024-07-09T07:00:00.123Z\n" +
                "s3,2024-07-08T22:31:00Z,2024-07-09T06:45:30Z,Z,Z,com.sec.android.app.shealth,AUTOMATICALLY_RECORDED,2024-07-09T07:00:00.123Z\n" +
                "s2,2024-07-10T13:00:00Z,2024-07-10T13:40:00Z,,,com.example.nap,MANUAL_ENTRY,2024-07-10T13:41:00Z\n"

        assertEquals(expected, CsvExport.writeSleepSessions(listOf(nap, utcNight, night)))
    }

    @Test
    fun `TA-4 format exact du fichier des stades`() {
        val shortNight = night.copy(stages = night.stages.take(2))

        val expected =
            "session_id,start_utc,end_utc,stage\n" +
                "s1,2024-07-08T22:31:00Z,2024-07-08T22:40:00Z,AWAKE_IN_BED\n" +
                "s1,2024-07-08T22:40:00Z,2024-07-08T23:30:00Z,LIGHT\n"

        assertEquals(expected, CsvExport.writeSleepStages(listOf(shortNight, nap)))
    }

    @Test
    fun `TA-4 format exact du fichier des pas`() {
        val expected =
            "id,start_utc,end_utc,start_offset,end_offset,count,source,recording_method,last_modified_utc\n" +
                "p1,2024-07-09T07:00:00Z,2024-07-09T07:15:00Z,+02:00,+02:00,1234,com.sec.android.app.shealth,AUTOMATICALLY_RECORDED,2024-07-09T07:16:00Z\n" +
                "p2,2024-07-09T12:00:00Z,2024-07-09T12:01:00Z,,,0,com.example.pedometer,UNKNOWN,2024-07-09T12:02:00Z\n"

        assertEquals(expected, CsvExport.writeSteps(listOf(stepsUnknownZone, stepsMorning)))
    }

    // ---------------------------------------------------------------- TA-5
    @Test
    fun `TA-5 un champ avec virgule, guillemets et saut de ligne fait l'aller-retour intact`() {
        val trickySource = "app,avec \"guillemets\"\net saut de ligne"
        val record = nap.copy(source = trickySource)

        val sessionsCsv = CsvExport.writeSleepSessions(listOf(record))
        val readBack = CsvExport.readSleepRecords(sessionsCsv, CsvExport.writeSleepStages(listOf(record)))

        assertTrue(
            "le champ doit être entouré de guillemets et ses guillemets doublés",
            sessionsCsv.contains("\"app,avec \"\"guillemets\"\"\net saut de ligne\""),
        )
        assertEquals(listOf(record), readBack)
    }

    // ---------------------------------------------------------------- TA-6
    @Test
    fun `TA-6 manifeste avec version, comptes de lignes et plus ancienne session`() {
        val manifest = buildExportManifest(
            sleepRecords = listOf(nap, night),
            steps = listOf(stepsMorning, stepsUnknownZone),
            exportedAt = t("2026-09-23T20:00:00Z"),
            appVersion = "4.0.0",
            historyAccess = HistoryAccess.FULL,
        )

        val expected = """
            {
              "contract_version": "1",
              "exported_at": "2026-09-23T20:00:00Z",
              "app_version": "4.0.0",
              "sleep_sessions_count": 2,
              "sleep_stages_count": 8,
              "steps_count": 2,
              "history_access": "FULL",
              "oldest_sleep_start": "2024-07-08T22:31:00Z"
            }
        """.trimIndent() + "\n"

        assertEquals(expected, manifest.toJson())
    }

    @Test
    fun `TA-6 manifeste sans donnees et historique limite`() {
        val manifest = buildExportManifest(
            sleepRecords = emptyList(),
            steps = emptyList(),
            exportedAt = t("2026-09-23T20:00:00Z"),
            appVersion = "4.0.0",
            historyAccess = HistoryAccess.LIMITED_30_DAYS,
        )

        val json = manifest.toJson()

        assertTrue(json, json.contains("\"sleep_sessions_count\": 0,"))
        assertTrue(json, json.contains("\"history_access\": \"LIMITED_30_DAYS\","))
        assertTrue(json, json.contains("\"oldest_sleep_start\": null"))
    }
}
