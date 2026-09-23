---
title: "Usage Sessions — sessions d'usage intra-journée (Phase B_us)"
slug: 2026-05-27-usage-sessions
status: draft
created: 2026-05-27
implements: []
tested_by: []
---

# Spec — Usage Sessions — sessions d'usage intra-journée (Phase B_us)

## Vision

L'anneau usage du Cadran v2 ne peut pas afficher une distribution horaire réelle avec les seuls agrégats quotidiens de `UsageDailyEntity` : un `totalTimeForegroundMs` et un `lastTimeUsedMs` ne révèlent ni quand le téléphone a été allumé, ni combien de fois. Or l'exposition à l'écran en soirée est un levier documenté du drift circadien dans le Non-24 — savoir qu'une app représente 2h de foreground est sans valeur si on ne sait pas si c'est à 14h ou à 23h.

Android expose les events bruts de foreground via `UsageStatsManager.queryEvents()`, mais ne les conserve que ~7-10 jours. Si l'application ne persiste pas ces events régulièrement, l'historique fin est définitivement perdu. Cette spec capture et persiste les **sessions foreground** (par app, avec `startMs`/`endMs` réels) dans une nouvelle table Room `usage_session`, pour constituer une timeline d'usage durable et exacte qui alimentera `RadialDay.usageSessions` dans le Cadran v2.

---

## Contexte et dépendances

### Consommateur principal

La spec `cadran-v2` (`docs/vault/specs/2026-05-27-cadran-v2.md`) attend un champ `usageSessions: List<UsageSession>` dans `RadialDay`. Tant que cette liste est vide, l'anneau usage du Cadran s'affiche en état dégradé (anneau grisé + badge "Sessions horaires non disponibles"). Une fois cette spec livrée, `RadialClockViewModel` charge les sessions depuis `UsageSessionDao` et peuple `RadialDay.usageSessions`, débloquant l'affichage horaire réel.

### Spec parente

Cette spec étend le pipeline existant de `LocalUsageStatsService` / `UsageStatsCollectionWorker`. Elle ne remplace pas `UsageDailyEntity` ni `UsageStatsDao` : la table `usage_daily` est conservée telle quelle. Les sessions viennent en **complément**, dans une table distincte.

### Contraintes non-négociables

- **C1** : zéro appel réseau. Tout reste dans la base Room locale. La permission `PACKAGE_USAGE_STATS` est réutilisée — aucune nouvelle permission n'est requise.
- **C2** : les sessions d'usage révèlent des patterns comportementaux (heure d'endormissement, activité nocturne). Les `packageName` ne sont pas chiffrés au niveau Room (Room ne chiffre pas les colonnes individuellement), mais la base entière est chiffrée via SQLCipher (pattern `NightfallKeyManager` déjà en place). Aucun log de `packageName` en clair au-delà du niveau `DEBUG` Timber.
- **C3** : les tests unitaires couvrent la logique de reconstruction des sessions sans Context Android (source mockable). Les tests DAO utilisent Room in-memory (pattern `SleepDaoTest`).

---

## Décisions techniques

### DT-1 — Interface mockable `UsageEventsSource`

Créer une interface `UsageEventsSource` symétrique de l'interface `UsageStatsSource` existante :

```kotlin
interface UsageEventsSource {
    /**
     * Renvoie les events bruts sur la fenêtre [fromMs, toMs].
     * Les events sont retournés dans l'ordre chronologique ascendant.
     */
    fun queryEvents(fromMs: Long, toMs: Long): List<UsageEventRecord>
}

/**
 * DTO interne immutable — découple le service de `android.app.usage.UsageEvents.Event`
 * pour permettre l'injection de fixtures en test sans Context Android.
 */
data class UsageEventRecord(
    val packageName: String,
    val eventType: Int,      // constantes UsageEvents.Event.* — voir DT-2
    val timeStampMs: Long,
)
```

`AndroidUsageEventsSource` implémente `UsageEventsSource` en déléguant à `UsageStatsManager.queryEvents(fromMs, toMs)` et en itérant via `UsageEvents.hasNextEvent()` / `getNextEvent()`.

