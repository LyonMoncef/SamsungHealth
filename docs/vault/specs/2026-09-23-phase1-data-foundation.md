---
title: "Phase 1 — Fondations données on-device (contrat, Health Connect, export)"
slug: 2026-09-23-phase1-data-foundation
phase: 1
status: validated
created: 2026-09-23
branch: feat/phase1-data-foundation
tags: [android, core, health-connect, contrat, export, rgpd]
implements: []
tested_by: []
---

# Spec — Phase 1 — Fondations données on-device

## Vision

Nightfall lit désormais ses données directement dans **Health Connect**, sans serveur et sans export CSV manuel (ADR-4). Cette spec pose les trois fondations dont dépend toute la suite :

1. **1.1 — Le contrat de données** : les types qui représentent une session de sommeil et une mesure de pas, dans un module `core/` en Kotlin pur. C'est la pièce « gelée » partagée entre l'app et le notebook de validation (Phase 2).
2. **1.2 — L'ingestion Health Connect** : lire **tout l'historique** (permission `READ_HEALTH_DATA_HISTORY`, celle dont l'absence plafonnait l'ancienne app à 30 jours) et le convertir vers le contrat. Remplace l'import CSV Samsung.
3. **1.3 — L'export** : écrire ces données brutes dans une archive ZIP (CSV + manifeste) que le notebook sait relire.

Le problème produit que cela résout : l'ancienne app affichait des données incomplètes (30 jours) et déjà transformées par Samsung. Le contrat ne transporte que des **faits bruts** ; toute interprétation (quelle nuit, sieste ou sommeil principal, dédup entre sources) devient un calcul explicite, validé dans le notebook avant d'être porté en Kotlin.

Confirmation terrain (2026-09-23) : darkhour, qui lit Health Connect avec cette permission, remonte **1146 sessions depuis le 2024-07-08** sur le téléphone de l'utilisateur. C'est la valeur de référence pour valider la 1.2.

---

## Contexte et dépendances

