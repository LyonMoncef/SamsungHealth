package fr.datasaillance.nightfall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.ui.components.DsTopBar
import fr.datasaillance.nightfall.ui.theme.NightfallTheme

/**
 * Explication de l'usage des données, exigée par Health Connect : sans cette activité
 * (ACTION_SHOW_PERMISSIONS_RATIONALE, et VIEW_PERMISSION_USAGE sur Android 14+), la demande
 * de permissions n'aboutit pas. Déclarée dans le manifeste.
 */
class HealthConnectRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NightfallTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column {
                        DsTopBar(title = "Données Health Connect", eyebrow = "Nightfall")
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                "Nightfall lit votre sommeil, vos pas et leur historique complet pour rendre visible " +
                                    "votre rythme circadien.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "Ces données restent sur ce téléphone. Elles ne sont envoyées à aucun serveur, " +
                                    "ni à aucun service tiers. Nightfall ne les recopie pas : elles sont relues " +
                                    "dans Health Connect à chaque ouverture.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Seul un export que vous déclenchez vous-même permet d'en sortir une copie, " +
                                    "vers l'emplacement que vous choisissez.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "Nightfall demande uniquement la lecture. Il n'écrit rien dans Health Connect.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
