---
title: "Phase 4 Android Import SAF"
slug: 2026-05-06-p4-android-import
status: ready
created: 2026-05-06
phase: P4
spec_type: feature
related_specs:
  - 2026-05-06-p4-android-shell
  - 2026-04-24-v2-csv-import-sqlalchemy
implements: []
tested_by:
  - android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/import_/ImportViewModelTest.kt
  - android-app/app/src/test/java/fr/datasaillance/nightfall/data/http/CountingRequestBodyTest.kt
branch: feat/p4-android-import
tags: [phase4, android, saf, import, samsung-health, csv, multipart, rgpd]
---

# Spec — Phase 4 Android Import SAF

## Vision

L'écran Import SAF permet à l'utilisateur de sélectionner son export Samsung Health (dossier ou ZIP) via le Storage Access Framework Android, et de l'envoyer directement vers son backend Nightfall self-hosted — sans aucun intermédiaire tiers. Le parcours est volontairement séquentiel et transparent : connexion vérifiée, notice RGPD obligatoire, sélection du fichier, upload progressif par type de données. C'est la seule voie d'entrée des données santé dans la Phase 4, et chaque décision technique y réduit la surface d'attaque et de fuite.

---

## Contexte et dépendances

Ce module est une destination du NavGraph défini dans `p4-android-shell`. Il doit être intégré sans modifier l'architecture du shell.

| Dépendance | Spec de référence | Point d'intégration |
|---|---|---|
| NavGraph + deep link `nightfall://import` | `2026-05-06-p4-android-shell` | `ImportScreen.kt` remplace le stub `Text("Import — p4-android-import")` |
| `RetrofitClient` + `AuthInterceptor` | `2026-05-06-p4-android-shell` | `NightfallApi.kt` reçoit les nouveaux endpoints import |
| `TokenDataStore` | `2026-05-06-p4-android-shell` | `ImportViewModel` lit l'URL backend via injection Hilt |
| Endpoints REST `/api/sleep`, `/api/heartrate`, `/api/steps`, `/api/exercise` | `2026-04-24-v2-csv-import-sqlalchemy` | Acceptent actuellement du JSON ; cette spec ajoute un endpoint `/import` CSV multipart sur chaque router |

---

## Décisions techniques

| # | Décision | Justification |
|---|---|---|
| D1 | Storage Access Framework (`ACTION_GET_CONTENT` ou `ACTION_OPEN_DOCUMENT_TREE`) — pas d'accès `READ_EXTERNAL_STORAGE` | SAF = permission par URI délégué, pas de permission large. Requis API 28+. Aucun accès au stockage en dehors du document sélectionné par l'utilisateur |
| D2 | Lecture des CSV via `ContentResolver.openInputStream(uri)` — jamais de copie dans le stockage externe ou le cache de l'app | C2 : les données Art.9 ne doivent pas persister en clair sur l'appareil après import |
| D3 | Upload via `OkHttp RequestBody` streamé (`RequestBody.create(contentType, inputStream)` + `CountingRequestBody`) — pas de lecture complète en mémoire | Les exports Samsung Health peuvent dépasser 50 Mo ; un `ByteArray` complet en mémoire causerait un `OutOfMemoryError`. Le streaming résout les deux contraintes |
| D4 | `multipart/form-data` avec champ `file` pour chaque CSV | Cohérence avec l'upload de fichiers standard HTTP ; facilite les tests curl et la validation backend |
| D5 | Nouveaux endpoints `/api/sleep/import`, `/api/heartrate/import`, `/api/steps/import`, `/api/exercise/import` côté backend — les POST existants (JSON) ne sont pas modifiés | Séparation nette JSON (scripts desktop) vs multipart CSV (mobile) ; évite une migration forcée des callers existants |
| D6 | Vérification de connectivité via `GET /api/health` avant toute sélection | Échouer tôt avant que l'utilisateur sélectionne son fichier — meilleure UX qu'un timeout à mi-upload |
| D7 | `ImportViewModel` (@HiltViewModel) avec `StateFlow<ImportUiState>` | Pattern Compose standard ; `collectAsStateWithLifecycle()` côté UI évite les fuites lifecycle |
| D8 | Upload séquentiel par type (sleep → heartrate → steps → exercise) — pas de parallélisme | Simplifité du feedback par type ; évite la saturation de la connexion réseau sur VPS modeste |
| D9 | Progress par fichier : `(bytesUploaded / totalBytes).toFloat()` via `CountingRequestBody` | Feedback utilisateur précis sans bibliothèque tierce |
| D10 | Notice RGPD obligatoire (pas de skip) affichée avant la sélection SAF | Art. 13 RGPD : l'utilisateur doit être informé avant que ses données transitent, même vers son propre serveur |
| D11 | TLS obligatoire pour toute URL qui n'est pas `10.0.2.2` — hérité de `network_security_config.xml` du shell | C1 + C3 : cleartext limité à l'émulateur dev local uniquement |
| D12 | `ImportScreen.kt` dans `src/main` (pas dans un source set flavor) | L'import SAF est identique dans les deux flavors `webview` et `native` |

