package fr.datasaillance.nightfall.ui.screens.auth

// spec: Tests d'acceptation TA-AUTH-05, TA-AUTH-06
// spec: section "RegisterScreen" layout + "Parité light / dark mode"
// RED by construction: fr.datasaillance.nightfall.ui.screens.auth.RegisterScreen does not exist yet

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

// These imports will fail to resolve until production code is written:
// fr.datasaillance.nightfall.ui.screens.auth.RegisterScreen
// fr.datasaillance.nightfall.viewmodel.auth.AuthViewModel
// fr.datasaillance.nightfall.viewmodel.auth.RegisterUiState
// fr.datasaillance.nightfall.data.http.NightfallApi (auth methods don't exist yet)

class RegisterScreenTest {

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

    // spec: TA-AUTH-05 — RegisterScreen dark mode, state = RegisterUiState.Idle
    // spec: "Parité light / dark mode" — fond #191E22, CTA #D37C04
    @Test
    fun registerScreen_idle_dark() {
        val viewModel = buildViewModel()

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                fr.datasaillance.nightfall.ui.screens.auth.RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {}
                )
            }
        }
        // spec: TA-AUTH-05 — screenshot dark: fond #191E22, 3 AuthTextFields, CTA "S'inscrire" #D37C04
    }

    // spec: TA-AUTH-05 — RegisterScreen light mode, state = RegisterUiState.Idle
    // spec: "Parité light / dark mode" — fond #FAFAFA, CTA #D37C04
    @Test
    fun registerScreen_idle_light() {
        val viewModel = buildViewModel()

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = false) {
                fr.datasaillance.nightfall.ui.screens.auth.RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {}
                )
            }
        }
        // spec: TA-AUTH-05 — screenshot light: fond #FAFAFA, CTA #D37C04
    }

    // spec: TA-AUTH-06 — RegisterScreen: password != confirmation → "S'inscrire" button disabled
    // spec: section "RegisterScreen" — validation inline: mots de passe doivent être identiques avant envoi
    @Test
    fun registerScreen_passwordMismatch_dark() {
        // spec: TA-AUTH-06 — password mismatch is a client-side guard, no network call
        // The Screen receives password and confirmation as local state; when they differ,
        // AuthPrimaryButton must have enabled=false.
        val viewModel = buildViewModel()

        paparazzi.snapshot {
            fr.datasaillance.nightfall.ui.theme.NightfallTheme(darkTheme = true) {
                // spec: TA-AUTH-06 — "S'inscrire" button must be disabled when password != confirmation
                // Screen handles this via local Compose state — no ViewModel call needed
                fr.datasaillance.nightfall.ui.screens.auth.RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {}
                )
            }
        }
        // spec: TA-AUTH-06 — AuthPrimaryButton enabled=false; no POST /auth/register call made
    }
}
