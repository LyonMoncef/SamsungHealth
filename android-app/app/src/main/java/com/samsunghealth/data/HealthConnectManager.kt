package com.samsunghealth.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
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

class HealthConnectManager(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())

    val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
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

    private fun mapStageType(stage: Int): String = when (stage) {
        SleepSessionRecord.STAGE_TYPE_LIGHT -> "light"
        SleepSessionRecord.STAGE_TYPE_DEEP -> "deep"
        SleepSessionRecord.STAGE_TYPE_REM -> "rem"
        SleepSessionRecord.STAGE_TYPE_AWAKE -> "awake"
        else -> "unknown"
    }
}
