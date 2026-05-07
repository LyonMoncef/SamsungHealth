package fr.datasaillance.nightfall.ui.theme

// spec: Tests d'acceptation TA-04, TA-05, TA-12, TA-13
// spec: section "Thème — NightfallTheme" + "Tests d'acceptation"
// RED by construction: fr.datasaillance.nightfall.ui.theme.* does not exist yet

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import org.junit.Rule
import org.junit.Test

// These imports will fail to resolve until the production code is written:
// fr.datasaillance.nightfall.ui.theme.NightfallTheme
// fr.datasaillance.nightfall.ui.theme.Background        (0xFF191E22)
// fr.datasaillance.nightfall.ui.theme.Teal700           (0xFF0E9EB0)
// fr.datasaillance.nightfall.ui.theme.Amber600          (0xFFD37C04)
// fr.datasaillance.nightfall.ui.theme.BackgroundLight   (0xFFFAFAFA)

class NightfallThemeTest {

    // spec: D10 — Paparazzi pour screenshot tests des composables (tests offline, pas d'émulateur)
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar"
    )

    // spec: TA-13 — fond #191E22, NavigationBar couleur #232E32, accent primary #0E9EB0
    @Test
    fun nightfallTheme_darkMode_snapshot() {
        paparazzi.snapshot {
            NightfallTheme(darkTheme = true) {
                Surface {
                    Text(text = "Nightfall Dark")
                }
            }
        }
        // Paparazzi compares to golden snapshot — fails RED until NightfallTheme exists
        // and until golden is recorded with `./gradlew recordPaparazziDebug`
    }

    // spec: TA-12 — screenshot NightfallTheme light correspond au snapshot de référence
    @Test
    fun nightfallTheme_lightMode_snapshot() {
        paparazzi.snapshot {
            NightfallTheme(darkTheme = false) {
                Surface {
                    Text(text = "Nightfall Light")
                }
            }
        }
        // Paparazzi compares to golden snapshot — fails RED until NightfallTheme exists
    }

    // spec: section "Thème — NightfallTheme" — primary = Color(0xFF0E9EB0)
    @Test
    fun nightfallTheme_primaryColor() {
        // Assert the constant exists and has the correct value
        // This test fails RED because Teal700 doesn't exist in fr.datasaillance.nightfall.ui.theme
        val expected = Color(0xFF0E9EB0)
        assert(Teal700 == expected) {
            "primary color (Teal700) must be 0xFF0E9EB0 per DataSaillance tokens — spec: Color.kt"
        }
    }

    // spec: section "Thème — NightfallTheme" — secondary = Color(0xFFD37C04)
    @Test
    fun nightfallTheme_secondaryColor() {
        val expected = Color(0xFFD37C04)
        assert(Amber600 == expected) {
            "secondary color (Amber600) must be 0xFFD37C04 per DataSaillance tokens — spec: Color.kt"
        }
    }

    // spec: TA-13 — fond background dark = #191E22
    @Test
    fun nightfallTheme_backgroundDark_color() {
        val expected = Color(0xFF191E22)
        assert(Background == expected) {
            "Background dark must be 0xFF191E22 — spec: Color.kt"
        }
    }

    // spec: TA-05 — fond background light = #FAFAFA
    @Test
    fun nightfallTheme_backgroundLight_color() {
        val expected = Color(0xFFFAFAFA)
        assert(BackgroundLight == expected) {
            "BackgroundLight must be 0xFFFAFAFA — spec: Color.kt"
        }
    }
}
