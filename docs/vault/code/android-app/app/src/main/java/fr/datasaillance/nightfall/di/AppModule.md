---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/di/AppModule.kt
git_blob: 3c347ed626e0cb9476052e9f3783c920fbaec161
last_synced: '2026-05-07T03:51:34Z'
loc: 13
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
import fr.datasaillance.nightfall.data.settings.SettingsDataStore

object AppModule {
    fun provideTokenDataStore(context: Context): TokenDataStore =
        TokenDataStore(context)

    fun provideSettingsDataStore(context: Context): SettingsDataStore =
        SettingsDataStore(context)
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `provideTokenDataStore` (function) — lines 8-9
- `provideSettingsDataStore` (function) — lines 11-12