---

## Architecture

### Structure des packages concernés

```
android-app/
  app/
    src/
      main/
        java/fr/datasaillance/nightfall/
          ui/
            screens/
              import_/
                ImportScreen.kt          ← composable principal du stepper
                ImportStep.kt            ← sealed class des étapes du stepper
          data/
            http/
              NightfallApi.kt            ← ajout des 4 méthodes @Multipart @POST
              CountingRequestBody.kt     ← OkHttp RequestBody avec callback progress
            import_/
              ImportRepository.kt        ← logique upload, stream CSV, appel API
          domain/
            import_/
              ImportUiState.kt           ← sealed class états UI
              ImportResult.kt            ← data class résultat par type
          viewmodel/
            import_/
              ImportViewModel.kt         ← @HiltViewModel, StateFlow<ImportUiState>
      test/
        java/fr/datasaillance/nightfall/
          viewmodel/import_/
            ImportViewModelTest.kt
          data/http/
            CountingRequestBodyTest.kt
```

### Sealed class ImportUiState

```kotlin
// domain/import_/ImportUiState.kt
sealed class ImportUiState {
    object Idle : ImportUiState()
    object Connecting : ImportUiState()
    data class ConnectionFailed(val message: String) : ImportUiState()
    object Connected : ImportUiState()                         // ping OK, attente sélection
    object Selecting : ImportUiState()                        // SAF picker ouvert
    data class Uploading(
        val currentType: ImportDataType,
        val progress: Float,                                  // 0f..1f
        val completedTypes: List<ImportDataType>,
        val skippedTypes: List<ImportDataType>,
    ) : ImportUiState()
    data class Success(val results: List<ImportResult>) : ImportUiState()
    data class Error(val message: String, val retryable: Boolean) : ImportUiState()
}
```

### Enum ImportDataType

```kotlin
// domain/import_/ImportDataType.kt
enum class ImportDataType(
    val samsungFilenamePrefix: String,
    val apiPath: String,
    val labelRes: Int,
    val iconRes: Int,
) {
    SLEEP(
        samsungFilenamePrefix = "com.samsung.health.sleep",
        apiPath = "/api/sleep/import",
        labelRes = R.string.import_type_sleep,
        iconRes = R.drawable.ic_import_sleep,
    ),
    HEART_RATE(
        samsungFilenamePrefix = "com.samsung.health.heart_rate",
        apiPath = "/api/heartrate/import",
        labelRes = R.string.import_type_heartrate,
        iconRes = R.drawable.ic_import_heartrate,
    ),
    STEPS(
        samsungFilenamePrefix = "com.samsung.health.step_daily_trend",
        apiPath = "/api/steps/import",
        labelRes = R.string.import_type_steps,
        iconRes = R.drawable.ic_import_steps,
    ),
    EXERCISE(
        samsungFilenamePrefix = "com.samsung.health.exercise",
        apiPath = "/api/exercise/import",
        labelRes = R.string.import_type_exercise,
        iconRes = R.drawable.ic_import_exercise,
    ),
}
```

### Data class ImportResult

