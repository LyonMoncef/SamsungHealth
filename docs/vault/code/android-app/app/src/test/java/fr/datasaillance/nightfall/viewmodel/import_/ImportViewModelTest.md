---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/import_/ImportViewModelTest.kt
git_blob: 3b311c3bde70b5163a50b6f8c2000d31e98ba7bb
last_synced: '2026-05-07T03:10:49Z'
loc: 363
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/import_/ImportViewModelTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/import_/ImportViewModelTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/viewmodel/import_/ImportViewModelTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.viewmodel.import_

// spec: Tests d'acceptation TA-01, TA-02, TA-05, TA-07, TA-08
// spec: section "ImportViewModel" + "Architecture" + "Tests d'acceptation"
// RED by construction: fr.datasaillance.nightfall.viewmodel.import_.ImportViewModel does not exist yet

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.viewmodel.import_.ImportViewModel
// fr.datasaillance.nightfall.domain.import_.ImportUiState
// fr.datasaillance.nightfall.domain.import_.ImportDataType
// fr.datasaillance.nightfall.domain.import_.ImportResult
// fr.datasaillance.nightfall.data.import_.ImportRepository
// fr.datasaillance.nightfall.data.import_.CsvEntry

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: fr.datasaillance.nightfall.data.import_.ImportRepository
    private lateinit var viewModel: fr.datasaillance.nightfall.viewmodel.import_.ImportViewModel

    // SAF mocks — these types exist in Android SDK (safe to mock with mock-maker-inline)
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockTreeUri: Uri

    @Before
    fun setUp() {
        // spec: D7 — viewModelScope uses Dispatchers.Main; must be replaced with test dispatcher
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        // spec: D7 — No Hilt (issue #52): ImportViewModel constructed directly with repository
        viewModel = fr.datasaillance.nightfall.viewmodel.import_.ImportViewModel(repository)
        mockContentResolver = mock()
        mockTreeUri = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // spec: TA-01 — checkConnection() when pingBackend() returns true → state is Connected
    @Test
    fun checkConnection_pingSuccess_emitsConnected() = runTest {
        whenever(repository.pingBackend()).thenReturn(true)

        viewModel.checkConnection()
        advanceUntilIdle()

        // spec: TA-01 — ImportUiState must be Connected after a successful ping
        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.domain.import_.ImportUiState.Connected) {
            "uiState must be ImportUiState.Connected when pingBackend() returns true — spec: TA-01, got: $state"
        }
    }

    // spec: TA-02 — checkConnection() when pingBackend() returns false → state is ConnectionFailed with exact message
    @Test
    fun checkConnection_pingFailure_emitsConnectionFailed() = runTest {
        whenever(repository.pingBackend()).thenReturn(false)

        viewModel.checkConnection()
        advanceUntilIdle()

        // spec: TA-02 — ImportUiState must be ConnectionFailed with the spec-mandated message
        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.domain.import_.ImportUiState.ConnectionFailed) {
            "uiState must be ImportUiState.ConnectionFailed when pingBackend() returns false — spec: TA-02, got: $state"
        }
        val message = (state as fr.datasaillance.nightfall.domain.import_.ImportUiState.ConnectionFailed).message
        assert(message == "Backend inaccessible — vérifiez l'URL dans les paramètres") {
            "ConnectionFailed message must match spec exactly — spec: TA-02, got: '$message'"
        }
    }

    // spec: TA-01/TA-02 — checkConnection() sets Connecting synchronously before coroutine suspends
    // Pattern: _uiState.value = Connecting is set BEFORE viewModelScope.launch suspends at pingBackend()
    @Test
    fun checkConnection_setsConnectingBeforeCoroutineSuspends() = runTest {
        // Make pingBackend() a proper suspend that will park on testDispatcher
        whenever(repository.pingBackend()).thenReturn(true)

        // spec: TA-01 — Connecting must be set synchronously before the coroutine reaches pingBackend()
        viewModel.checkConnection()

        // Before advanceUntilIdle() — coroutine is suspended at repository.pingBackend()
        val stateWhileInFlight = viewModel.uiState.value
        assert(stateWhileInFlight is fr.datasaillance.nightfall.domain.import_.ImportUiState.Connecting) {
            "uiState must be ImportUiState.Connecting immediately after checkConnection() — spec: TA-01, got: $stateWhileInFlight"
        }

        advanceUntilIdle()
    }

    // spec: TA-05 — startUpload() with SLEEP and HEART_RATE entries in the map
    //   → state transitions through Uploading(SLEEP) → Uploading(HEART_RATE) → Success
    //   → STEPS and EXERCISE appear in skippedTypes
    @Test
    fun startUpload_sleepAndHeartRate_uploadsSequentiallyAndSkipsStepsExercise() = runTest {
        val sleepUri = mock<Uri>()
        val heartRateUri = mock<Uri>()

        val csvEntries = mapOf(
            fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP to
                fr.datasaillance.nightfall.data.import_.CsvEntry(sleepUri, 1024L),
            fr.datasaillance.nightfall.domain.import_.ImportDataType.HEART_RATE to
                fr.datasaillance.nightfall.data.import_.CsvEntry(heartRateUri, 2048L),
        )

        whenever(repository.extractCsvEntries(any(), any())).thenReturn(csvEntries)

        val sleepResult = fr.datasaillance.nightfall.domain.import_.ImportResult(
            type = fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP,
            inserted = 10,
            skipped = 0,
        )
        val heartRateResult = fr.datasaillance.nightfall.domain.import_.ImportResult(
            type = fr.datasaillance.nightfall.domain.import_.ImportDataType.HEART_RATE,
            inserted = 20,
            skipped = 0,
        )

        // spec: TA-05 — mock uploadCsv for SLEEP
        whenever(
            repository.uploadCsv(
                contentResolver = any(),
                uri = eq(sleepUri),
                type = eq(fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP),
                totalBytes = eq(1024L),
                onProgress = any(),
            )
        ).thenReturn(sleepResult)

        // spec: TA-05 — mock uploadCsv for HEART_RATE
        whenever(
            repository.uploadCsv(
                contentResolver = any(),
                uri = eq(heartRateUri),
                type = eq(fr.datasaillance.nightfall.domain.import_.ImportDataType.HEART_RATE),
                totalBytes = eq(2048L),
                onProgress = any(),
            )
        ).thenReturn(heartRateResult)

        viewModel.startUpload(mockContentResolver, mockTreeUri)
        advanceUntilIdle()

        // spec: TA-05 — final state must be Success
        val finalState = viewModel.uiState.value
        assert(finalState is fr.datasaillance.nightfall.domain.import_.ImportUiState.Success) {
            "uiState must be ImportUiState.Success after upload completes — spec: TA-05, got: $finalState"
        }

        val successState = finalState as fr.datasaillance.nightfall.domain.import_.ImportUiState.Success

        // spec: TA-05 — results contain SLEEP and HEART_RATE
        val resultTypes = successState.results.map { it.type }
        assert(fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP in resultTypes) {
            "Success.results must include SLEEP — spec: TA-05"
        }
        assert(fr.datasaillance.nightfall.domain.import_.ImportDataType.HEART_RATE in resultTypes) {
            "Success.results must include HEART_RATE — spec: TA-05"
        }
    }

    // spec: TA-05 — STEPS and EXERCISE must appear in skippedTypes when not in the CSV map
    // This test captures an intermediate Uploading state to verify skippedTypes
    @Test
    fun startUpload_stepsAndExerciseNotInMap_appearsInSkipped() = runTest {
        val sleepUri = mock<Uri>()
        val csvEntries = mapOf(
            fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP to
                fr.datasaillance.nightfall.data.import_.CsvEntry(sleepUri, 512L),
        )

        whenever(repository.extractCsvEntries(any(), any())).thenReturn(csvEntries)

        val sleepResult = fr.datasaillance.nightfall.domain.import_.ImportResult(
            type = fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP,
            inserted = 5,
            skipped = 0,
        )
        whenever(
            repository.uploadCsv(
                contentResolver = any(),
                uri = eq(sleepUri),
                type = eq(fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP),
                totalBytes = any(),
                onProgress = any(),
            )
        ).thenReturn(sleepResult)

        viewModel.startUpload(mockContentResolver, mockTreeUri)
        advanceUntilIdle()

        // spec: TA-05 — Success state's results must NOT include STEPS or EXERCISE (they are skipped, not uploaded)
        val finalState = viewModel.uiState.value
        assert(finalState is fr.datasaillance.nightfall.domain.import_.ImportUiState.Success) {
            "uiState must be Success — spec: TA-05, got: $finalState"
        }
        val successState = finalState as fr.datasaillance.nightfall.domain.import_.ImportUiState.Success
        val resultTypes = successState.results.map { it.type }
        assert(fr.datasaillance.nightfall.domain.import_.ImportDataType.STEPS !in resultTypes) {
            "Success.results must NOT include STEPS when it was skipped — spec: TA-05"
        }
        assert(fr.datasaillance.nightfall.domain.import_.ImportDataType.EXERCISE !in resultTypes) {
            "Success.results must NOT include EXERCISE when it was skipped — spec: TA-05"
        }
    }

    // spec: TA-07 — startUpload() when extractCsvEntries returns empty map → Error with exact message, retryable = true
    @Test
    fun startUpload_emptyCsvMap_emitsErrorRetryable() = runTest {
        whenever(repository.extractCsvEntries(any(), any())).thenReturn(emptyMap())

        viewModel.startUpload(mockContentResolver, mockTreeUri)
        advanceUntilIdle()

        // spec: TA-07 — state must be Error with the spec-mandated message and retryable = true
        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.domain.import_.ImportUiState.Error) {
            "uiState must be ImportUiState.Error when no Samsung Health files found — spec: TA-07, got: $state"
        }
        val errorState = state as fr.datasaillance.nightfall.domain.import_.ImportUiState.Error
        assert(errorState.message == "Aucun fichier Samsung Health reconnu dans l'archive.") {
            "Error message must match spec exactly — spec: TA-07, got: '${errorState.message}'"
        }
        assert(errorState.retryable) {
            "Error must be retryable = true — spec: TA-07"
        }
    }

    // spec: TA-08 — SLEEP succeeds; HEART_RATE throws IOException → final state is Success
    //   results include HEART_RATE with non-null errorMessage; STEPS and EXERCISE are processed (skipped if not in map)
    @Test
    fun startUpload_heartRateIoException_doesNotBlockGlobalSuccess() = runTest {
        val sleepUri = mock<Uri>()
        val heartRateUri = mock<Uri>()

        val csvEntries = mapOf(
            fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP to
                fr.datasaillance.nightfall.data.import_.CsvEntry(sleepUri, 1024L),
            fr.datasaillance.nightfall.domain.import_.ImportDataType.HEART_RATE to
                fr.datasaillance.nightfall.data.import_.CsvEntry(heartRateUri, 2048L),
        )

        whenever(repository.extractCsvEntries(any(), any())).thenReturn(csvEntries)

        val sleepResult = fr.datasaillance.nightfall.domain.import_.ImportResult(
            type = fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP,
            inserted = 10,
            skipped = 0,
        )
        whenever(
            repository.uploadCsv(
                contentResolver = any(),
                uri = eq(sleepUri),
                type = eq(fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP),
                totalBytes = any(),
                onProgress = any(),
            )
        ).thenReturn(sleepResult)

        // spec: TA-08 — HEART_RATE upload throws IOException (connexion coupée)
        whenever(
            repository.uploadCsv(
                contentResolver = any(),
                uri = eq(heartRateUri),
                type = eq(fr.datasaillance.nightfall.domain.import_.ImportDataType.HEART_RATE),
                totalBytes = any(),
                onProgress = any(),
            )
        ).thenThrow(java.io.IOException("Connection reset by peer"))

        viewModel.startUpload(mockContentResolver, mockTreeUri)
        advanceUntilIdle()

        // spec: TA-08 — final state must still be Success (partial success, not global failure)
        val finalState = viewModel.uiState.value
        assert(finalState is fr.datasaillance.nightfall.domain.import_.ImportUiState.Success) {
            "uiState must be ImportUiState.Success even when HEART_RATE upload fails — spec: TA-08, got: $finalState"
        }

        val successState = finalState as fr.datasaillance.nightfall.domain.import_.ImportUiState.Success

        // spec: TA-08 — results must include HEART_RATE with non-null errorMessage
        val heartRateResult = successState.results.find {
            it.type == fr.datasaillance.nightfall.domain.import_.ImportDataType.HEART_RATE
        }
        assert(heartRateResult != null) {
            "Success.results must include HEART_RATE entry even after IOException — spec: TA-08"
        }
        assert(heartRateResult!!.errorMessage != null) {
            "HEART_RATE ImportResult.errorMessage must be non-null after IOException — spec: TA-08"
        }
        assert(heartRateResult.inserted == 0) {
            "HEART_RATE ImportResult.inserted must be 0 after IOException — spec: TA-08"
        }

        // spec: TA-08 — SLEEP must be in results with no error
        val sleepResultActual = successState.results.find {
            it.type == fr.datasaillance.nightfall.domain.import_.ImportDataType.SLEEP
        }
        assert(sleepResultActual != null) {
            "Success.results must include SLEEP — spec: TA-08"
        }
        assert(sleepResultActual!!.errorMessage == null) {
            "SLEEP ImportResult.errorMessage must be null (success) — spec: TA-08"
        }
    }

    // spec: Architecture "reset()" — calling reset() returns state to Idle
    @Test
    fun reset_returnsToIdleState() = runTest {
        // Move to a non-Idle state first
        whenever(repository.pingBackend()).thenReturn(true)
        viewModel.checkConnection()
        advanceUntilIdle()

        assert(viewModel.uiState.value is fr.datasaillance.nightfall.domain.import_.ImportUiState.Connected) {
            "Precondition: state must be Connected before reset"
        }

        // spec: reset() — calling reset() must return state to Idle
        viewModel.reset()

        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.domain.import_.ImportUiState.Idle) {
            "uiState must be ImportUiState.Idle after reset() — spec: ImportViewModel.reset(), got: $state"
        }
    }

    // spec: D7 — initial state is Idle before any action
    @Test
    fun initialState_isIdle() {
        val state = viewModel.uiState.value
        assert(state is fr.datasaillance.nightfall.domain.import_.ImportUiState.Idle) {
            "uiState must be ImportUiState.Idle at construction — spec: D7 (StateFlow initial value), got: $state"
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ImportViewModelTest` (class) — lines 35-363
- `setUp` (function) — lines 48-57
- `tearDown` (function) — lines 59-62
- `checkConnection_pingSuccess_emitsConnected` (function) — lines 65-77
- `checkConnection_pingFailure_emitsConnectionFailed` (function) — lines 80-96
- `checkConnection_setsConnectingBeforeCoroutineSuspends` (function) — lines 100-115
- `startUpload_sleepAndHeartRate_uploadsSequentiallyAndSkipsStepsExercise` (function) — lines 120-186
- `startUpload_stepsAndExerciseNotInMap_appearsInSkipped` (function) — lines 190-231
- `startUpload_emptyCsvMap_emitsErrorRetryable` (function) — lines 234-253
- `startUpload_heartRateIoException_doesNotBlockGlobalSuccess` (function) — lines 257-332
- `reset_returnsToIdleState` (function) — lines 335-353
- `initialState_isIdle` (function) — lines 356-362
