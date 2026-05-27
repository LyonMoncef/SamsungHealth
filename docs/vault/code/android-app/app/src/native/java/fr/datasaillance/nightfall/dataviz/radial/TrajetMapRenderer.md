---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/TrajetMapRenderer.kt
git_blob: bc5f45db4f1541884f097a22ea3466ae2e23e41c
last_synced: '2026-05-27T06:16:08Z'
loc: 37
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/TrajetMapRenderer.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/TrajetMapRenderer.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/dataviz/radial/TrajetMapRenderer.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `TrajetMapRenderer` (class) — lines 18-25
- `Render` (function) — lines 19-24
- `Render` (function) — lines 29-36
