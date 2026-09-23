package fr.datasaillance.nightfall.ui.screens.places

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import fr.datasaillance.nightfall.data.local.database.NightfallDatabase
import fr.datasaillance.nightfall.viewmodel.places.LabeledPlacesViewModel

/**
 * Route Compose qui assemble `LabeledPlacesViewModel` + `LabeledPlacesScreen` côté
 * flavor `native`. La séparation route/screen suit le pattern `RadialRoute` :
 *   - le screen reste pur (state-in / events-out),
 *   - la route fait l'instanciation DAO + ViewModel + collectAsState,
 *   - les flavors qui ne savent pas afficher l'écran (ex. webview) fournissent un
 *     stub `LabeledPlacesRoute()` qui ne dépend pas de ce screen.
 */
@Composable
fun LabeledPlacesRoute(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val db = remember(context) { NightfallDatabase.get(context.applicationContext) }
    val viewModel = remember(db) {
        LabeledPlacesViewModel(
            labeledPlaceDao = db.labeledPlaceDao(),
            locationDao = db.locationDao(),
        )
    }
    val state by viewModel.uiState.collectAsState()

    LabeledPlacesScreen(
        state = state,
        onAddPlace = { label, category, lat, lng, radius ->
            viewModel.addPlace(label, category, lat, lng, radius)
        },
        onUpdatePlace = { viewModel.updatePlace(it) },
        onDeletePlace = { viewModel.deletePlace(it) },
        onBack = onBack,
    )
}
