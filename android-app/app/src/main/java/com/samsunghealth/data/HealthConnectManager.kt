package com.samsunghealth.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SleepStageData(
    val stageType: String,
    val stageStart: String,
    val stageEnd: String,
)

data class SleepSessionData(
    val sleepStart: String,
    val sleepEnd: String,
    val stages: List<SleepStageData>,
)

data class StepsHourlyData(
    val date: String,
    val hour: Int,
    val stepCount: Int,
)

data class HeartRateHourlyData(
    val date: String,
    val hour: Int,
    val minBpm: Int,
    val maxBpm: Int,
    val avgBpm: Int,
    val sampleCount: Int,
)

data class ExerciseSessionData(
    val exerciseType: String,
    val exerciseStart: String,
    val exerciseEnd: String,
    val durationMinutes: Double,
)

class HealthConnectManager(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault())

    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    fun getSdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    suspend fun hasAllPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return permissions.all { it in granted }
    }

    suspend fun readSleepSessions(since: Instant): List<SleepSessionData> {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.after(since),
        )
        val response = client.readRecords(request)

        return response.records.map { record ->
            val stages = record.stages.map { stage ->
                SleepStageData(
                    stageType = mapStageType(stage.stage),
                    stageStart = formatter.format(stage.startTime),
                    stageEnd = formatter.format(stage.endTime),
                )
            }
            SleepSessionData(
                sleepStart = formatter.format(record.startTime),
                sleepEnd = formatter.format(record.endTime),
                stages = stages,
            )
        }
    }

    suspend fun readSteps(since: Instant): List<StepsHourlyData> {
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.after(since),
        )
        val response = client.readRecords(request)
        val zone = ZoneId.systemDefault()

        val hourlyMap = mutableMapOf<Pair<String, Int>, Long>()
        for (record in response.records) {
            val startZoned = record.startTime.atZone(zone)
            val endZoned = record.endTime.atZone(zone)
            val date = startZoned.toLocalDate().toString()
            val hour = startZoned.hour
            val key = Pair(date, hour)
            hourlyMap[key] = (hourlyMap[key] ?: 0L) + record.count
        }

        return hourlyMap.map { (key, count) ->
            StepsHourlyData(
                date = key.first,
                hour = key.second,
                stepCount = count.toInt(),
            )
        }
    }

    suspend fun readHeartRate(since: Instant): List<HeartRateHourlyData> {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.after(since),
        )
        val response = client.readRecords(request)
        val zone = ZoneId.systemDefault()

        val hourlyMap = mutableMapOf<Pair<String, Int>, MutableList<Long>>()
        for (record in response.records) {
            for (sample in record.samples) {
                val zoned = sample.time.atZone(zone)
                val date = zoned.toLocalDate().toString()
                val hour = zoned.hour
                val key = Pair(date, hour)
                hourlyMap.getOrPut(key) { mutableListOf() }.add(sample.beatsPerMinute)
            }
        }

        return hourlyMap.map { (key, bpms) ->
            HeartRateHourlyData(
                date = key.first,
                hour = key.second,
                minBpm = bpms.min().toInt(),
                maxBpm = bpms.max().toInt(),
                avgBpm = (bpms.sum().toDouble() / bpms.size).toInt(),
                sampleCount = bpms.size,
            )
        }
    }

    suspend fun readExerciseSessions(since: Instant): List<ExerciseSessionData> {
        val request = ReadRecordsRequest(
            recordType = ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.after(since),
        )
        val response = client.readRecords(request)

        return response.records.map { record ->
            val durationMs = record.endTime.toEpochMilli() - record.startTime.toEpochMilli()
            ExerciseSessionData(
                exerciseType = mapExerciseType(record.exerciseType),
                exerciseStart = formatter.format(record.startTime),
                exerciseEnd = formatter.format(record.endTime),
                durationMinutes = durationMs / 60000.0,
            )
        }
    }

    private fun mapStageType(stage: Int): String = when (stage) {
        SleepSessionRecord.STAGE_TYPE_LIGHT -> "light"
        SleepSessionRecord.STAGE_TYPE_DEEP -> "deep"
        SleepSessionRecord.STAGE_TYPE_REM -> "rem"
        SleepSessionRecord.STAGE_TYPE_AWAKE -> "awake"
        else -> "unknown"
    }

    private fun mapExerciseType(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "running"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "walking"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "cycling"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "swimming"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "hiking"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "strength_training"
        else -> "other"
    }
}
