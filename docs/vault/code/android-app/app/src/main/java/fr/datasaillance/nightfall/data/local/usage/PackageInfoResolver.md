---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/PackageInfoResolver.kt
git_blob: 2f9dfba01cbe529326107127d72f01fba02966c7
last_synced: '2026-05-20T18:53:28Z'
loc: 28
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/PackageInfoResolver.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/PackageInfoResolver.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/usage/PackageInfoResolver.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.usage

import android.content.pm.PackageManager

/**
 * Résout `packageName → label utilisateur` via `PackageManager.getApplicationLabel`.
 * Cache mémoire — un packageName peut être résolu N fois pour différents jours,
 * inutile de re-query Android à chaque fois.
 *
 * Si le package est désinstallé ou inconnu (rare — apps removed après collecte),
 * fallback sur le packageName brut.
 */
open class PackageInfoResolver(private val pm: PackageManager) {

    private val cache = HashMap<String, String>()

    open fun labelFor(packageName: String): String {
        cache[packageName]?.let { return it }
        val label = runCatching {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: packageName
        cache[packageName] = label
        return label
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `PackageInfoResolver` (class) — lines 13-28
- `labelFor` (function) — lines 17-27
