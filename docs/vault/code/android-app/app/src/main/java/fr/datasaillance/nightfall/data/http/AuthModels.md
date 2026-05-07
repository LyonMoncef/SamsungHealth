---
type: code-source
language: kotlin
file_path: android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/AuthModels.kt
git_blob: 5830c67f0834c1b963e8ebb7142cfbbe9de12e4d
last_synced: '2026-05-07T02:02:39Z'
loc: 12
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/AuthModels.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/AuthModels.kt`](../../../android-app/app/src/main/java/fr/datasaillance/nightfall/data/http/AuthModels.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.data.http

import kotlinx.serialization.Serializable

@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class LoginResponse(val access_token: String, val refresh_token: String, val token_type: String, val expires_in: Int)
@Serializable data class RegisterRequest(val email: String, val password: String)
@Serializable data class RegisterResponse(val id: String, val email: String)
@Serializable data class PasswordResetRequest(val email: String)
@Serializable data class StatusResponse(val status: String)
@Serializable data class GoogleStartRequest(val return_to: String? = null)
@Serializable data class GoogleStartResponse(val authorize_url: String)
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `LoginRequest` (class) — lines 5-5
- `LoginResponse` (class) — lines 6-6
- `RegisterRequest` (class) — lines 7-7
- `RegisterResponse` (class) — lines 8-8
- `PasswordResetRequest` (class) — lines 9-9
- `StatusResponse` (class) — lines 10-10
- `GoogleStartRequest` (class) — lines 11-11
- `GoogleStartResponse` (class) — lines 12-12
