---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/NightfallApi.kt
git_blob: 3ff676ff6624b9cd7074cf4be2c6058e47fbecc1
last_synced: '2026-05-07T00:48:24Z'
loc: 9
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
import retrofit2.http.GET

interface NightfallApi {
    @GET("health")
    suspend fun health(): Response<Unit>
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallApi` (class) — lines 6-9
- `health` (function) — lines 7-8