La raison du DTO `UsageEventRecord` : `UsageEvents.Event` est une classe Android SDK qui ne peut pas être instanciée facilement en test unitaire natif (JVM pure). Le DTO découple complètement la logique de reconstruction de l'API Android.

### DT-2 — Types d'events utilisés et mapping par API level

La reconstruction des sessions s'appuie sur les types d'events suivants de `android.app.usage.UsageEvents.Event` :

| Constante | Valeur int | Sémantique | API level |
|---|---|---|---|
| `ACTIVITY_RESUMED` | 1 | L'app passe au premier plan (focus reçu) | API 29+ |
| `ACTIVITY_PAUSED` | 2 | L'app perd le focus mais reste visible | API 29+ |
| `FOREGROUND_SERVICE_START` | 9 | — non utilisé pour les sessions | — |
| `MOVE_TO_FOREGROUND` | 1 | Alias de `ACTIVITY_RESUMED` sur API < 29 | API 21–28 |
| `MOVE_TO_BACKGROUND` | 2 | Alias de `ACTIVITY_PAUSED` sur API < 29 | API 21–28 |
| `ACTIVITY_STOPPED` | 23 | L'activité est détruite (fin définitive) | API 29+ |
| `KEYGUARD_SHOWN` | 12 | Écran verrouillé | API 28+ |
| `SCREEN_NON_INTERACTIVE` | 14 | Écran éteint (non-interactif) | API 28+ |

**Hypothèse documentée** : sur API 21–28, `MOVE_TO_FOREGROUND` (1) et `MOVE_TO_BACKGROUND` (2) ont les mêmes valeurs entières que `ACTIVITY_RESUMED` et `ACTIVITY_PAUSED`. La logique de reconstruction peut traiter les valeurs int directement sans branchement API level explicite — les constantes sont numériquement identiques. `ACTIVITY_STOPPED` (23) n'est disponible qu'en API 29+ ; en dessous, la fin d'une session est détectée uniquement par `MOVE_TO_BACKGROUND` (2). `KEYGUARD_SHOWN` (12) et `SCREEN_NON_INTERACTIVE` (14) ne sont disponibles qu'en API 28+ ; en dessous, les sessions ouvertes en fin de fenêtre sont fermées à `toMs`.

**Events d'ouverture de session** : type == 1 (`ACTIVITY_RESUMED` / `MOVE_TO_FOREGROUND`)

**Events de fermeture de session** : type == 2 (`ACTIVITY_PAUSED` / `MOVE_TO_BACKGROUND`) ou type == 23 (`ACTIVITY_STOPPED`)

**Events de fermeture forcée de toutes les sessions ouvertes** : type == 12 (`KEYGUARD_SHOWN`) ou type == 14 (`SCREEN_NON_INTERACTIVE`)

### DT-3 — Algorithme de reconstruction des sessions

La reconstruction opère sur la séquence d'events triée chronologiquement pour une fenêtre `[fromMs, toMs]` :

```
openSessions: MutableMap<String, Long>   // packageName -> startMs de la session en cours
completedSessions: MutableList<RawSession>

Pour chaque event e dans l'ordre chronologique :
  si e.type est un event d'ouverture (1) :
    si openSessions contient déjà e.packageName :
      // event RESUMED orphelin sans PAUSED précédent — fermer l'ancienne session
      completedSessions += RawSession(e.packageName, openSessions[e.packageName], e.timeStampMs)
    openSessions[e.packageName] = e.timeStampMs

  si e.type est un event de fermeture par package (2 ou 23) :
    si openSessions contient e.packageName :
      completedSessions += RawSession(e.packageName, openSessions.remove(e.packageName), e.timeStampMs)
    // event PAUSED orphelin sans RESUMED précédent — ignoré silencieusement

  si e.type est un event de fermeture globale (12 ou 14) :
    pour chaque (pkg, startMs) dans openSessions :
      completedSessions += RawSession(pkg, startMs, e.timeStampMs)
    openSessions.clear()

// Sessions restantes ouvertes en fin de fenêtre → fermées à toMs
pour chaque (pkg, startMs) dans openSessions :
  completedSessions += RawSession(pkg, startMs, toMs)

// Filtre : sessions de durée nulle ou négative → ignorées
completedSessions.filter { it.endMs > it.startMs }
```

