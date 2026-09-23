package fr.datasaillance.nightfall.core.model

/** Les 4 méthodes d'enregistrement de Health Connect, en 1:1 (RECORDING_METHOD_UNKNOWN = 0 … MANUAL_ENTRY = 3). */
enum class RecordingMethod {
    UNKNOWN,
    ACTIVELY_RECORDED,
    AUTOMATICALLY_RECORDED,
    MANUAL_ENTRY,
}
