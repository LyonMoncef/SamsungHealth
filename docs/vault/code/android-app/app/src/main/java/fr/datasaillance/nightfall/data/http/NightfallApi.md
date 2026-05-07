---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt
git_blob: 3f989123930d5deec5041d161889cc1dc163e6c3
last_synced: '2026-05-07T03:10:49Z'
loc: 46
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

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

@kotlinx.serialization.Serializable
data class ImportApiResponse(val inserted: Int, val skipped: Int)

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
- `ImportApiResponse` (class) — lines 12-13
- `NightfallApi` (class) — lines 15-46
- `health` (function) — lines 16-17
- `login` (function) — lines 19-20
- `register` (function) — lines 22-23
- `requestPasswordReset` (function) — lines 25-26
- `googleStart` (function) — lines 28-29
- `importSleep` (function) — lines 31-33
- `importHeartRate` (function) — lines 35-37
- `importSteps` (function) — lines 39-41
- `importExercise` (function) — lines 43-45