`RawSession` est un data class interne au service (non persisté directement) : `(packageName: String, startMs: Long, endMs: Long)`. `durationMs` est dérivé : `endMs - startMs`.

### DT-4 — Entité Room `UsageSessionEntity`

Nouvelle table `usage_session` en complément de `usage_daily` :

```kotlin
@Entity(
    tableName = "usage_session",
    indices = [
        Index(value = ["package_name", "start_ms"], unique = true),  // idempotence REPLACE
        Index("date"),
        Index("start_ms"),
        Index("package_name"),
    ],
)
data class UsageSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "start_ms")     val startMs: Long,
    @ColumnInfo(name = "end_ms")       val endMs: Long,
    @ColumnInfo(name = "duration_ms")  val durationMs: Long,         // dérivé : endMs - startMs

    // date au format yyyy-MM-dd dans le fuseau du device (ZoneId.systemDefault())
    // dérivée de startMs pour permettre des requêtes par jour cohérentes avec usage_daily
    val date: String,

    @ColumnInfo(name = "source")           val source: String = "events",
    @ColumnInfo(name = "collected_at_ms")  val collectedAtMs: Long,
)
```

L'index unique sur `(package_name, start_ms)` garantit l'idempotence : un re-run sur la même fenêtre de temps utilise `OnConflictStrategy.REPLACE` et écrase la row existante (une session peut être rouvrée ou allongée si l'event de fermeture tarde). Conséquence : `durationMs` peut être mis à jour à chaque re-run tant que la fenêtre est encore en rétention Android.

Champ `date` en `String` (format `yyyy-MM-dd`) pour s'aligner sur la convention de `UsageDailyEntity` et permettre des jointures/filtres par date identiques.

### DT-5 — DAO `UsageSessionDao`

```kotlin
@Dao
interface UsageSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sessions: List<UsageSessionEntity>): List<Long>

    @Query("SELECT * FROM usage_session WHERE start_ms >= :fromMs AND end_ms <= :toMs ORDER BY start_ms ASC")
    suspend fun getSessionsInRange(fromMs: Long, toMs: Long): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_session WHERE date = :date ORDER BY start_ms ASC")
    suspend fun getByDate(date: String): List<UsageSessionEntity>

    @Query("SELECT COUNT(*) FROM usage_session")
    suspend fun count(): Int

    @Query("DELETE FROM usage_session")
    suspend fun deleteAll()
}
```

Conventions respectées : même structure que `UsageStatsDao`, `suspend fun`, retour de liste pour `upsert`, `ORDER BY` explicite sur toutes les queries de liste.

### DT-6 — Service `UsageSessionsService`

Nouveau service parallèle à `LocalUsageStatsService`, injecté de la même façon :

```kotlin
class UsageSessionsService(
    private val dao: UsageSessionDao,
    private val eventsSource: UsageEventsSource,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Long = System::currentTimeMillis,
)
```

Méthodes publiques :

**`collectSessions(targetDate: LocalDate): Int`**
- Calcule `fromMs` = début de `targetDate` en timezone device, `toMs` = début de `targetDate + 1j`
- Appelle `eventsSource.queryEvents(fromMs, toMs)`
- Exécute l'algorithme DT-3 pour produire les sessions
- Dérive `date` depuis `startMs` via `Instant.ofEpochMilli(startMs).atZone(zone).toLocalDate()`
- Appelle `dao.upsert(sessions)` — idempotent via index unique
- Retourne le nombre de sessions persistées

**`backfillSessions(days: Int): Int`**
- Symétrique de `LocalUsageStatsService.backfillDays(days)` — itère de `today - days` à `today`
- Chaque jour est collecté indépendamment ; les erreurs par jour sont catchées et loggées sans interrompre le backfill des autres jours
- Retourne le nombre total de sessions persistées sur l'ensemble du backfill

### DT-7 — Intégration dans `UsageStatsCollectionWorker`

Le worker quotidien collecte désormais les deux sources dans le même `doWork()` :

```
1. daily stats : service.collectDailyStats(target)    // inchangé
2. sessions    : sessionsService.collectSessions(target)  // nouveau
```

Les deux services reçoivent le même `target` (date J-1 par défaut, ou date fournie via `inputData`).

Si `collectSessions` lève une exception, elle est catchée indépendamment de `collectDailyStats` — un échec de collecte de sessions ne doit pas empêcher la collecte daily de se terminer.

**Backfill au premier lancement** : le worker doit détecter si la table `usage_session` est vide (`dao.count() == 0`) et, si oui, appeler `backfillSessions(days = 10)` avant la collecte normale du jour courant. Ce backfill est best-effort : Android n'a que ~7-10 jours d'events en rétention, et les events de la plage la plus ancienne peuvent être partiels ou absents.

### DT-8 — Migration Room v4 → v5

Migration additive — aucun `ALTER` destructif sur les tables existantes :

```kotlin
object Migration4to5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `usage_session` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `package_name` TEXT NOT NULL,
                `start_ms` INTEGER NOT NULL,
                `end_ms` INTEGER NOT NULL,
                `duration_ms` INTEGER NOT NULL,
                `date` TEXT NOT NULL,
                `source` TEXT NOT NULL DEFAULT 'events',
                `collected_at_ms` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_usage_session_package_name_start_ms` ON `usage_session` (`package_name`, `start_ms`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_session_date` ON `usage_session` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_session_start_ms` ON `usage_session` (`start_ms`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_session_package_name` ON `usage_session` (`package_name`)")
    }
}
```

`NightfallDatabase.version` passe de 4 à 5. `UsageSessionEntity::class` est ajoutée à la liste `entities`. `Migration4to5` est ajoutée à `.addMigrations(...)`. La méthode abstraite `usageSessionDao(): UsageSessionDao` est ajoutée à la classe `NightfallDatabase`.

### DT-9 — DTO `UsageSession` et intégration `RadialDay`

Le DTO `UsageSession` défini provisoirement dans `cadran-v2` (DT-9 de cette spec) est aligné avec les données de `UsageSessionEntity` :

```kotlin
data class UsageSession(
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
)
```

Ce DTO reste défini dans la couche UI / ViewModel (module `:app`) — il ne dépend pas de `UsageSessionEntity`. Le mapping `UsageSessionEntity → UsageSession` est effectué dans `RadialClockViewModel` au moment du chargement des données pour un jour donné.

`RadialClockViewModel` reçoit (ou récupère via injection) une référence à `UsageSessionDao` et appelle `getByDate(date)` pour alimenter `RadialDay.usageSessions`.

---

## Livrables

- [ ] **L1** — Interface `UsageEventsSource` + DTO `UsageEventRecord` dans `data/local/usage/`
- [ ] **L2** — `AndroidUsageEventsSource` wrappant `UsageStatsManager.queryEvents()`
- [ ] **L3** — Algorithme de reconstruction des sessions (fonction pure `reconstructSessions(events, fromMs, toMs, zone)`)
- [ ] **L4** — `UsageSessionEntity` (`data/local/entity/usage/UsageSessionEntity.kt`)
- [ ] **L5** — `UsageSessionDao` (`data/local/dao/UsageSessionDao.kt`)
- [ ] **L6** — `UsageSessionsService` avec `collectSessions(targetDate)` et `backfillSessions(days)` (`data/local/usage/UsageSessionsService.kt`)
- [ ] **L7** — `Migration4to5` dans `NightfallDatabase.kt` (bump version 4 → 5, entité ajoutée, DAO abstrait ajouté)
- [ ] **L8** — Mise à jour de `UsageStatsCollectionWorker` : collecte daily + sessions dans le même run, backfill si table vide, échecs indépendants
- [ ] **L9** — Mise à jour de `RadialClockViewModel` : chargement `UsageSessionDao.getByDate()` → mapping vers `UsageSession` → peuplement `RadialDay.usageSessions`

---

## Tests d'acceptation

Les tests DAO utilisent Room in-memory avec `@RunWith(AndroidJUnit4::class)` (pattern `SleepDaoTest`). Les tests de service injectent un `UsageEventsSource` de fixture (pas de Context Android) avec `runTest { }` de `kotlinx-coroutines-test`.

### TA-1 — Session simple : RESUMED puis PAUSED

**Given** une source d'events avec deux events :
- `{packageName="com.example.app", eventType=1, timeStampMs=T+0}`
- `{packageName="com.example.app", eventType=2, timeStampMs=T+60_000}`

**When** `reconstructSessions(events, fromMs=T-1, toMs=T+120_000, zone)` est appelé.

**Then** exactement 1 session est retournée : `packageName="com.example.app"`, `startMs=T+0`, `endMs=T+60_000`, `durationMs=60_000`.

### TA-2 — Session laissée ouverte en fin de fenêtre

**Given** une source d'events avec un seul event d'ouverture (pas de fermeture dans la fenêtre) :
- `{packageName="com.browser", eventType=1, timeStampMs=T+0}`

**When** `reconstructSessions(events, fromMs=T-1, toMs=T+300_000, zone)` est appelé.

**Then** 1 session est retournée : `startMs=T+0`, `endMs=T+300_000` (fermée à `toMs`).

### TA-3 — Fermeture par KEYGUARD_SHOWN clôture toutes les sessions ouvertes

**Given** deux apps ouvertes simultanément, puis un event `KEYGUARD_SHOWN` :
- `{packageName="com.app1", eventType=1, timeStampMs=T+0}`
- `{packageName="com.app2", eventType=1, timeStampMs=T+10_000}`
- `{packageName="", eventType=12, timeStampMs=T+30_000}`  ← packageName ignoré pour les events globaux

**When** `reconstructSessions(events, fromMs=T-1, toMs=T+60_000, zone)` est appelé.

**Then** 2 sessions sont retournées : `com.app1` avec `endMs=T+30_000`, `com.app2` avec `endMs=T+30_000`. Aucune session n'est fermée à `toMs`.

### TA-4 — Event PAUSED orphelin ignoré

**Given** un event de fermeture sans ouverture précédente :
- `{packageName="com.orphan", eventType=2, timeStampMs=T+0}`

**When** `reconstructSessions(events, fromMs=T-1, toMs=T+60_000, zone)` est appelé.

**Then** 0 sessions sont retournées. Aucune exception n'est levée.

### TA-5 — RESUMED consécutifs sans PAUSED (event orphelin de début)

**Given** deux events RESUMED pour la même app sans PAUSED entre eux :
- `{packageName="com.app1", eventType=1, timeStampMs=T+0}`
- `{packageName="com.app1", eventType=1, timeStampMs=T+20_000}`
- `{packageName="com.app1", eventType=2, timeStampMs=T+50_000}`

**When** `reconstructSessions(events, fromMs=T-1, toMs=T+60_000, zone)` est appelé.

**Then** 2 sessions sont retournées :
- `startMs=T+0`, `endMs=T+20_000` (le second RESUMED ferme implicitement la première session)
- `startMs=T+20_000`, `endMs=T+50_000`

### TA-6 — Idempotence DAO : re-run même fenêtre, pas de doublon

**Given** un `UsageSessionDao` Room in-memory et une session `{packageName="com.app", startMs=1000, endMs=2000}`.

**When** `dao.upsert(listOf(entity))` est appelé deux fois avec la même `(packageName, startMs)`.

**Then** `dao.count()` retourne 1 (pas de doublon). La valeur `endMs` de la row est celle du second upsert (REPLACE sémantique).

### TA-7 — Agrégation par date cohérente avec le fuseau device

**Given** un `startMs` correspondant à `2026-01-15T23:30:00Z` (UTC), device configuré en `Europe/Paris` (UTC+1 en hiver).

**When** `UsageSessionsService.collectSessions(LocalDate.of(2026, 1, 16))` est appelé (car la session commence à `00:30:00` heure locale Paris le 16 janvier).

**Then** la session persistée a `date="2026-01-16"` (date locale Paris, pas `"2026-01-15"` UTC).

### TA-8 — Une app utilisée à 3 moments distincts produit 3 sessions séparées

**Given** 6 events pour la même app, 3 paires RESUMED/PAUSED séparées :
- RESUMED à T+0, PAUSED à T+60_000
- RESUMED à T+120_000, PAUSED à T+180_000
- RESUMED à T+300_000, PAUSED à T+360_000

**When** `reconstructSessions(events, ...)` est appelé.

**Then** exactement 3 sessions sont retournées avec des startMs/endMs distincts et non contigus. Aucun arc continu couvrant T+0 à T+360_000 n'est produit.

### TA-9 — Zéro appel réseau

**Given** `UsageSessionsService` instancié avec `AndroidUsageEventsSource`.

**When** `collectSessions(today)` est appelé.

**Then** aucune connexion réseau sortante n'est établie. Vérifiable en test instrumenté via un `OkHttpClient.Interceptor` ou `StrictMode.ThreadPolicy` avec `detectNetwork()`. La contrainte C1 est respectée à chaque run.

### TA-10 — Skip propre si permission PACKAGE_USAGE_STATS absente

**Given** `UsageStatsPermissionHelper.hasPermission()` retourne `false`.

**When** `UsageStatsCollectionWorker.doWork()` est exécuté.

**Then** le worker retourne `Result.success()` sans appeler `collectSessions()` ni `collectDailyStats()`. `UsageSessionDao.count()` reste à 0. Un log Timber de niveau INFO est émis.

### TA-11 — DAO `getSessionsInRange` retourne uniquement les sessions dans la fenêtre

**Given** trois sessions persistées :
- `{startMs=1_000, endMs=2_000}`
- `{startMs=5_000, endMs=6_000}`
- `{startMs=10_000, endMs=11_000}`

**When** `dao.getSessionsInRange(fromMs=4_000, toMs=9_000)` est appelé.

**Then** 1 session est retournée : `startMs=5_000`.

---

## Suite naturelle

### Consommateur immédiat

Une fois cette spec livrée, `RadialClockViewModel` doit être mis à jour (L9) pour charger les sessions via `UsageSessionDao.getByDate(date)` et peupler `RadialDay.usageSessions`. C'est la seule modification nécessaire dans la couche UI pour que l'anneau usage du Cadran v2 passe de l'état dégradé à l'affichage horaire réel. La surface d'intégration est `RadialDay.usageSessions: List<UsageSession>` définie dans la spec `cadran-v2` (DT-9).

### Specs aval

- **`cadran-v2`** (`docs/vault/specs/2026-05-27-cadran-v2.md`) — Consommateur direct. Débloqué dès que `UsageSessionEntity` existe et que `RadialDay.usageSessions` est alimenté. Les TA-2, TA-5 et TA-6 de `cadran-v2` passent de l'état dégradé à fonctionnel.
- **`labeled-places`** — Indépendant de cette spec, mais conjoint pour compléter l'anneau timeline du Cadran v2.

---

## Hypothèses et points d'attention

1. **Mapping event types API level** : les valeurs entières de `ACTIVITY_RESUMED` (1) et `MOVE_TO_FOREGROUND` (1) sont identiques, de même que `ACTIVITY_PAUSED` (2) et `MOVE_TO_BACKGROUND` (2). Cette coïncidence est documentée AOSP et stable depuis API 21. L'algorithme de reconstruction n'a pas besoin de se brancher sur `Build.VERSION.SDK_INT` pour ces deux paires.

2. **`KEYGUARD_SHOWN` et `SCREEN_NON_INTERACTIVE` absents sous API 28** : sur les devices tournant API < 28, les sessions ouvertes en fin de fenêtre seront fermées à `toMs` (comportement de repli). Ce cas est acceptable étant donné la cible Android minimale du projet.

3. **Rétention Android** : le backfill de 10 jours est best-effort. Les jours les plus anciens de la plage peuvent renvoyer zéro events sans que ce soit une erreur — l'idempotence du DAO garantit qu'un re-run ultérieur sur la même fenêtre, s'il trouve de nouveaux events, corrigera les données.

4. **`durationMs` persisté** : la valeur est dérivée de `endMs - startMs` mais stockée explicitement dans la table pour des raisons de performance des requêtes d'agrégation futures (ex. `SUM(duration_ms) GROUP BY date`). En cas d'incohérence constatée, la valeur calculée fait foi.

5. **SQLCipher** : la base Room entière est chiffrée via `NightfallKeyManager` (pattern déjà en place depuis v1). La table `usage_session` bénéficie du même chiffrement sans configuration supplémentaire.
