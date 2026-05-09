---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/di/AppModule.kt
git_blob: ae126d603890e049b3a64e3625459b444eebf224
last_synced: '2026-05-09T04:03:35Z'
loc: 18
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
import fr.datasaillance.nightfall.data.http.NightfallApi
import fr.datasaillance.nightfall.data.settings.SettingsDataStore
import fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel

object AppModule {
    fun provideTokenDataStore(context: Context): TokenDataStore =
        TokenDataStore(context)

    fun provideSettingsDataStore(context: Context): SettingsDataStore =
        SettingsDataStore(context)

    fun provideAuthViewModel(api: NightfallApi, tokenDataStore: TokenDataStore): AuthViewModel =
        AuthViewModel(api, tokenDataStore)
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `provideTokenDataStore` (function) — lines 10-11
- `provideSettingsDataStore` (function) — lines 13-14
- `provideAuthViewModel` (function) — lines 16-17
