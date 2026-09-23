package fr.datasaillance.nightfall.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.StepsInterval
import java.time.Instant

/**
 * La vraie source : lit Health Connect page par page, sur une seule plage (de 1970 à maintenant),
 * et convertit chaque page vers le contrat. Une instance = une lecture (la fin de plage est figée à sa création).
 */
class HealthConnectReader(
    private val client: HealthConnectClient,
    now: Instant = Instant.now(),
) : HealthRecordsSource {

    private val wholeHistory = TimeRangeFilter.between(Instant.EPOCH, now)

    override suspend fun sleepPage(pageToken: String?): RecordPage<SleepRecord> {
        val response = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = wholeHistory, pageSize = PAGE_SIZE, pageToken = pageToken),
        )
        return RecordPage(response.records.map { it.toSleepRecord() }, response.pageToken)
    }

    override suspend fun stepsPage(pageToken: String?): RecordPage<StepsInterval> {
        val response = client.readRecords(
            ReadRecordsRequest(StepsRecord::class, timeRangeFilter = wholeHistory, pageSize = PAGE_SIZE, pageToken = pageToken),
        )
        return RecordPage(response.records.map { it.toStepsInterval() }, response.pageToken)
    }

    private companion object {
        const val PAGE_SIZE = 1000
    }
}

/**
 * Lit tout Health Connect sur ce téléphone si c'est possible (installé, permissions accordées), sinon null.
 * Ne consulte ni ne remplit le cache : c'est à l'appelant de décider.
 */
suspend fun loadFromDevice(context: Context): HealthData? =
    loadIfReady(currentHealthConnectState(context)) { HealthConnectReader(HealthConnectClient.getOrCreate(context)) }

/** Interroge le téléphone (Health Connect installé ? permissions ? fonctionnalité historique ?) puis décide de l'état. */
suspend fun currentHealthConnectState(context: Context): HealthConnectState {
    val sdkStatus = HealthConnectClient.getSdkStatus(context)
    if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
        return decideHealthConnectState(sdkStatus, emptySet(), historyFeatureAvailable = false)
    }
    val client = HealthConnectClient.getOrCreate(context)
    val granted = client.permissionController.getGrantedPermissions()
    val historyStatus = client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY)
    return decideHealthConnectState(
        sdkStatus,
        granted,
        historyFeatureAvailable = historyStatus == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE,
    )
}
