---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/AuthInterceptor.kt
git_blob: a2d950af7ed5dba6a3c385279da3ac60258cbaa8
last_synced: '2026-05-07T00:48:24Z'
loc: 19
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/AuthInterceptor.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/AuthInterceptor.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/AuthInterceptor.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.http

import fr.datasaillance.nightfall.data.auth.TokenDataStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenDataStore: TokenDataStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenDataStore.getToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `AuthInterceptor` (class) — lines 7-19
- `intercept` (function) — lines 8-18
