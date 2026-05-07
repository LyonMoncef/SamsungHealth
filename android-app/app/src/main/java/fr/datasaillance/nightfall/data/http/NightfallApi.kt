package fr.datasaillance.nightfall.data.http

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface NightfallApi {
    @GET("health")
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
