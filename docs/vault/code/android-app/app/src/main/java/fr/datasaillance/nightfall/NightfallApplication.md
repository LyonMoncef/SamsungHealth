---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/NightfallApplication.kt
git_blob: 4c29484e08f98a4d0d2dff45baf03f7c5855a49b
last_synced: '2026-05-20T18:53:28Z'
loc: 19
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
        // try/catch défensif pour Robolectric (WorkManager pas init en test) — pas
        // d'impact prod, WorkManager est toujours initialisé sur device via le
        // ContentProvider AndroidX par défaut.
        runCatching { UsageStatsScheduler.schedulePeriodic(this) }
            .onFailure { Timber.w("scope=app onCreate scheduler_failed error=${it::class.simpleName}") }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `NightfallApplication` (class) — lines 7-19
- `onCreate` (function) — lines 8-18
