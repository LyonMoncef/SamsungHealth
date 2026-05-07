package fr.datasaillance.nightfall.data.http

import retrofit2.Response
import retrofit2.http.GET

interface NightfallApi {
    @GET("health")
    suspend fun health(): Response<Unit>
}
