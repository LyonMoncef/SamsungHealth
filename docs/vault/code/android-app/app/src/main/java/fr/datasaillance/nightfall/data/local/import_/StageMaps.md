---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/StageMaps.kt
git_blob: 3e04906a8015fbebeda6436c57510e9e15afd7ef
last_synced: '2026-05-09T15:30:15Z'
loc: 19
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/StageMaps.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/StageMaps.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/local/import_/StageMaps.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.local.import_

/** Codes Samsung sleep_stage → libellé canonique (mêmes valeurs que server/services/csv_import.py). */
internal val SLEEP_STAGE_MAP: Map<Int, String> = mapOf(
    40001 to "AWAKE",
    40002 to "LIGHT",
    40003 to "DEEP",
    40004 to "REM",
)

/** Codes Samsung exercise_type → libellé. Codes inconnus stockés comme `samsung_<code>`. */
internal val EXERCISE_TYPE_MAP: Map<Int, String> = mapOf(
    1001 to "running",
    1002 to "cycling",
    1007 to "walking",
    1008 to "hiking",
    3000 to "swimming",
    90001 to "indoor_cycling",
)
```

---

## Appendix — symbols & navigation *(auto)*
