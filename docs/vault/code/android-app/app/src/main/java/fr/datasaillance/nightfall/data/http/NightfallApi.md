---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt
git_blob: e00c2ce51b9232acaada50a5ff6f07cf8aa0031d
last_synced: '2026-05-07T02:02:39Z'
loc: 24
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
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallApi` (class) — lines 9-24
- `health` (function) — lines 10-11
- `login` (function) — lines 13-14
- `register` (function) — lines 16-17
- `requestPasswordReset` (function) — lines 19-20
- `googleStart` (function) — lines 22-23
