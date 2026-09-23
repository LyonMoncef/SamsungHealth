package fr.datasaillance.nightfall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import fr.datasaillance.nightfall.ui.navigation.NavGraph
import fr.datasaillance.nightfall.ui.theme.NightfallTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NightfallTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
