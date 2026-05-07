---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/RetrofitClient.kt
git_blob: e66d3ecee00a0fc4d7fcba15c7280b5f08c169d2
last_synced: '2026-05-07T00:48:24Z'
loc: 24
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/RetrofitClient.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/RetrofitClient.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/RetrofitClient.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.http

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {
    fun create(baseUrl: String, authInterceptor: AuthInterceptor): NightfallApi {
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(okHttp)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NightfallApi::class.java)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `create` (function) — lines 11-23
