---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/PlaceCategory.kt
git_blob: d9bc8e40f78d4a49fdc3f7b6f61ddb2b0f62ce83
last_synced: '2026-05-27T05:17:18Z'
loc: 8
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/PlaceCategory.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/PlaceCategory.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/entity/location/PlaceCategory.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.entity.location

/**
 * Catégorie sémantique d'un lieu labellisé. Plusieurs lieux par catégorie sont
 * autorisés (ex. plusieurs DOMICILE en cas de déménagement partiel ou résidence
 * secondaire). Stockée en base comme `TEXT` via [PlaceCategoryConverter].
 */
enum class PlaceCategory { DOMICILE, TRAVAIL, FAMILLE, VACANCES, AUTRE }
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `PlaceCategory` (class) — lines 8-8