```kotlin
// domain/import_/ImportResult.kt
data class ImportResult(
    val type: ImportDataType,
    val inserted: Int,
    val skipped: Int,
    val errorMessage: String? = null,
)
```

### ImportViewModel

```kotlin
// viewmodel/import_/ImportViewModel.kt
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: ImportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun checkConnection() {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Connecting
            val ok = repository.pingBackend()
            _uiState.value = if (ok) ImportUiState.Connected else ImportUiState.ConnectionFailed(
                "Backend inaccessible — vérifiez l'URL dans les paramètres"
            )
        }
    }

    fun startUpload(contentResolver: ContentResolver, treeUri: Uri) {
        viewModelScope.launch {
            val csvEntries = repository.extractCsvEntries(contentResolver, treeUri)
            if (csvEntries.isEmpty()) {
                _uiState.value = ImportUiState.Error(
                    "Aucun fichier Samsung Health reconnu dans l'archive.",
                    retryable = true
                )
                return@launch
            }
            val results = mutableListOf<ImportResult>()
            val completed = mutableListOf<ImportDataType>()
            val skipped = mutableListOf<ImportDataType>()

            for (type in ImportDataType.entries) {
                val entry = csvEntries[type]
                if (entry == null) {
                    skipped.add(type)
                    continue
                }
                try {
                    _uiState.value = ImportUiState.Uploading(
                        currentType = type,
                        progress = 0f,
                        completedTypes = completed.toList(),
                        skippedTypes = skipped.toList(),
                    )
                    val result = repository.uploadCsv(
                        contentResolver = contentResolver,
                        uri = entry.uri,
                        type = type,
                        totalBytes = entry.size,
                    ) { progress ->
                        _uiState.value = ImportUiState.Uploading(
                            currentType = type,
                            progress = progress,
                            completedTypes = completed.toList(),
                            skippedTypes = skipped.toList(),
                        )
                    }
                    results.add(result)
                    completed.add(type)
                } catch (e: Exception) {
                    results.add(ImportResult(type = type, inserted = 0, skipped = 0, errorMessage = e.message))
                    // upload continue pour les autres types — pas de fail global
                }
            }
            _uiState.value = ImportUiState.Success(results)
        }
    }

    fun reset() {
        _uiState.value = ImportUiState.Idle
    }
}
```

### ImportRepository

```kotlin
// data/import_/ImportRepository.kt
class ImportRepository @Inject constructor(
    private val api: NightfallApi,
    private val tokenDataStore: TokenDataStore,
) {
    // Retourne true si GET /api/health répond HTTP 200 dans les 10 s
    suspend fun pingBackend(): Boolean

    // Parcourt le treeUri (dossier SAF) ou décompresse le ZIP en mémoire (via ZipInputStream)
    // Retourne Map<ImportDataType, CsvEntry> — CsvEntry = (uri: Uri, size: Long)
    // Ne copie jamais les bytes sur disque
    suspend fun extractCsvEntries(
        contentResolver: ContentResolver,
        treeUri: Uri
    ): Map<ImportDataType, CsvEntry>

    // Ouvre un InputStream streamé depuis ContentResolver, construit un CountingRequestBody,
    // appelle l'endpoint multipart correspondant à `type`
    // Invoque onProgress(Float) à chaque chunk OkHttp
    suspend fun uploadCsv(
        contentResolver: ContentResolver,
        uri: Uri,
        type: ImportDataType,
        totalBytes: Long,
        onProgress: (Float) -> Unit,
    ): ImportResult
}

data class CsvEntry(val uri: Uri, val size: Long)
```

### NightfallApi — ajouts Phase 4 import

```kotlin
// data/http/NightfallApi.kt  (ajouts dans l'interface existante)
interface NightfallApi {
    // ... méthodes existantes

    @Multipart
    @POST("api/sleep/import")
    suspend fun importSleep(@Part file: MultipartBody.Part): ImportApiResponse

    @Multipart
    @POST("api/heartrate/import")
    suspend fun importHeartRate(@Part file: MultipartBody.Part): ImportApiResponse

    @Multipart
    @POST("api/steps/import")
    suspend fun importSteps(@Part file: MultipartBody.Part): ImportApiResponse

    @Multipart
    @POST("api/exercise/import")
    suspend fun importExercise(@Part file: MultipartBody.Part): ImportApiResponse
}

@Serializable
data class ImportApiResponse(
    val inserted: Int,
    val skipped: Int,
)
```

