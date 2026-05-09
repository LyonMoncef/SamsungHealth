---
type: code-source
language: kotlin
file_path: android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepNightCard.kt
git_blob: 6cf69f76130deaedb0d53d1a64630cec1496faf6
last_synced: '2026-05-09T04:03:35Z'
loc: 132
annotations: []
imports: []
exports: []
tags:
- code
- kotlin
---

# android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepNightCard.kt

> [!info] Code mirror
> Ce fichier est un **miroir auto-généré** de [`android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepNightCard.kt`](../../../android-app/app/src/native/java/fr/datasaillance/nightfall/ui/screens/sleep/SleepNightCard.kt).
> Code = source de vérité. Annotations dans `docs/vault/annotations/`.
> Régénéré par `code-cartographer` au commit. Ne pas éditer directement.

```kotlin
package fr.datasaillance.nightfall.ui.screens.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.datasaillance.nightfall.data.sleep.SleepSessionResponse
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FMT_NIGHT = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)
private val FMT_TIME = DateTimeFormatter.ofPattern("HH:mm")

private val ColorTeal = Color(0xFF0E9EB0)
private val ColorAmber = Color(0xFFD37C04)
private val ColorRed = Color(0xFFB00020)

@Composable
fun SleepNightCard(
    session: SleepSessionResponse,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val start = remember(session.sleep_start) {
        runCatching { OffsetDateTime.parse(session.sleep_start) }.getOrNull()
    }
    val end = remember(session.sleep_end) {
        runCatching { OffsetDateTime.parse(session.sleep_end) }.getOrNull()
    }

    val duration = if (start != null && end != null) Duration.between(start, end) else null
    val hours = duration?.toHours() ?: 0L
    val minutes = duration?.toMinutesPart() ?: 0

    val indicatorColor = when {
        hours >= 7 -> ColorTeal
        hours >= 5 -> ColorAmber
        else -> ColorRed
    }

    val nightLabel = start?.format(FMT_NIGHT)?.replaceFirstChar { it.uppercase() } ?: ""
    val bedTime = start?.format(FMT_TIME) ?: ""
    val wakeTime = end?.format(FMT_TIME) ?: ""
    val durationText = if (duration != null) "${hours}h ${minutes.toString().padStart(2, '0')}" else ""

    val deepScore = remember(session.stages, duration) {
        val stages = session.stages
        if (!stages.isNullOrEmpty() && duration != null && duration.toMinutes() > 0) {
            val deepMinutes = stages
                .filter { it.stage == "DEEP" }
                .sumOf { stage ->
                    val s = runCatching { OffsetDateTime.parse(stage.stage_start) }.getOrNull()
                    val e = runCatching { OffsetDateTime.parse(stage.stage_end) }.getOrNull()
                    if (s != null && e != null) Duration.between(s, e).toMinutes() else 0L
                }
            (deepMinutes * 100L / duration.toMinutes()).toInt()
        } else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleep_card_${session.id}")
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(indicatorColor)
            )
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text(
                    text = nightLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "Coucher $bedTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Réveil $wakeTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (deepScore != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profond $deepScore%",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTeal
                        )
                    }
                }
            }
        }
    }
}
```

---

## Appendix — symbols & navigation *(auto)*

### Symbols
- `SleepNightCard` (function) — lines 38-132
