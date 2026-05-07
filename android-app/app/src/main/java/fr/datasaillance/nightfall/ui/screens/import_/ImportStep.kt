package fr.datasaillance.nightfall.ui.screens.import_

sealed class ImportStep(val index: Int, val label: String) {
    object Connection : ImportStep(1, "Connexion")
    object Selection  : ImportStep(2, "Sélection")
    object Upload     : ImportStep(3, "Import")
}
