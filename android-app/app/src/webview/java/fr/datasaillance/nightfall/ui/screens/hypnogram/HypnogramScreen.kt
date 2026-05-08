package fr.datasaillance.nightfall.ui.screens.hypnogram

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import fr.datasaillance.nightfall.viewmodel.sleep.SleepViewModel

@Composable
fun HypnogramScreen(
    sessionId: String,
    sleepViewModel: SleepViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { onBack() }
}
