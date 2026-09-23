---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt
git_blob: 57474487dd33e85cf2020a2eb1b58a1347202f8c
last_synced: '2026-05-09T16:40:48Z'
loc: 34
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ImportApiResponse` (class) — lines 16-17
- `NightfallApi` (class) — lines 19-34
- `health` (function) — lines 20-21
- `login` (function) — lines 23-24
- `register` (function) — lines 26-27
- `requestPasswordReset` (function) — lines 29-30
- `googleStart` (function) — lines 32-33
