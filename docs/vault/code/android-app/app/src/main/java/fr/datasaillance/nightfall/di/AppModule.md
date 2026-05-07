---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/di/AppModule.kt
git_blob: 1211e49a1bfe7903b4e590e752ed8cb3e924b484
last_synced: '2026-05-07T00:48:24Z'
loc: 9
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/di/AppModule.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/di/AppModule.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/di/AppModule.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.di

import android.content.Context
import fr.datasaillance.nightfall.data.auth.TokenDataStore

object AppModule {
    fun provideTokenDataStore(context: Context): TokenDataStore =
        TokenDataStore(context)
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `provideTokenDataStore` (function) — lines 7-8
