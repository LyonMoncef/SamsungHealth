---
title: "Labeled Places — lieux labellisés configurables"
slug: 2026-05-27-labeled-places
phase: B_lp
status: draft
created: 2026-05-27
branch: feat/labeled-places
tags: [android, room, location, cadran, rgpd]
implements: []
tested_by: []
---

# Spec — Labeled Places — lieux labellisés configurables

## Vision

Les `LocationVisitEntity` stockées dans Room ont un `placeName`/`address` souvent absent ou non standardisé (provenance Google Takeout). Il n'existe aujourd'hui aucune notion de "lieu connu" permettant de distinguer, pour l'anneau timeline du Cadran, une visite à un lieu habituel (domicile, travail, famille, vacances) d'un déplacement non qualifié.

Cette spec livre la couche de configuration qui permet à l'utilisateur de déclarer une liste de **lieux labellisés** (coordonnées + rayon + catégorie + libellé libre). Un service `PlaceResolver` résout ensuite chaque visite (`lat`/`lng`) contre cette liste par distance haversine. Une visite qui tombe dans le rayon d'au moins un lieu labellisé est dite **ancrée** — quelle que soit la catégorie (DOMICILE, TRAVAIL, FAMILLE, VACANCES, AUTRE). Le consommateur principal est le `RadialClockViewModel` (spec `cadran-v2`, DT-6) qui enrichit chaque `RadialVisit` d'un flag `anchored: Boolean` et d'un `placeLabel: String?` avant de les passer au canvas.

Le problème produit que cela résout : un utilisateur Non-24 qui passe plusieurs jours chez de la famille ou en vacances verrait ces séjours colorés en gris "non qualifié" sur le Cadran, alors qu'ils correspondent à un comportement ancré (sédentaire, lumière naturelle du lieu, pas de déplacement subi). La distinction visuele amber (ancré) vs couleur activité vs gris (non qualifié) n'a de sens que si l'utilisateur peut déclarer ses lieux habituels, y compris ceux hors domicile.

---

## Contexte et dépendances

### Consommateur principal

`cadran-v2` spec DT-6 et TA-10 : l'anneau timeline affiche :
- **Lieu ancré** : `accent-amber` `#d37c04`, opacité 0.85
- **Déplacement / activité** : couleur par type (palette `Activity` existante)
- **Lieu non labellisé** : `extras.textMuted` `#7a9aaa`, opacité 0.55

La logique de coloration existante (`placeColor()` dans `MultiDonutClock.kt` ligne 113–117) est une approximation temporaire basée sur le préfixe du `placeName` — elle sera remplacée par la résolution `PlaceResolver` une fois cette spec livrée.

### Modèle location existant

`LocationVisitEntity` (table `location_visits`, version DB 3+) contient :
- `lat: Double`, `lng: Double` — coordonnées en degrés décimaux
- `startMs: Long`, `endMs: Long` — timestamps epoch millis UTC
- `placeName: String?`, `address: String?` — souvent absents
- `placeId: String?` — identifiant Google, pas fiable comme clé métier

