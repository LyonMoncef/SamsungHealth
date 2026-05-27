package fr.datasaillance.nightfall.ui.screens.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import fr.datasaillance.nightfall.data.local.entity.location.LabeledPlaceEntity
import fr.datasaillance.nightfall.data.local.entity.location.PlaceCategory
import fr.datasaillance.nightfall.data.local.location.PlaceSuggestion
import fr.datasaillance.nightfall.ui.theme.DataSaillance
import fr.datasaillance.nightfall.viewmodel.places.LabeledPlacesUiState

/* ============================================================
 * LabeledPlacesScreen — configuration des lieux connus (DT-8).
 *
 * Liste réactive des lieux + suggestions issues du clustering local + saisie
 * manuelle via bottom sheet. Pas de map picker (C1 — aucun réseau / tuile / geocoding).
 * Tokens DataSaillance, FontFamily.Default (Roboto), light + dark via MaterialTheme.
 * Les coordonnées affichées sont celles saisies par l'utilisateur — jamais les
 * coords brutes de LocationVisitEntity.
 * ============================================================ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabeledPlacesScreen(
    state: LabeledPlacesUiState,
    onAddPlace: (label: String, category: PlaceCategory, lat: Double, lng: Double, radiusMeters: Int) -> Unit,
    onUpdatePlace: (LabeledPlaceEntity) -> Unit,
    onDeletePlace: (LabeledPlaceEntity) -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.colorScheme
    val extras = DataSaillance.extras

    var editing by remember { mutableStateOf<LabeledPlaceEntity?>(null) }
    var prefill by remember { mutableStateOf<PlaceSuggestion?>(null) }
    var sheetOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = { Text("Lieux connus", fontFamily = FontFamily.Default) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.background,
                    titleContentColor = palette.onBackground,
                    navigationIconContentColor = palette.onBackground,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editing = null; prefill = null; sheetOpen = true },
                containerColor = palette.secondary,
                contentColor = palette.onSecondary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter manuellement")
            }
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues16,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.suggestions.isNotEmpty()) {
                item { SectionLabel("Suggestions", extras.textMuted) }
                items(state.suggestions, key = { "sug-${it.lat}-${it.lng}" }) { sug ->
                    PlaceSuggestionCard(
                        suggestion = sug,
                        onAdd = { prefill = sug; editing = null; sheetOpen = true },
                    )
                }
            }

            item { SectionLabel("Mes lieux", extras.textMuted) }
            if (state.places.isEmpty()) {
                item {
                    Text(
                        "Aucun lieu connu. Ajoutez-en un avec le bouton +.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extras.textMuted,
                        fontFamily = FontFamily.Default,
                    )
                }
            } else {
                items(state.places, key = { it.id }) { place ->
                    LabeledPlaceCard(
                        place = place,
                        onEdit = { editing = place; prefill = null; sheetOpen = true },
                        onDelete = { onDeletePlace(place) },
                    )
                }
            }
        }
    }

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = palette.surface,
        ) {
            AddEditPlaceSheet(
                existing = editing,
                prefill = prefill,
                onSave = { label, category, lat, lng, radius ->
                    val current = editing
                    if (current != null) {
                        onUpdatePlace(
                            current.copy(
                                label = label,
                                category = category,
                                lat = lat,
                                lng = lng,
                                radiusMeters = radius,
                            )
                        )
                    } else {
                        onAddPlace(label, category, lat, lng, radius)
                    }
                    sheetOpen = false
                },
                onDelete = editing?.let { place -> { onDeletePlace(place); sheetOpen = false } },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        letterSpacing = 0.06.em,
        fontFamily = FontFamily.Default,
    )
}

@Composable
private fun PlaceSuggestionCard(
    suggestion: PlaceSuggestion,
    onAdd: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val extras = DataSaillance.extras
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    suggestion.approximateAddress ?: "Lieu fréquent",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.onSurface,
                    fontFamily = FontFamily.Default,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${suggestion.visitCount} visites",
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.textMuted,
                    fontFamily = FontFamily.Default,
                )
            }
            OutlinedButton(onClick = onAdd) {
                Text("Ajouter", fontFamily = FontFamily.Default)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledPlaceCard(
    place: LabeledPlaceEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = MaterialTheme.colorScheme
    val extras = DataSaillance.extras
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    place.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.onSurface,
                    fontFamily = FontFamily.Default,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                place.category.name,
                                fontFamily = FontFamily.Default,
                                letterSpacing = 0.06.em,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = palette.primary.copy(alpha = 0.16f),
                            labelColor = palette.primary,
                        ),
                    )
                    Text(
                        "${place.radiusMeters} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.textMuted,
                        fontFamily = FontFamily.Default,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Éditer", tint = palette.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = palette.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditPlaceSheet(
    existing: LabeledPlaceEntity?,
    prefill: PlaceSuggestion?,
    onSave: (label: String, category: PlaceCategory, lat: Double, lng: Double, radiusMeters: Int) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val palette = MaterialTheme.colorScheme
    val extras = DataSaillance.extras

    var label by remember { mutableStateOf(existing?.label ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: PlaceCategory.DOMICILE) }
    var latText by remember {
        mutableStateOf(existing?.lat?.toString() ?: prefill?.lat?.toString() ?: "")
    }
    var lngText by remember {
        mutableStateOf(existing?.lng?.toString() ?: prefill?.lng?.toString() ?: "")
    }
    var radiusText by remember { mutableStateOf((existing?.radiusMeters ?: 150).toString()) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val lat = latText.toDoubleOrNull()
    val lng = lngText.toDoubleOrNull()
    val radius = radiusText.toIntOrNull()
    val valid = label.isNotBlank() &&
        lat != null && lat in -90.0..90.0 &&
        lng != null && lng in -180.0..180.0 &&
        radius != null && radius in 50..5000

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (existing != null) "Éditer le lieu" else "Nouveau lieu",
            style = MaterialTheme.typography.titleMedium,
            color = palette.onSurface,
            fontFamily = FontFamily.Default,
        )

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Libellé", fontFamily = FontFamily.Default) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
        ) {
            OutlinedTextField(
                value = category.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Catégorie", fontFamily = FontFamily.Default) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
            ) {
                PlaceCategory.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name, fontFamily = FontFamily.Default) },
                        onClick = { category = option; categoryExpanded = false },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text("Latitude", fontFamily = FontFamily.Default) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = { lngText = it },
                label = { Text("Longitude", fontFamily = FontFamily.Default) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = radiusText,
            onValueChange = { radiusText = it },
            label = { Text("Rayon (m, 50–5000)", fontFamily = FontFamily.Default) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = { if (valid) onSave(label.trim(), category, lat!!, lng!!, radius!!) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.secondary,
                contentColor = palette.onSecondary,
            ),
        ) {
            Text("Enregistrer", fontFamily = FontFamily.Default)
        }

        if (onDelete != null) {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Supprimer", color = palette.error, fontFamily = FontFamily.Default)
            }
        }
    }
}

private val PaddingValues16 = PaddingValues(16.dp)
