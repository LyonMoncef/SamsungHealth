---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/di/NetworkModule.kt
git_blob: 41c5a12a6a68b9fff89ad4344068125d0942bc8d
last_synced: '2026-05-07T00:48:24Z'
loc: 35
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/di/NetworkModule.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/di/NetworkModule.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/di/NetworkModule.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import fr.datasaillance.nightfall.data.auth.TokenDataStore
import fr.datasaillance.nightfall.data.http.AuthInterceptor
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.data.network.BackendUrlStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {

    fun provideAuthInterceptor(tokenDataStore: TokenDataStore): AuthInterceptor =
        AuthInterceptor(tokenDataStore)

    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    fun provideRetrofit(okHttpClient: OkHttpClient, backendUrlStore: BackendUrlStore): Retrofit =
        Retrofit.Builder()
            .baseUrl(backendUrlStore.getUrl().trimEnd('/') + "/")
            .client(okHttpClient)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()

    fun provideNightfallApi(retrofit: Retrofit): NightfallApi =
        retrofit.create(NightfallApi::class.java)
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `provideAuthInterceptor` (function) — lines 16-17
- `provideOkHttpClient` (function) — lines 19-24
- `provideRetrofit` (function) — lines 26-31
- `provideNightfallApi` (function) — lines 33-34
