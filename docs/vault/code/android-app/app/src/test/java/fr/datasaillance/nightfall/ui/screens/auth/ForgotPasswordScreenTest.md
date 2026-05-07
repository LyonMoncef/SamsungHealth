---
type: code-source
language: kotlin
file_path: android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreenTest.kt
git_blob: 85c533b7377d73106c8301d4c847d4c86d527af3
last_synced: '2026-05-07T02:02:39Z'
loc: 71
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreenTest.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreenTest.kt`](../../../android-app/app/src/test/java/fr/datasaillance/nightfall/ui/screens/auth/ForgotPasswordScreenTest.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.auth

// spec: Tests d'acceptation TA-AUTH-07
// spec: section "ForgotPasswordScreen" layout + "Parité light / dark mode"
// RED by construction: fr.datasaillance.nightfall.ui.screens.auth.ForgotPasswordScreen does not exist yet

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.ui.screens.auth.ForgotPasswordScreen
// fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
// fr.datasaillance.nightfall.viewmodel.auth.ForgotPasswordUiState
// fr.datasaillance.nightfall.data.http.NightfallApi (auth methods don't exist yet)

class ForgotPasswordScreenTest {

    // spec: D10 — Paparazzi offline screenshot tests
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    private fun buildViewModel(): fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel {
        val api = mock<fr.datasaillance.nightfall.data.http.NightfallApi>()
        val tokenDataStore = mock<fr.datasaillance.nightfall.data.auth.TokenDataStore>()
        // spec: D1 — AuthViewModel constructed directly without Hilt (issue #52)
        return fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel(api, tokenDataStore)
    }

    // spec: TA-AUTH-07 — ForgotPasswordScreen dark mode, state = ForgotPasswordUiState.Idle
    // spec: section "ForgotPasswordScreen" — titre, champ email, bouton "Envoyer le lien"
    @Test
    fun forgotPasswordScreen_idle_dark() {
        val viewModel = buildViewModel()

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                fr.datasaillance.nightfall.ui.screens.auth.ForgotPasswordScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        // spec: TA-AUTH-07 — Idle state: titre visible, AuthTextField email, bouton "Envoyer le lien"
    }

    // spec: TA-AUTH-07 — ForgotPasswordScreen dark mode, state = ForgotPasswordUiState.Sent
    // spec: section "ForgotPasswordScreen" — "Un email a été envoyé si ce compte existe." (anti-enum)
    @Test
    fun forgotPasswordScreen_sent_dark() {
        // spec: TA-AUTH-07 — anti-enum: same confirmation message regardless of whether account exists
        // The Screen replaces the form with a confirmation message when forgotState == Sent
        val viewModel = buildViewModel()

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                fr.datasaillance.nightfall.ui.screens.auth.ForgotPasswordScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }
        // spec: TA-AUTH-07 — Sent state: form replaced by "Un email a été envoyé si ce compte existe."
        // spec: TA-AUTH-07 — TextButton "Retour à la connexion" remains visible
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `ForgotPasswordScreenTest` (class) — lines 19-71
- `buildViewModel` (function) — lines 28-33
- `forgotPasswordScreen_idle_dark` (function) — lines 37-50
- `forgotPasswordScreen_sent_dark` (function) — lines 54-70
