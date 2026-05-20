---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsPermissionHelper.kt
git_blob: 0be75b3b5c37c29d41e892877ff24c1690c50319
last_synced: '2026-05-09T18:49:36Z'
loc: 37
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsPermissionHelper.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsPermissionHelper.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/UsageStatsPermissionHelper.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Gère la permission `PACKAGE_USAGE_STATS` qui est de type `appop` :
 * elle ne se demande pas via `requestPermissions()` standard mais s'active
 * manuellement par l'utilisateur dans Settings → Apps → Usage Access.
 */
class UsageStatsPermissionHelper(private val context: Context) {

    fun hasPermission(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION") // unsafeCheckOpNoThrow API stable, deprecation cosmétique
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Intent à lancer pour amener l'utilisateur sur l'écran Settings de gestion
     * des accès Usage. Pas de `data=` explicite (juste l'action) — Settings
     * affiche la liste de toutes les apps demandant la permission.
     *
     * Le caller doit ajouter `FLAG_ACTIVITY_NEW_TASK` si lancé depuis un Context
     * non-Activity.
     */
    fun intentToGrantPermission(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `UsageStatsPermissionHelper` (class) — lines 14-37
- `hasPermission` (function) — lines 16-25
- `intentToGrantPermission` (function) — lines 35-36