- **Structure** : projet Gradle à la racine (`app/`), purge du code serveur faite (PR #106). Compilation et tests unitaires exécutables sur S1.
- **Lecture directe** (décision 2026-09-23) : les données sommeil et pas ne sont **pas recopiées dans Room**. Health Connect est la source ; l'export ZIP sert de sauvegarde ponctuelle. Les données d'usage écran et GPS restent dans Room (elles ne viennent pas de Health Connect).
- **Bibliothèque** : `androidx.health.connect:connect-client:1.1.0` (dernière stable, la 1.2 est en alpha). Constantes vérifiées sur la bibliothèque réelle (voir DT-2).
- **Oracle** : darkhour (MIT) sert de référence de comportement ; aucun code n'en est copié.

---

## Décisions techniques

### DT-1 — Module `core/` en Kotlin pur

- Nouveau module Gradle `:core`, plugin `org.jetbrains.kotlin.jvm` (même version Kotlin que l'app, 2.1.0), cible JVM 17 comme l'app.
- **Aucune dépendance Android**, ni Health Connect, ni bibliothèque tierce : Kotlin standard + `java.time`. Tests en JUnit simple.
- Package `fr.datasaillance.nightfall.core`. `app` en dépend via `implementation(project(":core"))`.
- Ce que `core/` contient en Phase 1 : le contrat (DT-2) et l'écriture/lecture CSV (DT-4). Les algorithmes (Phase 3) viendront s'y ajouter.

Pourquoi : le contrat et les calculs doivent être testables sans téléphone ni Robolectric, et relisibles sans connaître Android.

### DT-2 — Contrat : faits bruts, calqués sur Health Connect (v2 depuis le 2026-09-24 pour les pas)

```kotlin
data class SleepRecord(
    val id: String,                    // metadata.id Health Connect (stable)
    val start: Instant,
    val end: Instant,
    val startOffset: ZoneOffset?,      // fuseau au moment de l'enregistrement, si connu
    val endOffset: ZoneOffset?,
    val source: String,                // metadata.dataOrigin.packageName (ex. com.sec.android.app.shealth)
    val recordingMethod: RecordingMethod,
    val lastModified: Instant,
    val stages: List<SleepStage>,      // peut être vide
)

data class SleepStage(val start: Instant, val end: Instant, val type: StageType)

// v2 (2026-09-24) — remplace StepsInterval (v1, enregistrements bruts)
data class HourlySteps(
    val start: Instant,                // début de la tranche d'une heure (UTC)
    val end: Instant,
    val offset: ZoneOffset?,           // fuseau de la tranche, si Health Connect le connaît
    val count: Long,                   // total calculé par Health Connect, sources dédoublonnées
    val sources: List<String>,         // apps ayant contribué, triées
)

enum class StageType { UNKNOWN, AWAKE, SLEEPING, OUT_OF_BED, LIGHT, DEEP, REM, AWAKE_IN_BED }
enum class RecordingMethod { UNKNOWN, ACTIVELY_RECORDED, AUTOMATICALLY_RECORDED, MANUAL_ENTRY }
```

Règles :
- **Correspondance 1:1 avec Health Connect.** Les 8 stades et les 4 méthodes reprennent exactement les constantes de `connect-client` 1.1.0 (`STAGE_TYPE_UNKNOWN=0` … `STAGE_TYPE_AWAKE_IN_BED=7`, `RECORDING_METHOD_UNKNOWN=0` … `RECORDING_METHOD_MANUAL_ENTRY=3`). On ne fusionne pas `AWAKE` et `AWAKE_IN_BED` : c'est une interprétation, elle viendra plus tard.
- **Aucune validation sémantique dans le contrat.** Une session anormale (très courte, stades hors session) est conservée telle quelle : c'est le notebook qui décidera comment la traiter, en la voyant.
- **Versionné.** Toute modification du contrat incrémente sa version et met à jour cette spec.
- **v2 (2026-09-24) — les pas deviennent des totaux horaires.** Sur le téléphone de référence, un enregistrement de pas écrit par Samsung a un début égal à sa fin : la plateforme l'accepte, mais `connect-client` refuse de le convertir (`IllegalArgumentException: startTime must be before endTime`), ce qui faisait échouer toute la lecture des pas. Décision (utilisateur) : lire les pas via l'API d'agrégation de Health Connect, heure par heure. C'est robuste (calcul côté plateforme), c'est l'entrée naturelle de NPCRA (IS/IV/M10/L5 sur activité horaire), et Health Connect dédoublonne les sources selon les priorités de l'utilisateur. Contrepartie : plus de détail par enregistrement ni par source (seule la liste des sources contributrices est gardée). Le sommeil reste en enregistrements bruts.

### DT-3 — Ce qui n'est PAS dans le contrat (calculs à venir)

Date de la nuit, sommeil principal vs sieste, durée, efficacité, dédup entre sources qui se chevauchent, epochs d'activité (pour NPCRA), scores. Tous sont des **calculs** : Phase 2 (validation notebook) puis Phase 3 (port dans `core/`).

Seule exception en Phase 1 : la **dédup par identifiant** des sessions de sommeil (un même `id` Health Connect vu deux fois pendant la lecture est gardé une seule fois). Ce n'est pas une interprétation, c'est éviter un doublon technique. Pour les pas (v2), le total horaire est calculé par Health Connect lui-même.

### DT-4 — Format d'export CSV v1

Trois fichiers CSV + un manifeste, UTF-8, séparateur virgule, une ligne d'en-tête, fin de ligne `\n`, guillemets RFC 4180 uniquement si le champ contient une virgule, un guillemet ou un saut de ligne.

| Fichier | Colonnes |
|---|---|
| `sleep_sessions.csv` | `id,start_utc,end_utc,start_offset,end_offset,source,recording_method,last_modified_utc` |
| `sleep_stages.csv` | `session_id,start_utc,end_utc,stage` |
| `steps.csv` (v2) | `start_utc,end_utc,offset,count,sources` — `sources` triées, séparées par `;` |

- Instants en ISO-8601 UTC (`2024-07-08T22:31:00Z`), offsets au format `+02:00` (`Z` pour UTC), **vide** si inconnu. Énumérations par leur nom (`LIGHT`, `AUTOMATICALLY_RECORDED`).
- **Déterministe** : sessions triées par `start_utc` puis `id`, stades par `session_id` puis `start_utc`, pas par `start_utc`. Mêmes données → mêmes octets. Condition pour s'en servir comme golden fixtures.
- `manifest.json` : `contract_version` (`"2"`), `exported_at`, `app_version`, `sleep_sessions_count`, `sleep_stages_count`, `hourly_steps_count`, `steps_error` (null, ou pourquoi les pas n'ont pas pu être lus — le fichier des pas est alors vide), `history_access` (voir DT-5), `oldest_sleep_start`.
- L'écriture **et** la relecture CSV vivent dans `core/` (fonctions pures) : la relecture sert aux tests aller-retour et aux futures fixtures.

### DT-5 — Ingestion Health Connect (1.2)

- **Permissions** (lecture seule) : `READ_SLEEP`, `READ_STEPS`, `READ_HEALTH_DATA_HISTORY`. Aucune permission d'écriture.
- **Écran de justification obligatoire** : Health Connect exige une activité qui répond à `ACTION_SHOW_PERMISSIONS_RATIONALE` (et à `VIEW_PERMISSION_USAGE` / catégorie `HEALTH_PERMISSIONS` sur Android 14+). Texte simple expliquant l'usage local des données.
- **Disponibilité** : vérifier `getSdkStatus` (Health Connect installé et à jour) puis `FEATURE_READ_HEALTH_DATA_HISTORY`.
- **Jamais de troncature silencieuse** (c'est le bug d'origine) : si l'historique n'est pas accessible (fonctionnalité absente ou permission refusée), la lecture remonte `history_access = LIMITED_30_DAYS` et l'interface l'affiche explicitement. Sinon `FULL`.
- **Lecture** : une seule plage (de l'époque Unix à maintenant), paginée via `pageToken` (taille de page 1000). Arrêt quand le token est vide ; un token déjà vu déclenche une erreur (pas de boucle infinie). On ne découpe en fenêtres temporelles que si les quotas de lecture de Health Connect l'imposent en pratique.
- **Conversion** : fonction pure `SleepSessionRecord → SleepRecord`, côté `app` (elle dépend des classes Health Connect). *Correction 2026-09-24 : le constructeur de `Metadata` est `internal` (le bytecode l'expose comme public, d'où une première lecture erronée). Health Connect interdit de fabriquer l'origine (`dataOrigin`) et la date de modification d'une donnée. Les tests de conversion passent par les fabriques publiques `Metadata.…WithId`, qui fixent identifiant et méthode (origine vide, date `EPOCH`) ; l'origine et la date réelles sont vérifiées sur téléphone (TA-13).*
- **Accès** : les lectures passent par une interface (source de pages) qui renvoie **des objets du contrat** ; la vraie source (`HealthConnectReader`) lit une page Health Connect puis la convertit. Pagination, dédup et règle « pas de lecture avant d'être prêt » se testent ainsi sur des objets du contrat, avec une fausse source.

### DT-6 — Remplacement de l'import CSV (1.2)

- Suppression de l'import CSV Samsung : `LocalImportService`, `SamsungCsvParser`, `StageMaps`, `ImportRepository(Impl)` et la partie CSV de l'écran d'import. **L'import Google Takeout (GPS) reste.**
- Les écrans actuels (Sommeil, Timeline, Hypnogramme, Cadran) lisent via l'interface `SleepRepository`. Un adaptateur temporaire `HealthConnectSleepRepository` convertit le contrat vers les types qu'ils attendent, pour qu'ils continuent de fonctionner jusqu'à leur refonte (Phase 4). Lecture gardée en mémoire le temps de la session, rafraîchie à la demande.
- Les tables Room sommeil / stades / fréquence cardiaque / pas / exercice ne sont plus alimentées. Elles sont **laissées en place** (pas de migration destructive en Phase 1) et seront retirées lors de la refonte.

### DT-7 — Export (1.3)

- Bouton « Exporter mes données brutes » dans Profil. L'utilisateur choisit où enregistrer (sélecteur système `CreateDocument`), on écrit **un seul fichier ZIP** : les 3 CSV + `manifest.json`.
- Un seul fichier pour faciliter la copie vers S1 et le notebook.

---

## Contrat RGPD et sécurité (C1/C2)

- **C1 — Zéro réseau** : aucune donnée Health Connect n'est envoyée nulle part. Aucune nouvelle dépendance réseau.
- **Pas de copie persistante** : les données sommeil/pas ne sont gardées qu'en mémoire (lecture directe). Rien n'est ajouté à Room.
- **Export** : c'est le seul moyen pour les données de sortir de l'app, et il est déclenché par l'utilisateur vers une destination qu'il choisit. Le ZIP n'est **pas chiffré** : un avertissement l'indique avant l'export (données de santé Art.9 en clair).
- **Permissions minimales** : lecture sommeil, pas, historique. Pas d'écriture, pas de lecture en arrière-plan.

---

## Livrables

| Tranche | Livrable |
|---|---|
| 1.1 | Module `:core`, contrat v1 (DT-2), écriture/lecture CSV + manifeste (DT-4), tests TA-1 à TA-6 |
| 1.2 | Dépendance Health Connect, permissions + écran de justification, lecture paginée, conversion, dédup par id, adaptateur `HealthConnectSleepRepository`, retrait de l'import CSV, tests TA-7 à TA-12 |
| 1.3 | Export ZIP depuis Profil, avertissement, test TA-14 |

Chaque tranche fait l'objet d'une PR séparée vers `dev`.

---

## Tests d'acceptation

### `core/` (JUnit, sans Android)

- **TA-1 — Aller-retour sommeil** : écrire puis relire des `SleepRecord` redonne exactement les mêmes objets, y compris offsets absents, session sans stades, et les 8 `StageType`.
- **TA-2 — Aller-retour pas** : idem pour `HourlySteps` (v2 ; sources relues triées).
- **TA-3 — Déterminisme** : les mêmes données fournies dans un ordre différent produisent des fichiers identiques octet pour octet.
- **TA-4 — Format** : en-têtes exacts ; instants en UTC `…Z` ; offset `+02:00` ou vide ; énumérations par leur nom.
- **TA-5 — Échappement** : un champ contenant virgule, guillemet ou saut de ligne fait l'aller-retour intact.
- **TA-6 — Manifeste** : `contract_version = "1"`, comptes de lignes exacts, plus ancienne session correcte.

### `app/` (JVM)

- **TA-7 — Conversion sommeil** : chaque champ d'un `SleepSessionRecord` est reporté (origine et date de modification : valeurs par défaut des fabriques, les vraies valeurs sont couvertes par TA-13) ; les 8 constantes de stade et les 4 méthodes d'enregistrement correspondent 1:1.
- **TA-8 — Pas horaires** *(v2)* : lus par agrégation Health Connect (tranches d'un an pour trouver le début, puis heure par heure par blocs de 30 jours) ; vérifié sur téléphone. Un échec de lecture des pas ne bloque pas le sommeil et reste visible (écran + `steps_error` du manifeste).
- **TA-9 — Pagination** : une fausse source à 3 pages → tous les enregistrements collectés, arrêt sur token vide ; un token répété → erreur explicite.
- **TA-10 — Dédup par identifiant** : une même session de sommeil (`id`) lue sur deux pages n'est gardée qu'une fois.
- **TA-11 — Historique limité jamais silencieux** : fonctionnalité absente ou permission historique refusée → `history_access = LIMITED_30_DAYS` remonté à l'interface.
- **TA-12 — Permissions manquantes** : aucune lecture tentée, état explicite demandant l'autorisation.

### Sur le téléphone (manuel)

- **TA-13 — Parité avec darkhour** : le nombre de sessions lues et la date de la plus ancienne sont cohérents avec darkhour (1146 sessions depuis le 2024-07-08). Tout écart est expliqué avant de continuer.
- **TA-14 — Export exploitable** : le ZIP s'ouvre, et les 3 CSV se chargent tels quels dans pandas.

---

## Architecture des fichiers

```
core/
  build.gradle.kts
  src/main/kotlin/fr/datasaillance/nightfall/core/
    model/SleepRecord.kt            (SleepRecord, SleepStage, StageType, RecordingMethod)
    model/HourlySteps.kt            (v2, remplace StepsInterval.kt)
    export/CsvExport.kt             (écriture + relecture)
    export/ExportManifest.kt
  src/test/kotlin/fr/datasaillance/nightfall/core/export/CsvExportTest.kt
app/src/main/java/fr/datasaillance/nightfall/
  data/healthconnect/               (1.2 : source paginée, conversion, repository)
  ui/screens/healthconnect/         (1.2 : écran de justification des permissions)
```

---

## Hors périmètre

- Fréquence cardiaque, exercice, SpO2 : pas dans le contrat (on ajoutera si un algorithme en a besoin).
- Lecture en arrière-plan et synchronisation incrémentale.
- Refonte des écrans (Phase 4).
