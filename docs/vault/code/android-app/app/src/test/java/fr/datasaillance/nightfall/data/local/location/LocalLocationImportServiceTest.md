---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/location/LocalLocationImportServiceTest.kt
git_blob: e083ed80ee8b13fe8129c77949202b984f42308c
last_synced: '2026-05-20T14:36:27Z'
loc: 334
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/location/LocalLocationImportServiceTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/location/LocalLocationImportServiceTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/data/local/location/LocalLocationImportServiceTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.location

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class LocalLocationImportServiceTest {

    private lateinit var db: NightfallDatabase
    private lateinit var service: LocalLocationImportService

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NightfallDatabase::class.java,
        ).allowMainThreadQueries().build()
        service = LocalLocationImportService(db.locationDao())
    }

    @After
    fun tearDown() { db.close() }

    private fun jsonFixture(): String = """
        {
          "timelineObjects": [
            {
              "placeVisit": {
                "location": {
                  "latitudeE7": 487856200,
                  "longitudeE7": 23478500,
                  "placeId": "ChIJ_paris_home",
                  "name": "Maison",
                  "address": "1 rue de Rivoli, Paris"
                },
                "duration": {
                  "startTimestamp": "2024-01-15T08:30:00.000Z",
                  "endTimestamp": "2024-01-15T17:45:00.000Z"
                },
                "visitConfidence": "HIGH_CONFIDENCE"
              }
            },
            {
              "activitySegment": {
                "startLocation": { "latitudeE7": 487856200, "longitudeE7": 23478500 },
                "endLocation":   { "latitudeE7": 487900000, "longitudeE7": 23500000 },
                "duration": {
                  "startTimestamp": "2024-01-15T17:45:00.000Z",
                  "endTimestamp": "2024-01-15T18:15:00.000Z"
                },
                "activityType": "WALKING",
                "distance": 2150,
                "confidence": "MEDIUM"
              }
            },
            {
              "placeVisit": {
                "location": {
                  "latitudeE7": 487900000,
                  "longitudeE7": 23500000
                },
                "duration": {
                  "startTimestamp": "2024-01-15T18:15:00.000Z",
                  "endTimestamp": "2024-01-15T20:00:00.000Z"
                }
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parse_extracts_visits_and_segments() = runTest {
        val r = service.importJson(jsonFixture())
        assertEquals(2, r.visitsInserted)
        assertEquals(1, r.segmentsInserted)
        assertEquals(0, r.visitsSkipped)

        val visits = db.locationDao().getAllVisits()
        assertEquals(2, visits.size)
        val first = visits.first()
        assertEquals(48.78562, first.lat, 0.00001)
        assertEquals(2.34785, first.lng, 0.00001)
        assertEquals("Maison", first.placeName)
        assertEquals("ChIJ_paris_home", first.placeId)
        assertEquals("HIGH_CONFIDENCE", first.confidence)

        val segments = db.locationDao().getAllSegments()
        assertEquals(1, segments.size)
        assertEquals("WALKING", segments.first().activityType)
        assertEquals(2150, segments.first().distanceMeters)
    }

    @Test
    fun import_idempotent() = runTest {
        service.importJson(jsonFixture())
        val r2 = service.importJson(jsonFixture())
        assertEquals(0, r2.visitsInserted)
        assertEquals(2, r2.visitsSkipped)
        assertEquals(0, r2.segmentsInserted)
        assertEquals(1, r2.segmentsSkipped)
        // Pas de doublons en DB
        assertEquals(2, db.locationDao().countVisits())
        assertEquals(1, db.locationDao().countSegments())
    }

    @Test
    fun parse_tolerant_to_missing_optional_fields() = runTest {
        // 2e visit du fixture n'a pas de placeId / name / address / confidence
        service.importJson(jsonFixture())
        val visits = db.locationDao().getAllVisits()
        val visitMinimal = visits.last()
        assertNull(visitMinimal.placeId)
        assertNull(visitMinimal.placeName)
        assertNull(visitMinimal.address)
        assertNull(visitMinimal.confidence)
    }

    @Test
    fun parse_ignores_malformed_entries() = runTest {
        val bad = """
            {
              "timelineObjects": [
                { "placeVisit": { "location": {} } },
                { "placeVisit": { "duration": { "startTimestamp": "2024-01-15T08:00:00Z" } } },
                { "activitySegment": {} },
                { "unknown": {} }
              ]
            }
        """.trimIndent()
        val r = service.importJson(bad)
        assertEquals(0, r.visitsInserted)
        assertEquals(0, r.segmentsInserted)
    }

    @Test
    fun activity_type_breakdown() = runTest {
        val multi = """
            {
              "timelineObjects": [
                ${segmentJson("2024-01-15T10:00:00Z", "2024-01-15T10:30:00Z", "WALKING")},
                ${segmentJson("2024-01-15T11:00:00Z", "2024-01-15T11:30:00Z", "WALKING")},
                ${segmentJson("2024-01-15T12:00:00Z", "2024-01-15T12:30:00Z", "CYCLING")},
                ${segmentJson("2024-01-15T13:00:00Z", "2024-01-15T13:30:00Z", "IN_PASSENGER_VEHICLE")}
              ]
            }
        """.trimIndent()
        service.importJson(multi)
        val breakdown = db.locationDao().getActivityTypeBreakdown()
        val map = breakdown.associate { it.activityType to it.cnt }
        assertEquals(2, map["WALKING"])
        assertEquals(1, map["CYCLING"])
        assertEquals(1, map["IN_PASSENGER_VEHICLE"])
    }

    @Test
    fun zip_import_extracts_only_semantic_history_files() = runTest {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Doit être pris : nom format Takeout
            zos.putNextEntry(ZipEntry("Takeout/Location History (Timeline)/Semantic Location History/2024/2024_JANUARY.json"))
            zos.write(jsonFixture().toByteArray())
            zos.closeEntry()
            // Doit être ignoré : autre fichier Takeout (Records.json)
            zos.putNextEntry(ZipEntry("Takeout/Location History (Timeline)/Records.json"))
            zos.write("{\"locations\":[]}".toByteArray())
            zos.closeEntry()
            // Doit être ignoré : fichier random
            zos.putNextEntry(ZipEntry("readme.txt"))
            zos.write("hello".toByteArray())
            zos.closeEntry()
        }
        val r = service.importZip(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(1, r.filesProcessed)
        assertEquals(2, r.visitsInserted)
        assertEquals(1, r.segmentsInserted)
    }

    @Test
    fun query_in_range() = runTest {
        service.importJson(jsonFixture())
        // 1er visit start = 2024-01-15T08:30:00Z = 1705307400000 ms
        val midDay = 1_705_320_000_000L // ~12:00 same day
        val nextDay = 1_705_392_000_000L // 24h later
        val visits = db.locationDao().getVisitsInRange(midDay, nextDay)
        // Seul le 2e visit (18:15 → 20:00) tombe dans la fenêtre
        assertEquals(1, visits.size)
        assertNotNull(visits.first())
    }

    private fun segmentJson(start: String, end: String, activity: String): String = """
        {
          "activitySegment": {
            "startLocation": { "latitudeE7": 487856200, "longitudeE7": 23478500 },
            "endLocation":   { "latitudeE7": 487900000, "longitudeE7": 23500000 },
            "duration": { "startTimestamp": "$start", "endTimestamp": "$end" },
            "activityType": "$activity"
          }
        }
    """.trimIndent()

    // ---- Nouveau format Google Timeline 2024+ (semanticSegments) ----

    private fun newFormatFixture(): String = """
        {
          "semanticSegments": [
            {
              "startTime": "2026-02-21T18:11:28.000+01:00",
              "endTime":   "2026-02-21T21:54:05.000+01:00",
              "visit": {
                "hierarchyLevel": 0,
                "probability": 0.876,
                "topCandidate": {
                  "placeId": "ChIJG3a7suW_9EcRF42jL3Q84rA",
                  "semanticType": "INFERRED_HOME",
                  "placeLocation": { "latLng": "45.81213°, 4.8888115°" }
                }
              }
            },
            {
              "startTime": "2026-02-21T17:29:14.000+01:00",
              "endTime":   "2026-02-21T17:30:52.000+01:00",
              "activity": {
                "start": { "latLng": "45.5276149°, 4.8801637°" },
                "end":   { "latLng": "45.5277361°, 4.8801797°" },
                "distanceMeters": 102.94,
                "probability": 0.97,
                "topCandidate": { "type": "WALKING", "probability": 0.74 }
              }
            },
            {
              "startTime": "2026-02-19T14:00:00.000+01:00",
              "endTime":   "2026-02-19T16:00:00.000+01:00",
              "timelinePath": [
                { "point": "45.527964°, 4.8787612°", "time": "2026-02-19T14:19:00.000+01:00" }
              ]
            }
          ],
          "rawSignals": [
            { "position": { "LatLng": "45.5280°, 4.8787°", "timestamp": "2026-04-20T16:12:08.000+02:00" } }
          ]
        }
    """.trimIndent()

    @Test
    fun new_format_extracts_visits_and_activities() = runTest {
        val r = service.importJson(newFormatFixture())
        assertEquals(1, r.visitsInserted)
        assertEquals(1, r.segmentsInserted)

        val visit = db.locationDao().getAllVisits().first()
        assertEquals(45.81213, visit.lat, 0.00001)
        assertEquals(4.8888115, visit.lng, 0.00001)
        assertEquals("ChIJG3a7suW_9EcRF42jL3Q84rA", visit.placeId)
        assertEquals("Maison", visit.placeName)  // INFERRED_HOME → Maison
        assertEquals("0.88", visit.confidence)

        val seg = db.locationDao().getAllSegments().first()
        assertEquals(45.5276149, seg.startLat, 0.00001)
        assertEquals(45.5277361, seg.endLat, 0.00001)
        assertEquals("WALKING", seg.activityType)
        assertEquals(102, seg.distanceMeters)  // truncated from 102.94
        assertEquals("0.97", seg.confidence)
    }

    @Test
    fun new_format_semantic_type_mapping() = runTest {
        val variants = """
            {
              "semanticSegments": [
                {
                  "startTime": "2026-01-01T00:00:00.000Z",
                  "endTime":   "2026-01-01T01:00:00.000Z",
                  "visit": { "probability": 0.5, "topCandidate": {
                    "placeId": "p1", "semanticType": "INFERRED_WORK",
                    "placeLocation": { "latLng": "45.0°, 4.0°" } } }
                },
                {
                  "startTime": "2026-01-01T02:00:00.000Z",
                  "endTime":   "2026-01-01T03:00:00.000Z",
                  "visit": { "probability": 0.5, "topCandidate": {
                    "placeId": "p2", "semanticType": "UNKNOWN",
                    "placeLocation": { "latLng": "45.1°, 4.1°" } } }
                }
              ]
            }
        """.trimIndent()
        service.importJson(variants)
        val visits = db.locationDao().getAllVisits().sortedBy { it.startMs }
        assertEquals("Travail", visits[0].placeName)
        assertNull(visits[1].placeName)  // UNKNOWN → null
    }

    @Test
    fun parse_latLngString_handles_negative_and_no_degree_symbol() {
        val (lat1, lng1) = TakeoutTimelineParser.parseLatLngString("45.81213°, 4.8888115°")!!
        assertEquals(45.81213, lat1, 0.00001)
        assertEquals(4.8888115, lng1, 0.00001)

        val (lat2, lng2) = TakeoutTimelineParser.parseLatLngString("-45.81213, -4.8888115")!!
        assertEquals(-45.81213, lat2, 0.00001)
        assertEquals(-4.8888115, lng2, 0.00001)

        assertNull(TakeoutTimelineParser.parseLatLngString(""))
        assertNull(TakeoutTimelineParser.parseLatLngString("not coords"))
        assertNull(TakeoutTimelineParser.parseLatLngString("45.0"))  // une seule valeur
    }

    @Test
    fun new_format_ignores_timeline_path_and_raw_signals() = runTest {
        val r = service.importJson(newFormatFixture())
        // Fixture a 1 visit + 1 activity + 1 timelinePath + 1 rawSignal
        // Seuls visit + activity doivent être importés
        assertEquals(1, r.visitsInserted)
        assertEquals(1, r.segmentsInserted)
        assertEquals(1, db.locationDao().countVisits())
        assertEquals(1, db.locationDao().countSegments())
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LocalLocationImportServiceTest` (class) — lines 21-334
- `setUp` (function) — lines 27-34
- `tearDown` (function) — lines 36-37
- `jsonFixture` (function) — lines 39-85
- `parse_extracts_visits_and_segments` (function) — lines 87-107
- `import_idempotent` (function) — lines 109-120
- `parse_tolerant_to_missing_optional_fields` (function) — lines 122-132
- `parse_ignores_malformed_entries` (function) — lines 134-149
- `activity_type_breakdown` (function) — lines 151-169
- `zip_import_extracts_only_semantic_history_files` (function) — lines 171-192
- `query_in_range` (function) — lines 194-204
- `segmentJson` (function) — lines 206-215
- `newFormatFixture` (function) — lines 219-258
- `new_format_extracts_visits_and_activities` (function) — lines 260-279
- `new_format_semantic_type_mapping` (function) — lines 281-307
- `parse_latLngString_handles_negative_and_no_degree_symbol` (function) — lines 309-322
- `new_format_ignores_timeline_path_and_raw_signals` (function) — lines 324-333
