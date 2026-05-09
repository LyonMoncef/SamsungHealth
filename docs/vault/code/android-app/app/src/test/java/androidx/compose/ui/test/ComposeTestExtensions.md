---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/androidx/compose/ui/test/ComposeTestExtensions.kt
git_blob: 70c16d2017f4af8b6263bab9284936206807cd89
last_synced: '2026-05-09T04:03:35Z'
loc: 15
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/androidx/compose/ui/test/ComposeTestExtensions.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/androidx/compose/ui/test/ComposeTestExtensions.kt`](../../../android-app/app/src/test/java/androidx/compose/ui/test/ComposeTestExtensions.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
@file:JvmName("ComposeTestExtensionsKt")
package androidx.compose.ui.test

/**
 * Top-level extension shims for SemanticsNodeInteraction.assertExists() and assertDoesNotExist().
 * In compose-ui-test 1.7.x and 1.8.x, these are member functions on SemanticsNodeInteraction,
 * not top-level extension functions. The test file imports them as top-level functions,
 * so we provide these shims to bridge the gap.
 */

fun assertExists(node: SemanticsNodeInteraction): SemanticsNodeInteraction =
    node.assertExists()

fun assertDoesNotExist(node: SemanticsNodeInteraction) =
    node.assertDoesNotExist()
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `assertExists` (function) — lines 11-12
- `assertDoesNotExist` (function) — lines 14-15
