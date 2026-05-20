---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/NightfallApplication.kt
git_blob: eaede47d93d15ff03dfdf106851edfb9959beb9c
last_synced: '2026-05-20T18:28:21Z'
loc: 15
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
import fr.datasaillance.nightfall.data.local.usage.UsageStatsScheduler
import timber.log.Timber

class NightfallApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // Phase B_us : worker quotidien (idempotent via uniqueWorkName).
        // Le worker no-op silencieusement si la permission UsageStats est absente.
        UsageStatsScheduler.schedulePeriodic(this)
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallApplication` (class) — lines 7-15
- `onCreate` (function) — lines 8-14