### CountingRequestBody

```kotlin
// data/http/CountingRequestBody.kt
class CountingRequestBody(
    private val delegate: RequestBody,
    private val totalBytes: Long,
    private val onProgress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = totalBytes
    override fun writeTo(sink: BufferedSink) {
        val counted = CountingSink(sink)
        val buffered = counted.buffer()
        delegate.writeTo(buffered)
        buffered.flush()
    }

    inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        private var bytesWritten = 0L
        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            bytesWritten += byteCount
            onProgress((bytesWritten.toFloat() / totalBytes).coerceIn(0f, 1f))
        }
    }
}
```

### ImportScreen — composable

L'écran est un stepper à 3 étapes (1 = Connexion, 2 = Sélection, 3 = Import). Il est rendu depuis le NavGraph existant sur la route `"import"`.

```kotlin
// ui/screens/import_/ImportScreen.kt
@Composable
fun ImportScreen(
    viewModel: ImportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Launcher SAF
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.startUpload(LocalContext.current.contentResolver, it) }
    }

    Scaffold(
        topBar = { /* TopAppBar "Importer des données" + bouton retour */ }
    ) { padding ->
        when (val state = uiState) {
            is ImportUiState.Idle,
            is ImportUiState.Connecting,
            is ImportUiState.ConnectionFailed,
            is ImportUiState.Connected -> StepConnectionContent(state, viewModel, padding)

            is ImportUiState.Selecting -> { /* SAF ouvert — état intermédiaire */ }

            is ImportUiState.Uploading -> StepUploadContent(state, padding)
            is ImportUiState.Success -> StepSuccessContent(state, onNavigateBack, padding)
            is ImportUiState.Error -> ImportErrorContent(state, viewModel, padding)
        }
    }
}
```

Composables internes (dans le même fichier ou sous-fichiers privés) :
- `StepConnectionContent` — affiche l'état ping, bouton "Vérifier la connexion", indicateur de progression si `Connecting`
- `RgpdNoticeCard` — carte avec texte RGPD obligatoire (voir section RGPD)
- `StepSelectionContent` — s'affiche après connexion confirmée ; `RgpdNoticeCard` + bouton "Sélectionner le dossier Samsung Health"
- `StepUploadContent` — liste des 4 types (`ImportDataType.entries`) avec icône, label, état (en attente / en cours + LinearProgressIndicator / terminé / ignoré)
- `StepSuccessContent` — résumé par type : lignes importées, doublons ignorés
- `ImportErrorContent` — message d'erreur + bouton "Réessayer" si `retryable = true`

### Sealed class ImportStep (pour le stepper visuel)

```kotlin
// ui/screens/import_/ImportStep.kt
sealed class ImportStep(val index: Int, val label: String) {
    object Connection : ImportStep(1, "Connexion")
    object Selection  : ImportStep(2, "Sélection")
    object Upload     : ImportStep(3, "Import")
}
```

---

## Contrat backend — nouveaux endpoints CSV

Ces 4 endpoints sont à ajouter sur les routers FastAPI existants. Ils ne modifient pas les endpoints JSON actuels.

### Contrat commun

- Méthode : `POST`
- Auth : Bearer JWT (via `get_current_user`)
- Content-Type : `multipart/form-data`, champ `file`
- Réponse 201 : `{"inserted": int, "skipped": int}`
- Réponse 400 : `{"detail": "Invalid CSV format"}` si le fichier ne correspond pas au schéma attendu
- Réponse 413 : si le fichier dépasse la limite configurée (recommandé : 50 Mo)

