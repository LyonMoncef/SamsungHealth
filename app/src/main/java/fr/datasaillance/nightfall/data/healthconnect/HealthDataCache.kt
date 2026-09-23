package fr.datasaillance.nightfall.data.healthconnect

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dernière lecture Health Connect, partagée entre les écrans le temps de la session.
 * Lecture directe (spec DT-5/DT-6) : rien n'est écrit sur disque, tout disparaît à la fermeture de l'app.
 */
class HealthDataCache {
    private val _data = MutableStateFlow<HealthData?>(null)
    val data: StateFlow<HealthData?> = _data.asStateFlow()

    fun update(newData: HealthData) {
        _data.value = newData
    }
}

/** L'instance unique utilisée par l'app. */
val sharedHealthDataCache = HealthDataCache()
