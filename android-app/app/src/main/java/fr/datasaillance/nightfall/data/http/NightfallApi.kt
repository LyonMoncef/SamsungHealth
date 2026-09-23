package fr.datasaillance.nightfall.data.http

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// Phase D : les endpoints `/api/sleep/import*`, `/api/heartrate/import`,
// `/api/steps/import`, `/api/exercise/import` et `GET /api/sleep` ne sont plus
// utilisés — l'app lit/écrit en local via Room (LocalSleepRepository +
// LocalImportService). Les endpoints serveur sont conservés pour rétrocompat
// éventuelle (cf. spec local-first §Phase D) mais l'API client ne les expose
// plus pour éviter tout retour en arrière silencieux.

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
}
