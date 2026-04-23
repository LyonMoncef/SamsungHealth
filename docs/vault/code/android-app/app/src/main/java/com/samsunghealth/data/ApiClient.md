---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/com/samsunghealth/data/ApiClient.kt
git_blob: 19b558888ba450d752c3a776c409256452151eb4
last_synced: '2026-04-23T08:13:16Z'
loc: 109
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/com/samsunghealth/data/ApiClient.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/com/samsunghealth/data/ApiClient.kt`](../../../android-app/app/src/main/java/com/samsunghealth/data/ApiClient.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package com.samsunghealth.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@Serializable
data class StagePayload(
    val stage_type: String,
    val stage_start: String,
    val stage_end: String,
)

@Serializable
data class SessionPayload(
    val sleep_start: String,
    val sleep_end: String,
    val stages: List<StagePayload>? = null,
)

@Serializable
data class BulkPayload(
    val sessions: List<SessionPayload>,
)

@Serializable
data class StepsHourlyPayload(
    val date: String,
    val hour: Int,
    val step_count: Int,
)

@Serializable
data class StepsBulkPayload(
    val records: List<StepsHourlyPayload>,
)

@Serializable
data class HeartRateHourlyPayload(
    val date: String,
    val hour: Int,
    val min_bpm: Int,
    val max_bpm: Int,
    val avg_bpm: Int,
    val sample_count: Int,
)

@Serializable
data class HeartRateBulkPayload(
    val records: List<HeartRateHourlyPayload>,
)

@Serializable
data class ExercisePayload(
    val exercise_type: String,
    val exercise_start: String,
    val exercise_end: String,
    val duration_minutes: Double,
)

@Serializable
data class ExerciseBulkPayload(
    val sessions: List<ExercisePayload>,
)

@Serializable
data class InsertResponse(
    val inserted: Int,
    val skipped: Int,
)

interface HealthApi {
    @POST("/api/sleep")
    suspend fun postSleep(@Body body: BulkPayload): InsertResponse

    @POST("/api/steps")
    suspend fun postSteps(@Body body: StepsBulkPayload): InsertResponse

    @POST("/api/heartrate")
    suspend fun postHeartRate(@Body body: HeartRateBulkPayload): InsertResponse

    @POST("/api/exercise")
    suspend fun postExercise(@Body body: ExerciseBulkPayload): InsertResponse
}

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(baseUrl: String): HealthApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(HealthApi::class.java)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `StagePayload` (class) — lines 13-18
- `SessionPayload` (class) — lines 20-25
- `BulkPayload` (class) — lines 27-30
- `StepsHourlyPayload` (class) — lines 32-37
- `StepsBulkPayload` (class) — lines 39-42
- `HeartRateHourlyPayload` (class) — lines 44-52
- `HeartRateBulkPayload` (class) — lines 54-57
- `ExercisePayload` (class) — lines 59-65
- `ExerciseBulkPayload` (class) — lines 67-70
- `InsertResponse` (class) — lines 72-76
- `HealthApi` (class) — lines 78-90
- `postSleep` (function) — lines 79-80
- `postSteps` (function) — lines 82-83
- `postHeartRate` (function) — lines 85-86
- `postExercise` (function) — lines 88-89
- `create` (function) — lines 95-108
