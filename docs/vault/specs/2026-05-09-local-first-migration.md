---
title: "Migration local-first — données santé sur le téléphone"
slug: 2026-05-09-local-first-migration
status: draft
created: 2026-05-09
implements: []
tested_by: []
---

# Spec — Migration local-first des données santé

## Vision

Aujourd'hui Nightfall stocke les données Art.9 chiffrées sur le VPS de l'utilisateur. C'est un **compromis acceptable** mais pas l'idéal du `C1 — Local-first absolu` de la VISION. Le VPS introduit :
- Une dépendance réseau (timeout, latence, panne)
- Un point de fuite potentiel (logs OS, snapshots VM, sauvegardes hébergeur)
- Un coût opérationnel (maintenance, mises à jour, certifs)
- Un anti-pattern produit : un import de 60k stages en 30s sur un VPS faible vs millisecondes en SQLite local

**Cible** : toutes les données santé restent sur le téléphone. Le serveur FastAPI devient **optionnel** (mode "backup chiffré E2EE" pour multi-device).

## Décisions techniques

### Stockage local
- **Room (SQLAlchemy → Kotlin)** : ORM officiel Jetpack, schémas miroirs des modèles `server/db/models.py`
- **SQLCipher** : chiffrement transparent au repos avec clé Android Keystore (KeyAlias dédié, hardware-backed quand dispo)
- Tables : `users` (1 ligne), `sleep_sessions`, `sleep_stages`, `heart_rate_hourly`, `steps_hourly`, `exercise_sessions`, + `audit_log` (RGPD C2)

### Parsing CSV Samsung
- Portage Kotlin du `server/services/csv_import.py` (parse_samsung_csv + parse_*_rows)
- Mêmes règles : skip ligne metadata, `utf-8-sig` pour BOM, mapping stage codes 40001-40004, etc.
- Bulk insert Room par batch de 1000 avec `OnConflictStrategy.IGNORE` (équivalent `ON CONFLICT DO NOTHING`)

### Architecture Repository
- `SleepRepository` interface inchangée — l'app ne sait pas si la donnée vient de Room ou de Retrofit
- 2 implémentations : `LocalSleepRepository` (Room) et `RemoteSleepRepository` (existant)
- `RepositoryFactory` choisit selon flag utilisateur (par défaut : local)

### Migration des données existantes
- Premier lancement post-migration : si VPS configuré et données présentes, l'app propose un export/import depuis le VPS vers le local
- Téléchargement en streaming par fenêtre de 30 jours pour éviter timeout sur gros historiques
- Vérification d'intégrité par count avant suppression côté serveur (optionnelle)

### Mode backup E2EE (Phase D)
- Hors scope de cette spec — esquissé pour ne pas peindre dans un coin
- Idée : chiffrement côté client avec clé dérivée d'un passphrase, upload du blob chiffré sur le VPS
- Le serveur ne voit jamais les clés ni le contenu déchiffré
- Implique signature/MAC pour intégrité

## Phases

### Phase A — Stockage local (Foundation)
**Livrables**
- `android-app/app/src/main/java/.../data/local/database/NightfallDatabase.kt` (Room DB v1)
- Entities : `SleepSessionEntity`, `SleepStageEntity`, `HeartRateHourlyEntity`, `StepsHourlyEntity`, `ExerciseSessionEntity`
- DAOs : `SleepDao`, `HeartRateDao`, `StepsDao`, `ExerciseDao` (read + bulk write)
- Init SQLCipher avec clé depuis Android Keystore (KeyAlias `nightfall_db_key`)
- Tests Room (Robolectric in-memory) sur les DAOs

**Scope**
- Aucun changement UI, aucun import, aucun read business
- Juste l'infra DB qui tourne en parallèle du code existant

### Phase B — Imports en local
**Livrables**
- Port Kotlin de `parse_samsung_csv` + `parse_sleep_rows` + `parse_sleep_stage_rows` + `parse_heartrate_rows` + `parse_steps_rows` + `parse_exercise_rows`
- `LocalImportService` qui prend le ZIP Samsung depuis SAF, extrait les CSV en mémoire, parse, écrit en Room
- `ImportRepositoryImpl` modifié : flag `useLocalImport` (par défaut true), bypass complet du `NightfallApi.importXxx`
- Tests unit : 1 test par parser, fixtures CSV inline (équivalent des tests `tests/server/test_import_csv_multipart.py`)

