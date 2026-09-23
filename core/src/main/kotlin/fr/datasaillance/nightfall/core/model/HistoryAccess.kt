package fr.datasaillance.nightfall.core.model

/**
 * Profondeur d'historique réellement accessible lors de la lecture Health Connect.
 * LIMITED_30_DAYS doit toujours être affiché à l'utilisateur (spec DT-5 : jamais de troncature silencieuse).
 */
enum class HistoryAccess {
    FULL,
    LIMITED_30_DAYS,
}
