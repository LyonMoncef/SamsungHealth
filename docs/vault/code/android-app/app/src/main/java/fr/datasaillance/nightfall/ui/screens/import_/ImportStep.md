---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/import_/ImportStep.kt
git_blob: 163687e8daf1eac2d5a2aec739ca682e39821544
last_synced: '2026-05-07T03:10:49Z'
loc: 7
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/import_/ImportStep.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/import_/ImportStep.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/ui/screens/import_/ImportStep.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.import_

sealed class ImportStep(val index: Int, val label: String) {
    object Connection : ImportStep(1, "Connexion")
    object Selection  : ImportStep(2, "Sélection")
    object Upload     : ImportStep(3, "Import")
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ImportStep` (class) — lines 3-7