**Scope**
- L'écran Import écrit en local dès qu'on touche ce flag
- Le VPS reste fonctionnel pour les écrans qui n'ont pas migré

### Phase C — Lectures locales
**Livrables**
- `LocalSleepRepository` implémentant `SleepRepository` via `SleepDao`
- DI bascule sur `LocalSleepRepository` quand `useLocalRead = true`
- Migration progressive des écrans : Sleep → Hypnogramme → Timeline (ordre inverse de criticité)
- Tests Compose Robolectric : Hypnogramme en < 200ms avec fixture de 60k stages

**Scope**
- Backend FastAPI continue à tourner (utilisé par d'autres écrans pas encore migrés ou en fallback)
- À la fin de cette phase, aucun écran ne lit le VPS pour les données santé

### Phase D — VPS optionnel / backup E2EE
**Livrables (hors scope détaillé ici, à re-spec)**
- Mode "VPS désactivé" qui retire complètement les appels réseau santé
- Optionnel : feature backup E2EE (passphrase user, blob chiffré stocké sur VPS)
- Documentation utilisateur : comment migrer depuis l'ancien mode VPS vers local

## Livrables (vue d'ensemble par phase)

- [ ] Phase A — Room + SQLCipher + DAOs + tests
- [ ] Phase B — port Kotlin des parsers Samsung CSV + tests + flag d'import local
- [ ] Phase C — `LocalSleepRepository` + bascule progressive des écrans + tests
- [ ] Phase D — désactivation VPS / backup E2EE optionnel (re-spec dédiée)

## Tests d'acceptation

1. **Import local sleep_stage en < 5s pour 60k rows** — given un ZIP Samsung valide, when l'utilisateur déclenche l'import en mode local, then les stages sont en DB Room en moins de 5 secondes (vs 30s+ via VPS aujourd'hui).
2. **Hypnogramme s'affiche en < 200ms** — given une nuit avec 100 stages en DB Room locale, when l'utilisateur clique sur la nuit dans la Timeline, then l'écran Hypnogramme est rendu en moins de 200ms (vs 30s aujourd'hui).
3. **Aucun appel réseau santé en mode local** — given le mode local activé, when on inspecte les requêtes HTTP sortantes pendant un import + lecture de toutes les vues santé, then aucune requête vers `/api/sleep`, `/api/heartrate`, `/api/steps`, `/api/exercise` n'est émise.
4. **Données chiffrées au repos** — given une DB Room initialisée, when on inspecte le fichier `nightfall.db` sur le filesystem Android, then son contenu n'est pas lisible en clair (SQLCipher actif).
5. **Désinstallation = suppression** — given des données santé en DB locale, when l'utilisateur désinstalle l'app, then aucune trace ne subsiste sur le téléphone (cohérent avec C2 droit à l'oubli).

## Suite naturelle

Après cette migration, deux pistes :
- **Phase 6 — Synchronisation multi-device** via le mode backup E2EE — partager données entre tablette et téléphone du même user, sans confier le clair au VPS.
- **Phase 7 — Wear OS / Garmin** — étendre la collecte directement depuis montre Android, court-circuitant complètement Samsung Health Cloud.

## Risques et mitigations

- **Perte clé Keystore (factory reset, perte device)** → données illisibles. Mitigation : export régulier en clair (avec confirm utilisateur fort) ou backup E2EE.
- **Schéma Room évolue** → migrations Room bien documentées + tests de upgrade. Pattern identique à Alembic.
- **Bug parser Samsung silencieux côté Android** → tests unit avec fixtures réelles (un set anonymisé du ZIP utilisateur de référence).
- **Effort de port important (~3-4 semaines)** → faisable en 4 PRs séparées (1 par phase). Phase A et B peuvent tourner en parallèle du backend, pas besoin de tout migrer en bloc.

## Lien avec les contraintes

- **C1 — Local-first absolu** : ✅ atteint complètement
- **C2 — RGPD santé maximum** : ✅ chiffrement Art.9 via SQLCipher + Keystore (équivalent fonctionnel de l'AES-256-GCM serveur)
- **C3 — Sécurité intégrée** : `pentester` doit être étendu pour auditer Room/SQLCipher au lieu de juste FastAPI
- **No LLM on health data** : inchangé
