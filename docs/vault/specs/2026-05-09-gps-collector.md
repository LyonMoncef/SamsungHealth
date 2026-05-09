---
title: "GPS Collector — collecte locale des données de localisation"
slug: 2026-05-09-gps-collector
status: draft
created: 2026-05-09
implements: []
tested_by: []
---

# Spec — GPS Collector

## Vision

Capter le **3e signal personnel** (après sommeil et bien-être numérique) pour le local-first de Nightfall : les données de localisation. Objectif final = corréler dans une vue unifiée :
- Quand je dors, où je suis, ce que j'utilise comme apps
- Détecter des patterns : "je dors mieux à la campagne", "le drift circadien suit mes voyages", "mes phases d'usage YouTube tardives correspondent à des soirs sans sortie"

## Sources de données envisagées

| Source | Profondeur | Effort | Risques | Recommandation |
|---|---|---|---|---|
| **Google Takeout JSON** | Tout l'historique passé | Bas (parser JSON) | Format Google peut changer | ✅ Phase A_gps |
| **FusedLocationProvider** continu | À partir de l'install | Moyen (perm BG, batterie) | Conso ~1-2%/j | Phase B_gps |
| **Lecture cache Google Maps local** | Dernières semaines | N/A — root only | Casse Knox | ❌ ignoré |

Phase A_gps = parser Takeout. Phase B_gps = collecte continue. Phase C_gps = UI. Phase D_gps = fusion (avec sleep + usage).

## Décisions techniques

### Format Google Takeout

Depuis fin 2024 Google a déprécié la timeline cloud. L'export passe désormais par :
- **Sur device** : Google Maps → photo profil → "Vos données dans Maps" → Exporter
- **takeout.google.com** (legacy) si l'historique cloud est encore activé

L'export produit deux types de structure :
1. **`Semantic Location History/<année>/<année>_<mois>.json`** — format moderne, riche : `placeVisit` + `activitySegment`
2. **`Records.json`** — samples lat/lng bruts avec timestamps et accuracy

Phase A_gps cible **#1** (Semantic Location History). #2 sera traité plus tard si besoin (volumineux : ~100k samples par an).

### Modèle de données

Deux entities distinctes :

**LocationVisitEntity** (`placeVisit`) :
- Visite : un POI où on est resté un temps significatif
- Champs : `start_ms`, `end_ms`, `lat`, `lng`, `place_id?`, `place_name?`, `address?`, `confidence`
- Index unique `(start_ms, end_ms, lat, lng)` pour idempotence

**ActivitySegmentEntity** (`activitySegment`) :
- Trajet entre 2 visites : marche, voiture, vélo, transport, etc.
- Champs : `start_ms`, `end_ms`, `start_lat/lng`, `end_lat/lng`, `activity_type`, `distance_m`, `confidence`
- Index unique `(start_ms, end_ms, activity_type)`

### Parsing
- Pas de dépendance ajoutée — utilise `org.json.JSONObject` (built-in Android), tolérant aux variations de schéma
- Parser fichier-par-fichier ou ZIP entier (Takeout fournit un ZIP qu'on extrait)
- Idempotent via Room `OnConflictStrategy.IGNORE`

### Stockage local
- Ajout dans `NightfallDatabase` Room — migration v1 → v2 (cf. réserve : la branche `feat/usage-stats-collector` parallèle a aussi sa propre v2 ; la fusion finale fera une v3 unifiée)
- Chiffré au repos via SQLCipher déjà en place

### Permission

**Phase A_gps** ne demande **aucune permission** runtime — c'est un import de fichier que l'utilisateur a téléchargé manuellement. Même flow que Samsung Health import : SAF picker → ZIP → parse en mémoire → écrit en Room.

**Phase B_gps** (continue) demandera `ACCESS_FINE_LOCATION` + éventuellement `ACCESS_BACKGROUND_LOCATION` (Android 10+). Hors scope de cette spec.

## Phases

### Phase A_gps — Foundation + import Takeout
- Spec + entities + DAO
- Migration Room v1 → v2 (location_visits + activity_segments)
- `TakeoutTimelineParser` — parse JSON Semantic Location History
- `LocalLocationImportService` — pipeline `ZIP → parse → Room`
- Tests Robolectric avec fixtures JSON inline

### Phase B_gps — Collecte continue
- `FusedLocationProvider` + WorkManager / ForegroundService selon trade-off batterie
- Permission `ACCESS_FINE_LOCATION` flow
- Densification de l'historique futur (1 sample / 5min)

### Phase C_gps — UI carte
- Vue carte dédiée Nightfall — à designer (Mapbox / OSMdroid offline ?)
- Heatmap des visites
- Timeline géo (où j'étais à telle heure)

### Phase D_gps — Fusion sleep + usage + gps
- Vue corrélée jour-par-jour
- Score "hygiène nocturne" intégrant lieu (à la maison vs ailleurs) + apps utilisées + qualité sommeil
- Spec dédiée plus tard

## Livrables Phase A_gps

- [ ] `data/local/entity/location/LocationVisitEntity.kt`
- [ ] `data/local/entity/location/ActivitySegmentEntity.kt`
- [ ] `data/local/dao/LocationDao.kt`
- [ ] Migration NightfallDatabase v1 → v2 (location tables)
- [ ] `data/local/location/TakeoutTimelineParser.kt`
- [ ] `data/local/location/LocalLocationImportService.kt`
- [ ] Tests Robolectric in-memory (parser + service)

## Tests d'acceptation

1. **Parse fichier Semantic Location History** — given un JSON valide avec 3 placeVisit et 2 activitySegment, when `TakeoutTimelineParser.parse()`, then retourne `(3 visits, 2 segments)` avec champs corrects (lat/lng, timestamps, activityType).

2. **Idempotence import** — given un même JSON importé 2 fois consécutives, when on regarde la DB, then aucun doublon (index unique respecté), 2e import retourne `inserted=0`.

3. **Tolérance aux variations de schéma** — given un JSON avec champs optionnels manquants (ex: pas de `placeId`, pas de `address`), when parse, then l'entrée est créée avec valeurs nullables, pas de crash.

4. **Migration Room v1→v2** — given une DB v1 (sleep + hr + steps + exercise), when ouverture en v2, then les nouvelles tables `location_visits` + `activity_segments` créées, données existantes intactes.

## Risques et mitigations

- **Format Takeout évolue** → parser tolérant (org.json), tests sur fixtures représentatives, `TODO` documentés pour cas limites observés
- **Volumes énormes** (export 5 ans = 100k+ entrées) → bulk insert par batches de 1000 (cf. pattern Phase B sleep_stage)
- **Chevauchement entities futures** (ex: si Phase B_gps stocke aussi des visits via sample agrégé) → namespacer en `_takeout` vs `_live` pour éviter collision, ou unifier via une colonne `source`
- **Conflit migration Room avec branche `feat/usage-stats-collector`** → les 2 branches font v2 indépendamment ; à la consolidation finale, on rebase l'une sur l'autre et on bump à v3 avec toutes les tables. Documenté dans la spec parallèle aussi.

## Lien avec autres specs

- Hérite de `2026-05-09-local-first-migration.md` (architecture Room + SQLCipher + import service pattern)
- Branche soeur `2026-05-09-usage-stats-collector.md` (autre signal local)
- Sera référencée par `2026-XX-personal-correlation-dashboard.md` (Phase D commune)
