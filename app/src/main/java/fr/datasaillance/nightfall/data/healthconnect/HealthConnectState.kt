package fr.datasaillance.nightfall.data.healthconnect

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import fr.datasaillance.nightfall.core.model.HistoryAccess

/** Permissions demandées : lecture seule du sommeil, des pas et de l'historique complet (spec DT-5). */
object HealthConnectPermissions {
    val READ_SLEEP: String = HealthPermission.getReadPermission(SleepSessionRecord::class)
    val READ_STEPS: String = HealthPermission.getReadPermission(StepsRecord::class)
    const val READ_HISTORY: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    /** Sans celles-ci, on ne lit rien. */
    val REQUIRED: Set<String> = setOf(READ_SLEEP, READ_STEPS)

    /** Tout ce qu'on demande à l'utilisateur. */
    val ALL: Set<String> = REQUIRED + READ_HISTORY
}

/** Où en est Health Connect sur ce téléphone, du point de vue de Nightfall. */
sealed interface HealthConnectState {
    data object NotInstalled : HealthConnectState
    data object UpdateRequired : HealthConnectState
    data object PermissionsMissing : HealthConnectState
    data class Ready(val historyAccess: HistoryAccess) : HealthConnectState
}

/**
 * Décide de l'état à partir de ce que dit le téléphone. Fonction pure pour pouvoir la tester.
 * L'historique n'est complet que si la fonctionnalité existe ET que la permission est accordée ;
 * sinon il est limité à 30 jours, et l'interface doit le dire (spec DT-5 : jamais de troncature silencieuse).
 */
fun decideHealthConnectState(sdkStatus: Int, grantedPermissions: Set<String>, historyFeatureAvailable: Boolean): HealthConnectState {
    if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
        return HealthConnectState.UpdateRequired
    }
    if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
        return HealthConnectState.NotInstalled
    }
    if (!grantedPermissions.containsAll(HealthConnectPermissions.REQUIRED)) {
        return HealthConnectState.PermissionsMissing
    }
    val fullHistory = historyFeatureAvailable && HealthConnectPermissions.READ_HISTORY in grantedPermissions
    return HealthConnectState.Ready(if (fullHistory) HistoryAccess.FULL else HistoryAccess.LIMITED_30_DAYS)
}