| Endpoint | Router | Parsing CSV attendu |
|---|---|---|
| `POST /api/sleep/import` | `server/routers/sleep.py` | Colonnes : `sleep_start`, `sleep_end` (ISO 8601), `stage_type`, `stage_start`, `stage_end` |
| `POST /api/heartrate/import` | `server/routers/heartrate.py` | Colonnes : `date` (YYYY-MM-DD), `hour` (int), `min_bpm`, `max_bpm`, `avg_bpm`, `sample_count` |
| `POST /api/steps/import` | `server/routers/steps.py` | Colonnes : `date` (YYYY-MM-DD), `hour` (int), `step_count` |
| `POST /api/exercise/import` | `server/routers/exercise.py` | Colonnes : `exercise_type`, `exercise_start`, `exercise_end` (ISO 8601), `duration_minutes` |

Note : le CSV Samsung Health brut utilise des noms de colonnes différents (ex : `start_time`, `end_time`). Le mapping se fait dans `ImportRepository.extractCsvEntries` ou dans le parser backend — cette spec ne tranche pas ; le coder-android peut déléguer au backend (plus cohérent avec la logique de `scripts/import_samsung_csv.py` existante) ou normaliser côté Android. Le coder-android doit expliciter ce choix dans son PR.

---

## RGPD — Notice obligatoire

La carte `RgpdNoticeCard` est affichée entre l'étape Connexion confirmée et le bouton de sélection SAF. Elle n'est pas dismissable. Texte exact :

> "Ces données sont envoyées uniquement vers **[URL backend]**. Elles ne transitent par aucun serveur tiers. Aucune copie n'est conservée sur cet appareil après l'import."

- L'URL backend est substituée dynamiquement depuis `TokenDataStore` / settings
- `[URL backend]` est rendu en `fontWeight = FontWeight.SemiBold`, couleur `primary` (teal)
- Fond de carte : `MaterialTheme.colorScheme.surfaceVariant` ; bordure gauche couleur `primary` (2dp)
- Aucun bouton "Ignorer" ni "Plus tard" — c'est une information, pas un consentement (le consentement RGPD est recueilli à l'inscription)

---

## Design — tokens DataSaillance

| Élément | Token |
|---|---|
| Fond page | `MaterialTheme.colorScheme.background` |
| Carte RGPD | `colorScheme.surfaceVariant` + bordure `colorScheme.primary` |
| Bouton CTA principal ("Vérifier", "Sélectionner", "Fermer") | `FilledButton` Material 3 — `containerColor = colorScheme.primary` (Teal700 `#0E9EB0`) |
| Icône type import "en cours" | `colorScheme.primary` |
| Icône type import "terminé" | `colorScheme.tertiary` (Cyan500 `#07BCD3`) |
| Icône type import "ignoré" | `colorScheme.onSurface` opacité 38% |
| `LinearProgressIndicator` | `color = colorScheme.primary`, `trackColor = colorScheme.surfaceVariant` |
| Indicateur étape active | `colorScheme.secondary` (Amber600 `#D37C04`) |
| Police | Inter (corps/UI) + Playfair Display (titres) — héritées de `NightfallTheme` |
| Interdits | `#6366f1`, `linear-gradient` décoratif, `box-shadow` glow |

Light et dark mode sont tous les deux couverts via `NightfallTheme` — aucun `if (darkTheme)` inline dans `ImportScreen.kt`.

---

## Livrables

### Fichiers Android à créer

- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportUiState.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportDataType.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/domain/import_/ImportResult.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/CountingRequestBody.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/data/import_/ImportRepository.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/viewmodel/import_/ImportViewModel.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/import_/ImportStep.kt`
- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/import_/ImportScreen.kt` (remplace le stub du shell)

### Fichiers Android à modifier

- [ ] `android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt` — ajout des 4 méthodes `@Multipart @POST`

### Ressources Android à créer

- [ ] `android-app/app/src/main/res/values/strings.xml` — ajout des clés : `import_type_sleep`, `import_type_heartrate`, `import_type_steps`, `import_type_exercise`, `import_rgpd_notice`, `import_step_connection`, `import_step_selection`, `import_step_upload`
- [ ] `android-app/app/src/main/res/drawable/ic_import_sleep.xml` — icône vecteur lit / lune
- [ ] `android-app/app/src/main/res/drawable/ic_import_heartrate.xml` — icône vecteur coeur / ECG
- [ ] `android-app/app/src/main/res/drawable/ic_import_steps.xml` — icône vecteur pas
- [ ] `android-app/app/src/main/res/drawable/ic_import_exercise.xml` — icône vecteur sport

