---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt
git_blob: 15be5d2254cfd3874a877df8f8d79f1928b8101d
last_synced: '2026-05-09T04:03:34Z'
loc: 56
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
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
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ImportApiResponse` (class) — lines 14-15
- `NightfallApi` (class) — lines 17-56
- `health` (function) — lines 18-19
- `login` (function) — lines 21-22
- `register` (function) — lines 24-25
- `requestPasswordReset` (function) — lines 27-28
- `googleStart` (function) — lines 30-31
- `getSleepSessions` (function) — lines 33-39
- `importSleep` (function) — lines 41-43
- `importHeartRate` (function) — lines 45-47
- `importSteps` (function) — lines 49-51
- `importExercise` (function) — lines 53-55
