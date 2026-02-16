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
