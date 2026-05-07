package fr.datasaillance.nightfall.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

private fun iconForDestination(destination: NavDestination): ImageVector = when (destination) {
    is NavDestination.Sleep    -> Icons.Default.Home
    is NavDestination.Trends   -> Icons.Default.ShowChart
    is NavDestination.Activity -> Icons.Default.FitnessCenter
    is NavDestination.Profile  -> Icons.Default.AccountCircle
    else                       -> Icons.Default.Home
}

@Composable
fun BottomNavBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavDestination.bottomNavItems().forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick  = { onNavigate(destination.route) },
                icon     = { Icon(iconForDestination(destination), contentDescription = destination.label) },
                label    = { Text(destination.label) }
            )
        }
    }
}