### Fichiers backend à créer ou modifier

- [ ] `server/routers/sleep.py` — ajout endpoint `POST /api/sleep/import` (multipart CSV)
- [ ] `server/routers/heartrate.py` — ajout endpoint `POST /api/heartrate/import`
- [ ] `server/routers/steps.py` — ajout endpoint `POST /api/steps/import`
- [ ] `server/routers/exercise.py` — ajout endpoint `POST /api/exercise/import`

### Tests

- [ ] `android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/import_/ImportViewModelTest.kt` — JUnit4 + coroutines-test, tests des transitions d'état
- [ ] `android-app/app/src/test/java/fr/datasaillance/nightfall/data/http/CountingRequestBodyTest.kt` — vérifie que `onProgress` est appelé, que la progression atteint 1f
- [ ] `tests/server/test_import_csv_multipart.py` — pytest, tests des 4 endpoints backend (upload CSV factice, idempotence, format invalide)

---

## Tests d'acceptation

**TA-01 — Connexion réussie : progression vers étape Sélection**
- Given : le backend répond `HTTP 200` sur `GET /api/health`
- When : l'utilisateur appuie sur "Vérifier la connexion" depuis `ImportScreen`
- Then : `ImportUiState` passe de `Connecting` à `Connected` ; le stepper affiche l'étape 1 cochée ; la `RgpdNoticeCard` et le bouton "Sélectionner le dossier" apparaissent

**TA-02 — Connexion échouée : message d'erreur et retry**
- Given : le backend est inaccessible (timeout 10s ou connexion refusée)
- When : l'utilisateur appuie sur "Vérifier la connexion"
- Then : `ImportUiState` passe à `ConnectionFailed` ; un message "Backend inaccessible — vérifiez l'URL dans les paramètres" est affiché ; le bouton "Réessayer" relance `checkConnection()`

**TA-03 — Notice RGPD non bypassable**
- Given : l'état est `Connected`
- When : l'écran affiche l'étape Sélection
- Then : la `RgpdNoticeCard` est visible ; il n'existe aucun bouton "Ignorer" ni "Passer" ; le bouton "Sélectionner" est le seul CTA disponible

**TA-04 — Sélection SAF : lancement du picker**
- Given : l'état est `Connected` et la notice RGPD est visible
- When : l'utilisateur appuie sur "Sélectionner le dossier Samsung Health"
- Then : `ActivityResultContracts.OpenDocumentTree` est lancé ; le sélecteur de fichiers système s'ouvre

**TA-05 — Upload progressif : feedback par type**
- Given : l'utilisateur a sélectionné un dossier contenant `com.samsung.health.sleep.*.csv` et `com.samsung.health.heart_rate.*.csv`
- When : `startUpload()` est appelé
- Then : l'état passe séquentiellement par `Uploading(currentType = SLEEP, ...)` puis `Uploading(currentType = HEART_RATE, ...)` ; `completedTypes` s'incrémente après chaque type ; `STEPS` et `EXERCISE` apparaissent dans `skippedTypes`

**TA-06 — Progress 0 → 1 sur un fichier**
- Given : un `CountingRequestBody` wrappant un `RequestBody` de 1000 bytes
- When : `writeTo(sink)` est appelé en écrivant 500 bytes puis 500 bytes
- Then : `onProgress` est appelé au moins deux fois ; la dernière valeur est `1.0f` ; aucune valeur dépasse `1.0f`

**TA-07 — Fichier non reconnu : erreur explicite**
- Given : l'utilisateur sélectionne un dossier qui ne contient aucun fichier dont le nom commence par `com.samsung.health.*`
- When : `extractCsvEntries()` est appelé
- Then : le résultat est une `Map` vide ; `ImportViewModel` passe à `ImportUiState.Error("Aucun fichier Samsung Health reconnu dans l'archive.", retryable = true)`

