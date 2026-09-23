---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/di/NetworkModule.kt
git_blob: 78e3b1026e9a08b611209b41e27b6e02f03231b7
last_synced: '2026-05-09T13:23:16Z'
loc: 48
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
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
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
            .cookieJar(object : CookieJar {
                private val cookieStore = mutableMapOf<String, List<Cookie>>()
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    cookieStore[url.host] = cookies
                }
                override fun loadForRequest(url: HttpUrl): List<Cookie> =
                    cookieStore[url.host] ?: emptyList()
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            // 5 min pour absorber les imports lourds (sleep_stage = ~60k rows possible).
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
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
- `provideAuthInterceptor` (function) — lines 19-20
- `provideOkHttpClient` (function) — lines 22-37
- `saveFromResponse` (function) — lines 27-29
- `loadForRequest` (function) — lines 30-31
- `provideRetrofit` (function) — lines 39-44
- `provideNightfallApi` (function) — lines 46-47
