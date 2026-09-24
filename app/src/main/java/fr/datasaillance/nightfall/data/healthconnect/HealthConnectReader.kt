package fr.datasaillance.nightfall.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import fr.datasaillance.nightfall.core.model.SleepRecord
import fr.datasaillance.nightfall.core.model.HourlySteps
import java.time.Duration
import java.time.Instant

/**
 * La vraie source. Sommeil : lu page par page sur une seule plage (de 1970 à maintenant) et converti vers le contrat.
 * Pas : totaux horaires calculés par Health Connect (API d'agrégation), sans passer par les enregistrements bruts.
 * Une instance = une lecture (la fin de plage est figée à sa création).
 */
class HealthConnectReader(
    private val client: HealthConnectClient,
    private val now: Instant = Instant.now(),
) : HealthRecordsSource {

    private val wholeHistory = TimeRangeFilter.between(Instant.EPOCH, now)

    override suspend fun sleepPage(pageToken: String?): RecordPage<SleepRecord> {
        val response = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = wholeHistory, pageSize = PAGE_SIZE, pageToken = pageToken),
        )
        return RecordPage(response.records.map { it.toSleepRecord() }, response.pageToken)
    }

    /**
     * Pas horaires, en deux passes :
     * 1. une requête avec des tranches d'un an depuis 1970, pour trouver la première année qui contient des pas ;
     * 2. à partir de là, des tranches d'une heure, par blocs de 30 jours (réponses de taille raisonnable).
     * L'agrégation se fait côté Health Connect : un enregistrement brut invalide (début = fin, cf. 2026-09-24)
     * ne peut pas faire échouer la lecture, et les sources en double (téléphone + montre) sont déjà dédoublonnées.
     */
    override suspend fun hourlySteps(): List<HourlySteps> {
        val yearly = client.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = wholeHistory,
                timeRangeSlicer = Duration.ofDays(365),
            ),
        )
        val firstYearWithSteps = yearly.firstOrNull { it.result[StepsRecord.COUNT_TOTAL] != null }?.startTime
            ?: return emptyList()

        val hours = mutableListOf<HourlySteps>()
        var blockStart = firstYearWithSteps
        while (blockStart < now) {
            val blockEnd = minOf(blockStart.plus(STEPS_BLOCK), now)
            val groups = client.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(blockStart, blockEnd),
                    timeRangeSlicer = Duration.ofHours(1),
                ),
            )
            for (group in groups) {
                val count = group.result[StepsRecord.COUNT_TOTAL] ?: continue // heure sans aucune donnée de pas
                hours.add(
                    HourlySteps(
                        start = group.startTime,
                        end = group.endTime,
                        offset = group.zoneOffset,
                        count = count,
                        sources = group.result.dataOrigins.map { it.packageName }.sorted(),
                    ),
                )
            }
            blockStart = blockEnd
        }
        return hours
    }

    private companion object {
        const val PAGE_SIZE = 1000
        val STEPS_BLOCK: Duration = Duration.ofDays(30)
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
