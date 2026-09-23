package fr.datasaillance.nightfall.viewmodel.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.datasaillance.nightfall.data.local.dao.LabeledPlaceDao
import fr.datasaillance.nightfall.data.local.dao.LocationDao
import fr.datasaillance.nightfall.data.local.entity.location.LabeledPlaceEntity
import fr.datasaillance.nightfall.data.local.entity.location.PlaceCategory
import fr.datasaillance.nightfall.data.local.location.PlaceSuggestion
import fr.datasaillance.nightfall.data.local.location.PlaceSuggestionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * État de l'écran de configuration des lieux labellisés. Aucune dépendance réseau (C1)
 * — toutes les données viennent de Room (lieux + visites locales).
 */
data class LabeledPlacesUiState(
    val places: List<LabeledPlaceEntity> = emptyList(),
    val suggestions: List<PlaceSuggestion> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * ViewModel CRUD des lieux labellisés + génération des suggestions (clustering local).
 *
 * Ne reçoit volontairement aucun client réseau (Retrofit/OkHttp) — la résolution et les
 * suggestions sont 100 % locales (contrainte C1 zéro réseau, TA-8).
 */
class LabeledPlacesViewModel(
    private val labeledPlaceDao: LabeledPlaceDao,
    private val locationDao: LocationDao,
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<PlaceSuggestion>>(emptyList())

    val uiState: StateFlow<LabeledPlacesUiState> =
        combine(labeledPlaceDao.getAllFlow(), _suggestions) { places, suggestions ->
            LabeledPlacesUiState(places = places, suggestions = suggestions, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LabeledPlacesUiState(),
        )

    init {
        refreshSuggestions()
    }

    fun refreshSuggestions() {
        viewModelScope.launch {
            val visits = runCatching { locationDao.getAllVisits() }.getOrDefault(emptyList())
            _suggestions.value = PlaceSuggestionService.computeSuggestions(visits)
            Timber.i("scope=labeled_places suggestions_computed count=${_suggestions.value.size}")
        }
    }

    fun addPlace(
        label: String,
        category: PlaceCategory,
        lat: Double,
        lng: Double,
        radiusMeters: Int,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            runCatching {
                labeledPlaceDao.insert(
                    LabeledPlaceEntity(
                        label = label.trim(),
                        category = category,
                        lat = lat,
                        lng = lng,
                        radiusMeters = radiusMeters,
                        createdAtMs = now,
                        updatedAtMs = now,
                    )
                )
            }.onSuccess {
                Timber.i("scope=labeled_places place_added category=$category")
            }.onFailure {
                Timber.w("scope=labeled_places place_add_failed error=${it::class.simpleName}")
            }
        }
    }

    fun updatePlace(place: LabeledPlaceEntity) {
        viewModelScope.launch {
            runCatching { labeledPlaceDao.update(place.copy(updatedAtMs = System.currentTimeMillis())) }
                .onSuccess { Timber.i("scope=labeled_places place_updated id=${place.id}") }
                .onFailure { Timber.w("scope=labeled_places place_update_failed error=${it::class.simpleName}") }
        }
    }

    fun deletePlace(place: LabeledPlaceEntity) {
        viewModelScope.launch {
            runCatching { labeledPlaceDao.delete(place) }
                .onSuccess { Timber.i("scope=labeled_places place_deleted id=${place.id}") }
                .onFailure { Timber.w("scope=labeled_places place_delete_failed error=${it::class.simpleName}") }
        }
    }
}