`ActivitySegmentEntity` (table `activity_segments`) contient des segments de mouvement — ils ne font pas l'objet d'une résolution de lieu (pas de stationnaire, pas de lieu d'arrivée canonique). L'anneau timeline les affiche par type d'activité indépendamment de `labeled-places`.

### Version DB courante

`NightfallDatabase` version 4 (migration v3→v4 : table `location_paths`). ⚠️ La spec `usage-sessions` (Phase B_us) introduit déjà une migration v4→v5 et doit être mergée **avant** celle-ci dans l'ordre d'implémentation convenu (`usage-sessions → labeled-places`). La migration de cette spec est donc **v5→v6** (à ré-ajuster si l'ordre de merge change).

La DB est chiffrée via SQLCipher + clé Android Keystore (`NightfallKeyManager`). `LabeledPlaceEntity` s'y insère sans modification du mécanisme de chiffrement — SQLCipher chiffre l'ensemble de la DB au niveau fichier, ce qui couvre `labeled_place` au même titre que les autres tables.

---

## Décisions techniques

### DT-1 — Entité Room : `LabeledPlaceEntity`

Nouvelle entité `@Entity(tableName = "labeled_place")` :

```kotlin
package fr.datasaillance.nightfall.data.local.entity.location

@Entity(
    tableName = "labeled_place",
    indices = [Index("category")],
)
data class LabeledPlaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Libellé libre saisi par l'utilisateur. Ex : "Maison de mamie", "Bureau Paris". */
    val label: String,

    /** Catégorie sémantique. Plusieurs lieux par catégorie autorisés. */
    val category: PlaceCategory,

    /** Latitude en degrés décimaux. */
    val lat: Double,

    /** Longitude en degrés décimaux. */
    val lng: Double,

    /** Rayon de match en mètres. Défaut : 150 m. */
    @ColumnInfo(name = "radius_meters") val radiusMeters: Int = 150,

    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at_ms") val updatedAtMs: Long = System.currentTimeMillis(),
)

enum class PlaceCategory { DOMICILE, TRAVAIL, FAMILLE, VACANCES, AUTRE }
```

Contraintes de la table :
- Pas d'index unique sur `(label, category)` — l'utilisateur peut avoir deux "Maison de mamie" à des adresses différentes si besoin.
- Pas de notion de "domicile unique" — plusieurs DOMICILE autorisés (ex. déménagement partiel, résidence secondaire).
- `category` stocké comme `TEXT` (Room convertit l'enum en nom via `@TypeConverter`).

### DT-2 — DAO : `LabeledPlaceDao`

```kotlin
package fr.datasaillance.nightfall.data.local.dao

@Dao
interface LabeledPlaceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(place: LabeledPlaceEntity): Long

    @Update
    suspend fun update(place: LabeledPlaceEntity)

    @Delete
    suspend fun delete(place: LabeledPlaceEntity)

    /** Flux réactif pour la liste de configuration — l'écran se met à jour en temps réel. */
    @Query("SELECT * FROM labeled_place ORDER BY category ASC, label ASC")
    fun getAllFlow(): Flow<List<LabeledPlaceEntity>>

    /** Snapshot synchrone utilisé par PlaceResolver (appelé hors UI thread). */
    @Query("SELECT * FROM labeled_place")
    suspend fun getAll(): List<LabeledPlaceEntity>

    @Query("SELECT COUNT(*) FROM labeled_place")
    suspend fun count(): Int
}
```

Conventions suivies : `suspend` pour les opérations ponctuelles, `Flow` pour la réactivité UI — identique à `UsageStatsDao` et `SleepDao`.

### DT-3 — Service : `PlaceResolver`

Classe pure (sans `Context`, sans coroutines, sans Room), testable en JUnit unitaire sans instrumentation.

```kotlin
package fr.datasaillance.nightfall.data.local.location

/** DTO immuable exposé au ViewModel et au canvas — pas d'entité Room. */
data class LabeledPlace(
    val id: Long,
    val label: String,
    val category: PlaceCategory,
    val lat: Double,
    val lng: Double,
    val radiusMeters: Int,
)

class PlaceResolver(private val places: List<LabeledPlace>) {

    /**
     * Retourne le lieu labellisé le plus proche dont le centre est à distance
     * ≤ radiusMeters du point (lat, lng). Si plusieurs matchent, le plus proche
     * gagne. Retourne null si aucun lieu ne matche.
     *
     * Déterministe et pur : même entrée → même sortie.
     */
    fun resolve(lat: Double, lng: Double): LabeledPlace?
}
```

**Algorithme haversine** : formule standard, rayon terrestre R = 6 371 000 m.

```
a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlng/2)
d = 2R * arcsin(√a)
```

Toutes les valeurs intermédiaires en radians. Pas de dépendance à `android.location.Location` (évite l'instrumentation en test unitaire).

**Règles de résolution :**
1. Filtrer les lieux dont `d ≤ radiusMeters`.
2. Parmi les matchs, retourner celui avec le `d` minimal.
3. Égalité de distance (cas quasi-impossible en pratique) → retourner le premier par ordre d'`id` croissant (déterminisme garanti).
4. `places` vide → retourne toujours `null` sans exception.

### DT-4 — Migration Room : v5 → v6

`Migration5to6` dans `NightfallDatabase` (vient après le `Migration4to5` de `usage-sessions`) :

```sql
CREATE TABLE IF NOT EXISTS `labeled_place` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `label` TEXT NOT NULL,
    `category` TEXT NOT NULL,
    `lat` REAL NOT NULL,
    `lng` REAL NOT NULL,
    `radius_meters` INTEGER NOT NULL DEFAULT 150,
    `created_at_ms` INTEGER NOT NULL,
    `updated_at_ms` INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS `index_labeled_place_category` ON `labeled_place` (`category`);
```

`NightfallDatabase.version` passe de 5 à 6. `LabeledPlaceEntity::class` est ajouté à la liste `entities = [...]`. `LabeledPlaceDao` est déclaré comme abstract fun. `Migration5to6` est ajoutée à `addMigrations(...)`.

### DT-5 — Enrichissement de `RadialVisit`

`RadialVisit` dans `MultiDonutClock.kt` est étendu de deux champs :

```kotlin
data class RadialVisit(
    val startMs: Long,
    val endMs: Long,
    val placeName: String,
    val anchored: Boolean = false,       // true si PlaceResolver a trouvé un match
    val placeLabel: String? = null,      // libellé du lieu labellisé matché, null sinon
)
```

Les deux champs ont des valeurs par défaut — compatibilité ascendante assurée : tous les appelants existants (tests, snapshots) compilent sans modification.

### DT-6 — Intégration dans `RadialClockViewModel`

Le ViewModel reçoit en injection une instance de `LabeledPlaceDao` (ou un `suspend fun getPlaces()` wrapper pour faciliter le test). Dans `buildDaysMap()`, après le chargement des visites :

```kotlin
val labeledPlaces = labeledPlaceDao.getAll().map { it.toLabeledPlace() }
val resolver = PlaceResolver(labeledPlaces)

// Dans le mapping LocationVisitEntity → RadialVisit :
val match = resolver.resolve(it.lat, it.lng)
RadialVisit(
    startMs = it.startMs,
    endMs = it.endMs,
    placeName = it.placeName ?: it.address ?: "—",
    anchored = match != null,
    placeLabel = match?.label,
)
```

Si `labeled_place` est vide (utilisateur n'a pas encore configuré de lieux), tous les `RadialVisit` ont `anchored = false` — l'anneau timeline affiche tout en gris `extras.textMuted`. Pas de crash, pas d'état dégradé spécial : c'est le comportement nominal "0 lieux configurés".

La logique `placeColor()` existante (`MultiDonutClock.kt` ligne 113–117) est conservée intacte dans cette PR — elle sert de fallback tant que l'intégration n'est pas câblée. L'utilisation de `anchored` dans le rendu canvas est dans le scope de `cadran-v2`, pas de cette spec.

### DT-7 — Suggestions depuis les visites fréquentes

L'écran de configuration propose deux voies de saisie des coordonnées :

**Voie A — Suggestions automatiques (clusters de visites)** : un `suspend fun computeSuggestions(visits: List<LocationVisitEntity>): List<PlaceSuggestion>` regroupe les visites par proximité (clustering naïf : rayon fixe de 200 m, seuil de récurrence ≥ 3 visites). Chaque cluster devient un `PlaceSuggestion` (lat/lng barycentre, nb visites, date dernière visite). L'utilisateur choisit un suggestion, saisit un libellé et une catégorie, confirme.

```kotlin
data class PlaceSuggestion(
    val lat: Double,
    val lng: Double,
    val visitCount: Int,
    val lastVisitMs: Long,
    val approximateAddress: String?,  // placeName/address le plus fréquent du cluster
)
```

**Voie B — Saisie manuelle** : formulaire lat/lng/rayon/label/catégorie. Pas de map picker (contrainte C1 — aucun appel réseau pour la géolocalisation inverse ou l'affichage de tuiles).

Les deux voies aboutissent au même `LabeledPlaceEntity` en base. L'algorithme de clustering est dans le ViewModel (`LabeledPlacesViewModel`), pas dans le DAO.

### DT-8 — Écran de configuration Compose (`LabeledPlacesScreen`)

Structure de l'écran :

```
LabeledPlacesScreen
├── TopAppBar (titre "Lieux connus", bouton retour)
├── Section "Suggestions" (lazy list, PlaceSuggestionCard par item)
│   └── PlaceSuggestionCard : barycentre estimé + nb visites + CTA "Ajouter"
├── Section "Mes lieux" (lazy list, LabeledPlaceCard par item)
│   └── LabeledPlaceCard : label + category chip + rayon + actions edit/delete
└── FAB "Ajouter manuellement" → bottom sheet AddEditPlaceSheet
```

`AddEditPlaceSheet` (bottom sheet modal) :
- Champ `label` (texte libre, obligatoire)
- Dropdown `category` (DOMICILE / TRAVAIL / FAMILLE / VACANCES / AUTRE)
- Champs `lat` / `lng` (Double, clavier numérique, validation ± plage WGS-84)
- Champ `radiusMeters` (Int, min 50, max 5000, défaut 150)
- Bouton "Enregistrer" / "Supprimer" (mode édition)

Tokens appliqués (DataSaillance) :
- `FontFamily.Default` (Roboto système — pas de Playfair sur Android natif)
- Background : `MaterialTheme.colorScheme.background` (dark `#191e22`, light `#ffffff`)
- Surface cards : `MaterialTheme.colorScheme.surface` (dark `#232e32`, light `#f4f8fa`)
- Accent CTA (FAB, bouton enregistrer) : `accent-amber` `#d37c04`
- Category chips : `accent-teal` `#0e9eb0` pour la catégorie sélectionnée
- Texte secondaire / rayon : `extras.textMuted` (`#7a9aaa` / `#81868b`)
- Bordures : `extras.border` (`#2e3d44` / `#d0dde3`)
- Interdit : `#6366f1`, gradients décoratifs, glows/halos
- `letterSpacing = 0.06.em` sur labels uppercase

Light ET dark mode obligatoires — `MaterialTheme` + tokens garantissent la bascule automatique.

---

## Contrat RGPD et sécurité (C1/C2)

### Sensibilité des données

Les coordonnées d'un domicile ou d'un lieu de travail constituent des données parmi les plus ré-identifiantes de l'ensemble du dataset. Elles permettent l'inférence de l'adresse domicile, des habitudes de déplacement, et par croisement avec les stages de sommeil, de l'état de santé à un niveau de précision supérieur à `LocationVisitEntity` seule.

### C1 — Zéro réseau

- Aucune requête réseau ne doit être émise lors de la configuration d'un lieu, de la résolution `PlaceResolver`, ou de l'affichage des suggestions.
- Pas de geocoding inverse (Google Maps API, Nominatim externe, ou tout autre service tiers).
- Pas de basemap réseau dans l'écran de configuration.
- Les `PlaceSuggestion` sont générées à partir des `LocationVisitEntity` locales uniquement.

### C2 — Chiffrement at-rest

La DB `nightfall.db` est chiffrée via SQLCipher + Android Keystore (`NightfallKeyManager`). La table `labeled_place` bénéficie de ce chiffrement au niveau fichier sans action supplémentaire.

**Note de risque à documenter** : le niveau de chiffrement actuel est SQLCipher full-DB (fichier chiffré), pas un chiffrement applicatif champ par champ comme côté serveur (AES-256-GCM). Les données `LocationVisitEntity` existantes ont le même niveau de protection. Si une exigence future impose un chiffrement applicatif des coordonnées de domicile (champ `lat`/`lng` chiffré même en cas de fuite du fichier DB avec la clé), cela constitue une évolution de `NightfallKeyManager` hors scope de cette spec — à traiter comme dépendance risque et loggé en backlog.

### C2 — Surface export/erase RGPD

`labeled_place` contient des données ré-identifiantes (coordonnées domicile/travail) couvertes par le droit d'accès et d'effacement RGPD. Si Nightfall évolue vers une architecture backend (sync ou multi-user), les endpoints `/rgpd/export` et `/rgpd/erase` (`server/security/rgpd.py`) doivent inclure `labeled_place` dans leur périmètre. Pour la version Android local-only actuelle, l'effacement est couvert par "Effacer mes données" (suppression de `nightfall.db`). Ce point doit être ré-évalué avant tout déploiement backend de la table.

### Absence d'affichage en clair des coordonnées GPS dans l'UI

L'écran de configuration affiche les coordonnées saisies par l'utilisateur (il les a lui-même entrées) — ceci est acceptable. En revanche, les coordonnées brutes de `LocationVisitEntity` ne sont **jamais** affichées dans l'UI (cohérent avec `cadran-v2` DT-7).

---

## Livrables

- [ ] **L1** — `PlaceCategory` enum + `LabeledPlaceEntity` (`labeled_place`) + `@TypeConverter` enum↔String
- [ ] **L2** — `LabeledPlaceDao` : insert/update/delete/getAllFlow/getAll/count
- [ ] **L3** — `LabeledPlace` DTO (immuable, sans Room) + extension `LabeledPlaceEntity.toLabeledPlace()`
- [ ] **L4** — `PlaceResolver` : resolve haversine, plus proche dans rayon, null si aucun
- [ ] **L5** — `PlaceSuggestion` DTO + `computeSuggestions()` (clustering naïf ≥ 3 visites dans 200 m)
- [ ] **L6** — Migration Room v5→v6 (`Migration5to6`, `CREATE TABLE labeled_place`) + bump version DB (dépend du merge préalable de `usage-sessions` v4→v5)
- [ ] **L7** — `NightfallDatabase` : ajout `LabeledPlaceEntity::class`, `abstract fun labeledPlaceDao()`, `Migration5to6`
- [ ] **L8** — `RadialVisit` étendu : `anchored: Boolean = false`, `placeLabel: String? = null` (rétrocompatible)
- [ ] **L9** — `RadialClockViewModel` : injection `LabeledPlaceDao`, résolution `PlaceResolver` dans `buildDaysMap()`
- [ ] **L10** — `LabeledPlacesViewModel` : `StateFlow<LabeledPlacesUiState>`, CRUD via DAO, `computeSuggestions()`
- [ ] **L11** — `LabeledPlacesScreen` Compose : liste, suggestions, FAB, `AddEditPlaceSheet`
- [ ] **L12** — Navigation : entrée `LabeledPlaces` dans `NavDestination` + `NavGraph`, accès depuis `SettingsScreen`
- [ ] **L13** — Vérification light + dark mode : contraste WCAG AA sur labels et chips

---

## Tests d'acceptation

### TA-1 — Match dans le rayon → ancré

**Given** un `PlaceResolver` initialisé avec un lieu DOMICILE à `(48.8566, 2.3522)`, `radiusMeters = 150`.
**When** `resolve(48.8567, 2.3524)` est appelé (point à ~18 m du centre).
**Then** retourne le lieu DOMICILE. `match != null`, `match.category == PlaceCategory.DOMICILE`.

### TA-2 — Hors rayon → null

**Given** le même `PlaceResolver` que TA-1.
**When** `resolve(48.8580, 2.3550)` est appelé (point à ~250 m du centre, hors rayon 150 m).
**Then** retourne `null`.

### TA-3 — Plusieurs candidats → le plus proche gagne

**Given** un `PlaceResolver` avec deux lieux A (centre à 80 m du point test, rayon 150) et B (centre à 120 m du point test, rayon 150). Les deux matchent.
**When** `resolve(lat_test, lng_test)` est appelé.
**Then** retourne le lieu A (le plus proche).

### TA-4 — Visite multi-jours sur lieu VACANCES → ancrée sur toute la durée

**Given** un `PlaceResolver` avec un lieu VACANCES à `(43.2965, 5.3698)`, `radiusMeters = 300`.
**When** une `LocationVisitEntity` a `startMs = T` et `endMs = T + 4 jours`, `lat = 43.2966`, `lng = 5.3700` (dans le rayon).
**Then** `resolver.resolve(43.2966, 5.3700)` retourne le lieu VACANCES (la durée de la visite n'affecte pas la résolution — elle est basée sur les coordonnées seules). Le `RadialVisit` mappé aura `anchored = true` et `placeLabel = <label du lieu>`.

### TA-5 — Liste vide → jamais de match

**Given** un `PlaceResolver` initialisé avec `places = emptyList()`.
**When** `resolve(48.8566, 2.3522)` est appelé.
**Then** retourne `null` sans exception.

### TA-6 — CRUD lieu : persistance et réflexion dans la résolution

**Given** la DB Room in-memory (test instrumenté) avec `LabeledPlaceDao`.
**When** un lieu est inséré via `dao.insert(LabeledPlaceEntity(...))`, puis `dao.getAll()` est appelé.
**Then** le lieu est présent dans la liste. Un `PlaceResolver` construit avec cette liste résout correctement une visite dans son rayon (TA-1 rejoué dynamiquement).

**When** le lieu est supprimé via `dao.delete(...)`, puis `dao.getAll()` est appelé.
**Then** la liste est vide. Le `PlaceResolver` reconstruit retourne `null` pour le même point.

### TA-7 — `getAllFlow` réactivité

**Given** la DB Room in-memory, un collecteur sur `dao.getAllFlow()`.
**When** un lieu est inséré via `dao.insert(...)`.
**Then** le flow émet une nouvelle liste contenant le lieu inséré sans que le collecteur ait besoin de se ré-abonner.

### TA-8 — Zéro appel réseau

**Given** l'écran `LabeledPlacesScreen` est ouvert avec des visites en DB.
**When** l'utilisateur consulte les suggestions, ajoute un lieu, édite un lieu, supprime un lieu.
**Then** aucun appel réseau n'est émis (vérifiable via `MockWebServer` / `OkHttp.Interceptor` zéro request ou audit des dépendances réseau du ViewModel — le ViewModel ne doit pas injecter `RetrofitClient` ni aucun client réseau).

### TA-9 — `RadialClockViewModel` enrichit correctement `RadialVisit`

**Given** `labeledPlaceDao.getAll()` retourne un lieu DOMICILE à `(48.8566, 2.3522)`, rayon 150 m.
**Given** `locationDao.getVisitsInRange(...)` retourne deux visites : une dans le rayon du DOMICILE, une hors rayon.
**When** `buildDaysMap()` est exécuté.
**Then** le `RadialVisit` correspondant à la visite dans le rayon a `anchored = true` et `placeLabel` non null. Le `RadialVisit` hors rayon a `anchored = false` et `placeLabel == null`.

### TA-10 — Rendu light + dark mode (écran config)

**Given** `uiMode = Configuration.UI_MODE_NIGHT_YES` (dark).
**Then** le fond de `LabeledPlacesScreen` est `#191e22`, les cards `#232e32`, les chips catégorie utilisent `accent-teal`, le FAB `accent-amber`. Aucune couleur hardcodée `#6366f1`, aucun gradient décoratif.

**Given** `uiMode = Configuration.UI_MODE_NIGHT_NO` (light).
**Then** fond `#ffffff`, cards `#f4f8fa`. Contraste WCAG AA respecté sur les labels de lieux et chips.

---

## Architecture des fichiers

```
android-app/app/src/main/java/fr/datasaillance/nightfall/
├── data/local/
│   ├── entity/location/
│   │   ├── LabeledPlaceEntity.kt        (L1 — entité + enum PlaceCategory)
│   │   └── PlaceCategory.kt             (L1 — ou inline dans LabeledPlaceEntity)
│   ├── dao/
│   │   └── LabeledPlaceDao.kt           (L2)
│   ├── location/
│   │   ├── LabeledPlace.kt              (L3 — DTO immuable)
│   │   ├── PlaceResolver.kt             (L4)
│   │   └── PlaceSuggestionService.kt    (L5 — computeSuggestions)
│   └── database/
│       └── NightfallDatabase.kt         (L6, L7 — migration + déclaration)
├── dataviz/radial/
│   └── MultiDonutClock.kt               (L8 — RadialVisit étendu)
├── viewmodel/
│   ├── radial/
│   │   └── RadialClockViewModel.kt      (L9 — injection LabeledPlaceDao)
│   └── places/
│       └── LabeledPlacesViewModel.kt    (L10)
└── ui/screens/places/
    └── LabeledPlacesScreen.kt           (L11)
```

---

## Suite naturelle

### Consommateur immédiat

`cadran-v2` : une fois cette spec livrée, `RadialVisit.anchored` est disponible. La logique de rendu dans `MultiDonutClock.kt` peut remplacer l'appel `placeColor(visit.placeName)` par une branche `if (visit.anchored) accentAmber else extras.textMuted` — débloquant TA-10 de `cadran-v2`.

### Itération future : détection automatique du domicile

Une heuristique "lieu le plus fréquent la nuit (entre 1h et 6h)" sur les `LocationVisitEntity` pourrait pré-remplir les suggestions avec une confiance haute, permettant à l'utilisateur de labelliser son domicile en un tap sans chercher ses coordonnées. Cette logique serait une extension de `computeSuggestions()` avec un scoring nuit. Non dans scope de cette PR — à logguer en backlog.

### Chiffrement applicatif des coordonnées (risque C2)

Si une exigence future impose un chiffrement champ-par-champ des coordonnées `lat`/`lng` de `LabeledPlaceEntity` (au-delà du SQLCipher full-DB actuel), cela impacte `NightfallKeyManager` et nécessite un `@TypeConverter` chiffrant. À traiter dans une spec dédiée avant tout déploiement multi-utilisateur.