**TA-08 — Erreur réseau à mi-upload : pas de blocage global**
- Given : l'upload de SLEEP réussit ; l'upload de HEART_RATE lève une `IOException` (connexion coupée)
- When : `startUpload()` traite les 4 types
- Then : `results` contient un `ImportResult(HEART_RATE, inserted=0, skipped=0, errorMessage="...")` ; les types STEPS et EXERCISE (si présents) continuent à être uploadés ; l'état final est `Success` (avec erreur partielle dans les résultats)

**TA-09 — Upload idempotent**
- Given : l'utilisateur relance un import avec les mêmes fichiers qu'un import précédent
- When : les CSV sont envoyés aux endpoints backend
- Then : la réponse backend retourne `{"inserted": 0, "skipped": N}` ; `ImportResult.skipped` est non nul ; `ImportResult.inserted` est 0 ; aucune erreur n'est levée

**TA-10 — Aucune copie fichier sur le disque**
- Given : l'utilisateur sélectionne un dossier Samsung Health
- When : `extractCsvEntries()` puis `uploadCsv()` s'exécutent
- Then : aucun fichier CSV n'est écrit dans `context.cacheDir`, `context.filesDir` ou le stockage externe de l'app ; seuls des `InputStream` ouverts depuis `ContentResolver.openInputStream(uri)` sont utilisés

**TA-11 — URL backend dans la notice RGPD**
- Given : l'URL backend configurée est `https://sh-prod.datasaillance.fr`
- When : `RgpdNoticeCard` est rendu
- Then : le texte de la carte contient la chaîne `https://sh-prod.datasaillance.fr` en police SemiBold

**TA-12 — Deep link nightfall://import avec token valide**
- Given : l'app est installée, un JWT valide est présent dans `TokenDataStore`, l'app est en arrière-plan
- When : le système Android résout l'intent `nightfall://import`
- Then : l'app s'ouvre sur `ImportScreen` dans l'état `Idle` ; le bouton "Vérifier la connexion" est visible ; aucune erreur de navigation

---

## Contraintes RGPD et sécurité (C1–C5)

| Contrainte | Application dans ce module |
|---|---|
| **C1** Local-first | `ImportRepository` ne cible que `backendUrl` de l'utilisateur. Aucun appel vers Firebase, S3, Supabase ou tout autre tiers. La vérification est testée via `TA-10` |
| **C2** Chiffrement / pas de cache en clair | Aucune copie des CSV sur le stockage de l'app (`TA-10`). Transit TLS obligatoire sauf émulateur (`D11`). Les données Art.9 sont chiffrées à l'écriture côté backend (responsabilité des routers FastAPI existants) |
| **C3** Sécurité | Pentester agent passe avant merge. Points d'attention : injection de chemin via URI SAF, dépassement mémoire via ZIP bomb (voir note ci-dessous), absence de validation de type MIME côté Android |
| **C4** Design DataSaillance | Tokens teal/amber/cyan via `NightfallTheme`. Aucun `#6366f1`, aucun gradient décoratif. Police Inter + Playfair Display |
| **C5** No LLM | Aucun appel LLM dans tout ce module |

**Note sécurité ZIP bomb** : si le fichier sélectionné est un ZIP, `ImportRepository.extractCsvEntries` doit implémenter une limite de décompression (ex : 200 Mo décompressés max, ou 10 entrées max) pour éviter un `OutOfMemoryError` volontaire. Cette limite est à définir avec le coder-android et à documenter dans le PR.

---

## Suite naturelle

| Spec | Slug | Relation |
|---|---|---|
| WebView Bridge | `p4-webview-bridge` | Étape suivante Phase 4 — injection JWT dans la WebView, `WebViewClient` SSL, cookies `httpOnly` |
| Auth Android | `p4-android-auth` | Si `p4-android-auth` est implémenté avant cet écran, `ImportViewModel` peut déclencher un refresh token avant l'upload |
| Compose Canvas (Phase 5) | `p5-compose-canvas` | Les données importées via ce module alimentent les visualisations natives |
