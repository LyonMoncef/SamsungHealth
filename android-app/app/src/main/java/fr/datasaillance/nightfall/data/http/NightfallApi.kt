package fr.datasaillance.nightfall.data.http

import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

@kotlinx.serialization.Serializable
data class ImportApiResponse(val inserted: Int, val skipped: Int)

interface NightfallApi {
    @GET("healthz")
    suspend fun health(): Response<Unit>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest, @Header("X-Registration-Token") registrationToken: String?): RegisterResponse

    @POST("auth/password/reset/request")
    suspend fun requestPasswordReset(@Body body: PasswordResetRequest): StatusResponse

    @POST("auth/google/start")
    suspend fun googleStart(@Body body: GoogleStartRequest): GoogleStartResponse

    @GET("api/sleep")
    suspend fun getSleepSessions(
        @Header("Authorization") token: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("include_stages") includeStages: Boolean = true
    ): List<SleepSessionResponse>

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

    @Multipart
    @POST("api/sleep/import-stages")
    suspend fun importSleepStages(@Part file: MultipartBody.Part): ImportApiResponse
}
