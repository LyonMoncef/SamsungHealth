---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/NightfallApplication.kt
git_blob: 2df915ee0ce4d67969c6ee73a4475f0e9fc7e986
last_synced: '2026-05-07T00:48:24Z'
loc: 11
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/NightfallApplication.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/NightfallApplication.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/NightfallApplication.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall

import android.app.Application
import timber.log.Timber

class NightfallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallApplication` (class) — lines 6-11
- `onCreate` (function) — lines 7-10
