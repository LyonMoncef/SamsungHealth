package fr.datasaillance.nightfall.data.local.debug

import android.content.Context
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dump éphémère des tables local-first en JSON pour besoin design / debug.
 * Écrit dans `Android/data/<pkg>/files/` (accessible via `adb pull` sans run-as).
 *
 * 2 fichiers générés :
 * - `timeline_<window>.json` : visits + activities + paths (location_*)
 * - `usage_daily_<window>.json` : usage_daily sur la fenêtre
 *
 * Filtre : fenêtre `windowDays` jours en arrière depuis aujourd'hui (defaut 90).
 * Sérialisation manuelle (pas de kotlinx.serialization sur les entities Room).
 */
object DebugExporter {

    suspend fun exportAll(context: Context, windowDays: Int = 90): List<File> {
        val db = NightfallDatabase.get(context.applicationContext)
        val today = LocalDate.now()
        val fromDate = today.minusDays(windowDays.toLong())
        val fromMs = fromDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toMs = today.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tag = "${fromDate}_${today}"

        val dir = context.getExternalFilesDir(null) ?: context.filesDir

        // --- Timeline (location) ---
        val visits = db.locationDao().getVisitsInRange(fromMs, toMs)
        val segments = db.locationDao().getSegmentsInRange(fromMs, toMs)
        val paths = db.locationDao().getPathsInRange(fromMs, toMs)

        val timelineJson = buildString {
            append("{\n  \"visits\": [")
            visits.forEachIndexed { i, v ->
                if (i > 0) append(",")
                append("\n    {")
                append("\"start_ms\":${v.startMs},")
                append("\"end_ms\":${v.endMs},")
                append("\"lat\":${v.lat},")
                append("\"lng\":${v.lng},")
                append("\"place_id\":${jsonStr(v.placeId)},")
                append("\"place_name\":${jsonStr(v.placeName)},")
                append("\"address\":${jsonStr(v.address)},")
                append("\"confidence\":${jsonStr(v.confidence)}")
                append("}")
            }
            append("\n  ],\n  \"activities\": [")
            segments.forEachIndexed { i, s ->
                if (i > 0) append(",")
                append("\n    {")
                append("\"start_ms\":${s.startMs},")
                append("\"end_ms\":${s.endMs},")
                append("\"start_lat\":${s.startLat},")
                append("\"start_lng\":${s.startLng},")
                append("\"end_lat\":${s.endLat},")
                append("\"end_lng\":${s.endLng},")
                append("\"activity_type\":${jsonStr(s.activityType)},")
                append("\"distance_m\":${s.distanceMeters ?: "null"},")
                append("\"confidence\":${jsonStr(s.confidence)}")
                append("}")
            }
            append("\n  ],\n  \"paths\": [")
            paths.forEachIndexed { i, p ->
                if (i > 0) append(",")
                append("\n    {")
                append("\"start_ms\":${p.startMs},")
                append("\"end_ms\":${p.endMs},")
                append("\"point_count\":${p.pointCount},")
                append("\"points_json\":${jsonStr(p.pointsJson)}")
                append("}")
            }
            append("\n  ]\n}\n")
        }
        val timelineFile = File(dir, "timeline_${tag}.json")
        timelineFile.writeText(timelineJson)

        // --- Usage daily ---
        val fromStr = fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val toStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val usageRows = db.usageStatsDao().getInRange(fromStr, toStr)
        val usageJson = buildString {
            append("[")
            usageRows.forEachIndexed { i, r ->
                if (i > 0) append(",")
                append("\n  {")
                append("\"date\":${jsonStr(r.date)},")
                append("\"package_name\":${jsonStr(r.packageName)},")
                append("\"total_time_foreground_ms\":${r.totalTimeForegroundMs},")
                append("\"total_time_visible_ms\":${r.totalTimeVisibleMs},")
                append("\"total_time_fgs_ms\":${r.totalTimeForegroundServiceMs},")
                append("\"last_time_used_ms\":${r.lastTimeUsedMs},")
                append("\"app_launch_count\":${r.appLaunchCount}")
                append("}")
            }
            append("\n]\n")
        }
        val usageFile = File(dir, "usage_daily_${tag}.json")
        usageFile.writeText(usageJson)

        return listOf(timelineFile, usageFile)
    }

    private fun jsonStr(s: String?): String {
        if (s == null) return "null"
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}
