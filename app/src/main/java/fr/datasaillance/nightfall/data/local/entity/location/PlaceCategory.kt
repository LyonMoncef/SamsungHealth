package fr.datasaillance.nightfall.data.local.entity.location

/**
 * Catégorie sémantique d'un lieu labellisé. Plusieurs lieux par catégorie sont
 * autorisés (ex. plusieurs DOMICILE en cas de déménagement partiel ou résidence
 * secondaire). Stockée en base comme `TEXT` via [PlaceCategoryConverter].
 */
enum class PlaceCategory { DOMICILE, TRAVAIL, FAMILLE, VACANCES, AUTRE }
