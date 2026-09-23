package fr.datasaillance.nightfall.dataviz.radial

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/* ============================================================
 * TrajetMapRenderer — interface carte déférée (DT-7 / L9).
 *
 * Contrainte C1 : aucune coordonnée GPS ne sort de l'appareil dans cette PR.
 * La carte des trajets est branchée derrière une interface pluggable. Le
 * rendu par défaut (`NullTrajetMapRenderer`) est un no-op : la carte
 * contextuelle Timeline n'affiche que du texte (lieux, horaires, distance).
 *
 * Un futur renderer canvas local (tracé normalisé, fond neutre, zéro réseau)
 * pourra se brancher ici. Tout basemap réseau (Google Maps, MapLibre tuiles
 * externes) est une dérogation C1 explicite à documenter dans une spec séparée.
 * ============================================================ */
interface TrajetMapRenderer {
    @Composable
    fun Render(
        visits: List<RadialVisit>,
        activities: List<RadialActivity>,
        modifier: Modifier,
    )
}

/** Renderer par défaut : ne produit aucune vue, aucun réseau (C1). */
object NullTrajetMapRenderer : TrajetMapRenderer {
    @Composable
    override fun Render(
        visits: List<RadialVisit>,
        activities: List<RadialActivity>,
        modifier: Modifier,
    ) {
        // no-op — aucun basemap, aucune coordonnée affichée.
    }
}
